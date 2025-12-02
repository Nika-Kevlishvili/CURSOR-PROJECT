package bg.energo.phoenix.model.response.contract.productContract.terminations;

import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;

import java.time.LocalDate;

public interface ProductContractTerminationWithActionsResponse {

    Long getContractId();

    Long getActionId();

    Long getTerminationId();

    Boolean getNoticeDue();

    LocalDate getActionExecutionDate();

    ActionPenaltyPayer getActionPenaltyPayer();

    Long getActionTypeId();

    AutoTerminationFrom getAutoTerminationFrom();

    LocalDate getContractTerminationDate();

}
