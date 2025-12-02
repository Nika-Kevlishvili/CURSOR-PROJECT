package bg.energo.phoenix.model.enums.customer.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerRelatedOrdersTableColumn {
    ORDER_NUMBER("orderNumber"),
    ORDER_TYPE("orderType"),
    ORDER_STATUS("status"),
    CREATION_DATE("createDateForSort"),
    INVOICE_PAID("invoicePaid"),
    VALUE_OF_THE_ORDER("orderValue"),
    INVOICE_PAYMENT_TERM("invoicePaymentTerm"),
    INVOICE_MATURITY_DATE("invoiceMaturityDate");

    private final String value;
}
