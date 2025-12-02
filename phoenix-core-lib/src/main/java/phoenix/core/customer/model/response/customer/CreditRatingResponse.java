package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.nomenclature.customer.CreditRating;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditRatingResponse {
    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Boolean defaultSelection;
    private Long orderingId;

    public CreditRatingResponse(CreditRating entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.status = entity.getStatus();
        this.defaultSelection = entity.getIsDefault();
        this.orderingId = entity.getOrderingId();

    }
}
