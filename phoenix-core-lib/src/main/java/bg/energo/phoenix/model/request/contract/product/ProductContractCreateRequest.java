package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractPointOfDeliveriesValidator;
import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractUntilValidator;
import bg.energo.phoenix.model.customAnotations.contract.products.ValidProductContractStatusOnCreate;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractAdditionalParametersRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ProductContractCreateRequest {

    @Valid
    @ValidProductContractStatusOnCreate
    @ProductContractUntilValidator
    private ProductContractBasicParametersCreateRequest basicParameters;

    @Valid
    private ProductContractAdditionalParametersRequest additionalParameters;

    @Valid
    private ProductContractProductParametersCreateRequest productParameters;

    @ProductContractPointOfDeliveriesValidator
    @NotEmpty(message = "productContractPointOfDeliveries-Point Of Deliveries must not be empty")
    private List<@Valid ProductContractPointOfDeliveryRequest> productContractPointOfDeliveries;
}
