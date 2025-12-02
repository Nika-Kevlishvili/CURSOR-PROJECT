package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import lombok.Data;

@Data
public class ObjectionToChangeOfCbgShortResponse {

    private Long id;

    private String number;

    public ObjectionToChangeOfCbgShortResponse(ObjectionToChangeOfCbg objection) {
        this.id = objection.getId();
        this.number = objection.getChangeOfCbgNumber();
    }
}
