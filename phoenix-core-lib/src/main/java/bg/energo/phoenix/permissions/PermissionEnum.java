package bg.energo.phoenix.permissions;

import bg.energo.common.security.acl.definitions.AclPermissionDefinition;
import bg.energo.common.security.acl.definitions.AclValue;
import bg.energo.common.security.acl.enums.AclPermissionType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public enum PermissionEnum {

    NOMENCLATURE_VIEW(
            "bg.energo.phoenix.security.verb.nomenclature_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.nomenclature_view",
            "bg.energo.phoenix.security.verb.nomenclature_view.desc"
    ),
    NOMENCLATURE_EDIT(
            "bg.energo.phoenix.security.verb.nomenclature_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.nomenclature_edit",
            "bg.energo.phoenix.security.verb.nomenclature_edit.desc"
    ),
    CUSTOMER_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.customer_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_basic",
            "bg.energo.phoenix.security.verb.customer_view_basic.desc"
    ),
    CUSTOMER_VIEW_BASIC_AM(
            "bg.energo.phoenix.security.verb.customer_view_basic_am", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_basic_am",
            "bg.energo.phoenix.security.verb.customer_view_basic_am.desc"
    ),
    CUSTOMER_VIEW_GDPR(
            "bg.energo.phoenix.security.verb.customer_view_gdrp", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_gdrp",
            "bg.energo.phoenix.security.verb.customer_view_gdrp.desc"
    ),
    CUSTOMER_VIEW_GDPR_AM(
            "bg.energo.phoenix.security.verb.customer_view_gdrp_am", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_gdrp_am",
            "bg.energo.phoenix.security.verb.customer_view_gdrp_am.desc"
    ),

    CUSTOMER_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.customer_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_deleted",
            "bg.energo.phoenix.security.verb.customer_view_deleted.desc"
    ),
    CUSTOMER_CREATE(
            "bg.energo.phoenix.security.verb.customer_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_create",
            "bg.energo.phoenix.security.verb.customer_create.desc"

    ),
    CUSTOMER_EDIT(
            "bg.energo.phoenix.security.verb.customer_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit",
            "bg.energo.phoenix.security.verb.customer_edit.desc"

    ),
    CUSTOMER_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.customer_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit_locked",
            "bg.energo.phoenix.security.verb.customer_edit_locked.desc"
    ),
    CUSTOMER_EDIT_AM(
            "bg.energo.phoenix.security.verb.customer_edit_am", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit_am",
            "bg.energo.phoenix.security.verb.customer_edit_am.desc"

    ),
    CUSTOMER_EDIT_AUTOMATIC_UPDATE(
            "bg.energo.phoenix.security.verb.customer_edit_automatic_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit_automatic_update",
            "bg.energo.phoenix.security.verb.customer_edit_automatic_update.desc"
    ),
    CUSTOMER_EDIT_SEGMENT(
            "bg.energo.phoenix.security.verb.customer_edit_segment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit_segment",
            "bg.energo.phoenix.security.verb.customer_edit_segment.desc"
    ),
    CUSTOMER_DELETE_BASIC(
            "bg.energo.phoenix.security.verb.customer_delete_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_delete_basic",
            "bg.energo.phoenix.security.verb.customer_delete_basic.desc"
    ),
    CUSTOMER_DELETE_POTENTIAL(
            "bg.energo.phoenix.security.verb.customer_delete_potential", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_delete_potential",
            "bg.energo.phoenix.security.verb.customer_delete_potential.desc"
    ),
    CUSTOMER_VIEW_RELATED_CONTRACTS(
            "bg.energo.phoenix.security.verb.customer_view_related_contracts", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_related_contracts",
            "bg.energo.phoenix.security.verb.customer_view_related_contracts.desc"
    ),
    CUSTOMER_VIEW_DELETED_RELATED_CONTRACTS(
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_contracts", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_contracts",
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_contracts.desc"
    ),
    CUSTOMER_VIEW_RELATED_ORDERS(
            "bg.energo.phoenix.security.verb.customer_view_related_orders", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_related_orders",
            "bg.energo.phoenix.security.verb.customer_view_related_orders.desc"
    ),
    CUSTOMER_VIEW_DELETED_RELATED_ORDERS(
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_orders", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_orders",
            "bg.energo.phoenix.security.verb.customer_view_deleted_related_orders.desc"
    ),
    CUSTOMER_VIEW_LIABILITIES_AND_RECEIVABLES(
            "bg.energo.phoenix.security.verb.customer_liabilities_and_receivables", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liabilities_and_receivables",
            "bg.energo.phoenix.security.verb.customer_liabilities_and_receivables.desc"
    ),
    MI_CUSTOMER_EDIT_SEGMENT(
            "bg.energo.phoenix.security.verb.mi_customer_edit_segment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mi_customer_edit_segment",
            "bg.energo.phoenix.security.verb.mi_customer_edit_segment.desc"
    ),
    MI_CREATE(
            "bg.energo.phoenix.security.verb.mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mi_create",
            "bg.energo.phoenix.security.verb.mi_create.desc"
    ),
    MI_EDIT(
            "bg.energo.phoenix.security.verb.mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mi_edit",
            "bg.energo.phoenix.security.verb.mi_edit.desc"
    ),
    MI_EDIT_AM(
            "bg.energo.phoenix.security.verb.mi_edit_am", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mi_edit_am",
            "bg.energo.phoenix.security.verb.mi_edit_am.desc"
    ),

    GCC_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.gcc_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.gcc_view_basic",
            "bg.energo.phoenix.security.verb.gcc_view_basic.desc"
    ),
    GCC_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.gcc_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.gcc_view_deleted",
            "bg.energo.phoenix.security.verb.gcc_view_deleted.desc"
    ),
    GCC_CREATE("bg.energo.phoenix.security.verb.gcc_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.gcc_create",
            "bg.energo.phoenix.security.verb.gcc_create.desc"
    ),
    GCC_EDIT(
            "bg.energo.phoenix.security.verb.gcc_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.gcc_edit",
            "bg.energo.phoenix.security.verb.gcc_edit.desc"
    ),
    GCC_DELETE(
            "bg.energo.phoenix.security.verb.gcc_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.gcc_delete",
            "bg.energo.phoenix.security.verb.gcc_delete.desc"
    ),
    UC_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.uc_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.uc_view_basic",
            "bg.energo.phoenix.security.verb.uc_view_basic.desc"
    ),
    UC_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.uc_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.uc_view_deleted",
            "bg.energo.phoenix.security.verb.uc_view_deleted.desc"
    ),
    UC_CREATE(
            "bg.energo.phoenix.security.verb.uc_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.uc_create",
            "bg.energo.phoenix.security.verb.uc_create.desc"
    ),
    UC_EDIT(
            "bg.energo.phoenix.security.verb.uc_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.uc_edit",
            "bg.energo.phoenix.security.verb.uc_edit.desc"
    ),
    UC_DELETE(
            "bg.energo.phoenix.security.verb.uc_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.uc_delete",
            "bg.energo.phoenix.security.verb.uc_delete.desc"
    ),
    SM_VIEW(
            "bg.energo.phoenix.security.verb.sm_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sm_view",
            "bg.energo.phoenix.security.verb.sm_view.desc"
    ),
    SM_EDIT(
            "bg.energo.phoenix.security.verb.sm_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sm_edit",
            "bg.energo.phoenix.security.verb.sm_edit.desc"
    ),
    OVERRIDE_PARALLEL_EDIT_LOCK(
            "bg.energo.phoenix.security.context.override_parallel_edit_lock", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.context.override_parallel_edit_lock",
            "bg.energo.phoenix.security.context.override_parallel_edit_lock.desc"
    ),
    PROCESS_START(
            "bg.energo.phoenix.security.verb.process_start", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_start",
            "bg.energo.phoenix.security.verb.process_start.desc"
    ),
    PROCESS_EDIT(
            "bg.energo.phoenix.security.verb.process_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_edit",
            "bg.energo.phoenix.security.verb.process_edit.desc"
    ),
    PROCESS_CANCEL(
            "bg.energo.phoenix.security.verb.process_cancel", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_cancel",
            "bg.energo.phoenix.security.verb.process_cancel.desc"
    ),
    PROCESS_PAUSE(
            "bg.energo.phoenix.security.verb.process_pause", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_pause",
            "bg.energo.phoenix.security.verb.process_pause.desc"
    ),
    PROCESS_VIEW(
            "bg.energo.phoenix.security.verb.process_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_view",
            "bg.energo.phoenix.security.verb.process_view.desc"
    ),
    PROCESS_START_SU(
            "bg.energo.phoenix.security.verb.process_start_su", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_start_su",
            "bg.energo.phoenix.security.verb.process_start_su.desc"
    ),
    PROCESS_EDIT_SU(
            "bg.energo.phoenix.security.verb.process_edit_su", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_edit_su",
            "bg.energo.phoenix.security.verb.process_edit_su.desc"
    ),
    PROCESS_CANCEL_SU(
            "bg.energo.phoenix.security.verb.process_cancel_su", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_cancel_su",
            "bg.energo.phoenix.security.verb.process_cancel_su.desc"
    ),
    PROCESS_PAUSE_SU(
            "bg.energo.phoenix.security.verb.process_pause_su", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_pause_su",
            "bg.energo.phoenix.security.verb.process_pause_su.desc"
    ),
    PROCESS_VIEW_SU(
            "bg.energo.phoenix.security.verb.process_view_su", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_view_su",
            "bg.energo.phoenix.security.verb.process_view_su.desc"
    ),
    PRODUCT_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.product_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_view_basic",
            "bg.energo.phoenix.security.verb.product_view_basic.desc"
    ),
    PRODUCT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.product_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_view_deleted",
            "bg.energo.phoenix.security.verb.product_view_deleted.desc"
    ),
    PRODUCT_CREATE(
            "bg.energo.phoenix.security.verb.product_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_create",
            "bg.energo.phoenix.security.verb.product_create.desc"
    ),
    PRODUCT_DELETE(
            "bg.energo.phoenix.security.verb.product_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_delete",
            "bg.energo.phoenix.security.verb.product_delete.desc"
    ),
    PRODUCT_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.product_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_edit_basic",
            "bg.energo.phoenix.security.verb.product_edit_basic.desc"
    ),
    PRODUCT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.product_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_edit_locked",
            "bg.energo.phoenix.security.verb.product_edit_locked.desc"
    ),
    INDIVIDUAL_PRODUCT_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.individual_product_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_view_basic",
            "bg.energo.phoenix.security.verb.individual_product_view_basic.desc"
    ),
    INDIVIDUAL_PRODUCT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.individual_product_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_view_deleted",
            "bg.energo.phoenix.security.verb.individual_product_view_deleted.desc"
    ),
    INDIVIDUAL_PRODUCT_CREATE(
            "bg.energo.phoenix.security.verb.individual_product_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_create",
            "bg.energo.phoenix.security.verb.individual_product_create.desc"
    ),
    INDIVIDUAL_PRODUCT_DELETE(
            "bg.energo.phoenix.security.verb.individual_product_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_delete",
            "bg.energo.phoenix.security.verb.individual_product_delete.desc"
    ),
    INDIVIDUAL_PRODUCT_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.individual_product_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_edit_basic",
            "bg.energo.phoenix.security.verb.individual_product_edit_basic.desc"
    ),
    INDIVIDUAL_PRODUCT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.individual_product_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_edit_locked",
            "bg.energo.phoenix.security.verb.individual_product_edit_locked.desc"
    ),
    AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_PRODUCT(
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_product", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_product",
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_product.desc"
    ),
    GOODS_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.goods_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_view_basic",
            "bg.energo.phoenix.security.verb.goods_view_basic.desc"
    ),
    GOODS_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.goods_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_view_deleted",
            "bg.energo.phoenix.security.verb.goods_view_deleted.desc"
    ),
    GOODS_CREATE(
            "bg.energo.phoenix.security.verb.goods_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_create",
            "bg.energo.phoenix.security.verb.goods_create.desc"
    ),
    GOODS_DELETE(
            "bg.energo.phoenix.security.verb.goods_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_delete",
            "bg.energo.phoenix.security.verb.goods_delete.desc"
    ),
    GOODS_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.goods_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_edit_basic",
            "bg.energo.phoenix.security.verb.goods_edit_basic.desc"
    ),
    GOODS_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.goods_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_edit_locked",
            "bg.energo.phoenix.security.verb.goods_edit_locked.desc"
    ),

    SERVICES_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.services_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_view_basic",
            "bg.energo.phoenix.security.verb.services_view_basic.desc"
    ),
    SERVICES_VIEW_INDIVIDUAL_BASIC(
            "bg.energo.phoenix.security.verb.services_view_individual_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_view_individual_basic",
            "bg.energo.phoenix.security.verb.services_view_individual_basic.desc"
    ),
    SERVICES_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.services_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_view_deleted",
            "bg.energo.phoenix.security.verb.services_view_deleted.desc"
    ),
    SERVICES_VIEW_INDIVIDUAL_DELETED(
            "bg.energo.phoenix.security.verb.services_view_individual_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_view_individual_deleted",
            "bg.energo.phoenix.security.verb.services_view_individual_deleted.desc"
    ),
    SERVICES_CREATE(
            "bg.energo.phoenix.security.verb.services_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_create",
            "bg.energo.phoenix.security.verb.services_create.desc"
    ),
    SERVICES_CREATE_INDIVIDUAL(
            "bg.energo.phoenix.security.verb.services_create_individual", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_create_individual",
            "bg.energo.phoenix.security.verb.services_create_individual.desc"
    ),
    SERVICES_DELETE(
            "bg.energo.phoenix.security.verb.services_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_delete",
            "bg.energo.phoenix.security.verb.services_delete.desc"
    ),
    SERVICES_DELETE_INDIVIDUAL(
            "bg.energo.phoenix.security.verb.services_delete_individual", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_delete_individual",
            "bg.energo.phoenix.security.verb.services_delete_individual.desc"
    ),
    SERVICES_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.services_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_edit_basic",
            "bg.energo.phoenix.security.verb.services_edit_basic.desc"
    ),
    SERVICES_EDIT_INDIVIDUAL_BASIC(
            "bg.energo.phoenix.security.verb.services_edit_individual_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_edit_individual_basic",
            "bg.energo.phoenix.security.verb.services_edit_individual_basic.desc"
    ),
    SERVICES_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.services_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_edit_locked",
            "bg.energo.phoenix.security.verb.services_edit_locked.desc"
    ),
    SERVICES_EDIT_INDIVIDUAL_LOCKED(
            "bg.energo.phoenix.security.verb.services_edit_individual_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.services_edit_individual_locked",
            "bg.energo.phoenix.security.verb.services_edit_individual_locked.desc"
    ),
    AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_SERVICE(
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_service", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_service",
            "bg.energo.phoenix.security.verb.automatic_related_contract_update_for_service.desc"
    ),
    TERMS_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.terms_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_view_basic",
            "bg.energo.phoenix.security.verb.terms_view_basic.desc"
    ),
    TERMS_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.terms_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_view_deleted",
            "bg.energo.phoenix.security.verb.terms_view_deleted.desc"
    ),
    TERMS_CREATE(
            "bg.energo.phoenix.security.verb.terms_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_create",
            "bg.energo.phoenix.security.verb.terms_create.desc"
    ),
    TERMS_DELETE(
            "bg.energo.phoenix.security.verb.terms_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_delete",
            "bg.energo.phoenix.security.verb.terms_delete.desc"
    ),
    TERMS_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.terms_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_edit_basic",
            "bg.energo.phoenix.security.verb.terms_edit_basic.desc"
    ),
    TERMS_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.terms_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_edit_locked",
            "bg.energo.phoenix.security.verb.terms_edit_locked.desc"
    ),
    TERMS_GROUP_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.terms_group_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_view_basic",
            "bg.energo.phoenix.security.verb.terms_group_view_basic.desc"
    ),
    TERMS_GROUP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.terms_group_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_view_deleted",
            "bg.energo.phoenix.security.verb.terms_group_view_deleted.desc"
    ),
    TERMS_GROUP_CREATE(
            "bg.energo.phoenix.security.verb.terms_group_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_create",
            "bg.energo.phoenix.security.verb.terms_group_create.desc"
    ),
    TERMS_GROUP_DELETE(
            "bg.energo.phoenix.security.verb.terms_group_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_delete",
            "bg.energo.phoenix.security.verb.terms_group_delete.desc"
    ),
    TERMS_GROUP_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.terms_group_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_edit_basic",
            "bg.energo.phoenix.security.verb.terms_group_edit_basic.desc"
    ),
    TERMS_GROUP_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.terms_group_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terms_group_edit_locked",
            "bg.energo.phoenix.security.verb.terms_group_edit_locked.desc"
    ),

    PRICE_PARAMETER_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.price_parameter_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_view_basic",
            "bg.energo.phoenix.security.verb.price_parameter_view_basic.desc"
    ),
    PRICE_PARAMETER_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.price_parameter_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_view_deleted",
            "bg.energo.phoenix.security.verb.price_parameter_view_deleted.desc"
    ),
    PRICE_PARAMETER_CREATE(
            "bg.energo.phoenix.security.verb.price_parameter_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_create",
            "bg.energo.phoenix.security.verb.price_parameter_create.desc"
    ),
    PRICE_PARAMETER_DELETE(
            "bg.energo.phoenix.security.verb.price_parameter_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_delete",
            "bg.energo.phoenix.security.verb.price_parameter_delete.desc"
    ),
    PRICE_PARAMETER_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.price_parameter_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_edit_basic",
            "bg.energo.phoenix.security.verb.price_parameter_edit_basic.desc"
    ),
    PRICE_PARAMETER_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.price_parameter_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_parameter_edit_locked",
            "bg.energo.phoenix.security.verb.price_parameter_edit_locked.desc"
    ),
    PRICE_COMPONENT_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.price_component_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_view_basic",
            "bg.energo.phoenix.security.verb.price_component_view_basic.desc"
    ),
    PRICE_COMPONENT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.price_component_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_view_deleted",
            "bg.energo.phoenix.security.verb.price_component_view_deleted.desc"
    ),
    PRICE_COMPONENT_CREATE(
            "bg.energo.phoenix.security.verb.price_component_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_create",
            "bg.energo.phoenix.security.verb.price_component_create.desc"
    ),
    PRICE_COMPONENT_DELETE(
            "bg.energo.phoenix.security.verb.price_component_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_delete",
            "bg.energo.phoenix.security.verb.price_component_delete.desc"
    ),
    PRICE_COMPONENT_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.price_component_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_edit_basic",
            "bg.energo.phoenix.security.verb.price_component_edit_basic.desc"
    ),
    PRICE_COMPONENT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.price_component_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_edit_locked",
            "bg.energo.phoenix.security.verb.price_component_edit_locked.desc"
    ),
    TERMINATION_GROUP_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.termination_group_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_view_basic",
            "bg.energo.phoenix.security.verb.termination_group_view_basic.desc"
    ),
    TERMINATION_GROUP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.termination_group_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_view_deleted",
            "bg.energo.phoenix.security.verb.termination_group_view_deleted.desc"
    ),
    TERMINATION_GROUP_CREATE(
            "bg.energo.phoenix.security.verb.termination_group_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_create",
            "bg.energo.phoenix.security.verb.termination_group_create.desc"
    ),
    TERMINATION_GROUP_DELETE(
            "bg.energo.phoenix.security.verb.termination_group_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_delete",
            "bg.energo.phoenix.security.verb.termination_group_delete.desc"
    ),
    TERMINATION_GROUP_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.termination_group_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_edit_basic",
            "bg.energo.phoenix.security.verb.termination_group_edit_basic.desc"
    ),
    TERMINATION_GROUP_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.termination_group_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_group_edit_locked",
            "bg.energo.phoenix.security.verb.termination_group_edit_locked.desc"
    ),
    TERMINATION_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.termination_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_view_basic",
            "bg.energo.phoenix.security.verb.termination_view_basic.desc"
    ),
    TERMINATION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.termination_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_view_deleted",
            "bg.energo.phoenix.security.verb.termination_view_deleted.desc"
    ),
    TERMINATION_CREATE(
            "bg.energo.phoenix.security.verb.termination_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_create",
            "bg.energo.phoenix.security.verb.termination_create.desc"
    ),
    TERMINATION_DELETE(
            "bg.energo.phoenix.security.verb.termination_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_delete",
            "bg.energo.phoenix.security.verb.termination_delete.desc"
    ),
    TERMINATION_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.termination_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_edit_basic",
            "bg.energo.phoenix.security.verb.termination_edit_basic.desc"
    ),
    TERMINATION_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.termination_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.termination_edit_locked",
            "bg.energo.phoenix.security.verb.termination_edit_locked.desc"
    ),
    PENALTY_CREATE(
            "bg.energo.phoenix.security.verb.penalty_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_create",
            "bg.energo.phoenix.security.verb.penalty_create.desc"
    ),
    PENALTY_EDIT(
            "bg.energo.phoenix.security.verb.penalty_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_edit",
            "bg.energo.phoenix.security.verb.penalty_edit.desc"
    ),
    PENALTY_DELETE(
            "bg.energo.phoenix.security.verb.penalty_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_delete",
            "bg.energo.phoenix.security.verb.penalty_delete.desc"
    ),
    PENALTY_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.penalty_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_view_basic",
            "bg.energo.phoenix.security.verb.penalty_view_basic.desc"
    ),
    PENALTY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.penalty_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_view_deleted",
            "bg.energo.phoenix.security.verb.penalty_view_deleted.desc"
    ),
    PENALTY_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.penalty_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_edit_locked",
            "bg.energo.phoenix.security.verb.penalty_edit_locked_desc"
    ),
    PENALTY_GROUP_CREATE(
            "bg.energo.phoenix.security.verb.penalty_group_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_create",
            "bg.energo.phoenix.security.verb.penalty_group_create.desc"
    ),
    PENALTY_GROUP_EDIT(
            "bg.energo.phoenix.security.verb.penalty_group_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_edit",
            "bg.energo.phoenix.security.verb.penalty_group_edit.desc"
    ),
    PENALTY_GROUP_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.penalty_group_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_edit_locked",
            "bg.energo.phoenix.security.verb.penalty_group_edit_locked.desc"
    ),
    PENALTY_GROUP_DELETE(
            "bg.energo.phoenix.security.verb.penalty_group_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_delete",
            "bg.energo.phoenix.security.verb.penalty_group_delete.desc"
    ),
    PENALTY_GROUP_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.penalty_group_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_view_basic",
            "bg.energo.phoenix.security.verb.penalty_group_view_basic.desc"
    ),
    PENALTY_GROUP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.penalty_group_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.penalty_group_view_deleted",
            "bg.energo.phoenix.security.verb.penalty_group_view_deleted.desc"
    ),
    PRICE_COMPONENT_GROUP_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.price_component_group_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_view_basic",
            "bg.energo.phoenix.security.verb.price_component_group_view_basic.desc"
    ),
    PRICE_COMPONENT_GROUP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.price_component_group_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_view_deleted",
            "bg.energo.phoenix.security.verb.price_component_group_view_deleted.desc"
    ),
    PRICE_COMPONENT_GROUP_CREATE(
            "bg.energo.phoenix.security.verb.price_component_group_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_create",
            "bg.energo.phoenix.security.verb.price_component_group_create.desc"
    ),
    PRICE_COMPONENT_GROUP_DELETE(
            "bg.energo.phoenix.security.verb.price_component_group_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_delete",
            "bg.energo.phoenix.security.verb.price_component_group_delete.desc"
    ),
    PRICE_COMPONENT_GROUP_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.price_component_group_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_edit_basic",
            "bg.energo.phoenix.security.verb.price_component_group_edit_basic.desc"
    ),
    PRICE_COMPONENT_GROUP_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.price_component_group_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.price_component_group_edit_locked",
            "bg.energo.phoenix.security.verb.price_component_group_edit_locked.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_basic",
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_basic.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_deleted",
            "bg.energo.phoenix.security.verb.interim_advance_payment_view_deleted.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_CREATE(
            "bg.energo.phoenix.security.verb.interim_advance_payment_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_create",
            "bg.energo.phoenix.security.verb.interim_advance_payment_create.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_DELETE(
            "bg.energo.phoenix.security.verb.interim_advance_payment_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_delete",
            "bg.energo.phoenix.security.verb.interim_advance_payment_delete.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_EDIT_BASIC(
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_basic",
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_basic.desc"
    ),
    INTERIM_ADVANCE_PAYMENT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_locked",
            "bg.energo.phoenix.security.verb.interim_advance_payment_edit_locked.desc"
    ),
    ADVANCED_PAYMENT_GROUP_CREATE(
            "bg.energo.phoenix.security.verb.advanced_payment_group_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_create",
            "bg.energo.phoenix.security.verb.advanced_payment_group_create.desc"
    ),
    ADVANCED_PAYMENT_GROUP_EDIT(
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit",
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit.desc"
    ),
    ADVANCED_PAYMENT_GROUP_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit_locked",
            "bg.energo.phoenix.security.verb.advanced_payment_group_edit_locked.desc"
    ),
    ADVANCED_PAYMENT_GROUP_DELETE(
            "bg.energo.phoenix.security.verb.advanced_payment_group_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_delete",
            "bg.energo.phoenix.security.verb.advanced_payment_group_delete.desc"
    ),
    ADVANCED_PAYMENT_GROUP_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_basic",
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_basic.desc"
    ),
    ADVANCED_PAYMENT_GROUP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_deleted",
            "bg.energo.phoenix.security.verb.advanced_payment_group_view_deleted.desc"
    ),
    UNWANTED_CUSTOMER_MI_CREATE(
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_create",
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_create.desc"
    ),
    UNWANTED_CUSTOMER_MI_EDIT(
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_edit",
            "bg.energo.phoenix.security.verb.unwanted_customer_mi_edit.desc"
    ),
    METERS_CREATE(
            "bg.energo.phoenix.security.verb.meters_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_create",
            "bg.energo.phoenix.security.verb.meters_create.desc"
    ),
    METERS_EDIT(
            "bg.energo.phoenix.security.verb.meters_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_edit",
            "bg.energo.phoenix.security.verb.meters_edit.desc"
    ),
    METERS_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.meters_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_edit_locked",
            "bg.energo.phoenix.security.verb.meters_edit_locked.desc"
    ),
    METERS_DELETE(
            "bg.energo.phoenix.security.verb.meters_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_delete",
            "bg.energo.phoenix.security.verb.meters_delete.desc"
    ),
    METERS_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.meters_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_view_basic",
            "bg.energo.phoenix.security.verb.meters_view_basic.desc"
    ),
    METERS_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.meters_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_view_deleted",
            "bg.energo.phoenix.security.verb.meters_view_deleted.desc"
    ),
    POD_CREATE(
            "bg.energo.phoenix.security.verb.pod_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_create",
            "bg.energo.phoenix.security.verb.pod_create.desc"
    ),
    POD_EDIT(
            "bg.energo.phoenix.security.verb.pod_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_edit",
            "bg.energo.phoenix.security.verb.pod_edit.desc"
    ),
    POD_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.pod_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_edit_locked",
            "bg.energo.phoenix.security.verb.pod_edit_locked.desc"
    ),
    POD_DELETE(
            "bg.energo.phoenix.security.verb.pod_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_delete",
            "bg.energo.phoenix.security.verb.pod_delete.desc"
    ),
    POD_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.pod_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_view_basic",
            "bg.energo.phoenix.security.verb.pod_view_basic.desc"
    ),
    POD_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.pod_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_view_deleted",
            "bg.energo.phoenix.security.verb.pod_view_deleted.desc"
    ),
    POD_BLOCK_BILLING(
            "bg.energo.phoenix.security.verb.pod_block_billing", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_block_billing",
            "bg.energo.phoenix.security.verb.pod_block_billing.desc"
    ),
    POD_BLOCK_DISCONNECTION(
            "bg.energo.phoenix.security.verb.pod_block_disconnection", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_block_disconnection",
            "bg.energo.phoenix.security.verb.pod_block_disconnection.desc"
    ),

    POD_EDIT_ADDITIONAL_PARAMS(
            "bg.energo.phoenix.security.verb.pod_edit_additional_params", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_edit_additional_params",
            "bg.energo.phoenix.security.verb.pod_edit_additional_params.desc"
    ),
    DISCOUNT_CREATE(
            "bg.energo.phoenix.security.verb.discount_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_create",
            "bg.energo.phoenix.security.verb.discount_create.desc"
    ),
    DISCOUNT_EDIT(
            "bg.energo.phoenix.security.verb.discount_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_edit",
            "bg.energo.phoenix.security.verb.discount_edit.desc"
    ),
    DISCOUNT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.discount_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_edit_locked",
            "bg.energo.phoenix.security.verb.discount_edit_locked.desc"
    ),
    DISCOUNT_DELETE(
            "bg.energo.phoenix.security.verb.discount_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_delete",
            "bg.energo.phoenix.security.verb.discount_delete.desc"
    ),
    DISCOUNT_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.discount_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_view_basic",
            "bg.energo.phoenix.security.verb.discount_view_basic.desc"
    ),
    DISCOUNT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.discount_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.discount_view_deleted",
            "bg.energo.phoenix.security.verb.discount_view_deleted.desc"
    ),

    ACTIVATION_DEACTIVATION_MI(
            "bg.energo.phoenix.security.verb.supply_automatic_activation_deactivation", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.supply_automatic_activation_deactivation",
            "bg.energo.phoenix.security.verb.supply_automatic_activation_deactivation.desc"
    ),
    CUSTOMER_RECEIVABLE_MI_CREATE(
            "bg.energo.phoenix.security.verb.customer_receivable_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_mi_create",
            "bg.energo.phoenix.security.verb.customer_receivable_mi_create.desc"
    ),
    PAYMENT_MI_CREATE(
            "bg.energo.phoenix.security.verb.payment_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_mi_create",
            "bg.energo.phoenix.security.verb.payment_mi_create.desc"
    ),
    CUSTOMER_RECEIVABLE_MI_EDIT(
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit",
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit.desc"
    ),
    CUSTOMER_RECEIVABLE_MI_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit_locked",
            "bg.energo.phoenix.security.verb.customer_receivable_mi_edit_locked.desc"
    ),
    POD_MI_CREATE(
            "bg.energo.phoenix.security.verb.pod_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_mi_create",
            "bg.energo.phoenix.security.verb.pod_mi_create.desc"
    ),
    POD_MI_UPDATE(
            "bg.energo.phoenix.security.verb.pod_mi_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_mi_update",
            "bg.energo.phoenix.security.verb.pod_mi_update.desc"
    ),
    POD_MI_BILLING(
            "bg.energo.phoenix.security.verb.pod_mi_billing", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_mi_billing",
            "bg.energo.phoenix.security.verb.pod_mi_billing.desc"
    ),
    POD_MI_DISCONNECTION(
            "bg.energo.phoenix.security.verb.pod_mi_disconnection", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_mi_disconnection",
            "bg.energo.phoenix.security.verb.pod_mi_disconnection.desc"
    ),
    POD_MI_IMPOSSIBLE_DISCONNECT(
            "bg.energo.phoenix.security.verb.pod_mi_impossible_disconnect", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_mi_impossible_disconnect",
            "bg.energo.phoenix.security.verb.pod_mi_impossible_disconnect.desc"
    ),
    POD_IMPOSSIBLE_DISCONNECT(
            "bg.energo.phoenix.security.verb.pod_impossible_disconnect", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_impossible_disconnect",
            "bg.energo.phoenix.security.verb.pod_impossible_disconnect.desc"
    ),
    BILLING_BY_PROFILE_CREATE(
            "bg.energo.phoenix.security.verb.billing_by_profile_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_create",
            "bg.energo.phoenix.security.verb.billing_by_profile_create.desc"
    ),
    BILLING_BY_PROFILE_EDIT(
            "bg.energo.phoenix.security.verb.billing_by_profile_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_edit",
            "bg.energo.phoenix.security.verb.billing_by_profile_edit.desc"
    ),
    BILLING_BY_PROFILE_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.billing_by_profile_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_edit_locked",
            "bg.energo.phoenix.security.verb.billing_by_profile_edit_locked.desc"
    ),
    BILLING_BY_PROFILE_DELETE(
            "bg.energo.phoenix.security.verb.billing_by_profile_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_delete",
            "bg.energo.phoenix.security.verb.billing_by_profile_delete.desc"
    ),
    BILLING_BY_PROFILE_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.billing_by_profile_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_view_basic",
            "bg.energo.phoenix.security.verb.billing_by_profile_view_basic.desc"
    ),
    BILLING_BY_PROFILE_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.billing_by_profile_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_profile_view_deleted",
            "bg.energo.phoenix.security.verb.billing_by_profile_view_deleted.desc"
    ),
    METERS_MI_CREATE(
            "bg.energo.phoenix.security.verb.meters_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_mi_create",
            "bg.energo.phoenix.security.verb.meters_mi_create.desc"
    ),
    METERS_MI_UPDATE(
            "bg.energo.phoenix.security.verb.meters_mi_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.meters_mi_update",
            "bg.energo.phoenix.security.verb.meters_mi_update.desc"
    ),
    BILLING_BY_SCALES_CREATE(
            "bg.energo.phoenix.security.verb.billing_by_scales_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_create",
            "bg.energo.phoenix.security.verb.billing_by_scales_create.desc"
    ),
    BILLING_BY_SCALES_EDIT(
            "bg.energo.phoenix.security.verb.billing_by_scales_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_edit",
            "bg.energo.phoenix.security.verb.billing_by_scales_edit.desc"
    ),
    BILLING_BY_SCALES_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.billing_by_scales_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_edit_locked",
            "bg.energo.phoenix.security.verb.billing_by_scales_edit_locked.desc"
    ),
    BILLING_BY_SCALES_DELETE(
            "bg.energo.phoenix.security.verb.billing_by_scales_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_delete",
            "bg.energo.phoenix.security.verb.billing_by_scales_delete.desc"
    ),
    BILLING_BY_SCALES_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.billing_by_scales_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_view_basic",
            "bg.energo.phoenix.security.verb.billing_by_scales_view_basic.desc"
    ),
    BILLING_BY_SCALES_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.billing_by_scales_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_by_scales_view_deleted",
            "bg.energo.phoenix.security.verb.billing_by_scales_view_deleted.desc"
    ),

    PRODUCT_CONTRACT_CREATE(
            "bg.energo.phoenix.security.verb.product_contract_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_create",
            "bg.energo.phoenix.security.verb.product_contract_create.desc"
    ),

    PRODUCT_CONTRACT_VIEW(
            "bg.energo.phoenix.security.verb.product_contract_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_view",
            "bg.energo.phoenix.security.verb.product_contract_view.desc"
    ),

    PRODUCT_CONTRACT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.product_contract_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_view_deleted",
            "bg.energo.phoenix.security.verb.product_contract_view_deleted.desc"
    ),

    PRODUCT_CONTRACT_EDIT(
            "bg.energo.phoenix.security.verb.product_contract_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit",
            "bg.energo.phoenix.security.verb.product_contract_edit.desc"
    ),

    PRODUCT_CONTRACT_EDIT_READY(
            "bg.energo.phoenix.security.verb.product_contract_edit_ready", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit_ready",
            "bg.energo.phoenix.security.verb.product_contract_edit_ready.desc"
    ),

    PRODUCT_CONTRACT_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.product_contract_edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit_draft",
            "bg.energo.phoenix.security.verb.product_contract_edit_draft.desc"
    ),
    PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS(
            "bg.energo.phoenix.security.verb.product_contract_edit_additional_agreements", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit_additional_agreements",
            "bg.energo.phoenix.security.verb.product_contract_edit_additional_agreements.desc"
    ),
    PRODUCT_CONTRACT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.product_contract_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit_locked",
            "bg.energo.phoenix.security.verb.product_contract_edit_locked.desc"
    ),
    PRODUCT_CONTRACT_EDIT_STATUS(
            "bg.energo.phoenix.security.verb.product_contract_edit_status", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_edit_status",
            "bg.energo.phoenix.security.verb.product_contract_edit_status.desc"
    ),
    PRODUCT_CONTRACT_DELETE(
            "bg.energo.phoenix.security.verb.product_contract_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_delete",
            "bg.energo.phoenix.security.verb.product_contract_delete.desc"
    ),
    PRODUCT_CONTRACT_MI_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.product_contract_mi_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_mi_edit_locked",
            "bg.energo.phoenix.security.verb.product_contract_mi_edit_locked.desc"
    ),
    PRODUCT_CONTRACT_GENERATE("bg.energo.phoenix.security.verb.product_contract_generate", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_generate",
            "bg.energo.phoenix.security.verb.product_contract_generate.desc"),
    PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE("bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_base", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_base",
            "bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_base.desc"),
    PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE("bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_advance", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_advance",
            "bg.energo.phoenix.security.verb.product_contract_generate_additional_agreement_advance.desc"),
    INDIVIDUAL_PRODUCT_CONTRACT_CREATE(
            "bg.energo.phoenix.security.verb.individual_product_contract_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_contract_create",
            "bg.energo.phoenix.security.verb.individual_product_contract_create.desc"
    ),

    INDIVIDUAL_PRODUCT_CONTRACT_VIEW(
            "bg.energo.phoenix.security.verb.individual_product_contract_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.product_contract_",
            "bg.energo.phoenix.security.verb.product_contract_.desc"
    ),

    INDIVIDUAL_PRODUCT_CONTRACT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.individual_product_contract_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_contract_view_deleted",
            "bg.energo.phoenix.security.verb.individual_product_contract_view_deleted.desc"
    ),

    INDIVIDUAL_PRODUCT_CONTRACT_EDIT(
            "bg.energo.phoenix.security.verb.individual_product_contract_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_contract_edit",
            "bg.energo.phoenix.security.verb.individual_product_contract_edit.desc"
    ),
    INDIVIDUAL_PRODUCT_CONTRACT_DELETE(
            "bg.energo.phoenix.security.verb.individual_product_contract_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.individual_product_contract_delete",
            "bg.energo.phoenix.security.verb.individual_product_contract_delete.desc"
    ),
    INTEREST_RATES_CREATE(
            "bg.energo.phoenix.security.verb.interest_rates_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interest_rates_create",
            "bg.energo.phoenix.security.verb.interest_rates_create.desc"
    ),
    INTEREST_RATES_EDIT(
            "bg.energo.phoenix.security.verb.interest_rates_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interest_rates_edit",
            "bg.energo.phoenix.security.verb.interest_rates_edit.desc"
    ),
    INTEREST_RATES_DELETE(
            "bg.energo.phoenix.security.verb.interest_rates_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interest_rates_delete",
            "bg.energo.phoenix.security.verb.interest_rates_delete.desc"
    ),
    INTEREST_RATES_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.interest_rates_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interest_rates_view_basic",
            "bg.energo.phoenix.security.verb.interest_rates_view_basic.desc"
    ),
    INTEREST_RATES_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.interest_rates_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.interest_rates_view_deleted",
            "bg.energo.phoenix.security.verb.interest_rates_view_deleted.desc"
    ),
    TASK_CREATE(
            "bg.energo.phoenix.security.verb.task_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_create",
            "bg.energo.phoenix.security.verb.task_create.desc"
    ),
    CUSTOMER_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.customer_edit_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_edit_task_on_preview",
            "bg.energo.phoenix.security.verb.customer_edit_task_on_preview.desc"

    ),
    CONTRACT_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.contract_create_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.contract_create_task_on_preview",
            "bg.energo.phoenix.security.verb.contract_create_task_on_preview.desc"

    ),
    ORDER_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.order_create_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.order_create_task_on_preview",
            "bg.energo.phoenix.security.verb.order_create_task_on_preview.desc"

    ),
    BILLING_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.billing_create_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.billing_create_task_on_preview",
            "bg.energo.phoenix.security.verb.billing_create_task_on_preview.desc"

    ),

    RECEIVABLE_BLOCKING_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.receivable_blocking_create_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_create_task_on_preview",
            "bg.energo.phoenix.security.verb.receivable_blocking_create_task_on_preview.desc"

    ),

    DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK_ON_PREVIEW(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task_on_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task_on_preview",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task_on_preview.desc"

    ),

    DISCONNECTION_POWER_SUPPLY_REQUEST_CREATE_TASK(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_request_create_task.desc"

    ),

    CUSTOMER_ASSESSMENT_CREATE_TASK(
            "bg.energo.phoenix.security.verb.customer_assessment_create_task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_create_task",
            "bg.energo.phoenix.security.verb.customer_assessment_create_task.desc"

    ),
    RESCHEDULING_TO_A_BALANCING_GROUP_COORDINATOR_CREATE_TASK(
            "bg.energo.phoenix.security.verb.rescheduling_to_change_balancing_group_coordinator_create_task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling_to_change_balancing_group_coordinator_create_task",
            "bg.energo.phoenix.security.verb.rescheduling_to_change_balancing_group_coordinator_create_task.desc"
    ),
    TASK_EDIT(
            "bg.energo.phoenix.security.verb.task_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_edit",
            "bg.energo.phoenix.security.verb.task_edit.desc"
    ),
    TASK_EDIT_SUPER_USER(
            "bg.energo.phoenix.security.verb.task_edit_super_user", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_edit_super_user",
            "bg.energo.phoenix.security.verb.task_edit_super_user.desc"
    ),
    TASK_DELETE(
            "bg.energo.phoenix.security.verb.task_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_delete",
            "bg.energo.phoenix.security.verb.task_delete.desc"
    ),
    TASK_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.task_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_view_basic",
            "bg.energo.phoenix.security.verb.task_view_basic.desc"
    ),
    TASK_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.task_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.task_view_deleted",
            "bg.energo.phoenix.security.verb.task_view_deleted.desc"
    ),
    SYSTEM_ACTIVITY_CREATE(
            "bg.energo.phoenix.security.verb.system_activity_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_create",
            "bg.energo.phoenix.security.verb.system_activity_create.desc"
    ),
    CREATE_ACTIVITY_FROM_CONTRACT_PREVIEW(
            "bg.energo.phoenix.security.verb.create_activity_from_contract_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_activity_from_contract_preview",
            "bg.energo.phoenix.security.verb.create_activity_from_contract_preview.desc"
    ),
    CREATE_ACTIVITY_FROM_ORDER_PREVIEW(
            "bg.energo.phoenix.security.verb.create_activity_from_order_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_activity_from_order_preview",
            "bg.energo.phoenix.security.verb.create_activity_from_order_preview.desc"
    ),
    CREATE_ACTIVITY_FROM_TASK_PREVIEW(
            "bg.energo.phoenix.security.verb.create_activity_from_task_preview", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_activity_from_task_preview",
            "bg.energo.phoenix.security.verb.create_activity_from_task_preview.desc"
    ),
    SYSTEM_ACTIVITY_EDIT(
            "bg.energo.phoenix.security.verb.system_activity_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_edit",
            "bg.energo.phoenix.security.verb.system_activity_edit.desc"
    ),
    SYSTEM_ACTIVITY_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.system_activity_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_edit_locked",
            "bg.energo.phoenix.security.verb.system_activity_edit_locked.desc"
    ),
    SYSTEM_ACTIVITY_DELETE(
            "bg.energo.phoenix.security.verb.system_activity_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_delete",
            "bg.energo.phoenix.security.verb.system_activity_delete.desc"
    ),
    SYSTEM_ACTIVITY_VIEW_BASIC(
            "bg.energo.phoenix.security.verb.system_activity_view_basic", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_view_basic",
            "bg.energo.phoenix.security.verb.system_activity_view_basic.desc"
    ),
    SYSTEM_ACTIVITY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.system_activity_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.system_activity_view_deleted",
            "bg.energo.phoenix.security.verb.system_activity_view_deleted.desc"
    ),
    SERVICE_CONTRACT_CREATE(
            "bg.energo.phoenix.security.verb.service_contract_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_create",
            "bg.energo.phoenix.security.verb.service_contract_create.desc"
    ),

    SERVICE_CONTRACT_VIEW(
            "bg.energo.phoenix.security.verb.service_contract_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_view",
            "bg.energo.phoenix.security.verb.service_contract_view.desc"
    ),

    SERVICE_CONTRACT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.service_contract_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_view_deleted",
            "bg.energo.phoenix.security.verb.service_contract_view_deleted.desc"
    ),

    SERVICE_CONTRACT_EDIT(
            "bg.energo.phoenix.security.verb.service_contract_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_edit",
            "bg.energo.phoenix.security.verb.service_contract_edit.desc"
    ),
    SERVICE_CONTRACT_EDIT_READY(
            "bg.energo.phoenix.security.verb.service_contract_edit_ready", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_edit_ready",
            "bg.energo.phoenix.security.verb.service_contract_edit_ready.desc"
    ),
    SERVICE_CONTRACT_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.service_contract_edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_edit_draft",
            "bg.energo.phoenix.security.verb.service_contract_edit_draft.desc"
    ),
    SERVICE_CONTRACT_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.service_contract_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_edit_locked",
            "bg.energo.phoenix.security.verb.service_contract_edit_locked.desc"
    ),
    SERVICE_CONTRACT_EDIT_STATUSES(
            "bg.energo.phoenix.security.verb.service_contract_edit_statuses", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_edit_statuses",
            "bg.energo.phoenix.security.verb.service_contract_edit_statuses.desc"
    ),
    SERVICE_CONTRACT_DELETE(
            "bg.energo.phoenix.security.verb.service_contract_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_delete",
            "bg.energo.phoenix.security.verb.service_contract_delete.desc"
    ),
    SERVICE_CONTRACT_GENERATE("bg.energo.phoenix.security.verb.service_contract_generate", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_generate",
            "bg.energo.phoenix.security.verb.service_contract_generate.desc"),
    SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE("bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_base", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_base",
            "bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_base.desc"),
    SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE("bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_advance", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_advance",
            "bg.energo.phoenix.security.verb.service_contract_generate_additional_agreement_advance.desc"),
    SERVICE_ORDER_CREATE(
            "bg.energo.phoenix.security.verb.service_order_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_create",
            "bg.energo.phoenix.security.verb.service_order_create.desc"
    ),
    SERVICE_ORDER_EDIT_REQUESTED(
            "bg.energo.phoenix.security.verb.service_order_edit_requested", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_edit_requested",
            "bg.energo.phoenix.security.verb.service_order_edit_requested.desc"
    ),
    SERVICE_ORDER_EDIT_CONFIRMED(
            "bg.energo.phoenix.security.verb.service_order_edit_confirmed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_edit_confirmed",
            "bg.energo.phoenix.security.verb.service_order_edit_confirmed.desc"
    ),
    SERVICE_ORDER_EDIT_STATUSES(
            "bg.energo.phoenix.security.verb.service_order_edit_statuses", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_edit_statuses",
            "bg.energo.phoenix.security.verb.service_order_edit_statuses.desc"
    ),
    SERVICE_ORDER_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.service_order_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_edit_locked",
            "bg.energo.phoenix.security.verb.service_order_edit_locked.desc"
    ),
    SERVICE_ORDER_CREATE_PRO_FORMA_INVOICE(
            "bg.energo.phoenix.security.verb.service_order_create_pro_forma_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_create_pro_forma_invoice",
            "bg.energo.phoenix.security.verb.service_order_create_pro_forma_invoice.desc"
    ),
    SERVICE_ORDER_CREATE_INVOICE(
            "bg.energo.phoenix.security.verb.service_order_create_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_create_invoice",
            "bg.energo.phoenix.security.verb.service_order_create_invoice.desc"
    ),
    SERVICE_ORDER_DELETE(
            "bg.energo.phoenix.security.verb.service_order_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_delete",
            "bg.energo.phoenix.security.verb.service_order_delete.desc"
    ),
    SERVICE_ORDER_VIEW(
            "bg.energo.phoenix.security.verb.service_order_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_view",
            "bg.energo.phoenix.security.verb.service_order_view.desc"
    ),
    SERVICE_ORDER_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.service_order_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_order_view_deleted",
            "bg.energo.phoenix.security.verb.service_order_view_deleted.desc"
    ),
    GOODS_ORDER_CREATE(
            "bg.energo.phoenix.security.verb.goods_order_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_create",
            "bg.energo.phoenix.security.verb.goods_order_create.desc"
    ),
    GOODS_ORDER_EDIT_REQUESTED(
            "bg.energo.phoenix.security.verb.goods_order_edit_requested", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_edit_requested",
            "bg.energo.phoenix.security.verb.goods_order_edit_requested.desc"
    ),
    GOODS_ORDER_EDIT_CONFIRMED(
            "bg.energo.phoenix.security.verb.goods_order_edit_confirmed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_edit_confirmed",
            "bg.energo.phoenix.security.verb.goods_order_edit_confirmed.desc"
    ),
    GOODS_ORDER_EDIT_STATUS(
            "bg.energo.phoenix.security.verb.goods_order_edit_status", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_edit_status",
            "bg.energo.phoenix.security.verb.goods_order_edit_status.desc"
    ),
    GOODS_ORDER_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.goods_order_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_edit_locked",
            "bg.energo.phoenix.security.verb.goods_order_edit_locked.desc"
    ),
    GOODS_ORDER_DELETE(
            "bg.energo.phoenix.security.verb.goods_order_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_delete",
            "bg.energo.phoenix.security.verb.goods_order_delete.desc"
    ),
    GOODS_ORDER_VIEW(
            "bg.energo.phoenix.security.verb.goods_order_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_view",
            "bg.energo.phoenix.security.verb.goods_order_view.desc"
    ),
    GOODS_ORDER_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.goods_order_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_view_deleted",
            "bg.energo.phoenix.security.verb.goods_order_view_deleted.desc"
    ),
    GOODS_ORDER_CREATE_PRO_FORMA_INVOICE(
            "bg.energo.phoenix.security.verb.goods_order_create_pro_forma_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_create_pro_forma_invoice",
            "bg.energo.phoenix.security.verb.goods_order_create_pro_forma_invoice.desc"
    ),
    GOODS_ORDER_CREATE_INVOICE(
            "bg.energo.phoenix.security.verb.goods_order_create_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.goods_order_create_invoice",
            "bg.energo.phoenix.security.verb.goods_order_create_invoice.desc"
    ),
    EXPRESS_CONTRACT_CREATE(
            "bg.energo.phoenix.security.verb.express-contract-create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.express-contract-create",
            "bg.energo.phoenix.security.verb.express-contract-create.desc"
    ),
    ACTIONS_CREATE(
            "bg.energo.phoenix.security.verb.actions_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_create",
            "bg.energo.phoenix.security.verb.actions_create.desc"
    ),
    ACTIONS_EDIT(
            "bg.energo.phoenix.security.verb.actions_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_edit",
            "bg.energo.phoenix.security.verb.actions_edit.desc"
    ),
    ACTIONS_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.actions_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_edit_locked",
            "bg.energo.phoenix.security.verb.actions_edit_locked.desc"
    ),
    ACTIONS_DELETE(
            "bg.energo.phoenix.security.verb.actions_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_delete",
            "bg.energo.phoenix.security.verb.actions_delete.desc"
    ),
    ACTIONS_VIEW_ACTIVE(
            "bg.energo.phoenix.security.verb.actions_view_active", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_view_active",
            "bg.energo.phoenix.security.verb.actions_view_active.desc"
    ),
    ACTIONS_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.actions_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_view_deleted",
            "bg.energo.phoenix.security.verb.actions_view_deleted.desc"
    ),
    ACTIONS_CLAIM_PENALTY(
            "bg.energo.phoenix.security.verb.actions_claim_penalty", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.actions_claim_penalty",
            "bg.energo.phoenix.security.verb.actions_claim_penalty.desc"
    ),
    SERVICE_CONTRACT_MI_CREATE(
            "bg.energo.phoenix.security.verb.service_contract_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_mi_create",
            "bg.energo.phoenix.security.verb.service_contract_mi_create.desc"
    ),
    SERVICE_CONTRACT_MI_EDIT(
            "bg.energo.phoenix.security.verb.service_contract_mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_mi_edit",
            "bg.energo.phoenix.security.verb.service_contract_mi_edit.desc"
    ),
    SERVICE_CONTRACT_EDIT_CLOCKED(
            "bg.energo.phoenix.security.verb.service_contract_mi_edit_locked", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.service_contract_mi_edit_locked",
            "bg.energo.phoenix.security.verb.service_contract_mi_edit_locked.desc"
    ),
    POD_MANUAL_ACTIVATION_DEACTIVATION(
            "bg.energo.phoenix.security.verb.pod_manual_activation_deactivation", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pod_manual_activation_deactivation",
            "bg.energo.phoenix.security.verb.pod_manual_activation_deactivation.desc"
    ),

    COMPANY_DETAIL_CREATE(
            "bg.energo.phoenix.security.verb.company_detail_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.company_detail_create",
            "bg.energo.phoenix.security.verb.company_detail_create.desc"
    ),
    COMPANY_DETAIL_VIEW(
            "bg.energo.phoenix.security.verb.company_detail_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.company_detail_view",
            "bg.energo.phoenix.security.verb.company_detail_view.desc"
    ),

    COMPANY_DETAIL_EDIT(
            "bg.energo.phoenix.security.verb.company_detail_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.company_detail_edit",
            "bg.energo.phoenix.security.verb.company_detail_edit.desc"
    ),
    ACCOUNTING_PERIOD_EDIT(
            "bg.energo.phoenix.security.verb.accounting_period_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.accounting_period_edit",
            "bg.energo.phoenix.security.verb.accounting_period_edit.desc"
    ),
    ACCOUNTING_PERIOD_VIEW(
            "bg.energo.phoenix.security.verb.accounting_period_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.accounting_period_view",
            "bg.energo.phoenix.security.verb.accounting_period_view.desc"
    ),
    CREATE_BILLING_RUN(
            "bg.energo.phoenix.security.verb.create_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run",
            "bg.energo.phoenix.security.verb.create_billing_run.desc"
    ),
    EDIT_BILLING_RUN(
            "bg.energo.phoenix.security.verb.edit_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run",
            "bg.energo.phoenix.security.verb.edit_billing_run.desc"
    ),
    EDIT_BILLING_RUN_WITH_STATUS(
            "bg.energo.phoenix.security.verb.edit_billing_run_with_status", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_with_status",
            "bg.energo.phoenix.security.verb.edit_billing_run_with_status.desc"
    ),
    VIEW_BILLING_RUN(
            "bg.energo.phoenix.security.verb.view_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run",
            "bg.energo.phoenix.security.verb.view_billing_run.desc"
    ),
    VIEW_STANDARD_BILLING_RUN(
            "bg.energo.phoenix.security.verb.view_standard_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_standard_billing_run",
            "bg.energo.phoenix.security.verb.view_standard_billing_run.desc"
    ),
    VIEW_PERIOD_BILLING_RUN(
            "bg.energo.phoenix.security.verb.view_periodic_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_periodic_billing_run",
            "bg.energo.phoenix.security.verb.view_periodic_billing_run.desc"
    ),
    VIEW_DELETED_BILLING_RUN(
            "bg.energo.phoenix.security.verb.view_deleted_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_deleted_billing_run",
            "bg.energo.phoenix.security.verb.view_deleted_billing_run.desc"
    ),
    DELETE_BILLING_RUN(
            "bg.energo.phoenix.security.verb.delete_billing_run", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run",
            "bg.energo.phoenix.security.verb.delete_billing_run.desc"
    ),
    CREATE_BILLING_RUN_STANDARD(
            "bg.energo.phoenix.security.verb.create_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_standard",
            "bg.energo.phoenix.security.verb.create_billing_run_standard.desc"
    ),
    EDIT_BILLING_RUN_STANDARD(
            "bg.energo.phoenix.security.verb.edit_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_standard",
            "bg.energo.phoenix.security.verb.edit_billing_run_standard.desc"
    ),
    VIEW_BILLING_RUN_STANDARD(
            "bg.energo.phoenix.security.verb.view_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_standard",
            "bg.energo.phoenix.security.verb.view_billing_run_standard.desc"
    ),
    DELETE_BILLING_RUN_STANDARD(
            "bg.energo.phoenix.security.verb.delete_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_standard",
            "bg.energo.phoenix.security.verb.delete_billing_run_standard.desc"
    ),
    START_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.start_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_standard",
            "bg.energo.phoenix.security.verb.start_billing_run_standard.desc"
    ),
    PAUSE_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.pause_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_standard",
            "bg.energo.phoenix.security.verb.pause_billing_run_standard.desc"
    ),
    CONTINUE_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.continue_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_standard",
            "bg.energo.phoenix.security.verb.continue_billing_run_standard.desc"
    ),
    START_GENERATING_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_standard",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_standard.desc"
    ),
    TERMINATE_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.terminate_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_standard",
            "bg.energo.phoenix.security.verb.terminate_billing_run_standard.desc"
    ),
    START_ACCOUNTING_BILLING_STANDARD(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_standard", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_standard",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_standard.desc"
    ),
    CREATE_BILLING_RUN_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.create_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.create_billing_run_manual_invoice.desc"
    ),
    EDIT_BILLING_RUN_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_invoice.desc"
    ),
    VIEW_BILLING_RUN_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.view_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.view_billing_run_manual_invoice.desc"
    ),
    DELETE_BILLING_RUN_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_invoice.desc"
    ),
    START_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.start_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.start_billing_run_manual_invoice.desc"
    ),
    PAUSE_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_invoice.desc"
    ),
    CONTINUE_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_invoice.desc"
    ),
    START_GENERATING_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_invoice.desc"
    ),
    TERMINATE_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_invoice.desc"
    ),
    START_ACCOUNTING_BILLING_MANUAL_INVOICE(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_invoice", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_invoice",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_invoice.desc"
    ),
    CREATE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.create_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.create_billing_run_manual_interim_advance_payment.desc"
    ),
    EDIT_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_interim_advance_payment.desc"
    ),
    VIEW_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.view_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.view_billing_run_manual_interim_advance_payment.desc"
    ),
    DELETE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_interim_advance_payment.desc"
    ),
    START_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.start_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.start_billing_run_manual_interim_advance_payment.desc"
    ),
    PAUSE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_interim_advance_payment.desc"
    ),
    CONTINUE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_interim_advance_payment.desc"
    ),
    START_GENERATING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_interim_advance_payment.desc"
    ),
    TERMINATE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_interim_advance_payment.desc"
    ),
    START_ACCOUNTING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_interim_advance_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_interim_advance_payment",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_interim_advance_payment.desc"
    ),
    CREATE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.create_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.create_billing_run_manual_credit_or_debit_note.desc"
    ),
    EDIT_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.edit_billing_run_manual_credit_or_debit_note.desc"
    ),
    VIEW_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.view_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.view_billing_run_manual_credit_or_debit_note.desc"
    ),
    DELETE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.delete_billing_run_manual_credit_or_debit_note.desc"
    ),
    START_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.start_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.start_billing_run_manual_credit_or_debit_note.desc"
    ),
    PAUSE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.pause_billing_run_manual_credit_or_debit_note.desc"
    ),
    CONTINUE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.continue_billing_run_manual_credit_or_debit_note.desc"
    ),
    START_GENERATING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_manual_credit_or_debit_note.desc"
    ),
    TERMINATE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.terminate_billing_run_manual_credit_or_debit_note.desc"
    ),
    START_ACCOUNTING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_credit_or_debit_note", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_credit_or_debit_note",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_manual_credit_or_debit_note.desc"
    ),
    CREATE_BILLING_RUN_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_correction.desc"
    ),
    EDIT_BILLING_RUN_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_correction.desc"
    ),
    VIEW_BILLING_RUN_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_correction.desc"
    ),
    DELETE_BILLING_RUN_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_correction.desc"
    ),
    START_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_correction.desc"
    ),
    PAUSE_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_correction.desc"
    ),
    CONTINUE_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_correction.desc"
    ),
    START_GENERATING_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_correction.desc"
    ),
    TERMINATE_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_correction.desc"
    ),
    START_ACCOUNTING_BILLING_INVOICE_CORRECTION(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_correction", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_correction",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_correction.desc"
    ),
    CREATE_BILLING_RUN_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.create_billing_run_invoice_reversal.desc"
    ),
    EDIT_BILLING_RUN_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.edit_billing_run_invoice_reversal.desc"
    ),
    VIEW_BILLING_RUN_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.view_billing_run_invoice_reversal.desc"
    ),
    DELETE_BILLING_RUN_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.delete_billing_run_invoice_reversal.desc"
    ),
    START_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.start_billing_run_invoice_reversal.desc"
    ),
    PAUSE_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.pause_billing_run_invoice_reversal.desc"
    ),
    CONTINUE_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.continue_billing_run_invoice_reversal.desc"
    ),
    START_GENERATING_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.start_generating_billing_run_invoice_reversal.desc"
    ),
    TERMINATE_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.terminate_billing_run_invoice_reversal.desc"
    ),
    START_ACCOUNTING_BILLING_INVOICE_REVERSAL(
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_reversal",
            "bg.energo.phoenix.security.verb.start_accounting_billing_run_invoice_reversal.desc"
    ),
    PROCESS_PERIODICITY_CREATE(
            "bg.energo.phoenix.security.verb.process_periodicity.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_periodicity.create",
            "bg.energo.phoenix.security.verb.process_periodicity.create.desc"
    ),
    PROCESS_PERIODICITY_VIEW(
            "bg.energo.phoenix.security.verb.process_periodicity.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_periodicity.view",
            "bg.energo.phoenix.security.verb.process_periodicity.view.desc"
    ),
    PROCESS_PERIODICITY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.process_periodicity.view.deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_periodicity.view.deleted",
            "bg.energo.phoenix.security.verb.process_periodicity.view.deleted.desc"
    ),
    PROCESS_PERIODICITY_DELETE(
            "bg.energo.phoenix.security.verb.process_periodicity.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_periodicity.delete",
            "bg.energo.phoenix.security.verb.process_periodicity.delete.desc"
    ),
    INVOICE_VIEW(
            "bg.energo.phoenix.security.verb.invoice.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.invoice.view",
            "bg.energo.phoenix.security.verb.invoice.view.desc"
    ),
    INVOICE_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.invoice.view_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.invoice.view_draft",
            "bg.energo.phoenix.security.verb.invoice.view_draft.desc"
    ),
    INVOICE_CANCELLATION(
            "bg.energo.phoenix.security.verb.invoice.cancellation", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.invoice.cancellation",
            "bg.energo.phoenix.security.verb.invoice.cancellation.desc"
    ),
    RECEIVABLE_BLOCKING_CREATE_AS_DRAFT(
            "bg.energo.phoenix.security.verb.receivable_blocking_create_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_create_draft",
            "bg.energo.phoenix.security.verb.receivable_blocking_create_draft.desc"
    ),
    RECEIVABLE_BLOCKING_CREATE_AS_EXECUTE(
            "bg.energo.phoenix.security.verb.receivable_blocking_create_execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_create_execute",
            "bg.energo.phoenix.security.verb.receivable_blocking_create_execute.desc"
    ),
    RECEIVABLE_BLOCKING_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft",
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft.desc"
    ),
    RECEIVABLE_BLOCKING_VIEW_EXECUTED(
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed",
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed.desc"
    ),
    RECEIVABLE_BLOCKING_EDIT_EXECUTED(
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_executed",
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_executed.desc"
    ),
    RECEIVABLE_BLOCKING_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_draft",
            "bg.energo.phoenix.security.verb.receivable_blocking_edit_draft.desc"
    ),
    RECEIVABLE_BLOCKING_VIEW_DELETED_EXECUTED(
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed_deleted",
            "bg.energo.phoenix.security.verb.receivable_blocking_view_executed_deleted.desc"
    ),
    RECEIVABLE_BLOCKING_VIEW_DELETED_DRAFT(
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft_deleted",
            "bg.energo.phoenix.security.verb.receivable_blocking_view_draft_deleted.desc"
    ),
    RECEIVABLE_BLOCKING_DELETE_DRAFT(
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_draft",
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_draft.desc"
    ),

    RECEIVABLE_BLOCKING_DELETE_EXECUTED(
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_executed",
            "bg.energo.phoenix.security.verb.receivable_blocking_delete_executed.desc"
    ),
    PROCESS_PERIODICITY_UPDATE(
            "bg.energo.phoenix.security.verb.process_periodicity.update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.process_periodicity.update",
            "bg.energo.phoenix.security.verb.process_periodicity.update.desc"
    ),
    CUSTOMER_RECEIVABLE_CREATE(
            "bg.energo.phoenix.security.verb.customer_receivable_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_create",
            "bg.energo.phoenix.security.verb.customer_receivable_create.desc"
    ),
    CUSTOMER_RECEIVABLE_UPDATE(
            "bg.energo.phoenix.security.verb.customer_receivable_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_update",
            "bg.energo.phoenix.security.verb.customer_receivable_update.desc"
    ),
    CUSTOMER_RECEIVABLE_VIEW(
            "bg.energo.phoenix.security.verb.customer_receivable_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_view",
            "bg.energo.phoenix.security.verb.customer_receivable_view.desc"
    ),
    CUSTOMER_RECEIVABLE_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.customer_receivable_view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_view_delete",
            "bg.energo.phoenix.security.verb.customer_receivable_view_delete.desc"
    ), CUSTOMER_RECEIVABLE_DELETE(
            "bg.energo.phoenix.security.verb.customer_receivable_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_receivable_delete",
            "bg.energo.phoenix.security.verb.customer_receivable_delete.desc"
    ), BLOCKED_FOR_LIABILITIES_OFFSETTING(
            "bg.energo.phoenix.security.verb.blocked_for_liabilities_offsetting", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.blocked_for_liabilities_offsetting",
            "bg.energo.phoenix.security.verb.blocked_for_liabilities_offsetting.desc"
    ),
    CREATE_COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.verb.collection_channel.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.collection_channel.create",
            "bg.energo.phoenix.security.verb.collection_channel.create.desc"
    ),

    EDIT_COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.verb.collection_channel.update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.collection_channel.update",
            "bg.energo.phoenix.security.verb.collection_channel.update.desc"
    ),

    VIEW_COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.verb.collection_channel.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.collection_channel.view",
            "bg.energo.phoenix.security.verb.collection_channel.view.desc"
    ),

    VIEW_DELETED_COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.verb.collection_channel.view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.collection_channel.view_deleted",
            "bg.energo.phoenix.security.verb.collection_channel.view_deleted.desc"
    ),

    DELETE_COLLECTION_CHANNEL(
            "bg.energo.phoenix.security.verb.collection_channel.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.collection_channel.delete",
            "bg.energo.phoenix.security.verb.collection_channel.delete.desc"
    ),
    CUSTOMER_LIABILITY_CREATE(
            "bg.energo.phoenix.security.verb.customer_liability_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_create",
            "bg.energo.phoenix.security.verb.customer_liability_create.desc"
    ),
    CUSTOMER_LIABILITY_UPDATE(
            "bg.energo.phoenix.security.verb.customer_liability_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_update",
            "bg.energo.phoenix.security.verb.customer_liability_update.desc"
    ),
    CUSTOMER_LIABILITY_VIEW(
            "bg.energo.phoenix.security.verb.customer_liability_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_view",
            "bg.energo.phoenix.security.verb.customer_liability_view.desc"
    ),
    CUSTOMER_LIABILITY_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.customer_liability_view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_view_delete",
            "bg.energo.phoenix.security.verb.customer_liability_view_delete.desc"
    ),
    CUSTOMER_LIABILITY_DELETE(
            "bg.energo.phoenix.security.verb.customer_liability_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_delete",
            "bg.energo.phoenix.security.verb.customer_liability_delete.desc"
    ),
    CUSTOMER_LIABILITY_BLOCKED_FOR_PAYMENT(
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_payment",
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_payment.desc"
    ),
    CUSTOMER_LIABILITY_BLOCKED_FOR_REMINDER_LETTERS(
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_reminder_letters", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_reminder_letters",
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_reminder_letters.desc"
    ),
    CUSTOMER_LIABILITY_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS(
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_calculation_of_late_payment_fines_interests", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_calculation_of_late_payment_fines_interests",
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_calculation_of_late_payment_fines_interests.desc"
    ),
    CUSTOMER_LIABILITY_BLOCKED_FOR_LIABILITIES_OFFSETTING(
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_liabilities_offsetting", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_liabilities_offsetting",
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_liabilities_offsetting.desc"
    ),
    CUSTOMER_LIABILITY_BLOCKED_FOR_SUPPLY_TERMINATION(
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_supply_termination", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_supply_termination",
            "bg.energo.phoenix.security.verb.customer_liability_blocked_for_supply_termination.desc"
    ),
    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT(
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_payment", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_payment",
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_payment.desc"
    ),
    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS(
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_reminder_letters", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_reminder_letters",
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_reminder_letters.desc"
    ),
    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS(
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_calculation_of_late_payment_fines_interests", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_calculation_of_late_payment_fines_interests",
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_calculation_of_late_payment_fines_interests.desc"
    ),
    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING(
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_liabilities_offsetting", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_liabilities_offsetting",
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_liabilities_offsetting.desc"
    ),
    CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION(
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_supply_termination", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_supply_termination",
            "bg.energo.phoenix.security.verb.customer_liability_edit_blocked_for_supply_termination.desc"
    ),
    CUSTOMER_LIABILITY_MI_CREATE(
            "bg.energo.phoenix.security.verb.customer_liability_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_mi_create",
            "bg.energo.phoenix.security.verb.customer_liability_mi_create.desc"
    ),
    CUSTOMER_LIABILITY_MI_EDIT(
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit",
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit.desc"
    ),
    CUSTOMER_LIABILITY_MI_EDIT_LOCKED(
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit_locked",
            "bg.energo.phoenix.security.verb.customer_liability_mi_edit_locked.desc"
    ),
    RECEIVABLE_PAYMENT_MANUAL_CREATE(
            "bg.energo.phoenix.security.verb.receivable-payment.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.create",
            "bg.energo.phoenix.security.verb.receivable-payment.create.desc"
    ),
    RECEIVABLE_PAYMENT_EDIT(
            "bg.energo.phoenix.security.verb.receivable-payment.update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.update",
            "bg.energo.phoenix.security.verb.receivable-payment.update.desc"
    ),
    RECEIVABLE_PAYMENT_VIEW(
            "bg.energo.phoenix.security.verb.receivable-payment.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.view",
            "bg.energo.phoenix.security.verb.receivable-payment.view.desc"
    ),
    RECEIVABLE_PAYMENT_DELETE(
            "bg.energo.phoenix.security.verb.receivable-payment.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.delete",
            "bg.energo.phoenix.security.verb.receivable-payment.delete.desc"
    ),
    RECEIVABLE_PAYMENT_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.receivable-payment.view-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.view-delete",
            "bg.energo.phoenix.security.verb.receivable-payment.view-delete.desc"
    ),
    RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING(
            "bg.energo.phoenix.security.verb.receivable-payment.block-for-liability-offsetting", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.block-for-liability-offsetting",
            "bg.energo.phoenix.security.verb.receivable-payment.block-for-liability-offsetting.desc"
    ),
    RECEIVABLE_PAYMENT_REVERSE(
            "bg.energo.phoenix.security.verb.receivable-payment.reverse", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.receivable-payment.reverse",
            "bg.energo.phoenix.security.verb.receivable-payment.reverse.desc"
    ),
    DEPOSIT_CREATE(
            "bg.energo.phoenix.security.verb.deposit_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.deposit_create",
            "bg.energo.phoenix.security.verb.deposit_create.desc"
    ),
    DEPOSIT_UPDATE(
            "bg.energo.phoenix.security.verb.deposit_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.deposit_update",
            "bg.energo.phoenix.security.verb.deposit_update.desc"
    ),
    DEPOSIT_DELETE(
            "bg.energo.phoenix.security.verb.deposit_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.deposit_delete",
            "bg.energo.phoenix.security.verb.deposit_delete.desc"
    ),
    DEPOSIT_VIEW(
            "bg.energo.phoenix.security.verb.deposit_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.deposit_view",
            "bg.energo.phoenix.security.verb.deposit_view.desc"
    ),
    DEPOSIT_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.deposit_view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.deposit_view_delete",
            "bg.energo.phoenix.security.verb.deposit_view_delete.desc"
    ),

    PAYMENT_PACKAGE_CREATE(
            "bg.energo.phoenix.security.verb.payment_package.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_package.create",
            "bg.energo.phoenix.security.verb.payment_package.create.desc"
    ),

    PAYMENT_PACKAGE_VIEW(
            "bg.energo.phoenix.security.verb.payment_package.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_package.view",
            "bg.energo.phoenix.security.verb.payment_package.view.desc"
    ),

    PAYMENT_PACKAGE_EDIT(
            "bg.energo.phoenix.security.verb.payment_package.edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_package.edit",
            "bg.energo.phoenix.security.verb.payment_package.edit.desc"
    ),
    PAYMENT_PACKAGE_DELETE(
            "bg.energo.phoenix.security.verb.payment_package.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_package.delete",
            "bg.energo.phoenix.security.verb.payment_package.delete.desc"
    ),

    PAYMENT_PACKAGE_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.payment_package.view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.payment_package.view_deleted",
            "bg.energo.phoenix.security.verb.payment_package.view_deleted.desc"
    ),
    REMINDER_CREATE(
            "bg.energo.phoenix.security.verb.reminder_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reminder_create",
            "bg.energo.phoenix.security.verb.reminder_create.desc"
    ),

    REMINDER_VIEW(
            "bg.energo.phoenix.security.verb.reminder_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reminder_view",
            "bg.energo.phoenix.security.verb.reminder_view.desc"
    ),

    REMINDER_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.reminder_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reminder_view_deleted",
            "bg.energo.phoenix.security.verb.reminder_view_deleted.desc"
    ),

    REMINDER_DELETE(
            "bg.energo.phoenix.security.verb.reminder_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reminder_delete",
            "bg.energo.phoenix.security.verb.reminder_delete.desc"
    ),

    REMINDER_EDIT(
            "bg.energo.phoenix.security.verb.reminder_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reminder_edit",
            "bg.energo.phoenix.security.verb.reminder_edit.desc"
    ),
    MANUAL_LIABILITY_OFFSETTING_CREATE(
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_create",
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_create.desc"
    ),

    MANUAL_LIABILITY_OFFSETTING_EDIT(
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_edit",
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_edit.desc"
    ),

    MANUAL_LIABILITY_OFFSETTING_VIEW(
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_view",
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_view.desc"
    ),

    MANUAL_LIABILITY_OFFSETTING_REVERSAL(
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_reversal", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_reversal",
            "bg.energo.phoenix.security.verb.manual_liability_offsetting_reversal.desc"
    ),

    LATE_PAYMENT_FINE_CREATE(
            "bg.energo.phoenix.security.verb.late_payment_fine_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.late_payment_fine_create",
            "bg.energo.phoenix.security.verb.late_payment_fine_create.desc"

    ),
    LATE_PAYMENT_FINE_UPDATE(
            "bg.energo.phoenix.security.verb.late_payment_fine_update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.late_payment_fine_update",
            "bg.energo.phoenix.security.verb.late_payment_fine_update.desc"
    ),
    LATE_PAYMENT_FINE_VIEW(
            "bg.energo.phoenix.security.verb.late_payment_fine_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.late_payment_fine_view",
            "bg.energo.phoenix.security.verb.late_payment_fine_view.desc"

    ),
    LATE_PAYMENT_FINE_REVERSE(
            "bg.energo.phoenix.security.verb.late_payment_fine_reverse", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.late_payment_fine_reverse",
            "bg.energo.phoenix.security.verb.late_payment_fine_reverse.desc"

    ),

    DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_draft",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_draft.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_EXECUTE(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_execute",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_create_requests_as_execute.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_EDIT_REQUESTS_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_edit_requests_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_edit_requests_draft",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_edit_requests_draft.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_draft",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_draft.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_EXECUTED(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_execute",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_execute.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_FEE_CHARGED(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_fee_charged", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_fee_charged",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_fee_charged.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DELETED(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_deleted",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_view_requests_deleted.desc"
    ),

    DISCONNECTION_POWER_SUPPLY_DELETED_REQUESTS_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection_power_supply_delete_requests_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection_power_supply_delete_requests_draft",
            "bg.energo.phoenix.security.verb.disconnection_power_supply_delete_requests_draft.desc"
    ),
    BALANCING_GROUP_OBJECTION_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.balancing_group_objection_create_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_create_draft",
            "bg.energo.phoenix.security.verb.balancing_group_objection_create_draft.desc"
    ),

    BALANCING_GROUP_OBJECTION_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.balancing_group_objection_edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_edit_draft",
            "bg.energo.phoenix.security.verb.balancing_group_objection_edit_draft.desc"
    ),

    BALANCING_GROUP_OBJECTION_SAVE_AND_SEND_DRAFT(
            "bg.energo.phoenix.security.verb.balancing_group_objection_save_and_send_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_save_and_send_draft",
            "bg.energo.phoenix.security.verb.balancing_group_objection_save_and_send_draft.desc"
    ),

    BALANCING_GROUP_OBJECTION_VIEW(
            "bg.energo.phoenix.security.verb.balancing_group_objection_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_view",
            "bg.energo.phoenix.security.verb.balancing_group_objection_view.desc"
    ),

    BALANCING_GROUP_OBJECTION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.balancing_group_objection_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_view_deleted",
            "bg.energo.phoenix.security.verb.balancing_group_objection_view_deleted.desc"
    ),

    BALANCING_GROUP_OBJECTION_DELETE(
            "bg.energo.phoenix.security.verb.balancing_group_objection_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.balancing_group_objection_delete",
            "bg.energo.phoenix.security.verb.balancing_group_objection_delete.desc"
    ),

    CUSTOMER_ASSESSMENT_CREATE(
            "bg.energo.phoenix.security.verb.customer_assessment_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_create",
            "bg.energo.phoenix.security.verb.customer_assessment_create.desc"
    ),
    CUSTOMER_ASSESSMENT_EDIT(
            "bg.energo.phoenix.security.verb.customer_assessment_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_edit",
            "bg.energo.phoenix.security.verb.customer_assessment_edit.desc"
    ),
    CUSTOMER_ASSESSMENT_VIEW(
            "bg.energo.phoenix.security.verb.customer_assessment_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_view",
            "bg.energo.phoenix.security.verb.customer_assessment_view.desc"
    ),
    CUSTOMER_ASSESSMENT_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.customer_assessment_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_view_deleted",
            "bg.energo.phoenix.security.verb.customer_assessment_view_deleted.desc"
    ),
    CUSTOMER_ASSESSMENT_DELETE(
            "bg.energo.phoenix.security.verb.customer_assessment_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_assessment_delete",
            "bg.energo.phoenix.security.verb.customer_assessment_delete.desc"
    ),
    RESCHEDULING_CREATE(
            "bg.energo.phoenix.security.verb.rescheduling.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.create",
            "bg.energo.phoenix.security.verb.rescheduling.create.desc"
    ),
    RESCHEDULING_CREATE_TASK(
            "bg.energo.phoenix.security.verb.rescheduling.create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.create-task",
            "bg.energo.phoenix.security.verb.rescheduling.create-task.desc"
    ),
    RESCHEDULING_UPDATE(
            "bg.energo.phoenix.security.verb.rescheduling.update", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.update",
            "bg.energo.phoenix.security.verb.rescheduling.update.desc"
    ),
    RESCHEDULING_VIEW(
            "bg.energo.phoenix.security.verb.rescheduling.view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.view",
            "bg.energo.phoenix.security.verb.rescheduling.view.desc"
    ),
    RESCHEDULING_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.rescheduling.view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.view_delete",
            "bg.energo.phoenix.security.verb.rescheduling.view_delete.desc"
    ),
    RESCHEDULING_DELETE(
            "bg.energo.phoenix.security.verb.rescheduling.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.rescheduling.delete",
            "bg.energo.phoenix.security.verb.rescheduling.delete.desc"
    ),
    RESCHEDULING_REVERSE(
            "bg.energo.phoenix.security.verb.rescheduling.reverse",AclPermissionType.SIMPLE,null,
               "bg.energo.phoenix.security.verb.rescheduling.reverse",
            "bg.energo.phoenix.security.verb.rescheduling.reverse.desc"
    )
    ,
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_draft",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_draft.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_CREATE_TASK(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_task",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_create_task.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_edit_draft",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_edit_draft.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_SAVE_AND_SEND_DRAFT(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_save_and_send_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_save_and_send_draft",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_save_and_send_draft.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view_delete",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view_delete.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_VIEW(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_view.desc"
    ),
    OBJECTION_WITHDRAWAL_TO_A_CHANGE_OF_A_BALANCING_GROUP_COORDINATOR_DELETE(
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_delete",
            "bg.energo.phoenix.security.verb.objection_withdrawal_to_a_change_of_a_balancing_group_coordinator_delete.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-draft",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-draft.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_EXECUTE(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-execute",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-execute.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-draft",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-draft.desc"
    ),


    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_EDIT_EXECUTED(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-executed",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-edit-executed.desc"
    ),


    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_EXECUTED(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-executed",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-executed.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-draft",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-draft.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-deleted",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-view-deleted.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_DELETE_DRAFT(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-delete-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-delete-draft",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-delete-draft.desc"
    ),

    CANCELLATION_OF_A_DISCONNECTION_OF_THE_POWER_SUPPLY_CREATE_TASK(
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-task",
            "bg.energo.phoenix.security.verb.cancellation-of-a-disconnection-of-the-power-supply-create-task.desc"
    ),

    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_CREATE(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.create",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.create.desc"
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.edit_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.edit_draft",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.edit_draft.desc"
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_draft",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_draft.desc"
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_executed",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_executed.desc"
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_delete",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.view_delete.desc"
    ),
    REMINDER_FOR_DISCONNECTION_OF_POWER_SUPPLY_DELETE(
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.delete",
            "bg.energo.phoenix.security.verb.power_supply_disconnection_reminders.delete.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_SAVE_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-draft",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-draft.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_SAVE_AND_EXECUTE(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-and-execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-and-execute",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save-and-execute.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_SAVE(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-save.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-draft",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-draft.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_EDIT_EXECUTED(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-executed",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-edit-executed.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-draft",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-draft.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-executed",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-executed.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-deleted",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-view-deleted.desc"
    ),
    DISCONNECTION_OF_POWER_SUPPLY_DELETE(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-delete",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-delete.desc"
    ),
    DISCONNECTION_OF_POWER_CREATE_TASK(
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-create-task",
            "bg.energo.phoenix.security.verb.disconnection-of-power-supply-create-task.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-draft",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-draft.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_CREATE_TASK(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-task",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-task.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_CREATE_EXECUTE(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-execute", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-execute",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-create-execute.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_EDIT_DRAFT(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-draft",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-draft.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_EDIT_EXECUTED(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-executed",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-edit-executed.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-draft",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-draft.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-executed", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-executed",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-executed.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-deleted",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-view-deleted.desc"
    ),
    RECONNECTION_OF_POWER_SUPPLY_DELETE(
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-delete",
            "bg.energo.phoenix.security.verb.reconnection-of-power-supply-delete.desc"
    ),
    TEST_PERMISSIONS(
            "bg.energo.phoenix.security.verb.test-permissions", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.test-permissions",
            "bg.energo.phoenix.security.verb.test-permissions.desc"
    ),
    EMAIL_COMMUNICATION_CREATE_AND_SEND(
            "bg.energo.phoenix.security.verb.email-communication-create-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-create-send",
            "bg.energo.phoenix.security.verb.email-communication-create-send.desc"
    ),
    EMAIL_COMMUNICATION_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.email-communication-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-create-draft",
            "bg.energo.phoenix.security.verb.email-communication-create-draft.desc"
    ),
    EMAIL_COMMUNICATION_DELETE(
            "bg.energo.phoenix.security.verb.email-communication-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-delete",
            "bg.energo.phoenix.security.verb.email-communication-delete.desc"
    ),

    EMAIL_COMMUNICATION_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.email-communication-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-view-draft",
            "bg.energo.phoenix.security.verb.email-communication-view-draft.desc"
    ),

    EMAIL_COMMUNICATION_VIEW_SEND(
            "bg.energo.phoenix.security.verb.email-communication-view-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-view-send",
            "bg.energo.phoenix.security.verb.email-communication-view-send.desc"
    ),

    EMAIL_COMMUNICATION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.email-communication-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-view-deleted",
            "bg.energo.phoenix.security.verb.email-communication-view-deleted.desc"
    ),

    EMAIL_COMMUNICATION_RESEND(
            "bg.energo.phoenix.security.verb.email-communication-resend", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-resend",
            "bg.energo.phoenix.security.verb.email-communication-resend.desc"
    ),
    SMS_COMMUNICATION_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.sms-communication-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-create-draft",
            "bg.energo.phoenix.security.verb.sms-communication-create-draft.desc"
    ),
    SMS_COMMUNICATION_CREATE_AND_SEND(
            "bg.energo.phoenix.security.verb.sms-communication-create-and-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-create-and-send",
            "bg.energo.phoenix.security.verb.sms-communication-create-and-send.desc"
    ),
    SMS_COMMUNICATION_CREATE_SAVE(
            "bg.energo.phoenix.security.verb.sms-communication-create-save", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-create-save",
            "bg.energo.phoenix.security.verb.sms-communication-create-save.desc"
    ),
    SMS_COMMUNICATION_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.sms-communication-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-view-draft",
            "bg.energo.phoenix.security.verb.sms-communication-view-draft.desc"),
    SMS_COMMUNICATION_VIEW_SEND(
            "bg.energo.phoenix.security.verb.sms-communication-view-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-view-send",
            "bg.energo.phoenix.security.verb.sms-communication-view-send.desc"
    ),
    SMS_COMMUNICATION_VIEW_DELETE(
            "bg.energo.phoenix.security.verb.sms-communication-view-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-view-delete",
            "bg.energo.phoenix.security.verb.sms-communication-view-delete.desc"
    ),
    SMS_COMMUNICATION_DELETE(
            "bg.energo.phoenix.security.verb.sms-communication-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-delete",
            "bg.energo.phoenix.security.verb.sms-communication-delete.desc"
    ),
    SMS_COMMUNICATION_EDIT(
            "bg.energo.phoenix.security.verb.sms-communication-edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-edit",
            "bg.energo.phoenix.security.verb.sms-communication-edit.desc"
    ),
    SMS_COMMUNICATION_CREATE_ACTIVITY(
            "bg.energo.phoenix.security.verb.sms-communication-create-activity", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-create-activity",
            "bg.energo.phoenix.security.verb.sms-communication-create-activity.desc"
    ),
    SMS_COMMUNICATION_CREATE_TASK(
            "bg.energo.phoenix.security.verb.sms-communication-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-create-task",
            "bg.energo.phoenix.security.verb.sms-communication-create-task.desc"
    ),
    SMS_COMMUNICATION_RESEND(
            "bg.energo.phoenix.security.verb.sms-communication-resend", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.sms-communication-resend",
            "bg.energo.phoenix.security.verb.sms-communication-resend.desc"
    ),
    MASS_SMS_COMMUNICATION_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-draft",
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-draft.desc"
    ),
    MASS_SMS_COMMUNICATION_CREATE_SEND(
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-send",
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-send.desc"
    ),
    MASS_SMS_COMMUNICATION_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-draft",
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-draft.desc"
    ),
    MASS_SMS_COMMUNICATION_VIEW_SENT(
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-sent", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-sent",
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-sent.desc"
    ),
    MASS_SMS_COMMUNICATION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-deleted",
            "bg.energo.phoenix.security.verb.mass-sms-communication-view-deleted.desc"
    ),
    MASS_SMS_COMMUNICATION_DELETE(
            "bg.energo.phoenix.security.verb.mass-sms-communication-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-delete",
            "bg.energo.phoenix.security.verb.mass-sms-communication-delete.desc"
    ),
    MASS_SMS_COMMUNICATION_EDIT(
            "bg.energo.phoenix.security.verb.mass-sms-communication-edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-edit",
            "bg.energo.phoenix.security.verb.mass-sms-communication-edit.desc"
    ),
    MASS_SMS_COMMUNICATION_CREATE_TASK(
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-task",
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-task.desc"
    ),
    MASS_SMS_COMMUNICATION_CREATE_ACTIVITY(
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-activity", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-activity",
            "bg.energo.phoenix.security.verb.mass-sms-communication-create-activity.desc"
    ),
    EMAIL_COMMUNICATION_EDIT(
            "bg.energo.phoenix.security.verb.email-communication-edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-edit",
            "bg.energo.phoenix.security.verb.email-communication-edit.desc"
    ),

    EMAIL_COMMUNICATION_CREATE_TASK(
            "bg.energo.phoenix.security.verb.email-communication-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-create-task",
            "bg.energo.phoenix.security.verb.email-communication-create-task.desc"

    ),

    EMAIL_COMMUNICATION_CREATE_ACTIVITY(
            "bg.energo.phoenix.security.verb.email-communication-create-activity", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.email-communication-create-activity",
            "bg.energo.phoenix.security.verb.email-communication-create-activity.desc"

    ),

    MASS_EMAIL_COMMUNICATION_VIEW_DRAFT(
            "bg.energo.phoenix.security.verb.mass-email-communication-view-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-view-draft",
            "bg.energo.phoenix.security.verb.mass-email-communication-view-draft.desc"
    ),

    MASS_EMAIL_COMMUNICATION_VIEW_SEND(
            "bg.energo.phoenix.security.verb.mass-email-communication-view-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-view-send",
            "bg.energo.phoenix.security.verb.mass-email-communication-view-send.desc"
    ),

    MASS_EMAIL_COMMUNICATION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.mass-email-communication-view-deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-view-deleted",
            "bg.energo.phoenix.security.verb.mass-email-communication-view-deleted.desc"
    ),

    MASS_EMAIL_COMMUNICATION_CREATE_AND_SEND(
            "bg.energo.phoenix.security.verb.mass-email-communication-create-send", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-create-send",
            "bg.energo.phoenix.security.verb.mass-email-communication-create-send.desc"
    ),

    MASS_EMAIL_COMMUNICATION_CREATE_DRAFT(
            "bg.energo.phoenix.security.verb.mass-email-communication-create-draft", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-create-draft",
            "bg.energo.phoenix.security.verb.mass-email-communication-create-draft.desc"
    ),


    MASS_EMAIL_COMMUNICATION_DELETE(
            "bg.energo.phoenix.security.verb.mass-email-communication-delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-delete",
            "bg.energo.phoenix.security.verb.mass-email-communication-delete.desc"
    ),


    MASS_EMAIL_COMMUNICATION_EDIT(
            "bg.energo.phoenix.security.verb.mass-email-communication-edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-edit",
            "bg.energo.phoenix.security.verb.mass-email-communication-edit.desc"
    ),

    MASS_EMAIL_COMMUNICATION_CREATE_TASK(
            "bg.energo.phoenix.security.verb.mass-email-communication-create-task", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-create-task",
            "bg.energo.phoenix.security.verb.mass-email-communication-create-task.desc"

    ),

    MASS_EMAIL_COMMUNICATION_CREATE_ACTIVITY(
            "bg.energo.phoenix.security.verb.mass-email-communication-create-activity", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.mass-email-communication-create-activity",
            "bg.energo.phoenix.security.verb.mass-email-communication-create-activity.desc"

    ),

    CREATE_TEMPLATE(
            "bg.energo.phoenix.security.verb.template_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.template_create",
            "bg.energo.phoenix.security.verb.template_create.desc"
    ),
    VIEW_TEMPLATE(
            "bg.energo.phoenix.security.verb.template_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.template_view",
            "bg.energo.phoenix.security.verb.template_view.desc"
    ),
    VIEW_DELETED_TEMPLATE(
            "bg.energo.phoenix.security.verb.template_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.template_view_deleted",
            "bg.energo.phoenix.security.verb.template_view_deleted.desc"
    ),
    EDIT_TEMPLATE(
            "bg.energo.phoenix.security.verb.template_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.template_edit",
            "bg.energo.phoenix.security.verb.template_edit.desc"
    ),
    CUSTOMER_RELATIONSHIP_VIEW(
            "bg.energo.phoenix.security.verb.customer_relationship_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_relationship_view",
            "bg.energo.phoenix.security.verb.customer_relationship_view.desc"
    ),
    CUSTOMER_RELATIONSHIP_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.customer_relationship_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.customer_relationship_view_deleted",
            "bg.energo.phoenix.security.verb.customer_relationship_view_deleted.desc"
    ),
    DELETE_TEMPLATE(
            "bg.energo.phoenix.security.verb.template_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.template_delete",
            "bg.energo.phoenix.security.verb.template_delete.desc"
    ),
    CALCULATE_DEFAULT_INTEREST(
            "bg.energo.phoenix.security.verb.calculate-default-interest", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.calculate-default-interest",
            "bg.energo.phoenix.security.verb.calculate-default-interest.desc"
    ),
    GOVERNMENT_COMPENSATION_CREATE(
            "bg.energo.phoenix.security.verb.compensation_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_create",
            "bg.energo.phoenix.security.verb.compensation_create.desc"
    ),
    GOVERNMENT_COMPENSATION_EDIT(
            "bg.energo.phoenix.security.verb.compensation_edit", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_edit",
            "bg.energo.phoenix.security.verb.compensation_edit.desc"
    ),
    GOVERNMENT_COMPENSATION_VIEW(
            "bg.energo.phoenix.security.verb.compensation_view", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_view",
            "bg.energo.phoenix.security.verb.compensation_view.desc"
    ),
    GOVERNMENT_COMPENSATION_VIEW_DELETED(
            "bg.energo.phoenix.security.verb.compensation_view_deleted", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_view_deleted",
            "bg.energo.phoenix.security.verb.compensation_view_deleted.desc"
    ),
    GOVERNMENT_COMPENSATION_DELETE(
            "bg.energo.phoenix.security.verb.compensation_delete", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_delete",
            "bg.energo.phoenix.security.verb.compensation_delete.desc"
    ),
    GOVERNMENT_COMPENSATION_MI_CREATE(
            "bg.energo.phoenix.security.verb.compensation_mi_create", AclPermissionType.SIMPLE, null,
            "bg.energo.phoenix.security.verb.compensation_mi_create",
            "bg.energo.phoenix.security.verb.compensation_mi_create.desc"
    );

    @Getter
    private final String id;

    @Getter
    private final AclPermissionType type;

    @Getter
    private final List<SubPermissionEnum> values;

    @Getter
    private final String titleKey;

    @Getter
    private final String descriptionKey;

    PermissionEnum(String id, AclPermissionType type, List<SubPermissionEnum> values, String titleKey, String descriptionKey) {
        this.id = id;
        this.type = type;
        this.values = values;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
    }

    public AclPermissionDefinition getAclPermissionDefinition() {
        AclPermissionDefinition aclPermission = new AclPermissionDefinition();
        if (values != null) {
            aclPermission.setValues(new ArrayList<>());
            for (SubPermissionEnum subPermission : values) {
                aclPermission.getValues().add(new AclValue(subPermission.getKey(),
                        subPermission.getValue(),
                        subPermission.isTranslatable()));
            }
        }
        return aclPermission;
    }

    public List<AclValue> getAclValues() {
        List<AclValue> aclValues = new ArrayList<>();
        if (values != null) {
            for (SubPermissionEnum subPermission : values) {
                aclValues.add(subPermission.getAclValue());
            }
            return aclValues;
        }
        return null;
    }
}
