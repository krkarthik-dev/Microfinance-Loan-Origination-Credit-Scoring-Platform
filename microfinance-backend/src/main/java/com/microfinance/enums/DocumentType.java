package com.microfinance.enums;

/**
 * Types of KYC documents uploaded by borrowers.
 * Actual files are stored in AWS S3; only the type and S3 reference is stored in the DB.
 */
public enum DocumentType {

    /** Aadhaar card — 12-digit government-issued identity document. */
    AADHAAR,

    /** PAN card — 10-character alphanumeric tax identity card. */
    PAN
}
