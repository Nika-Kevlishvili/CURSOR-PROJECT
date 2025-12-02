package bg.energo.phoenix.util.archivation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EDMSArchivationConstraints {
    DOCUMENT_TYPE_PRODUCT_FILE("Product File"),
    DOCUMENT_TYPE_SERVICE_FILE("Service File"),
    DOCUMENT_TYPE_PRODUCT_CONTRACT_FILE("Product Contract File"),
    DOCUMENT_TYPE_PRODUCT_CONTRACT_DOCUMENT("Product Contract Document"),
    DOCUMENT_TYPE_PRODUCT_CONTRACT_GENERATED_DOCUMENT("Product Contract Generated Document"),
    DOCUMENT_TYPE_SERVICE_CONTRACT_FILE("Service Contract File"),
    DOCUMENT_TYPE_SERVICE_CONTRACT_DOCUMENT("Service Contract Document"),
    DOCUMENT_TYPE_SERVICE_CONTRACT_GENERATED_DOCUMENT("Service Contract Generated Document"),
    DOCUMENT_TYPE_ACTION_FILE("Action File"),
    DOCUMENT_TYPE_CUSTOMER_ASSESSMENT_FILE("Customer Assessment File"),
    DOCUMENT_TYPE_CANCELLATION_OF_DISCONNECTION_FILE("Cancellation Of Disconnection File"),
    DOCUMENT_TYPE_RECONNECTION_OF_THE_POWER_SUPPLY_FILE("Reconnection Of The Power Supply File"),
    DOCUMENT_TYPE_SYSTEM_ACTIVITY_FILE("System Activity File"),
    DOCUMENT_TYPE_SMS_COMMUNICATION_FILE("SMS Communication File"),
    DOCUMENT_TYPE_EMAIL_FILE("Email File"),
    DOCUMENT_TYPE_RESCHEDULING_FILE("Rescheduling File"),
    DOCUMENT_TYPE_INVOICE_CANCELLATION_DOCUMENT("Invoice Cancellation Document"),
    DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT("Invoice Generated Document");

    private final String value;
}
