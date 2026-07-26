package com.microfinance.controller;

import com.microfinance.entity.LoanApplication;
import com.microfinance.entity.LoanDocument;
import com.microfinance.entity.AuditLog;
import com.microfinance.entity.CorrectionRequest;
import com.microfinance.dto.CorrectionRequestDTO;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.LoanDocumentRepository;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.UserRepository;
import com.microfinance.repository.CorrectionRequestRepository;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.KycDocumentRepository;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.entity.KycDocument;
import com.microfinance.enums.DocumentType;
import com.microfinance.service.LoanSubmissionService;
import com.microfinance.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

/**
 * Handles loan application submission (AC3, AC4) including PDF generation.
 * All endpoints require ROLE_APPLICANT.
 */
@RestController
@RequestMapping("/api/applicant/loan")
@RequiredArgsConstructor
public class LoanApplicationSubmitController {

    private final LoanSubmissionService submissionService;
    private final LoanApplicationRepository loanApplicationRepo;
    private final LoanDocumentRepository loanDocumentRepo;
    private final AuditLogRepository auditLogRepo;
    private final UserRepository userRepo;
    private final CorrectionRequestRepository correctionRequestRepo;
    private final UserProfileRepository userProfileRepo;
    private final KycDocumentRepository kycDocumentRepo;

