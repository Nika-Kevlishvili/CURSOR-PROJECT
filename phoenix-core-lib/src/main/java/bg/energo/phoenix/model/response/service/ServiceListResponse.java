package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ServiceListResponse {
    private Long serviceId;
    private String name;
    private String serviceGroupName;
    private ServiceStatus status;
    private ServiceDetailStatus serviceDetailStatus;
    private String serviceTypeName;
    private String contractTermsName;
    private String salesChannelsName;
    private Boolean globalSalesChannel;
    private String contractTemplateName;
    private LocalDateTime dateOfCreation;
    private boolean isIndividualService;

    public ServiceListResponse(ServiceListMiddleResponse middleResponse) {
        this.serviceId = middleResponse.getServiceId();
        this.name = middleResponse.getName();
        this.serviceGroupName = middleResponse.getServiceGroupName();
        this.status = middleResponse.getStatus();
        this.serviceDetailStatus = middleResponse.getServiceDetailStatus();
        this.serviceTypeName = middleResponse.getServiceTypeName();
        this.contractTermsName = middleResponse.getContractTermsName();
        this.salesChannelsName = middleResponse.getSalesChannelsName();
        this.globalSalesChannel = middleResponse.getGlobalSalesChannel();
        this.contractTemplateName = middleResponse.getContractTemplateName();
        this.dateOfCreation = middleResponse.getDateOfCreation();
        this.isIndividualService = middleResponse.getIndividualService();
    }

}
