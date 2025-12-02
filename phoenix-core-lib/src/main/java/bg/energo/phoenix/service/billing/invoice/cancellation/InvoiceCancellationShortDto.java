package bg.energo.phoenix.service.billing.invoice.cancellation;

public interface InvoiceCancellationShortDto {

    Long getBaseInvoiceId();
    boolean getValidInvoice();
}
