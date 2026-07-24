package com.microfinance.repository;

import com.microfinance.entity.User;
import com.microfinance.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entity.
 * Provides CRUD operations and custom queries for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Find user by email — used for login and duplicate checks. */
    Optional<User> findByEmail(String email);

    /** Find user by username — used for login. */
    Optional<User> findByUsername(String username);

    /** Check if an email is already registered. */
    boolean existsByEmail(String email);

    /** Check if a username is already taken. */
    boolean existsByUsername(String username);

    /** Get all active users by role — used for admin staff management. */
    List<User> findByRoleAndActiveTrue(UserRole role);

    /** Get all users by a list of roles. */
    List<User> findByRoleIn(List<UserRole> roles);
}
