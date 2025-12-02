package bg.energo.phoenix.model.enums.receivable.latePaymentFine;

import lombok.Getter;

@Getter
public enum LatePaymentFineSortingType {
    NUMBER("id"),
    CREATE_DATE("createDate"),
    TYPE("type"),
    CUSTOMER("customer"),
    REVERSED("reversed"),
    BILLING_GROUP("billingGroup"),
    AMOUNT("totalAmount"),
    CURRENCY("currency"),
    DUE_DATE("dueDate"),
    LOGICAL_DATE("logicalDate");

    private final String value;

    private LatePaymentFineSortingType(String value) {
        this.value = value;
    }

}
