package bg.energo.phoenix.model.customAnotations.contract.interestRate;

import bg.energo.phoenix.model.request.contract.interestRate.InterestRateCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InterestRateValidPeriodicityValidator.InterestRateValidPeriodicityValidatorImpl.class})
public @interface InterestRateValidPeriodicityValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InterestRateValidPeriodicityValidatorImpl implements ConstraintValidator<InterestRateValidPeriodicityValidator, InterestRateCreateRequest> {
        @Override
        public boolean isValid(InterestRateCreateRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            boolean isValid = true;

            if (request.getGracePeriod() != null) {
                if (request.getPeriodicity() == null) {
                    isValid = false;
                    validationMessage.append("periodicity-[Periodicity] Must not be null when [GracePeriod] is filled in;");
                }
            } else {
                if (request.getPeriodicity() != null) {
                    isValid = false;
                    validationMessage.append("periodicity-[Periodicity] Must cant be filled in when [GracePeriod] is null;");
                }
            }


            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
