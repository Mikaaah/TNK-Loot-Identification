package dev.mikaaah.tnkloot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;
import static org.junit.jupiter.api.Assertions.*;

class TnkConfigTest {
    @TempDir Path directory;
    private String defaults() throws Exception { return TnkConfig.defaultJson(); }

    @Test void bundledDefaultsHaveEightCoveredTiers() throws Exception {
        var cfg = TnkConfig.parse(defaults());
        assertEquals(8, cfg.tiers().size());
        assertEquals(7, cfg.highestTier());
        assertEquals("novice", cfg.tierForRank(-1).id());
        assertEquals("legendary", cfg.tierForRank(100).id());
        assertEquals(3, cfg.tierForName("EXPLORER").orElseThrow().rank());
        assertEquals("tnkloot:ancient", cfg.tierForName("legendary").orElseThrow().apotheosisRarity().toString());
        assertTrue(cfg.replaceWeaponLoot());
        assertTrue(cfg.handheldIdentification());
        assertTrue(cfg.stationIdentification());
        assertEquals(TnkConfig.Category.WEAPON, cfg.weapons().getFirst().category());
        assertEquals(25, cfg.costs().amount(7, "minecraft:diamond_sword"));
        assertThrows(UnsupportedOperationException.class, () -> cfg.weapons().clear());
    }
    @Test void rejectsBrokenEntriesAndTypos() throws Exception {
        String base = defaults();
        for (String bad : new String[] {
            base.replace("\"rank\": 1", "\"rank\": 0"),
            base.replace("\"id\": \"apprentice\"", "\"id\": \"novice\""),
            base.replace("\"weight\": 5", "\"weight\": 0"),
            base.replace("\"weight\": 5", "\"weight\": 1.5"),
            base.replace("\"weight\": 5", "\"weight\": 4294967296"),
            base.replace("\"weight\": 5", "\"weight\": \"5\""),
            base.replace("\"share_ftb_teams\": true", "\"share_ftb_teams\": \"true\""),
            base.replace("minecraft:wooden_sword", "Bad ID!"),
            base.replace("minecraft:story/upgrade_tools", "Bad ID!"),
            base.replace("\"weight\"", "\"wieght\""),
            base.replace("\"rank\": 0", "\"rank\": -1"),
            base.replace("\"progression_mode\": \"ADVANCEMENTS\"", "\"progression_mode\": \"UNKNOWN\"")
        }) assertThrows(RuntimeException.class, () -> TnkConfig.parse(bad), bad);
    }
    @Test void reloadIsAtomicAndGeneratesDefaults() throws Exception {
        Path file = directory.resolve("nested/tiers.json");
        assertTrue(TnkConfig.reload(file));
        var previous = TnkConfig.get();
        Files.writeString(file, defaults().replace("\"weight\": 5", "\"weight\": -1"));
        assertFalse(TnkConfig.reload(file));
        assertSame(previous, TnkConfig.get());
        Files.writeString(file, "{broken");
        assertFalse(TnkConfig.reload(file));
        assertSame(previous, TnkConfig.get());
    }
    @Test void modesAndOptionalItemsRequireNoFtbDependency() throws Exception {
        for (var mode : TnkConfig.Mode.values()) {
            var cfg = TnkConfig.parse(defaults().replace("ADVANCEMENTS", mode.name()).replace("minecraft:wooden_sword", "optionalmod:blade"));
            assertEquals(mode, cfg.mode());
        }
    }
    @Test void rejectsUncoveredTierUnlessIdentificationDisabled() throws Exception {
        String config = """
            {"tiers":[{"rank":0,"id":"start"},{"rank":10,"id":"end"}],
             "weapons":[{"item":"minecraft:wooden_sword","min_tier":0,"max_tier":0}]}
            """;
        assertThrows(IllegalArgumentException.class, () -> TnkConfig.parse(config));
        var disabled = TnkConfig.parse(config.replace("{\"tiers\"", "{\"identification_enabled\":false,\"tiers\""));
        assertEquals(0, disabled.tierForRank(8).rank());
        assertEquals(10, disabled.tierForRank(999).rank());
    }

    @Test void lootReplacementCanBeDisabledAndRarityIdsAreValidated() throws Exception {
        String config = defaults().replace("\"replace_weapon_loot\": true", "\"replace_weapon_loot\": false");
        assertFalse(TnkConfig.parse(config).replaceWeaponLoot());
        assertThrows(IllegalArgumentException.class, () -> TnkConfig.parse(
            config.replace("tnkloot:common", "bad rarity id")));
    }

    @Test void customApotheosisRaritiesArePresentAndProgressionOrdered() throws Exception {
        String[] ladder = {"common", "uncommon", "rare", "epic", "legendary", "mythic", "ancient"};
        for (int i = 0; i < ladder.length; i++) {
            var stream = getClass().getResourceAsStream("/data/tnkloot/rarities/" + ladder[i] + ".json");
            assertNotNull(stream, ladder[i]);
            try (stream) {
                var json = JsonParser.parseString(new String(stream.readAllBytes())).getAsJsonObject();
                assertEquals("apotheosis:rarity", json.get("type").getAsString());
                assertFalse(json.getAsJsonArray("rules").isEmpty());
                assertTrue(json.getAsJsonObject("weights").entrySet().stream()
                    .allMatch(e -> e.getValue().getAsJsonObject().get("weight").getAsInt() == 0));
            }
        }
        var cfg = TnkConfig.parse(defaults());
        assertEquals("tnkloot:common", cfg.tierForRank(0).apotheosisRarity().toString());
        assertEquals("tnkloot:ancient", cfg.tierForRank(7).apotheosisRarity().toString());
    }

    @Test void costsSupportTierDefaultsAndPerItemOverrides() throws Exception {
        String config = defaults().replace("\"per_item\": {}", "\"per_item\": {\"minecraft:wooden_sword\": 4}");
        var costs = TnkConfig.parse(config).costs();
        assertEquals(1, costs.amount(0, "minecraft:iron_sword"));
        assertEquals(4, costs.amount(0, "minecraft:wooden_sword"));
        assertThrows(IllegalArgumentException.class, () -> TnkConfig.parse(config.replace("\"minecraft:wooden_sword\": 4", "\"bad id\": 4")));
        assertThrows(RuntimeException.class, () -> TnkConfig.parse(config.replace("\"minecraft:wooden_sword\": 4", "\"minecraft:wooden_sword\": -4")));
    }

    @Test void everyConfiguredTokenCategoryHasPoolCoveragePerTier() throws Exception {
        String config = """
            {"tiers":[{"rank":0,"id":"start"},{"rank":1,"id":"next"}],
             "weapons":[{"item":"minecraft:leather_helmet","category":"helmet","min_tier":0,"max_tier":0}]}
            """;
        assertThrows(IllegalArgumentException.class, () -> TnkConfig.parse(config));
    }
}
