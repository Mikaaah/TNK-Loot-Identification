package dev.mikaaah.tnkloot.loot;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class WeightedPoolTest {
    @Test void exactWeightBoundaries() {
        var pool = new WeightedPool<>(List.of(new WeightedPool.Entry<>("wood", 5), new WeightedPool.Entry<>("stone", 2)));
        assertEquals("wood", pool.at(0));
        assertEquals("wood", pool.at(4));
        assertEquals("stone", pool.at(5));
        assertEquals("stone", pool.at(6));
        assertThrows(IllegalArgumentException.class, () -> pool.at(7));
    }
    @Test void emptyPoolDoesNotDrawRandom() {
        assertTrue(new WeightedPool<>(List.of()).roll(() -> { throw new AssertionError(); }).isEmpty());
    }
    @Test void largeWeightsDoNotOverflow() {
        var pool = new WeightedPool<>(List.of(new WeightedPool.Entry<>("a", Integer.MAX_VALUE), new WeightedPool.Entry<>("b", Integer.MAX_VALUE)));
        assertEquals("b", pool.at(4294967293L));
        assertThrows(IllegalArgumentException.class, () -> new WeightedPool.Entry<>("bad", 0));
    }
    @Test void seededWeightedDistribution() {
        var pool = new WeightedPool<>(List.of(new WeightedPool.Entry<>("a", 3), new WeightedPool.Entry<>("b", 1)));
        Random random = new Random(1234);
        int hits = 0;
        for (int i = 0; i < 100000; i++) if (pool.roll(random::nextLong).orElseThrow().equals("a")) hits++;
        assertTrue(hits > 74000 && hits < 76000, "hits=" + hits);
    }
    @Test void rejectionRetriesOutOfRangeRandomValue() {
        var pool = new WeightedPool<>(List.of(new WeightedPool.Entry<>("a", 3)));
        long[] random = {-1L, 0L};
        int[] index = {0};
        assertEquals("a", pool.roll(() -> random[index[0]++]).orElseThrow());
        assertEquals(2, index[0]);
    }
}