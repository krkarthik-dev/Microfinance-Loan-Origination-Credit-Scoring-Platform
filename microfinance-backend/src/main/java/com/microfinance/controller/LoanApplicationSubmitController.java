package com.microfinance.controller;

import com.microfinance.entity.LoanApplication;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.service.LoanSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @GetMapping("/{applicationNumber}/status")
    public ResponseEntity<?> getApplicationStatus(
            @PathVariable String applicationNumber,
            Principal principal
    ) {
        return loanApplicationRepo.findByApplicationNumber(applicationNumber)
                .map(app -> ResponseEntity.ok(Map.of(
                        "status", app.getStatus().name(),
                        "isDirect", app.getLoanOfficer() != null
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
