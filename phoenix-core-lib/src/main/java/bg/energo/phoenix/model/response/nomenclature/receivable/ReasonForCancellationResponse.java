package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForCancellation;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReasonForCancellationResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;

    public ReasonForCancellationResponse(ReasonForCancellation reasonForCancellation) {
        this.id = reasonForCancellation.getId();
        this.name = reasonForCancellation.getName();
        this.orderingId = reasonForCancellation.getOrderingId();
        this.defaultSelection=reasonForCancellation.isDefaultSelection();
        this.status=reasonForCancellation.getStatus();
    }
}
