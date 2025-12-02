package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductLinkToProduct;
import bg.energo.phoenix.model.entity.product.product.ProductLinkToService;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.product.product.ProductAllowSalesUnder;
import bg.energo.phoenix.model.enums.product.product.ProductObligatory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductRelatedEntityShortResponse {
    private Long id;
    private String name;
    private String type;
    private String fullName;
    private ProductObligatory obligatory;
    private ProductAllowSalesUnder allowSalesUnder;
    private LocalDateTime createDate;

    public ProductRelatedEntityShortResponse(ProductLinkToProduct linkedProduct, ProductDetails linkedProductDetails) {
        this.id = linkedProduct.getLinkedProduct().getId();
        if (linkedProductDetails != null) {
            this.name = "%s - %s (%s)".formatted("Product", linkedProductDetails.getName(), id);
            this.fullName = "%s - %s (%s, %s) (%s)".formatted(
                    "Product",
                    linkedProductDetails.getName(),
                    linkedProduct.getObligatory().getDescription(),
                    linkedProduct.getAllowSalesUnder().getDescription(),
                    id
            );
        }
        this.type = "PRODUCT";
        this.obligatory = linkedProduct.getObligatory();
        this.allowSalesUnder = linkedProduct.getAllowSalesUnder();
        this.createDate = linkedProduct.getCreateDate();
    }

    public ProductRelatedEntityShortResponse(ProductLinkToService linkedService, ServiceDetails linkedServiceDetails) {
        this.id = linkedService.getLinkedService().getId();
        if (linkedServiceDetails != null) {
            this.name = "%s - %s (%s)".formatted("Service", linkedServiceDetails.getName(), id);
            this.fullName = "%s - %s (%s, %s) (%s)".formatted(
                    "Service",
                    linkedServiceDetails.getName(),
                    linkedService.getObligatory().getDescription(),
                    linkedService.getAllowSalesUnder().getDescription(),
                    id
            );
        }
        this.type = "SERVICE";
        this.obligatory = linkedService.getObligatory();
        this.allowSalesUnder = linkedService.getAllowSalesUnder();
        this.createDate = linkedService.getCreateDate();
    }
}
