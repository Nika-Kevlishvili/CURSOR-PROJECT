package bg.energo.phoenix.model.request.product.penalty.penalty;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.product.penalty.ValidPenaltyFormula;
import bg.energo.phoenix.model.customAnotations.product.penalty.ValidPenaltyRequest;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PartyReceivingPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.PenaltyPaymentTermRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@Builder
@ValidPenaltyRequest
@ToString
public class PenaltyRequest {

    @NotBlank(message = "name-Name should not be blank;")
    @Size(max = 1024, message = "name-Name max allowed size is {max};")
    private String name;

    @Size(max = 2048, message = "contractClauseNumber-contractClauseNumber max allowed size is {max};")
    private String contractClauseNumber;

    @NotEmpty(message = "penaltyReceivingParties-At least one party should be present;")
    private Set<PartyReceivingPenalty> penaltyReceivingParties;

    @NotNull(message = "penaltyApplicability-penaltyApplicability should not be null;")
    private PenaltyApplicability penaltyApplicability;

    @ValidPenaltyFormula(message = "amountFormula-Formula is not valid;")
    private String amountFormula;

    @DecimalMin(value = "0.01", message = "minAmount-minAmount should be more than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "minAmount-minAmount should be less than 999999999999.99;")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.01", message = "maxAmount-maxAmount should be more than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "maxAmount-maxAmount should be less than 999999999999.99;")
    private BigDecimal maxAmount;

    @NotNull(message = "currencyId-currencyId should not be null;")
    private Long currencyId;

    private Long processId;

    private String processStartCode;

    private Boolean automaticSubmission;

    @Size(max = 4096, message = "additionalInformation-additionalInformation max allowed size is {max};")
    private String additionalInformation;

    @NotNull(message = "penaltyPaymentTermRequest-Penalty payment term request must not be null;")
    @Valid
    private PenaltyPaymentTermRequest penaltyPaymentTermRequest;

    private Boolean noInterestOnOverdueDebts;

    @DuplicatedValuesValidator(fieldPath = "actionTypeList")
    @NotEmpty(message = "actionTypeList-actionTypeList should not be empty;")
    private List<Long> actionTypeList;

    private Long templateId;
    //TODO TEMPLATE for delivery purpose - should be removed
    //@NotNull(message = "emailTemplateId-Email template can not be null!;")
    private Long emailTemplateId;

}
