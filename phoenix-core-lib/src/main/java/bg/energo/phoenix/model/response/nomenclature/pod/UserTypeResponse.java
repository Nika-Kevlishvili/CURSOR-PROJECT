package bg.energo.phoenix.model.response.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.UserType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserTypeResponse {

    private Long id;
    private String name;
    private NomenclatureItemStatus status;
    private Boolean defaultSelection;
    private Long orderingId;

    public UserTypeResponse(UserType userType) {
        this.id = userType.getId();
        this.name = userType.getName();
        this.status = userType.getStatus();
        this.defaultSelection = userType.isDefaultSelection();
        this.orderingId = userType.getOrderingId();
    }

}
