package com.microfinance.service;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.entity.LoanProduct;
import com.microfinance.repository.LoanApplicationRepository;
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
    private final LoanApplicationRepository loanApplicationRepository;

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
    public LoanProductDTO createProduct(LoanProductDTO dto) {
        validateProductParameters(dto);
        if (loanProductRepository.existsByProductName(dto.getProductName())) {
            throw new IllegalArgumentException("A loan product with this name already exists");
        }
        LoanProduct product = LoanProduct.builder()
                .productName(dto.getProductName())
                .description(dto.getDescription() != null ? dto.getDescription() : "")
                .minAmount(dto.getMinAmount())
                .maxAmount(dto.getMaxAmount())
                .interestRatePa(dto.getInterestRatePa())
                .minTenureMonths(dto.getMinTenureMonths())
                .maxTenureMonths(dto.getMaxTenureMonths())
                .processingFeePct(dto.getProcessingFeePct() != null ? dto.getProcessingFeePct() : BigDecimal.ZERO)
                .active(dto.isActive())
                .build();
        return mapToDTO(loanProductRepository.save(product));
    }

    @Transactional
    public LoanProductDTO updateProduct(Long id, LoanProductDTO dto) {
        validateProductParameters(dto);
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found"));

        if (!product.getProductName().equalsIgnoreCase(dto.getProductName()) &&
                loanProductRepository.existsByProductName(dto.getProductName())) {
            throw new IllegalArgumentException("A loan product with this name already exists");
        }

        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        product.setMinAmount(dto.getMinAmount());
        product.setMaxAmount(dto.getMaxAmount());
        product.setInterestRatePa(dto.getInterestRatePa());
        product.setMinTenureMonths(dto.getMinTenureMonths());
        product.setMaxTenureMonths(dto.getMaxTenureMonths());
        if (dto.getProcessingFeePct() != null) {
            product.setProcessingFeePct(dto.getProcessingFeePct());
        }
        product.setActive(dto.isActive());
        return mapToDTO(loanProductRepository.save(product));
    }

    @Transactional
    public LoanProductDTO toggleStatus(Long id, boolean active) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found"));
        product.setActive(active);
        return mapToDTO(loanProductRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found"));
        if (loanApplicationRepository.existsByLoanProductId(id)) {
            throw new IllegalStateException("Cannot permanently delete a loan product linked to historical applications. Please toggle status to Inactive instead.");
        }
        loanProductRepository.delete(product);
    }

    @Transactional
    public LoanProductDTO updateInterestRate(Long id, BigDecimal newRate) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found"));
        
        product.setInterestRatePa(newRate);
        return mapToDTO(loanProductRepository.save(product));
    }

    private void validateProductParameters(LoanProductDTO dto) {
        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (dto.getMinAmount() == null || dto.getMaxAmount() == null ||
                dto.getMaxAmount().compareTo(dto.getMinAmount()) < 0 ||
                dto.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount limits: min amount must be > 0 and max amount >= min amount");
        }
        if (dto.getMinTenureMonths() == null || dto.getMaxTenureMonths() == null ||
                dto.getMaxTenureMonths() < dto.getMinTenureMonths() ||
                dto.getMinTenureMonths() <= 0) {
            throw new IllegalArgumentException("Invalid tenure limits: min tenure must be > 0 and max tenure >= min tenure");
        }
        if (dto.getInterestRatePa() == null || dto.getInterestRatePa().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest rate must be greater than zero");
        }
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
