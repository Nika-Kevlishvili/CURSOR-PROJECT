package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import java.math.BigDecimal;

public interface TaxCalculationExpressReconnectionResponse {
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
    BigDecimal getTaxForExpressReconnection();
    String getPriceComponent();
    Long getCurrencyId();
    Long getSavedInvoiceId();
}
