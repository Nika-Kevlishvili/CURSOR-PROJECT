package bg.energo.phoenix.model.response.contract.order.service;

import java.math.BigDecimal;

public interface ServiceOrderForInvoiceResponse {
    Long getId();

    Long getServiceDetailId();

    String getIncomeAccountNumber();

    String getCostCenterControllingOrder();

    Long getApplicableInterestRateId();

    Boolean getDirectDebit();

    Long getBankId();

    String getIban();

    Long getCustomerDetailId();
    Long getCustomerId();

    Long getCustomerCommunicationId();

    Long getVatRateId();

    BigDecimal getVatRatePercent();

    Long getCurrencyId();

    Integer getQuantity();

    Integer getPaymentTermValue();

    Long getTemplateDetailId();

    Long getPodQuantity();

    String getRecognizedPodIds();

    String getUnrecognizedPods();

}
