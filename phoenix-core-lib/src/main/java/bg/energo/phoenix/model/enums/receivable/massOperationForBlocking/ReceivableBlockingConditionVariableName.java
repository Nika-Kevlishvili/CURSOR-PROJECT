package bg.energo.phoenix.model.enums.receivable.massOperationForBlocking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReceivableBlockingConditionVariableName {

    CONTRACT(ReceivableBlockingConditionVariableType.STRING),
    PRODUCT(ReceivableBlockingConditionVariableType.NUMBER),
    SERVICE(ReceivableBlockingConditionVariableType.NUMBER),
    PRODUCT_TYPE(ReceivableBlockingConditionVariableType.NUMBER),
    SERVICE_TYPE(ReceivableBlockingConditionVariableType.NUMBER),
    CUSTOMER_TYPE(ReceivableBlockingConditionVariableType.STRING),
    POD_GRID_OP(ReceivableBlockingConditionVariableType.NUMBER),
    POD_TYPE(ReceivableBlockingConditionVariableType.STRING),
    CONTRACT_TYPE(ReceivableBlockingConditionVariableType.STRING),
    POD_MEASUREMENT_TYPE(ReceivableBlockingConditionVariableType.STRING),
    POD_VOLTAGE_LEVEL(ReceivableBlockingConditionVariableType.STRING),
    PURPOSE_OF_CONSUMPTION(ReceivableBlockingConditionVariableType.STRING),
    INTERIM_ADVANCE_PAYMENT(ReceivableBlockingConditionVariableType.STRING),
    SEGMENT(ReceivableBlockingConditionVariableType.NUMBER),
    CUSTOMER_NUMBER(ReceivableBlockingConditionVariableType.NUMBER),
    COLLECTION_CHANNEL(ReceivableBlockingConditionVariableType.NUMBER);

    private final ReceivableBlockingConditionVariableType type;

}
