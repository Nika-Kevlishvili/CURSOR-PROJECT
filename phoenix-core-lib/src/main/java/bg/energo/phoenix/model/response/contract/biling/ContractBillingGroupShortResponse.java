package bg.energo.phoenix.model.response.contract.biling;

import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContractBillingGroupShortResponse {

    private Long id;

    private String groupNumber;

    public ContractBillingGroupShortResponse(ContractBillingGroup group) {
        this.id = group.getId();
        this.groupNumber = group.getGroupNumber();
    }
}
