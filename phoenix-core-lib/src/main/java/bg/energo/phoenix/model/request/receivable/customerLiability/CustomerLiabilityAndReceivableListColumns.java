package bg.energo.phoenix.model.request.receivable.customerLiability;

public enum CustomerLiabilityAndReceivableListColumns {
    ID("id_raw"),
    OUTGOING_DOCUMENT("outgoing_document"),
    CREATION_DATE("create_date_raw"),
    DUE_DATE("due_date_raw"),
    BILLING_GROUP("billingGroup"),
    CONTRACT_ORDER("contract_order"),
    INITIAL_AMOUNT("initial_amount"),
    CURRENT_AMOUNT("current_amount"),
    POD("pods"),
    POD_ADDRESS("address"),
    OFFSETTING("offseting");

    private final String value;

    CustomerLiabilityAndReceivableListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
