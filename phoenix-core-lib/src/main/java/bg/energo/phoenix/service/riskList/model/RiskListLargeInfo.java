package bg.energo.phoenix.service.riskList.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RiskListLargeInfo {

    private String consumption;

    private String status;

    @JsonProperty("final_decision")
    private String finalDecision;

    @JsonProperty("contract_changes_recommendations")
    private List<String> contractChangesRecommendations;

    private String date;

    private String evaluation;

    private List<BigDecimal> threshold;

}
