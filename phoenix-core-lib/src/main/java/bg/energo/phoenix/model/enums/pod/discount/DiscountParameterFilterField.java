package bg.energo.phoenix.model.enums.pod.discount;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DiscountParameterFilterField {
    ALL("ALL"),
    ID("ID"),
    POD_IDENTIFIER("POD_IDENTIFIER"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER");

    @Getter
    private final String value;
}
