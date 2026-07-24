package com.microfinance.enums;

/**
 * Loan application lifecycle states.
 *
 * <p>State transitions are strictly controlled and follow the production loan flow:
 * DRAFT, SUBMITTED, KYC, review, approval, closing, and repayment.
 */
public enum ApplicationStatus {

    DRAFT,
    SUBMITTED,
    PENDING_KYC,
    UNDER_REVIEW,
    INFO_REQUESTED,
    PENDING_MANAGER_APPROVAL,
    APPROVED,
    CLOSING,
    ACTIVE_REPAYMENT,
    REJECTED,
    WITHDRAWN
}
