package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a loan product offered by the microfinance institution.
 *
 * <p>Loan products are configured by the admin and referenced by all
 * loan applications. Defines the financial parameters (amount range,
 * interest rate, tenure) that govern a loan type.
 */
@Entity
@Table(name = "loan_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "product_name", nullable = false, unique = true, length = 100)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxAmount;

    /** Annual interest rate in percentage (e.g., 12.50 means 12.50% per annum) */
    @Column(name = "interest_rate_pa", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRatePa;

    @Column(name = "min_tenure_months", nullable = false)
    private Integer minTenureMonths;

    @Column(name = "max_tenure_months", nullable = false)
    private Integer maxTenureMonths;

    /** Processing fee as a percentage of the loan amount */
    @Column(name = "processing_fee_pct", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal processingFeePct = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
