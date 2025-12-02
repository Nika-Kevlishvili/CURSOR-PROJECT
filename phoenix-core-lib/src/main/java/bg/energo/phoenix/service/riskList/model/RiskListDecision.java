package bg.energo.phoenix.service.riskList.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RiskListDecision {
    PERMIT("permit"),
    DENY("deny"),
    MISSING("missing");

    private final String value;
}
