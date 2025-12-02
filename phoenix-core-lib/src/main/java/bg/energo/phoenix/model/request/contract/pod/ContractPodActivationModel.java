package bg.energo.phoenix.model.request.contract.pod;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractPodActivationModel {

    private ContractPods contractPods;
    private Long contractId;
}
