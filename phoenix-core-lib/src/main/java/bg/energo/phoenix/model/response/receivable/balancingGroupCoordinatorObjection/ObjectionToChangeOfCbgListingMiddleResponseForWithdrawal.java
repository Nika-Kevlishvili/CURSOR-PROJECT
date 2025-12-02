package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;

public interface ObjectionToChangeOfCbgListingMiddleResponseForWithdrawal {
    Long getId();

    String getNumber();

    ChangeOfCbgStatus getChangeStatus();
}
