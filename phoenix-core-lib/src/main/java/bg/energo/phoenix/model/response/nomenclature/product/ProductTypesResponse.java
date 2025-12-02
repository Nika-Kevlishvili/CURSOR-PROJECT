package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductTypesResponse {

    private Long id;
    private String name;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;

    public ProductTypesResponse(ProductTypes productTypes) {
        this.id = productTypes.getId();
        this.name = productTypes.getName();
        this.orderingId = productTypes.getOrderingId();
        this.defaultSelection = productTypes.getIsDefault();
        this.status = productTypes.getStatus();
    }

}
