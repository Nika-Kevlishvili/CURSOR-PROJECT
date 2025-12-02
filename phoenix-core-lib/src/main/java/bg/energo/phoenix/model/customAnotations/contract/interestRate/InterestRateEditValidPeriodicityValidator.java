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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InterestRateEditValidPeriodicityValidator.InterestRateEditValidPeriodicityValidatorImpl.class})
public @interface InterestRateEditValidPeriodicityValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InterestRateEditValidPeriodicityValidatorImpl implements ConstraintValidator<InterestRateEditValidPeriodicityValidator, InterestRateEditRequest> {
        @Override
        public boolean isValid(InterestRateEditRequest request, ConstraintValidatorContext context) {
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
            List<InterestRatePeriodsEditRequest> interestRatePeriodsEditRequest =
                    request.getInterestRatePeriods();

            for (int i = 0; i < interestRatePeriodsEditRequest.size(); i++) {
                InterestRatePeriodsEditRequest interestRatePeriods =
                        interestRatePeriodsEditRequest.get(i);

                if (interestRatePeriods.getValidFrom() != null) {
                    if (!EPBDateUtils.isDateInRange(interestRatePeriods.getValidFrom(), LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                        isValid = false;
                        validationMessage.append("interestRatePeriodsCreateRequest[%s].validFrom-[ValidFrom] %s Should be in range of 01.01.1990 to 31.12.2090 dates;".formatted(i, interestRatePeriods.getValidFrom()));
                    }
                } else {
                    isValid = false;
                    validationMessage.append("interestRatePeriodsCreateRequest[%s].validFrom-[ValidFrom] should be present.;".formatted(i));
                }
            }
            if(isValid){
                isValid = checkForSamePeriodFromDates(request.getInterestRatePeriods(),validationMessage);
            }
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
        private boolean checkForSamePeriodFromDates(List<InterestRatePeriodsEditRequest> interestRatePeriodsCreateRequest, StringBuilder validationMessage) {
            Set<LocalDate> validFromSet = new HashSet<>();
            for (int i=0;i< interestRatePeriodsCreateRequest.size(); i++) {
                InterestRatePeriodsEditRequest request = interestRatePeriodsCreateRequest.get(i);
                LocalDate validFrom = request.getValidFrom();
                if (!validFromSet.add(validFrom)) {
                    validationMessage.append("interestRatePeriodsCreateRequest[%s].validFrom-[validFrom] Duplicate validFrom value found: %s ;".formatted(i,validFrom));
                    return false;
                }
            }
            return true;
        }
    }
}
