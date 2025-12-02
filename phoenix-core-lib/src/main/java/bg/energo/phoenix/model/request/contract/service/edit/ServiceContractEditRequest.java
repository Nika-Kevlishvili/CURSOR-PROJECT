package bg.energo.phoenix.model.request.contract.service.edit;

import bg.energo.phoenix.model.customAnotations.contract.service.edit.ServiceContractBasicParametersEditValidator;
import bg.energo.phoenix.model.request.contract.service.ServiceContractAdditionalParametersRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ServiceContractBasicParametersEditValidator
public class ServiceContractEditRequest {

    public boolean savingAsNewVersion;

    @Valid
    private ServiceContractBasicParametersEditRequest basicParameters;

    @Valid
    @NotNull(message = "additionalParameters-Additional parameters should not be null;")
    private ServiceContractAdditionalParametersRequest additionalParameters;

    @Valid
    private ServiceContractServiceParametersEditRequest serviceParameters;

}
