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
    public record Tier(int rank, String id, String name, List<ResourceLocation> advancements, ResourceLocation apotheosisRarity) {}
    public enum Category {
        WEAPON("unidentified_weapon"), BOW("unidentified_bow"), HELMET("unidentified_helmet"),
        CHESTPLATE("unidentified_chestplate"), LEGGINGS("unidentified_leggings"), BOOTS("unidentified_boots"),
        TOOL("unidentified_tool"), ACCESSORY("unidentified_accessory");
        private final String token;
        Category(String token) { this.token = token; }
        public String token() { return token; }
        public static Category parse(String value) { return valueOf(value.toUpperCase(Locale.ROOT)); }
    }
    public record Weapon(String id, Category category, int minTier, int maxTier, int weight) {}
    public record Costs(boolean enabled, String item, int defaultAmount, Map<Integer,Integer> tierAmounts, Map<String,Integer> itemAmounts) {
        public int amount(int tier, String itemId) { return itemAmounts.getOrDefault(itemId, tierAmounts.getOrDefault(tier, defaultAmount)); }
    }
    public record Settings(Mode mode, boolean shareFtbTeams, boolean enableIdentification, boolean replaceWeaponLoot,
                           boolean handheldIdentification, boolean stationIdentification, List<Tier> tiers,
                           List<Weapon> weapons, Costs costs) {
        public int highestTier() { return tiers.getLast().rank(); }
        public Optional<Tier> tierForName(String id) { return tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst(); }
        public Tier tierForRank(int rank) {
            return tiers.stream().filter(t -> t.rank() <= rank)
                .max(Comparator.comparingInt(Tier::rank)).orElse(tiers.getFirst());
        }
    }

    private static volatile Settings current = new Settings(Mode.MANUAL, false, false, false, true, true,
        List.of(new Tier(0, "novice", "Novice", List.of(), ResourceLocation.withDefaultNamespace("common"))), List.of(),
        new Costs(false, "minecraft:emerald", 1, Map.of(), Map.of()));
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

    /** Persist an editor change only after the same strict parser accepts the complete candidate. */
    public static synchronized boolean editTier(int rank, String name, ResourceLocation rarity) {
        Settings s = current;
        List<Tier> tiers = new ArrayList<>(s.tiers());
        int index = -1;
        for (int i = 0; i < tiers.size(); i++) if (tiers.get(i).rank() == rank) index = i;
        if (index < 0) return false;
        Tier old = tiers.get(index);
        tiers.set(index, new Tier(old.rank(), old.id(), name == null ? old.name() : name,
            old.advancements(), rarity == null ? old.apotheosisRarity() : rarity));
        return save(replace(s, tiers, s.weapons()));
    }

    public static synchronized boolean addTier(String id, String name, ResourceLocation rarity) {
        Settings s = current;
        int rank = s.highestTier() + 1;
        List<Tier> tiers = new ArrayList<>(s.tiers());
        tiers.add(new Tier(rank, id, name, List.of(), rarity));
        List<Weapon> weapons = new ArrayList<>(s.weapons());
        for (Category c : weapons.stream().map(Weapon::category).distinct().toList()) {
            Weapon template = weapons.stream().filter(w -> w.category() == c).findFirst().orElseThrow();
            weapons.add(new Weapon(template.id(), c, rank, rank, template.weight()));
        }
        return save(replace(s, tiers, weapons));
    }

    public static synchronized boolean addWeaponToTier(String item, Category category, int rank) {
        Settings s = current;
        List<Weapon> weapons = new ArrayList<>(s.weapons());
        weapons.removeIf(w -> w.category() == category && w.minTier() == rank && w.maxTier() == rank && w.id().equals(item));
        weapons.add(new Weapon(item, category, rank, rank, 1));
        return save(replace(s, s.tiers(), weapons));
    }

    public static synchronized boolean removeWeaponFromTier(String item, Category category, int rank) {
        Settings s = current;
        List<Weapon> weapons = new ArrayList<>(s.weapons());
        boolean removed = weapons.removeIf(w -> w.category() == category && w.minTier() == rank && w.maxTier() == rank && w.id().equals(item));
        return removed && save(replace(s, s.tiers(), weapons));
    }

    private static Settings replace(Settings s, List<Tier> tiers, List<Weapon> weapons) {
        return new Settings(s.mode(), s.shareFtbTeams(), s.enableIdentification(), s.replaceWeaponLoot(),
            s.handheldIdentification(), s.stationIdentification(), List.copyOf(tiers), List.copyOf(weapons), s.costs());
    }

    private static boolean save(Settings candidate) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("progression_mode", candidate.mode().name());
            root.addProperty("share_ftb_teams", candidate.shareFtbTeams());
            root.addProperty("identification_enabled", candidate.enableIdentification());
            root.addProperty("replace_weapon_loot", candidate.replaceWeaponLoot());
            root.addProperty("handheld_identification", candidate.handheldIdentification());
            root.addProperty("station_identification", candidate.stationIdentification());
            JsonObject costs = new JsonObject();
            costs.addProperty("enabled", candidate.costs().enabled()); costs.addProperty("item", candidate.costs().item());
            costs.addProperty("default", candidate.costs().defaultAmount());
            JsonObject perTier = new JsonObject(); candidate.costs().tierAmounts().forEach((k,v) -> perTier.addProperty(k.toString(), v));
            JsonObject perItem = new JsonObject(); candidate.costs().itemAmounts().forEach((k,v) -> perItem.addProperty(k, v));
            costs.add("per_tier", perTier); costs.add("per_item", perItem); root.add("identification_costs", costs);
            JsonArray tierArray = new JsonArray();
            candidate.tiers().forEach(t -> {
                JsonObject o = new JsonObject(); o.addProperty("rank", t.rank()); o.addProperty("id", t.id()); o.addProperty("name", t.name());
                JsonArray adv = new JsonArray(); t.advancements().forEach(a -> adv.add(a.toString()));
                o.add("advancements", adv); o.addProperty("apotheosis_rarity", t.apotheosisRarity().toString()); tierArray.add(o);
            }); root.add("tiers", tierArray);
            JsonArray weaponArray = new JsonArray();
            candidate.weapons().forEach(w -> {
                JsonObject o = new JsonObject(); o.addProperty("item", w.id()); o.addProperty("category", w.category().name().toLowerCase(Locale.ROOT));
                o.addProperty("min_tier", w.minTier()); o.addProperty("max_tier", w.maxTier()); o.addProperty("weight", w.weight()); weaponArray.add(o);
            }); root.add("weapons", weaponArray);
            Settings validated = parse(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Path target = path(); Files.createDirectories(target.toAbsolutePath().getParent());
            Path tmp = target.resolveSibling("tiers.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            current = validated; return true;
        } catch (Exception ex) { TnkLoot.LOG.error("Could not save tier editor changes: {}", ex.getMessage()); return false; }
    }

    public static String defaultJson() throws IOException {
        try (var stream = TnkConfig.class.getResourceAsStream("/default-tiers.json")) {
            if (stream == null) throw new IOException("Bundled default-tiers.json missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String tierSnapshot() {
        JsonArray array = new JsonArray();
        current.tiers().forEach(t -> { JsonObject o = new JsonObject(); o.addProperty("rank", t.rank()); o.addProperty("id", t.id()); o.addProperty("name", t.name()); o.addProperty("apotheosis_rarity", t.apotheosisRarity().toString()); array.add(o); });
        return array.toString();
    }
    public static List<Tier> parseTierSnapshot(String json) {
        List<Tier> result = new ArrayList<>();
        for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            result.add(new Tier(o.get("rank").getAsInt(), o.get("id").getAsString(), o.get("name").getAsString(), List.of(), ResourceLocation.parse(o.get("apotheosis_rarity").getAsString())));
        }
        return List.copyOf(result);
    }

    /** Strict parsing prevents a typo from silently deleting a tier or emptying a loot pool. */
    public static Settings parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        keys(root, "progression_mode", "share_ftb_teams", "identification_enabled", "replace_weapon_loot", "handheld_identification", "station_identification", "identification_costs", "tiers", "weapons");
        Mode mode = Mode.valueOf(string(root, "progression_mode", "ADVANCEMENTS").toUpperCase(Locale.ROOT));
        boolean share = bool(root, "share_ftb_teams", true);
        boolean identification = bool(root, "identification_enabled", true);
        boolean replaceWeaponLoot = bool(root, "replace_weapon_loot", true);
        boolean handheld = bool(root, "handheld_identification", true);
        boolean station = bool(root, "station_identification", true);
        Costs costs = parseCosts(root.has("identification_costs") ? root.getAsJsonObject("identification_costs") : new JsonObject());
        List<Tier> tiers = new ArrayList<>();
        Set<Integer> ranks = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("tiers")) {
            JsonObject e = element.getAsJsonObject();
            keys(e, "rank", "id", "name", "advancements", "apotheosis_rarity");
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
            String rarityValue = string(e, "apotheosis_rarity", defaultApotheosisRarity(rank));
            ResourceLocation apotheosisRarity = ResourceLocation.tryParse(rarityValue);
            if (apotheosisRarity == null) throw new IllegalArgumentException("Invalid Apotheosis rarity ID: " + rarityValue);
            tiers.add(new Tier(rank, id, name, List.copyOf(advancements), apotheosisRarity));
        }
        tiers.sort(Comparator.comparingInt(Tier::rank));
        if (tiers.isEmpty() || tiers.getFirst().rank() != 0) throw new IllegalArgumentException("A rank-0 tier is required");
        for (int rank : costs.tierAmounts().keySet()) if (!ranks.contains(rank))
            throw new IllegalArgumentException("Identification cost references unknown tier rank " + rank);
        List<Weapon> weapons = new ArrayList<>();
        Map<String,Category> itemCategories = new HashMap<>();
        for (JsonElement element : root.getAsJsonArray("weapons")) {
            JsonObject e = element.getAsJsonObject();
            keys(e, "item", "category", "min_tier", "max_tier", "weight");
            String id = string(e, "item", null);
            if (ResourceLocation.tryParse(id) == null) throw new IllegalArgumentException("Invalid item ID: " + id);
            Category category = Category.parse(string(e, "category", "weapon"));
            Category prior = itemCategories.putIfAbsent(id, category);
            if (prior != null && prior != category) throw new IllegalArgumentException("Item is assigned to multiple categories: " + id);
            int min = integer(e, "min_tier", null);
            int max = integer(e, "max_tier", Integer.MAX_VALUE);
            int weight = integer(e, "weight", 1);
            if (min < 0 || max < min || weight < 1) throw new IllegalArgumentException("Invalid tier range or weight: " + e);
            weapons.add(new Weapon(id, category, min, max, weight));
        }
        if (identification) for (Category category : weapons.stream().map(Weapon::category).distinct().toList())
            for (Tier tier : tiers) if (weapons.stream().noneMatch(w -> w.category() == category && tier.rank() >= w.minTier() && tier.rank() <= w.maxTier()))
                throw new IllegalArgumentException("No configured " + category.name().toLowerCase(Locale.ROOT) + " items for tier " + tier.id());
        return new Settings(mode, share, identification, replaceWeaponLoot, handheld, station, List.copyOf(tiers), List.copyOf(weapons), costs);
    }

    private static Costs parseCosts(JsonObject obj) {
        keys(obj, "enabled", "item", "default", "per_tier", "per_item");
        boolean enabled = bool(obj, "enabled", true);
        String item = string(obj, "item", "minecraft:emerald");
        if (ResourceLocation.tryParse(item) == null) throw new IllegalArgumentException("Invalid identification cost item: " + item);
        int fallback = integer(obj, "default", 1);
        if (fallback < 0) throw new IllegalArgumentException("Identification cost cannot be negative");
        Map<Integer,Integer> tiers = new HashMap<>();
        if (obj.has("per_tier")) for (var e : obj.getAsJsonObject("per_tier").entrySet()) {
            int rank = Integer.parseInt(e.getKey()), amount = e.getValue().getAsBigDecimal().intValueExact();
            if (rank < 0 || amount < 0) throw new IllegalArgumentException("Invalid per-tier identification cost");
            tiers.put(rank, amount);
        }
        Map<String,Integer> items = new HashMap<>();
        if (obj.has("per_item")) for (var e : obj.getAsJsonObject("per_item").entrySet()) {
            if (ResourceLocation.tryParse(e.getKey()) == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Invalid per-item identification cost: " + e.getKey());
            int amount = e.getValue().getAsBigDecimal().intValueExact();
            if (amount < 0) throw new IllegalArgumentException("Identification cost cannot be negative");
            items.put(e.getKey(), amount);
        }
        return new Costs(enabled, item, fallback, Map.copyOf(tiers), Map.copyOf(items));
    }

    private static String defaultApotheosisRarity(int rank) {
        return switch (rank) {
            case 0 -> "tnkloot:common";
            case 1 -> "tnkloot:uncommon";
            case 2 -> "tnkloot:rare";
            case 3 -> "tnkloot:epic";
            case 4 -> "tnkloot:legendary";
            case 5, 6 -> "tnkloot:mythic";
            default -> "tnkloot:ancient";
        };
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
