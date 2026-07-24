package com.microfinance.controller;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminLoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanProductDTO>> getAllProducts() {
        return ResponseEntity.ok(loanProductService.getAllProducts());
    }

    @PutMapping("/{id}/apr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanProductDTO> updateApr(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newRate = request.get("interestRatePa");
        if (newRate == null) {
            throw new IllegalArgumentException("interestRatePa is required");
        }
        return ResponseEntity.ok(loanProductService.updateInterestRate(id, newRate));
    }
}
