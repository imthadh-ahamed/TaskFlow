package com.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the Spring application context loads without a running database.
 * JdbcTemplate and JdbcClient are mocked so JDBC-backed repositories can be
 * wired without a real DataSource.
 */
@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    JdbcClient jdbcClient;

    @Test
    void contextLoads() {
    }
}
