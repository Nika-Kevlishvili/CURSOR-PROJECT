package bg.energo.phoenix.model.request.receivable.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentSearchFields {

    ALL("ALL"),
    PAYMENT_NUMBER("PAYMENT_NUMBER"),
    CUSTOMER_IDENTIFIER("CUSTOMER_IDENTIFIER"),
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    PAYMENT_PACKAGE("PAYMENT_PACKAGE"),
    OUTGOING_DOCUMENT("OUTGOING_DOCUMENT");

    private final String value;
}
