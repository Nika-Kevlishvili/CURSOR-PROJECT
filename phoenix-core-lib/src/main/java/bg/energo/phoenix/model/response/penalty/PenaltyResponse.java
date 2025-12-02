package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PartyReceivingPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.response.penalty.copy.PenaltyPaymentTermsCopyResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class PenaltyResponse {
    private Long id;
    private String name;
    private String contractClauseNumber;
    private Long processId;
    private String processStartCode;
    private Set<PartyReceivingPenalty> penaltyReceivingParties;
    private PenaltyApplicability applicability;
    private String amountCalculationFormula;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Long currencyId;
    private String currencyName;
    private boolean automaticSubmission;
    private String additionalInfo;
    private PenaltyPaymentTermsCopyResponse paymentTerm;
    private EntityStatus status;
    private boolean noInterestOnOverdueDebts;

    private List<PenaltyVariableResponse> priceParameterVariablesInfo;
    private List<PenaltyVariableResponse> priceComponentTagVariablesInfo;

    private Boolean isLocked;
    private List<PenaltyActionTypeResponse> actionTypeResponses;
    private ContractTemplateShortResponse templateResponse;
    private ContractTemplateShortResponse emailTemplateResponse;


    @Builder
    public PenaltyResponse(Long id,
                           String name,
                           String contractClauseNumber,
                           Long processId,
                           String processStartCode,
                           Set<PartyReceivingPenalty> penaltyReceivingParties,
                           PenaltyApplicability applicability,
                           String amountCalculationFormula,
                           BigDecimal minAmount,
                           BigDecimal maxAmount,
                           Long currencyId,
                           String currencyName,
                           boolean automaticSubmission,
                           String additionalInfo,
                           PenaltyPaymentTermsCopyResponse paymentTerm,
                           EntityStatus status,
                           boolean noInterestOnOverdueDebts,
                           List<PenaltyVariableResponse> priceParameterVariablesInfo,
                           List<PenaltyVariableResponse> priceComponentTagVariablesInfo,
                           Boolean isLocked,
                           List<PenaltyActionTypeResponse> actionTypeResponses,
                           ContractTemplateShortResponse templateResponse,
                           ContractTemplateShortResponse emailTemplateResponse) {

        this.id = id;
        this.name = name;
        this.contractClauseNumber = contractClauseNumber;
        this.processId = processId;
        this.processStartCode = processStartCode;
        this.penaltyReceivingParties = penaltyReceivingParties;
        this.applicability = applicability;
        this.amountCalculationFormula = amountCalculationFormula;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.currencyId = currencyId;
        this.currencyName = currencyName;
        this.automaticSubmission = automaticSubmission;
        this.additionalInfo = additionalInfo;
        this.paymentTerm = paymentTerm;
        this.status = status;
        this.noInterestOnOverdueDebts = noInterestOnOverdueDebts;
        this.priceParameterVariablesInfo = priceParameterVariablesInfo;
        this.priceComponentTagVariablesInfo = priceComponentTagVariablesInfo;
        this.isLocked = isLocked;
        this.actionTypeResponses = actionTypeResponses;
        this.templateResponse=templateResponse;
        this.emailTemplateResponse=emailTemplateResponse;
    }
}
