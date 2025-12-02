package bg.energo.phoenix.service.notifications.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    PROCESS_CUSTOMER_MASS_IMPORT_COMPLETED("notification.process-customer-mass-import-completed"),
    PROCESS_CUSTOMER_MASS_IMPORT_ERROR("notification.process-customer-mass-import-error"),
    PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT_COMPLETED("notification.process-unwanted-customer-mass-import-completed"),
    PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT_ERROR("notification.process-unwanted-customer-mass-import-error"),
    PROCESS_POD_MASS_IMPORT_COMPLETED("notification.process-pod-mass-import-completed"),
    PROCESS_POD_MASS_IMPORT_ERROR("notification.process-pod-mass-import-error"),
    PROCESS_PRODUCT_MASS_IMPORT_COMPLETED("notification.process-product-mass-import-completed"),
    PROCESS_PRODUCT_MASS_IMPORT_ERROR("notification.process-product-mass-import-error"),
    PROCESS_SERVICE_MASS_IMPORT_ERROR("notification.process-service-mass-import-completed"),
    PROCESS_SERVICE_MASS_IMPORT_COMPLETED("notification.process-service-mass-import-error"),
    PROCESS_METER_MASS_IMPORT_COMPLETED("notification.process-meter-mass-import-completed"),
    PROCESS_METER_MASS_IMPORT_ERROR("notification.process-meter-mass-import-error"),
    PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT_COMPLETED("notification.process-supply-automatic-activation-mass-import-completed"),
    PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT_ERROR("notification.process-supply-automatic-activation-mass-import-error"),
    PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_COMPLETED("notification.process-supply-automatic-deactivation-mass-import-completed"),
    PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT_ERROR("notification.process-supply-automatic-deactivation-mass-import-error"),
    PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_COMPLETED("notification.process-supply-action-deactivation-mass-import-completed"),
    PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT_ERROR("notification.process-supply-action-deactivation-mass-import-error"),
    PRODUCT_CONTRACT_MASS_IMPORT_COMPLETED("notification.product-contract-mass-import-completed"),
    PRODUCT_CONTRACT_MASS_IMPORT_ERROR("notification.product-contract-mass-import-error"),
    PROCESS_SERVICE_CONTRACT_MASS_IMPORT_COMPLETED("notification.process-service-contract-mass-import-completed"),
    PROCESS_SERVICE_CONTRACT_MASS_IMPORT_ERROR("notification.process-service-contract-mass-import-error"),
    PROCESS_INVOICE_CANCELLATION_COMPLETED("notification.process-invoice-cancellation-completed"),
    PROCESS_INVOICE_CANCELLATION_ERROR("notification.process-invoice-cancellation-error"),
    X_ENERGIE_EXCEPTION_REPORT_COMPLETED("notification.x-energie-exception-report-completed"),
    X_ENERGIE_EXCEPTION_REPORT_ERROR("notification.x-energie-exception-report-error"),
    PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT_COMPLETED("notification.process-customer-receivable-mass-import-completed"),
    PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT_ERROR("notification.process-customer-receivable-mass-import-error"),
    PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT_COMPLETED("notification.process-customer-liability-mass-import-completed"),
    PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT_ERROR("notification.process-customer-liability-mass-import-error"),
    PROCESS_PAYMENT_MASS_IMPORT_COMPLETED("notification.process-payment-mass-import-completed"),
    PROCESS_PAYMENT_MASS_IMPORT_ERROR("notification.process-payment-mass-import-error"),
    PROCESS_REMINDER_COMPLETED("notification.process-reminder-completed"),
    PROCESS_REMINDER_ERROR("notification.process-reminder-error"),
    PAYMENT_PACKAGE_ERROR("notification.payment-package-error"),
    TASK_CREATION("notification.task-creation"),
    TASK_EXPIRATION_PERFORMER_EXISTS("notification.task-expiration-performer-exists"),
    TASK_EXPIRATION_PERFORMER_NOT_EXISTS("notification.task-expiration-performer-not-exists"),
    TASK_OVERDUE_STAGE_PERFORMERS("notification.task-overdue-stage-performers"),
    TASK_OVERDUE_PERFORMER_NOT_EXISTS("notification.task-overdue-performer-not-exists"),
    TASK_STAGE_COMPLETION_NEXT_PERFORMERS("notification.task-stage-completion-next-performers"),
    TASK_COMPLETION("notification.task-completion"),
    TASK_TERMINATED("notification.task-terminated"),
    BILLING_RUN_STARTUP("notification.billing.startup"),
    BILLING_RUN_ERROR("notification.billing.error"),
    BILLING_RUN_WARNING("notification.billing.warning"),
    BILLING_RUN_COMPLETE("notification.billing.completion"),
    PROCESS_GOVERNMENT_COMPENSATION_COMPLETE("notification.government-compensation-mass-import-completed"),
    PROCESS_GOVERNMENT_COMPENSATION_ERROR("notification.government-compensation-mass-import-error");

    private final String notificationResourceLocation;
}
