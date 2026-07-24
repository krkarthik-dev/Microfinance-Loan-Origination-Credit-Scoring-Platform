package com.microfinance.controller;

import com.microfinance.dto.StaffCreationRequestDTO;
import com.microfinance.dto.StaffDTO;
import com.microfinance.dto.StaffPasswordResponseDTO;
import com.microfinance.service.AdminStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffDTO>> getAllStaff() {
        return ResponseEntity.ok(adminStaffService.getAllStaff());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffPasswordResponseDTO> createStaff(@RequestBody StaffCreationRequestDTO request) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminStaffService.createStaff(request, adminUsername));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableStaff(@PathVariable Long id) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        adminStaffService.disableStaff(id, adminUsername);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> enableStaff(@PathVariable Long id) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        adminStaffService.enableStaff(id, adminUsername);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffPasswordResponseDTO> resetStaffPassword(@PathVariable Long id) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminStaffService.resetStaffPassword(id, adminUsername));
    }
}
