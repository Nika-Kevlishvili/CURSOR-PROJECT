package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@ObjectionWithdrawalToAChangeOfCBGEditRequestValidator
public class ObjectionWithdrawalToAChangeOfCBGEditRequest extends ObjectionWithdrawalToAChangeOfCBGBaseRequest {

    private List<@Valid ProcessResultChangeRequest> processResultChangeRequests;
}
