package bg.energo.phoenix.util.template;

import bg.energo.phoenix.model.enums.template.*;
import bg.energo.phoenix.model.request.template.TemplateBaseRequest;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TemplateRequestValidatorUtil {

    public static void validateRequest(ContractTemplatePurposes templatePurpose, TemplateBaseRequest request, List<String> validationMessages) {
        if (Objects.nonNull(templatePurpose)) {
            validateType(templatePurpose, request.getTemplateType(), validationMessages);
            validateLanguage(templatePurpose, request.getLanguage(), validationMessages);
            validateOutputFileFormat(templatePurpose, request.getOutputFileFormat(), request.getTemplateType(), validationMessages);
            validateSigning(templatePurpose, request.getTemplateType(), request.getOutputFileFormat(), request.getFileSignings(), validationMessages);
            validateCheckboxes(templatePurpose, request, validationMessages);
        }
    }

    private static void validateType(ContractTemplatePurposes templatePurpose, ContractTemplateType templateType, List<String> validationMessages) {
        switch (templatePurpose) {
            case TERMINATION -> {
                if (!Objects.equals(templateType, ContractTemplateType.EMAIL)) {
                    validationMessages.add("templateRequest.templateType[templateType] type must be Email when Template Purpose is TERMINATION;");
                }
            }
            case SERVICE, PRODUCT, PRODUCT_ADDITIONAL_TEMPLATE_ADVANCE,
                    PRODUCT_ADDITIONAL_TEMPLATE_BASE,
                    SERVICE_ADDITIONAL_TEMPLATE_BASE,
                    SERVICE_ADDITIONAL_TEMPLATE_ADVANCE,
                    ADDITIONAL_AGREEMENT,
                    REQUEST_DISCONNECT_POWER,
                    CANCEL_DISCONNECTION_POWER, RECONNECTION_POWER, RESCHEDULING -> {
                if (!Objects.equals(templateType, ContractTemplateType.DOCUMENT)) {
                    validationMessages.add("templateRequest.templateType[templateType] type must be Document when Template Purpose is %s;".formatted(templatePurpose));
                }
            }
            case OBJECTION_WITHDRAW_CHANGE_COORD, OBJECTION_CHANGE_COORD, MANUAL_LIABILITY_OFFSET, INVOICE_CANCEL,
                    DEPOSIT, EMAIL, INVOICE, PENALTY, LATE_PAYMENT_FINE -> {
                if (!Arrays.asList(ContractTemplateType.DOCUMENT, ContractTemplateType.EMAIL).contains(templateType)) {
                    validationMessages.add("templateRequest.templateType[templateType] type must be Document or Email when Template Purpose is %s;".formatted(templatePurpose));
                }
            }
            case SMS -> {
                if (!Objects.equals(ContractTemplateType.SMS, templateType)) {
                    validationMessages.add("templateRequest.templateType[templateType] type must be SMS when Template Purpose is %s;".formatted(templatePurpose));
                }
            }
        }
    }

    private static void validateLanguage(ContractTemplatePurposes templatePurpose, ContractTemplateLanguage language, List<String> validationMessages) {
        if (!Arrays.asList(ContractTemplatePurposes.SERVICE, ContractTemplatePurposes.PRODUCT)
                .contains(templatePurpose) &&
            !Objects.equals(ContractTemplateLanguage.BULGARIAN, language)) {
            validationMessages.add("templateRequest.language [language] Language must be Bulgarian when Template Purpose is %s;".formatted(templatePurpose));
        }
    }

    private static void validateOutputFileFormat(ContractTemplatePurposes templatePurpose, List<ContractTemplateFileFormat> outputFileFormat, ContractTemplateType templateType, List<String> validationMessages) {
        if (Objects.nonNull(templatePurpose) && !CollectionUtils.isEmpty(outputFileFormat)) {
            if (Arrays.asList(ContractTemplatePurposes.PENALTY, ContractTemplatePurposes.INVOICE_CANCEL,
                    ContractTemplatePurposes.INVOICE,
                    ContractTemplatePurposes.REMINDER,
                    ContractTemplatePurposes.LATE_PAYMENT_FINE,
                    ContractTemplatePurposes.REMINDER_DISCONNECT_POWER,
                    ContractTemplatePurposes.MANUAL_LIABILITY_OFFSET,
                    ContractTemplatePurposes.DEPOSIT,
                    ContractTemplatePurposes.PENALTY).contains(templatePurpose) &&
                (outputFileFormat.size() > 1 || !outputFileFormat.contains(ContractTemplateFileFormat.PDF))) {
                validationMessages.add("templateRequest.outputFileFormat[outputFileFormat] File format must be only PDF when Template Purpose is %s;".formatted(templatePurpose));
            } else if (templatePurpose.equals(ContractTemplatePurposes.RESCHEDULING) &&
                       outputFileFormat.contains(ContractTemplateFileFormat.XLSX)) {
                validationMessages.add("templateRequest.outputFileFormat[outputFileFormat] File format can be PDF or DOCX when Template Purpose is %s;".formatted(templatePurpose));
            } else if (Arrays.asList(ContractTemplatePurposes.REQUEST_DISCONNECT_POWER,
                    ContractTemplatePurposes.CANCEL_DISCONNECTION_POWER,
                    ContractTemplatePurposes.RECONNECTION_POWER).contains(templatePurpose) &&
                       (outputFileFormat.size() > 1)) {
                validationMessages.add("templateRequest.outputFileFormat[outputFileFormat] File format must be single selected when Template Purpose is %s;".formatted(templatePurpose));
            }
        }
    }

    private static void validateSigning(ContractTemplatePurposes templatePurpose, ContractTemplateType templateType, List<ContractTemplateFileFormat> outputFileFormat, List<ContractTemplateSigning> fileSignings, List<String> validationMessages) {
        if (Objects.equals(templateType, ContractTemplateType.DOCUMENT) &&
            !CollectionUtils.isEmpty(outputFileFormat) && outputFileFormat.contains(ContractTemplateFileFormat.PDF) &&
            (!CollectionUtils.isEmpty(fileSignings))) {
            if (List.of(ContractTemplatePurposes.INVOICE,
                    ContractTemplatePurposes.ADDITIONAL_AGREEMENT,
                    ContractTemplatePurposes.INVOICE_CANCEL,
                    ContractTemplatePurposes.PENALTY,
                    ContractTemplatePurposes.LATE_PAYMENT_FINE,
                    ContractTemplatePurposes.REQUEST_DISCONNECT_POWER,
                    ContractTemplatePurposes.CANCEL_DISCONNECTION_POWER,
                    ContractTemplatePurposes.RECONNECTION_POWER).contains(templatePurpose) &&
                fileSignings.size() > 1) {
                validationMessages.add("templateRequest.fileSignings-[fileSignings] Signing must be single selected when template purpose is %s".formatted(templatePurpose));
            }
            switch (templatePurpose) {
                case PENALTY, INVOICE_CANCEL,
                        OBJECTION_WITHDRAW_CHANGE_COORD, OBJECTION_CHANGE_COORD,
                        MANUAL_LIABILITY_OFFSET, DEPOSIT, EMAIL, INVOICE,
                        LATE_PAYMENT_FINE -> {
                    if (fileSignings.contains(ContractTemplateSigning.SIGNING_WITH_QUALIFIED_SIGNATURE) ||
                        fileSignings.contains(ContractTemplateSigning.SIGNING_WITH_TABLET)) {
                        validationMessages.add("templateRequest.fileSignings-[fileSignings] Signing must be either No and/or System Certificate when Template Purpose is %s;".formatted(templatePurpose));
                    }
                }
                case CANCEL_DISCONNECTION_POWER, RECONNECTION_POWER, REQUEST_DISCONNECT_POWER -> {
                    if (!fileSignings.contains(ContractTemplateSigning.NO)) {
                        validationMessages.add("templateRequest.fileSignings-[fileSignings] Signing must be only Manual or No signing option when Template Purpose is %s;".formatted(templatePurpose));
                    }
                }
                case REMINDER, REMINDER_DISCONNECT_POWER -> {
                    if (!fileSignings.contains(ContractTemplateSigning.NO) || fileSignings.size() > 1) {
                        validationMessages.add("templateRequest.fileSignings-[fileSignings] Signing can be only No when Template Purpose is %s;".formatted(templatePurpose));
                    }
                }
            }
        }
    }

    private static void validateCheckboxes(ContractTemplatePurposes templatePurpose, TemplateBaseRequest request, List<String> validationMessages) {

        ContractTemplateType templateType = request.getTemplateType();

        if ((!Objects.equals(templatePurpose, ContractTemplatePurposes.INVOICE) ||
             (!Objects.equals(templateType, ContractTemplateType.DOCUMENT))
            ) && Objects.equals(request.getDefaultGoodsOrderDocument(), Boolean.TRUE)) {
            validationMessages.add("templateRequest.defaultGoodsOrderDocument-[defaultGoodsOrderDocument] Default goods order document template must not be provided;");
        }

        if ((!Objects.equals(templatePurpose, ContractTemplatePurposes.INVOICE) ||
             (!Objects.equals(templateType, ContractTemplateType.EMAIL))
            ) && Objects.equals(request.getDefaultGoodsOrderEmail(), Boolean.TRUE)) {
            validationMessages.add("templateRequest.defaultGoodsOrderEmail-[defaultGoodsOrderEmail] Default goods order Email template must not be provided;");
        }

        if ((!Objects.equals(templatePurpose, ContractTemplatePurposes.LATE_PAYMENT_FINE) ||
             (!Objects.equals(templateType, ContractTemplateType.DOCUMENT))
            ) && Objects.equals(request.getDefaultLatePaymentFineDocument(), Boolean.TRUE)) {
            validationMessages.add("templateRequest.defaultLatePaymentFineDocument-[defaultLatePaymentFineDocument] Default Late payment fine document template must not be provided;");
        }

        if ((!Objects.equals(templatePurpose, ContractTemplatePurposes.LATE_PAYMENT_FINE) ||
             (!Objects.equals(templateType, ContractTemplateType.EMAIL))
            ) && Objects.equals(request.getDefaultLatePaymentFineEmail(), Boolean.TRUE)) {
            validationMessages.add("templateRequest.defaultLatePaymentFineEmail-[defaultLatePaymentFineEmail] Default Late payment fine Email template must not be provided;");
        }
    }
}
