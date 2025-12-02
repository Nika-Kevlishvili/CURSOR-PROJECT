package bg.energo.phoenix.model.response.contract.action.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ActionPenaltyCalculationResponse {

    // fields related to action
    Long getActionId();
    Long getActionTypeId();
    String getActionPods();
    Long getProductContractId();
    Long getServiceContractId();
    LocalDate getExecutionDate();
    Long getActionTerminationId();
    String getActionPenaltyPayer();
    Boolean getActionDontAllowAutoPenaltyClaim();
    BigDecimal getActionPenaltyClaimedAmount();
    Long getActionPenaltyClaimCurrency();
    Boolean getActionClaimAmountManuallyEntered();

    // fields related to penalty
    Long getPenaltyId();
    String getPenaltyApplicability();
    String getPenaltyActionTypeId();
    String getPenaltyFormula();
    Long getCurrencyId();
    BigDecimal getPenaltyLowerLimit();
    BigDecimal getPenaltyUpperLimit();
    boolean getPenaltyAutomaticSubmission();

}
