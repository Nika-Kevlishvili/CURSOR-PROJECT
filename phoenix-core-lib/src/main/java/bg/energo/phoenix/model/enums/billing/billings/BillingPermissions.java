package bg.energo.phoenix.model.enums.billing.billings;

import bg.energo.phoenix.permissions.PermissionEnum;

import java.util.List;
import java.util.Map;

public enum BillingPermissions {
    CREATE(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.CREATE_BILLING_RUN_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.CREATE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.CREATE_BILLING_RUN_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.CREATE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.CREATE_BILLING_RUN_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.CREATE_BILLING_RUN_INVOICE_REVERSAL
    )),
    EDIT(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.EDIT_BILLING_RUN_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.EDIT_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.EDIT_BILLING_RUN_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.EDIT_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.EDIT_BILLING_RUN_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.EDIT_BILLING_RUN_INVOICE_REVERSAL
    )),
    DELETE(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.DELETE_BILLING_RUN_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.DELETE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.DELETE_BILLING_RUN_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.DELETE_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.DELETE_BILLING_RUN_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.DELETE_BILLING_RUN_INVOICE_REVERSAL
    )),
    VIEW(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.VIEW_BILLING_RUN_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.VIEW_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.VIEW_BILLING_RUN_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.VIEW_BILLING_RUN_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.VIEW_BILLING_RUN_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.VIEW_BILLING_RUN_INVOICE_REVERSAL
    )),
    START(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.START_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.START_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.START_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.START_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.START_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.START_BILLING_INVOICE_REVERSAL
    )),
    PAUSE(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.PAUSE_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.PAUSE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.PAUSE_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.PAUSE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.PAUSE_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.PAUSE_BILLING_INVOICE_REVERSAL
    )),
    RESUME(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.CONTINUE_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.CONTINUE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.CONTINUE_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.CONTINUE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.CONTINUE_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.CONTINUE_BILLING_INVOICE_REVERSAL
    )),
    START_GENERATING(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.START_GENERATING_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.START_GENERATING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.START_GENERATING_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.START_GENERATING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.START_GENERATING_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.START_GENERATING_BILLING_INVOICE_REVERSAL
    )),
    TERMINATE(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.TERMINATE_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.TERMINATE_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.TERMINATE_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.TERMINATE_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.TERMINATE_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.TERMINATE_BILLING_INVOICE_REVERSAL
    )),
    START_ACCOUNTING(Map.of(
            BillingType.STANDARD_BILLING, PermissionEnum.START_ACCOUNTING_BILLING_STANDARD,
            BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, PermissionEnum.START_ACCOUNTING_BILLING_MANUAL_CREDIT_OR_DEBIT_NOTE,
            BillingType.MANUAL_INVOICE, PermissionEnum.START_ACCOUNTING_BILLING_MANUAL_INVOICE,
            BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT, PermissionEnum.START_ACCOUNTING_BILLING_MANUAL_INTERIM_ADVANCE_PAYMENT,
            BillingType.INVOICE_CORRECTION, PermissionEnum.START_ACCOUNTING_BILLING_INVOICE_CORRECTION,
            BillingType.INVOICE_REVERSAL, PermissionEnum.START_ACCOUNTING_BILLING_INVOICE_REVERSAL
    ));
    private final Map<BillingType, PermissionEnum> permissions;

    BillingPermissions(Map<BillingType, PermissionEnum> permissions) {
        this.permissions = permissions;
    }

    public PermissionEnum getRelevantPermission(BillingType type) {
        return this.permissions.get(type);
    }

    public List<PermissionEnum> getAllPermissionsByAction(){
        return this.permissions.values().stream().toList();
    }
}
