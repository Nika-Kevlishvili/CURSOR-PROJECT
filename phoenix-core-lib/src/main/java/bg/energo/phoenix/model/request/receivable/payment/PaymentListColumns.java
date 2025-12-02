package bg.energo.phoenix.model.request.receivable.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentListColumns {

    PAYMENT_NUMBER("id"),
    BILLING_GROUP("billingGroup"),
    OUTGOING_DOCUMENT_TYPE("outgoing_document_type"),
    COLLECTION_CHANNEL("paymentChannel"),
    PAYMENT_PACKAGE("paymentPackage"),
    PAYMENT_DATE("payment_date"),
    INITIAL_AMOUNT("initialAmount"),
    CURRENT_AMOUNT("currentAmount"),
    CUSTOMER_PAYMENT_ID("id");

    private final String value;
}
