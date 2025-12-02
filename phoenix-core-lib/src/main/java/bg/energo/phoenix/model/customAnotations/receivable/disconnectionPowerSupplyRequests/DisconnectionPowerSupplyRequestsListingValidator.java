package bg.energo.phoenix.model.customAnotations.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing.DPSRequestsListingRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalDate;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DisconnectionPowerSupplyRequestsListingValidator.PodMeasurementValidatorImpl.class})
public @interface DisconnectionPowerSupplyRequestsListingValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<DisconnectionPowerSupplyRequestsListingValidator, DPSRequestsListingRequest> {
        @Override
        public boolean isValid(DPSRequestsListingRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (isDatesInvalid(request.getGridOperatorRequestRegistrationDateFrom(), request.getGridOperatorRequestRegistrationDateTo())) {
                errors.append("gridOperatorRequestRegistrationDateFrom-gridOperatorRequestRegistrationDateFrom can not be after gridOperatorRequestRegistrationDateTo;");
            }

            if (isDatesInvalid(request.getCustomerReminderLetterSentDateFrom(), request.getCustomerReminderLetterSentDateTo())) {
                errors.append("customerReminderLetterSentDateFrom-customerReminderLetterSentDateFrom can not be after customerReminderLetterSentDateTo;");
            }

            if (isDatesInvalid(request.getGridOperatorDisconnectionFeePayDateFrom(), request.getGridOperatorDisconnectionFeePayDateTo())) {
                errors.append("gridOperatorDisconnectionFeePayDateFrom-gridOperatorDisconnectionFeePayDateFrom can not be after gridOperatorDisconnectionFeePayDateTo;");
            }

            if (isDatesInvalid(request.getPowerSupplyDisconnectionDateFrom(), request.getPowerSupplyDisconnectionDateTo())) {
                errors.append("powerSupplyDisconnectionDateFrom-powerSupplyDisconnectionDateFrom can not be after powerSupplyDisconnectionDateTo;");
            }

            if (Objects.nonNull(request.getNumberOfPodsFrom()) && Objects.nonNull(request.getNumberOfPodsTo()) && request.getNumberOfPodsFrom() > request.getNumberOfPodsTo()) {
                errors.append("numberOfPodsFrom-numberOfPodsFrom can not be more than numberOfPodsTo;");
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }

        private boolean isDatesInvalid(LocalDate dateFrom, LocalDate dateTo) {
            boolean datesAreNonNull = Objects.nonNull(dateFrom) && Objects.nonNull(dateTo);
            boolean fromDateIsAfterToDate = !datesAreNonNull || dateFrom.isAfter(dateTo);
            return datesAreNonNull && fromDateIsAfterToDate;
        }
    }
}
