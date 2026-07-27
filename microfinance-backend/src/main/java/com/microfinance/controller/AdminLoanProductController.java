package com.microfinance.controller;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanProductDTO> createProduct(@RequestBody LoanProductDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanProductService.createProduct(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanProductDTO> updateProduct(@PathVariable Long id, @RequestBody LoanProductDTO dto) {
        return ResponseEntity.ok(loanProductService.updateProduct(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanProductDTO> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active == null) {
            throw new IllegalArgumentException("active status is required");
        }
        return ResponseEntity.ok(loanProductService.toggleStatus(id, active));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        loanProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
