package com.microfinance.enums;

/**
 * Decision types that a Loan Officer or Manager can make on an application.
 * Each decision is recorded as an immutable entry in loan_decisions.
 */
public enum DecisionType {

    /** Application is approved — funds can be disbursed. */
    APPROVED,

    /** Application is rejected — borrower is notified with reasons. */
    REJECTED,

    /** Application is escalated to manager for final decision. */
    ESCALATED
}
