package bg.energo.phoenix.model.enums.product.price.priceComponent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceComponentConditionVariableName {
    // reminder: don't include any "OR" or "AND" in the names, parser is getting confused
    CUSTOMER_TYPE(PriceComponentConditionVariableType.STRING),
    CUSTOMER_SEGMENT(PriceComponentConditionVariableType.NUMBER),
    CUSTOMER_PREFERENCES(PriceComponentConditionVariableType.NUMBER),
    POD_COUNTRY(PriceComponentConditionVariableType.NUMBER),
    POD_REGION(PriceComponentConditionVariableType.NUMBER),
    POD_POPULATED_PLACE(PriceComponentConditionVariableType.NUMBER),
    POD_GRID_OP(PriceComponentConditionVariableType.NUMBER),
    POD_VOLTAGE_LEVEL(PriceComponentConditionVariableType.STRING),
    POD_MEASUREMENT_TYPE(PriceComponentConditionVariableType.STRING),
    POD_PROVIDED_POWER(PriceComponentConditionVariableType.NUMBER),
    POD_MULTIPLIER(PriceComponentConditionVariableType.NUMBER),
    DIRECT_DEBIT(PriceComponentConditionVariableType.STRING),
    CONTRACT_SUB_STATUS_IN_PERPETUITY(PriceComponentConditionVariableType.STRING),
    ACTIVE_POWER_SUPPLY_TERMINATION(PriceComponentConditionVariableType.STRING),
    CONTRACT_TYPE(PriceComponentConditionVariableType.STRING),
    RISK_ASSESSMENT_ADDITIONAL_CONDITIONS(PriceComponentConditionVariableType.NUMBER),
    CONTRACT_CAMPAIGN(PriceComponentConditionVariableType.NUMBER),
    PURPOSE_OF_CONSUMPTION(PriceComponentConditionVariableType.STRING),
    // Max date - contract activation date, it means we should search for > “Months difference between Max Date of Billing Run and Contract Activation Date”
    MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE(PriceComponentConditionVariableType.NUMBER),
    // Current date (Billing run Execution Date) - contract activation date, it means we should search for > “Months difference  between Current Date and Contract Activation Date”
    MONTHS_DIFFERENCE_BETWEEN_CURRENT_DATE_CONTRACT_ACTIVATION_DATE(PriceComponentConditionVariableType.NUMBER),
    POD_ADDITIONAL_PARAMETERS(PriceComponentConditionVariableType.NUMBER);

    private final PriceComponentConditionVariableType type;
}
