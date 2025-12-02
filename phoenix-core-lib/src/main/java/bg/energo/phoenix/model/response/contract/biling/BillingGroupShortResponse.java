package bg.energo.phoenix.model.response.contract.biling;

import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BillingGroupShortResponse {

    private Long id;

    public BillingGroupShortResponse(ContractBillingGroup group) {
        this.id = group.getId();
    }
}
