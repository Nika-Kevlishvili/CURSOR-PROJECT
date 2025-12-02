package bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieDealDatesUpdate;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;

public record XEnergieGeneratorDealDatesUpdateModel(
        ProductContract productContract,
        ProductContractDetails productContractDetails,
        ContractPods contractPods
) {
}
