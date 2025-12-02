package bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisconnectionPowerSupplyRequestsConditionVariableName {

    CONTRACT(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    PRODUCT(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    SERVICE(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    PRODUCT_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    SERVICE_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    CUSTOMER_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    POD_GRID_OP(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    POD_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    CONTRACT_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    POD_MEASUREMENT_TYPE(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    POD_VOLTAGE_LEVEL(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    PURPOSE_OF_CONSUMPTION(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    INTERIM_ADVANCE_PAYMENT(DisconnectionPowerSupplyRequestsConditionVariableType.STRING),
    SEGMENT(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    CUSTOMER_NUMBER(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER),
    COLLECTION_CHANNEL(DisconnectionPowerSupplyRequestsConditionVariableType.NUMBER);

    private final DisconnectionPowerSupplyRequestsConditionVariableType type;

}
