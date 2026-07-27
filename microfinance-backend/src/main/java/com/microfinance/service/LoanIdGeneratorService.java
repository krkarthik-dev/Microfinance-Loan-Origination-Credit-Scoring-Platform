package com.microfinance.service;

import com.microfinance.repository.LoanIdSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * US64: Smart Loan ID Generation & Monthly Sequence Reset
 * Generates formatted IDs matching LN-MMYYYYXXXX with thread-safe synchronization enclosing transaction commit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanIdGeneratorService {

    private final LoanIdSequenceRepository sequenceRepository;

    /**
     * Generates a unique, sequential loan ID in format LN-MMYYYYXXXX.
     * Synchronized at service level without transaction proxy so that the underlying
     * REQUIRES_NEW transaction commits fully before the monitor lock is released.
     */
    public synchronized String generateNextLoanId() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        String sequenceKey = String.format("%02d%04d", now.getMonthValue(), now.getYear());

        Long sequenceVal = sequenceRepository.getNextValAtomic(sequenceKey);

        return String.format("LN-%s%04d", sequenceKey, sequenceVal);
    }
}
