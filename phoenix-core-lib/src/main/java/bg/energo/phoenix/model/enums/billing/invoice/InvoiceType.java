package bg.energo.phoenix.model.enums.billing.invoice;

import lombok.Getter;

@Getter
public enum InvoiceType {
    MANUAL("M"),
    STANDARD("S"),
    INTERIM_AND_ADVANCE_PAYMENT("INT"),
    CORRECTION("C"),
    REVERSAL("R"),
    RECONNECTION("REC");

    private final String prefix;

    InvoiceType(String prefix) {
        this.prefix = prefix;
    }
}
