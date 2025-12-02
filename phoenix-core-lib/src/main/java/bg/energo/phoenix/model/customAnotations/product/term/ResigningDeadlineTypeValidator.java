package bg.energo.phoenix.model.customAnotations.product.term;

import bg.energo.phoenix.model.request.product.term.terms.BaseTermsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ResigningDeadlineTypeValidator.ResigningDeadlineTypeValidatorImpl.class)
public @interface ResigningDeadlineTypeValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ResigningDeadlineTypeValidatorImpl implements ConstraintValidator<ResigningDeadlineTypeValidator, BaseTermsRequest> {
        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if (request.getResigningDeadlineValue() != null && request.getResigningDeadlineType() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("resigningDeadlineType-Resigning Deadline Type is required;")
                        .addConstraintViolation();
                return false;
            }
            if (request.getResigningDeadlineValue() == null && request.getResigningDeadlineType() != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("resigningDeadlineType-Resigning Deadline Type is not required;")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
