package bg.energo.phoenix.model.enums.product.term.terms.filter;

import lombok.Getter;

/**
 * <h1>TermsParameterFilterField</h1>
 * {@link #ALL} to search in all fields
 * {@link #NAME} to search in only Name field
 * {@link #PAYMENT_TERM_NAME} to search in only PAYMENT TERM NAME field
 */
public enum TermsParameterFilterField {
    ALL("ALL"),
    NAME("NAME"),
    PAYMENT_TERM_NAME("PAYMENT_TERM_NAME");
    @Getter
    private final String value;

    TermsParameterFilterField(String value) {
        this.value = value;
    }
}
