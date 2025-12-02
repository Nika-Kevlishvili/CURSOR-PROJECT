package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.contract.order.service.ValidServiceOrderProxies;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyUpdateRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderBasicParametersUpdateRequest extends ServiceOrderBasicParametersRequest {

    @NotNull(message = "basicParameters.orderStatus-Order status is mandatory;")
    private ServiceOrderStatus orderStatus;

    @ValidServiceOrderProxies
    private List<ServiceOrderProxyUpdateRequest> proxies;

}
