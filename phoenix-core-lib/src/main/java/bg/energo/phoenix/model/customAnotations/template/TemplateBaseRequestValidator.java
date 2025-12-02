package bg.energo.phoenix.model.customAnotations.template;

import bg.energo.phoenix.model.enums.template.*;
import bg.energo.phoenix.model.request.template.TemplateBaseRequest;
import bg.energo.phoenix.model.request.template.TemplateCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TemplateBaseRequestValidator.TemplateBaseRequestValidatorImpl.class)
public @interface TemplateBaseRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TemplateBaseRequestValidatorImpl implements ConstraintValidator<TemplateBaseRequestValidator, TemplateBaseRequest> {
        Set<ContractTemplatePurposes> PURPOSES_THAT_REQUIRE_VALIDATION = Set.of(
                ContractTemplatePurposes.REQUEST_DISCONNECT_POWER,
                ContractTemplatePurposes.REMINDER_DISCONNECT_POWER,
                ContractTemplatePurposes.CANCEL_DISCONNECTION_POWER,
                ContractTemplatePurposes.RECONNECTION_POWER,
                ContractTemplatePurposes.OBJECTION_CHANGE_COORD,
                ContractTemplatePurposes.OBJECTION_WITHDRAW_CHANGE_COORD,
                ContractTemplatePurposes.REMINDER
        );
        Set<ContractTemplateFileName> FILE_NAMES_THAT_REQUIRE_VALIDATION = Set.of(
                ContractTemplateFileName.FILE_ID,
                ContractTemplateFileName.TIMESTAMP,
                ContractTemplateFileName.DOCUMENT_NUMBER
        );
        
