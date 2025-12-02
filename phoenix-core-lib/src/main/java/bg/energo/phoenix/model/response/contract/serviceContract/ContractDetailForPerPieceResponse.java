package bg.energo.phoenix.model.response.contract.serviceContract;

public interface ContractDetailForPerPieceResponse {
    Long getContractId();
    Long getContractDetailId();
    Integer getInvoicePaymentTermValue();
    Long getApplicableInterestRate();
    Long getCustomerDetailId();
    Boolean getContractDirectDebit();
    Long getContractBankId();
    String getContractIban();
    Boolean getCustomerDirectDebit();
    Long getCustomerBankId();
    String getCustomerIban();
    Long getServiceDetailId();
    String getIncomeAccountNumber();
    String getCostCenterControllingOrder();
    Long getCommunicationIdForBilling();
    Integer getQuantity();
}
