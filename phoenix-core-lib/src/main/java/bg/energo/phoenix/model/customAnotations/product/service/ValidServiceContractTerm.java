package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermType;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.BaseServiceContractTermRequest;
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
@Constraint(validatedBy = ValidServiceContractTerm.ServiceContractTermValidator.class)
public @interface ValidServiceContractTerm {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractTermValidator implements ConstraintValidator<ValidServiceContractTerm, BaseServiceContractTermRequest> {

        @Override
        public boolean isValid(BaseServiceContractTermRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            ServiceContractTermPeriodType periodType = request.getPeriodType();
            if (periodType == null) {
                return false;
            }

            Integer value = request.getValue();
            ServiceContractTermType termType = request.getTermType();

            if (periodType.equals(ServiceContractTermPeriodType.PERIOD)) {
                if (termType == null || value == null) {
                    validationMessage.append("contractTerms.termType-Term type and value should be present when period type is PERIOD;");
                }

                if (Boolean.FALSE.equals(request.getAutomaticRenewal()) && request.getNumberOfRenewals() != null) {
                    validationMessage.append("contractTerms.numberOfRenewals-Number of renewals should not be present when automatic renewal is false;");
                }

                if (Boolean.TRUE.equals(request.getAutomaticRenewal())) {
                    if (request.getRenewalPeriodType() == null) {
                        validationMessage.append("contractTerms.renewalPeriodType-renewal period type must be presented when automatic renewal is true;");
                    }

                    if (request.getRenewalPeriodValue() == null) {
                        validationMessage.append("contractTerms.renewalPeriodValue-renewal period value must be presented when automatic renewal is true;");
                    }
                }

            } else {
                if (termType != null || value != null) {
                    validationMessage.append("contractTerms.termType-Term type and value should not be present when period type is not PERIOD;");
                }

                if (request.getAutomaticRenewal() != null || request.getNumberOfRenewals() != null) {
                    validationMessage.append("contractTerms.automaticRenewal-Automatic renewal information should not be present when period type is not PERIOD;");
                }
            }

            boolean isValid = validationMessage.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }

    }

}
