package bg.energo.phoenix.model.request.contract.order.service;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class ServiceOrderUpdateRequest {

    @Valid
    private ServiceOrderBasicParametersUpdateRequest basicParameters;

    @Valid
    private ServiceOrderServiceParametersRequest serviceParameters;

}
