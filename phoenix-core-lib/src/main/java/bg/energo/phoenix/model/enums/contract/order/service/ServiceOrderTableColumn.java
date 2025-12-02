package bg.energo.phoenix.model.enums.contract.order.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceOrderTableColumn {
    ORDER_NUMBER("orderNumber"), // and its registration date
    CUSTOMER_NAME("customerName"),
    SERVICE_NAME("serviceName"),
    DATE_OF_CREATION("createDate"),
    INVOICE_PAYMENT_TERM("invoicePaymentTerm"),
    INVOICE_MATURITY_DATE("invoiceMaturityDate"),
    INVOICE_PAID("invoicePaid"),
    ACCOUNT_MANAGER("accountManager"),
    ORDER_VALUE("valueOfTheOrder");

    private final String value;
}