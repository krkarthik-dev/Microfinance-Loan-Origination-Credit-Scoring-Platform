package com.microfinance.enums;

/**
 * Loan application lifecycle states.
 *
 * <p>State transitions are strictly controlled and must follow the defined flow:
 * <pre>
 * DRAFT → SUBMITTED → RISK_ASSESSMENT → UNDER_REVIEW
 *                                            ↓
 *                               APPROVED / REJECTED / ESCALATED
 *                                            ↓ (if ESCALATED)
 *                               FINAL_APPROVED / FINAL_REJECTED
 * </pre>
 */
public enum ApplicationStatus {

    /** Application created but not yet submitted by the borrower. */
    DRAFT,

    /** Formally submitted — awaiting ML credit scoring. */
    SUBMITTED,

    /** ML model is executing asynchronously. */
    RISK_ASSESSMENT,

    /** Score received; assigned to a loan officer for review. */
    UNDER_REVIEW,

    /** Loan officer approved the application. */
    APPROVED,

    /** Loan officer rejected the application. */
    REJECTED,

    /** Loan officer escalated to manager for final decision. */
    ESCALATED,

    /** Manager gave final approval on escalated application. */
    FINAL_APPROVED,

    /** Manager gave final rejection on escalated application. */
    FINAL_REJECTED
}
