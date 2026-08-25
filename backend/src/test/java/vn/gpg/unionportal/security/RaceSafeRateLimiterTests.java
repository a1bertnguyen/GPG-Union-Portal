package vn.gpg.unionportal.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RaceSafeRateLimiterTests {
    @Test
    void concurrentRequestsCannotConsumeMoreThanCapacity() throws Exception {
        var clock = new AtomicLong();
        var limiter = new RaceSafeRateLimiter(clock::get);
        int capacity = 25;
        int attempts = 500;
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Boolean>>();
        var executor = Executors.newFixedThreadPool(32);

        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return limiter.tryAcquire("same-user", capacity, Duration.ofMinutes(1)).allowed();
                }));
            }
            start.countDown();

            long allowed = 0;
            for (var future : futures) {
                if (future.get()) allowed++;
            }
            assertThat(allowed).isEqualTo(capacity);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void tokenBucketRefillsUsingMonotonicTime() {
        var clock = new AtomicLong();
        var limiter = new RaceSafeRateLimiter(clock::get);
        var window = Duration.ofSeconds(10);

        assertThat(limiter.tryAcquire("user", 2, window).allowed()).isTrue();
        assertThat(limiter.tryAcquire("user", 2, window).allowed()).isTrue();
        var rejected = limiter.tryAcquire("user", 2, window);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(5);

        clock.addAndGet(Duration.ofSeconds(5).toNanos());
        assertThat(limiter.tryAcquire("user", 2, window).allowed()).isTrue();
    }
}
