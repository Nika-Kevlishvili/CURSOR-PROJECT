package bg.energo.phoenix.model.enums.contract.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionTableColumn {
    ID("(id)"),
    DATE_OF_CREATION("(createDate)"),
    ACTION_STATUS("(actionStatus)"),
    ACTION_TYPE_NAME("(actionTypeName)"),
    NOTICE_RECEIVING_DATE("(noticeReceivingDate)"),
    EXECUTION_DATE("(executionDate)"),
    PENALTY_CLAIMED("(penaltyClaimed)"),
    PENALTY_CLAIM_AMOUNT("(penaltyClaimAmountValue)"),
    PENALTY_PAYER("(penaltyPayer)"),
    CUSTOMER_NAME("(customerName)"),
    CONTRACT_NUMBER("(contractNumber)"),
    POD_IDENTIFIERS("(podIdentifiers)"),
    PENALTY_NAME("(penaltyName)"),
    TERMINATION_NAME("(terminationName)");

    private final String value;
}
