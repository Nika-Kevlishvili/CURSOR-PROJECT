package bg.energo.phoenix.model.enums.receivable.deposit;

public enum DepositListingType {
    CUSTOMER_NUMBER("CUSTOMER_NUMBER"),
    DEPOSIT_NUMBER("DEPOSIT_NUMBER"),
    CONTRACT_ORDER("CONTRACT_ORDER"),
    ALL("ALL");

    private final String value;

    private DepositListingType(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

}
