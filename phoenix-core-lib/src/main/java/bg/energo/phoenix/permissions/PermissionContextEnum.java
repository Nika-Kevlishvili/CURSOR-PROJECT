package bg.energo.phoenix.permissions;

import bg.energo.common.security.acl.definitions.AclPermissionDefinition;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bg.energo.phoenix.permissions.PermissionEnum.*;

public enum PermissionContextEnum {
    // BUNDLE 1
    COUNTRIES(
            "bg.energo.phoenix.security.context.countries",
            "bg.energo.phoenix.security.context.countries.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    REGIONS(
            "bg.energo.phoenix.security.context.regions",
            "bg.energo.phoenix.security.context.regions.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    MUNICIPALITIES(
            "bg.energo.phoenix.security.context.municipalities",
            "bg.energo.phoenix.security.context.municipalities.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    POPULATED_PLACES(
            "bg.energo.phoenix.security.context.populated-places",
            "bg.energo.phoenix.security.context.populated-places.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    DISTRICTS(
            "bg.energo.phoenix.security.context.districts",
            "bg.energo.phoenix.security.context.districts.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    REPRESENTATION_METHODS(
            "bg.energo.phoenix.security.context.representation-methods",
            "bg.energo.phoenix.security.context.representation-methods.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    BANKS(
            "bg.energo.phoenix.security.context.banks",
            "bg.energo.phoenix.security.context.banks.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SEGMENTS(
            "bg.energo.phoenix.security.context.segments",
            "bg.energo.phoenix.security.context.segments.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    TITLES(
            "bg.energo.phoenix.security.context.titles",
            "bg.energo.phoenix.security.context.titles.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ACCOUNT_MANAGER_TYPES(
            "bg.energo.phoenix.security.context.account-manager-types",
            "bg.energo.phoenix.security.context.account-manager-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    BELONGING_CAPITAL_OWNERS(
            "bg.energo.phoenix.security.context.belonging-capital-owners",
            "bg.energo.phoenix.security.context.belonging-capital-owners.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PLATFORMS(
            "bg.energo.phoenix.security.context.platforms",
            "bg.energo.phoenix.security.context.platforms.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    LEGAL_FORMS(
            "bg.energo.phoenix.security.context.legal-forms",
            "bg.energo.phoenix.security.context.legal-forms.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ECONOMIC_BRANCH_CI(
            "bg.energo.phoenix.security.context.economic-branch-ci",
            "bg.energo.phoenix.security.context.economic-branch-ci.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    CI_CONNECTION_TYPE(
            "bg.energo.phoenix.security.context.ci-connection-type",
            "bg.energo.phoenix.security.context.ci-connection-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    GCC_CONNECTION_TYPE(
            "bg.energo.phoenix.security.context.gcc-connection-type",
            "bg.energo.phoenix.security.context.gcc-connection-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    CONTACT_PURPOSE(
            "bg.energo.phoenix.security.context.contact-purpose",
            "bg.energo.phoenix.security.context.contact-purpose.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    UNWANTED_CUSTOMER_REASON(
            "bg.energo.phoenix.security.context.unwanted-customer-reason",
            "bg.energo.phoenix.security.context.unwanted-customer-reason.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    OWNERSHIP_FORM(
            "bg.energo.phoenix.security.context.ownership-form",
            "bg.energo.phoenix.security.context.ownership-form.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PREFERENCES(
            "bg.energo.phoenix.security.context.preferences",
            "bg.energo.phoenix.security.context.preferences.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    CREDIT_RATING(
            "bg.energo.phoenix.security.context.credit-rating",
            "bg.energo.phoenix.security.context.credit-rating.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ECONOMIC_BRANCH_NCEA(
            "bg.energo.phoenix.security.context.economic-branch-ncea",
            "bg.energo.phoenix.security.context.economic-branch-ncea.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    RESIDENTIAL_AREAS(
            "bg.energo.phoenix.security.context.residential-areas",
            "bg.energo.phoenix.security.context.residential-areas.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    STREETS(
            "bg.energo.phoenix.security.context.streets",
            "bg.energo.phoenix.security.context.streets.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ZIP_CODES(
            "bg.energo.phoenix.security.context.zip-codes",
            "bg.energo.phoenix.security.context.zip-codes.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    BLOCKING_REASON(
            "bg.energo.phoenix.security.context.blocking-reason",
            "bg.energo.phoenix.security.context.blocking-reason.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    REASON_FOR_DISCONNECTION(
            "bg.energo.phoenix.security.context.reason-for-disconnection",
            "bg.energo.phoenix.security.context.reason-for-disconnection.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    REASON_FOR_CANCELLATION(
            "bg.energo.phoenix.security.context.reason-for-cancellation",
            "bg.energo.phoenix.security.context.reason-for-cancellation.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    CUSTOMER(
            "bg.energo.phoenix.security.context.customer",
            "bg.energo.phoenix.security.context.customer.desc",
            Arrays.asList(
                    CUSTOMER_EDIT,
                    CUSTOMER_EDIT_AM,
                    CUSTOMER_VIEW_BASIC,
                    CUSTOMER_VIEW_BASIC_AM,
                    CUSTOMER_VIEW_DELETED,
                    CUSTOMER_VIEW_GDPR,
                    CUSTOMER_VIEW_GDPR_AM,
                    CUSTOMER_CREATE,
                    CUSTOMER_DELETE_BASIC,
                    CUSTOMER_DELETE_POTENTIAL,
                    CUSTOMER_VIEW_RELATED_CONTRACTS,
                    CUSTOMER_VIEW_DELETED_RELATED_CONTRACTS,
                    CUSTOMER_VIEW_RELATED_ORDERS,
                    CUSTOMER_VIEW_DELETED_RELATED_ORDERS,
                    CUSTOMER_VIEW_LIABILITIES_AND_RECEIVABLES,
                    CUSTOMER_EDIT_LOCKED,
                    CUSTOMER_EDIT_AUTOMATIC_UPDATE,
                    CUSTOMER_EDIT_SEGMENT
            )

    ),
    GCC(
            "bg.energo.phoenix.security.context.gcc",
            "bg.energo.phoenix.security.context.gcc.desc",
            Arrays.asList(
                    GCC_CREATE,
                    GCC_EDIT,
                    GCC_VIEW_BASIC,
                    GCC_VIEW_DELETED,
                    GCC_DELETE)
    ),
    UC(
            "bg.energo.phoenix.security.context.uc",
            "bg.energo.phoenix.security.context.uc.desc",
            Arrays.asList(
                    UC_CREATE,
                    UC_EDIT,
                    UC_VIEW_BASIC,
                    UC_VIEW_DELETED,
                    UC_DELETE
            )
    ),
    SM(
            "bg.energo.phoenix.security.context.sm",
            "bg.energo.phoenix.security.context.sm.desc",
            Arrays.asList(
                    SM_EDIT,
                    SM_VIEW
            )
    ),
    PARALLEL_EDIT_LOCK(
            "bg.energo.phoenix.security.context.override_parallel_edit_lock",
            "bg.energo.phoenix.security.context.override_parallel_edit_lock.desc",
            List.of(OVERRIDE_PARALLEL_EDIT_LOCK)
    ),
    PROCESS(
            "bg.energo.phoenix.security.context.process",
            "bg.energo.phoenix.security.context.process.desc",
            Arrays.asList(
                    PROCESS_START,
                    PROCESS_EDIT,
                    PROCESS_PAUSE,
                    PROCESS_CANCEL,
                    PROCESS_VIEW,
                    PROCESS_START_SU,
                    PROCESS_EDIT_SU,
                    PROCESS_CANCEL_SU,
                    PROCESS_PAUSE_SU,
                    PROCESS_VIEW_SU)
    ),
    CUSTOMER_MI(
            "bg.energo.phoenix.security.context.customer_mi",
            "bg.energo.phoenix.security.context.customer_mi.desc",
            Arrays.asList(
                    MI_CREATE,
                    MI_EDIT_AM,
                    MI_EDIT,
                    MI_CUSTOMER_EDIT_SEGMENT)
    ),
    // BUNDLE 2
    PRODUCT_TYPE(
            "bg.energo.phoenix.security.context.product-types",
            "bg.energo.phoenix.security.context.product-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    GOODS_UNITS(
            "bg.energo.phoenix.security.context.goods-units",
            "bg.energo.phoenix.security.context.goods-units.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SALE_AREAS(
            "bg.energo.phoenix.security.context.sales-areas",
            "bg.energo.phoenix.security.context.sales-areas.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SALE_CHANNELS(
            "bg.energo.phoenix.security.context.sales-channels",
            "bg.energo.phoenix.security.context.sales-channels.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    GOODS_SUPPLIERS(
            "bg.energo.phoenix.security.context.goods-suppliers",
            "bg.energo.phoenix.security.context.goods-suppliers.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    GOODS_GROUPS(
            "bg.energo.phoenix.security.context.goods-groups",
            "bg.energo.phoenix.security.context.goods-groups.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PRODUCT_GROUPS(
            "bg.energo.phoenix.security.context.product-groups",
            "bg.energo.phoenix.security.context.product-groups.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SERVICE_GROUPS(
            "bg.energo.phoenix.security.context.service-groups",
            "bg.energo.phoenix.security.context.service-groups.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    CURRENCIES(
            "bg.energo.phoenix.security.context.currencies",
            "bg.energo.phoenix.security.context.currencies.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    VAT_RATES(
            "bg.energo.phoenix.security.context.vat-rates",
            "bg.energo.phoenix.security.context.vat-rates.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SERVICE_TYPES(
            "bg.energo.phoenix.security.context.service-type",
            "bg.energo.phoenix.security.context.service-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PRICE_TYPES_ELECTRICITY(
            "bg.energo.phoenix.security.context.electricity-price-type",
            "bg.energo.phoenix.security.context.electricity-price-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SERVICE_UNITS(
            "bg.energo.phoenix.security.context.service-units",
            "bg.energo.phoenix.security.context.service-units.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PC_PRICE_TYPES(
            "bg.energo.phoenix.security.context.price-component-price-type",
            "bg.energo.phoenix.security.context.price-component-price-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PC_VALUE_TYPES(
            "bg.energo.phoenix.security.context.price-component-value-type",
            "bg.energo.phoenix.security.context.price-component-value-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    EXPIRATION_PERIOD(
            "bg.energo.phoenix.security.context.expiration-period",
            "bg.energo.phoenix.security.context.expiration-period.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PRODUCTS(
            "bg.energo.phoenix.security.context.products",
            "bg.energo.phoenix.security.context.products.desc",
            Arrays.asList(
                    PRODUCT_VIEW_BASIC,
                    PRODUCT_VIEW_DELETED,
                    PRODUCT_CREATE,
                    PRODUCT_DELETE,
                    PRODUCT_EDIT_BASIC,
                    PRODUCT_EDIT_LOCKED,
                    INDIVIDUAL_PRODUCT_VIEW_BASIC,
                    INDIVIDUAL_PRODUCT_VIEW_DELETED,
                    INDIVIDUAL_PRODUCT_CREATE,
                    INDIVIDUAL_PRODUCT_DELETE,
                    INDIVIDUAL_PRODUCT_EDIT_BASIC,
                    INDIVIDUAL_PRODUCT_EDIT_LOCKED,
                    AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_PRODUCT
            )
    ),
    SERVICES(
            "bg.energo.phoenix.security.context.services",
            "bg.energo.phoenix.security.context.services.desc",
            Arrays.asList(
                    SERVICES_VIEW_BASIC,
                    SERVICES_VIEW_DELETED,
                    SERVICES_CREATE,
                    SERVICES_DELETE,
                    SERVICES_EDIT_BASIC,
                    SERVICES_EDIT_LOCKED,
                    SERVICES_VIEW_INDIVIDUAL_BASIC,
                    SERVICES_VIEW_INDIVIDUAL_DELETED,
                    SERVICES_CREATE_INDIVIDUAL,
                    SERVICES_DELETE_INDIVIDUAL,
                    SERVICES_EDIT_INDIVIDUAL_BASIC,
                    SERVICES_EDIT_INDIVIDUAL_LOCKED,
                    AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_SERVICE
            )
    ),
    GOODS(
            "bg.energo.phoenix.security.context.goods",
            "bg.energo.phoenix.security.context.goods.desc",
            Arrays.asList(GOODS_VIEW_BASIC, GOODS_VIEW_DELETED, GOODS_CREATE, GOODS_DELETE, GOODS_EDIT_BASIC, GOODS_EDIT_LOCKED)
    ),
    PRICE_COMPONENT(
            "bg.energo.phoenix.security.context.price_component",
            "bg.energo.phoenix.security.context.price_component.desc",
            Arrays.asList(PRICE_COMPONENT_VIEW_BASIC, PRICE_COMPONENT_VIEW_DELETED, PRICE_COMPONENT_CREATE, PRICE_COMPONENT_DELETE, PRICE_COMPONENT_EDIT_BASIC, PRICE_COMPONENT_EDIT_LOCKED)
    ),
    PRICE_PARAMETER(
            "bg.energo.phoenix.security.context.price_parameter",
            "bg.energo.phoenix.security.context.price_parameter.desc",
            Arrays.asList(PRICE_PARAMETER_VIEW_BASIC, PRICE_PARAMETER_VIEW_DELETED, PRICE_PARAMETER_CREATE, PRICE_PARAMETER_DELETE, PRICE_PARAMETER_EDIT_BASIC, PRICE_PARAMETER_EDIT_LOCKED)
    ),
    TERMS(
            "bg.energo.phoenix.security.context.terms",
            "bg.energo.phoenix.security.context.terms.desc",
            Arrays.asList(TERMS_VIEW_BASIC, TERMS_VIEW_DELETED, TERMS_CREATE, TERMS_DELETE, TERMS_EDIT_BASIC, TERMS_EDIT_LOCKED)
    ),
    TERMS_GROUP(
            "bg.energo.phoenix.security.context.terms_group",
            "bg.energo.phoenix.security.context.terms_group.desc",
            Arrays.asList(TERMS_GROUP_VIEW_BASIC, TERMS_GROUP_VIEW_DELETED, TERMS_GROUP_CREATE, TERMS_GROUP_DELETE, TERMS_GROUP_EDIT_BASIC, TERMS_GROUP_EDIT_LOCKED)
    ),
    CALENDAR(
            "bg.energo.phoenix.security.context.calendar",
            "bg.energo.phoenix.security.context.calendar.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    GRID_OPERATOR(
            "bg.energo.phoenix.security.context.grid-operators",
            "bg.energo.phoenix.security.context.grid-operators.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),
    TERMINATION(
            "bg.energo.phoenix.security.context.termination",
            "bg.energo.phoenix.security.context.termination.desc",
            Arrays.asList(TERMINATION_VIEW_BASIC, TERMINATION_VIEW_DELETED, TERMINATION_CREATE, TERMINATION_DELETE, TERMINATION_EDIT_BASIC, TERMINATION_EDIT_LOCKED)
    ),
    TERMINATION_GROUP(
            "bg.energo.phoenix.security.context.termination_group",
            "bg.energo.phoenix.security.context.termination_group.desc",
            Arrays.asList(TERMINATION_GROUP_VIEW_BASIC, TERMINATION_GROUP_VIEW_DELETED, TERMINATION_GROUP_CREATE, TERMINATION_GROUP_DELETE, TERMINATION_GROUP_EDIT_BASIC, TERMINATION_GROUP_EDIT_LOCKED)
    ),
    PENALTY(
            "bg.energo.phoenix.security.context.penalties",
            "bg.energo.phoenix.security.context.penalties.desc",
            Arrays.asList(PENALTY_CREATE, PENALTY_EDIT, PENALTY_EDIT_LOCKED, PENALTY_DELETE, PENALTY_VIEW_BASIC, PENALTY_VIEW_DELETED)
    ),
    PENALTY_GROUP(
            "bg.energo.phoenix.security.context.penalty-groups",
            "bg.energo.phoenix.security.context.penalty-groups.desc",
            Arrays.asList(PENALTY_GROUP_CREATE, PENALTY_GROUP_EDIT, PENALTY_GROUP_EDIT_LOCKED, PENALTY_GROUP_DELETE, PENALTY_GROUP_VIEW_BASIC, PENALTY_GROUP_VIEW_DELETED)
    ),
    SCALES(
            "bg.energo.phoenix.security.context.scales",
            "bg.energo.phoenix.security.context.scales.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PRICE_COMPONENT_GROUP(
            "bg.energo.phoenix.security.context.price_component_group",
            "bg.energo.phoenix.security.context.price_component_group.desc",
            Arrays.asList(PRICE_COMPONENT_GROUP_VIEW_BASIC, PRICE_COMPONENT_GROUP_VIEW_DELETED, PRICE_COMPONENT_GROUP_CREATE, PRICE_COMPONENT_GROUP_DELETE, PRICE_COMPONENT_GROUP_EDIT_BASIC, PRICE_COMPONENT_GROUP_EDIT_LOCKED)
    ),
    INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.context.interim_advance_payment",
            "bg.energo.phoenix.security.context.interim_advance_payment.desc",
            Arrays.asList(INTERIM_ADVANCE_PAYMENT_VIEW_BASIC, INTERIM_ADVANCE_PAYMENT_VIEW_DELETED, INTERIM_ADVANCE_PAYMENT_CREATE, INTERIM_ADVANCE_PAYMENT_DELETE, INTERIM_ADVANCE_PAYMENT_EDIT_BASIC, INTERIM_ADVANCE_PAYMENT_EDIT_LOCKED)
    ),
    ADVANCED_PAYMENT_GROUP(
            "bg.energo.phoenix.security.context.advanced-payment",
            "bg.energo.phoenix.security.context.advanced-payment.desc",
            Arrays.asList(ADVANCED_PAYMENT_GROUP_CREATE, ADVANCED_PAYMENT_GROUP_EDIT, ADVANCED_PAYMENT_GROUP_EDIT_LOCKED, ADVANCED_PAYMENT_GROUP_DELETE, ADVANCED_PAYMENT_GROUP_VIEW_BASIC, ADVANCED_PAYMENT_GROUP_VIEW_DELETED)
    ),
    UNWANTED_CUSTOMER_MI(
            "bg.energo.phoenix.security.context.unwanted_customer_mi",
            "bg.energo.phoenix.security.context.unwanted_customer_mi.desc",
            Arrays.asList(
                    UNWANTED_CUSTOMER_MI_CREATE,
                    UNWANTED_CUSTOMER_MI_EDIT)
    ),
    // BUNDLE 3
    POD(
            "bg.energo.phoenix.security.context.pod",
            "bg.energo.phoenix.security.context.pod.desc",
            Arrays.asList(
                    POD_CREATE,
                    POD_IMPOSSIBLE_DISCONNECT,
                    POD_EDIT,
                    POD_EDIT_LOCKED,
                    POD_DELETE,
                    POD_VIEW_BASIC,
                    POD_VIEW_DELETED,
                    POD_BLOCK_BILLING,
                    POD_BLOCK_DISCONNECTION,
                    POD_EDIT_ADDITIONAL_PARAMS
            )
    ),
    METERS(
            "bg.energo.phoenix.security.context.meters",
            "bg.energo.phoenix.security.context.meters.desc",
            Arrays.asList(METERS_CREATE, METERS_EDIT, METERS_EDIT_LOCKED, METERS_DELETE, METERS_VIEW_BASIC, METERS_VIEW_DELETED)
    ),
    PROFILES(
            "bg.energo.phoenix.security.context.profiles",
            "bg.energo.phoenix.security.context.profiles.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    DEACTIVATION_PURPOSE(
            "bg.energo.phoenix.security.context.deactivation-purpose",
            "bg.energo.phoenix.security.context.deactivation-purpose.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    COORDINATOR_BALANCING_GROUPS(
            "bg.energo.phoenix.security.context.balancing-group-coordinators",
            "bg.energo.phoenix.security.context.balancing-group-coordinators.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    USER_TYPES(
            "bg.energo.phoenix.security.context.user-types",
            "bg.energo.phoenix.security.context.user-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    DISCOUNT(
            "bg.energo.phoenix.security.context.discount",
            "bg.energo.phoenix.security.context.discount.desc",
            Arrays.asList(DISCOUNT_CREATE, DISCOUNT_EDIT, DISCOUNT_EDIT_LOCKED, DISCOUNT_DELETE, DISCOUNT_VIEW_BASIC, DISCOUNT_VIEW_DELETED)
    ),

