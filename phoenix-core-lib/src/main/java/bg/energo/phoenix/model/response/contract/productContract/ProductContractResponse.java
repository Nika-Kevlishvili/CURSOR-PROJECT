package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import lombok.Data;

import java.util.List;


@Data
public class ProductContractResponse {
    private BasicParametersResponse basicParameters;
    private AdditionalParametersResponse additionalParameters;
    private ProductContractThirdPageFields thirdPageTabs;
    private ThirdPagePreview productParameters;
    private List<ContractPodsResponseImpl> contractPodsResponses;
    private List<BillingGroupListingResponse> billingGroups;
    private List<ProductContractVersionWithStatusResponse> versions;
    private ProductContractStatus status;
    private List<ProxyResponse> proxy;
    private Boolean locked;
    private Boolean lockedByInvoice;
}
