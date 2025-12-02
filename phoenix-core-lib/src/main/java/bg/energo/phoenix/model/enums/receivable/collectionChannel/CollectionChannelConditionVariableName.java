package bg.energo.phoenix.model.enums.receivable.collectionChannel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionChannelConditionVariableName {
    // reminder: don't include any "OR" or "AND" in the names, parser is getting confused
    CONTRACT(CollectionChannelConditionVariableType.STRING),
    PRODUCT(CollectionChannelConditionVariableType.NUMBER),
    SERVICE(CollectionChannelConditionVariableType.NUMBER),
    PRODUCT_TYPE(CollectionChannelConditionVariableType.NUMBER),
    SERVICE_TYPE(CollectionChannelConditionVariableType.NUMBER),
    CUSTOMER_TYPE(CollectionChannelConditionVariableType.STRING),
    POD_GRID_OP(CollectionChannelConditionVariableType.NUMBER),
    CONTRACT_TYPE(CollectionChannelConditionVariableType.STRING),
    POD_MEASUREMENT_TYPE(CollectionChannelConditionVariableType.STRING),
    POD_VOLTAGE_LEVEL(CollectionChannelConditionVariableType.STRING),
    PURPOSE_OF_CONSUMPTION(CollectionChannelConditionVariableType.STRING),
    POD_TYPE(CollectionChannelConditionVariableType.STRING),
    INTERIM_ADVANCE_PAYMENT(CollectionChannelConditionVariableType.STRING),
    CUSTOMER_SEGMENT(CollectionChannelConditionVariableType.NUMBER),
    CUSTOMER_NUMBER(CollectionChannelConditionVariableType.NUMBER);

    private final CollectionChannelConditionVariableType type;
}
