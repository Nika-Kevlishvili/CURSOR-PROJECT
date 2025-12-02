package bg.energo.phoenix.model.enums.copy.domain;

public enum CopyDomain {

    TERMINATIONS("terminations"),
    TERMS("terms"),
    PENALTIES("penalties"),
    INTERIM_ADVANCED_PAYMENT("iap"),
    PRICE_COMPONENTS("price-components");


    CopyDomain(final String value) {
        this.value = value;
    }
    private final String value;

    public String getValue() {
        return value;
    }
}
