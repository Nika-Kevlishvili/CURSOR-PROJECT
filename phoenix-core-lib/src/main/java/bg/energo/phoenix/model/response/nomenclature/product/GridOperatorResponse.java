package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GridOperatorResponse {

    private Long id;
    private String name;
    private String fullName;
    private String powerSupplyTerminationRequestEmail;
    private String powerSupplyReconnectionRequestEmail;
    private String objectionToChangeCBGEmail;
    private String codeForXEnergy;
    private String gridOperatorCode;
    private NomenclatureItemStatus status;
    private Boolean defaultSelection;
    private Boolean ownedByEnergoPro;
    private Long orderingId;
    private Boolean isHardCoded;

    public GridOperatorResponse(GridOperator gridOperator) {
        this.id = gridOperator.getId();
        this.name = gridOperator.getName();
        this.fullName = gridOperator.getFullName();
        this.powerSupplyTerminationRequestEmail = gridOperator.getPowerSupplyTerminationRequestEmail();
        this.powerSupplyReconnectionRequestEmail = gridOperator.getPowerSupplyReconnectionRequestEmail();
        this.objectionToChangeCBGEmail = gridOperator.getObjectionToChangeCBGEmail();
        this.codeForXEnergy = gridOperator.getCodeForXEnergy();
        this.gridOperatorCode = gridOperator.getGridOperatorCode();
        this.status = gridOperator.getStatus();
        this.defaultSelection = gridOperator.isDefaultSelection();
        this.ownedByEnergoPro = gridOperator.getOwnedByEnergoPro();
        this.orderingId = gridOperator.getOrderingId();
        this.isHardCoded = gridOperator.getIsHardCoded();
    }
}
