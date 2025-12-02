package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.request.contract.relatedEntities.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceContractRelatedFields {
    @NotNull(message = "serviceContractBasicParametersCreateRequest.relatedFields.type-type can not be null;")
    private EntityType type;
    @NotNull(message = "serviceContractBasicParametersCreateRequest.relatedFields.contractId-ContractId can not be null;")
    private Long contractId;
}
