package bg.energo.phoenix.service.billing.invoice.cancellation;

public interface InvoiceCancellationDto  extends InvoiceCancellationShortDto{

    Long getBillingDataId();
    Long getInterimInvoiceId();
    boolean getShouldDelete();
    Long getScaleId();
    String getType();
}
