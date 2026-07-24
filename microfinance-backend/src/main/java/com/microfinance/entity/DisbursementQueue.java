package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursement_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "approved_amount", nullable = false)
    private BigDecimal approvedAmount;

    @Column(nullable = false, length = 50)
    private String status; // e.g. PENDING_DISBURSEMENT, PROCESSING, COMPLETED, FAILED

    @Column(name = "queued_at", nullable = false)
    private LocalDateTime queuedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
