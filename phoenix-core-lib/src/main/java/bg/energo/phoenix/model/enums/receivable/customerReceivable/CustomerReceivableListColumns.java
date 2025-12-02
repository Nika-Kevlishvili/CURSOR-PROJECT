package bg.energo.phoenix.model.enums.receivable.customerReceivable;

import lombok.Getter;

@Getter
public enum CustomerReceivableListColumns {
    ID("id"),
    CUSTOMER("customer_id"),
    BILLING_GROUP("contract_billing_group_id"),
    ALTERNATIVE_RECIPIENT_OF_AN_INVOICE("alt_invoice_recipient_customer_id"),
    INITIAL_AMOUNT("initialAmount"),
    CURRENT_AMOUNT("currentAmount"),
    OCCURRENCE_DATE("occurrence_date"),
    DUE_DATE("due_date");

    private final String value;

    private CustomerReceivableListColumns(String value) {
        this.value = value;
    }
}
