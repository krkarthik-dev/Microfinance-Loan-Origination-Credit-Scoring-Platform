package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable, append-only audit trail for all significant platform events.
 *
 * <p>Audit logs are NEVER updated or deleted — only inserted. Every important
 * action (loan submission, status change, decision, KYC verification) is
 * recorded here for traceability and compliance.
 *
 * <p>{@code performedBy} is nullable to support SYSTEM-generated events
 * (e.g., ML scoring completion triggered by the async event listener).
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Type of entity affected — e.g., LOAN_APPLICATION, USER, LOAN_PRODUCT */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** Primary key of the affected entity */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Action performed — e.g., CREATED, SUBMITTED, APPROVED, STATUS_CHANGED */
    @Column(nullable = false, length = 50)
    private String action;

    /** User who triggered the action; null for SYSTEM events */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    /** Previous state serialized as JSON string */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** New state serialized as JSON string */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Client IP address for security traceability */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
