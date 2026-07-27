package com.microfinance.controller;

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
import com.microfinance.repository.CorrectionRequestRepository;
import com.microfinance.entity.CorrectionRequest;
import com.microfinance.repository.SystemNotificationRepository;
import com.microfinance.entity.SystemNotification;
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
import org.springframework.transaction.annotation.Transactional;

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
    private final com.microfinance.repository.DisbursementQueueRepository disbursementQueueRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final SystemNotificationRepository systemNotificationRepository;
    private final com.microfinance.service.RepaymentService repaymentService;

    /**
     * US16: Returns a list of all applications currently in the UNDER_REVIEW state.
     * The sorting logic by RiskTier/CreditScore will be handled by the frontend grid
     * as requested, or can be pre-sorted here.
     */
    @GetMapping("/applications/queue")
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OfficerApplicationSummaryDTO>> getUnderReviewApplications() {
        List<ApplicationStatus> statuses = List.of(
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.PENDING_KYC,
                ApplicationStatus.UNDER_REVIEW
        );
        List<OfficerApplicationSummaryDTO> queue = loanApplicationRepository.findSummariesByStatuses(statuses);
        
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/applications/approved")
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OfficerApplicationSummaryDTO>> getApprovedApplications() {
        List<ApplicationStatus> statuses = List.of(
                ApplicationStatus.APPROVED,
                ApplicationStatus.CLOSING
        );
        List<OfficerApplicationSummaryDTO> queue = loanApplicationRepository.findSummariesByStatuses(statuses);
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/applications/rejected")
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OfficerApplicationSummaryDTO>> getRejectedApplications() {
        List<ApplicationStatus> statuses = List.of(
                ApplicationStatus.REJECTED
        );
        List<OfficerApplicationSummaryDTO> queue = loanApplicationRepository.findSummariesByStatuses(statuses);
        return ResponseEntity.ok(queue);
    }

    /**
     * US18: Fetch comprehensive details for the Application Review page.
     */
    @GetMapping("/applications/{applicationNumber}/details")
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
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

        List<String> recentlyCorrected = correctionRequestRepository.findByLoanApplicationId(app.getId()).stream()
                .filter(CorrectionRequest::isResolved)
                .map(CorrectionRequest::getSection)
                .distinct()
                .collect(Collectors.toList());

        OfficerApplicationDetailDTO dto = OfficerApplicationDetailDTO.builder()
                .applicationId(app.getId())
                .applicantId(app.getApplicant().getId())
                .applicationNumber(app.getApplicationNumber())
                .status(app.getStatus().name())
                .submittedAt(app.getSubmittedAt())
                .kycVerified(profile.isKycVerified())
                .kycStatus(profile.getKycStatus())
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
                .recentlyCorrectedSections(recentlyCorrected)
                .build();

        if (app.getStatus() == ApplicationStatus.SUBMITTED) {
            app.setStatus(ApplicationStatus.UNDER_REVIEW);
            loanApplicationRepository.save(app);
            
            AuditLog audit = AuditLog.builder()
                .entityType("LOAN_APPLICATION")
                .entityId(app.getId())
                .action("REVIEW_STARTED")
                .performedBy(userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null))
                .oldValue(ApplicationStatus.SUBMITTED.name())
                .newValue(ApplicationStatus.UNDER_REVIEW.name())
                .build();
            auditLogRepository.save(audit);
        }
        
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
    @PostMapping("/applications/{applicationNumber}/disburse")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> initiateDisbursement(@PathVariable String applicationNumber) {
        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationNumber(applicationNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LoanApplication app = appOpt.get();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Evaluate Dual-Approval Rule
        com.microfinance.entity.CreditScore cs = creditScoreRepository.findByApplicationId(app.getId()).orElse(null);
        boolean requiresDualApproval = app.getAppliedAmount().compareTo(new BigDecimal("1000000")) > 0 || (cs != null && "HIGH".equals(cs.getRiskTier()));
        
        // SoD Check
        if (requiresDualApproval && !isAdmin) {
            AuditLog audit = AuditLog.builder()
                    .entityType("LOAN_APPLICATION")
                    .entityId(app.getId())
                    .action("UNAUTHORIZED_DISBURSEMENT_ATTEMPT")
                    .performedBy(currentUser)
                    .oldValue(app.getStatus().name())
                    .newValue(app.getStatus().name())
                    .build();
            auditLogRepository.save(audit);
            return ResponseEntity.status(403).body("Forbidden: High-liability loans require Manager approval for disbursement.");
        }

        // Find in queue
        Optional<com.microfinance.entity.DisbursementQueue> queueOpt = disbursementQueueRepository.findAll().stream()
                .filter(q -> q.getLoanApplication().getId().equals(app.getId()) && "PENDING_DISBURSEMENT".equals(q.getStatus()))
                .findFirst();
                
        if (queueOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No pending disbursement found for this application.");
        }

        com.microfinance.entity.DisbursementQueue queueItem = queueOpt.get();

        // 1. Mark Queue Item as COMPLETED
        queueItem.setStatus("COMPLETED");
        queueItem.setProcessedAt(LocalDateTime.now());
        disbursementQueueRepository.save(queueItem);

        // 2. Update Loan Application Status to ACTIVE_REPAYMENT
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.ACTIVE_REPAYMENT);
        loanApplicationRepository.save(app);

        // 3. Audit Logging
        AuditLog audit = AuditLog.builder()
                .entityType("LOAN_APPLICATION")
                .entityId(app.getId())
                .action("MANUAL_DISBURSEMENT")
                .performedBy(currentUser)
                .oldValue(oldStatus.name())
                .newValue(ApplicationStatus.ACTIVE_REPAYMENT.name())
                .build();
        auditLogRepository.save(audit);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/applications/{applicationNumber}/audit-trail")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AuditLog>> getApplicationAuditTrail(@PathVariable String applicationNumber) {
        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationNumber(applicationNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(log -> "LOAN_APPLICATION".equals(log.getEntityType()) && log.getEntityId().equals(appOpt.get().getId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(logs);
    }


    /**
     * US20: Process Officer underwriting decision.
     */
    @PutMapping("/applications/{applicationNumber}/decision")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> submitDecision(
            @PathVariable String applicationNumber,
            @RequestBody OfficerDecisionRequestDTO request) {

        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationNumber(applicationNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LoanApplication app = appOpt.get();
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW && app.getStatus() != ApplicationStatus.PENDING_MANAGER_APPROVAL) {
            return ResponseEntity.badRequest().body("Application is not in a valid state for decisions.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User officer = userRepository.findByEmail(auth.getName()).orElse(null);

        ApplicationStatus oldStatus = app.getStatus();
        ApplicationStatus newStatus;
        boolean addToDisbursementQueue = false;

        switch (request.getDecision().toUpperCase()) {
            case "APPROVE":
                newStatus = ApplicationStatus.CLOSING;
                addToDisbursementQueue = true; // Still add to disbursement queue or handle later? Actually US 43 AC4 says Direct Closing: transitions directly to CLOSING. We can add to queue here, or admin will handle it from CLOSING state. Let's keep adding it to queue for now so Admin can see it.
                break;
            case "RECOMMEND_APPROVAL":
                com.microfinance.entity.CreditScore cs = creditScoreRepository.findByApplicationId(app.getId()).orElse(null);
                if (app.getAppliedAmount().compareTo(new BigDecimal("1000000")) <= 0 && (cs == null || !"HIGH".equals(cs.getRiskTier()))) {
                    return ResponseEntity.badRequest().body("Application does not meet dual-approval criteria.");
                }
                newStatus = ApplicationStatus.PENDING_MANAGER_APPROVAL;
                break;
            case "REJECT":
                newStatus = ApplicationStatus.REJECTED;
                break;
            case "ESCALATE":
                newStatus = ApplicationStatus.PENDING_MANAGER_APPROVAL;
                break;
            case "REQUEST_INFO":
                newStatus = ApplicationStatus.INFO_REQUESTED;
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
                .oldValue(oldStatus.name())
                .newValue(newStatus.name())
                .build();
        // Since we don't have a specific notes field in AuditLog, we can store it in newValue or a similar construct.
        // But for simplicity, we'll serialize the state to newValue
        audit.setNewValue("{\"status\":\"" + newStatus.name() + "\", \"details\":\"" + noteDetails + "\"}");
        
        auditLogRepository.save(audit);

        // US53: Save Granular Correction Requests & US54 AC1: Notification Gateway
        if (newStatus == ApplicationStatus.INFO_REQUESTED) {
            if (request.getCorrectionRequests() != null) {
                request.getCorrectionRequests().forEach(cr -> {
                    CorrectionRequest correctionRequest = CorrectionRequest.builder()
                            .loanApplication(app)
                            .section(cr.getSection())
                            .comments(cr.getComments())
                            .resolved(false)
                            .build();
                    correctionRequestRepository.save(correctionRequest);
                });
            }
            
            SystemNotification notification = SystemNotification.builder()
                    .user(app.getApplicant())
                    .message("Action Required: Loan Officer requested corrections for loan " + app.getApplicationNumber())
                    .linkUrl("/applicant/loan/" + app.getApplicationNumber() + "/corrections")
                    .isRead(false)
                    .build();
            systemNotificationRepository.save(notification);
        }

        // AC4: Disbursement Queue Routing
        if (addToDisbursementQueue) {
            com.microfinance.entity.DisbursementQueue queueItem = com.microfinance.entity.DisbursementQueue.builder()
                    .loanApplication(app)
                    .approvedAmount(app.getAppliedAmount()) // Here we assume approved amount is full applied amount
                    .status("PENDING_DISBURSEMENT")
                    .queuedAt(LocalDateTime.now())
                    .build();
            disbursementQueueRepository.save(queueItem);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * US21: Fetch all users pending KYC Verification.
     */
    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
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
                    .kycStatus(profile.getKycStatus())
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
            profile.setKycStatus("APPROVED");
            userProfileRepository.save(profile);
            
            // Also mark the specific documents as verified
            List<KycDocument> docs = kycDocumentRepository.findByUserId(userId);
            for (KycDocument doc : docs) {
                doc.setVerified(true);
                doc.setVerifiedBy(officer);
                doc.setVerifiedAt(LocalDateTime.now());
                kycDocumentRepository.save(doc);
            }

            // AC4: Automatically transition any active loan applications tied to that user from PENDING_KYC to UNDER_REVIEW
            List<LoanApplication> pendingKycApps = loanApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(userId).stream()
                    .filter(app -> app.getStatus() == ApplicationStatus.PENDING_KYC)
                    .collect(Collectors.toList());
            for (LoanApplication app : pendingKycApps) {
                app.setStatus(ApplicationStatus.UNDER_REVIEW);
                app.setUpdatedAt(LocalDateTime.now());
                loanApplicationRepository.save(app);

                AuditLog appAudit = AuditLog.builder()
                        .entityType("LOAN_APPLICATION")
                        .entityId(app.getId())
                        .action("UNDER_REVIEW")
                        .performedBy(officer)
                        .oldValue("PENDING_KYC")
                        .newValue("UNDER_REVIEW")
                        .build();
                auditLogRepository.save(appAudit);
            }
        } else {
            profile.setKycVerified(false);
            profile.setKycStatus("REJECTED");
            userProfileRepository.save(profile);
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
                .oldValue("kycVerified=" + !isApproved)
                .newValue("{\"kycVerified\":" + isApproved + ", \"kycStatus\":\"" + profile.getKycStatus() + "\", \"details\":\"" + noteDetails + "\"}")
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
                .kycVerified(true) // US25: Officer verifies KYC during walk-in creation
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

            // US25 AC1: Assign the officer and create Audit Log
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User officer = userRepository.findByEmail(auth.getName()).orElse(null);
            
            if (officer != null) {
                saved.setLoanOfficer(officer);
                loanApplicationRepository.save(saved);

                AuditLog audit = AuditLog.builder()
                        .entityType("LOAN_APPLICATION")
                        .entityId(saved.getId())
                        .action("DIRECT_DEAL_SUBMITTED")
                        .performedBy(officer)
                        .newValue("{\"email\":\"" + email + "\", \"officer_id\":" + officer.getId() + "}")
                        .build();
                auditLogRepository.save(audit);
            }

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

    @GetMapping("/disbursed-loans")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> getDisbursedLoans(Authentication authentication) {
        return ResponseEntity.ok(repaymentService.getOfficerDisbursedLoans(authentication.getName()));
    }

    @GetMapping("/loans/{applicationNumber}/repayment-schedule")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> getOfficerRepaymentSchedule(@PathVariable String applicationNumber) {
        try {
            return ResponseEntity.ok(repaymentService.getOfficerRepaymentSchedule(applicationNumber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/loans/{applicationNumber}/installments/{installmentId}/pay")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> collectInstallmentPayment(
            @PathVariable String applicationNumber,
            @PathVariable Long installmentId,
            @RequestBody com.microfinance.dto.PaymentCollectionRequestDto request,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(repaymentService.markInstallmentAsPaid(applicationNumber, installmentId, request, authentication.getName()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
