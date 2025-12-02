package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractUntilValidator;
import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractUpdateRequestStartDateValidator;
import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractUpdateStatusValidator;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractAdditionalParametersRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@ProductContractUpdateRequestStartDateValidator
@ProductContractUpdateStatusValidator
public class ProductContractUpdateRequest {
    @Valid
    @ProductContractUntilValidator
    private ProductContractBasicParametersUpdateRequest basicParameters;

    @Valid
    private ProductContractAdditionalParametersRequest additionalParameters;

    @Valid
    private ProductContractProductParametersCreateRequest productParameters;

    private boolean savingAsNewVersion;

    private boolean updateDealNumber;

    private LocalDate startDate;

    @NotEmpty(message = "podRequests-podRequests must not be empty")
    private List<@Valid ContractPodRequest> podRequests;
}
