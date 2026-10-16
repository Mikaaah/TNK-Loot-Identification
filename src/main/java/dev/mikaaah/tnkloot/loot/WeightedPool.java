package dev.mikaaah.tnkloot.loot;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Immutable weighted pool with unbiased bounded selection and long totals. */
public final class WeightedPool<T> {
    public record Entry<T>(T value, int weight) {
        public Entry { if (weight < 1) throw new IllegalArgumentException("Weight must be positive"); }
    }
    private final List<Entry<T>> entries;
    private final long total;

    public WeightedPool(List<Entry<T>> entries) {
        this.entries = List.copyOf(entries);
        long sum = 0;
        for (Entry<T> entry : entries) sum = Math.addExact(sum, entry.weight());
        total = sum;
    }
    public boolean isEmpty() { return entries.isEmpty(); }
    public Optional<T> roll(LongSupplier random) {
        if (isEmpty()) return Optional.empty();
        // Rejection avoids modulo bias, including totals larger than Integer.MAX_VALUE.
        long bits, ticket;
        do {
            bits = random.getAsLong() >>> 1;
            ticket = bits % total;
        } while (bits - ticket + (total - 1) < 0L);
        return Optional.of(at(ticket));
    }
    T at(long ticket) {
        if (ticket < 0 || ticket >= total) throw new IllegalArgumentException("Ticket outside pool");
        for (Entry<T> entry : entries) {
            if (ticket < entry.weight()) return entry.value();
            ticket -= entry.weight();
        }
        throw new IllegalStateException("Weighted pool exhausted");
    }
}