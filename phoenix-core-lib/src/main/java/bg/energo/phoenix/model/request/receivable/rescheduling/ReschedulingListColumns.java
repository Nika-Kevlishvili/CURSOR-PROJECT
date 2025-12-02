package bg.energo.phoenix.model.request.receivable.rescheduling;

public enum ReschedulingListColumns {

    NUMBER("rescheduling_number"),
    CUSTOMER("customer_id"),
    STATUS("status"),
    NUMBER_OF_INSTALLMENTS("number_of_installment"),
    AMOUNT_OF_THE_INSTALLMENT("amount_of_the_installment"),
    INSTALLMENT_DUE_DAY_OF_THE_MONTH("installment_due_day"),
    CREATION_DATE("create_date"),
    RESCHEDULING_STATUS("rescheduling_status"),
    REVERSED("reversed");

    private final String value;

    ReschedulingListColumns(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
