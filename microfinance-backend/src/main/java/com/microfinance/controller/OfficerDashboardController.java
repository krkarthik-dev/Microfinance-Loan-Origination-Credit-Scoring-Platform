package com.microfinance.controller;

import com.microfinance.dto.OfficerApplicationDetailDTO;
import com.microfinance.dto.OfficerApplicationDetailDTO;
import com.microfinance.dto.OfficerApplicationSummaryDTO;
import com.microfinance.dto.OfficerDecisionRequestDTO;
import com.microfinance.dto.PendingKycDTO;
import com.microfinance.dto.KycDecisionRequestDTO;
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
import com.microfinance.service.LoanSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final PasswordEncoder passwordEncoder;
    private final LoanSubmissionService loanSubmissionService;

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

    /**
     * US21: Fetch all users pending KYC Verification.
     */
    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<PendingKycDTO>> getPendingKyc() {
        List<UserProfile> pendingProfiles = userProfileRepository.findByKycVerifiedFalse();
        
        List<PendingKycDTO> dtos = pendingProfiles.stream().map(profile -> {
            Long userId = profile.getUser().getId();
            
            Long panDocId = kycDocumentRepository.findByUserIdAndDocumentType(userId, DocumentType.PAN)
                    .map(KycDocument::getId).orElse(null);
            Long aadhaarDocId = kycDocumentRepository.findByUserIdAndDocumentType(userId, DocumentType.AADHAAR)
                    .map(KycDocument::getId).orElse(null);
                    
            // Only include if they actually uploaded documents
            if (panDocId == null || aadhaarDocId == null) {
                return null;
            }

            return PendingKycDTO.builder()
                    .userId(userId)
                    .fullName(profile.getFirstName() + " " + profile.getLastName())
                    .email(profile.getUser().getEmail())
                    .panNumber(profile.getPanNumber())
                    .aadhaarNumber(profile.getAadhaarNumber())
                    .profileCreatedAt(profile.getCreatedAt())
                    .panDocumentId(panDocId)
                    .aadhaarDocumentId(aadhaarDocId)
                    .build();
        }).filter(dto -> dto != null).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * US21: Process KYC Verification Decision.
     */
    @PutMapping("/kyc/{userId}/decision")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> submitKycDecision(
            @PathVariable Long userId,
            @RequestBody KycDecisionRequestDTO request) {

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile profile = profileOpt.get();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User officer = userRepository.findByEmail(auth.getName()).orElse(null);

        boolean isApproved = "APPROVE".equalsIgnoreCase(request.getDecision());
        
        if (isApproved) {
            profile.setKycVerified(true);
            userProfileRepository.save(profile);
            
            // Also mark the specific documents as verified
            List<KycDocument> docs = kycDocumentRepository.findByUserId(userId);
            for (KycDocument doc : docs) {
                doc.setVerified(true);
                doc.setVerifiedBy(officer);
                doc.setVerifiedAt(LocalDateTime.now());
                kycDocumentRepository.save(doc);
            }
        }

        // Audit Logging
        String action = isApproved ? "KYC_APPROVED" : "KYC_REJECTED";
        String noteDetails = "Decision: " + request.getDecision();
        if (!isApproved && request.getRejectionReason() != null) {
            noteDetails += " | Reason: " + request.getRejectionReason();
        }

        AuditLog audit = AuditLog.builder()
                .entityType("USER_PROFILE")
                .entityId(profile.getId())
                .action(action)
                .performedBy(officer)
                .oldValue("kycVerified=false")
                .newValue("{\"kycVerified\":" + isApproved + ", \"details\":\"" + noteDetails + "\"}")
                .build();
        
        auditLogRepository.save(audit);

        return ResponseEntity.ok().build();
    }

    /**
     * US22: Walk-In Account Generation
     */
    @PostMapping("/direct-application")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> createDirectApplication(@RequestBody com.microfinance.dto.DirectApplicationRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email is already registered.");
        }

        // 1. Create User
        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(com.microfinance.enums.UserRole.ROLE_APPLICANT)
                .mustChangePassword(true) // Force password change on first login
                .build();
        userRepository.save(user);

        // 2. Create UserProfile
        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .employmentType(request.getEmploymentType())
                .monthlyIncome(request.getMonthlyIncome())
                // Set default/dummy values for required fields not captured in basic form
                .phoneNumber(request.getEmail()) // placeholder since it's unique
                .gender("UNKNOWN")
                .addressLine1("Direct Application")
                .city("Unknown")
                .state("Unknown")
                .pincode("000000")
                .kycVerified(false)
                .build();
        
        userProfileRepository.save(profile);

        // Log action
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User officer = userRepository.findByEmail(auth.getName()).orElse(null);
        AuditLog audit = AuditLog.builder()
                .entityType("USER")
                .entityId(user.getId())
                .action("DIRECT_ACCOUNT_CREATED")
                .performedBy(officer)
                .newValue("{\"email\":\"" + user.getEmail() + "\", \"mustChangePassword\":true}")
                .build();
        auditLogRepository.save(audit);

        return ResponseEntity.ok(java.util.Map.of("email", user.getEmail()));
    }

    /**
     * US23: Direct Deal Origination via Standard Stepper
     */
    @PostMapping(value = "/direct-application/{email}/loan/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<?> submitDirectApplication(
            @PathVariable String email,
            @org.springframework.web.bind.annotation.RequestParam("principalAmount") BigDecimal principalAmount,
            @org.springframework.web.bind.annotation.RequestParam("tenureMonths")    int tenureMonths,
            @org.springframework.web.bind.annotation.RequestParam("purpose")         String purpose,
            @org.springframework.web.bind.annotation.RequestParam("guarantorName")    String guarantorName,
            @org.springframework.web.bind.annotation.RequestParam("guarantorAddress") String guarantorAddress,
            @org.springframework.web.bind.annotation.RequestParam("guarantorCity")    String guarantorCity,
            @org.springframework.web.bind.annotation.RequestParam("guarantorZip")     String guarantorZip,
            @org.springframework.web.bind.annotation.RequestParam("guarantorAadhaar") String guarantorAadhaar,
            @org.springframework.web.bind.annotation.RequestParam("guarantorPan")     String guarantorPan,
            @org.springframework.web.bind.annotation.RequestParam("incomeCert")    org.springframework.web.multipart.MultipartFile incomeCert,
            @org.springframework.web.bind.annotation.RequestParam("photo")         org.springframework.web.multipart.MultipartFile photo,
            @org.springframework.web.bind.annotation.RequestParam("guarantorId")   org.springframework.web.multipart.MultipartFile guarantorId,
            @org.springframework.web.bind.annotation.RequestParam(value = "otherDocs", required = false) org.springframework.web.multipart.MultipartFile[] otherDocs,
            @org.springframework.web.bind.annotation.RequestParam("signature") org.springframework.web.multipart.MultipartFile signature
    ) {
        try {
            LoanApplication saved = loanSubmissionService.submitApplication(
                    email,
                    principalAmount, tenureMonths, purpose,
                    guarantorName, guarantorAddress, guarantorCity, guarantorZip,
                    guarantorAadhaar, guarantorPan,
                    incomeCert, photo, guarantorId, otherDocs,
                    signature
            );

            // Return the generated PDF directly as a downloadable file
            byte[] pdfBytes = saved.getApplicationPdf();
            String filename = saved.getApplicationNumber() + "_application.pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header("X-Application-Number", saved.getApplicationNumber())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to generate application PDF: " + e.getMessage()));
        }
    }
}
