package bg.energo.phoenix.service.riskList.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@NoArgsConstructor
public class RiskListBasicInfoResponse {

    private RiskListDecision decision;

    private List<String> recommendations;

    public RiskListBasicInfoResponse(String decision, List<String> recommendations) {
        this.decision = StringUtils.isEmpty(decision) ? null : RiskListDecision.valueOf(decision.toUpperCase());
        this.recommendations = recommendations;
    }

    public void setDecision(String decision) {
        this.decision = RiskListDecision.valueOf(decision.toUpperCase());
    }
}
