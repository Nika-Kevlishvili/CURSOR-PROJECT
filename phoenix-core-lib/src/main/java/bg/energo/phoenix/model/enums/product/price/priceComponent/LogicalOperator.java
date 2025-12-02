package bg.energo.phoenix.model.enums.product.price.priceComponent;

import lombok.Getter;

@Getter
public enum LogicalOperator {
    AND("AND","&&"),
    OR("OR","||"),
    NOT_EQUALS("<>","!="),
    NOT("NOT","!");

    private final String originalValue;
    private final String value;

    LogicalOperator(String originalValue, String value) {
        this.originalValue = originalValue;
        this.value = value;
    }
}
