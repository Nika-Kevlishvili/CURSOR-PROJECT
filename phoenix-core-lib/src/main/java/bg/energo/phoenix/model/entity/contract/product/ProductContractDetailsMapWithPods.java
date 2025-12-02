package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductContractDetailsMapWithPods {
    private PointOfDelivery pointOfDelivery;
    private ProductContractDetails productContractDetails;
    private ContractPods contractPods;
}
