package bg.energo.phoenix.model.enums.billing.billings;

public enum BillingRunListColumns {
    ID("id"),
    NUMBER("billing_number"),
    AUTOMATION("run_stage"),
    CRITERIA("run_stage"),
    BILLING_CRITERIA("criteria"),
    APPLICATION_LEVEL("application_level"),
    BILLING_TYPE("type"),
    STATUS("status"),
    ACCOUNTING_PERIOD("account_period_id"),
    TYPE_OF_PERFORMANCE("execution_type"),
    PROCESS_PERIODICITY("process_periodicity");

    private final String value;

    BillingRunListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
