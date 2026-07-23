package com.microfinance.controller;

import com.microfinance.dto.KycUploadResponse;
import com.microfinance.enums.DocumentType;
import com.microfinance.service.KycDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applicant/kyc")
@RequiredArgsConstructor
public class KycDocumentController {

    private final KycDocumentService kycDocumentService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<KycUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            Authentication authentication) throws Exception {
        
        KycUploadResponse response = kycDocumentService.uploadDocument(authentication.getName(), documentType, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentType}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<KycUploadResponse> getDocumentMetadata(
            @PathVariable DocumentType documentType,
            Authentication authentication) {
        
        KycUploadResponse metadata = kycDocumentService.getDocumentMetadata(authentication.getName(), documentType);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/{documentType}/view")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable DocumentType documentType,
            Authentication authentication) {
        
        byte[] data = kycDocumentService.getDocumentBytes(authentication.getName(), documentType);
        String contentType = kycDocumentService.getDocumentContentType(authentication.getName(), documentType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documentType.name() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
