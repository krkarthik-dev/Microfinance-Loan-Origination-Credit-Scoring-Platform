package com.microfinance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub controller for Applicant endpoints.
 * Secured by SecurityConfig to allow ROLE_APPLICANT.
 */
@RestController
@RequestMapping("/api/applicant")
public class ApplicantController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getApplicantDashboard() {
        return ResponseEntity.ok("Applicant Dashboard Data");
    }
}
