package bg.energo.phoenix.model.request.receivable.deposit;

import bg.energo.phoenix.model.enums.receivable.deposit.ContractType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepositContractRequest {

    @NotNull(message = "depositCreateRequest.depositContractRequest-[contractId] must not be null;")
    private Long contractId;

    @NotNull(message = "depositCreateRequest.depositContractRequest-[contractType] must not be null;")
    private ContractType contractType;
}
