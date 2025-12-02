package bg.energo.phoenix.model.customAnotations.contract.service.edit;

import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractBasicParametersEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceContractTerminationDateValidator.ServiceContractTerminationDateValidatorImpl.class})
public @interface ServiceContractTerminationDateValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractTerminationDateValidatorImpl implements ConstraintValidator<ServiceContractTerminationDateValidator, ServiceContractBasicParametersEditRequest> {

        @Override
        public boolean isValid(ServiceContractBasicParametersEditRequest serviceContractBasicParametersEditRequest, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            if (serviceContractBasicParametersEditRequest.getTerminationDate() != null && serviceContractBasicParametersEditRequest.getTerminationDate().isAfter(LocalDate.now())) {
                context.buildConstraintViolationWithTemplate("basicParameters.terminationDate-Termination date should be less or equal than current day;").addConstraintViolation();
                return false;
            }
            if (serviceContractBasicParametersEditRequest.getTerminationDate() != null && (serviceContractBasicParametersEditRequest.getTerminationDate().isBefore(LocalDate.now())
                    || serviceContractBasicParametersEditRequest.getTerminationDate().isEqual(LocalDate.now()))) {
                if (!(serviceContractBasicParametersEditRequest.getContractStatus() == ServiceContractDetailStatus.TERMINATED)) {
                    context.buildConstraintViolationWithTemplate("basicParameters.contractStatus-Contract Status should be Terminated;").addConstraintViolation();
                    return false;
                }
            }
            if (serviceContractBasicParametersEditRequest.getTerminationDate() == null && serviceContractBasicParametersEditRequest.getContractStatus() == ServiceContractDetailStatus.TERMINATED) {
                context.buildConstraintViolationWithTemplate("basicParameters.terminationDate-Termination date should not be empty;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
