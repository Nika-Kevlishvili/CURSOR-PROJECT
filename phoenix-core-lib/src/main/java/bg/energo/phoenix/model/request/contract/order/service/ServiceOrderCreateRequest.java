package bg.energo.phoenix.model.request.contract.order.service;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class ServiceOrderCreateRequest {

    @Valid
    private ServiceOrderBasicParametersCreateRequest basicParameters;

    @Valid
    private ServiceOrderServiceParametersRequest serviceParameters;

}
