package bg.energo.phoenix.model.enums.product.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceObligationCondition {
    OBLIGATORY_CONDITION("Obligatory Condition"),
    AT_LEAST_ONE_CONDITION("At least one condition");

    private final String description;
}
