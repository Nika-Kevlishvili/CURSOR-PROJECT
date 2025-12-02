package bg.energo.phoenix.model.customAnotations.pod.pod;

import bg.energo.phoenix.model.request.pod.pod.PodBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PodBlockingValidator.PodBlockingValidatorImpl.class})
public @interface PodBlockingValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodBlockingValidatorImpl implements ConstraintValidator<PodBlockingValidator, PodBaseRequest> {
        @Override
        public boolean isValid(PodBaseRequest podBaseRequest, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            StringBuilder violationBuilder = new StringBuilder();
            if (podBaseRequest.isBlockedDisconnection()) {
                if (Objects.isNull(podBaseRequest.getBlockedDisconnectionRequest())) {
                    violationBuilder.append("blockedDisconnectionRequest-Blocked disconnection request can not be null;");
                } else {
                    if (Objects.isNull(podBaseRequest.getBlockedDisconnectionRequest().getFrom())) {
                        violationBuilder.append("blockedDisconnectionRequest.from-Block for disconnection from date must not be null;");
                    }
                }
            } else {
                if (Objects.nonNull(podBaseRequest.getBlockedDisconnectionRequest())) {
                    violationBuilder.append("blockedDisconnectionRequest-Blocked disconnection request must be null while is blocked disconnection field is checked;");
                }
            }

            if (podBaseRequest.isBlockedBilling()) {
                if (Objects.isNull(podBaseRequest.getBlockedBillingRequest())) {
                    violationBuilder.append("blockedBillingRequest-Blocked billing request can not be null;");
                } else {
                    if (Objects.isNull(podBaseRequest.getBlockedBillingRequest().getFrom())) {
                        violationBuilder.append("blockedBillingRequest.from-Block for billing from date must not be null;");
                    }
                }
            } else {
                if (Objects.nonNull(podBaseRequest.getBlockedBillingRequest())) {
                    violationBuilder.append("blockedBillingRequest-Blocked billing request must be null while is blocked billing field is checked;");
                }
            }

            if (!violationBuilder.isEmpty()) {
                context.buildConstraintViolationWithTemplate(violationBuilder.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
