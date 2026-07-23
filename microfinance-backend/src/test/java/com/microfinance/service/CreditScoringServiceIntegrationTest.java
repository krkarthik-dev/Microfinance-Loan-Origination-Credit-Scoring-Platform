package com.microfinance.service;

import com.microfinance.dto.MlScoringRequest;
import com.microfinance.dto.MlScoringResponse;
import com.microfinance.entity.CreditScore;
import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.LoanProduct;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.RiskTier;
import com.microfinance.enums.UserRole;
import com.microfinance.event.LoanSubmittedEvent;
import com.microfinance.repository.CreditScoreRepository;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.LoanProductRepository;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CreditScoringServiceIntegrationTest {

    @Autowired
    private CreditScoringService creditScoringService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private CreditScoreRepository creditScoreRepository;

    @MockBean
    private PythonMlExecutionService pythonMlExecutionService;

    private User savedApplicant;
    private LoanApplication savedApplication;
    private LoanProduct savedProduct;

    @BeforeEach
    void setUp() {
        creditScoreRepository.deleteAll();
        loanApplicationRepository.deleteAll();
        loanProductRepository.deleteAll();
        userRepository.deleteAll();

        User applicant = User.builder()
                .username("mlapplicant")
                .email("mlapplicant@test.com")
                .passwordHash("hash")
                .role(UserRole.ROLE_APPLICANT)
                .active(true)
                .build();
        savedApplicant = userRepository.saveAndFlush(applicant);

        LoanProduct product = LoanProduct.builder()
                .productName("Business Loan Test")
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRatePa(new BigDecimal("12.50"))
                .minTenureMonths(6)
                .maxTenureMonths(60)
                .build();
        savedProduct = loanProductRepository.saveAndFlush(product);

        LoanApplication application = LoanApplication.builder()
                .applicationNumber("APP-2026-TEST")
                .applicant(savedApplicant)
                .loanProduct(savedProduct)
                .appliedAmount(new BigDecimal("50000"))
                .tenureMonths(12)
                .purpose("Business")
                .status(ApplicationStatus.SUBMITTED)
                .build();
        savedApplication = loanApplicationRepository.saveAndFlush(application);
    }

    @Test
    @DisplayName("Should process LoanSubmittedEvent, invoke ML Service, and save CreditScore")
    void shouldProcessEventAndSaveCreditScore() {
        // Arrange
        MlScoringResponse mockResponse = new MlScoringResponse(750, 0.15, null);
        when(pythonMlExecutionService.executeScoringModel(any(MlScoringRequest.class)))
                .thenReturn(mockResponse);

        LoanSubmittedEvent event = LoanSubmittedEvent.builder()
                .applicationId(savedApplication.getId())
                .applicantId(savedApplicant.getId())
                .appliedAmount(new BigDecimal("50000"))
                .build();

        // Act
        // We directly invoke the method instead of publishing the event to test it synchronously
        creditScoringService.handleLoanSubmittedEvent(event);

        // Assert
        verify(pythonMlExecutionService, timeout(5000).times(1)).executeScoringModel(any(MlScoringRequest.class));

        CreditScore score = null;
        for (int i = 0; i < 50; i++) {
            Optional<CreditScore> scoreOpt = creditScoreRepository.findByApplicationId(savedApplication.getId());
            if (scoreOpt.isPresent()) {
                score = scoreOpt.get();
                break;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        assertThat(score).isNotNull();
        assertThat(score.getCreditScore()).isEqualTo(750);
        assertThat(score.getProbabilityOfDefault()).isEqualByComparingTo(new BigDecimal("0.15"));
        assertThat(score.getRiskTier()).isEqualTo(RiskTier.LOW);
    }
}
