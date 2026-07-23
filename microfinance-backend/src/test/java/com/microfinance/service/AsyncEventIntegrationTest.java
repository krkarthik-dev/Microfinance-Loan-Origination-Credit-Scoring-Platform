package com.microfinance.service;

import com.microfinance.event.LoanSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Integration tests to verify AC3 (Thread Handoff) and AC4 (Non-Blocking execution).
 */
@SpringBootTest
@ActiveProfiles("test")
class AsyncEventIntegrationTest {

    @Autowired
    private LoanApplicationService loanApplicationService;

    @SpyBean
    private CreditScoringService creditScoringService;

    @Test
    @DisplayName("AC3 & AC4: Event listener executes on async thread while main thread returns immediately")
    void shouldExecuteListenerAsynchronously() {
        long startTime = System.currentTimeMillis();

        // ── ACT ──
        // This should return ALMOST IMMEDIATELY (AC4)
        loanApplicationService.submitApplication(99L, 100L, new BigDecimal("50000.00"));
        
        long executionTimeMs = System.currentTimeMillis() - startTime;

        // ── ASSERT MAIN THREAD (AC4) ──
        // The main thread should not block for the 3 seconds the listener sleeps.
        // Even with framework overhead, it should take < 1000ms.
        assertThat(executionTimeMs).isLessThan(1000L);

        // ── ASSERT BACKGROUND THREAD (AC3) ──
        // Verify the listener was called (which proves thread handoff since it takes 3s to complete and we are verifying asynchronously)
        verify(creditScoringService, timeout(5000)).handleLoanSubmittedEvent(any(LoanSubmittedEvent.class));
    }
}
