package com.TravelBuddy;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false"
})
class TravelBuddyApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }
}
