package com.microfinance.repository;

import com.microfinance.entity.LoanApplication;
import com.microfinance.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link LoanApplication} entity.
 * Supports borrower tracking, officer queues, and admin reporting.
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    /** Borrower's application history. */
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    /** Officer's assigned applications — ordered by most recent first. */
    List<LoanApplication> findByLoanOfficerIdOrderByCreatedAtDesc(Long officerId);

    /** All applications in a given lifecycle state — used for officer dashboards. */
    List<LoanApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status);

    /** Find by human-readable application number. */
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    /** Count applications per status — used for manager dashboard metrics. */
    long countByStatus(ApplicationStatus status);
}
