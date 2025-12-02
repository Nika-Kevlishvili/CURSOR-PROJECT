package bg.energo.phoenix.model.enums.receivable.paymentPackage;

public enum PaymentPackageListingType {
    ALL("ALL"),
    NUMBER("NUMBER");

    private final String value;

    private PaymentPackageListingType(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
