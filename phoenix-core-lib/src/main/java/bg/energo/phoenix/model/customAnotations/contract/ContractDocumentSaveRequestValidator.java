package bg.energo.phoenix.model.customAnotations.contract;

import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.contract.product.ContractDocumentSaveRequest;
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
@Constraint(validatedBy = {ContractDocumentSaveRequestValidator.ContractDocumentSaveRequestValidatorImpl.class})
public @interface ContractDocumentSaveRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContractDocumentSaveRequestValidatorImpl implements ConstraintValidator<ContractDocumentSaveRequestValidator, ContractDocumentSaveRequest.DocumentSaveRequestModels> {
        @Override
        public boolean isValid(ContractDocumentSaveRequest.DocumentSaveRequestModels document, ConstraintValidatorContext constraintValidatorContext) {
            StringBuilder validationMessages = new StringBuilder();
            if (CollectionUtils.isNotEmpty(document.getOutputFileFormat())) {
                if (document.getOutputFileFormat().contains(FileFormat.PDF)) {
                    if (CollectionUtils.isEmpty(document.getSignings())) {
                        validationMessages.append("signings-[signings] must be provided when output file format is PDF;");
                    } else if (document.getSignings().contains(ContractTemplateSigning.NO) &&
                               document.getSignings().size() > 1) {
                        validationMessages.append("signings-[signings] other signings must not be provided when 'Manual or no signing' is selected;");
                    }
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
