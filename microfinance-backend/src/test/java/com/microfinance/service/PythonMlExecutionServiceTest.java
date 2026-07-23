package com.microfinance.service;

import com.microfinance.dto.MlScoringRequest;
import com.microfinance.dto.MlScoringResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PythonMlExecutionServiceTest {

    @Autowired
    private PythonMlExecutionService pythonMlExecutionService;

    @Test
    @DisplayName("Should successfully execute Python ML script and return parsed response")
    void shouldExecutePythonScriptAndReturnResponse() {
        // Arrange
        MlScoringRequest request = MlScoringRequest.builder()
                .applicationId(1L)
                .applicantId(2L)
                .appliedAmount(new BigDecimal("15000.00"))
                .income(new BigDecimal("50000.00"))
                .age(30)
                .existingDebt(new BigDecimal("1000.00"))
                .build();

        // Act
        MlScoringResponse response = pythonMlExecutionService.executeScoringModel(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNull();
        
        // The mock model should return a valid credit score between 300 and 900
        assertThat(response.getCreditScore()).isNotNull()
                .isGreaterThanOrEqualTo(300)
                .isLessThanOrEqualTo(900);
                
        // The probability of default should be between 0 and 1
        assertThat(response.getProbabilityOfDefault()).isNotNull()
                .isGreaterThanOrEqualTo(0.0)
                .isLessThanOrEqualTo(1.0);
    }
}
