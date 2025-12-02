package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
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
@Constraint(validatedBy = {ProductTermsValidator.ProductTermsValidatorImpl.class})
public @interface ProductTermsValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductTermsValidatorImpl implements ConstraintValidator<ProductTermsValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessageBuilder = new StringBuilder();

            Long termId = request.getTermId();
            Long termGroupId = request.getTermGroupId();

            if (termId == null && termGroupId == null) {
                validationMessageBuilder.append("basicSettings.termIds-[Term Group ID] or [Term ID] must be defined;");
                validationMessageBuilder.append("basicSettings.termGroupIds-[Term Group ID] or [Term ID] must be defined;");
            }

            if (termId != null && termGroupId != null) {
                validationMessageBuilder.append("basicSettings.termIds-Either [Term Group IDs] or [Term IDs] must be defined;");
                validationMessageBuilder.append("basicSettings.termGroupIds-Either [Term Group IDs] or [Term IDs] must be defined;");
            }

            if (validationMessageBuilder.length() > 0) {
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
