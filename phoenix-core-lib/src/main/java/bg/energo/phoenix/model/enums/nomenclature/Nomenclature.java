package bg.energo.phoenix.model.enums.nomenclature;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;

import java.util.Arrays;

public enum Nomenclature {

    // address
    COUNTRIES("countries"),
    REGIONS("regions"),
    MUNICIPALITIES("municipalities"),
    POPULATED_PLACES("populated-places"),
    DISTRICTS("districts"),
    RESIDENTIAL_AREAS("residential-areas"),
    STREETS("streets"),
    ZIP_CODES("zip-codes"),

    // customer
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
    MISSING_CUSTOMER("missing-customer"),

    // product
    VAT_RATES("vat-rates"),
    CURRENCIES("currencies"),
    SALES_AREAS("sales-areas"),
    GOODS_SUPPLIERS("goods-suppliers"),
    GOODS_GROUPS("goods-groups"),
    SALES_CHANNELS("sales-channels"),
    GOODS_UNITS("goods-units"),
    PRODUCT_TYPES("product-types"),
    PRICE_COMPONENT_PRICE_TYPE("price-component-price-type"),
    SERVICE_UNITS("service-units"),
    ELECTRICITY_PRICE_TYPE("electricity-price-type"),
    SERVICE_TYPE("service-type"),
    PRODUCT_GROUPS("product-groups"),
    PC_VALUE_TYPE("price-component-value-type"),
    SERVICE_GROUPS("service-groups"),
    CALENDAR("calendar"),
    GRID_OPERATORS("grid-operators"),
    SCALES("scales"),

    // points of delivery
    PROFILES("profiles"),
    BALANCING_GROUP_COORDINATORS("balancing-group-coordinators"),
    USER_TYPES("user-types"),

    // contract
    CAMPAIGNS("campaigns"),
    EXTERNAL_INTERMEDIARIES("external-intermediaries"),
    ACTIVITIES("activities"),
    SUB_ACTIVITIES("sub-activities"),
    BASE_INTEREST_RATES("base-interest-rates"),
    DEACTIVATION_PURPOSE("deactivation-purpose"),
    TASK_TYPES("task-types"),
    TASK_TYPE_DETAILS("task-type-details"),
    CONTRACT_VERSION_TYPES("contract-version-types"),
    ACTION_TYPES("action-types"),

    // billing
    PREFIXES("prefixes"),
    MEASUREMENT_TYPE("measurement-type"),
    RISK_ASSESSMENTS("risk-assessment"),
    COLLECTION_PARTNERS("collection-partner"),

    // receivable
    BLOCKING_REASON("blocking-reason"),

    REASON_FOR_DISCONNECTION("reason-for-disconnection"),
    BALANCING_GROUP_COORDINATOR_GROUND("balancing-group-coordinator-ground"),
    GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG("ground-for-objection-withdrawal-to-change-of-a-cbg"),
    REASON_FOR_CANCELLATION("reason-for-cancellation"),
    TAX_FOR_THE_GRID_OPERATOR("tax-for-the-grid-operator"),
    ADDITIONAL_CONDITIONS("additional-conditions"),
    CUSTOMER_ASSESSMENTS_CRITERIA("customer-assessment-criteria"),
    EMAIL_MAILBOXES("email-mailboxes"),
    SMS_SENDING_NUMBERS("sms-sending-numbers"),
    TOPIC_OF_COMMUNICATION("topic-of-communication"),
    EXPIRATION_PERIOD("expiration-period"),
    POD_ADDITIONAL_PARAMETERS("pod-additional-parameters"),
    INCOME_ACCOUNT_NUMBER("income-account-number");

    private final String value;

    Nomenclature(final String value) {
        this.value = value;
    }

    public static Nomenclature fromValue(String value) {
        return Arrays
                .stream(Nomenclature.values())
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
    }

    public String getValue() {
        return value;
    }

}
