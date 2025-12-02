package bg.energo.phoenix.model.customAnotations.template;

import bg.energo.phoenix.model.request.template.TemplateCreateRequest;
import bg.energo.phoenix.util.template.TemplateRequestValidatorUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TemplateCreateRequestValidator.TemplateCreateRequestValidatorImpl.class)
public @interface TemplateCreateRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TemplateCreateRequestValidatorImpl implements ConstraintValidator<TemplateCreateRequestValidator, TemplateCreateRequest> {
        @Override
        public boolean isValid(TemplateCreateRequest request, ConstraintValidatorContext constraintValidatorContext) {
            List<String> validationMessages = new ArrayList<>();
            TemplateRequestValidatorUtil.validateRequest(request.getTemplatePurpose(), request, validationMessages);
            if (!validationMessages.isEmpty()) {
                constraintValidatorContext.buildConstraintViolationWithTemplate(String.join("", validationMessages)).addConstraintViolation();
                return false;
            }
            return true;
        }

    }
}
