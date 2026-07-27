package com.microfinance.service;

import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.LoanDocument;
import com.microfinance.entity.LoanProduct;
import com.microfinance.entity.User;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.LoanDocumentRepository;
import com.microfinance.repository.LoanProductRepository;
import com.microfinance.repository.UserRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.KycDocumentRepository;
import com.microfinance.entity.UserProfile;
import com.microfinance.event.LoanSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles the full loan application submission workflow.
 * Persists all form data, documents, signature, and generates the PDF.
 */
@Service
@RequiredArgsConstructor
public class LoanSubmissionService {

    private final LoanApplicationRepository loanApplicationRepo;
    private final LoanDocumentRepository    loanDocumentRepo;
    private final LoanProductRepository     loanProductRepo;
    private final UserRepository            userRepo;
    private final UserProfileRepository     userProfileRepo;
    private final KycDocumentRepository     kycDocumentRepo;
    private final PdfGenerationService      pdfGenerationService;
    private final DocumentStorageService    documentStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final LoanIdGeneratorService    loanIdGeneratorService;

    /**
     * Submits a complete loan application including documents and signature.
     * Returns the generated PDF as a byte array.
     */
    @Transactional
    public LoanApplication submitApplication(
            String applicantEmail,
            // Step 1
            BigDecimal principalAmount,
            int tenureMonths,
            String purpose,
            // Step 2 — Guarantor
            String guarantorName,
            String guarantorAddress,
            String guarantorCity,
            String guarantorZip,
            String guarantorAadhaar,
            String guarantorPan,
            // Step 3 — Docs
            MultipartFile incomeCertFile,
            MultipartFile photoFile,
            MultipartFile guarantorIdFile,
            MultipartFile[] otherFiles,
            // Step 4 — Signature
            MultipartFile signatureFile
    ) throws IOException {

        // 1. Resolve applicant user
        User applicant = userRepo.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalStateException("Applicant not found: " + applicantEmail));

        // 2. Resolve a default loan product (first active one)
        LoanProduct product = loanProductRepo.findAll().stream()
                .filter(LoanProduct::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active loan product configured."));

        // 3. Evaluate KYC Status (US67)
        UserProfile profile = userProfileRepo.findByUserId(applicant.getId()).orElse(null);
        boolean kycVerified = profile != null && profile.isKycVerified();
        String kycStatus = profile != null && profile.getKycStatus() != null ? profile.getKycStatus() : "MISSING";
        
        boolean hasPanDocument = kycDocumentRepo.findByUserIdAndDocumentType(applicant.getId(), com.microfinance.enums.DocumentType.PAN).isPresent();
        boolean hasAadhaarDocument = kycDocumentRepo.findByUserIdAndDocumentType(applicant.getId(), com.microfinance.enums.DocumentType.AADHAAR).isPresent();
        
        if (!hasPanDocument && !hasAadhaarDocument && "MISSING".equals(kycStatus)) {
            throw new IllegalStateException("Cannot submit loan application without uploading KYC documents in your profile first.");
        }
        
        ApplicationStatus initialStatus = kycVerified ? ApplicationStatus.SUBMITTED : ApplicationStatus.PENDING_KYC;

        // 4. Build and save the LoanApplication entity
        LoanApplication app = LoanApplication.builder()
                .applicationNumber(loanIdGeneratorService.generateNextLoanId())
                .applicant(applicant)
                .loanProduct(product)
                .appliedAmount(principalAmount)
                .tenureMonths(tenureMonths)
                .purpose(purpose)
                .guarantorName(guarantorName)
                .guarantorAddress(guarantorAddress)
                .guarantorCity(guarantorCity)
                .guarantorZip(guarantorZip)
                .guarantorAadhaar(guarantorAadhaar)
                .guarantorPan(guarantorPan)
                .termsAccepted(true)
                .status(initialStatus)
                .submittedAt(LocalDateTime.now())
                .build();

        // 4. Attach signature
        if (signatureFile != null && !signatureFile.isEmpty()) {
            app.setSignatureImage(documentStorageService.extractBytes(signatureFile));
            app.setSignatureContentType(documentStorageService.extractContentType(signatureFile));
        }

        // 5. Generate PDF (before final save, so we can embed app number)
        byte[] pdf = pdfGenerationService.generateApplicationPdf(
                app,
                documentStorageService.extractBytes(signatureFile),
                documentStorageService.extractContentType(signatureFile)
        );
        app.setApplicationPdf(pdf);

        // 6. Persist application
        LoanApplication saved = loanApplicationRepo.save(app);

        // 7. Persist uploaded documents
        saveDocument(saved, "INCOME_CERTIFICATE", incomeCertFile);
        saveDocument(saved, "PHOTOGRAPH", photoFile);
        saveDocument(saved, "GUARANTOR_ID", guarantorIdFile);
        if (otherFiles != null) {
            for (MultipartFile other : otherFiles) {
                if (other != null && !other.isEmpty()) {
                    saveDocument(saved, "OTHER", other);
                }
            }
        }

        // 8. Publish event for async scoring
        eventPublisher.publishEvent(new LoanSubmittedEvent(
                saved.getId(),
                applicant.getId(),
                saved.getAppliedAmount()
        ));

        return saved;
    }

    private void saveDocument(LoanApplication app, String type, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return;
        LoanDocument doc = LoanDocument.builder()
                .application(app)
                .documentType(type)
                .fileName(documentStorageService.extractFileName(file))
                .contentType(documentStorageService.extractContentType(file))
                .fileData(documentStorageService.extractBytes(file))
                .build();
        loanDocumentRepo.save(doc);
    }
}
