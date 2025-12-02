package bg.energo.phoenix.model.request.contract.service.edit;

import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceContractEditStatusRequest {
    @NotNull(message = "contractStatus-[contractStatus] is mandatory;")
    private ServiceContractDetailStatus contractStatus;

    @NotNull(message = "subStatus-subStatus is mandatory;")
    private ServiceContractDetailsSubStatus contractSubStatus;
    @NotNull(message = "versionStatus-versionStatus is mandatory;")
    private ContractVersionStatus contractVersionStatus;
}
