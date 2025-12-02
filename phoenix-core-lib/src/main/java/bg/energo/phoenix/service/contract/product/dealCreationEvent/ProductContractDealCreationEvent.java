package bg.energo.phoenix.service.contract.product.dealCreationEvent;

import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductContractDealCreationEvent {
    private ProductContractDetails productContractDetails;
}
