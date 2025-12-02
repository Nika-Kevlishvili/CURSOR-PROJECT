package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedService;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceLinkedServiceShortResponse {
    private Long id;
    private String name;
    private String type;
    private String fullName;
    private ServiceObligationCondition obligatory;
    private ServiceAllowsSalesUnder allowSalesUnder;
    private LocalDateTime createDate;

    public ServiceLinkedServiceShortResponse(ServiceDetails linkedServiceDetails, ServiceLinkedService serviceLinkedService) {
        this.id = linkedServiceDetails.getService().getId();
        this.name = "%s - %s (%s)".formatted("Service", linkedServiceDetails.getName(), serviceLinkedService.getService().getId());
        this.fullName = "%s - %s (%s, %s)".formatted(
                "Service",
                linkedServiceDetails.getName(),
                serviceLinkedService.getServiceObligationCondition().getDescription(),
                serviceLinkedService.getAllowsSalesUnder().getDescription()
        );
        this.type = "SERVICE";
        this.obligatory = serviceLinkedService.getServiceObligationCondition();
        this.allowSalesUnder = serviceLinkedService.getAllowsSalesUnder();
        this.createDate = serviceLinkedService.getCreateDate();
    }
}
