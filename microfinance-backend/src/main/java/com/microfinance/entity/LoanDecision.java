package com.microfinance.entity;

import com.microfinance.enums.DecisionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a single decision event on a loan application.
 *
 * <p>An application can accumulate multiple decision records as it progresses
 * through the lifecycle (e.g., ESCALATED by officer, then FINAL_APPROVED by manager).
 * Each record is immutable — decisions are never updated, only new ones appended.
 */
@Entity
@Table(name = "loan_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoanDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    /** The loan officer or manager who made this decision */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionType decision;

    /** Justification or comments for the decision (required for rejections/escalations) */
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "decided_at", nullable = false)
    @Builder.Default
    private LocalDateTime decidedAt = LocalDateTime.now();
}
