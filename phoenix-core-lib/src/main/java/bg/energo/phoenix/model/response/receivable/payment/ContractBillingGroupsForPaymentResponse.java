package bg.energo.phoenix.model.response.receivable.payment;

import lombok.Data;

@Data
public class ContractBillingGroupsForPaymentResponse {

    private Long id;

    private String groupNumber;

    private Long productContractId;

    private Long productContractVersion;

    public ContractBillingGroupsForPaymentResponse(ContractBillingGroupsForPaymentMiddleResponse contractBillingGroupsForPaymentMiddleResponse) {
        this.id = contractBillingGroupsForPaymentMiddleResponse.getId();
        this.groupNumber = contractBillingGroupsForPaymentMiddleResponse.getGroupNumber();
        this.productContractId = contractBillingGroupsForPaymentMiddleResponse.getProductContractId();
        this.productContractVersion = contractBillingGroupsForPaymentMiddleResponse.getProductContractVersion();
    }
}
