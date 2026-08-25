package vn.gpg.unionportal.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * In-memory token bucket. Updating a bucket through ConcurrentHashMap.compute makes
 * refill and consume one atomic operation for every identity, avoiding check-then-act races.
 */
public class RaceSafeRateLimiter {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final LongSupplier nanoTime;

    public RaceSafeRateLimiter() {
        this(System::nanoTime);
    }

    public RaceSafeRateLimiter(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public Decision tryAcquire(String key, int configuredCapacity, Duration configuredWindow) {
        int capacity = Math.max(1, configuredCapacity);
        long windowNanos = Math.max(1, configuredWindow.toNanos());
        long now = nanoTime.getAsLong();
        double tokensPerNano = capacity / (double) windowNanos;
        var decision = new AtomicReference<Decision>();

        buckets.compute(key, (ignored, current) -> {
            double available = capacity;
            if (current != null) {
                long elapsed = Math.max(0, now - current.lastRefillNanos());
                available = Math.min(capacity, current.tokens() + elapsed * tokensPerNano);
            }

            boolean allowed = available >= 1d;
            double remainingTokens = allowed ? available - 1d : available;
            long retryAfterSeconds = allowed ? 0 : Math.max(1,
                    (long) Math.ceil(((1d - remainingTokens) / tokensPerNano) / 1_000_000_000d));
            decision.set(new Decision(allowed, capacity, (int) Math.floor(remainingTokens), retryAfterSeconds));
            return new Bucket(remainingTokens, now, now);
        });

        if ((operations.incrementAndGet() & 1023L) == 0) {
            removeIdleBuckets(now, windowNanos * 2);
        }
        return decision.get();
    }

    private void removeIdleBuckets(long now, long maxIdleNanos) {
        buckets.forEach((key, bucket) -> {
            if (now - bucket.lastSeenNanos() > maxIdleNanos) {
                buckets.remove(key, bucket);
            }
        });
    }

    private record Bucket(double tokens, long lastRefillNanos, long lastSeenNanos) {
    }

    public record Decision(boolean allowed, int limit, int remaining, long retryAfterSeconds) {
    }
}