    /**
     * POST /api/applicant/loan/submit
     *
     * Accepts all step data as multipart/form-data.
     * Returns the generated PDF as an attachment.
     */
    @PreAuthorize("hasRole('APPLICANT')")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitApplication(
            Principal principal,
            // Step 1
            @RequestParam("principalAmount") BigDecimal principalAmount,
            @RequestParam("tenureMonths")    int tenureMonths,
            @RequestParam("purpose")         String purpose,
            // Step 2 – Guarantor
            @RequestParam("guarantorName")    String guarantorName,
            @RequestParam("guarantorAddress") String guarantorAddress,
            @RequestParam("guarantorCity")    String guarantorCity,
            @RequestParam("guarantorZip")     String guarantorZip,
            @RequestParam("guarantorAadhaar") String guarantorAadhaar,
            @RequestParam("guarantorPan")     String guarantorPan,
            // Step 3 – Documents
            @RequestParam("incomeCert")    MultipartFile incomeCert,
            @RequestParam("photo")         MultipartFile photo,
            @RequestParam("guarantorId")   MultipartFile guarantorId,
            @RequestParam(value = "otherDocs", required = false) java.util.List<MultipartFile> otherDocs,
            // Step 4 – Signature
            @RequestParam("signature") MultipartFile signature
    ) {
        try {
            LoanApplication saved = submissionService.submitApplication(
                    principal.getName(),
                    principalAmount, tenureMonths, purpose,
                    guarantorName, guarantorAddress, guarantorCity, guarantorZip,
                    guarantorAadhaar, guarantorPan,
                    incomeCert, photo, guarantorId, otherDocs != null ? otherDocs.toArray(new MultipartFile[0]) : null,
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate application PDF: " + e.getMessage()));
        }
    }

    /**
     * GET /api/applicant/loan/{applicationNumber}/pdf
     * Allows the borrower to re-download their application PDF after submission.
     */
    @PreAuthorize("hasRole('APPLICANT')")
    @GetMapping("/{applicationNumber}/pdf")
    public ResponseEntity<?> downloadPdf(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        // Delegate to service (placeholder — can be fleshed out later)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "PDF re-download coming in a future sprint."));
    }

    /**
     * GET /api/applicant/loan/{applicationNumber}/status
     * Returns the current status of the loan application for the tracker.
     */
    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional(readOnly = true)
    @GetMapping("/{applicationNumber}/status")
    public ResponseEntity<?> getApplicationStatus(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    return ResponseEntity.ok(Map.of(
                            "status", app.getStatus().name(),
                            "isDirect", app.getLoanOfficer() != null
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional(readOnly = true)
    @GetMapping("/{applicationNumber}/info-request")
    public ResponseEntity<?> getOfficerNotes(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    if (app.getStatus() != ApplicationStatus.INFO_REQUESTED) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Application is not in INFO_REQUESTED state"));
                    }

                    // Find latest INFO_REQUESTED audit log
                    return auditLogRepo.findAll().stream()
                            .filter(log -> "LOAN_APPLICATION".equals(log.getEntityType()) 
                                    && log.getEntityId().equals(app.getId())
                                    && "INFO_REQUESTED".equals(log.getAction()))
                            .max(java.util.Comparator.comparing(AuditLog::getCreatedAt))
                            .map(log -> {
                                // Extract notes from newValue JSON
                                String notes = log.getNewValue();
                                if (notes != null && notes.contains("\"details\":\"")) {
                                    int start = notes.indexOf("\"details\":\"") + 11;
                                    int end = notes.indexOf("\"", start);
                                    if (end > start) {
                                        notes = notes.substring(start, end);
                                    }
                                }
                                return ResponseEntity.ok(Map.of("notes", notes != null ? notes : "Please provide the requested information."));
                            })
                            .orElse(ResponseEntity.ok(Map.of("notes", "Please provide the requested information.")));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional
    @PostMapping("/{applicationNumber}/upload-correction")
    public ResponseEntity<?> uploadCorrection(
            @PathVariable String applicationNumber,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    if (app.getStatus() != ApplicationStatus.INFO_REQUESTED) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Application is not in INFO_REQUESTED state"));
                    }

                    try {
                        LoanDocument doc = LoanDocument.builder()
                                .application(app)
                                .documentType("CORRECTION")
                                .fileName(file.getOriginalFilename())
                                .contentType(file.getContentType())
                                .fileData(file.getBytes())
                                .build();
                        loanDocumentRepo.save(doc);

                        // Update Status
                        app.setStatus(ApplicationStatus.UNDER_REVIEW);
                        loanApplicationRepo.save(app);
                        
                        // Mark Correction Requests as resolved
                        correctionRequestRepo.findByLoanApplicationIdAndResolvedFalse(app.getId())
                                .forEach(cr -> {
                                    cr.setResolved(true);
                                    correctionRequestRepo.save(cr);
                                });

                        // Audit Log
                        AuditLog audit = AuditLog.builder()
                                .entityType("LOAN_APPLICATION")
                                .entityId(app.getId())
                                .action("CORRECTION_SUBMITTED")
                                .performedBy(userRepo.findByEmail(principal.getName()).orElse(null))
                                .oldValue("INFO_REQUESTED")
                                .newValue("UNDER_REVIEW")
                                .build();
                        auditLogRepo.save(audit);

                        return ResponseEntity.ok(Map.of("message", "Correction submitted successfully"));
                    } catch (IOException e) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to store document: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional
    @PostMapping("/{applicationNumber}/resubmit-corrections")
    public ResponseEntity<?> resubmitCorrections(
            @PathVariable String applicationNumber,
            @RequestParam(value = "guarantorName", required = false) String guarantorName,
            @RequestParam(value = "guarantorAddress", required = false) String guarantorAddress,
            @RequestParam(value = "guarantorCity", required = false) String guarantorCity,
            @RequestParam(value = "guarantorZip", required = false) String guarantorZip,
            @RequestParam(value = "appliedAmount", required = false) BigDecimal appliedAmount,
            @RequestParam(value = "tenureMonths", required = false) Integer tenureMonths,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "addressLine1", required = false) String addressLine1,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "pincode", required = false) String pincode,
            @RequestParam(value = "panFile", required = false) MultipartFile panFile,
            @RequestParam(value = "aadhaarFile", required = false) MultipartFile aadhaarFile,
            @RequestParam(value = "incomeCertFile", required = false) MultipartFile incomeCertFile,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    if (app.getStatus() != ApplicationStatus.INFO_REQUESTED) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Application is not in INFO_REQUESTED state"));
                    }

                    try {
                        if (guarantorName != null) app.setGuarantorName(guarantorName);
                        if (guarantorAddress != null) app.setGuarantorAddress(guarantorAddress);
                        if (guarantorCity != null) app.setGuarantorCity(guarantorCity);
                        if (guarantorZip != null) app.setGuarantorZip(guarantorZip);
                        if (appliedAmount != null) app.setAppliedAmount(appliedAmount);
                        if (tenureMonths != null) app.setTenureMonths(tenureMonths);
                        if (purpose != null) app.setPurpose(purpose);

                        if (phone != null || addressLine1 != null || city != null || state != null || pincode != null) {
                            userProfileRepo.findByUserId(app.getApplicant().getId()).ifPresent(profile -> {
                                if (phone != null) profile.setPhoneNumber(phone);
                                if (addressLine1 != null) profile.setAddressLine1(addressLine1);
                                if (city != null) profile.setCity(city);
                                if (state != null) profile.setState(state);
                                if (pincode != null) profile.setPincode(pincode);
                                userProfileRepo.save(profile);
                            });
                        }

                        if (panFile != null && !panFile.isEmpty()) {
                            updateKycDoc(app.getApplicant(), DocumentType.PAN, panFile);
                        }
                        if (aadhaarFile != null && !aadhaarFile.isEmpty()) {
                            updateKycDoc(app.getApplicant(), DocumentType.AADHAAR, aadhaarFile);
                        }
                        if (incomeCertFile != null && !incomeCertFile.isEmpty()) {
                            updateLoanDoc(app, "INCOME_CERTIFICATE", incomeCertFile);
                        }
                        if (photoFile != null && !photoFile.isEmpty()) {
                            updateLoanDoc(app, "PHOTOGRAPH", photoFile);
                        }

                        app.setStatus(ApplicationStatus.SUBMITTED);
                        loanApplicationRepo.save(app);

                        correctionRequestRepo.findByLoanApplicationIdAndResolvedFalse(app.getId())
                                .forEach(cr -> {
                                    cr.setResolved(true);
                                    correctionRequestRepo.save(cr);
                                });

                        AuditLog audit = AuditLog.builder()
                                .entityType("LOAN_APPLICATION")
                                .entityId(app.getId())
                                .action("RESUBMITTED_CORRECTIONS")
                                .performedBy(userRepo.findByEmail(principal.getName()).orElse(null))
                                .oldValue("INFO_REQUESTED")
                                .newValue("SUBMITTED")
                                .build();
                        auditLogRepo.save(audit);

                        return ResponseEntity.ok(Map.of("message", "Corrections submitted successfully and application reverted to SUBMITTED"));
                    } catch (Exception e) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to resubmit corrections: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void updateKycDoc(User user, DocumentType type, MultipartFile file) throws IOException {
        java.util.List<KycDocument> docs = kycDocumentRepo.findByUserId(user.getId());
        KycDocument doc = docs.stream().filter(d -> d.getDocumentType() == type).findFirst().orElse(null);
        if (doc != null) {
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setFileData(file.getBytes());
            doc.setFileSizeBytes(file.getSize());
            doc.setVerified(false);
            kycDocumentRepo.save(doc);
        } else {
            KycDocument newDoc = KycDocument.builder()
                    .user(user)
                    .documentType(type)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileData(file.getBytes())
                    .fileSizeBytes(file.getSize())
                    .verified(false)
                    .build();
            kycDocumentRepo.save(newDoc);
        }
    }

    private void updateLoanDoc(LoanApplication app, String type, MultipartFile file) throws IOException {
        java.util.List<LoanDocument> docs = loanDocumentRepo.findByApplicationId(app.getId());
        LoanDocument doc = docs.stream().filter(d -> d.getDocumentType().equals(type)).findFirst().orElse(null);
        if (doc != null) {
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setFileData(file.getBytes());
            loanDocumentRepo.save(doc);
        } else {
            LoanDocument newDoc = LoanDocument.builder()
                    .application(app)
                    .documentType(type)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileData(file.getBytes())
                    .build();
            loanDocumentRepo.save(newDoc);
        }
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional(readOnly = true)
    @GetMapping("/{applicationNumber}/audit-trail")
    public ResponseEntity<?> getApplicationAuditTrail(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    
                    java.util.List<AuditLog> logs = auditLogRepo.findAll().stream()
                            .filter(log -> "LOAN_APPLICATION".equals(log.getEntityType()) && log.getEntityId().equals(app.getId()))
                            .sorted(java.util.Comparator.comparing(AuditLog::getCreatedAt))
                            .collect(java.util.stream.Collectors.toList());
                            
                    return ResponseEntity.ok(logs);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional(readOnly = true)
    @GetMapping("/{applicationNumber}/correction-requests")
    public ResponseEntity<?> getCorrectionRequests(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }
                    
                    java.util.List<CorrectionRequestDTO> dtos = correctionRequestRepo.findByLoanApplicationIdAndResolvedFalse(app.getId())
                            .stream()
                            .map(cr -> CorrectionRequestDTO.builder()
                                    .section(cr.getSection())
                                    .comments(cr.getComments())
                                    .build())
                            .collect(java.util.stream.Collectors.toList());
                            
                    return ResponseEntity.ok(dtos);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('APPLICANT')")
    @Transactional
    @PostMapping("/{applicationNumber}/withdraw")
    public ResponseEntity<?> withdrawApplication(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> {
                    if (!app.getApplicant().getEmail().equals(principal.getName())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", "Access Denied"));
                    }

                    java.util.List<ApplicationStatus> allowedStatuses = java.util.List.of(
                            ApplicationStatus.DRAFT,
                            ApplicationStatus.SUBMITTED,
                            ApplicationStatus.PENDING_KYC,
                            ApplicationStatus.UNDER_REVIEW,
                            ApplicationStatus.INFO_REQUESTED,
                            ApplicationStatus.PENDING_MANAGER_APPROVAL
                    );

                    if (!allowedStatuses.contains(app.getStatus())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Application cannot be withdrawn once approved or in later stages."));
                    }

                    ApplicationStatus oldStatus = app.getStatus();
                    app.setStatus(ApplicationStatus.WITHDRAWN);
                    loanApplicationRepo.save(app);

                    AuditLog audit = AuditLog.builder()
                            .entityType("LOAN_APPLICATION")
                            .entityId(app.getId())
                            .action("WITHDRAWN_BY_APPLICANT")
                            .performedBy(userRepo.findByEmail(principal.getName()).orElse(null))
                            .oldValue(oldStatus.name())
                            .newValue(ApplicationStatus.WITHDRAWN.name())
                            .build();
                    auditLogRepo.save(audit);

                    return ResponseEntity.ok(Map.of("message", "Application successfully withdrawn.", "status", "WITHDRAWN"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
