package com.microfinance.dto;

import com.microfinance.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KycUploadResponse {
    private Long id;
    private DocumentType documentType;
    private String fileName;
    private Long fileSizeBytes;
    private boolean verified;
    private LocalDateTime uploadedAt;
}
