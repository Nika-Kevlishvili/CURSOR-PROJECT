package bg.energo.phoenix.model.entity.product.penalty.penalty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PenaltySearchField {
    ALL("ALL"),
    NAME("NAME"),
    PAYMENT_TERM_NAME("PAYMENT_TERM_NAME");

    private final String value;
}
