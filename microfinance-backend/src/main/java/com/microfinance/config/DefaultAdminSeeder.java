package com.microfinance.config;

import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@microfinance.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Seeding default Administrator account: {}", adminEmail);
            User defaultAdmin = User.builder()
                    .email(adminEmail)
                    .username("Default Admin") // Full Name mapped to username
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(UserRole.ROLE_ADMIN)
                    .active(true)
                    .mustChangePassword(true)
                    .build();
            userRepository.save(defaultAdmin);
            log.info("Default Administrator account successfully seeded.");
        } else {
            log.info("Default Administrator account already exists. Skipping seeding.");
        }
    }
}
