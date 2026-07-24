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
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "loan_applications")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LoanApplication extends BaseEntity {



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

    // ── Guarantor Details ──────────────────────────────────────────────────────
    @Column(name = "guarantor_name", length = 200)
    private String guarantorName;

    @Column(name = "guarantor_address", columnDefinition = "TEXT")
    private String guarantorAddress;

    @Column(name = "guarantor_city", length = 100)
    private String guarantorCity;

    @Column(name = "guarantor_zip", length = 10)
    private String guarantorZip;

    @Column(name = "guarantor_aadhaar", length = 12)
    private String guarantorAadhaar;

    @Column(name = "guarantor_pan", length = 10)
    private String guarantorPan;

    // ── Signature & PDF ────────────────────────────────────────────────────────
    @Column(name = "signature_image", columnDefinition = "BYTEA")
    private byte[] signatureImage;

    @Column(name = "signature_content_type", length = 50)
    private String signatureContentType;

    @Column(name = "application_pdf", columnDefinition = "BYTEA")
    private byte[] applicationPdf;

    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private boolean termsAccepted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    /** Set when the borrower formally submits the application */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

}
