package dev.mikaaah.tnkloot.progression;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionDataTest {
    @Test void unlockIsMonotonicAndIndependentByIdentity() {
        var data = new ProgressionData();
        assertEquals(0, data.get("player:a"));
        assertTrue(data.unlock("player:a", 4));
        assertFalse(data.unlock("player:a", 4));
        assertFalse(data.unlock("player:a", 2));
        assertEquals(4, data.get("player:a"));
        assertEquals(0, data.get("team:a"));
        data.set("player:a", 0);
        assertEquals(0, data.get("player:a"));
    }
    @Test void saveLoadRoundTripPreservesPlayerAndTeam() {
        var original = new ProgressionData();
        original.unlock("player:a", 5);
        original.unlock("team:party", 7);
        var restored = ProgressionData.load(original.save(new CompoundTag(), null), null);
        assertEquals(5, restored.get("player:a"));
        assertEquals(7, restored.get("team:party"));
        assertEquals(0, restored.get("player:new"));
    }
    @Test void corruptedNegativeProgressLoadsAsZero() {
        var tag = new CompoundTag();
        var entries = new CompoundTag();
        entries.putInt("player:a", -20);
        tag.put("ranks", entries);
        assertEquals(0, ProgressionData.load(tag, null).get("player:a"));
    }
}