package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
public class ResigningDataMapModel {
    private LocalDate calculatedSigningDate;
    private Map<ProductContractDetails, Map<PointOfDelivery, ContractPods>> podsMap;
}
