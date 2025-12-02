package bg.energo.phoenix.model.enums.customer.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum customerRelatedPaymentSortFields {
    PAYMENT_NUMBER("payment_number"),
    BILLING_GROUP("cbg.group_number"),
    OUTGOING_DOCUMENT_TYPE("outgoing_document_type"),
    PAYMENT_CHANNEL("ccd.name"),
    PAYMENT_PACKAGE("pp.id"),
    PAYMENT_DATE("payment_date"),
    INITIAL_AMOUNT("initial_amount"),
    CURRENT_AMOUNT("current_amount"),
    CUSTOMER_PAYMENT_ID("id");

    private final String value;

}
