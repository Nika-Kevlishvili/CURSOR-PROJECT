package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForDisconnection;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReasonForDisconnectionResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;


    public ReasonForDisconnectionResponse(ReasonForDisconnection reasonForDisconnection) {
        this.id = reasonForDisconnection.getId();
        this.name = reasonForDisconnection.getName();
        this.orderingId = reasonForDisconnection.getOrderingId();
        this.defaultSelection = reasonForDisconnection.isDefaultSelection();
        this.status = reasonForDisconnection.getStatus();
    }
}
