package bg.energo.phoenix.model.request.contract.action;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class CalculatePenaltyAmountRequest {

    @NotNull(message = "actionTypeId-Action type is required;")
    private Long actionTypeId;

    @NotNull(message = "executionDate-Execution date is required;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate executionDate;

    @NotNull(message = "penaltyId-Penalty is required;")
    private Long penaltyId;

    @NotNull(message = "penaltyPayer-Penalty payer is required;")
    private ActionPenaltyPayer penaltyPayer;

    @NotNull(message = "customerId-Customer is required;")
    private Long customerId;

    private Long terminationId;

    @NotNull(message = "contractId-Contract is required;")
    private Long contractId;

    @NotNull(message = "contractType-Contract type is required;")
    private ContractType contractType;

    @DuplicatedValuesValidator(fieldPath = "pods")
    private List<Long> pods;
}
