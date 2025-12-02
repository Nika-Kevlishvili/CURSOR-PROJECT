package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesArea;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>SalesAreaResponse Object</h1>
 * {@link #id}
 * {@link #name}
 * {@link #loginPortalTag}
 * {@link #orderingId}
 * {@link #defaultSelection}
 * {@link #status}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesAreaResponse {
    private Long id;
    private String name;
    private String loginPortalTag;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;

    public SalesAreaResponse(SalesArea salesArea) {
        this.id = salesArea.getId();
        this.name = salesArea.getName();
        this.loginPortalTag = salesArea.getLoginPortalTag();
        this.orderingId = salesArea.getOrderingId();
        this.defaultSelection = salesArea.isDefaultSelection();
        this.status = salesArea.getStatus();
    }
}
