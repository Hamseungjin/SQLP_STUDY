package com.example.sqlp_hsj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkOrderDataLoader implements ApplicationRunner {

    private static final String[] STATUSES = {"CREATED", "PAID", "SHIPPED", "CANCELLED", "REFUNDED"};

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${sqltuning.practice.mode:none}")
    private String mode;
    @Value("${sqltuning.data-loader.target-row-count:1000000}")
    private int targetRowCount;
    @Value("${sqltuning.data-loader.batch-size:5000}")
    private int batchSize;
    @Value("${sqltuning.data-loader.skip-if-exists:true}")
    private boolean skipIfExists;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!"load-data".equalsIgnoreCase(mode)) {
            return;
        }

        Long current = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        long existing = current == null ? 0L : current;
        if (skipIfExists && existing >= targetRowCount) {
            log.info("Skip bulk insert: existing rows={} >= target={}", existing, targetRowCount);
            return;
        }

        int rowsToInsert = (int) Math.max(0, targetRowCount - existing);
        log.info("Start bulk load. existing={}, target={}, toInsert={}, batchSize={}", existing, targetRowCount, rowsToInsert, batchSize);

        long start = System.nanoTime();
        int inserted = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO orders(order_date, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 1; i <= rowsToInsert; i++) {
                    LocalDateTime orderDate = randomDateInLastThreeYears();
                    ps.setObject(1, orderDate);
                    ps.setLong(2, ThreadLocalRandom.current().nextLong(1, 100_001));
                    ps.setString(3, STATUSES[ThreadLocalRandom.current().nextInt(STATUSES.length)]);
                    ps.setBigDecimal(4, randomAmount());
                    ps.setObject(5, LocalDateTime.now());
                    ps.addBatch();

                    if (i % batchSize == 0) {
                        ps.executeBatch();
                        conn.commit();
                        inserted += batchSize;
                        log.info("Progress: inserted={} / {}", inserted, rowsToInsert);
                    }
                }

                int remaining = rowsToInsert % batchSize;
                if (remaining != 0) {
                    ps.executeBatch();
                    conn.commit();
                    inserted += remaining;
                    log.info("Progress: inserted={} / {}", inserted, rowsToInsert);
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Bulk load complete. inserted={}, elapsed={} ms", inserted, elapsedMs);
    }

    private LocalDateTime randomDateInLastThreeYears() {
        long sec = ThreadLocalRandom.current().nextLong(0, 3L * 365 * 24 * 60 * 60);
        return LocalDateTime.now().minusSeconds(sec);
    }

    private BigDecimal randomAmount() {
        long v = ThreadLocalRandom.current().nextLong(1_000, 1_000_001);
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.UNNECESSARY);
    }
}