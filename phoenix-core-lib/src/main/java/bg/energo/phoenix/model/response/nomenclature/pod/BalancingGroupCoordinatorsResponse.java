package bg.energo.phoenix.model.response.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.BalancingGroupCoordinators;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalancingGroupCoordinatorsResponse {
    private Long id;
    private String name;
    private String fullName;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;

    public BalancingGroupCoordinatorsResponse(BalancingGroupCoordinators balancingGroupCoordinators) {
        this.id = balancingGroupCoordinators.getId();
        this.name = balancingGroupCoordinators.getName();
        this.fullName = balancingGroupCoordinators.getFullName();
        this.orderingId = balancingGroupCoordinators.getOrderingId();
        this.defaultSelection = balancingGroupCoordinators.isDefaultSelection();
        this.status = balancingGroupCoordinators.getStatus();
        this.systemUserId = balancingGroupCoordinators.getSystemUserId();
    }
}
