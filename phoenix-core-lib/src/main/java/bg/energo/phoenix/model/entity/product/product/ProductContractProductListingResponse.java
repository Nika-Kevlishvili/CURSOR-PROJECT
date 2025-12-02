package bg.energo.phoenix.model.entity.product.product;

import lombok.Data;

@Data
public class ProductContractProductListingResponse {
    private Long id;
    private Long contractId;
    private String name;
    private Long versionId;

    public ProductContractProductListingResponse(ProductContractProductListingMiddleResponse response) {
        this.contractId = response.getId();
        this.id = response.getDetailId();
        this.name = response.getName() + " (version "+response.getVersionId()+")";
        this.versionId = response.getVersionId();
    }
}
