package bg.energo.phoenix.model.enums.product.term.termsGroup;

import lombok.Getter;

public enum TermsGroupParameterFilterField {
    ALL("ALL"),
    TERMS_GROUP_NAME("TERMS_GROUP_NAME"),
    TERMS_NAME("TERMS_NAME"),
    PAYMENT_TERM_NAME("PAYMENT_TERM_NAME");
    @Getter
    private final String value;

    TermsGroupParameterFilterField(String value) {
        this.value = value;
    }
}
