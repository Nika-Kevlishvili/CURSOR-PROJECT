package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.CreditRating;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
