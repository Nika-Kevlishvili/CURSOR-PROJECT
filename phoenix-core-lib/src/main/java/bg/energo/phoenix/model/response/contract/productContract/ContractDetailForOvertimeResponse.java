package bg.energo.phoenix.model.response.contract.productContract;

public interface ContractDetailForOvertimeResponse {
    Long getContractId();
    Long getContractDetailId();
    Integer getInvoicePaymentTermValue();
    Long getApplicableInterestRate();
    Long getCustomerDetailId();
    Boolean getBillingGroupDirectDebit();
    Long getBillingGroupBankId();
    String getBillingGroupIban();
    Boolean getContractDirectDebit();
    Long getContractBankId();
    String getContractIban();
    Boolean getCustomerDirectDebit();
    Long getCustomerBankId();
    String getCustomerIban();
    Long getServiceOrProductDetailId();
    String getIncomeAccountNumber();
    String getCostCenterControllingOrder();
    Long getCommunicationIdForBilling();
    Long getBillingGroupCommunicationIdForBilling();
    String getContractType();
}
