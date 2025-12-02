package bg.energo.phoenix.model.enums.product.term.terms;

import lombok.Getter;

public enum TermStatus {
    ACTIVE("ACTIVE"),
    DELETED("DELETED");

    @Getter
    private final String value;

    TermStatus(String value) {
        this.value = value;
    }
}
