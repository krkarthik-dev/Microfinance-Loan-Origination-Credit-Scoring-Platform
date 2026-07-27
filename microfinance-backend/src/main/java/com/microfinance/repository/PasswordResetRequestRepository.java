package com.microfinance.repository;

import com.microfinance.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    Optional<PasswordResetRequest> findByRequestId(String requestId);
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<PasswordResetRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByRequestId(String requestId);
}
