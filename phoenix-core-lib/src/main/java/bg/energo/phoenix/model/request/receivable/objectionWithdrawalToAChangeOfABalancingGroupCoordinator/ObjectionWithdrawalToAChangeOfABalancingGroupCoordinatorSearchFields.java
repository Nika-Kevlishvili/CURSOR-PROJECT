package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields {

    ALL("ALL"),
    NUMBER("NUMBER"),
    CHANGE_OF_CBG_NUMBER("CHANGE_OF_CBG_NUMBER"),
    POD_IDENTIFIER("POD_IDENTIFIER");

    private final String value;
}
