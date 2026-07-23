package com.microfinance.service;

import com.microfinance.event.LoanSubmittedEvent;
import com.microfinance.dto.MlScoringRequest;
import com.microfinance.dto.MlScoringResponse;
import com.microfinance.entity.CreditScore;
import com.microfinance.entity.LoanApplication;
import com.microfinance.enums.RiskTier;
import com.microfinance.repository.CreditScoreRepository;
import com.microfinance.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AC3: Background service that intercepts events and runs ML logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditScoringService {

    private final PythonMlExecutionService pythonMlExecutionService;
    private final CreditScoreRepository creditScoreRepository;
    private final LoanApplicationRepository loanApplicationRepository;

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
            log.info("Background thread [{}] - Invoking Python ML Script...", 
                     Thread.currentThread().getName());
                     
            MlScoringRequest request = MlScoringRequest.builder()
                    .applicationId(event.getApplicationId())
                    .applicantId(event.getApplicantId())
                    .appliedAmount(event.getAppliedAmount())
                    .income(new BigDecimal("60000")) // default simulated
                    .age(35) // default simulated
                    .existingDebt(new BigDecimal("2000")) // default simulated
                    .build();

            MlScoringResponse response = pythonMlExecutionService.executeScoringModel(request);

            if (response.getError() != null) {
                log.error("ML Scoring failed: {}", response.getError());
                return;
            }

            log.info("Background thread [{}] - ML Scoring complete. Score: {}, PoD: {}", 
                     Thread.currentThread().getName(), response.getCreditScore(), response.getProbabilityOfDefault());

            LoanApplication application = loanApplicationRepository.findById(event.getApplicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found: " + event.getApplicationId()));

            RiskTier riskTier = response.getCreditScore() > 700 ? RiskTier.LOW : 
                               (response.getCreditScore() > 600 ? RiskTier.MEDIUM : RiskTier.HIGH);

            CreditScore scoreEntity = CreditScore.builder()
                    .application(application)
                    .creditScore(response.getCreditScore())
                    .probabilityOfDefault(BigDecimal.valueOf(response.getProbabilityOfDefault()))
                    .scoredAt(LocalDateTime.now())
                    .riskTier(riskTier)
                    .modelVersion("RandomForest_v1")
                    .build();

            creditScoreRepository.save(scoreEntity);
            log.info("Credit score saved for Application ID: {}", event.getApplicationId());

        } catch (Exception e) {
            log.error("ML scoring thread exception", e);
        }
    }
}
