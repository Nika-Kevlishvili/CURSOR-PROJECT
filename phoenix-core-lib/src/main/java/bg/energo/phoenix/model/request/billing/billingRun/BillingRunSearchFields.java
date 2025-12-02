package bg.energo.phoenix.model.request.billing.billingRun;

import lombok.Getter;

public enum BillingRunSearchFields {
    ALL("ALL"),
    NUMBER("BILLINGNUMBER"),
    BILLING_TYPE("BILLINGTYPE"),
    INVOICE_DUE_DATE("INVOICEDUEDATE"),
    STATUS("STATUS"),
    TYPE_OF_PERFORMANCE("PERFORMANCETYPE"),
    PERIODICAL_BILLING_RUN("PERIODICAL_BILLING_RUN"),
    BILLING_CRITERIA("BILLINFCRITERIA"),
    APPLICATION_LEVEL("APPLICATIONLEVEL"),
    PERIODICITY("PROCESSPERIODICITYNAME");
    @Getter
    private String value;

    BillingRunSearchFields(String value) {
        this.value = value;
    }
}
