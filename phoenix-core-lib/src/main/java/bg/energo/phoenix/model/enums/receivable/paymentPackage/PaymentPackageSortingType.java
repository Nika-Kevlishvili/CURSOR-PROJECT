package bg.energo.phoenix.model.enums.receivable.paymentPackage;

public enum PaymentPackageSortingType {

    NUMBER("id"),
    COLLECTION_CHANNEL("collectionChannel"),
    STATUS("status"),
    PAYMENT_DATE("paymentDate");

    private final String value;

    private PaymentPackageSortingType(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
