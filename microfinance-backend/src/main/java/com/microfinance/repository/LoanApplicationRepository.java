package com.microfinance.repository;

import com.microfinance.entity.LoanApplication;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.dto.OfficerApplicationSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /** US66: Check if any historical application references this loan product. */
    boolean existsByLoanProductId(Long loanProductId);

    // US06: Borrower Dashboard Metrics
    int countByApplicantIdAndStatusIn(Long applicantId, List<ApplicationStatus> statuses);

    @Query("SELECT COALESCE(SUM(l.approvedAmount), 0) FROM LoanApplication l WHERE l.applicant.id = :applicantId AND l.status IN :statuses")
    BigDecimal sumApprovedAmountByApplicantIdAndStatusIn(@Param("applicantId") Long applicantId, @Param("statuses") List<ApplicationStatus> statuses);

    // US16: Officer Dashboard Query
    @Query("SELECT new com.microfinance.dto.OfficerApplicationSummaryDTO(" +
           "a.id, a.applicationNumber, p.firstName, p.lastName, " +
           "a.appliedAmount, a.tenureMonths, a.purpose, a.submittedAt, " +
           "c.creditScore, c.riskTier, a.status) " +
           "FROM LoanApplication a " +
           "JOIN UserProfile p ON p.user = a.applicant " +
           "LEFT JOIN CreditScore c ON c.application = a " +
           "WHERE a.status IN :statuses")
    List<OfficerApplicationSummaryDTO> findSummariesByStatuses(@Param("statuses") List<ApplicationStatus> statuses);

    // US26: Admin Dashboard Queries
    @Query("SELECT COALESCE(SUM(l.approvedAmount), 0) FROM LoanApplication l WHERE l.status IN :statuses AND l.updatedAt >= :startDate")
    BigDecimal sumApprovedAmountByStatusInAndUpdatedAtAfter(@Param("statuses") List<ApplicationStatus> statuses, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<ApplicationStatus> statuses);
}
