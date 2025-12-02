package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListResponse {

    private Long id;
    private String withdrawalChangeOfCbgNumber;
    private String changeOfCbgNumber;
    private ObjectionWithdrawalToChangeOfCbgStatus withdrawalChangeOfCbgStatus;
    private EntityStatus status;
    private Long numberOfPods;
    private LocalDate createDate;

    public ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListResponse(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.withdrawalChangeOfCbgNumber = middleResponse.getWithdrawalChangeOfCbgNumber();
        this.changeOfCbgNumber = middleResponse.getChangeOfCbgNumber();
        this.withdrawalChangeOfCbgStatus = middleResponse.getWithdrawalChangeOfCbgStatus();
        this.status = middleResponse.getStatus();
        this.numberOfPods = middleResponse.getNumberOfPods();
        this.createDate = middleResponse.getCreateDate();
    }
}
