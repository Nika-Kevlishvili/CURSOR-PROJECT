package bg.energo.phoenix.model.request.contract.order.service.proxy;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceOrderProxyBaseRequest {

    @Valid
    @NotNull(message = "basicParameters.proxies.proxy-Proxy is required;")
    private ServiceOrderProxyRequest proxy;

    @Valid
    private ServiceOrderAuthorizedProxyRequest authorizedProxy;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.proxies.managers")
    private List<Long> managers;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.proxies.files")
    private List<Long> files;

}
