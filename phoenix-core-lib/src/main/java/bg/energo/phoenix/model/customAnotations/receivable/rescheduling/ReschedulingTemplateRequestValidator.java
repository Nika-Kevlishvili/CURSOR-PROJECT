package bg.energo.phoenix.model.customAnotations.receivable.rescheduling;

import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingTemplateRequest;
import bg.energo.phoenix.service.document.enums.FileFormat;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;


@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ReschedulingTemplateRequestValidator.ReschedulingTemplateRequestValidatorImpl.class})
public @interface ReschedulingTemplateRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ReschedulingTemplateRequestValidatorImpl implements ConstraintValidator<ReschedulingTemplateRequestValidator, ReschedulingTemplateRequest> {
        @Override
        public boolean isValid(ReschedulingTemplateRequest document, ConstraintValidatorContext constraintValidatorContext) {
            StringBuilder validationMessages = new StringBuilder();
            if (CollectionUtils.isNotEmpty(document.getOutputFileFormat())) {
                if (document.getOutputFileFormat().contains(FileFormat.PDF) && CollectionUtils.isEmpty(document.getSignings())) {
                    validationMessages.append("signings-[signings] must be provided when output file format is PDF;");
                } else if (!document.getOutputFileFormat().contains(FileFormat.PDF) && CollectionUtils.isNotEmpty(document.getSignings())) {
                    validationMessages.append("signings-[signings] must not be provided when output file format is not PDF;");
                }
            }

            if (!validationMessages.isEmpty()) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
