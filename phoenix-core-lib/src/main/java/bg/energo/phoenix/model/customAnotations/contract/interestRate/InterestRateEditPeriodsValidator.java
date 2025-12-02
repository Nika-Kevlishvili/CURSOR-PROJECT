package bg.energo.phoenix.model.customAnotations.contract.interestRate;

import bg.energo.phoenix.model.request.contract.interestRate.InterestRateEditRequest;
import bg.energo.phoenix.model.request.contract.interestRate.InterestRatePeriodsEditRequest;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InterestRateEditPeriodsValidator.InterestRateEditPeriodsValidatorImpl.class})
public @interface InterestRateEditPeriodsValidator {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InterestRateEditPeriodsValidatorImpl implements ConstraintValidator<InterestRateEditPeriodsValidator, InterestRateEditRequest> {
        @Override
        public boolean isValid(InterestRateEditRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            boolean isValid = true;

            List<InterestRatePeriodsEditRequest> interestRatePeriodsCreateRequest =
                    request.getInterestRatePeriods();

            for (int i = 0; i < interestRatePeriodsCreateRequest.size(); i++) {
                InterestRatePeriodsEditRequest interestRatePeriods =
                        interestRatePeriodsCreateRequest.get(i);
                if (interestRatePeriods.getValidFrom() != null) {
                    if (!EPBDateUtils.isDateInRange(interestRatePeriods.getValidFrom(), LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                        isValid = false;
                        validationMessage.append("InterestRateEditRequest[%s].validFrom-[ValidFrom] %s Should be in range of 01.01.1990 to 31.12.2090 dates;".formatted(i, interestRatePeriods.getValidFrom()));
                    }
                } else {
                    isValid = false;
                    validationMessage.append("InterestRateEditRequest[%s].validFrom-[ValidFrom] should be present.".formatted(i));
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
