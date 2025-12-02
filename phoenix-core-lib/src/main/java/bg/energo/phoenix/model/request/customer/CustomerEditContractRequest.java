package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.enums.contract.ContractType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerEditContractRequest {
    private Long contractId;
    private ContractType contractType;
}
