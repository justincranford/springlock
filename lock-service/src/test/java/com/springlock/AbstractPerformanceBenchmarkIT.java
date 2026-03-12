package com.springlock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.springlock.lock.LockService;

/**
 * Dedicated performance benchmark producing CSV + Markdown reports.
 *
 * <p>Runs warm-up rounds followed by measured rounds for sequential
 * and concurrent acquire/release operations, collecting timing data
 * for statistical analysis.
 */
@SpringBootTest
@Tag("integration")
public abstract class AbstractPerformanceBenchmarkIT {

    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURED_ROUNDS = 10;
    private static final int SEQUENTIAL_OPS = 200;
    private static final int CONCURRENT_THREADS = 20;
    private static final int CONCURRENT_ITERS_PER_THREAD = 50;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String OWNER = "bench-owner";

    private static final List<String[]> CSV_ROWS = new ArrayList<>();
    private static String resolvedLabel;

    protected abstract String backendLabel();

    @Autowired
    protected LockService lockService;

    @BeforeEach
    void cleanup() {
        lockService.releaseLock("bench-seq", OWNER);
        lockService.releaseLock("bench-conc", OWNER);
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            lockService.releaseLock("bench-conc", "t-" + i);
        }
    }

    @Test
    void benchmark_sequentialThroughput() {
        resolvedLabel = backendLabel();
        String key = "bench-seq-" + UUID.randomUUID();

        for (int w = 0; w < WARMUP_ROUNDS; w++) {
            runSequential(key);
            lockService.releaseLock(key, OWNER);
        }

        double[] opsPerSec = new double[MEASURED_ROUNDS];
        long[] elapsedMs = new long[MEASURED_ROUNDS];
        for (int r = 0; r < MEASURED_ROUNDS; r++) {
            long start = System.nanoTime();
            runSequential(key);
            long elapsed = System.nanoTime() - start;
            elapsedMs[r] = elapsed / 1_000_000;
            opsPerSec[r] = SEQUENTIAL_OPS * 1_000_000_000.0 / elapsed;
            lockService.releaseLock(key, OWNER);
        }

        for (int r = 0; r < MEASURED_ROUNDS; r++) {
            CSV_ROWS.add(new String[]{
                backendLabel(), "sequential", String.valueOf(r + 1),
                String.valueOf(SEQUENTIAL_OPS), String.valueOf(elapsedMs[r]),
                String.format("%.1f", opsPerSec[r])
            });
        }

        double mean = mean(opsPerSec);
        double stddev = stddev(opsPerSec, mean);
        System.out.printf("[%s] sequential throughput: %.0f +/- %.0f ops/sec "
            + "(min=%.0f, max=%.0f, n=%d)%n",
            backendLabel(), mean, stddev,
            Arrays.stream(opsPerSec).min().orElse(0),
            Arrays.stream(opsPerSec).max().orElse(0),
            MEASURED_ROUNDS);
    }

    @Test
    void benchmark_concurrentThroughput() throws Exception {
        resolvedLabel = backendLabel();
        String key = "bench-conc";
        int totalOps = CONCURRENT_THREADS * CONCURRENT_ITERS_PER_THREAD;

        for (int w = 0; w < WARMUP_ROUNDS; w++) {
            runConcurrent(key);
        }

        double[] opsPerSec = new double[MEASURED_ROUNDS];
        long[] elapsedMs = new long[MEASURED_ROUNDS];
        for (int r = 0; r < MEASURED_ROUNDS; r++) {
            long start = System.nanoTime();
            int acquired = runConcurrent(key);
            long elapsed = System.nanoTime() - start;
            elapsedMs[r] = elapsed / 1_000_000;
            opsPerSec[r] = totalOps * 1_000_000_000.0 / elapsed;
        }

        for (int r = 0; r < MEASURED_ROUNDS; r++) {
            CSV_ROWS.add(new String[]{
                backendLabel(), "concurrent-" + CONCURRENT_THREADS + "t",
                String.valueOf(r + 1), String.valueOf(totalOps),
                String.valueOf(elapsedMs[r]),
                String.format("%.1f", opsPerSec[r])
            });
        }

        double mean = mean(opsPerSec);
        double stddev = stddev(opsPerSec, mean);
        System.out.printf("[%s] concurrent throughput (%d threads): "
            + "%.0f +/- %.0f ops/sec (min=%.0f, max=%.0f, n=%d)%n",
            backendLabel(), CONCURRENT_THREADS,
            mean, stddev,
            Arrays.stream(opsPerSec).min().orElse(0),
            Arrays.stream(opsPerSec).max().orElse(0),
            MEASURED_ROUNDS);
    }

    @AfterAll
    static void writeReport() throws IOException {
        if (CSV_ROWS.isEmpty()) return;
        Path buildDir = Path.of("build", "reports", "perf");
        Files.createDirectories(buildDir);

        String label = resolvedLabel != null ? resolvedLabel : "unknown";
        Path csv = buildDir.resolve("perf-" + label + ".csv");
        try (PrintWriter w = new PrintWriter(
                Files.newBufferedWriter(csv, StandardCharsets.UTF_8))) {
            w.println("backend,test_type,round,total_ops,elapsed_ms,ops_per_sec");
            for (String[] row : CSV_ROWS) {
                w.println(String.join(",", row));
            }
        }
        System.out.printf("[%s] CSV report written to %s (%d rows)%n",
            label, csv.toAbsolutePath(), CSV_ROWS.size());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void runSequential(String key) {
        for (int i = 0; i < SEQUENTIAL_OPS; i++) {
            boolean acquired = lockService.acquireLock(key, OWNER, TTL);
            if (acquired) {
                lockService.releaseLock(key, OWNER);
            }
        }
    }

    private int runConcurrent(String key) throws Exception {
        AtomicInteger acquired = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            String owner = "t-" + t;
            pool.submit(() -> {
                try {
                    go.await();
                    for (int i = 0; i < CONCURRENT_ITERS_PER_THREAD; i++) {
                        try {
                            if (lockService.acquireLock(key, owner, TTL)) {
                                acquired.incrementAndGet();
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
        go.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(errors.get()).isZero();
        return acquired.get();
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double stddev(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }
}