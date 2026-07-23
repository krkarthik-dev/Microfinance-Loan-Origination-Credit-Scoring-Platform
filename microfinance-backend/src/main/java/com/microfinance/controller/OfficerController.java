package com.microfinance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub controller for Officer endpoints.
 * Secured by SecurityConfig to allow ROLE_OFFICER and ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/officer")
public class OfficerController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getOfficerDashboard() {
        return ResponseEntity.ok("Officer Dashboard Data");
    }
}
