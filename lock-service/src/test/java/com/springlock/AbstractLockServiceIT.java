package com.springlock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.springlock.lock.LockService;
import com.springlock.lock.model.LockInfo;

@SpringBootTest
public abstract class AbstractLockServiceIT {

    // ── Constants ──────────────────────────────────────────────────────────────
    protected static final String OWNER_A = "owner-A";
    protected static final String OWNER_B = "owner-B";
    protected static final Duration SHORT_TTL = Duration.ofSeconds(2);
    protected static final Duration LONG_TTL  = Duration.ofMinutes(5);

    private static final int CONCURRENCY_THREADS = 20;
    private static final int STRESS_ITERATIONS   = 50;
    private static final long PERF_WARN_MS       = 200;

    /** Backend label shown in perf logs, e.g. "postgres17-jpa". Override per subclass. */
    protected String backendLabel() { return getClass().getSimpleName(); }

    @Autowired
    protected LockService lockService;

    // ── Setup / Teardown ───────────────────────────────────────────────────────
    @BeforeEach
    void cleanupBefore() { cleanupStandardKeys(); }

    @AfterEach
    void cleanupAfter() { cleanupStandardKeys(); }

    private void cleanupStandardKeys() {
        for (String key : new String[]{"test-lock", "stress-lock", "renew-lock"}) {
            lockService.releaseLock(key, OWNER_A);
            lockService.releaseLock(key, OWNER_B);
            for (int i = 0; i < CONCURRENCY_THREADS; i++) {
                lockService.releaseLock(key, "thread-" + i);
                lockService.releaseLock(key, "stress-owner-" + i);
            }
        }
    }

    // ── Happy-path parameterized: multiple lock keys ───────────────────────────
    static Stream<Arguments> lockKeyVariants() {
        return Stream.of(
            Arguments.of("simple-key"),
            Arguments.of("key/with/slashes"),
            Arguments.of("key:with:colons"),
            Arguments.of("key-" + UUID.randomUUID()),
            Arguments.of("a".repeat(200))
        );
    }

