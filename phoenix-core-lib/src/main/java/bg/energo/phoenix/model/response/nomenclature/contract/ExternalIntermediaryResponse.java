package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.ExternalIntermediary;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ExternalIntermediaryResponse {
    private Long id;
    private String name;
    private String identifier;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public ExternalIntermediaryResponse(ExternalIntermediary externalIntermediary) {
        this.id = externalIntermediary.getId();
        this.name = externalIntermediary.getName();
        this.identifier = externalIntermediary.getIdentifier();
        this.orderingId = externalIntermediary.getOrderingId();
        this.defaultSelection = externalIntermediary.isDefaultSelection();
        this.status = externalIntermediary.getStatus();
        this.systemUserId = externalIntermediary.getSystemUserId();
    }
}
