package com.microfinance.service;

import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.LoanProduct;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.DocumentType;
import com.microfinance.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycStateSyncTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepo;
    @Mock
    private LoanDocumentRepository loanDocumentRepo;
    @Mock
    private LoanProductRepository loanProductRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private UserProfileRepository userProfileRepo;
    @Mock
    private KycDocumentRepository kycDocumentRepo;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LoanIdGeneratorService loanIdGeneratorService;

    @InjectMocks
    private LoanSubmissionService loanSubmissionService;

    @Test
    @DisplayName("US67 AC1: Submit loan when KYC is MISSING throws IllegalStateException")
    void testSubmitApplication_WhenKycMissing_ThrowsException() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(loanProductRepo.findAll()).thenReturn(Collections.singletonList(LoanProduct.builder().active(true).build()));
        when(userProfileRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(kycDocumentRepo.findByUserIdAndDocumentType(1L, DocumentType.PAN)).thenReturn(Optional.empty());
        when(kycDocumentRepo.findByUserIdAndDocumentType(1L, DocumentType.AADHAAR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanSubmissionService.submitApplication(
                "test@example.com",
                BigDecimal.valueOf(50000),
                12,
                "Business",
                "Guarantor",
                "Address",
                "City",
                "123456",
                "123456789012",
                "ABCDE1234F",
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                new MultipartFile[]{},
                mock(MultipartFile.class)
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot submit loan application without uploading KYC documents");
    }

    @Test
    @DisplayName("US67 AC1: Submit loan when KYC is PENDING initializes status to PENDING_KYC")
    void testSubmitApplication_WhenKycPending_SetsStatusToPendingKyc() throws IOException {
        User user = User.builder().id(1L).email("test@example.com").build();
        UserProfile profile = UserProfile.builder().kycVerified(false).kycStatus("PENDING").build();
        LoanProduct product = LoanProduct.builder().id(10L).active(true).interestRatePa(BigDecimal.valueOf(12)).build();

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(loanProductRepo.findAll()).thenReturn(Collections.singletonList(product));
        when(userProfileRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(loanIdGeneratorService.generateNextLoanId()).thenReturn("LN-2026-0001");
        when(pdfGenerationService.generateApplicationPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(loanApplicationRepo.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));

        LoanApplication app = loanSubmissionService.submitApplication(
                "test@example.com",
                BigDecimal.valueOf(50000),
                12,
                "Business",
                "Guarantor",
                "Address",
                "City",
                "123456",
                "123456789012",
                "ABCDE1234F",
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                new MultipartFile[]{},
                mock(MultipartFile.class)
        );

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING_KYC);
    }

    @Test
    @DisplayName("US67 AC1: Submit loan when KYC is APPROVED initializes status to SUBMITTED")
    void testSubmitApplication_WhenKycApproved_SetsStatusToSubmitted() throws IOException {
        User user = User.builder().id(1L).email("test@example.com").build();
        UserProfile profile = UserProfile.builder().kycVerified(true).kycStatus("APPROVED").build();
        LoanProduct product = LoanProduct.builder().id(10L).active(true).interestRatePa(BigDecimal.valueOf(12)).build();

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(loanProductRepo.findAll()).thenReturn(Collections.singletonList(product));
        when(userProfileRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(loanIdGeneratorService.generateNextLoanId()).thenReturn("LN-2026-0002");
        when(pdfGenerationService.generateApplicationPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(loanApplicationRepo.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));

        LoanApplication app = loanSubmissionService.submitApplication(
                "test@example.com",
                BigDecimal.valueOf(50000),
                12,
                "Business",
                "Guarantor",
                "Address",
                "City",
                "123456",
                "123456789012",
                "ABCDE1234F",
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                new MultipartFile[]{},
                mock(MultipartFile.class)
        );

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }
}
