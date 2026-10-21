package com.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the Spring application context loads without a running database.
 * JdbcTemplate is mocked so JDBC-backed repositories can be wired without
 * a real DataSource (excluded via test application.properties).
 */
@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }
}
