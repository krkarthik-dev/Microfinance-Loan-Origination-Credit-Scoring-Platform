package com.microfinance.repository;

import com.microfinance.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link UserProfile} entity.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** Fetch profile by the owning user's ID. */
    Optional<UserProfile> findByUserId(Long userId);

    /** Check if a profile already exists for a user. */
    boolean existsByUserId(Long userId);

    /** Check PAN uniqueness before saving. */
    boolean existsByPanNumber(String panNumber);

    /** Check Aadhaar uniqueness before saving. */
    boolean existsByAadhaarNumber(String aadhaarNumber);
}
