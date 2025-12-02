package bg.energo.phoenix.model.enums.receivable.reminder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReminderConditionVariableName {

    CONTRACT(ReminderConditionVariableType.STRING),
    PRODUCT(ReminderConditionVariableType.NUMBER),
    SERVICE(ReminderConditionVariableType.NUMBER),
    PRODUCT_TYPE(ReminderConditionVariableType.NUMBER),
    SERVICE_TYPE(ReminderConditionVariableType.NUMBER),
    CUSTOMER_TYPE(ReminderConditionVariableType.STRING),
    POD_GRID_OP(ReminderConditionVariableType.NUMBER),
    POD_TYPE(ReminderConditionVariableType.STRING),
    CONTRACT_TYPE(ReminderConditionVariableType.STRING),
    POD_MEASUREMENT_TYPE(ReminderConditionVariableType.STRING),
    POD_VOLTAGE_LEVEL(ReminderConditionVariableType.STRING),
    PURPOSE_OF_CONSUMPTION(ReminderConditionVariableType.STRING),
    INTERIM_ADVANCE_PAYMENT(ReminderConditionVariableType.STRING),
    SEGMENT(ReminderConditionVariableType.NUMBER),
    CUSTOMER_NUMBER(ReminderConditionVariableType.NUMBER);

    private final ReminderConditionVariableType type;
}
