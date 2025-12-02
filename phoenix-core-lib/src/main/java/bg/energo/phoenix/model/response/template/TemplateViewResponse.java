package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TemplateViewResponse {
    public TemplateViewResponse(ContractTemplateDetail detail, ContractTemplate template) {
        this.id = detail.getTemplateId();
        this.versionId = detail.getVersion();
        this.purpose = template.getTemplatePurpose();
        this.startDate = detail.getStartDate();
        this.name = detail.getName();
        this.language = detail.getLanguage();
        this.customerTypes = detail.getCustomerType();
        this.purposeOfConsumptions = detail.getConsumptionPurposes();
        this.type = detail.getTemplateType();
        this.subject = detail.getSubject();
        this.outputFileFormats = detail.getOutputFileFormat();
        this.fileNameSuffix = detail.getFileNameSuffix();
        this.fileNamePrefix = detail.getFileNamePrefix();
        this.fileName = detail.getFileName();
        this.fileSigning = detail.getFileSigning();
        this.quantity = detail.getQuantity();
        this.defaultGoodsOrderDocument = template.getDefaultForGoodsOrderDocument();
        this.defaultGoodsOrderEmail = template.getDefaultForGoodsOrderEmail();
        this.defaultLatePaymentFineDocument = template.getDefaultForLatePaymentFineDocument();
        this.defaultLatePaymentFineEmail = template.getDefaultForLatePaymentFineEmail();
        this.status = template.getStatus();
    }

    private Long id;
    private Integer versionId;
    private ContractTemplatePurposes purpose;
    private LocalDate startDate;
    private String name;
    private ContractTemplateLanguage language;
    private List<CustomerType> customerTypes;
    private List<PurposeOfConsumption> purposeOfConsumptions;
    private ContractTemplateType type;
    private String subject;
    private List<ContractTemplateFileFormat> outputFileFormats;
    private ContractTemplateSuffix fileNameSuffix;
    private String fileNamePrefix;
    private List<ContractTemplateFileName> fileName;
    private List<ContractTemplateSigning> fileSigning;
    private Integer quantity;
    private TemplateFileResponse file;
    private Boolean defaultGoodsOrderDocument;
    private Boolean defaultGoodsOrderEmail;
    private Boolean defaultLatePaymentFineDocument;
    private Boolean defaultLatePaymentFineEmail;
    private ContractTemplateStatus status;
    private List<TemplateVersionResponse> versions;

}
