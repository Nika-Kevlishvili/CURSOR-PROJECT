package bg.energo.phoenix.model.enums.product.price.priceComponent;

import lombok.Getter;

@Getter
public enum PriceComponentConditionVariableForParsing {
    CUSTOMER_SEGMENT(PriceComponentConditionVariableType.NUMBER),
    CUSTOMER_PREFERENCES(PriceComponentConditionVariableType.NUMBER),
    POD_COUNTRY(PriceComponentConditionVariableType.NUMBER),
    POD_REGION(PriceComponentConditionVariableType.NUMBER),
    POD_POPULATED_PLACE(PriceComponentConditionVariableType.NUMBER),
    POD_GRID_OPERATOR(PriceComponentConditionVariableType.NUMBER),
    POD_MEASUREMENT_TYPE(PriceComponentConditionVariableType.NUMBER),
    POD_PROVIDED_POWER(PriceComponentConditionVariableType.NUMBER),
    POD_MULTIPLICATOR(PriceComponentConditionVariableType.NUMBER),
    RISK_ASSESSMENT_ADDITIONAL_CONDITIONS(PriceComponentConditionVariableType.NUMBER),
    CONTRACT_CAMPAIGN(PriceComponentConditionVariableType.NUMBER);

    private final PriceComponentConditionVariableType type;

    PriceComponentConditionVariableForParsing(PriceComponentConditionVariableType type) {
        this.type = type;
    }
}
