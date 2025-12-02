package bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote;


public interface BillingRunInvoiceResponse {

    Long getInvoiceId();

    String getInvoiceNumber();
    Boolean getCanSelectAccordingToContract();
    Boolean getCanSelectManual();
}
