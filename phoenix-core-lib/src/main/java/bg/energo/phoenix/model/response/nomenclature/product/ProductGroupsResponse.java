package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ProductGroupsResponse {
    private Long id;
    private String name;
    private String nameTransliterated;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ProductGroupsResponse(ProductGroups productGroups) {
        this.id = productGroups.getId();
        this.name = productGroups.getName();
        this.nameTransliterated = productGroups.getNameTransliterated();
        this.orderingId = productGroups.getOrderingId();
        this.defaultSelection = productGroups.isDefaultSelection();
        this.status = productGroups.getStatus();
        this.systemUserId = productGroups.getSystemUserId();
    }
}
