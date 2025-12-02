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

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ProductMonthlyInstallmentValidator.ProductMonthlyInstallmentValidatorImpl.class})
public @interface ProductMonthlyInstallmentValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductMonthlyInstallmentValidatorImpl implements ConstraintValidator<ProductMonthlyInstallmentValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            if (request.getEqualMonthlyInstallmentsActivation() == null) {
                return true;
            }

            boolean isValid = true;
            StringBuilder exceptionMessageBuilder = new StringBuilder();

            if (request.getEqualMonthlyInstallmentsActivation()) {
                if (request.getCurrencyId() == null) {
                    exceptionMessageBuilder.append("priceSettings.currencyId-Currency Must not be null while [equalMonthlyInstallmentsActivation] is true;");
                }
            } else {
                if (request.getAmount() != null) {
                    exceptionMessageBuilder.append("priceSettings.amount-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getAmountFrom() != null) {
                    exceptionMessageBuilder.append("priceSettings.amountFrom-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getAmountTo() != null) {
                    exceptionMessageBuilder.append("priceSettings.amountTo-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getInstallmentNumber() != null) {
                    exceptionMessageBuilder.append("priceSettings.installmentNumber-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getInstallmentNumberFrom() != null) {
                    exceptionMessageBuilder.append("priceSettings.installmentNumberFrom-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getInstallmentNumberTo() != null) {
                    exceptionMessageBuilder.append("priceSettings.installmentNumberTo-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (request.getCurrencyId() != null) {
                    exceptionMessageBuilder.append("priceSettings.currencyId-Must be null while [equalMonthlyInstallmentsActivation] is false;");
                }
            }

            if (!exceptionMessageBuilder.isEmpty()) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(exceptionMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
