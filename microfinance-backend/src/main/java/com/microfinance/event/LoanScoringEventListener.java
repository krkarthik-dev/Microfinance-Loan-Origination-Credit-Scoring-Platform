package com.microfinance.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.entity.CreditScore;
import com.microfinance.entity.LoanApplication;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.RiskTier;
import com.microfinance.repository.CreditScoreRepository;
import com.microfinance.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoanScoringEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoanScoringEventListener.class);
    
    private final LoanApplicationRepository loanApplicationRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void handleLoanSubmittedEvent(LoanSubmittedEvent event) {
        log.info("Received LoanSubmittedEvent for application ID: {}", event.getApplicationId());

        LoanApplication application = loanApplicationRepository.findById(event.getApplicationId())
                .orElseThrow(() -> new IllegalStateException("Application not found"));

        // 1. Update status to RISK_ASSESSMENT
        application.setStatus(ApplicationStatus.RISK_ASSESSMENT);
        loanApplicationRepository.save(application);

        try {
            // 2. Execute Python script
            String pythonScriptPath = Paths.get("ml", "score_application.py").toAbsolutePath().toString();
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python", 
                    pythonScriptPath, 
                    String.valueOf(event.getApplicationId()),
                    event.getAppliedAmount().toString()
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script exited with code {}. Output: {}", exitCode, output.toString());
                return; // Or handle error state
            }

            // 3. Parse JSON output
            JsonNode resultNode = objectMapper.readTree(output.toString());
            
            if (resultNode.has("error")) {
                log.error("Error from Python script: {}", resultNode.get("error").asText());
                return;
            }

            int score = resultNode.get("creditScore").asInt();
            double pod = resultNode.get("probabilityOfDefault").asDouble();
            String riskTierStr = resultNode.get("riskTier").asText();
            String modelVersion = resultNode.get("modelVersion").asText();

            // 4. Save CreditScore
            CreditScore creditScore = CreditScore.builder()
                    .application(application)
                    .creditScore(score)
                    .probabilityOfDefault(BigDecimal.valueOf(pod))
                    .riskTier(RiskTier.valueOf(riskTierStr))
                    .modelVersion(modelVersion)
                    .scoredAt(LocalDateTime.now())
                    .build();
            
            creditScoreRepository.save(creditScore);

            // 5. Update application status to UNDER_REVIEW
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            loanApplicationRepository.save(application);
            
            log.info("Successfully scored application ID: {}", event.getApplicationId());

        } catch (Exception e) {
            log.error("Failed to process ML scoring for application ID: {}", event.getApplicationId(), e);
        }
    }
}
