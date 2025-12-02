package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.receivable.deposit.DepositServiceContract;
import bg.energo.phoenix.model.enums.receivable.deposit.ContractType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositServiceContractResponse {
    private Long id;
    private Long contractId;
    private String serviceContractNumber;
    private ContractType type;

    public DepositServiceContractResponse(DepositServiceContract depositServiceContract, String serviceContractNumber) {
        this.id = depositServiceContract.getId();
        this.contractId = depositServiceContract.getContractId();
        this.type = ContractType.SERVICE_CONTRACT;
        this.serviceContractNumber = serviceContractNumber;
    }
}
