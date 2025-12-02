package bg.energo.phoenix.model.enums.billing.billings;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;

public enum DocumentType {
    DEBIT_NOTE,
    CREDIT_NOTE;

    public InvoiceDocumentType mapToInvoiceDocumentType() {
        switch (this) {
            case DEBIT_NOTE -> {
                return InvoiceDocumentType.DEBIT_NOTE;
            }
            case CREDIT_NOTE -> {
                return InvoiceDocumentType.CREDIT_NOTE;
            }
            default -> throw new IllegalArgumentsProvidedException("Cannot determinate Document Type");
        }
    }
}
