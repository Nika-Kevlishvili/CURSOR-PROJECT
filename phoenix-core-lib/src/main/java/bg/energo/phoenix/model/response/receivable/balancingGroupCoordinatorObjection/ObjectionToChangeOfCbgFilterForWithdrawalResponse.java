package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import lombok.Data;

@Data
public class ObjectionToChangeOfCbgFilterForWithdrawalResponse {
    private Long id;

    private String number;

    private ChangeOfCbgStatus changeStatus;

    public ObjectionToChangeOfCbgFilterForWithdrawalResponse(ObjectionToChangeOfCbgListingMiddleResponseForWithdrawal middleResponse) {
        this.id = middleResponse.getId();
        this.number = middleResponse.getNumber();
        this.changeStatus = middleResponse.getChangeStatus();
    }
}
