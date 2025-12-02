package bg.energo.phoenix.model.enums.billing.billings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingRunConditionVariableName {
    CONTRACT(BillingRunConditionVariableType.STRING),
    PRODUCT(BillingRunConditionVariableType.NUMBER),
    SERVICE(BillingRunConditionVariableType.NUMBER),
    PRODUCT_TYPE(BillingRunConditionVariableType.NUMBER),
    SERVICE_TYPE(BillingRunConditionVariableType.NUMBER),
    CUSTOMER_TYPE(BillingRunConditionVariableType.STRING),
    GRID_OP(BillingRunConditionVariableType.NUMBER),
    CONTRACT_TYPE(BillingRunConditionVariableType.STRING),
    MEASUREMENT_TYPE(BillingRunConditionVariableType.STRING),
    VOLTAGE_LEVEL(BillingRunConditionVariableType.STRING),
    PURPOSE_OF_CONSUMPTION(BillingRunConditionVariableType.STRING),
    INTERIM_ADVANCE_PAYMENT(BillingRunConditionVariableType.STRING),
    CUSTOMER_SEGMENT(BillingRunConditionVariableType.NUMBER),
    CUSTOMER_NUMBER(BillingRunConditionVariableType.NUMBER),
    POD_ADDITIONAL_PARAMETER(BillingRunConditionVariableType.NUMBER);

    private final BillingRunConditionVariableType type;
}
