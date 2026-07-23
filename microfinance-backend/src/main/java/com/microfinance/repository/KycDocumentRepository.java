package com.microfinance.repository;

import com.microfinance.entity.KycDocument;
import com.microfinance.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    Optional<KycDocument> findByUserIdAndDocumentType(Long userId, DocumentType documentType);
    List<KycDocument> findByUserId(Long userId);
}
