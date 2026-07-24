package com.microfinance.service;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.entity.LoanProduct;
import com.microfinance.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    @Transactional(readOnly = true)
    public List<LoanProductDTO> getActiveProducts() {
        return loanProductRepository.findByActiveTrueOrderByProductNameAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanProductDTO> getAllProducts() {
        return loanProductRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanProductDTO updateInterestRate(Long id, BigDecimal newRate) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found"));
        
        product.setInterestRatePa(newRate);
        return mapToDTO(loanProductRepository.save(product));
    }

    private LoanProductDTO mapToDTO(LoanProduct product) {
        return LoanProductDTO.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .interestRatePa(product.getInterestRatePa())
                .minTenureMonths(product.getMinTenureMonths())
                .maxTenureMonths(product.getMaxTenureMonths())
                .processingFeePct(product.getProcessingFeePct())
                .active(product.isActive())
                .build();
    }
}
