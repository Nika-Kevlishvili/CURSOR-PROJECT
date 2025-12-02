package bg.energo.phoenix.service.contract.product.models;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;

public record ProductContractCreationPayload(ProductContract productContract,
                                             ProductContractDetails productContractDetails) {
}
