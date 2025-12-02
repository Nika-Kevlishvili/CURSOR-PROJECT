package bg.energo.phoenix.permissions;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ContextLogicalGroupEnum {
    //    BUSINESS(
//            "bg.energo.phoenix.security.logical_groups.business",
//            Arrays.stream(PermissionContextEnum.values()).toList()
//    ),
//    TEST(
//            "bg.energo.phoenix.security.logical_groups.test",
//            List.of(
//                    PermissionContextEnum.TEST_PERMISSIONS_CONTEXT)
//    ),
    CUSTOMER
            (
                    "bg.energo.phoenix.security.logical_groups.customer",
                    Arrays.asList(
                            PermissionContextEnum.CUSTOMER,
                            PermissionContextEnum.GCC,
                            PermissionContextEnum.UC,
                            PermissionContextEnum.CUSTOMER_MI,
                            PermissionContextEnum.UNWANTED_CUSTOMER_MI,
                            PermissionContextEnum.CUSTOMER_RELATIONSHIP
                    )

            ),
    POINTS_OF_DELIVERY
            (
                    "bg.energo.phoenix.security.logical_groups.points_of_delivery",
                    Arrays.asList(
                            PermissionContextEnum.POD,
                            PermissionContextEnum.METERS,
                            PermissionContextEnum.SUPPLY_AUTOMATIC_ACTIVATION_DEACTIVATION_MI,
                            PermissionContextEnum.POD_MI,
                            PermissionContextEnum.METERS_MI
                    )
            ),
    CUSTOMER_COMMUNICATION
            (
                    "bg.energo.phoenix.security.logical_groups.customer_communication",
                    Arrays.asList(
                            PermissionContextEnum.MASS_SMS_COMMUNICATION,
                            PermissionContextEnum.SMS_COMMUNICATION,
                            PermissionContextEnum.MASS_EMAIL_COMMUNICATION,
                            PermissionContextEnum.EMAIL_COMMUNICATION
                    )
            ),
    CONTRACTS_AND_ORDERS
            (
                    "bg.energo.phoenix.security.logical_groups.contracts_and_orders",
                    Arrays.asList(
                            PermissionContextEnum.PRODUCT_CONTRACTS,
                            PermissionContextEnum.SERVICE_CONTRACTS,
                            PermissionContextEnum.SERVICE_ORDERS,
                            PermissionContextEnum.GOODS_ORDERS,
                            PermissionContextEnum.EXPRESS_CONTRACT,
                            PermissionContextEnum.PRODUCT_CONTRACTS_MI,
                            PermissionContextEnum.SERVICE_CONTRACT_MI,
                            PermissionContextEnum.POD_MANUAL_ACTIVATION,
                            PermissionContextEnum.ACTIONS
                    )),
    PRODUCT_AND_SERVICES
            (
                    "bg.energo.phoenix.security.logical_groups.product_and_services",
                    Arrays.asList(
                            PermissionContextEnum.PRODUCTS,
                            PermissionContextEnum.SERVICES,
                            PermissionContextEnum.GOODS,
                            PermissionContextEnum.PRICE_COMPONENT,
                            PermissionContextEnum.PRICE_PARAMETER,
                            PermissionContextEnum.TERMS,
                            PermissionContextEnum.TERMS_GROUP,
                            PermissionContextEnum.TERMINATION,
                            PermissionContextEnum.TERMINATION_GROUP,
                            PermissionContextEnum.PENALTY,
                            PermissionContextEnum.PENALTY_GROUP,
                            PermissionContextEnum.PRICE_COMPONENT_GROUP,
                            PermissionContextEnum.INTERIM_ADVANCE_PAYMENT,
                            PermissionContextEnum.ADVANCED_PAYMENT_GROUP
                    )
            ),
    ENERGY_DATA
            (
                    "bg.energo.phoenix.security.logical_groups.energy_data",
                    Arrays.asList(
                            PermissionContextEnum.DISCOUNT,
                            PermissionContextEnum.BILLING_BY_PROFILE,
                            PermissionContextEnum.BILLING_BY_SCALES
                    )
            ),
    BILLING
            (
                    "bg.energo.phoenix.security.logical_groups.billing",
                    Arrays.asList(
                            PermissionContextEnum.ACCOUNTING_PERIOD,
                            PermissionContextEnum.BILLING_RUN,
                            PermissionContextEnum.INVOICE,
                            PermissionContextEnum.GOVERNMENT_COMPENSATION,
                            PermissionContextEnum.GOVERNMENT_COMPENSATION_MI
                    )
            ),
    RECEIVABLES_MANAGEMENT
            (
                    "bg.energo.phoenix.security.logical_groups.receivables_management",
                    Arrays.asList(
                            PermissionContextEnum.INTEREST_RATES,
                            PermissionContextEnum.DEPOSIT,
                            PermissionContextEnum.CUSTOMER_RECEIVABLE,
                            PermissionContextEnum.CUSTOMER_RECEIVABLE_MI,
                            PermissionContextEnum.PAYMENT_MI,
                            PermissionContextEnum.COLLECTION_CHANNEL,
                            PermissionContextEnum.CUSTOMER_LIABILITY,
                            PermissionContextEnum.CUSTOMER_LIABILITY_MI,
                            PermissionContextEnum.RECEIVABLE_BLOCKING,
                            PermissionContextEnum.RECEIVABLE_PAYMENT,
                            PermissionContextEnum.PAYMENT_PACKAGE,
                            PermissionContextEnum.REMINDER,
                            PermissionContextEnum.MANUAL_LIABILITY_OFFSETTING,
                            PermissionContextEnum.LATE_PAYMENT_FINE,
                            PermissionContextEnum.DISCONNECTION_POWER_SUPPLY,
                            PermissionContextEnum.BALANCING_GROUP_OBJECTION,
                            PermissionContextEnum.GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG,
                            PermissionContextEnum.CUSTOMER_ASSESSMENT,
                            PermissionContextEnum.RESCHEDULING,
                            PermissionContextEnum.OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR,
                            PermissionContextEnum.CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY,
                            PermissionContextEnum.REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY,
                            PermissionContextEnum.DISCONNECTION_OF_POWER_SUPPLY,
                            PermissionContextEnum.RECONNECTION_OF_POWER_SUPPLY,
                            PermissionContextEnum.DEFAULT_INTEREST_CALCULATION
                    )),
    OPERATIONS_MANAGEMENT
            (
                    "bg.energo.phoenix.security.logical_groups.operations_management",
                    Arrays.asList(
                            PermissionContextEnum.PROCESS,
                            PermissionContextEnum.TASK,
                            PermissionContextEnum.SYSTEM_ACTIVITIES,
                            PermissionContextEnum.PROCESS_PERIODICITY,
                            PermissionContextEnum.TEMPLATE
                    )

            ),
    MASTER_DATA
            (
                    "bg.energo.phoenix.security.logical_groups.master_data",
                    Arrays.asList(
                            PermissionContextEnum.COUNTRIES,
                            PermissionContextEnum.REGIONS,
                            PermissionContextEnum.MUNICIPALITIES,
                            PermissionContextEnum.POPULATED_PLACES,
                            PermissionContextEnum.DISTRICTS,
                            PermissionContextEnum.REPRESENTATION_METHODS,
                            PermissionContextEnum.BANKS,
                            PermissionContextEnum.SEGMENTS,
                            PermissionContextEnum.TITLES,
                            PermissionContextEnum.ACCOUNT_MANAGER_TYPES,
                            PermissionContextEnum.BELONGING_CAPITAL_OWNERS,
                            PermissionContextEnum.PLATFORMS,
                            PermissionContextEnum.LEGAL_FORMS,
                            PermissionContextEnum.ECONOMIC_BRANCH_CI,
                            PermissionContextEnum.CI_CONNECTION_TYPE,
                            PermissionContextEnum.GCC_CONNECTION_TYPE,
                            PermissionContextEnum.CONTACT_PURPOSE,
                            PermissionContextEnum.UNWANTED_CUSTOMER_REASON,
                            PermissionContextEnum.OWNERSHIP_FORM,
                            PermissionContextEnum.PREFERENCES,
                            PermissionContextEnum.CREDIT_RATING,
                            PermissionContextEnum.ECONOMIC_BRANCH_NCEA,
                            PermissionContextEnum.RESIDENTIAL_AREAS,
                            PermissionContextEnum.STREETS,
                            PermissionContextEnum.ZIP_CODES,
                            PermissionContextEnum.BLOCKING_REASON,
                            PermissionContextEnum.REASON_FOR_DISCONNECTION,
                            PermissionContextEnum.REASON_FOR_CANCELLATION,
                            PermissionContextEnum.PRODUCT_TYPE,
                            PermissionContextEnum.GOODS_UNITS,
                            PermissionContextEnum.SALE_AREAS,
                            PermissionContextEnum.SALE_CHANNELS,
                            PermissionContextEnum.GOODS_SUPPLIERS,
                            PermissionContextEnum.GOODS_GROUPS,
                            PermissionContextEnum.PRODUCT_GROUPS,
                            PermissionContextEnum.CURRENCIES,
                            PermissionContextEnum.VAT_RATES,
                            PermissionContextEnum.SERVICE_TYPES,
                            PermissionContextEnum.PRICE_TYPES_ELECTRICITY,
                            PermissionContextEnum.SERVICE_UNITS,
                            PermissionContextEnum.PC_PRICE_TYPES,
                            PermissionContextEnum.PC_VALUE_TYPES,
                            PermissionContextEnum.CALENDAR,
                            PermissionContextEnum.GRID_OPERATOR,
                            PermissionContextEnum.SCALES,
                            PermissionContextEnum.PROFILES,
                            PermissionContextEnum.DEACTIVATION_PURPOSE,
                            PermissionContextEnum.COORDINATOR_BALANCING_GROUPS,
                            PermissionContextEnum.USER_TYPES,
                            PermissionContextEnum.CAMPAIGNS,
                            PermissionContextEnum.EXTERNAL_INTERMEDIARIES,
                            PermissionContextEnum.ACTIVITIES,
                            PermissionContextEnum.SUB_ACTIVITIES,
                            PermissionContextEnum.TASK_TYPES,
                            PermissionContextEnum.BASE_INTEREST_RATES,
                            PermissionContextEnum.CONTRACT_VERSION_TYPES,
                            PermissionContextEnum.ACTION_TYPES,
                            PermissionContextEnum.PREFIXES,
                            PermissionContextEnum.COMPANY_DETAIL,
                            PermissionContextEnum.MEASUREMENT_TYPE,
                            PermissionContextEnum.RISK_ASSESSMENT,
                            PermissionContextEnum.COLLECTION_PARTNER,
                            PermissionContextEnum.BALANCING_GROUP_COORDINATOR_GROUND,
                            PermissionContextEnum.ADDITIONAL_CONDITION,
                            PermissionContextEnum.CUSTOMER_ASSESSMENT_TYPE,
                            PermissionContextEnum.CUSTOMER_ASSESSMENT_CRITERIA,
                            PermissionContextEnum.TAXES_FOR_THE_GRID_OPERATOR,
                            PermissionContextEnum.POD_PARAMS,
                            PermissionContextEnum.EMAIL_MAILBOXES,
                            PermissionContextEnum.SMS_SENDING_NUMBERS,
                            PermissionContextEnum.INCOME_ACCOUNT_NUMBER,
                            PermissionContextEnum.TOPICS_OF_COMMUNICATIONS,
                            PermissionContextEnum.MISSING_CUSTOMER
                    )),
    SYSTEM_SETTINGS
            (
                    "bg.energo.phoenix.security.logical_groups.system_settings",
                    List.of(
                            PermissionContextEnum.SM,
                            PermissionContextEnum.PARALLEL_EDIT_LOCK
                            ));


    @Getter
    private String key;
    @Getter
    private List<PermissionContextEnum> context;

    ContextLogicalGroupEnum(String key, List<PermissionContextEnum> context) {
        this.key = key;
        this.context = context;
    }

    public List<String> getPermissionIds() {
        List<String> ids = new ArrayList<>();
        for (PermissionContextEnum context : context) {
            ids.add(context.getId());
        }
        return ids;
    }


}
