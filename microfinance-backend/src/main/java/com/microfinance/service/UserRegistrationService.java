package com.microfinance.service;

import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service handling user registration with ACID-compliant atomic operations.
 *
 * <p><b>AC3 — ACID Compliance Demonstration:</b><br>
 * {@link #registerUserWithProfile(User, UserProfile)} is wrapped in a single
 * {@code @Transactional} boundary. If user creation succeeds but profile
 * creation fails for any reason (constraint violation, validation error,
 * runtime exception), the entire transaction is rolled back — including
 * the user record. This guarantees the database never holds an orphaned
 * user without a corresponding profile.
 *
 * <p>Business logic stays in the service layer (SOLID: SRP).
 * Repositories are injected via constructor (SOLID: DIP).
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Atomically registers a new user and their profile in a single transaction.
     *
     * <p>Uses {@code saveAndFlush} to force SQL execution within the transaction
     * boundary — making the rollback behavior observable and testable.
     *
     * <p>If {@code profile} is null or any save operation throws, the transaction
     * is fully rolled back (both user and profile saves are undone).
     *
     * @param user    the user credentials and role to persist
     * @param profile the personal profile to associate with the user
     * @return the saved {@link User} entity with its generated ID
     * @throws IllegalArgumentException if profile is null
     * @throws RuntimeException         if any DB constraint is violated
     */
    public User registerUserWithProfile(User user, UserProfile profile) {
        log.debug("Registering user: {}", user.getEmail());

        // Step 1: Persist the user (SQL flushed immediately within transaction)
        User savedUser = userRepository.saveAndFlush(user);
        log.debug("User saved with ID: {}", savedUser.getId());

        // Step 2: Validate profile before attempting to save
        // If profile is null, IllegalArgumentException is thrown here —
        // the transaction rolls back, undoing the user save above.
        if (profile == null) {
            throw new IllegalArgumentException(
                    "UserProfile cannot be null. User registration requires a profile. " +
                    "Transaction will rollback — user '" + user.getEmail() + "' will NOT be persisted."
            );
        }

        // Step 3: Associate profile with the saved user and persist
        profile.setUser(savedUser);
        userProfileRepository.saveAndFlush(profile);
        log.debug("Profile saved for user ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Finds a user by email address.
     *
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Checks if an email is already registered.
     *
     * @param email the email to check
     * @return true if already registered
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }
}
