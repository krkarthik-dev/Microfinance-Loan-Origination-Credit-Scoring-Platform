package com.microfinance.enums;

/**
 * ML-generated risk classification for a loan application.
 * Used by loan officers to prioritize their review queue.
 */
public enum RiskTier {

    /** Low probability of default — generally safe to approve. */
    LOW,

    /** Moderate risk — requires careful review by loan officer. */
    MEDIUM,

    /** High probability of default — likely to be escalated or rejected. */
    HIGH
}
