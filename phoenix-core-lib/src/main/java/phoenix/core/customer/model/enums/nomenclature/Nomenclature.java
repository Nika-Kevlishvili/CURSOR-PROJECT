package phoenix.core.customer.model.enums.nomenclature;

import phoenix.core.exception.ClientException;
import phoenix.core.exception.ErrorCode;

import java.util.Arrays;

public enum Nomenclature {
    COUNTRIES("countries"),
    REGIONS("regions"),
    MUNICIPALITIES("municipalities"),
    POPULATED_PLACES("populated-places"),
    DISTRICTS("districts"),
    REPRESENTATION_METHODS("representation-methods"),
    BANKS("banks"),
    SEGMENTS("segments"),
    TITLES("titles"),
    ACCOUNT_MANAGER_TYPES("account-manager-types"),
    BELONGING_CAPITAL_OWNERS("belonging-capital-owners"),
    PLATFORMS("platforms"),
    LEGAL_FORMS("legal-forms"),
    ECONOMIC_BRANCH_CI("economic-branch-ci"),
    CI_CONNECTION_TYPE("ci-connection-type"),
    GCC_CONNECTION_TYPE("gcc-connection-type"),
    CONTACT_PURPOSE("contact-purpose"),
    UNWANTED_CUSTOMER_REASON("unwanted-customer-reason"),
    OWNERSHIP_FORM("ownership-form"),
    PREFERENCES("preferences"),
    CREDIT_RATING("credit-rating"),
    ECONOMIC_BRANCH_NCEA("economic-branch-ncea"),
    RESIDENTIAL_AREAS("residential-areas"),
    STREETS("streets"),
    ZIP_CODES("zip-codes");

    private final String value;

    Nomenclature(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Nomenclature fromValue(String value){
        return Arrays
                .stream(Nomenclature.values())
                .filter(v-> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
    }

}
