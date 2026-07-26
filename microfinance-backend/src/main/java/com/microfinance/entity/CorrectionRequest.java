package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_correction_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CorrectionRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "section", nullable = false)
    private String section;

    @Column(name = "comments", nullable = false, columnDefinition = "TEXT")
    private String comments;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;
}
