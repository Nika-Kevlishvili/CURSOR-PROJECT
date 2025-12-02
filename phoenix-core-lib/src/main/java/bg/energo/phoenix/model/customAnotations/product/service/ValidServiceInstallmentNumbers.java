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

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceInstallmentNumbers.ServiceInstallmentNumbersValidator.class})
public @interface ValidServiceInstallmentNumbers {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceInstallmentNumbersValidator implements ConstraintValidator<ValidServiceInstallmentNumbers, ServicePriceSettingsRequest> {

        @Override
        public boolean isValid(ServicePriceSettingsRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            if (request.getEqualMonthlyInstallmentsActivation() == null) {
                return false;
            }

            Boolean equalMonthlyInstallmentsActivation = request.getEqualMonthlyInstallmentsActivation();
            Short installmentNumber = request.getInstallmentNumber();
            Short installmentNumberFrom = request.getInstallmentNumberFrom();
            Short installmentNumberTo = request.getInstallmentNumberTo();

            if (equalMonthlyInstallmentsActivation) {
                if (request.getCurrencyId() == null) {
                    validationMessage.append("priceSettings.currencyId-[Currency] must not be null while [equalMonthlyInstallmentsActivation] is true;");
                }

                validateInstallmentNumberFrom(installmentNumber, installmentNumberFrom, installmentNumberTo, validationMessage);
                validateInstallmentNumber(installmentNumber, installmentNumberFrom, installmentNumberTo, validationMessage);
                validateInstallmentNumberTo(installmentNumber, installmentNumberFrom, installmentNumberTo, validationMessage);
            } else {
                validateNullField(installmentNumber, "priceSettings.installmentNumber-[Installment Number] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(installmentNumberFrom, "priceSettings.installmentNumberFrom-[Installment Number From] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(installmentNumberTo, "priceSettings.installmentNumberTo-[Installment Number To] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(request.getAmount(), "priceSettings.amount-[Amount] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(request.getAmountFrom(), "priceSettings.amountFrom-[AmountFrom] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(request.getAmountTo(), "priceSettings.amountTo-[AmountTo] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
                validateNullField(request.getCurrencyId(), "priceSettings.currencyId-[Currency] must be null while [equalMonthlyInstallmentsActivation] is false;", validationMessage);
            }

            boolean isValid = validationMessage.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }

        private void validateInstallmentNumber(Short number, Short numberFrom, Short numberTo, StringBuilder validationMessage) {
            if (number != null) {
                if (numberFrom != null && number.compareTo(numberFrom) < 0) {
                    validationMessage.append("priceSettings.installmentNumber-Installment number should not be less than installmentNumberFrom;");
                }

                if (numberTo != null && number.compareTo(numberTo) > 0) {
                    validationMessage.append("priceSettings.installmentNumber-Installment number should not be greater than installmentNumberTo;");
                }
            }
        }

        private void validateInstallmentNumberFrom(Short number, Short numberFrom, Short numberTo, StringBuilder validationMessage) {
            if (numberFrom != null) {
                if (number != null && numberFrom.compareTo(number) > 0) {
                    validationMessage.append("priceSettings.installmentNumberFrom-Installment numberFrom should not be greater than installmentNumber;");
                }

                if (numberTo != null && numberFrom.compareTo(numberTo) > 0) {
                    validationMessage.append("priceSettings.installmentNumberFrom-Installment numberFrom should not be greater than installmentNumberTo;");
                }
            }
        }

        private void validateInstallmentNumberTo(Short number, Short numberFrom, Short numberTo, StringBuilder validationMessage) {
            if (numberTo != null) {
                if (number != null && number.compareTo(numberTo) > 0) {
                    validationMessage.append("priceSettings.installmentNumberTo-Installment numberTo should not be less than installmentNumber;");
                }

                if (numberFrom != null && numberFrom.compareTo(numberTo) > 0) {
                    validationMessage.append("priceSettings.installmentNumberTo-Installment numberTo should not be less than installmentNumberFrom;");
                }
            }
        }

        private void validateNullField(Object fieldValue, String validationErrorMessage, StringBuilder validationMessage) {
            if (fieldValue != null) {
                validationMessage.append(validationErrorMessage);
            }
        }

    }

}
