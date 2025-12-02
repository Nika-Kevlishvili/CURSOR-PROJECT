package bg.energo.phoenix.model.enums.product.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductObligatory {
    OBLIGATORY_CONDITION("Obligatory Condition"),
    AT_LEAST_ONE_CONDITION("At least one condition");

    private final String description;
}
