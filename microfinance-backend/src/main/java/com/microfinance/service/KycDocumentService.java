package com.microfinance.service;

import com.microfinance.dto.KycUploadResponse;
import com.microfinance.entity.KycDocument;
import com.microfinance.entity.User;
import com.microfinance.enums.DocumentType;
import com.microfinance.repository.KycDocumentRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;

    @Transactional
    public KycUploadResponse uploadDocument(String username, DocumentType documentType, MultipartFile file) throws IOException {
        log.info("Uploading {} for user: {}", documentType, username);
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Enforce basic validation
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // Check for existing document of same type, overwrite if exists (but only if not verified)
        KycDocument document = kycDocumentRepository.findByUserIdAndDocumentType(user.getId(), documentType)
                .orElse(KycDocument.builder()
                        .user(user)
                        .documentType(documentType)
                        .build());

        if (document.isVerified()) {
            throw new IllegalStateException("Cannot overwrite a verified document.");
        }

        document.setFileData(file.getBytes());
        document.setContentType(file.getContentType());
        document.setFileName(file.getOriginalFilename());
        document.setFileSizeBytes(file.getSize());

        KycDocument saved = kycDocumentRepository.save(document);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public KycUploadResponse getDocumentMetadata(String username, DocumentType documentType) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return kycDocumentRepository.findByUserIdAndDocumentType(user.getId(), documentType)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public byte[] getDocumentBytes(String username, DocumentType documentType) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return kycDocumentRepository.findByUserIdAndDocumentType(user.getId(), documentType)
                .map(KycDocument::getFileData)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }
    
    @Transactional(readOnly = true)
    public String getDocumentContentType(String username, DocumentType documentType) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return kycDocumentRepository.findByUserIdAndDocumentType(user.getId(), documentType)
                .map(KycDocument::getContentType)
                .orElse("application/octet-stream");
    }

    private KycUploadResponse mapToDto(KycDocument document) {
        return KycUploadResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .fileName(document.getFileName())
                .fileSizeBytes(document.getFileSizeBytes())
                .verified(document.isVerified())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
