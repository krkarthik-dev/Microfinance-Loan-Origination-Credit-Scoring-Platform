package com.microfinance.entity;

import com.microfinance.enums.RiskTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores the ML credit scoring model output for a loan application.
 *
 * <p>One credit score per application (1:1). Populated asynchronously by the
 * Python ML pipeline after a {@code LoanSubmittedEvent} is processed.
 * The score guides the loan officer's decision.
 */
@Entity
@Table(name = "credit_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CreditScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    /** Credit score between 300 (highest risk) and 900 (lowest risk) */
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    /** Probability of default — e.g., 0.0342 means 3.42% chance */
    @Column(name = "probability_of_default", nullable = false, precision = 5, scale = 4)
    private BigDecimal probabilityOfDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false, length = 10)
    private RiskTier riskTier;

    /** Identifies which ML model version produced this score */
    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "scored_at", nullable = false)
    @Builder.Default
    private LocalDateTime scoredAt = LocalDateTime.now();
}