    SUPPLY_AUTOMATIC_ACTIVATION_DEACTIVATION_MI(
            "bg.energo.phoenix.security.context.supply_automatic_activation_deactivation_mi",
            "bg.energo.phoenix.security.context.supply_automatic_activation_deactivation_mi.desc",
            List.of(ACTIVATION_DEACTIVATION_MI)
    ),
    POD_MI(
            "bg.energo.phoenix.security.context.pod_mi",
            "bg.energo.phoenix.security.context.pod_mi.desc",
            Arrays.asList(
                    POD_MI_CREATE,
                    POD_MI_UPDATE,
                    POD_MI_BILLING,
                    POD_MI_DISCONNECTION,
                    POD_MI_IMPOSSIBLE_DISCONNECT,
                    POD_EDIT_ADDITIONAL_PARAMS
            )
    ),
    BILLING_BY_PROFILE(
            "bg.energo.phoenix.security.context.billing_by_profile",
            "bg.energo.phoenix.security.context.billing_by_profile.desc",
            Arrays.asList(BILLING_BY_PROFILE_CREATE, BILLING_BY_PROFILE_EDIT, BILLING_BY_PROFILE_EDIT_LOCKED, BILLING_BY_PROFILE_DELETE, BILLING_BY_PROFILE_VIEW_BASIC, BILLING_BY_PROFILE_VIEW_DELETED)
    ),
    METERS_MI(
            "bg.energo.phoenix.security.context.meters_mi",
            "bg.energo.phoenix.security.context.meters_mi.desc",
            Arrays.asList(METERS_MI_CREATE, METERS_MI_UPDATE)
    ),
    BILLING_BY_SCALES(
            "bg.energo.phoenix.security.context.billing_by_scales",
            "bg.energo.phoenix.security.context.billing_by_scales.desc",
            Arrays.asList(BILLING_BY_SCALES_CREATE, BILLING_BY_SCALES_EDIT, BILLING_BY_SCALES_EDIT_LOCKED, BILLING_BY_SCALES_DELETE, BILLING_BY_SCALES_VIEW_BASIC, BILLING_BY_SCALES_VIEW_DELETED)
    ),
    INTEREST_RATES(
            "bg.energo.phoenix.security.context.interest_rates",
            "bg.energo.phoenix.security.context.interest_rates.desc",
            Arrays.asList(INTEREST_RATES_CREATE, INTEREST_RATES_EDIT, INTEREST_RATES_DELETE, INTEREST_RATES_VIEW_BASIC, INTEREST_RATES_VIEW_DELETED)
    ),
    // BUNDLE 4
    CAMPAIGNS(
            "bg.energo.phoenix.security.context.campaigns",
            "bg.energo.phoenix.security.context.campaigns.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    EXTERNAL_INTERMEDIARIES(
            "bg.energo.phoenix.security.context.external-intermediaries",
            "bg.energo.phoenix.security.context.external-intermediaries.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ACTIVITIES(
            "bg.energo.phoenix.security.context.activities",
            "bg.energo.phoenix.security.context.activities.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    SUB_ACTIVITIES(
            "bg.energo.phoenix.security.context.sub-activities",
            "bg.energo.phoenix.security.context.sub-activities.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    TASK_TYPES(
            "bg.energo.phoenix.security.context.task-types",
            "bg.energo.phoenix.security.context.task-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    PRODUCT_CONTRACTS(
            "bg.energo.phoenix.security.context.product_contracts",
            "bg.energo.phoenix.security.context.product_contracts.desc",
            Arrays.asList(PRODUCT_CONTRACT_CREATE, PRODUCT_CONTRACT_EDIT, PRODUCT_CONTRACT_VIEW, PRODUCT_CONTRACT_VIEW_DELETED, PRODUCT_CONTRACT_DELETE, PRODUCT_CONTRACT_EDIT_READY, PRODUCT_CONTRACT_EDIT_DRAFT, PRODUCT_CONTRACT_EDIT_STATUS, PRODUCT_CONTRACT_EDIT_LOCKED, PRODUCT_CONTRACT_GENERATE, PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE, PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE,PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS)
    ),
    SERVICE_CONTRACTS(
            "bg.energo.phoenix.security.context.service_contracts",
            "bg.energo.phoenix.security.context.service_contracts.desc",
            Arrays.asList(SERVICE_CONTRACT_CREATE, SERVICE_CONTRACT_EDIT, SERVICE_CONTRACT_EDIT_STATUSES, SERVICE_CONTRACT_EDIT_READY, SERVICE_CONTRACT_EDIT_DRAFT, SERVICE_CONTRACT_EDIT_LOCKED, SERVICE_CONTRACT_VIEW, SERVICE_CONTRACT_VIEW_DELETED, SERVICE_CONTRACT_DELETE, SERVICE_CONTRACT_GENERATE, SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE, SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE)
    ),
    BASE_INTEREST_RATES(
            "bg.energo.phoenix.security.context.base-interest-rates",
            "bg.energo.phoenix.security.context.base-interest-rates.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    TASK(
            "bg.energo.phoenix.security.context.task",
            "bg.energo.phoenix.security.context.task.desc",
            Arrays.asList(TASK_CREATE, TASK_EDIT, TASK_EDIT_SUPER_USER,
                    TASK_DELETE, TASK_VIEW_BASIC, TASK_VIEW_DELETED,
                    CUSTOMER_CREATE_TASK_ON_PREVIEW, CONTRACT_CREATE_TASK_ON_PREVIEW,
                    ORDER_CREATE_TASK_ON_PREVIEW, BILLING_CREATE_TASK_ON_PREVIEW,
                    RECEIVABLE_BLOCKING_CREATE_TASK_ON_PREVIEW, CUSTOMER_ASSESSMENT_CREATE_TASK,
                    RESCHEDULING_TO_A_BALANCING_GROUP_COORDINATOR_CREATE_TASK, SMS_COMMUNICATION_CREATE_TASK,
                    MASS_SMS_COMMUNICATION_CREATE_TASK, MASS_EMAIL_COMMUNICATION_CREATE_TASK, EMAIL_COMMUNICATION_CREATE_TASK,
                    DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK_ON_PREVIEW, DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK
            )
    ),
    SYSTEM_ACTIVITIES(
            "bg.energo.phoenix.security.context.system_activities",
            "bg.energo.phoenix.security.context.system_activities.desc",
            Arrays.asList(
                    SYSTEM_ACTIVITY_CREATE,
                    SYSTEM_ACTIVITY_EDIT,
                    SYSTEM_ACTIVITY_EDIT_LOCKED,
                    SYSTEM_ACTIVITY_DELETE,
                    SYSTEM_ACTIVITY_VIEW_BASIC,
                    SYSTEM_ACTIVITY_VIEW_DELETED,
                    CREATE_ACTIVITY_FROM_CONTRACT_PREVIEW,
                    CREATE_ACTIVITY_FROM_ORDER_PREVIEW,
                    CREATE_ACTIVITY_FROM_TASK_PREVIEW,
                    MASS_SMS_COMMUNICATION_CREATE_ACTIVITY,
                    SMS_COMMUNICATION_CREATE_ACTIVITY,
                    MASS_EMAIL_COMMUNICATION_CREATE_ACTIVITY,
                    EMAIL_COMMUNICATION_CREATE_ACTIVITY
            )
    ),
    SERVICE_ORDERS(
            "bg.energo.phoenix.security.context.service_orders",
            "bg.energo.phoenix.security.context.service_orders.desc",
            Arrays.asList(
                    SERVICE_ORDER_CREATE,
                    SERVICE_ORDER_EDIT_REQUESTED,
                    SERVICE_ORDER_EDIT_CONFIRMED,
                    SERVICE_ORDER_EDIT_STATUSES,
                    SERVICE_ORDER_EDIT_LOCKED,
                    SERVICE_ORDER_CREATE_PRO_FORMA_INVOICE,
                    SERVICE_ORDER_CREATE_INVOICE,
                    SERVICE_ORDER_DELETE,
                    SERVICE_ORDER_VIEW,
                    SERVICE_ORDER_VIEW_DELETED
            )
    ),
    GOODS_ORDERS(
            "bg.energo.phoenix.security.context.goods_orders",
            "bg.energo.phoenix.security.context.goods_orders.desc",
            Arrays.asList(
                    GOODS_ORDER_CREATE,
                    GOODS_ORDER_EDIT_REQUESTED,
                    GOODS_ORDER_EDIT_CONFIRMED,
                    GOODS_ORDER_EDIT_STATUS,
                    GOODS_ORDER_EDIT_LOCKED,
                    GOODS_ORDER_DELETE,
                    GOODS_ORDER_VIEW,
                    GOODS_ORDER_VIEW_DELETED,
                    GOODS_ORDER_CREATE_INVOICE,
                    GOODS_ORDER_CREATE_PRO_FORMA_INVOICE
            )
    ),
    EXPRESS_CONTRACT(
            "bg.energo.phoenix.security.context.express-contracts",
            "bg.energo.phoenix.security.context.express-contracts.desc",
            List.of(EXPRESS_CONTRACT_CREATE)
    ),
    CONTRACT_VERSION_TYPES(
            "bg.energo.phoenix.security.context.contract-version-types",
            "bg.energo.phoenix.security.context.contract-version-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ACTION_TYPES(
            "bg.energo.phoenix.security.context.action-types",
            "bg.energo.phoenix.security.context.action-types.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    ACTIONS(
            "bg.energo.phoenix.security.context.actions",
            "bg.energo.phoenix.security.context.actions.desc",
            Arrays.asList(
                    ACTIONS_CREATE,
                    ACTIONS_EDIT,
                    ACTIONS_EDIT_LOCKED,
                    ACTIONS_DELETE,
                    ACTIONS_VIEW_ACTIVE,
                    ACTIONS_VIEW_DELETED,
                    ACTIONS_CLAIM_PENALTY
            )
    ),
    PRODUCT_CONTRACTS_MI(
            "bg.energo.phoenix.security.context.product_contracts_mi",
            "bg.energo.phoenix.security.context.product_contracts_mi.desc",
            Arrays.asList(
                    MI_EDIT,
                    MI_CREATE,
                    PRODUCT_CONTRACT_MI_EDIT_LOCKED
            )
    ),
    SERVICE_CONTRACT_MI(
            "bg.energo.phoenix.security.context.service_contract_mi",
            "bg.energo.phoenix.security.context.service_contract_mi.desc",
            Arrays.asList(SERVICE_CONTRACT_MI_CREATE, SERVICE_CONTRACT_MI_EDIT)
    ),
    POD_MANUAL_ACTIVATION(
            "bg.energo.phoenix.security.context.pod_manual_activation",
            "bg.energo.phoenix.security.context.pod_manual_activation.desc",
            List.of(
                    POD_MANUAL_ACTIVATION_DEACTIVATION
            )
    ),
    PREFIXES(
            "bg.energo.phoenix.security.context.prefixes",
            "bg.energo.phoenix.security.context.prefixes.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    COMPANY_DETAIL(
            "bg.energo.phoenix.security.context.company_detail",
            "bg.energo.phoenix.security.context.company_detail.desc",
            Arrays.asList(COMPANY_DETAIL_CREATE, COMPANY_DETAIL_VIEW, COMPANY_DETAIL_EDIT)
    ),
    ACCOUNTING_PERIOD(
            "bg.energo.phoenix.security.context.accounting_period",
            "bg.energo.phoenix.security.context.accounting_period.desc",
            Arrays.asList(ACCOUNTING_PERIOD_EDIT, ACCOUNTING_PERIOD_VIEW, COMPANY_DETAIL_EDIT)
    ),
    BILLING_RUN(
            "bg.energo.phoenix.security.context.billing_run",
            "bg.energo.phoenix.security.context.billing_run.desc",
            Arrays.asList(CREATE_BILLING_RUN,
                    EDIT_BILLING_RUN, EDIT_BILLING_RUN_WITH_STATUS,
                    VIEW_BILLING_RUN,
                    VIEW_PERIOD_BILLING_RUN, VIEW_DELETED_BILLING_RUN,
                    VIEW_STANDARD_BILLING_RUN, DELETE_BILLING_RUN,

                    CREATE_BILLING_RUN_STANDARD,
                    VIEW_BILLING_RUN_STANDARD,
                    EDIT_BILLING_RUN_STANDARD,
                    DELETE_BILLING_RUN_STANDARD,
                    START_BILLING_STANDARD,
                    CONTINUE_BILLING_STANDARD,
                    PAUSE_BILLING_STANDARD,
                    TERMINATE_BILLING_STANDARD,
                    START_ACCOUNTING_BILLING_STANDARD,
                    START_GENERATING_BILLING_STANDARD,

                    CREATE_BILLING_RUN_MANUAL_INVOICE,
                    VIEW_BILLING_RUN_MANUAL_INVOICE,
                    EDIT_BILLING_RUN_MANUAL_INVOICE,
                    DELETE_BILLING_RUN_MANUAL_INVOICE,
                    START_BILLING_MANUAL_INVOICE,
                    CONTINUE_BILLING_MANUAL_INVOICE,
                    PAUSE_BILLING_MANUAL_INVOICE,
                    TERMINATE_BILLING_MANUAL_INVOICE,
                    START_ACCOUNTING_BILLING_MANUAL_INVOICE,
                    START_GENERATING_BILLING_MANUAL_INVOICE,

                    CREATE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    VIEW_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    EDIT_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    DELETE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    START_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    CONTINUE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    PAUSE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    TERMINATE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    START_ACCOUNTING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
                    START_GENERATING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,

                    CREATE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    VIEW_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    EDIT_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    DELETE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    START_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    CONTINUE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    PAUSE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    TERMINATE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    START_ACCOUNTING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
                    START_GENERATING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,

                    CREATE_BILLING_RUN_INVOICE_CORRECTION,
                    VIEW_BILLING_RUN_INVOICE_CORRECTION,
                    EDIT_BILLING_RUN_INVOICE_CORRECTION,
                    DELETE_BILLING_RUN_INVOICE_CORRECTION,
                    START_BILLING_INVOICE_CORRECTION,
                    CONTINUE_BILLING_INVOICE_CORRECTION,
                    PAUSE_BILLING_INVOICE_CORRECTION,
                    TERMINATE_BILLING_INVOICE_CORRECTION,
                    START_ACCOUNTING_BILLING_INVOICE_CORRECTION,
                    START_GENERATING_BILLING_INVOICE_CORRECTION,

                    CREATE_BILLING_RUN_INVOICE_REVERSAL,
                    VIEW_BILLING_RUN_INVOICE_REVERSAL,
                    EDIT_BILLING_RUN_INVOICE_REVERSAL,
                    DELETE_BILLING_RUN_INVOICE_REVERSAL,
                    START_BILLING_INVOICE_REVERSAL,
                    CONTINUE_BILLING_INVOICE_REVERSAL,
                    PAUSE_BILLING_INVOICE_REVERSAL,
                    TERMINATE_BILLING_INVOICE_REVERSAL,
                    START_ACCOUNTING_BILLING_INVOICE_REVERSAL,
                    START_GENERATING_BILLING_INVOICE_REVERSAL)
    ),
    PROCESS_PERIODICITY(
            "bg.energo.phoenix.security.context.process_periodicity",
            "bg.energo.phoenix.security.context.process_periodicity.desc",
            Arrays.asList(PROCESS_PERIODICITY_CREATE, PROCESS_PERIODICITY_VIEW, PROCESS_PERIODICITY_VIEW_DELETED, PROCESS_PERIODICITY_DELETE, PROCESS_PERIODICITY_UPDATE)
    ),
    MEASUREMENT_TYPE(
            "bg.energo.phoenix.security.context.measurement-type",
            "bg.energo.phoenix.security.context.measurement-type.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    INVOICE(
            "bg.energo.phoenix.security.context.invoice",
            "bg.energo.phoenix.security.context.invoice.desc",
            List.of(INVOICE_VIEW, INVOICE_VIEW_DRAFT, INVOICE_CANCELLATION)
    ),

