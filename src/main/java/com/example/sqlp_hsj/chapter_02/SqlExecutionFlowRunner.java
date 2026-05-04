package com.example.sqlp_hsj.chapter_02;

import com.example.sqlp_hsj.ExplainPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqlExecutionFlowRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final ExplainPlanService explainPlanService;

    @Value("${sqltuning.practice.mode:none}")
    private String mode;
    @Value("${sqltuning.fetch-size.small:10}")
    private int smallFetchSize;
    @Value("${sqltuning.fetch-size.large:1000}")
    private int largeFetchSize;
    @Value("${sqltuning.fetch-size.limit:200000}")
    private int fetchLimit;
    @Value("${sqltuning.io-compare.amount-threshold:10000}")
    private int amountThreshold;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if ("fetch-size".equalsIgnoreCase(mode)) {
            runFetchSizeExperiment();
            return;
        }

        if ("io-compare".equalsIgnoreCase(mode)) {
            runIoCompareExperiment();
        }
    }

    private void runFetchSizeExperiment() throws Exception {
        String sql = "SELECT order_id, order_date, customer_id, status, amount FROM orders ORDER BY order_id LIMIT ?";
        log.info("Fetch benchmark start. limit={}, smallFetchSize={}, largeFetchSize={}", fetchLimit, smallFetchSize, largeFetchSize);

        long smallMs = measureSelectAndFetch(sql, smallFetchSize, fetchLimit);
        long largeMs = measureSelectAndFetch(sql, largeFetchSize, fetchLimit);

        log.info("Fetch benchmark done. smallFetch={} ms, largeFetch={} ms", smallMs, largeMs);
        log.info("Interpretation: smaller fetchSize generally increases fetch calls and network round trips, while larger fetchSize lowers round trips but may increase memory usage per call.");
    }

    private long measureSelectAndFetch(String sql, int fetchSize, int limit) throws Exception {
        long start = System.nanoTime();
        int rows = 0;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setFetchSize(fetchSize);
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getLong("order_id");
                    rs.getObject("order_date");
                    rs.getLong("customer_id");
                    rs.getString("status");
                    rs.getBigDecimal("amount");
                    rows++;
                }
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("fetchSize={} -> fetchedRows={}, elapsed={} ms", fetchSize, rows, elapsedMs);
        return elapsedMs;
    }

    private void runIoCompareExperiment() throws Exception {
        String fullScanSql = "SELECT * FROM orders WHERE amount > ?";
        LocalDate day = LocalDate.now().minusDays(1);
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        String rangeScanSql = "SELECT * FROM orders WHERE order_date >= ? AND order_date < ?";

        log.info("--- Full Table Scan likely query ---");
        explainPlanService.explain(fullScanSql, amountThreshold);
        explainPlanService.explainAnalyze(fullScanSql, amountThreshold);

        log.info("--- Index Range Scan likely query ---");
        explainPlanService.explain(rangeScanSql, from, to);
        explainPlanService.explainAnalyze(rangeScanSql, from, to);

        long first = timedSelect(rangeScanSql, from, to);
        long second = timedSelect(rangeScanSql, from, to);
        log.info("Buffer-cache effect sample (same query twice): first={} ms, second={} ms", first, second);

        printHandlerStatusSnapshot();
    }

    private long timedSelect(String sql, Object... params) throws Exception {
        long start = System.nanoTime();
        int count = 0;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                }
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("timedSelect rows={}, elapsed={} ms", count, elapsedMs);
        return elapsedMs;
    }

    private void printHandlerStatusSnapshot() {
        String[] metrics = {
                "Handler_read_first",
                "Handler_read_key",
                "Handler_read_next",
                "Handler_read_rnd",
                "Handler_read_rnd_next"
        };

        try (Connection connection = dataSource.getConnection(); Statement st = connection.createStatement()) {
            for (String metric : metrics) {
                try (ResultSet rs = st.executeQuery("SHOW SESSION STATUS LIKE '" + metric + "'")) {
                    if (rs.next()) {
                        log.info("{}={}", rs.getString("Variable_name"), rs.getString("Value"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read Handler metrics: {}", e.getMessage());
        }
    }
}