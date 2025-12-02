package bg.energo.phoenix.model.response.contract.order.service;

import lombok.Data;

@Data
public class ServiceOrderResponse {
    private ServiceOrderBasicParametersResponse basicParameters;
    private ServiceOrderServiceParametersResponse serviceParameters;
    private Boolean isLockedByInvoice;
}
