package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.request.product.service.ServicePriceSettingsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceAmountValues.ServiceAmountValuesValidator.class})
public @interface ValidServiceAmountValues {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceAmountValuesValidator implements ConstraintValidator<ValidServiceAmountValues, ServicePriceSettingsRequest> {

        @Override
        public boolean isValid(ServicePriceSettingsRequest request, ConstraintValidatorContext context) {
            BigDecimal amount = request.getAmount();
            BigDecimal amountFrom = request.getAmountFrom();
            BigDecimal amountTo = request.getAmountTo();

            StringBuilder validationMessage = new StringBuilder();

            validateAmount(amount, amountFrom, amountTo, validationMessage);
            validateAmountFrom(amount, amountFrom, amountTo, validationMessage);
            validateAmountTo(amount, amountFrom, amountTo, validationMessage);

            boolean isValid = validationMessage.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }

        private void validateAmount(BigDecimal amount, BigDecimal amountFrom, BigDecimal amountTo, StringBuilder validationMessage) {
            if (amount != null) {
                if (amountFrom != null && amount.compareTo(amountFrom) < 0) {
                    validationMessage.append("priceSettings.amount-Amount should not be less than amountFrom;");
                }

                if (amountTo != null && amount.compareTo(amountTo) > 0) {
                    validationMessage.append("priceSettings.amount-Amount should not be greater than amountTo;");
                }
            }
        }

        private void validateAmountFrom(BigDecimal amount, BigDecimal amountFrom, BigDecimal amountTo, StringBuilder validationMessage) {
            if (amountFrom != null) {
                if (amount != null && amountFrom.compareTo(amount) > 0) {
                    validationMessage.append("priceSettings.amountFrom-AmountFrom should not be greater than amount;");
                }

                if (amountTo != null && amountFrom.compareTo(amountTo) > 0) {
                    validationMessage.append("priceSettings.amountFrom-AmountFrom should not be greater than amountTo;");
                }
            }
        }

        private void validateAmountTo(BigDecimal amount, BigDecimal amountFrom, BigDecimal amountTo, StringBuilder validationMessage) {
            if (amountTo != null) {
                if (amount != null && amount.compareTo(amountTo) > 0) {
                    validationMessage.append("priceSettings.amountTo-AmountTo should not be less than amount;");
                }

                if (amountFrom != null && amountFrom.compareTo(amountTo) > 0) {
                    validationMessage.append("priceSettings.amountTo-AmountTo should not be less than amountFrom;");
                }
            }
        }

    }

}
