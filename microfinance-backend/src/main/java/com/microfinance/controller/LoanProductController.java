package com.microfinance.controller;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    public ResponseEntity<List<LoanProductDTO>> getActiveProducts() {
        return ResponseEntity.ok(loanProductService.getActiveProducts());
    }
}
