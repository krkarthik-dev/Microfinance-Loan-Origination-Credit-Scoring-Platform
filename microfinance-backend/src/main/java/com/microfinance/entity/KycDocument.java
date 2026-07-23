package com.microfinance.entity;

import com.microfinance.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Metadata and S3 reference for a KYC document uploaded by a borrower.
 *
 * <p>Actual document bytes are stored in AWS S3. Only the S3 key, bucket,
 * and verification status are persisted here. This avoids storing sensitive
 * binary data in the relational database.
 */
@Entity
@Table(name = "kyc_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** The borrower who uploaded this document */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, length = 255)
    private String s3Key;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    /** The officer who verified this document (null if not yet verified) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
