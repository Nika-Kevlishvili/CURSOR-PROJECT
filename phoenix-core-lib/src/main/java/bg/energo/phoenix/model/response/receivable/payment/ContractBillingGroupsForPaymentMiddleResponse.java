package bg.energo.phoenix.model.response.receivable.payment;

public interface ContractBillingGroupsForPaymentMiddleResponse {
    Long getId();
    String getGroupNumber();
    Long getProductContractId();
    Long getProductContractVersion();
}