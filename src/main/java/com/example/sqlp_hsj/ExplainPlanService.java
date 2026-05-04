package com.example.sqlp_hsj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExplainPlanService {

    private final JdbcTemplate jdbcTemplate;

    public void explain(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("EXPLAIN " + sql, args);
        log.info("EXPLAIN for: {}", sql);
        rows.forEach(row -> log.info("{}", row));
    }

    public void explainJson(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("EXPLAIN FORMAT=JSON " + sql, args);
        log.info("EXPLAIN JSON for: {}", sql);
        rows.forEach(row -> log.info("{}", row));
    }

    public void explainAnalyze(String sql, Object... args) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("EXPLAIN ANALYZE " + sql, args);
            log.info("EXPLAIN ANALYZE for: {}", sql);
            rows.forEach(row -> log.info("{}", row));
        } catch (Exception e) {
            log.warn("EXPLAIN ANALYZE failed (version/permission dependent): {}", e.getMessage());
        }
    }
}