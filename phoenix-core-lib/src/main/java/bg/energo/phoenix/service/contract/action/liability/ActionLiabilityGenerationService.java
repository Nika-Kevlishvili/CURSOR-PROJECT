package bg.energo.phoenix.service.contract.action.liability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionLiability;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResult;
import bg.energo.phoenix.repository.contract.action.ActionLiabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import static bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer.EPRES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionLiabilityGenerationService {

    private final ActionLiabilityRepository actionLiabilityRepository;


    /**
     * Generates liability if applicable based on the action penalty calculation result and the action penalty payer.
     * @return true if liability is generated, false otherwise
     */
    public boolean generateLiabilityIfApplicable(ActionPenaltyPayer penaltyPayer,
                                                 Boolean dontAllowAutomaticPenaltyClaim,
                                                 ActionPenaltyCalculationResult actionPenaltyCalculationResult,
                                                 Long actionId) {
        if (isLiabilityGenerationApplicable(penaltyPayer, dontAllowAutomaticPenaltyClaim, actionPenaltyCalculationResult)) {
            generateLiability(actionId);
            return true;
        }

        return false;
    }


    /**
     * @return true if penalty payer is not EPRES and automatic penalty claim is allowed, false otherwise
     */
    public boolean isLiabilityGenerationApplicable(ActionPenaltyPayer penaltyPayer,
                                                          Boolean dontAllowAutomaticPenaltyClaim,
                                                          ActionPenaltyCalculationResult actionPenaltyCalculationResult) {
        if (!penaltyPayer.equals(EPRES) && BooleanUtils.isFalse(dontAllowAutomaticPenaltyClaim)) {
            return actionPenaltyCalculationResult.isNotEmpty()
                   && BooleanUtils.isTrue(actionPenaltyCalculationResult.isAutomaticClaimSelectedInPenalty());
        }

        return false;
    }


    // TODO: 11/30/23 after implementing liability generation in the following bundles, replace this code
    public void generateLiability(Long actionId) {
        ActionLiability actionLiability = new ActionLiability();
        actionLiability.setActionId(actionId);
        actionLiability.setStatus(EntityStatus.ACTIVE);
        actionLiabilityRepository.save(actionLiability);
    }


    /**
     * @return true if liability is generated for the given action, false otherwise
     */
    public boolean hasLiabilityGenerated(Long actionId) {
        // TODO: 10/25/23 to be implemented in the following sprints
        // TODO: 11/29/23 run tests before pushing, because some of them depends on this implementation
        return actionLiabilityRepository.existsByActionIdAndStatus(actionId, EntityStatus.ACTIVE);
    }

}
