package com.microfinance.service;

import com.microfinance.event.LoanSubmittedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AC3: Background service that intercepts events and runs heavily ML logic.
 */
@Service
@Slf4j
public class CreditScoringService {

    /**
     * Intercepts LoanSubmittedEvent and processes it on the async executor thread pool.
     * 
     * @param event the event payload published by the main thread
     */
    @Async("asyncExecutor")
    @EventListener
    public void handleLoanSubmittedEvent(LoanSubmittedEvent event) {
        log.info("Background thread [{}] - Received LoanSubmittedEvent for Application ID: {}", 
                 Thread.currentThread().getName(), event.getApplicationId());

        try {
            log.info("Background thread [{}] - Starting ML scoring simulation (will sleep for 3 seconds)...", 
                     Thread.currentThread().getName());
                     
            // Simulate heavy ML processing (non-blocking to the main thread)
            Thread.sleep(3000);
            
            // In a full implementation, we would save a CreditScore entity here.
            log.info("Background thread [{}] - ML scoring simulation complete for Application ID: {}", 
                     Thread.currentThread().getName(), event.getApplicationId());
                     
        } catch (InterruptedException e) {
            log.error("ML scoring thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
