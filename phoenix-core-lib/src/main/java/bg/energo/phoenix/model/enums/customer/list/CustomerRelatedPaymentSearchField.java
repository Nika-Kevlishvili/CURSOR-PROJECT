package bg.energo.phoenix.model.enums.customer.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerRelatedPaymentSearchField {
    ALL("ALL"),
    PAYMENT_NUMBER("PAYMENT_NUMBER"),
    BILLING_GROUP("BILLING_GROUP"),
    PAYMENT_PACKAGE("PAYMENT_PACKAGE"),
    OUTGOING_DOCUMENT("OUTGOING_DOCUMENT");

    private final String value;
}
