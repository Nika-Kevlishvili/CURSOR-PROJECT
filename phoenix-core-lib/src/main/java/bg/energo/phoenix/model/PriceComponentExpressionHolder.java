package bg.energo.phoenix.model;

import bg.energo.phoenix.model.enums.product.price.priceComponent.MainLogicalOperator;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceComponentExpressionHolder {
    private MainLogicalOperator operator;
    private String condition;
    private String statement;

}
