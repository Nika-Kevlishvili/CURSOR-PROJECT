package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedProduct;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceLinkedProductShortResponse {
    private Long id;
    private String name;
    private String type;
    private String fullName;
    private ServiceObligationCondition obligatory;
    private ServiceAllowsSalesUnder allowSalesUnder;
    private LocalDateTime createDate;

    public ServiceLinkedProductShortResponse(ProductDetails linkedProductDetails, ServiceLinkedProduct serviceLinkedProduct) {
        this.id = linkedProductDetails.getProduct().getId();
        this.name = "%s - %s (%s)".formatted("Product", linkedProductDetails.getName(), serviceLinkedProduct.getProduct().getId());
        this.fullName = "%s - %s (%s, %s)".formatted(
                "Product",
                linkedProductDetails.getName(),
                serviceLinkedProduct.getServiceObligationCondition().getDescription(),
                serviceLinkedProduct.getAllowsSalesUnder().getDescription()
        );
        this.type = "PRODUCT";
        this.obligatory = serviceLinkedProduct.getServiceObligationCondition();
        this.allowSalesUnder = serviceLinkedProduct.getAllowsSalesUnder();
        this.createDate = serviceLinkedProduct.getCreateDate();
    }
}
