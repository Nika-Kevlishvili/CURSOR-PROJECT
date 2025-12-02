package bg.energo.phoenix.model.enums.receivable.collectionChannel;

import lombok.Getter;

@Getter
public enum CollectionChannelConditionVariableForParsing {
    PRODUCT_TYPE(CollectionChannelConditionVariableType.NUMBER),
    SERVICE_TYPE(CollectionChannelConditionVariableType.NUMBER),
    GRID_OPERATR(CollectionChannelConditionVariableType.NUMBER),
    SEGMENT(CollectionChannelConditionVariableType.NUMBER);

    private final CollectionChannelConditionVariableType type;

    CollectionChannelConditionVariableForParsing(CollectionChannelConditionVariableType type) {
        this.type = type;
    }
}
