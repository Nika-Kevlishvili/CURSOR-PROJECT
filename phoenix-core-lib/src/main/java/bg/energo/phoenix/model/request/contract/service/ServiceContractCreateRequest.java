package bg.energo.phoenix.model.request.contract.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@ServiceContractBasicParametersCreateValidator
public class ServiceContractCreateRequest {

    @Valid
    private ServiceContractBasicParametersCreateRequest basicParameters;

    @Valid
    @NotNull(message = "additionalParameters-Additional parameters should not be null;")
    private ServiceContractAdditionalParametersRequest additionalParameters;

    @Valid
    private ServiceContractServiceParametersCreateRequest serviceParameters;
}