    @ParameterizedTest(name = "[happy] acquire+release key={0}")
    @MethodSource("lockKeyVariants")
    void happyPath_acquireAndRelease(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.isLocked(key)).isTrue();
            assertThat(lockService.releaseLock(key, OWNER_A)).isTrue();
            assertThat(lockService.isLocked(key)).isFalse();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("happyPath_acquireAndRelease", start);
    }

    @ParameterizedTest(name = "[happy] renew key={0}")
    @MethodSource("lockKeyVariants")
    void happyPath_renewLock(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, SHORT_TTL)).isTrue();
            assertThat(lockService.renewLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.isLocked(key)).isTrue();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("happyPath_renew", start);
    }

    // ── Happy-path: TTL variant tests ──────────────────────────────────────────
    @ParameterizedTest(name = "[happy] acquire with ttl={0}s")
    @ValueSource(longs = {1, 5, 60, 300, 3600})
    void happyPath_variousTtls(long ttlSeconds) {
        String key = "ttl-test-" + ttlSeconds;
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, Duration.ofSeconds(ttlSeconds))).isTrue();
            assertThat(lockService.isLocked(key)).isTrue();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("happyPath_ttl_" + ttlSeconds, start);
    }

    // ── Sad-path parameterized ─────────────────────────────────────────────────
    @ParameterizedTest(name = "[sad] double-acquire same owner key={0}")
    @MethodSource("lockKeyVariants")
    void sadPath_doubleAcquireSameOwner(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isFalse();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("sadPath_doubleAcquire", start);
    }

    @ParameterizedTest(name = "[sad] acquire by B when held by A key={0}")
    @MethodSource("lockKeyVariants")
    void sadPath_acquireByOtherOwner(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.acquireLock(key, OWNER_B, LONG_TTL)).isFalse();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("sadPath_otherOwner", start);
    }

    @ParameterizedTest(name = "[sad] release by non-owner key={0}")
    @MethodSource("lockKeyVariants")
    void sadPath_releaseByNonOwner(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.releaseLock(key, OWNER_B)).isFalse();
            assertThat(lockService.isLocked(key)).isTrue();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("sadPath_releaseNonOwner", start);
    }

    @ParameterizedTest(name = "[sad] renew by non-owner key={0}")
    @MethodSource("lockKeyVariants")
    void sadPath_renewByNonOwner(String key) {
        long start = System.currentTimeMillis();
        try {
            assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
            assertThat(lockService.renewLock(key, OWNER_B, LONG_TTL)).isFalse();
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        logPerf("sadPath_renewNonOwner", start);
    }

    @ParameterizedTest(name = "[sad] release when not held key={0}")
    @MethodSource("lockKeyVariants")
    void sadPath_releaseNotHeld(String key) {
        assertThat(lockService.releaseLock(key, OWNER_A)).isFalse();
        assertThat(lockService.isLocked(key)).isFalse();
    }

    // ── Standard functional tests ──────────────────────────────────────────────
    @Test
    void acquireLock_whenNotHeld_shouldSucceed() {
        assertThat(lockService.acquireLock("test-lock", OWNER_A, LONG_TTL)).isTrue();
    }

    @Test
    void acquireLock_afterRelease_shouldSucceedForNewOwner() {
        lockService.acquireLock("test-lock", OWNER_A, LONG_TTL);
        lockService.releaseLock("test-lock", OWNER_A);
        assertThat(lockService.acquireLock("test-lock", OWNER_B, LONG_TTL)).isTrue();
        lockService.releaseLock("test-lock", OWNER_B);
    }

    @Test
    void isLocked_afterRelease_shouldReturnFalse() {
        lockService.acquireLock("test-lock", OWNER_A, LONG_TTL);
        lockService.releaseLock("test-lock", OWNER_A);
        assertThat(lockService.isLocked("test-lock")).isFalse();
    }

    // ── Concurrency test: mutual exclusion under race ──────────────────────────
    @Test
    @DisplayName("[concurrency] only one thread acquires a lock simultaneously")
    void concurrency_mutualExclusion() throws Exception {
        String key = "stress-lock";
        int threads = CONCURRENCY_THREADS;
        AtomicInteger acquireCount = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        java.util.concurrent.CopyOnWriteArrayList<String> acquiredOwners = new java.util.concurrent.CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final String owner = "thread-" + i;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    if (lockService.acquireLock(key, owner, LONG_TTL)) {
                        acquireCount.incrementAndGet();
                        acquiredOwners.add(owner);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }));
        }

        long startMs = System.currentTimeMillis();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        for (String owner : acquiredOwners) {
            lockService.releaseLock(key, owner);
        }

        assertThat(acquireCount.get())
            .as("Exactly one thread should acquire a fresh lock")
            .isEqualTo(1);
        logPerf("[concurrency] mutualExclusion " + threads + " threads", startMs);
    }

    // ── Stress test: acquire-release loop ─────────────────────────────────────
    @Test
    @DisplayName("[stress] acquire/release loop under concurrent load")
    void stress_acquireReleaseLoop() throws Exception {
        String key = "stress-lock";
        int threads = CONCURRENCY_THREADS;
        int itersPerThread = STRESS_ITERATIONS;
        AtomicInteger successfulAcquires = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long startMs = System.currentTimeMillis();
        for (int t = 0; t < threads; t++) {
            String owner = "stress-owner-" + t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < itersPerThread; i++) {
                        try {
                            if (lockService.acquireLock(key, owner, LONG_TTL)) {
                                successfulAcquires.incrementAndGet();
                                lockService.releaseLock(key, owner);
                            }
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(120, TimeUnit.SECONDS)).as("stress test must finish within 120s").isTrue();
        pool.shutdown();

        long elapsed = System.currentTimeMillis() - startMs;
        int total = threads * itersPerThread;
        System.out.printf("[%s] stress: %d threads x %d iters = %d ops in %dms (%.0f ops/sec), "
            + "acquired=%d, errors=%d%n",
            backendLabel(), threads, itersPerThread, total, elapsed,
            total * 1000.0 / elapsed, successfulAcquires.get(), errors.get());

        assertThat(errors.get()).as("zero errors during stress").isEqualTo(0);
        assertThat(successfulAcquires.get()).as("at least one acquire succeeded").isGreaterThan(0);
    }

    // ── Performance baseline: sequential acquire+release throughput ────────────
    @Test
    @DisplayName("[perf] sequential acquire/release throughput")
    void perf_sequentialThroughput() {
        int ops = STRESS_ITERATIONS * 2;
        String key = "perf-seq-" + UUID.randomUUID();
        long start = System.currentTimeMillis();
        try {
            for (int i = 0; i < ops; i++) {
                boolean acquired = lockService.acquireLock(key, OWNER_A, LONG_TTL);
                if (acquired) {
                    lockService.releaseLock(key, OWNER_A);
                }
            }
        } finally {
            lockService.releaseLock(key, OWNER_A);
        }
        long elapsed = System.currentTimeMillis() - start;
        double opsPerSec = ops * 1000.0 / elapsed;
        System.out.printf("[%s] sequential: %d acquire+release ops in %dms = %.0f ops/sec%n",
            backendLabel(), ops, elapsed, opsPerSec);
        assertThat(elapsed).as("sequential " + ops + " ops must complete in 60s").isLessThan(60_000);
    }

    // ── Robustness: concurrent renew under load ────────────────────────────────
    @Test
    @DisplayName("[robustness] concurrent renew attempts do not corrupt lock")
    void robustness_concurrentRenew() throws Exception {
        String key = "renew-lock";
        lockService.acquireLock(key, OWNER_A, LONG_TTL);

        int threads = CONCURRENCY_THREADS;
        AtomicInteger renewCount = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (lockService.renewLock(key, OWNER_A, LONG_TTL)) {
                        renewCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        long startMs = System.currentTimeMillis();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(lockService.isLocked(key)).as("lock must still be held after concurrent renewals").isTrue();
        lockService.releaseLock(key, OWNER_A);
        logPerf("[robustness] concurrentRenew " + threads + " threads", startMs);
    }

    // ── Resilience: isLocked returns false after expired TTL (short TTL test) ──
    @Test
    @DisplayName("[resilience] lock expires via TTL")
    void resilience_lockExpiresAfterTtl() throws Exception {
        String key = "expire-" + UUID.randomUUID();
        assertThat(lockService.acquireLock(key, OWNER_A, Duration.ofMillis(500))).isTrue();
        Thread.sleep(1500);
        // After expiry a new owner should be able to acquire
        boolean acquired = lockService.acquireLock(key, OWNER_B, LONG_TTL);
        try {
            assertThat(acquired).as("new owner should acquire after TTL expiry").isTrue();
        } finally {
            lockService.releaseLock(key, OWNER_B);
            lockService.releaseLock(key, OWNER_A);
        }
    }

    private void logPerf(String label, long startMs) {
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > PERF_WARN_MS) {
            System.out.printf("[%s] SLOW(%dms): %s%n", backendLabel(), elapsed, label);
        }
    }

    // ── Helper for subclasses running version-tracking specific tests ──────────
    @Test
    @DisplayName("[functional] acquire when not held")
    void functional_acquireWhenNotHeld() {
        assertThat(lockService.acquireLock("test-lock", OWNER_A, LONG_TTL)).isTrue();
    }

    @Test
    @DisplayName("[functional] is-locked when held")
    void functional_isLockedWhenHeld() {
        lockService.acquireLock("test-lock", OWNER_A, LONG_TTL);
        assertThat(lockService.isLocked("test-lock")).isTrue();
    }

    @Test
    @DisplayName("[functional] is-locked when not held")
    void functional_isLockedWhenNotHeld() {
        assertThat(lockService.isLocked("test-lock")).isFalse();
    }

    static Instant now() { return Instant.now(); }

    // ── findLock tests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("[functional] findLock returns info when lock is held")
    void findLock_whenLocked_returnsLockInfo() {
        lockService.acquireLock("test-lock", OWNER_A, LONG_TTL);
        Optional<LockInfo> info = lockService.findLock("test-lock");
        assertThat(info).isPresent();
        assertThat(info.get().lockKey()).isEqualTo("test-lock");
        assertThat(info.get().owner()).isEqualTo(OWNER_A);
        assertThat(info.get().expiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("[functional] findLock returns empty when not locked")
    void findLock_whenNotLocked_returnsEmpty() {
        assertThat(lockService.findLock("test-lock")).isEmpty();
    }
}
