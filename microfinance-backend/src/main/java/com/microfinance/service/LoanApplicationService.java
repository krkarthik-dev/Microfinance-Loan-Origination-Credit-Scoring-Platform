package com.microfinance.service;

import com.microfinance.event.LoanSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsible for managing loan application core workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Submits a loan application and triggers asynchronous ML scoring.
     * 
     * @param applicationId the ID of the application (stubbed for US04)
     * @param applicantId the borrower's ID
     * @param amount the requested loan amount
     */
    public void submitApplication(Long applicationId, Long applicantId, BigDecimal amount) {
        log.info("Main thread [{}] - Processing loan submission for Application ID: {}", 
                 Thread.currentThread().getName(), applicationId);

        // Simulated DB Save logic would go here.
        // E.g., applicationRepository.save(application);

        // AC2: Publish the event to trigger async background processing
        LoanSubmittedEvent event = LoanSubmittedEvent.builder()
                .applicationId(applicationId)
                .applicantId(applicantId)
                .appliedAmount(amount)
                .build();

        log.info("Main thread [{}] - Publishing LoanSubmittedEvent for Application ID: {}", 
                 Thread.currentThread().getName(), applicationId);
                 
        eventPublisher.publishEvent(event);

        log.info("Main thread [{}] - Finished submitApplication method.", 
                 Thread.currentThread().getName());
    }
}