    RISK_ASSESSMENT(
            "bg.energo.phoenix.security.context.risk-assessment",
            "bg.energo.phoenix.security.context.assessment.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),

    DEPOSIT(
            "bg.energo.phoenix.security.context.deposit",
            "bg.energo.phoenix.security.context.deposit.desc",
            List.of(DEPOSIT_CREATE, DEPOSIT_UPDATE, DEPOSIT_VIEW, DEPOSIT_DELETE, DEPOSIT_VIEW_DELETE)
    ),
    CUSTOMER_RECEIVABLE(
            "bg.energo.phoenix.security.context.customer_receivable",
            "bg.energo.phoenix.security.context.customer_receivable.desc",
            Arrays.asList(CUSTOMER_RECEIVABLE_CREATE, CUSTOMER_RECEIVABLE_DELETE, CUSTOMER_RECEIVABLE_UPDATE, CUSTOMER_RECEIVABLE_VIEW, CUSTOMER_RECEIVABLE_VIEW_DELETE, BLOCKED_FOR_LIABILITIES_OFFSETTING)
    ),
    CUSTOMER_RECEIVABLE_MI("bg.energo.phoenix.security.context.customer_receivable_mi",
            "bg.energo.phoenix.security.verb.customer_receivable_mi.desc",
            Arrays.asList(CUSTOMER_RECEIVABLE_MI_CREATE, CUSTOMER_RECEIVABLE_MI_EDIT, CUSTOMER_RECEIVABLE_MI_EDIT_LOCKED, CUSTOMER_RECEIVABLE_VIEW_DELETE, BLOCKED_FOR_LIABILITIES_OFFSETTING)
    ),
    PAYMENT_MI("bg.energo.phoenix.security.context.payment_mi",
            "bg.energo.phoenix.security.context.payment_mi.desc",
            Arrays.asList(PAYMENT_MI_CREATE, RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)
    ),

    COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.context.collection-channel",
            "bg.energo.phoenix.security.context.collection-channel.desc",
            Arrays.asList(CREATE_COLLECTION_CHANNEL, VIEW_COLLECTION_CHANNEL, VIEW_DELETED_COLLECTION_CHANNEL, EDIT_COLLECTION_CHANNEL, DELETE_COLLECTION_CHANNEL)
    ),
    CUSTOMER_LIABILITY(
            "bg.energo.phoenix.security.context.customer_liability",
            "bg.energo.phoenix.security.context.customer_liability.desc",
            Arrays.asList(
                    CUSTOMER_LIABILITY_CREATE,
                    CUSTOMER_LIABILITY_DELETE,
                    CUSTOMER_LIABILITY_UPDATE,
                    CUSTOMER_LIABILITY_VIEW,
                    CUSTOMER_LIABILITY_VIEW_DELETE,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_PAYMENT,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_REMINDER_LETTERS,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_LIABILITIES_OFFSETTING,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_SUPPLY_TERMINATION,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION
            )
    ),
    CUSTOMER_LIABILITY_MI(
            "bg.energo.phoenix.security.context.customer_liability_mi",
            "bg.energo.phoenix.security.context.customer_liability_mi.desc",
            Arrays.asList(
                    CUSTOMER_LIABILITY_MI_CREATE,
                    CUSTOMER_LIABILITY_MI_EDIT,
                    CUSTOMER_LIABILITY_MI_EDIT_LOCKED,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_PAYMENT,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_REMINDER_LETTERS,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_LIABILITIES_OFFSETTING,
                    CUSTOMER_LIABILITY_BLOCKED_FOR_SUPPLY_TERMINATION,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING,
                    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION
            )
    ),
    RECEIVABLE_BLOCKING(
            "bg.energo.phoenix.security.context.receivable_blocking",
            "bg.energo.phoenix.security.context.receivable_blocking.desc",
            Arrays.asList(RECEIVABLE_BLOCKING_CREATE_AS_DRAFT, RECEIVABLE_BLOCKING_CREATE_AS_EXECUTE, RECEIVABLE_BLOCKING_VIEW_DRAFT, RECEIVABLE_BLOCKING_VIEW_EXECUTED, RECEIVABLE_BLOCKING_VIEW_DELETED_DRAFT, RECEIVABLE_BLOCKING_VIEW_DELETED_EXECUTED, RECEIVABLE_BLOCKING_DELETE_EXECUTED, RECEIVABLE_BLOCKING_DELETE_DRAFT, RECEIVABLE_BLOCKING_EDIT_EXECUTED, RECEIVABLE_BLOCKING_EDIT_DRAFT)
    ),
    RECEIVABLE_PAYMENT(
            "bg.energo.phoenix.security.context.receivable-payment",
            "bg.energo.phoenix.security.context.receivable-payment.desc",
            Arrays.asList(RECEIVABLE_PAYMENT_MANUAL_CREATE, RECEIVABLE_PAYMENT_EDIT, RECEIVABLE_PAYMENT_VIEW, RECEIVABLE_PAYMENT_DELETE, RECEIVABLE_PAYMENT_VIEW_DELETE, RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING, RECEIVABLE_PAYMENT_REVERSE)
    ),
    PAYMENT_PACKAGE(
            "bg.energo.phoenix.security.context.payment_package",
            "bg.energo.phoenix.security.context.payment_package.desc",
            Arrays.asList(PAYMENT_PACKAGE_CREATE, PAYMENT_PACKAGE_VIEW, PAYMENT_PACKAGE_EDIT, PAYMENT_PACKAGE_DELETE, PAYMENT_PACKAGE_VIEW_DELETED)
    ),

