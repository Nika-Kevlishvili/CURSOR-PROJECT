package bg.energo.phoenix.model.response.contract.action.calculation.formula;

import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyFormulaEvaluationArguments {

    // fields related to penalty
    private Long penaltyId;
    private String penaltyFormula;
    private BigDecimal penaltyLowerLimit;
    private BigDecimal penaltyUpperLimit;
    private Long penaltyCurrencyId;
    private Boolean penaltyAutomaticSubmission;
    private PenaltyApplicability penaltyApplicability;

    // fields related to action
    private Long productContractId;
    private Long serviceContractId;
    private Long actionTypeId;
    private LocalDate actionExecutionDate;
    private Long actionTerminationId;
    private List<Long> actionPods;


    public static PenaltyFormulaEvaluationArguments fromEntitiesToArguments(Action action,
                                                                            Penalty penalty,
                                                                            List<Long> actionPods) {
        PenaltyFormulaEvaluationArguments arguments = new PenaltyFormulaEvaluationArguments();
        arguments.setPenaltyId(penalty.getId());
        arguments.setPenaltyFormula(penalty.getAmountCalculationFormula());
        arguments.setPenaltyLowerLimit(penalty.getMinAmount());
        arguments.setPenaltyUpperLimit(penalty.getMaxAmount());
        arguments.setPenaltyCurrencyId(penalty.getCurrencyId());
        arguments.setPenaltyAutomaticSubmission(penalty.isAutomaticSubmission());
        arguments.setPenaltyApplicability(penalty.getApplicability());
        arguments.setProductContractId(action.getProductContractId());
        arguments.setServiceContractId(action.getServiceContractId());
        arguments.setActionTypeId(action.getActionTypeId());
        arguments.setActionExecutionDate(action.getExecutionDate());
        arguments.setActionTerminationId(action.getTerminationId());
        arguments.setActionPods(ListUtils.emptyIfNull(actionPods));
        return arguments;
    }

    public static PenaltyFormulaEvaluationArguments fromRequestToArguments(ActionRequest request,
                                                                           Penalty penalty) {
        PenaltyFormulaEvaluationArguments arguments = new PenaltyFormulaEvaluationArguments();
        arguments.setPenaltyId(penalty.getId());
        arguments.setPenaltyFormula(penalty.getAmountCalculationFormula());
        arguments.setPenaltyLowerLimit(penalty.getMinAmount());
        arguments.setPenaltyUpperLimit(penalty.getMaxAmount());
        arguments.setPenaltyCurrencyId(penalty.getCurrencyId());
        arguments.setPenaltyAutomaticSubmission(penalty.isAutomaticSubmission());
        arguments.setPenaltyApplicability(penalty.getApplicability());
        arguments.setProductContractId(request.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? request.getContractId() : null);
        arguments.setServiceContractId(request.getContractType().equals(ContractType.SERVICE_CONTRACT) ? request.getContractId() : null);
        arguments.setActionTypeId(request.getActionTypeId());
        arguments.setActionExecutionDate(request.getExecutionDate());
        arguments.setActionTerminationId(request.getTerminationId());
        arguments.setActionPods(ListUtils.emptyIfNull(request.getPods())); // pods are not mandatory if action type not related to POD
        return arguments;
    }

    public static PenaltyFormulaEvaluationArguments fromCalculationResponseToArguments(ActionPenaltyCalculationResponse calculationResponse) {
        PenaltyFormulaEvaluationArguments arguments = new PenaltyFormulaEvaluationArguments();
        arguments.setPenaltyId(calculationResponse.getPenaltyId());
        arguments.setPenaltyFormula(calculationResponse.getPenaltyFormula());
        arguments.setPenaltyLowerLimit(calculationResponse.getPenaltyLowerLimit());
        arguments.setPenaltyUpperLimit(calculationResponse.getPenaltyUpperLimit());
        arguments.setPenaltyCurrencyId(calculationResponse.getCurrencyId());
        arguments.setPenaltyAutomaticSubmission(calculationResponse.getPenaltyAutomaticSubmission());
        arguments.setPenaltyApplicability(PenaltyApplicability.valueOf(calculationResponse.getPenaltyApplicability()));
        arguments.setProductContractId(calculationResponse.getProductContractId());
        arguments.setServiceContractId(calculationResponse.getServiceContractId());
        arguments.setActionTypeId(calculationResponse.getActionTypeId());
        arguments.setActionExecutionDate(calculationResponse.getExecutionDate());
        arguments.setActionTerminationId(calculationResponse.getActionTerminationId());
        arguments.setActionPods(StringUtils.isEmpty(calculationResponse.getActionPods())
                                        ? List.of()
                                        : Stream.of(calculationResponse.getActionPods().split(","))
                .map(Long::parseLong) // param returned from db and is safe to be parsed this way
                .collect(Collectors.toList()));
        return arguments;
    }
}
