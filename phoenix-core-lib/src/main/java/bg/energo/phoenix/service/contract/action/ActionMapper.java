package bg.energo.phoenix.service.contract.action;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ActionMapper {

    public static Action fromRequestToEntity(Action action,
                                             ActionRequest request,
                                             ActionPenaltyCalculationResult actionPenaltyCalculationResult) {
        action.setStatus(EntityStatus.ACTIVE);

        action.setActionStatus(
                request.getExecutionDate().isBefore(LocalDate.now())
                        || request.getExecutionDate().equals(LocalDate.now())
                        ? ActionStatus.EXECUTED : ActionStatus.AWAITING
        );

        action.setActionTypeId(request.getActionTypeId());
        action.setNoticeReceivingDate(request.getNoticeReceivingDate());
        action.setExecutionDate(request.getExecutionDate());
        action.setPenaltyPayer(request.getPenaltyPayer());
        BigDecimal amount = actionPenaltyCalculationResult.amount().stripTrailingZeros().scale() <= 0 ?
                actionPenaltyCalculationResult.amount().setScale(0, RoundingMode.HALF_UP)
                : actionPenaltyCalculationResult.amount();
        action.setCalculatedPenaltyAmount(amount);
        action.setCalculatedPenaltyCurrencyId(actionPenaltyCalculationResult.currencyId());

        if (!request.getPenaltyPayer().equals(ActionPenaltyPayer.EPRES)) {
            if (request.getPenaltyClaimAmount() == null) {
                action.setPenaltyClaimAmount(amount);
                action.setPenaltyClaimCurrencyId(actionPenaltyCalculationResult.currencyId());
                action.setClaimAmountManuallyEntered(false);
            } else {
                action.setPenaltyClaimAmount(request.getPenaltyClaimAmount());
                action.setPenaltyClaimCurrencyId(request.getPenaltyClaimAmountCurrencyId());
                action.setClaimAmountManuallyEntered(true);
            }
        }

        action.setDontAllowAutomaticPenaltyClaim(request.getDontAllowAutomaticPenaltyClaim());
        action.setAdditionalInfo(request.getAdditionalInformation());
        action.setPenaltyId(request.getPenaltyId());
        action.setWithoutPenalty(request.getWithoutPenalty());
        action.setTerminationId(request.getTerminationId());
        action.setWithoutAutomaticTermination(request.getWithoutAutomaticTermination());
        action.setCustomerId(request.getCustomerId());
        action.setTemplateId(request.getTemplateId());
        action.setEmailTemplateId(request.getEmailTemplateId());

        if (request.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
            action.setProductContractId(request.getContractId());
            action.setServiceContractId(null);
        } else {
            action.setServiceContractId(request.getContractId());
            action.setProductContractId(null);
        }

        return action;
    }

}
