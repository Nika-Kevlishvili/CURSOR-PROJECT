package bg.energo.phoenix.service.product.price.priceComponent;

import bg.energo.common.utils.StringUtils;
import bg.energo.phoenix.model.PriceComponentExpressionHolder;
import bg.energo.phoenix.model.enums.product.price.priceComponent.LogicalOperator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.MainLogicalOperator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableValue;
import bg.energo.phoenix.util.epb.EPBMathUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ExpressionStringParser {

    public static final String REGEX_FOR_BRACES = "(.*?)\\{(.*?)\\}";
    public static final String IF_ELSE_VALIDATOR_REGEX = "^(IF)(\\{[^{}]*}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*}){1})*$";
    public static final String IF_ELSE_CONTAINS_VALIDATOR_REGEX = "(IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*";
    public static final String CONDITION_VALIDATOR_REGEX = "^([^()]*|\\(([^()]*|\\([^()]*\\))*\\))*((\\.doubleValue\\(\\))*)*$";
    public static final String REGEX_FOR_CONDITION_AND_STATEMENT = "^\\(([^()]+(?:\\([^()]*\\)[^()]*)*)\\)(.*)$";

    public List<PriceComponentExpressionHolder> parseExpression(String expression) {
        try {
            expression = expression.replace(" ", "").replace("\n", "");
            expression = expression.replaceAll("(?i)in\\((.*?)\\)", " in($1)");
            expression = expression.replaceAll("\\s+", " ");
            List<PriceComponentExpressionHolder> result = new ArrayList<>();
            String expressionForIfElseValidation = expression;
            List<String> ifElseComponentList = new ArrayList<>(findAllIfElseComponents(expressionForIfElseValidation));
            String expressionForMathValidation = expression;
            for (String ifElseComponent : ifElseComponentList) {
                result.addAll(parseIfElseStatement(ifElseComponent));
                expressionForMathValidation = expressionForMathValidation.replace(ifElseComponent, "1");
            }
            if (isValidMathFormula(expressionForMathValidation)) {
                result.addAll(parseMathFormula(expressionForMathValidation));
                return result;
            }
            throw new IllegalArgumentException("formulaRequest.expression- [expression] Expression is not valid;");
        } catch (Exception e) {
            log.error("Error while parsing expression : {}", expression, e);
            throw new IllegalArgumentException("formulaRequest.expression- [expression] Expression is not valid;");
        }
    }

    public boolean isValidMultiplySyntax(String formula) {
        formula = formula.replace(" ", "").replace("\n", "");

        List<String> wrongMultiplySyntaxRegexPatterns = List.of(
                //$X1$2 instead of $X1$*2
                "\\$X[0-9]\\$[0-9]",
                //2$X1$2 instead of 2*$X1$2
                "[0-9]\\$X[0-9]\\$",
                //$X2$$X1$ instead of $X2$*$X1$
                "\\$\\$X",
                //2(if group) instead of 2* (if group)
                "((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)[0-9]",
                //(if group)2 instead of (if group)*2
                "[0-9]((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)[0-9]",
                //(if group)(if group) instead of (if group)*(if group)
                "((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)",
                //(if group)$X1$ instead of (if group)*$X1$
                "((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)\\$",
                //$X1$(if group) instead of $X1$*(if group)
                "\\$((IF)(\\{[^{}]*\\}){1}((IF|ELSEIF|ELSE)(\\{[^{}]*\\}){1})*)",
                //$PRICE_PROFILE$2 instead of $PRICE_PROFILE$*2
                "\\$PRICE_PROFILE\\$[0-9]",
                //2$PRICE_PROFILE$ instead of 2*$PRICE_PROFILE$
                "[0-9]\\$PRICE_PROFILE\\$",
                //$122234$3 instead of $122234$*3
                "\\$[0-9]+\\$[0-9]",
                //4$122234$ instead of 4*$122234$
                "[0-9]\\$[0-9]+\\$"
        );
        for (String pat : wrongMultiplySyntaxRegexPatterns) {
            Pattern pattern = Pattern.compile(pat);
            Matcher matcher = pattern.matcher(formula);
            if (matcher.find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the expression contains if-else statements
     */
    public static boolean isIfElseStatement(String expression) {
        Matcher matcher = Pattern.compile(IF_ELSE_VALIDATOR_REGEX).matcher(expression.replace(" ", "").replace("\n", ""));
        return matcher.matches();
    }

    public static List<String> findAllIfElseComponents(String expression) {
        List<String> ifElseComponents = new ArrayList<>();
        while (isContainsIfELseStatement(expression) != null) {
            String ifElseComponent = isContainsIfELseStatement(expression);
            assert ifElseComponent != null;
            expression = expression.replace(ifElseComponent, "");
            ifElseComponents.add(ifElseComponent);
        }
        return ifElseComponents;
    }

    public static String isContainsIfELseStatement(String expression) {
        Pattern pattern = Pattern.compile(IF_ELSE_CONTAINS_VALIDATOR_REGEX);
        Matcher matcher = pattern.matcher(expression);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    /**
     * Checks if the expression is a valid math formula
     */
    public static boolean isValidMathFormula(String expression) {
        String expr = expression.replace(".doubleValue()","");
        var variables = new HashSet<>(extractVariableNames(expr));
        return EPBMathUtils.isMathExpressionValid(expr.replace("$", ""), variables);
    }

    /**
     * Iterates over the condition and returns a list of all variables in the condition denoted by $variableName$
     */
    public static List<String> extractVariableNames(String condition) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(condition)) {
            Pattern pattern = Pattern.compile("\\$(.*?)\\$");
            Matcher matcher = pattern.matcher(condition);
            while (matcher.find()) {
                String match = matcher.group(1);
                result.add(match);
            }
        }
        return result;
    }

    private List<PriceComponentExpressionHolder> parseMathFormula(String expression) {
        return Collections.singletonList(PriceComponentExpressionHolder.builder().statement(expression).build());
    }

    public List<String> getVariables(String condition) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$(.*?)\\$");
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String match = matcher.group(1);
            result.add(match);
        }
        return result;
    }

    public List<PriceComponentExpressionHolder> parseIfElseStatement(String expression) {

        List<PriceComponentExpressionHolder> output = new ArrayList<>();

        Matcher matcher = Pattern.compile(REGEX_FOR_BRACES).matcher(expression);
        Pattern conditionPattern = Pattern.compile(CONDITION_VALIDATOR_REGEX);

        Pattern expressionPattern = Pattern.compile(REGEX_FOR_CONDITION_AND_STATEMENT);
        while (matcher.find()) {
            String mainCondition = matcher.group(1).trim();
            String value = matcher.group(2);
            var expressionHolderBuilder = PriceComponentExpressionHolder.builder();
            MainLogicalOperator operator = MainLogicalOperator.valueOf(mainCondition);
            expressionHolderBuilder.operator(operator);
            if (MainLogicalOperator.ELSE.equals(operator)) {
                if (isValidMathFormula(value)) {
                    expressionHolderBuilder.statement(value);
                }
            } else {
                Matcher expressionMatcher = expressionPattern.matcher(value);
                if (expressionMatcher.matches()) {
                    String condition = replaceParamsForIfElseStat(expressionMatcher.group(1));
                    if (!conditionPattern.matcher(condition).matches()) {
                        throw new IllegalArgumentException("Expression is not valid");
                    }
                    String statement = expressionMatcher.group(2);
                    try {
                        if (isValidMathFormula(statement)) {
                            expressionHolderBuilder.statement(statement);
                        }
                    } catch (Exception e) {
                        log.error("Error while validating math Formula");
                        throw new IllegalArgumentException("Expression is not valid");
                    }
                    expressionHolderBuilder.condition(condition);
                } else {
                    throw new IllegalArgumentException("Expression is not valid");
                }
            }

            output.add(expressionHolderBuilder.build());
        }
        return output;
    }

    public String replaceParams(String condition) {
        if (StringUtils.isNullOrEmpty(condition)) {
            return condition;
        }

        String result = condition;

        //replace '=' first. We want to replace '=' with '==' to make it a logical operator. We do not want to replace '=' in case of '<=' of '>='
        String regex = "(?<=[^<>])=(?!=)";
        result = result.replaceAll(regex, "==");

        for (LogicalOperator logicalOperator : LogicalOperator.values()) {
            result = result.replace(logicalOperator.getOriginalValue(), logicalOperator.getValue());
        }

        //replace in (values) with in {values} since the evaluator expects {} wrapper
        result = result.replaceAll("(?i)in\\((.*?)\\)", " in{$1}");
        result = result.replaceAll("\\s+", " ");

        return result;
    }

    public String replaceParamsForIfElseStat(String condition) {
        if (StringUtils.isNullOrEmpty(condition)) {
            return condition;
        }

        String result = condition;

        //replace '=' first. We want to replace '=' with '==' to make it a logical operator. We do not want to replace '=' in case of '<=' of '>='
        String regex = "(?<=[^<>])=(?!=)";
        result = result.replaceAll(regex, "==");

        for (LogicalOperator logicalOperator : LogicalOperator.values()) {
            result = result.replace(logicalOperator.getOriginalValue(), logicalOperator.getValue());
        }

        //replace in (values) with in {values} since the evaluator expects {} wrapper
        result = result.replaceAll("(?i)in.*\\(([^)]+)\\)", " in{$1}");
        result = result.replaceAll("\\s+", " ");

        return result;
    }

    public String replaceMainConditionFormat(String condition) {
        var valueVariables = Arrays.stream(PriceComponentConditionVariableValue.values())
                .map(Enum::name).toList();
        var result = condition;
        for (String allVar : valueVariables) {
            result = result.replace("$%s$".formatted(allVar), "\"%s\"".formatted(allVar));
        }
        return result;
    }

    public static boolean containsMultipleConsecutiveOperators(String expression) {
        String pattern = "\\+\\+|--|==|ANDAND|OROR|NOTNOT|inin|-\\+|\\+-";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(expression);
        return matcher.find();
    }
}
