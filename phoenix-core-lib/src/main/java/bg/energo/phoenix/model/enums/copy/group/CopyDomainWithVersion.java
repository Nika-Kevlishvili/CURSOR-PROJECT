package bg.energo.phoenix.model.enums.copy.group;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;

import java.util.Arrays;

public enum CopyDomainWithVersion {

    GOODS("goods"),
    TERMS_GROUP("terms-group"),
    TERMINATION_GROUPS("termination-groups"),
    PRODUCTS_GROUP("products-group"),
    PRODUCTS("products"),
    ADVANCE_PAYMENT_GROUP("advanced-payment-group"),
    PRICE_COMPONENT_GROUP("price-component-groups"),
    PENALTY_GROUPS("penalty-groups"),
    SERVICES("services");
    private final String value;

    CopyDomainWithVersion(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CopyDomainWithVersion fromValue(String value) {
        return Arrays
                .stream(CopyDomainWithVersion.values())
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
    }
}