        @Override
        public boolean isValid(TemplateBaseRequest request, ConstraintValidatorContext constraintValidatorContext) {
            StringBuilder validationMessages = new StringBuilder();
            boolean isValid = true;
            validateOutputFileFormat(request, validationMessages);
            validateFileName(request, validationMessages);
            validateSigning(request, validationMessages);
            validateSubject(request, validationMessages);
            validateQuantity(request, validationMessages);
            if (!validationMessages.isEmpty()) {
                constraintValidatorContext.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateQuantity(TemplateBaseRequest request, StringBuilder validationMessages) {
            List<ContractTemplateSigning> fileSignings = request.getFileSignings();
            if (CollectionUtils.isEmpty(fileSignings) || !fileSignings.contains(ContractTemplateSigning.SIGNING_WITH_QUALIFIED_SIGNATURE)) {
                if (Objects.nonNull(request.getQuantity())) {
                    validationMessages.append("templateRequest.quantity-[quantity] Quantity for Electronic Signature must be disabled when 'Signing with qualified signature' is not checked;");
                }
            } else {
                if (Objects.isNull(request.getQuantity())) {
                    validationMessages.append("templateRequest.quantity-[quantity] Quantity for Electronic Signature is mandatory when 'Signing with qualified signature' is checked;");
                }
            }
        }

        private void validateSubject(TemplateBaseRequest request, StringBuilder validationMessages) {
            ContractTemplateType templateType = request.getTemplateType();
            if (!Objects.equals(templateType, ContractTemplateType.EMAIL)) {
                if (Objects.nonNull(request.getSubject())) {
                    validationMessages.append("templateRequest.subject-[subject] Subject must be disabled when Template Type is not Email;");
                }
            } else {
                if (Objects.isNull(request.getSubject())) {
                    validationMessages.append("templateRequest.subject-[subject] Subject is mandatory when Template Type is Email;");
                }
            }
        }

        private void validateSigning(TemplateBaseRequest request, StringBuilder validationMessages) {
            ContractTemplateType templateType = request.getTemplateType();
            List<ContractTemplateFileFormat> fileFormat = request.getOutputFileFormat();
            List<ContractTemplateSigning> fileSignings = request.getFileSignings();
            if (Objects.equals(templateType, ContractTemplateType.DOCUMENT) &&
                !CollectionUtils.isEmpty(fileFormat) && fileFormat.contains(ContractTemplateFileFormat.PDF)) {
                if (CollectionUtils.isEmpty(fileSignings)) {
                    validationMessages.append("templateRequest.fileSignings-[fileSignings] Signing is mandatory when Template Type is Document and Output File Format is PDF;");
                }
            } else if (!CollectionUtils.isEmpty(fileSignings)) {
                validationMessages.append("templateRequest.fileSignings-[fileSignings] Signing must be empty when Template Type is not Document and Output File Format is not PDF;");
            }
        }

        private void validateFileName(TemplateBaseRequest request, StringBuilder validationMessages) {
            ContractTemplateType templateType = request.getTemplateType();
            List<ContractTemplateFileName> fileNames = request.getFileNames();
            if (Objects.nonNull(templateType)) {
                if (Arrays.asList(ContractTemplateType.SMS, ContractTemplateType.EMAIL)
                        .contains(templateType)) {
                    if (!CollectionUtils.isEmpty(fileNames)) {
                        validationMessages.append("templateRequest.fileNames-[fileNames] File Name must be empty when Template Type is Email or SMS;");
                    }
                    if (Objects.nonNull(request.getFileNamePrefix())) {
                        validationMessages.append("templateRequest.fileNamePrefix-[fileNamePrefix] File Name Prefix must be empty when Template Type is Email or SMS;");
                    }
                    if (Objects.nonNull(request.getFileNameSuffix())) {
                        validationMessages.append("templateRequest.fileNameSuffix-[fileNameSuffix] File Name Suffix must be empty when Template Type is Email or SMS;");
                    }
                } else if (CollectionUtils.isEmpty(fileNames)) {
                    validationMessages.append("templateRequest.fileNames-[fileNames] File Name is mandatory when Template Type is Document;");
                } else if (request instanceof TemplateCreateRequest createRequest) {
                    validatePurposeAndFilenames(validationMessages, createRequest, fileNames);
                }
            }
        }

        private void validatePurposeAndFilenames(StringBuilder validationMessages, TemplateCreateRequest createRequest, List<ContractTemplateFileName> fileNames) {
            if (PURPOSES_THAT_REQUIRE_VALIDATION.contains(createRequest.getTemplatePurpose())) {
                if (fileNames.stream().anyMatch(this::isInvalidFileName)) {
                    validationMessages.append(String.format("templateRequest.fileNames-[fileNames] Invalid value in fileNames for purpose '%s'. Allowed fileNames: %s;", createRequest.getTemplatePurpose(), FILE_NAMES_THAT_REQUIRE_VALIDATION));
                }
            }
        }

        private boolean isInvalidFileName(ContractTemplateFileName fileName) {
            return !FILE_NAMES_THAT_REQUIRE_VALIDATION.contains(fileName);
        }

        private void validateOutputFileFormat(TemplateBaseRequest request, StringBuilder validationMessages) {
            List<ContractTemplateFileFormat> fileFormat = request.getOutputFileFormat();
            ContractTemplateType templateType = request.getTemplateType();
            if (Objects.equals(templateType, ContractTemplateType.DOCUMENT)) {
                if (CollectionUtils.isEmpty(fileFormat)) {
                    validationMessages.append("templateRequest.outputFileFormat[outputFileFormat] File format is mandatory when Template Type is Document;");
                } else if (fileFormat.contains(ContractTemplateFileFormat.XLSX) && fileFormat.size() > 1) {
                    validationMessages.append("templateRequest.outputFileFormat[outputFileFormat] It is not possible to choose another format with XLSX;");
                }
            } else {
                if (!CollectionUtils.isEmpty(fileFormat)) {
                    validationMessages.append("templateRequest.outputFileFormat[outputFileFormat] File format must not be present when Template Type is Email or SMS;");
                }
            }
        }

    }
}
