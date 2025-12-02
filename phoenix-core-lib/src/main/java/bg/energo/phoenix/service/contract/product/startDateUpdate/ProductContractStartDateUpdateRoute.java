package bg.energo.phoenix.service.contract.product.startDateUpdate;

import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStartDateUpdateRouteTypes;

import java.time.LocalDate;
import java.util.List;

@Deprecated
public interface ProductContractStartDateUpdateRoute {
    ProductContractStartDateUpdateRouteTypes getRoute();

    void recalculateDates(LocalDate requestedStartDate,
                          ProductContractDetails currentContractDetails,
                          ProductContractDetails previousVersionDetails,
                          ProductContractDetails nextVersionDetails,
                          List<String> exceptionMessages);
}
