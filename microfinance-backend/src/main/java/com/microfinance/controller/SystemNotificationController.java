package com.microfinance.controller;

import com.microfinance.dto.NotificationDTO;
import com.microfinance.entity.SystemNotification;
import com.microfinance.entity.User;
import com.microfinance.repository.SystemNotificationRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SystemNotificationController {

    private final SystemNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<SystemNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userOpt.get().getId());
        
        List<NotificationDTO> dtos = notifications.stream()
            .map(n -> NotificationDTO.builder()
                .id(n.getId())
                .message(n.getMessage())
                .linkUrl(n.getLinkUrl())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build())
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<SystemNotification> notifOpt = notificationRepository.findById(id);
        if (notifOpt.isEmpty() || !notifOpt.get().getUser().getId().equals(userOpt.get().getId())) {
            return ResponseEntity.notFound().build();
        }
        
        SystemNotification notification = notifOpt.get();
        notification.setRead(true);
        notificationRepository.save(notification);
        
        return ResponseEntity.ok().build();
    }
}
