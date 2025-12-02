package bg.energo.phoenix.model.customAnotations.nomenclature.systemMessages;

import bg.energo.common.utils.StringUtils;
import bg.energo.phoenix.model.request.systemMessage.CreateSystemMessageRequest;
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
@Constraint(validatedBy = CreateSystemMessageRequestValidator.CreateSystemMessageRequestImpl.class)
public @interface CreateSystemMessageRequestValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CreateSystemMessageRequestImpl implements ConstraintValidator<CreateSystemMessageRequestValidator, CreateSystemMessageRequest> {
        @Override
        public boolean isValid(CreateSystemMessageRequest request, ConstraintValidatorContext context) {

            boolean status = true;
            List<String> errorMessage = new ArrayList<>();

            if (!StringUtils.isNullOrEmpty(request.getOkButtonText())) {
                if (StringUtils.isNullOrEmpty(request.getOkButtonTextTransliterated())) {
                    status = false;
                    errorMessage.add("okButtonTextTransliterated-[okButtonTextTransliterated] should be present when OkButtonText is not null or empty;");
                }
            }

            if (!StringUtils.isNullOrEmpty(request.getNoButtonText())) {
                if (StringUtils.isNullOrEmpty(request.getNoButtonTextTransliterated())) {
                    status = false;
                    errorMessage.add("noButtonTextTransliterated-[noButtonTextTransliterated] should be present when NoButtonText is not null or empty;");
                }
            }

            if (!StringUtils.isNullOrEmpty(request.getCancelButtonText())) {
                if (StringUtils.isNullOrEmpty(request.getCancelButtonTextTransliterated())) {
                    status = false;
                    errorMessage.add("cancelButtonTextTransliterated-[cancelButtonTextTransliterated] should be present when CancelButtonText is not null or empty;");
                }
            }

            if (!status) {
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
