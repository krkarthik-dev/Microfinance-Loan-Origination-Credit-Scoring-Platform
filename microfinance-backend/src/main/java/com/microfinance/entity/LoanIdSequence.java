package com.microfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loan_id_sequence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanIdSequence {

    @Id
    @Column(name = "sequence_key", length = 10, nullable = false)
    private String sequenceKey;

    @Column(name = "next_val", nullable = false)
    private Long nextVal;
}
