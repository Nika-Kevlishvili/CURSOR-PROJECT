package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;

import java.time.LocalDate;

public interface ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListMiddleResponse {
    String getWithdrawalChangeOfCbgNumber();
    String getChangeOfCbgNumber();
    ObjectionWithdrawalToChangeOfCbgStatus getWithdrawalChangeOfCbgStatus();
    EntityStatus getStatus();
    Long getNumberOfPods();
    LocalDate getCreateDate();
    Long getId();
}
