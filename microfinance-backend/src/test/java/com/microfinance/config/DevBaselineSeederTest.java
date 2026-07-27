package com.microfinance.config;

import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DevBaselineSeederTest {

    @Autowired
    private DevBaselineSeeder devBaselineSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void testSeederExecutionAndIdempotency() throws Exception {
        // 1. Verify initial startup seeding created exactly 11 accounts
        long initialCount = userRepository.count();
        assertEquals(11, initialCount, "Initial startup seeding should create exactly 11 baseline accounts!");

        // Verify marker row created in seeding_history table
        Integer markerCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM seeding_history WHERE script_name = ?",
            Integer.class, DevBaselineSeeder.SCRIPT_NAME
        );
        assertNotNull(markerCount);
        assertEquals(1, markerCount, "Permanent execution marker must be logged in seeding_history!");

        // 2. Add a new user to simulate work done after startup
        User customUser = User.builder()
                .email("newuser_after_boot@microfinance.com")
                .username("newuser_after_boot")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .mustChangePassword(false)
                .build();
        userRepository.save(customUser);
        assertEquals(12, userRepository.count(), "User count should be 12 after adding custom user!");

        // 3. Trigger seeder again (simulating subsequent server restart or invocation)
        devBaselineSeeder.run();

        // 4. Verify idempotency: data was NOT wiped and seeding was bypassed!
        assertEquals(12, userRepository.count(),
            "Idempotency violation! Subsequent seeder run wiped or modified existing data!");
        Optional<User> checkUser = userRepository.findByEmail("newuser_after_boot@microfinance.com");
        assertTrue(checkUser.isPresent(), "Custom user created after boot must be preserved!");
    }

    @Test
    void testProductionEnvironmentGatingThrowsException() {
        Environment mockEnv = Mockito.mock(Environment.class);
        Mockito.when(mockEnv.getActiveProfiles()).thenReturn(new String[]{"prod"});

        DevBaselineSeeder prodSeeder = new DevBaselineSeeder(
                jdbcTemplate, userRepository, userProfileRepository, passwordEncoder, mockEnv, transactionTemplate
        );

        assertThrows(SecurityException.class, prodSeeder::run,
                "Executing seeder in 'prod' profile must throw SecurityException!");
    }
}
