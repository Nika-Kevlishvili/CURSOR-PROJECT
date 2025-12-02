package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import java.math.BigDecimal;

public interface TaxCalculationResponse {
    Long getLiabilityId();
    Long getPodId();
    Long getCustomerId();
    Long getBillingGroupId();
    Long getInvoiceId();
    Long getEmailTemplateId();
    Long getDocumentTemplateId();
    String getNumberOfIncomeAccount();
    String getBasisForIssuing();
    String getCostCenterControllingOrder();
    BigDecimal getTaxForReconnection();
    String getPriceComponent();
    Long getCurrencyId();
    Long getResultId();
}
