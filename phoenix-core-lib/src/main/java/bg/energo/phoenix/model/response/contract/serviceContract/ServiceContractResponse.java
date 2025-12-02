package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractThirdPageFields;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersResponse;
import lombok.Data;

import java.util.List;

@Data
public class ServiceContractResponse {

    // TODO: 8/9/23 add fields later
    private ServiceContractBasicParametersResponse basicParameters;
    private ServiceContractAdditionalParametersResponse additionalParameters;
    private ServiceContractThirdPageFields thirdPageTabs;
    private ServiceParametersPreview serviceParameters;
    private List<ServiceContractVersions> versions;
    private EntityStatus status;
    private Boolean lockedByInvoice;

}
