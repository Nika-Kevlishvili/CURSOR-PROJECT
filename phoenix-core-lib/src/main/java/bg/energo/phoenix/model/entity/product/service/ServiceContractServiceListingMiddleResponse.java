package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;

public interface ServiceContractServiceListingMiddleResponse {
    Long getDetailId();
    Long getId();
    String getName();
    Long getVersionId();
    ServiceExecutionLevel getExecutionLevel();
}
