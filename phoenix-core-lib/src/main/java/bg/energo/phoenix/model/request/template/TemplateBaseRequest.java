package bg.energo.phoenix.model.request.template;

import bg.energo.phoenix.model.customAnotations.template.TemplateBaseRequestValidator;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TemplateBaseRequestValidator
public abstract class TemplateBaseRequest {

    @NotNull(message = "templateRequest.name-[name] Name is mandatory;")
    @Size(min = 1, max = 1024)
    private String name;

    @NotNull(message = "templateRequest.templateType-[templateType] Template Type is mandatory;")
    private ContractTemplateType templateType;

    @Size(min = 1, max = 2048, message = "templateRequest.subject-[subject] Subject length must be between {min} and {max};")
    private String subject;

    @NotNull(message = "templateRequest.language-[language] Language is mandatory;")
    private ContractTemplateLanguage language;

    private List<CustomerType> customerTypes;

    private List<PurposeOfConsumption> consumptionPurposes;

    private List<ContractTemplateFileFormat> outputFileFormat;

    private List<ContractTemplateFileName> fileNames;

    @Size(min = 1, max = 64)
    private String fileNamePrefix;

    private ContractTemplateSuffix fileNameSuffix;

    private List<ContractTemplateSigning> fileSignings;

    @Range(min = 1, max = 99, message = "templateRequest.quantity-[quantity] Quantity for Electronic Signature must be between {min} and {max};")
    private Integer quantity;

    @NotNull(message = "templateRequest.fileId-[fileId] fileId is mandatory;")
    private Long fileId;

    @NotNull(message = "templateRequest.templateStatus-[templateStatus] template status is mandatory;")
    private ContractTemplateStatus templateStatus;

    private Boolean defaultGoodsOrderDocument;

    private Boolean defaultGoodsOrderEmail;

    private Boolean defaultLatePaymentFineDocument;

    private Boolean defaultLatePaymentFineEmail;

    private Boolean setDefault;


}
