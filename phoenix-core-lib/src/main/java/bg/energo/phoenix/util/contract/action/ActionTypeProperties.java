package bg.energo.phoenix.util.contract.action;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class ActionTypeProperties {

    @Value("${action.type.contract-termination.with-notice}")
    private Long contractTerminationWithNoticeId;

    @Value("${action.type.contract-termination.without-notice}")
    private Long contractTerminationWithoutNoticeId;

    @Value("${action.type.pod-termination.with-notice}")
    private Long podTerminationWithNoticeId;

    @Value("${action.type.pod-termination.without-notice}")
    private Long podTerminationWithoutNoticeId;


    /**
     * @param actionTypeId ID of the action type
     * @return true if the action type is related to pod termination, false otherwise
     */
    public boolean isActionTypeRelatedToPodTermination(Long actionTypeId) {
        return List.of(getPodTerminationWithNoticeId(), getPodTerminationWithoutNoticeId()).contains(actionTypeId);
    }


    /**
     * @param actionTypeId ID of the action type
     * @return true if the action type is related to contract termination, false otherwise
     */
    public boolean isActionTypeRelatedToContractTermination(Long actionTypeId) {
        return List.of(getContractTerminationWithNoticeId(), getContractTerminationWithoutNoticeId()).contains(actionTypeId);
    }

}
