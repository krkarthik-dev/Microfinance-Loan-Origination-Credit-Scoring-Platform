package com.microfinance.controller;

import com.microfinance.dto.OfficerApplicationDetailDTO;
import com.microfinance.dto.OfficerApplicationDetailDTO;
import com.microfinance.dto.OfficerApplicationSummaryDTO;
import com.microfinance.dto.OfficerDecisionRequestDTO;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.CreditScore;
import com.microfinance.entity.KycDocument;
import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.LoanDocument;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.enums.ApplicationStatus;
import com.microfinance.enums.DocumentType;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.CreditScoreRepository;
import com.microfinance.repository.KycDocumentRepository;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.LoanDocumentRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
public class OfficerDashboardController {

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserProfileRepository userProfileRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * US16: Returns a list of all applications currently in the UNDER_REVIEW state.
     * The sorting logic by RiskTier/CreditScore will be handled by the frontend grid
     * as requested, or can be pre-sorted here.
     */
    @GetMapping("/applications/queue")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<OfficerApplicationSummaryDTO>> getUnderReviewApplications() {
        List<OfficerApplicationSummaryDTO> queue = loanApplicationRepository.findSummariesByStatus(ApplicationStatus.UNDER_REVIEW);
        return ResponseEntity.ok(queue);
    }

    /**
     * US18: Fetch comprehensive details for the Application Review page.
     */
    @GetMapping("/applications/{applicationNumber}/details")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> getApplicationDetails(@PathVariable String applicationNumber) {
        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationNumber(applicationNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LoanApplication app = appOpt.get();
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(app.getApplicant().getId());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("User profile missing");
        }
        UserProfile profile = profileOpt.get();

        Optional<CreditScore> scoreOpt = creditScoreRepository.findByApplicationId(app.getId());
        
        List<KycDocument> kycDocs = kycDocumentRepository.findByUserId(app.getApplicant().getId());
        Long panDocId = kycDocs.stream().filter(d -> d.getDocumentType() == DocumentType.PAN).map(KycDocument::getId).findFirst().orElse(null);
        Long aadhaarDocId = kycDocs.stream().filter(d -> d.getDocumentType() == DocumentType.AADHAAR).map(KycDocument::getId).findFirst().orElse(null);

        List<LoanDocument> loanDocs = loanDocumentRepository.findByApplicationId(app.getId());
        Long incomeDocId = loanDocs.stream().filter(d -> d.getDocumentType().equals("INCOME_CERTIFICATE")).map(LoanDocument::getId).findFirst().orElse(null);
        Long photoDocId = loanDocs.stream().filter(d -> d.getDocumentType().equals("PHOTOGRAPH")).map(LoanDocument::getId).findFirst().orElse(null);
        Long guarantorDocId = loanDocs.stream().filter(d -> d.getDocumentType().equals("GUARANTOR_ID")).map(LoanDocument::getId).findFirst().orElse(null);
        
        List<OfficerApplicationDetailDTO.DocumentSummaryDTO> otherDocs = loanDocs.stream()
                .filter(d -> d.getDocumentType().equals("OTHER"))
                .map(d -> new OfficerApplicationDetailDTO.DocumentSummaryDTO(d.getId(), d.getFileName(), d.getDocumentType()))
                .collect(Collectors.toList());

        // Calculate EMI manually for display
        BigDecimal emi = calculateEmi(app.getAppliedAmount(), app.getLoanProduct().getInterestRatePa(), app.getTenureMonths());
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(app.getTenureMonths()));

