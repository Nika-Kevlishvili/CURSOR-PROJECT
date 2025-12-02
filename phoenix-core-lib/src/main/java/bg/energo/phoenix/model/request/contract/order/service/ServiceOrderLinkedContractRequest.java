package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.enums.contract.ContractType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceOrderLinkedContractRequest {

    @NotNull(message = "serviceParameters.linkedContracts.id-Linked contract id is mandatory;")
    private Long id;

    @NotNull(message = "serviceParameters.linkedContracts.type-Linked contract type is mandatory;")
    private ContractType type;

}