    REMINDER(
            "bg.energo.phoenix.security.context.reminder",
            "bg.energo.phoenix.security.context.reminder.desc",
            Arrays.asList(REMINDER_CREATE, REMINDER_VIEW_DELETED, REMINDER_VIEW, REMINDER_DELETE, REMINDER_EDIT)
    ),
    COLLECTION_PARTNER(
            "bg.energo.phoenix.security.context.collection-partner",
            "bg.energo.phoenix.security.context.collection-partner.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),

    MANUAL_LIABILITY_OFFSETTING(
            "bg.energo.phoenix.security.context.manual-liability-offsetting",
            "bg.energo.phoenix.security.context.manual-liability-offsetting.desc",
            Arrays.asList(MANUAL_LIABILITY_OFFSETTING_CREATE, MANUAL_LIABILITY_OFFSETTING_EDIT, MANUAL_LIABILITY_OFFSETTING_VIEW, MANUAL_LIABILITY_OFFSETTING_REVERSAL)
    ),

    LATE_PAYMENT_FINE(
            "bg.energo.phoenix.security.context.late_payment_fine",
            "bg.energo.phoenix.security.context.late_payment_fine.desc",
            Arrays.asList(LATE_PAYMENT_FINE_CREATE, LATE_PAYMENT_FINE_UPDATE, LATE_PAYMENT_FINE_VIEW, LATE_PAYMENT_FINE_REVERSE)
    ),

    DISCONNECTION_POWER_SUPPLY(
            "bg.energo.phoenix.security.context.disconnection-power-supply-request",
            "bg.energo.phoenix.security.context.disconnection-power-supply-request.desc",
            Arrays.asList(
                    DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_DRAFT,
                    DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_EXECUTE,
                    DISCONNECTION_POWER_SUPPLY_EDIT_REQUESTS_DRAFT,
                    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DRAFT,
                    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_EXECUTED,
                    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_FEE_CHARGED,
                    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DELETED,
                    DISCONNECTION_POWER_SUPPLY_DELETED_REQUESTS_DRAFT
            )
    ),

    BALANCING_GROUP_OBJECTION(
            "bg.energo.phoenix.security.context.balancing_group_objection",
            "bg.energo.phoenix.security.context.balancing_group_objection.desc",
            Arrays.asList(BALANCING_GROUP_OBJECTION_CREATE_DRAFT,
                    BALANCING_GROUP_OBJECTION_EDIT_DRAFT,
                    BALANCING_GROUP_OBJECTION_SAVE_AND_SEND_DRAFT,
                    BALANCING_GROUP_OBJECTION_VIEW,
                    BALANCING_GROUP_OBJECTION_VIEW_DELETED,
                    BALANCING_GROUP_OBJECTION_DELETE)

    ),

    BALANCING_GROUP_COORDINATOR_GROUND(
            "bg.energo.phoenix.security.context.balancing_group_coordinator_ground",
            "bg.energo.phoenix.security.context.balancing_group_coordinator_ground.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),

    GROUND_FOR_OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_CBG(
            "bg.energo.phoenix.security.context.ground_for_objection_withdrawal_to_change_of_a_cbg",
            "bg.energo.phoenix.security.context.ground_for_objection_withdrawal_to_change_of_a_cbg.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),

    ADDITIONAL_CONDITION(
            "bg.energo.phoenix.security.context.additional_conditions",
            "bg.energo.phoenix.security.context.additional_conditions.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),

    CUSTOMER_ASSESSMENT_TYPE(
            "bg.energo.phoenix.security.context.customer_assessment_type",
            "bg.energo.phoenix.security.context.customer_assessment_type.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),

    CUSTOMER_ASSESSMENT_CRITERIA(
            "bg.energo.phoenix.security.context.customer_assessment_criteria",
            "bg.energo.phoenix.security.context.customer_assessment_criteria.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),
    CUSTOMER_ASSESSMENT(
            "bg.energo.phoenix.security.context.customer-assessment",
            "bg.energo.phoenix.security.context.customer-assessment.desc",
            Arrays.asList(
                    CUSTOMER_ASSESSMENT_CREATE, CUSTOMER_ASSESSMENT_EDIT,
                    CUSTOMER_ASSESSMENT_VIEW, CUSTOMER_ASSESSMENT_VIEW_DELETED,
                    CUSTOMER_ASSESSMENT_DELETE
            )
    ),
    RESCHEDULING(
            "bg.energo.phoenix.security.context.rescheduling",
            "bg.energo.phoenix.security.context.rescheduling.desc",
            Arrays.asList(RESCHEDULING_CREATE, RESCHEDULING_VIEW, RESCHEDULING_VIEW_DELETE, RESCHEDULING_UPDATE, RESCHEDULING_DELETE,RESCHEDULING_REVERSE,RESCHEDULING_CREATE_TASK)
    ),
    TAXES_FOR_THE_GRID_OPERATOR(
            "bg.energo.phoenix.security.context.taxes_for_the_grid_operator",
            "bg.energo.phoenix.security.context.taxes_for_the_grid_operator.desc",
            Arrays.asList(NOMENCLATURE_VIEW, NOMENCLATURE_EDIT)
    ),
    POD_PARAMS(
            "bg.energo.phoenix.security.context.pod-params",
            "bg.energo.phoenix.security.context.pod-params.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    OBJECTION_WITHDRAWAL_TO_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR(
            "bg.energo.phoenix.security.context.objection-withdrawal-to-a-change-of-a-balancing-group-coordinator",
            "bg.energo.phoenix.security.context.objection-withdrawal-to-a-change-of-a-balancing-group-coordinator.desc",
            Arrays.asList(OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_CREATE_DRAFT, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_CREATE_TASK, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_EDIT_DRAFT, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_SAVE_AND_SEND_DRAFT, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW_DELETE, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW, OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_DELETE)
    ),
    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY(
            "bg.energo.phoenix.security.context.cancellation-of-a-disconnection-of-the-power-supply",
            "bg.energo.phoenix.security.context.cancellation-of-a-disconnection-of-the-power-supply.desc",
            Arrays.asList(
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_DRAFT,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_EXECUTE,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_DRAFT,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_EXECUTED,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_EXECUTED,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DRAFT,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DELETED,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_DELETE_DRAFT,
                    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_TASK
            )
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY(
            "bg.energo.phoenix.security.context.power_supply_disconnection_reminders",
            "bg.energo.phoenix.security.context.power_supply_disconnection_reminders.desc",
            Arrays.asList(
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_CREATE,
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT,
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED,
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE,
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT,
                    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_DELETE
            )
    ),
    DISCONNECTION_OF_POWER_SUPPLY(
            "bg.energo.phoenix.security.context.disconnection-of-power-supply",
            "bg.energo.phoenix.security.context.disconnection-of-power-supply.desc",
            Arrays.asList(DISCONNECTION_OF_POWER_SUPPLY_SAVE_DRAFT, DISCONNECTION_OF_POWER_SUPPLY_SAVE_AND_EXECUTE, DISCONNECTION_OF_POWER_SUPPLY_SAVE, DISCONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT, DISCONNECTION_OF_POWER_SUPPLY_EDIT_EXECUTED, DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT, DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED, DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETED, DISCONNECTION_OF_POWER_SUPPLY_DELETE, DISCONNECTION_OF_POWER_CREATE_TASK)
    ),
    RECONNECTION_OF_POWER_SUPPLY(
            "bg.energo.phoenix.security.context.reconnection-of-power-supply",
            "bg.energo.phoenix.security.context.reconnection-of-power-supply.desc",
            Arrays.asList(RECONNECTION_OF_POWER_SUPPLY_CREATE_EXECUTE, RECONNECTION_OF_POWER_SUPPLY_CREATE_DRAFT, RECONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT, RECONNECTION_OF_POWER_SUPPLY_EDIT_EXECUTED, RECONNECTION_OF_POWER_SUPPLY_VIEW_DELETED, RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED, RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT, RECONNECTION_OF_POWER_SUPPLY_DELETE, RECONNECTION_OF_POWER_SUPPLY_CREATE_TASK)
    ),

    EMAIL_MAILBOXES(
            "bg.energo.phoenix.security.context.email-mailboxes",
            "bg.energo.phoenix.security.context.email-mailboxes.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),

    SMS_SENDING_NUMBERS(
            "bg.energo.phoenix.security.context.sms-sending-numbers",
            "bg.energo.phoenix.security.context.sms-sending-numbers.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),

    TOPICS_OF_COMMUNICATIONS(
            "bg.energo.phoenix.security.context.topic-of-communication",
            "bg.energo.phoenix.security.context.topic-of-communication.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),
    TEST_PERMISSIONS_CONTEXT(
            "bg.energo.phoenix.security.context.test-permissions-c",
            "bg.energo.phoenix.security.context.test-permissions-c.desc",
            Arrays.asList(TEST_PERMISSIONS)
    ),

    EMAIL_COMMUNICATION(
            "bg.energo.phoenix.security.context.email-communication",
            "bg.energo.phoenix.security.context.email-communication.desc",
            Arrays.asList(
                    EMAIL_COMMUNICATION_CREATE_DRAFT,
                    EMAIL_COMMUNICATION_CREATE_AND_SEND,
                    EMAIL_COMMUNICATION_DELETE,
                    EMAIL_COMMUNICATION_VIEW_SEND,
                    EMAIL_COMMUNICATION_VIEW_DRAFT,
                    EMAIL_COMMUNICATION_VIEW_DELETED,
                    EMAIL_COMMUNICATION_RESEND,
                    EMAIL_COMMUNICATION_EDIT
            )
    ),

    MASS_EMAIL_COMMUNICATION(
            "bg.energo.phoenix.security.context.mass-email-communication",
            "bg.energo.phoenix.security.context.mass-email-communication.desc",
            Arrays.asList(
                    MASS_EMAIL_COMMUNICATION_VIEW_SEND,
                    MASS_EMAIL_COMMUNICATION_VIEW_DRAFT,
                    MASS_EMAIL_COMMUNICATION_VIEW_DELETED,
                    MASS_EMAIL_COMMUNICATION_CREATE_DRAFT,
                    MASS_EMAIL_COMMUNICATION_CREATE_AND_SEND,
                    MASS_EMAIL_COMMUNICATION_DELETE,
                    MASS_EMAIL_COMMUNICATION_EDIT
            )
    ),
    SMS_COMMUNICATION(
            "bg.energo.phoenix.security.context.sms-communication",
            "bg.energo.phoenix.security.context.sms-communication.desc",
            Arrays.asList(
                    SMS_COMMUNICATION_EDIT,
                    SMS_COMMUNICATION_DELETE,
                    SMS_COMMUNICATION_CREATE_DRAFT,
                    SMS_COMMUNICATION_CREATE_AND_SEND,
                    SMS_COMMUNICATION_VIEW_DELETE,
                    SMS_COMMUNICATION_VIEW_DRAFT,
                    SMS_COMMUNICATION_VIEW_SEND,
                    SMS_COMMUNICATION_CREATE_SAVE,
                    SMS_COMMUNICATION_RESEND
            )
    ),

    CUSTOMER_RELATIONSHIP(
            "bg.energo.phoenix.security.context.customer-relationship",
            "bg.energo.phoenix.security.context.customer-relationship.desc",
            Arrays.asList(
                    CUSTOMER_RELATIONSHIP_VIEW,
                    CUSTOMER_RELATIONSHIP_VIEW_DELETED
            )
    ),
    MASS_SMS_COMMUNICATION(
            "bg.energo.phoenix.security.context.mass-sms-communication",
            "bg.energo.phoenix.security.context.mass-sms-communication.desc",
            Arrays.asList(
                    MASS_SMS_COMMUNICATION_CREATE_SEND,
                    MASS_SMS_COMMUNICATION_CREATE_DRAFT,
                    MASS_SMS_COMMUNICATION_VIEW_DRAFT,
                    MASS_SMS_COMMUNICATION_VIEW_DELETED,
                    MASS_SMS_COMMUNICATION_VIEW_SENT,
                    MASS_SMS_COMMUNICATION_DELETE,
                    MASS_SMS_COMMUNICATION_EDIT
            )
    ),
    TEMPLATE(
            "bg.energo.phoenix.security.context.template",
            "bg.energo.phoenix.security.context.template.desc",
            Arrays.asList(
                    CREATE_TEMPLATE,
                    VIEW_TEMPLATE,
                    VIEW_DELETED_TEMPLATE,
                    EDIT_TEMPLATE,
                    DELETE_TEMPLATE
            )
    ),
    DEFAULT_INTEREST_CALCULATION(
            "bg.energo.phoenix.security.context.default-interest-calculation",
            "bg.energo.phoenix.security.context.default-interest-calculation.desc",
            Arrays.asList(
                    CALCULATE_DEFAULT_INTEREST
            )
    ),

    INCOME_ACCOUNT_NUMBER(
            "bg.energo.phoenix.security.context.income-account-number",
            "bg.energo.phoenix.security.context.income-account-number.desc",
            Arrays.asList(
                    NOMENCLATURE_VIEW,
                    NOMENCLATURE_EDIT
            )
    ),
    MISSING_CUSTOMER(
            "bg.energo.phoenix.security.context.missing-customer",
            "bg.energo.phoenix.security.context.missing-customer.desc",
            Arrays.asList(NOMENCLATURE_EDIT, NOMENCLATURE_VIEW)
    ),

    GOVERNMENT_COMPENSATION(
            "bg.energo.phoenix.security.context.government_compensation",
            "bg.energo.phoenix.security.context.government_compensation.desc",
            Arrays.asList(
                    GOVERNMENT_COMPENSATION_CREATE,
                    GOVERNMENT_COMPENSATION_EDIT,
                    GOVERNMENT_COMPENSATION_VIEW,
                    GOVERNMENT_COMPENSATION_VIEW_DELETED,
                    GOVERNMENT_COMPENSATION_DELETE
            )
    ),
    GOVERNMENT_COMPENSATION_MI(
            "bg.energo.phoenix.security.context.government_compensation_mi",
            "bg.energo.phoenix.security.context.government_compensation_mi.desc",
            List.of(
                    GOVERNMENT_COMPENSATION_MI_CREATE
            )
    );

    @Getter
    private final String id;

    @Getter
    private final String descriptionKey;

    @Getter
    private final List<PermissionEnum> supportedPermissions;

    PermissionContextEnum(String id, String descriptionKey, List<PermissionEnum> supportedPermissions) {
        this.id = id;
        this.descriptionKey = descriptionKey;
        this.supportedPermissions = supportedPermissions;
    }

    public List<AclPermissionDefinition> getAclContextDefinition() {
        List<AclPermissionDefinition> definition = new ArrayList<>();
        for (PermissionEnum permission : supportedPermissions) {
            definition.add(new AclPermissionDefinition(
                    permission.getId(),
                    permission.getType(),
                    permission.getAclValues(),
                    permission.getTitleKey(),
                    permission.getDescriptionKey()
            ));
        }
        return definition;
    }

}
