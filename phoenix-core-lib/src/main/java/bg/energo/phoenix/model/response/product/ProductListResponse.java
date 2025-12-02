package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ProductListResponse {


    private Long id;
    private String name;
    private String groupName;
    private ProductStatus status;
    private ProductDetailStatus detailStatus;
    private Long productTypeId;
    private String productTypeName;
    private String contractTerms;
    private String salesChannels;
    private Boolean globalSalesChannel;
    private String productContractTemplate;
    private LocalDateTime dateOfCreation;
    private String individualProduct;

    public ProductListResponse(Long id,
                               String name,
                               String groupName,
                               ProductStatus status,
                               ProductDetailStatus detailStatus,
                               Long productTypeId,
                               String productTypeName,
                               String contractTerms,
                               String salesChannels,
                               LocalDateTime dateOfCreation,
                               Boolean globalSalesChannel,
                               String individualProduct) {
        this.id = id;
        this.name = name;
        this.groupName = groupName;
        this.status = status;
        this.detailStatus = detailStatus;
        this.productTypeId = productTypeId;
        this.productTypeName = productTypeName;
        this.contractTerms = contractTerms;
        this.salesChannels = salesChannels;
        this.dateOfCreation = dateOfCreation;
        this.globalSalesChannel = globalSalesChannel;
        this.individualProduct = individualProduct;
    }
}
