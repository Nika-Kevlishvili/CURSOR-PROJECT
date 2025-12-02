package bg.energo.phoenix.model.customAnotations.pod.billingByProfile;

import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfileCreateRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidBillingByProfileDatesRange.BillingByProfileDatesRangeValidator.class})
public @interface ValidBillingByProfileDatesRange {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingByProfileDatesRangeValidator implements ConstraintValidator<ValidBillingByProfileDatesRange, BillingByProfileCreateRequest> {

        private static final LocalDate MIN_DATE = LocalDate.of(1990, Month.JANUARY, 1);
        private static final LocalDate MAX_DATE = LocalDate.of(2090, Month.DECEMBER, 31);

        @Override
        public boolean isValid(BillingByProfileCreateRequest request, ConstraintValidatorContext context) {
            StringBuilder sb = new StringBuilder();

            if (request.getPeriodFrom().isBefore(MIN_DATE.atStartOfDay()) || request.getPeriodFrom().isAfter(MAX_DATE.atStartOfDay())) {
                sb.append("periodFrom-Period from should be between %s and %s;".formatted(MIN_DATE.toString(), MAX_DATE.toString()));
            }

            if (request.getPeriodTo().isBefore(MIN_DATE.atStartOfDay()) || request.getPeriodTo().isAfter(MAX_DATE.atStartOfDay())) {
                sb.append("periodTo-Period to should be between %s and %s;".formatted(MIN_DATE.toString(), MAX_DATE.toString()));
            }

            if (request.getPeriodFrom().isAfter(request.getPeriodTo())) {
                sb.append("periodFrom-Period from should be before (or equal) period to;");
            }

            if (ChronoUnit.YEARS.between(request.getPeriodFrom(), request.getPeriodTo()) > 1) {
                sb.append("periodFrom-Period from and period to should be within one year;");
                sb.append("periodTo-Period from and period to should be within one year;");
            }

            if (!isPeriodFromWithinAllowedRange(request.getEntries(), request)) {
                sb.append("entries-Billing by profile entries should be created within ONE_DAY/ONE_MONTH range and should not extend over the main range;");
            }

            if (!sb.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }


        /**
         * Validates if the periodFrom values of the billing by profile entries are within the allowed range
         *
         * @param requests List of billing by profile entries
         * @param request  Billing by profile request containing the period type and main range information
         * @return true if the periodFrom values are within the allowed range, false otherwise
         */
        private boolean isPeriodFromWithinAllowedRange(List<BillingByProfileDataCreateRequest> requests, BillingByProfileCreateRequest request) {
            // Billing by profile can be created without values
            if (CollectionUtils.isEmpty(requests)) {
                return true;
            }

            // If period type is ONE_MONTH or ONE_DAY, only one value is allowed
            if ((request.getPeriodType().equals(PeriodType.ONE_MONTH) || request.getPeriodType().equals(PeriodType.ONE_DAY)) && requests.size() > 1) {
                return false;
            }

            // Sort the list based on periodFrom values in ascending order
            requests.sort(Comparator.comparing(BillingByProfileDataCreateRequest::getPeriodFrom));

            LocalDateTime endOfDayForPeriodTo = LocalDateTime.of(request.getPeriodTo().toLocalDate(), LocalTime.MAX);

            // Check if the first and last periodFrom values are within the main range
            if (requests.get(0).getPeriodFrom().isBefore(request.getPeriodFrom())
                    || requests.get(requests.size() - 1).getPeriodFrom().isAfter(endOfDayForPeriodTo)) {
                return false;
            }

            LocalDate firstPeriodFrom = requests.get(0).getPeriodFrom().toLocalDate();
            LocalDate lastPeriodFrom = requests.get(requests.size() - 1).getPeriodFrom().toLocalDate();

            // Compare the dates of the first and last periodFrom values to check if they belong to the same day/month
            return firstPeriodFrom.isEqual(lastPeriodFrom);
        }
    }
}
