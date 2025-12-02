package bg.energo.phoenix.model.enums.receivable.deposit;

public enum DepositListingSortingType {
    ID("id"),
    DEPOSIT_NUMBER("depositNumber"),
    CONTRACT_ORDER_NUMBER("contractOrderNumber"),
    CUSTOMER_NUMBER("customerNumber"),
    PAYMENT_DEADLINE("paymentDeadline"),
    INITIAL_AMOUNT("initialAmount"),
    CURRENCY_NAME("currencyName"),
    CURRENT_AMOUNT("currentAmount");

    private final String value;

    private DepositListingSortingType(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

}
