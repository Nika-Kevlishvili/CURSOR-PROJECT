package bg.energo.phoenix.model.request.contract.action;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.contract.action.ValidActionRequest;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@ValidActionRequest
public class ActionRequest {

    @NotNull(message = "actionTypeId-Action type is required;")
    private Long actionTypeId;

    @NotNull(message = "noticeReceivingDate-Notice receiving date is required;")
    private LocalDate noticeReceivingDate;

    @NotNull(message = "executionDate-Execution date is required;")
    private LocalDate executionDate;

    @DecimalMin(value = "0",message = "penaltyClaimAmount-Allowed range is between 0 and 99999999999;")
    @DecimalMax(value = "99999999999",message = "penaltyClaimAmount-Allowed range is between 0 and 99999999999;")
    @Digits(integer = 11, fraction = 2, message = "penaltyClaimAmount-Penalty claim amount should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal penaltyClaimAmount;

    private Long penaltyClaimAmountCurrencyId;

    @NotNull(message = "penaltyPayer-Penalty payer is required;")
    private ActionPenaltyPayer penaltyPayer;

    @NotNull(message = "dontAllowAutomaticPenaltyClaim-[Do not allow automatic penalty claim] field is required;")
    private Boolean dontAllowAutomaticPenaltyClaim;

    @Size(max = 1024, message = "additionalInformation-additionalInformation max allowed size is {max};")
    private String additionalInformation;

    private Long penaltyId;

    @NotNull(message = "withoutPenalty-[Without penalty] field is required;")
    private Boolean withoutPenalty;

    private Long terminationId;

    private Boolean withoutAutomaticTermination;

    @NotNull(message = "customerId-Customer is required;")
    private Long customerId;

    @NotNull(message = "contractId-Contract is required;")
    private Long contractId;

    @NotNull(message = "contractType-Contract type is required;")
    private ContractType contractType;

    @DuplicatedValuesValidator(fieldPath = "pods")
    private List<Long> pods;

    @DuplicatedValuesValidator(fieldPath = "files")
    private List<Long> files;

    private Long templateId;
    private Long emailTemplateId;

}
