package com.microfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for deserializing the JSON output
 * from the Python ML Script.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlScoringResponse {
    private Integer creditScore;
    private Double probabilityOfDefault;
    private String error;
}
