package bg.energo.phoenix.model.response.contract.order.service.proxy;

import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxy;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceOrderProxyBaseResponse {

    private Long id;

    private String name;

    private ServiceOrderProxyResponse proxy;

    private ServiceOrderProxyResponse authorizedProxy;

    private List<ServiceOrderProxyManagerResponse> managers;

    private List<ServiceOrderProxyFileResponse> files;

    public ServiceOrderProxyBaseResponse(ServiceOrderProxy serviceOrderProxy) {
        this.id = serviceOrderProxy.getId();
        this.proxy = new ServiceOrderProxyResponse(
                serviceOrderProxy.getForeignEntity(),
                serviceOrderProxy.getName(),
                serviceOrderProxy.getIdentifier(),
                serviceOrderProxy.getEmail(),
                serviceOrderProxy.getMobilePhone(),
                serviceOrderProxy.getAttorneyPowerNumber(),
                serviceOrderProxy.getDate(),
                serviceOrderProxy.getValidTill(),
                serviceOrderProxy.getNotaryPublic(),
                serviceOrderProxy.getRegistrationNumber(),
                serviceOrderProxy.getOperationArea()
        );
        this.authorizedProxy = new ServiceOrderProxyResponse(
                serviceOrderProxy.getAuthorizedProxyForeignEntity(),
                serviceOrderProxy.getAuthorizedProxyName(),
                serviceOrderProxy.getAuthorizedProxyIdentifier(),
                serviceOrderProxy.getAuthorizedProxyEmail(),
                serviceOrderProxy.getAuthorizedProxyMobilePhone(),
                serviceOrderProxy.getAuthorizedProxyAttorneyPowerNumber(),
                serviceOrderProxy.getAuthorizedProxyDate(),
                serviceOrderProxy.getAuthorizedProxyValidTill(),
                serviceOrderProxy.getAuthorizedProxyNotaryPublic(),
                serviceOrderProxy.getAuthorizedProxyRegistrationNumber(),
                serviceOrderProxy.getAuthorizedProxyOperationArea()
        );
    }

}
