package com.TravelBuddy.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Normal DTO class representing AI-powered disaster risk assessment results.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskAssessmentResponse {
    private String riskLevel;
    private String riskSummary;
    private String recommendedAction;
    private boolean inSafeZone;
    private EvacuationRouteOption footOption;
    private EvacuationRouteOption vehicleOption;
    private List<String> emergencyChecklist;
    private boolean aiGenerated;
    private String modelUsed;
    private String personalizedFor;
}
