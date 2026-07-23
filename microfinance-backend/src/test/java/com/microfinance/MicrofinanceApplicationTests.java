package com.microfinance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot context load test.
 *
 * <p>Verifies that the entire Spring application context initializes
 * successfully without any bean wiring or configuration errors.
 *
 * <p>Uses the "test" profile to load H2 in-memory database instead
 * of PostgreSQL, so no external DB is required to run tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class MicrofinanceApplicationTests {

    @Test
    @DisplayName("Spring application context should load successfully")
    void contextLoads() {
        // If the context fails to load, this test will throw an exception
        // No assertions needed — the test itself is the assertion
    }
}
