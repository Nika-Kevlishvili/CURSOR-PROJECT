package bg.energo.phoenix.model.customAnotations.product.priceComponent;

import bg.energo.phoenix.model.request.product.price.priceComponent.FormulaValidationRequest;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentFormulaVariableRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidFormulaVariables.FormulaVariablesValidator.class})
public @interface ValidFormulaVariables {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class FormulaVariablesValidator implements ConstraintValidator<ValidFormulaVariables, FormulaValidationRequest> {

        @Override
        public boolean isValid(FormulaValidationRequest request, ConstraintValidatorContext context) {

            List<PriceComponentFormulaVariableRequest> variables = request.getVariables();
            Set<String> xValues = extractXValuesFromExpression(request);


            // if expression is empty and variables are empty, then the formula is valid
            if (CollectionUtils.isEmpty(xValues) && CollectionUtils.isEmpty(variables)) {
                return true;
            }

            // if expression is empty and variables are not empty, or otherwise, then the formula is invalid
            if ((CollectionUtils.isEmpty(xValues) && CollectionUtils.isNotEmpty(variables)) || (CollectionUtils.isNotEmpty(xValues) && CollectionUtils.isEmpty(variables))) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("formulaRequest.expression-Expression and variables should match;").addConstraintViolation();
                return false;
            }

            StringBuilder validationMessage = new StringBuilder();

            // if expression contains variables that are not present in the variables list, then the formula is invalid
            xValues.forEach(xValue -> {
                boolean isVariablePresent = variables.stream().anyMatch(variable -> variable.getVariable().equals(xValue));
                if (!isVariablePresent) {
                    validationMessage.append("formulaRequest.expression-Variables should contain %s;".formatted(xValue));
                }
            });

            validateValueFieldCombinations(variables, validationMessage);

            if (validationMessage.length() > 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }


        private static Set<String> extractXValuesFromExpression(FormulaValidationRequest request) {
            Set<String> xValues = new HashSet<>();
            Pattern pattern = Pattern.compile("\\$X\\d\\$");
            Matcher matcher = pattern.matcher(request.getExpression());
            while (matcher.find()) {
                String xValue = matcher.group();
                xValues.add(xValue.replace("$", ""));
            }
            return xValues;
        }


        private static void validateValueFieldCombinations(List<PriceComponentFormulaVariableRequest> variables, StringBuilder validationMessage) {
            for (int i = 0; i < variables.size(); i++) {
                PriceComponentFormulaVariableRequest variableRequest = variables.get(i);

                // every variable, if present, should have at least a description
                if (StringUtils.isEmpty(variableRequest.getDescription())) {
                    validationMessage.append("formulaRequest.variables[%s].description-Description cannot be blank;".formatted(i));
                }

                BigDecimal value = variableRequest.getValue();
                BigDecimal valueFrom = variableRequest.getValueFrom();
                BigDecimal valueTo = variableRequest.getValueTo();

                // if values are present, they should satisfy the range conditions
                if (value != null) {
                    if (valueFrom != null && value.compareTo(valueFrom) < 0) {
                        validationMessage.append("formulaRequest.variables[%s].value-Value should not be less than valueFrom;".formatted(i));
                    }

                    if (valueTo != null && value.compareTo(valueTo) > 0) {
                        validationMessage.append("formulaRequest.variables[%s].value-Value should not be more than valueTo;".formatted(i));
                    }
                }

                if (valueFrom != null) {
                    if (valueTo != null && valueFrom.compareTo(valueTo) > 0) {
                        validationMessage.append("formulaRequest.variables[%s].valueFrom-ValueFrom should not be greater than valueTo;".formatted(i));
                    }
                }
            }
        }

    }

}
