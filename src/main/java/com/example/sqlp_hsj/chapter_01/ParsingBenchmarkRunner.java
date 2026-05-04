package com.example.sqlp_hsj.chapter_01;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParsingBenchmarkRunner implements ApplicationRunner {

    private static final String[] STATUSES = {"CREATED", "PAID", "SHIPPED", "CANCELLED", "REFUNDED"};
    private final JdbcTemplate jdbcTemplate;

    @Value("${sqltuning.practice.mode:none}")
    private String mode;
    @Value("${sqltuning.parse-benchmark.iterations:12000}")
    private int iterations;

    @Override
    public void run(ApplicationArguments args) {
        if (!"parse-benchmark".equalsIgnoreCase(mode)) return;

        benchmarkLiteralSql();
        benchmarkBindSql();
        printDigestGuide();
    }

    private void benchmarkLiteralSql() {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long customerId = ThreadLocalRandom.current().nextLong(1, 100_001);
            String status = STATUSES[ThreadLocalRandom.current().nextInt(STATUSES.length)];
            String sql = "SELECT COUNT(*) FROM orders WHERE customer_id = " + customerId + " AND status = '" + status + "'";
            jdbcTemplate.queryForObject(sql, Long.class);
        }
        printStats("Literal SQL", start);
    }

    private void benchmarkBindSql() {
        long start = System.nanoTime();
        String sql = "SELECT COUNT(*) FROM orders WHERE customer_id = ? AND status = ?";
        for (int i = 0; i < iterations; i++) {
            long customerId = ThreadLocalRandom.current().nextLong(1, 100_001);
            String status = STATUSES[ThreadLocalRandom.current().nextInt(STATUSES.length)];
            jdbcTemplate.queryForObject(sql, Long.class, customerId, status);
        }
        printStats("Bind SQL", start);
    }

    private void printStats(String label, long start) {
        long elapsedNanos = System.nanoTime() - start;
        double elapsedMs = elapsedNanos / 1_000_000.0;
        double avgMs = elapsedMs / iterations;
        double tps = iterations / (elapsedNanos / 1_000_000_000.0);
        log.info("{} -> total={} ms, avg={} ms, throughput={} qps", label, String.format("%.2f", elapsedMs), String.format("%.4f", avgMs), String.format("%.2f", tps));
    }

    private void printDigestGuide() {
        log.info("Run this query for digest stats:\nSELECT digest_text, count_star, avg_timer_wait/1000000000 avg_ms FROM performance_schema.events_statements_summary_by_digest WHERE digest_text LIKE 'SELECT COUNT(*) FROM orders WHERE customer_id = %' ORDER BY count_star DESC LIMIT 10;");
    }
}