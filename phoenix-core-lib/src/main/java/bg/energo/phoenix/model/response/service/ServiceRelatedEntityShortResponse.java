package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedProduct;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedService;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceRelatedEntityShortResponse {
    private Long id;
    private String name;
    private String type;
    private String fullName;
    private ServiceObligationCondition obligatory;
    private ServiceAllowsSalesUnder allowSalesUnder;
    private LocalDateTime createDate;

    public ServiceRelatedEntityShortResponse(ServiceLinkedProduct linkedProduct, ProductDetails linkedProductDetails) {
        this.id = linkedProduct.getProduct().getId();
        if (linkedProductDetails != null) {
            this.name = "%s - %s (%s)".formatted("Product", linkedProductDetails.getName(), id);
            this.fullName = "%s - %s (%s, %s) (%s)".formatted(
                    "Product",
                    linkedProductDetails.getName(),
                    linkedProduct.getServiceObligationCondition().getDescription(),
                    linkedProduct.getAllowsSalesUnder().getDescription(),
                    id
            );
        }
        this.type = "PRODUCT";
        this.obligatory = linkedProduct.getServiceObligationCondition();
        this.allowSalesUnder = linkedProduct.getAllowsSalesUnder();
        this.createDate = linkedProduct.getCreateDate();
    }

    public ServiceRelatedEntityShortResponse(ServiceLinkedService linkedService, ServiceDetails linkedServiceDetails) {
        this.id = linkedService.getService().getId();
        if (linkedServiceDetails != null) {
            this.name = "%s - %s (%s)".formatted("Service", linkedServiceDetails.getName(), id);
            this.fullName = "%s - %s (%s, %s) (%s)".formatted(
                    "Service",
                    linkedServiceDetails.getName(),
                    linkedService.getServiceObligationCondition().getDescription(),
                    linkedService.getAllowsSalesUnder().getDescription(),
                    id
            );
        }
        this.type = "SERVICE";
        this.obligatory = linkedService.getServiceObligationCondition();
        this.allowSalesUnder = linkedService.getAllowsSalesUnder();
        this.createDate = linkedService.getCreateDate();
    }
}