        OfficerApplicationDetailDTO dto = OfficerApplicationDetailDTO.builder()
                .applicationId(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .submittedAt(app.getSubmittedAt())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .phone(profile.getPhoneNumber())
                .email(app.getApplicant().getEmail())
                .address(profile.getAddressLine1() + (profile.getAddressLine2() != null ? ", " + profile.getAddressLine2() : "") + ", " + profile.getCity() + ", " + profile.getState() + " - " + profile.getPincode())
                .employmentType(profile.getEmploymentType())
                .monthlyIncome(profile.getMonthlyIncome())
                .panNumber(profile.getPanNumber())
                .aadhaarNumber(profile.getAadhaarNumber())
                .appliedAmount(app.getAppliedAmount())
                .tenureMonths(app.getTenureMonths())
                .interestRate(app.getLoanProduct().getInterestRatePa())
                .monthlyEmi(emi)
                .totalPayable(totalPayable)
                .purpose(app.getPurpose())
                .guarantorName(app.getGuarantorName())
                .guarantorAddress(app.getGuarantorAddress())
                .guarantorCity(app.getGuarantorCity())
                .guarantorZip(app.getGuarantorZip())
                .guarantorAadhaar(app.getGuarantorAadhaar())
                .guarantorPan(app.getGuarantorPan())
                .creditScore(scoreOpt.map(CreditScore::getCreditScore).orElse(null))
                .riskTier(scoreOpt.map(CreditScore::getRiskTier).orElse(null))
                .probabilityOfDefault(scoreOpt.map(CreditScore::getProbabilityOfDefault).orElse(null))
                .panDocumentId(panDocId)
                .aadhaarDocumentId(aadhaarDocId)
                .incomeDocumentId(incomeDocId)
                .photoDocumentId(photoDocId)
                .guarantorIdDocumentId(guarantorDocId)
                .otherDocuments(otherDocs)
                .build();

        return ResponseEntity.ok(dto);
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int months) {
        if (principal == null || annualInterestRate == null || months <= 0) return BigDecimal.ZERO;
        double p = principal.doubleValue();
        double r = annualInterestRate.doubleValue() / 12 / 100;
        double n = months;
        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    @GetMapping("/documents/kyc/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<byte[]> getKycDocument(@PathVariable Long id) {
        return kycDocumentRepository.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(doc.getContentType()))
                        .body(doc.getFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/documents/loan/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<byte[]> getLoanDocument(@PathVariable Long id) {
        return loanDocumentRepository.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(doc.getContentType()))
                        .body(doc.getFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * US20: Process Officer underwriting decision.
     */
    @PutMapping("/applications/{applicationNumber}/decision")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> submitDecision(
            @PathVariable String applicationNumber,
            @RequestBody OfficerDecisionRequestDTO request) {

        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationNumber(applicationNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LoanApplication app = appOpt.get();
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            return ResponseEntity.badRequest().body("Application is not in UNDER_REVIEW status.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User officer = userRepository.findByEmail(auth.getName()).orElse(null);

        ApplicationStatus newStatus;
        switch (request.getDecision().toUpperCase()) {
            case "APPROVE":
                newStatus = ApplicationStatus.APPROVED;
                break;
            case "REJECT":
                newStatus = ApplicationStatus.REJECTED;
                break;
            case "ESCALATE":
                newStatus = ApplicationStatus.ESCALATED;
                break;
            default:
                return ResponseEntity.badRequest().body("Invalid decision.");
        }

        app.setStatus(newStatus);
        loanApplicationRepository.save(app);

        // Audit Logging
        String noteDetails = "Decision: " + request.getDecision();
        if (request.getRejectionReason() != null && !request.getRejectionReason().isBlank()) {
            noteDetails += " | Reason: " + request.getRejectionReason();
        }
        if (request.getInternalNotes() != null && !request.getInternalNotes().isBlank()) {
            noteDetails += " | Notes: " + request.getInternalNotes();
        }

        AuditLog audit = AuditLog.builder()
                .entityType("LOAN_APPLICATION")
                .entityId(app.getId())
                .action(newStatus.name())
                .performedBy(officer)
                .oldValue(ApplicationStatus.UNDER_REVIEW.name())
                .newValue(newStatus.name())
                .build();
        // Since we don't have a specific notes field in AuditLog, we can store it in newValue or a similar construct.
        // But for simplicity, we'll serialize the state to newValue
        audit.setNewValue("{\"status\":\"" + newStatus.name() + "\", \"details\":\"" + noteDetails + "\"}");
        
        auditLogRepository.save(audit);

        return ResponseEntity.ok().build();
    }
}
