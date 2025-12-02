package bg.energo.phoenix.service.contract.product.dealCreationEvent;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;

public record ProductContractPointOfDeliveryCreationEvent(
        ContractPods pointOfDelivery
) {
}
