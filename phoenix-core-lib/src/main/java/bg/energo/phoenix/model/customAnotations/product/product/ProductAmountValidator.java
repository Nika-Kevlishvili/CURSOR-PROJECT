package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
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
@Constraint(validatedBy = {ProductAmountValidator.ProductAmountValidatorImpl.class})
public @interface ProductAmountValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductAmountValidatorImpl implements ConstraintValidator<ProductAmountValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();

            BigDecimal amount = request.getAmount();
            BigDecimal amountFrom = request.getAmountFrom();
            BigDecimal amountTo = request.getAmountTo();

            if (amountFrom != null && amountTo != null && amount != null) {
                if (amountFrom.compareTo(amount) > 0 || amount.compareTo(amountTo) > 0) {
                    validationMessageBuilder.append(String.format("priceSettings.amount-Must be between Amount From [%s] and Amount To [%s];", amountFrom, amountTo));
                }

                if (amountFrom.compareTo(amountTo) > 0) {
                    validationMessageBuilder.append("priceSettings.amountFrom-Amount From must be less then Amount To;");
                }
            } else if (amountFrom != null && amount != null) {
                if (!(amountFrom.compareTo(amount) <= 0)) {
                    validationMessageBuilder.append("priceSettings.amount-Must be greater than [Amount From];");
                }
            } else if (amountTo != null && amount != null) {
                if (!(amount.compareTo(amountTo) <= 0)) {
                    validationMessageBuilder.append("priceSettings.amount-Must be less than [Amount To];");
                }
            }

            if (!validationMessageBuilder.isEmpty()) {
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();

            return isValid;
        }
    }
}
