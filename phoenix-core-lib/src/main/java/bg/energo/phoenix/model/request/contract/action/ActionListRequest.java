package bg.energo.phoenix.model.request.contract.action;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionSearchField;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;
import bg.energo.phoenix.model.enums.contract.action.ActionTableColumn;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@PromptSymbolReplacer
public class ActionListRequest {
    private int page;
    private int size;
    private String prompt;
    private ActionTableColumn sortBy;
    private ActionSearchField searchBy;
    private Sort.Direction sortDirection;

    private List<ActionStatus> actionStatuses;

    @DuplicatedValuesValidator(fieldPath = "actionTypeIds")
    private List<Long> actionTypeIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate creationDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate noticeReceivingDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate noticeReceivingDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate executionDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate executionDateTo;

    private List<Boolean> penaltyClaimed;

    private BigDecimal calculatedPenaltyFrom;
    private BigDecimal calculatedPenaltyTo;
    private List<Long> currencyIds;

    private BigDecimal claimedAmountFrom;
    private BigDecimal claimedAmountTo;

    private List<ActionPenaltyPayer> penaltyPayers;

}
