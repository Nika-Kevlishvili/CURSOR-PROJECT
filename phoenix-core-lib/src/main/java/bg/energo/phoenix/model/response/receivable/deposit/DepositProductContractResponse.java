package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.receivable.deposit.DepositProductContract;
import bg.energo.phoenix.model.enums.receivable.deposit.ContractType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositProductContractResponse {
    private Long id;
    private Long contractId;
    private ContractType type;
    private String contractName;

    public DepositProductContractResponse(DepositProductContract depositProductContract, String contractName) {
        this.id = depositProductContract.getId();
        this.contractId = depositProductContract.getContractId();
        this.type = ContractType.PRODUCT_CONTRACT;
        this.contractName = contractName;
    }
}
