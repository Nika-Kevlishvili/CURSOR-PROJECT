package bg.energo.phoenix.model.enums.product.product.list;

import lombok.Getter;

public enum ProductParameterFilterField {
    ALL("ALL"),
    NAME("NAME"),
    GROUP("GROUP"),
    PENALTIES_GROUP_NAME("PENALTYGROUP"),
    PENALTY_NAME("PENALTY"),
    TERMINATIONS_GROUP_NAME("TERMINATIONGROUP"),
    TERMINATION_TERMS_GROUP_NAME("TERMINATION"),
    TERM_NAME("TERM"),
    TERMS_GROUP_NAME("TERMS_GROUP_NAME"),
    INTERIM_ADVANCED_PAYMENTS_GROUP_NAME("INTERIMADVANCEPAYMENTGROUP"),
    INTERIM_ADVANCED_PAYMENTS_NAME("INTERIMADVANCEPAYMENT"),
    PRICE_COMPONENTS_GROUP_NAME("PRICECOMPONENTGROUP"),
    PRICE_COMPONENT("PRICECOMPONENT");

    @Getter
    private final String value;

    ProductParameterFilterField(String value) {
        this.value = value;
    }
}
