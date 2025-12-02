package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.enums.product.product.EntityType;
import bg.energo.phoenix.model.enums.product.product.ProductAllowSalesUnder;
import bg.energo.phoenix.model.enums.product.product.ProductObligatory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductRelatedEntityRequest {
    private Long id;
    private EntityType type;
    private ProductObligatory obligatory;
    private ProductAllowSalesUnder allowSalesUnder;
}
