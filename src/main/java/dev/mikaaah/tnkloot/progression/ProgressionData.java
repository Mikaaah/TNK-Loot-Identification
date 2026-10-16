package dev.mikaaah.tnkloot.progression;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

import java.util.HashMap;
import java.util.Map;

/** One persistent store for individual and team progress in each world save. */
public final class ProgressionData extends SavedData {
    private static final Factory<ProgressionData> FACTORY = new Factory<>(ProgressionData::new, ProgressionData::load);
    private final Map<String, Integer> ranks = new HashMap<>();

    public static ProgressionData of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "tnkloot_progress");
    }

    public int get(String key) { return ranks.getOrDefault(key, 0); }

    public boolean unlock(String key, int rank) {
        if (rank <= get(key)) return false;
        ranks.put(key, rank);
        setDirty();
        return true;
    }

    public void set(String key, int rank) {
        if (rank == get(key)) return;
        if (rank == 0) ranks.remove(key);
        else ranks.put(key, rank);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag entries = new CompoundTag();
        ranks.forEach(entries::putInt);
        nbt.put("ranks", entries);
        return nbt;
    }

    static ProgressionData load(CompoundTag nbt, HolderLookup.Provider registries) {
        ProgressionData data = new ProgressionData();
        CompoundTag entries = nbt.getCompound("ranks");
        for (String key : entries.getAllKeys()) data.ranks.put(key, Math.max(0, entries.getInt(key)));
        return data;
    }
}