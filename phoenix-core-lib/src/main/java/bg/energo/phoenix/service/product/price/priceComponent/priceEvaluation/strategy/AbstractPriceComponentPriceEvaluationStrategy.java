package bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.PriceComponentExpressionHolder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractPriceComponents;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.repository.contract.product.ProductContractPriceComponentRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationModel;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationResultModel;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractPriceComponentPriceEvaluationStrategy {
    protected final ExpressionStringParser expressionStringParser;
    protected final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    protected final BillingDataByProfileRepository billingDataByProfileRepository;
    protected final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;
    protected final ProductContractPriceComponentRepository productContractPriceComponentRepository;

    public abstract PriceComponentFormulaComplexity getComplexity();

    public abstract List<BillingDataPriceComponentPriceEvaluationResultModel> evaluate(String priceFormula, BillingDataPriceComponentPriceEvaluationModel model) throws RuntimeException, OgnlException;

    /**
     * Determines the complexity of a price component formula based on the variables used in the formula.
     *
     * @param formula the price component formula to be analyzed
     * @return the complexity of the price component formula
     */
    public static PriceComponentFormulaComplexity definePriceComponentFormulaComplexity(String formula) {
        Set<String> variables = new HashSet<>(ExpressionStringParser.extractVariableNames(formula));

        boolean containsPriceProfile = false;
        boolean containsPriceParameter = false;

        List<String> variableNames = Arrays.stream(PriceComponentMathVariableName.values()).map(PriceComponentMathVariableName::name).toList();

        for (String var : variables) {
            if (containsPriceParameter && containsPriceProfile) {
                break;
            }

            if (var.equals(PriceComponentMathVariableName.PRICE_PROFILE.name())) {
                containsPriceProfile = true;
            }

            if (!variableNames.contains(var)) {
                containsPriceParameter = true;
            }
        }

        if (containsPriceParameter && containsPriceProfile) {
            return PriceComponentFormulaComplexity.WITH_PRICE_PARAMETER_AND_PRICE_PROFILE;
        } else if (containsPriceProfile) {
            return PriceComponentFormulaComplexity.WITH_PRICE_PROFILE;
        } else if (containsPriceParameter) {
            return PriceComponentFormulaComplexity.WITH_PRICE_PARAMETER;
        } else {
            return PriceComponentFormulaComplexity.PRIMITIVE;
        }
    }

    /**
     * Validates the conditions of a list of PriceComponentExpressionHolders.
     *
     * @param context                         The OgnlContext object.
     * @param priceComponentExpressionHolders The list of PriceComponentExpressionHolders to validate.
     * @return An optional PriceComponentExpressionHolder that satisfies the condition, if any.
     * @throws OgnlException if there is an error during Ognl evaluation.
     */
    private Optional<PriceComponentExpressionHolder> validateConditions(OgnlContext context, List<PriceComponentExpressionHolder> priceComponentExpressionHolders) throws OgnlException {
        priceComponentExpressionHolders.sort(Comparator.comparing(PriceComponentExpressionHolder::getOperator));
        for (PriceComponentExpressionHolder expression : priceComponentExpressionHolders) {
            if (expression.getCondition() == null) {
                return Optional.of(expression);
            }

            if ((Boolean) Ognl.getValue(expression.getCondition(), context, context.getRoot())) {
                return Optional.of(expression);
            }
        }
        return Optional.empty();
    }

    /**
     * Puts the PriceComponentFormulaVariables in the given context.
     *
     * @param model                          The BillingDataPriceComponentPriceEvaluationModel.
     * @param variablesContext               The map representing the variables context.
     * @param priceComponentFormulaVariables A list of PriceComponentFormulaVariable objects.
     * @throws IllegalArgumentsProvidedException if any of the required variables are not found or have invalid values.
     */
    protected void putPriceComponentFormulaVariablesInContext(BillingDataPriceComponentPriceEvaluationModel model, Map<String, Object> variablesContext, List<PriceComponentFormulaVariable> priceComponentFormulaVariables) {
        for (PriceComponentFormulaVariable formulaVariable : priceComponentFormulaVariables) {
            if (variablesContext.containsKey("$%s$".formatted(formulaVariable.getVariable()))) {
                continue;
            }

            PriceComponentMathVariableName variable = formulaVariable.getVariable();
            List<PriceComponentFormulaVariable> respectiveFormulaVariables = priceComponentFormulaVariables
                    .stream()
                    .filter(pcf -> pcf.getVariable().equals(variable))
                    .toList();

            if (CollectionUtils.isEmpty(respectiveFormulaVariables)) {
                log.error("Respective Formula Variable [%s] not found in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
                throw new IllegalArgumentsProvidedException("Respective Formula Variable [%s] not found in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
            }

            if (respectiveFormulaVariables.size() > 1) {
                log.error("Multiple Formula Variables [%s] found in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
                throw new IllegalArgumentsProvidedException("Multiple Formula Variables [%s] found in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
            }

            PriceComponentFormulaVariable respectiveFormulaVariable = respectiveFormulaVariables.get(0);

            if (Objects.isNull(respectiveFormulaVariable.getValue())) {
                log.error("Formula Variable [%s] value is null in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
                throw new IllegalArgumentsProvidedException("Formula Variable [%s] value is null in Price Component with id: [%s];".formatted(variable, model.priceComponentId()));
            }

            variablesContext.put("$%s$".formatted(variable.name()), respectiveFormulaVariable.getValue());
        }
    }

    /**
     * Puts the product contract formula variables in the given variable context.
     *
     * @param model                              The billing data price component price evaluation model.
     * @param variableContext                    The variable context in which to put the formula variables.
     * @param productContractPriceComponentsList The list of product contract price components.
     */
    protected void putProductContractFormulaVariablesInContext(BillingDataPriceComponentPriceEvaluationModel model,
                                                               Map<String, Object> variableContext,
                                                               List<ProductContractPriceComponents> productContractPriceComponentsList) {
        if (CollectionUtils.isNotEmpty(productContractPriceComponentsList)) {
            for (ProductContractPriceComponents productContractPriceComponents : productContractPriceComponentsList) {
                if (Objects.nonNull(productContractPriceComponents.getValue())) {
                    Long priceComponentFormulaVariableId = productContractPriceComponents.getPriceComponentFormulaVariableId();

                    PriceComponentFormulaVariable priceComponentFormulaVariable = priceComponentFormulaVariableRepository
                            .findById(priceComponentFormulaVariableId)
                            .orElseThrow(() -> new IllegalArgumentsProvidedException("Formula Variable: [%s] not found for contract: [%s]".formatted(priceComponentFormulaVariableId, model.productContractDetailId())));

                    variableContext.put("$%s$".formatted(priceComponentFormulaVariable.getVariable()), productContractPriceComponents.getValue());
                }
            }
        }
    }

    /**
     * Evaluates an expression using the specified formula and variables context.
     *
     * @param formula          the formula expression to be evaluated
     * @param variablesContext the variables context containing the variable names and their corresponding values
     * @return the result of the evaluated expression as a BigDecimal
     * @throws OgnlException if there is an error during the evaluation of the expression
     */
    protected BigDecimal evaluateExpression(String formula, Map<String, Object> variablesContext) throws OgnlException {
        OgnlContext context = (OgnlContext) Ognl.createDefaultContext(variablesContext);

        List<String> ifElseComponentList = ExpressionStringParser.findAllIfElseComponents(formula);
        for (String ifElseComponent : ifElseComponentList) {
            List<PriceComponentExpressionHolder> priceComponentExpressions = expressionStringParser.parseIfElseStatement(ifElseComponent);
            PriceComponentExpressionHolder validExpression = validateConditions(context, priceComponentExpressions)
                    .orElseThrow(() -> {
                        log.error("Exception on determination of valid expression;");
                        return new ClientException("Exception on determination of valid expression;", ErrorCode.APPLICATION_ERROR);
                    });
            formula = formula.replace(ifElseComponent, "(" + validExpression.getStatement() + ")");
        }
        return (BigDecimal) Ognl.getValue(formula, context, context.getRoot(), BigDecimal.class);
    }


    /**
     * Adjusts the given initial date from based on the specified dimension.
     *
     * @param initialDateFrom the initial date from to be adjusted
     * @param dimension       the dimension to adjust the date by (FIFTEEN_MINUTES, ONE_HOUR, ONE_DAY, ONE_MONTH)
     * @return the adjusted date from based on the specified dimension
     */
    protected LocalDateTime adjustDateFromByDimension(LocalDateTime initialDateFrom, PeriodType dimension) {
        if (initialDateFrom == null || dimension == null) {
            return null;
        }
        switch (dimension) {
            case FIFTEEN_MINUTES -> {
                int minute = initialDateFrom.getMinute();
                if (minute < 15) {
                    return initialDateFrom.toLocalDate().atTime(initialDateFrom.getHour(), 0, 0);
                }

                if (minute < 30) {
                    return initialDateFrom.toLocalDate().atTime(initialDateFrom.getHour(), 15, 0);
                }

                if (minute < 45) {
                    return initialDateFrom.toLocalDate().atTime(initialDateFrom.getHour(), 30, 0);
                }

                return initialDateFrom.toLocalDate().atTime(initialDateFrom.getHour(), 45, 0);
            }
            case ONE_HOUR -> {
                return initialDateFrom.truncatedTo(ChronoUnit.HOURS);
            }
            case ONE_DAY -> {
                return initialDateFrom.truncatedTo(ChronoUnit.DAYS);
            }
            case ONE_MONTH -> {
                return initialDateFrom.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atTime(0, 0, 0);
            }
        }

        return initialDateFrom;
    }

    /**
     * Adjusts the given date according to the specified dimension.
     *
     * @param initialDateTo the initial LocalDateTime object to be adjusted
     * @param dimension     the PeriodType indicating the dimension of adjustment
     * @return the adjusted LocalDateTime object
     */
    protected LocalDateTime adjustDateToByDimension(LocalDateTime initialDateTo, PeriodType dimension) {
        if (initialDateTo == null || dimension == null) {
            return null;
        }
        switch (dimension) {
            case FIFTEEN_MINUTES -> {
                int minute = initialDateTo.getMinute();
                if (minute > 45) {
                    return initialDateTo.toLocalDate().atTime(initialDateTo.getHour(), 0, 0).plusHours(1);
                }

                if (minute > 30) {
                    return initialDateTo.toLocalDate().atTime(initialDateTo.getHour(), 45, 0);
                }

                if (minute > 15) {
                    return initialDateTo.toLocalDate().atTime(initialDateTo.getHour(), 30, 0);
                }

                return initialDateTo.toLocalDate().atTime(initialDateTo.getHour(), 15, 0);
            }
            case ONE_HOUR -> {
                if (!initialDateTo.truncatedTo(ChronoUnit.HOURS).equals(initialDateTo)) {
                    return initialDateTo.truncatedTo(ChronoUnit.HOURS).plusHours(1);
                }
            }
            case ONE_DAY -> {
                if (!initialDateTo.truncatedTo(ChronoUnit.DAYS).equals(initialDateTo)) {
                    return initialDateTo.truncatedTo(ChronoUnit.DAYS).plusDays(1);
                }

                return initialDateTo.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            }
            case ONE_MONTH -> {
                if (initialDateTo.getDayOfMonth() != 1) {
                    return initialDateTo.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfNextMonth());
                }

                return initialDateTo.plusMonths(1).toLocalDate().atTime(0, 0, 0);
            }
        }

        return initialDateTo;
    }
}
