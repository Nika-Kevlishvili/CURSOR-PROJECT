package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns {

    WITHDRAWAL_CHANGE_OF_CBG_NUMBER("withdrawalChangeOfCbgNumber"),
    CHANGE_OF_CBG_NUMBER("changeOfCbgNumber"),
    WITHDRAWAL_CHANGE_OF_CBG_STATUS("withdrawalChangeOfCbgStatus"),
    POD_ID("numberOfPods"),
    CREATE_DATE("createDate"),
    ID("id");

    private final String value;
}
