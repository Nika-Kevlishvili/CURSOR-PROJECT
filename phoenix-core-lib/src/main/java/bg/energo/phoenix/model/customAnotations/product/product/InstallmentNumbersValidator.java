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
@Constraint(validatedBy = {InstallmentNumbersValidator.InstallmentValidatorImpl.class})
public @interface InstallmentNumbersValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InstallmentValidatorImpl implements ConstraintValidator<InstallmentNumbersValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();

            Short installmentNumber = request.getInstallmentNumber();
            Short installmentNumberFrom = request.getInstallmentNumberFrom();
            Short installmentNumberTo = request.getInstallmentNumberTo();
            Boolean equalMonthlyInstallmentsActivation = request.getEqualMonthlyInstallmentsActivation();
            if (equalMonthlyInstallmentsActivation != null && equalMonthlyInstallmentsActivation) {
                if (installmentNumber != null && installmentNumberFrom != null && installmentNumberTo != null) {
                    if (installmentNumber < installmentNumberFrom || installmentNumber > installmentNumberTo) {
                        validationMessageBuilder.append(String.format("priceSettings.installmentNumber-[Installment Number] must be between installmentNumberFrom [%s] and installmentNumberTo [%s];", installmentNumberFrom, installmentNumberTo));
                    }

                    if (installmentNumberFrom > installmentNumberTo) {
                        validationMessageBuilder.append("priceSettings.installmentNumberFrom-[Installment Number From] must be less then [Installment Number To];");
                    }
                } else if (installmentNumberFrom != null && installmentNumber != null) {
                    if (installmentNumberFrom > installmentNumber) {
                        validationMessageBuilder.append("priceSettings.installmentNumber-[Installment Number] must be greater than [Installment Number From];");
                    }
                } else if (installmentNumberTo != null && installmentNumber != null) {
                    if (installmentNumber > installmentNumberTo) {
                        validationMessageBuilder.append("priceSettings.installmentNumber-[Installment Number] must be less than [Installment Number To];");
                    }
                }
            } else {
                if (installmentNumber != null) {
                    validationMessageBuilder.append("priceSettings.installmentNumber-[Installment Number] must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (installmentNumberFrom != null) {
                    validationMessageBuilder.append("priceSettings.installmentNumberFrom-[Installment Number From] must be null while [equalMonthlyInstallmentsActivation] is false;");
                }

                if (installmentNumberTo != null) {
                    validationMessageBuilder.append("priceSettings.installmentNumberTo-[Installment Number To] must be null while [equalMonthlyInstallmentsActivation] is false;");
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
