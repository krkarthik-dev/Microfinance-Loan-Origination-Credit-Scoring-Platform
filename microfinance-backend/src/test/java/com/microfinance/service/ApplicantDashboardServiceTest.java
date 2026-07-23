package com.microfinance.service;

import com.microfinance.dto.DashboardMetricsDto;
import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicantDashboardServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicantDashboardService applicantDashboardService;

    @Test
    @DisplayName("Should successfully return dashboard metrics for borrower")
    void shouldReturnDashboardMetrics() {
        // Arrange
        String username = "borrower@test.com";
        User user = User.builder().id(100L).email(username).build();
        
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
        
        when(loanApplicationRepository.countByApplicantIdAndStatusIn(eq(100L), any()))
                .thenReturn(3) // 3 pending
                .thenReturn(2); // 2 active
                
        when(loanApplicationRepository.sumApprovedAmountByApplicantIdAndStatusIn(eq(100L), any()))
                .thenReturn(new BigDecimal("150000.00"));
                
        LoanApplication mockApp = LoanApplication.builder()
                .applicationNumber("APP-123")
                .appliedAmount(new BigDecimal("1000.00"))
                .status(ApplicationStatus.SUBMITTED)
                .build();
        when(loanApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(mockApp));

        // Act
        DashboardMetricsDto metrics = applicantDashboardService.getDashboardMetrics(username);

        // Assert
        assertThat(metrics).isNotNull();
        assertThat(metrics.getPendingApplications()).isEqualTo(3);
        assertThat(metrics.getActiveLoans()).isEqualTo(2);
        assertThat(metrics.getTotalOutstanding()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(metrics.getRecentActivity()).hasSize(1);
        assertThat(metrics.getRecentActivity().get(0).getLoanId()).isEqualTo("APP-123");
    }
}
