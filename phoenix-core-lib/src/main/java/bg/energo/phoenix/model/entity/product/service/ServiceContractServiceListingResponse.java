package bg.energo.phoenix.model.entity.product.service;


import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import lombok.Data;

@Data
public class ServiceContractServiceListingResponse {
    private Long serviceId;
    private Long id;
    private String name;
    private Long versionId;
    private ServiceExecutionLevel executionLevel;

    public ServiceContractServiceListingResponse(ServiceContractServiceListingMiddleResponse response) {
        this.serviceId = response.getId();
        this.id = response.getDetailId();
        this.name = response.getName() + " (version "+response.getVersionId()+")";
        this.versionId = response.getVersionId();
        this.executionLevel = response.getExecutionLevel();
    }
}
