package bg.energo.phoenix.model.response.contract.action;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ActionListResponse {
    Long getId();
    LocalDateTime getCreateDate();
    EntityStatus getStatus();
    ActionStatus getActionStatus();
    String getActionTypeName();
    LocalDate getNoticeReceivingDate();
    LocalDate getExecutionDate();
    String getPenaltyClaimed();
    String getPenaltyClaimAmount();
    ActionPenaltyPayer getPenaltyPayer();
    String getCustomerName();
    String getContractNumber();
    String getPodIdentifiers();
    String getPenaltyName();
    String getTerminationName();
    Boolean getWithoutPenalty();
    Boolean getWithoutAutoTermination();
}