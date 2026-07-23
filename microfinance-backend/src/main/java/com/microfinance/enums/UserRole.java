package com.microfinance.enums;

/**
 * Platform roles for Role-Based Access Control (RBAC).
 * Each role maps to a completely isolated set of screens and API endpoints.
 */
public enum UserRole {

    /** Borrower — can apply for loans, track applications, upload KYC. */
    ROLE_APPLICANT,

    /** Loan Officer — reviews applications, makes first-level decisions, creates walk-in apps. */
    ROLE_OFFICER,

    /** Manager / Admin — handles escalations, manages staff, configures products. */
    ROLE_ADMIN
}
