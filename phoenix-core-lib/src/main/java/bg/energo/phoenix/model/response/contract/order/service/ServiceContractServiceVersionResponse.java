package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ServiceContractServiceVersionResponse {
    private Long id;
    private Long serviceId;
    private String name;
    private Long versionId;

    public ServiceContractServiceVersionResponse(ServiceDetails serviceDetails) {
        this.id = serviceDetails.getId();
        this.serviceId = serviceDetails.getService().getId();
        if (StringUtils.isNotEmpty(serviceDetails.getService().getCustomerIdentifier())) {
            this.name = "%s".formatted(serviceDetails.getName());
        } else {
            this.name = "%s (Version %s)".formatted(serviceDetails.getName(), serviceDetails.getVersion());
        }
        this.versionId = serviceDetails.getVersion();
    }
}
