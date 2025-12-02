package bg.energo.phoenix.model.enums.product.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductAllowSalesUnder {
    CONCLUDED_CONTRACT("Concluded Contract"),
    ACTIVATED_CONTRACT("Activated Contract");

    private final String description;
}
