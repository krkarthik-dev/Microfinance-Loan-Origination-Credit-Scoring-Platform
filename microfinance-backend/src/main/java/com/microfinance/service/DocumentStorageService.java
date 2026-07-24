package com.microfinance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * A central service for handling file extraction and storage logic.
 * Currently uses PostgreSQL (BYTEA) as requested, but acts as an abstraction layer
 * to avoid duplication in KycDocumentService and LoanSubmissionService.
 */
@Service
public class DocumentStorageService {

    public byte[] extractBytes(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return file.getBytes();
    }

    public String extractContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "application/octet-stream";
        }
        return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    }

    public String extractFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "unknown";
        }
        return file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }
}
