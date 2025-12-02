package bg.energo.phoenix.model.request.receivable.customerLiability;

public enum CustomerLiabilityListColumns {
    ID("id"),
    CUSTOMER("customerId"),
    BILLING_GROUP("billingGroup"),
    ALTERNATIVE_RECIPIENT_OF_AN_INVOICE("alternativeRecipientOfAnInvoice"),
    INITIAL_AMOUNT("initialAmount"),
    CURRENT_AMOUNT("currentAmount"),
    DUE_DATE("dueDate"),
    PODS("pods"),
    OCCURRENCE_DATE("occurrenceDate");

    private final String value;

    CustomerLiabilityListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
