package dev.mikaaah.tnkloot.config;

import com.google.gson.*;
import dev.mikaaah.tnkloot.TnkLoot;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Server-owned, immutable definitions. A failed reload never partially changes live settings. */
public final class TnkConfig {
    public enum Mode { ADVANCEMENTS, FTB_QUESTS, MANUAL, OFF }
    public record Tier(int rank, String id, String name, List<ResourceLocation> advancements) {}
    public record Weapon(String id, int minTier, int maxTier, int weight) {}
    public record Settings(Mode mode, boolean shareFtbTeams, boolean enableIdentification,
                           List<Tier> tiers, List<Weapon> weapons) {
        public int highestTier() { return tiers.getLast().rank(); }
        public Optional<Tier> tierForName(String id) { return tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst(); }
        public Tier tierForRank(int rank) {
            return tiers.stream().filter(t -> t.rank() <= rank)
                .max(Comparator.comparingInt(Tier::rank)).orElse(tiers.getFirst());
        }
    }

    private static volatile Settings current = new Settings(Mode.MANUAL, false, false,
        List.of(new Tier(0, "novice", "Novice", List.of())), List.of());
    private TnkConfig() {}
    public static Settings get() { return current; }
    public static Path path() { return FMLPaths.CONFIGDIR.get().resolve("tnkloot/tiers.json"); }
    public static boolean reload() { return reload(path()); }

    public static synchronized boolean reload(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (Files.notExists(path)) Files.writeString(path, defaultJson(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Settings candidate = parse(Files.readString(path, StandardCharsets.UTF_8));
            current = candidate;
            TnkLoot.LOG.info("Loaded {} tiers, {} weapons; mode {}", candidate.tiers().size(), candidate.weapons().size(), candidate.mode());
            return true;
        } catch (IOException | RuntimeException ex) {
            TnkLoot.LOG.error("Invalid config {}; keeping last known-good settings: {}", path, ex.getMessage());
            return false;
        }
    }

    public static String defaultJson() throws IOException {
        try (var stream = TnkConfig.class.getResourceAsStream("/default-tiers.json")) {
            if (stream == null) throw new IOException("Bundled default-tiers.json missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Strict parsing prevents a typo from silently deleting a tier or emptying a loot pool. */
    public static Settings parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        keys(root, "progression_mode", "share_ftb_teams", "identification_enabled", "tiers", "weapons");
        Mode mode = Mode.valueOf(string(root, "progression_mode", "ADVANCEMENTS").toUpperCase(Locale.ROOT));
        boolean share = bool(root, "share_ftb_teams", true);
        boolean identification = bool(root, "identification_enabled", true);
        List<Tier> tiers = new ArrayList<>();
        Set<Integer> ranks = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("tiers")) {
            JsonObject e = element.getAsJsonObject();
            keys(e, "rank", "id", "name", "advancements");
            int rank = integer(e, "rank", null);
            String id = string(e, "id", null).toLowerCase(Locale.ROOT);
            if (rank < 0 || rank == Integer.MAX_VALUE || !id.matches("[a-z0-9_]+") || !ranks.add(rank) || !ids.add(id))
                throw new IllegalArgumentException("Invalid or duplicate tier rank/id: " + e);
            List<ResourceLocation> advancements = new ArrayList<>();
            if (e.has("advancements")) for (JsonElement adv : e.getAsJsonArray("advancements")) {
                if (!adv.isJsonPrimitive() || !adv.getAsJsonPrimitive().isString())
                    throw new IllegalArgumentException("Advancement ID must be a string");
                ResourceLocation key = ResourceLocation.tryParse(adv.getAsString());
                if (key == null) throw new IllegalArgumentException("Invalid advancement ID: " + adv);
                advancements.add(key);
            }
            String name = string(e, "name", id);
            if (name.isBlank()) throw new IllegalArgumentException("Tier name cannot be blank");
            tiers.add(new Tier(rank, id, name, List.copyOf(advancements)));
        }
        tiers.sort(Comparator.comparingInt(Tier::rank));
        if (tiers.isEmpty() || tiers.getFirst().rank() != 0) throw new IllegalArgumentException("A rank-0 tier is required");
        List<Weapon> weapons = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("weapons")) {
            JsonObject e = element.getAsJsonObject();
            keys(e, "item", "min_tier", "max_tier", "weight");
            String id = string(e, "item", null);
            if (ResourceLocation.tryParse(id) == null) throw new IllegalArgumentException("Invalid item ID: " + id);
            int min = integer(e, "min_tier", null);
            int max = integer(e, "max_tier", Integer.MAX_VALUE);
            int weight = integer(e, "weight", 1);
            if (min < 0 || max < min || weight < 1) throw new IllegalArgumentException("Invalid tier range or weight: " + e);
            weapons.add(new Weapon(id, min, max, weight));
        }
        if (identification) for (Tier tier : tiers) {
            if (weapons.stream().noneMatch(w -> tier.rank() >= w.minTier() && tier.rank() <= w.maxTier()))
                throw new IllegalArgumentException("No configured weapons for tier " + tier.id());
        }
        return new Settings(mode, share, identification, List.copyOf(tiers), List.copyOf(weapons));
    }

    private static void keys(JsonObject object, String... allowed) {
        Set<String> accepted = Set.of(allowed);
        for (String key : object.keySet()) if (!accepted.contains(key)) throw new IllegalArgumentException("Unknown config key: " + key);
    }
    private static JsonPrimitive primitive(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException("Missing or invalid " + key);
        return value.getAsJsonPrimitive();
    }
    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (!object.has(key)) return fallback;
        JsonPrimitive value = primitive(object, key);
        if (!value.isBoolean()) throw new IllegalArgumentException(key + " must be a boolean");
        return value.getAsBoolean();
    }
    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) && fallback != null) return fallback;
        JsonPrimitive value = primitive(object, key);
        if (!value.isString()) throw new IllegalArgumentException(key + " must be a string");
        return value.getAsString();
    }
    private static int integer(JsonObject object, String key, Integer fallback) {
        if (!object.has(key) && fallback != null) return fallback;
        JsonPrimitive value = primitive(object, key);
        if (!value.isNumber()) throw new IllegalArgumentException(key + " must be an integer");
        return value.getAsBigDecimal().intValueExact();
    }
}