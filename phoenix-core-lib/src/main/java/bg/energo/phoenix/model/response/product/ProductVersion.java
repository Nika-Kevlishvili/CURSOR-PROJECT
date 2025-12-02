package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductVersion {
    private Long id;

    private Long detailId;

    private LocalDateTime createDate;

    private ProductDetailStatus status;

    public ProductVersion(Long id,
                          Long detailId,
                          LocalDateTime createDate,
                          ProductDetailStatus status) {
        this.id = id;
        this.detailId = detailId;
        this.createDate = createDate;
        this.status = status;
    }
}
