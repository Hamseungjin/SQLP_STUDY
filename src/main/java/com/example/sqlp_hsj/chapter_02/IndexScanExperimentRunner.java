package com.example.sqlp_hsj.chapter_02;

import com.example.sqlp_hsj.ExplainPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexScanExperimentRunner implements ApplicationRunner {
    private final ExplainPlanService explainPlanService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${sqltuning.practice.mode:none}")
    private String mode;
    @Value("${sqltuning.index-scan.sample-limit:3000}")
    private int sampleLimit;

    @Override
    public void run(ApplicationArguments args) {
        if (!"index-scan".equalsIgnoreCase(mode)) return;

        LocalDate day = LocalDate.now().minusDays(1);
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();

        String q1 = "SELECT * FROM orders WHERE order_date >= ? AND order_date < ? LIMIT " + sampleLimit;
        explainPlanService.explain(q1, from, to);
        explainPlanService.explainJson(q1, from, to);
        explainPlanService.explainAnalyze(q1, from, to);

        String q2 = "SELECT * FROM orders WHERE DATE(order_date) = ? LIMIT " + sampleLimit;
        explainPlanService.explain(q2, day);
        explainPlanService.explainJson(q2, day);

        String q3 = "SELECT * FROM orders WHERE order_date >= ? AND order_date < ? LIMIT " + sampleLimit;
        explainPlanService.explain(q3, from, to);
        explainPlanService.explainAnalyze(q3, from, to);

        explainCompositeIndex();
    }

    private void explainCompositeIndex() {
        long customerId = 12345L;
        String status = "PAID";

        explainPlanService.explain("SELECT * FROM orders WHERE customer_id = ? AND status = ?", customerId, status);
        explainPlanService.explain("SELECT * FROM orders WHERE status = ?", status);
        explainPlanService.explain("SELECT * FROM orders WHERE customer_id = ?", customerId);

        long t1 = timedQuery("SELECT COUNT(*) FROM orders WHERE DATE(order_date) = ?", LocalDate.now().minusDays(1));
        long t2 = timedQuery("SELECT COUNT(*) FROM orders WHERE order_date >= ? AND order_date < ?", LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().atStartOfDay());
        log.info("Function-on-column query took={} ms, range query took={} ms", t1, t2);
    }

    private long timedQuery(String sql, Object... args) {
        long start = System.nanoTime();
        jdbcTemplate.queryForObject(sql, Long.class, args);
        return (System.nanoTime() - start) / 1_000_000;
    }
}