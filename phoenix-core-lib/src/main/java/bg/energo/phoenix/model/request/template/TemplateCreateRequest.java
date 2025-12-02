package bg.energo.phoenix.model.request.template;

import bg.energo.phoenix.model.customAnotations.template.TemplateCreateRequestValidator;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@TemplateCreateRequestValidator
@EqualsAndHashCode(callSuper = false)
public class TemplateCreateRequest extends TemplateBaseRequest {

    @NotNull(message = "templateRequest.templatePurpose-[templatePurpose] Template Purpose is mandatory;")
    private ContractTemplatePurposes templatePurpose;

    public TemplateCreateRequest(String name,
                                 ContractTemplatePurposes templatePurpose,
                                 ContractTemplateType templateType,
                                 String subject,
                                 ContractTemplateLanguage language,
                                 List<CustomerType> customerTypes,
                                 List<PurposeOfConsumption> consumptionPurposes,
                                 List<ContractTemplateFileFormat> outputFileFormat,
                                 List<ContractTemplateFileName> fileNames,
                                 String fileNamePrefix,
                                 ContractTemplateSuffix fileNameSuffix,
                                 List<ContractTemplateSigning> fileSignings,
                                 Integer quantity,
                                 Long fileId,
                                 ContractTemplateStatus templateStatus,
                                 Boolean defaultForGoodsOrderDocument,
                                 Boolean defaultForGoodsOrderEmail,
                                 Boolean defaultForLatePaymentFineDocument,
                                 Boolean defaultForLatePaymentFineEmail,
                                 Boolean setDefault) {
        super(name, templateType, subject, language, customerTypes, consumptionPurposes, outputFileFormat, fileNames,
                fileNamePrefix, fileNameSuffix, fileSignings, quantity, fileId, templateStatus,
                defaultForGoodsOrderDocument, defaultForGoodsOrderEmail, defaultForLatePaymentFineDocument,
                defaultForLatePaymentFineEmail, setDefault);
        this.templatePurpose = templatePurpose;
    }

}
