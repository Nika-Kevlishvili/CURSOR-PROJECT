package bg.energo.phoenix.model.response.contract.action.calculation.formula;

import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyTransformationByApplicabilityArguments {
    private ContractType contractType;
    private Long contractId;
    private Long actionTypeId;
    private LocalDate executionDate;
    private List<Long> actionPods;
    private ActionPenaltyCalculationResult formulaEvaluationResult;
    private PenaltyApplicability penaltyApplicability;
    private String penaltyFormula;
    private Boolean penaltyAutomaticSubmission;
    private Long actionIdToExclude;

    public static PenaltyTransformationByApplicabilityArguments fromEvaluationArguments(PenaltyFormulaEvaluationArguments formulaEvaluationArguments,
                                                                                        ActionPenaltyCalculationResult formulaEvaluationResult,
                                                                                        Long actionIdToExclude) {
        PenaltyTransformationByApplicabilityArguments arguments = new PenaltyTransformationByApplicabilityArguments();
        arguments.setContractType(Objects.isNull(formulaEvaluationArguments.getProductContractId()) ? ContractType.SERVICE_CONTRACT : ContractType.PRODUCT_CONTRACT);
        arguments.setContractId(Objects.isNull(formulaEvaluationArguments.getProductContractId()) ? formulaEvaluationArguments.getServiceContractId() : formulaEvaluationArguments.getProductContractId());
        arguments.setActionTypeId(formulaEvaluationArguments.getActionTypeId());
        arguments.setExecutionDate(formulaEvaluationArguments.getActionExecutionDate());
        arguments.setActionPods(formulaEvaluationArguments.getActionPods());
        arguments.setFormulaEvaluationResult(formulaEvaluationResult);
        arguments.setPenaltyApplicability(formulaEvaluationArguments.getPenaltyApplicability());
        arguments.setPenaltyFormula(formulaEvaluationArguments.getPenaltyFormula());
        arguments.setPenaltyAutomaticSubmission(formulaEvaluationArguments.getPenaltyAutomaticSubmission());
        arguments.setActionIdToExclude(actionIdToExclude);
        return arguments;
    }
}
