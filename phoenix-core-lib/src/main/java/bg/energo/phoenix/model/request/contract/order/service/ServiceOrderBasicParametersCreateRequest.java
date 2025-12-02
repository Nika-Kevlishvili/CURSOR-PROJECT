package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.contract.order.service.ValidServiceOrderProxies;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyBaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderBasicParametersCreateRequest extends ServiceOrderBasicParametersRequest {

    @NotNull(message = "basicParameters.orderStatus-Order status is mandatory;")
    private ServiceOrderStatus orderStatus;

    @ValidServiceOrderProxies
    private List<@Valid ServiceOrderProxyBaseRequest> proxies;

    @JsonIgnore
    @AssertTrue(message = "basicParameters.orderStatus-Order Status on creation must be REQUESTED or CONFIRMED;")
    public boolean isOrderStatusValid() {
        if (Objects.nonNull(orderStatus)) {
            return List.of(ServiceOrderStatus.REQUESTED, ServiceOrderStatus.CONFIRMED).contains(orderStatus);
        }
        return true;
    }
}
