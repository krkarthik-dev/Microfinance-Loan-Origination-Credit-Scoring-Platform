package com.microfinance.entity;

import com.microfinance.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The core entity of the Microfinance Platform.
 *
 * <p>Represents every loan request — whether submitted online by a borrower
 * or created by a loan officer for a walk-in customer. Follows a strict
 * lifecycle managed by the workflow engine.
 *
 * <p>Note: {@code applicant_id} and {@code loan_officer_id} both reference
 * the users table but represent different roles — JPA handles this via
 * two separate @ManyToOne mappings with distinct join columns.
 */
@Entity
@Table(name = "loan_applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Human-readable identifier e.g., MF-2026-00001 */
    @Column(name = "application_number", nullable = false, unique = true, length = 20)
    private String applicationNumber;

    /** The borrower who submitted the application */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    /** The loan officer assigned to review (nullable until assignment) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_officer_id")
    private User loanOfficer;

    /** Type of loan product requested */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(name = "applied_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedAmount;

    /** Set after officer/manager approval decision */
    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    /** Set when the borrower formally submits the application */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
