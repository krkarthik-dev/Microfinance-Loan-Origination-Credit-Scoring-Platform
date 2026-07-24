package com.microfinance.repository;

import com.microfinance.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SystemNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
