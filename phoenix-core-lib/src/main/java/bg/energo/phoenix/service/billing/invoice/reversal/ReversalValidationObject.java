package bg.energo.phoenix.service.billing.invoice.reversal;


public interface ReversalValidationObject {
    Long getReversalId();
    String getInvoiceNumber();
    Long getInvoiceId();
}
