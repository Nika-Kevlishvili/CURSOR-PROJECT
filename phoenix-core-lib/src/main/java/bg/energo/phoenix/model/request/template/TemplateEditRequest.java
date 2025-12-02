package bg.energo.phoenix.model.request.template;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class TemplateEditRequest extends TemplateBaseRequest {

    private boolean saveAsNewVersion;

    private LocalDate startDate;

    public TemplateEditRequest(String name,
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
                               Boolean setDefault,
                               boolean saveAsNewVersion,
                               LocalDate startDate) {
        super(name, templateType, subject, language, customerTypes, consumptionPurposes, outputFileFormat, fileNames,
                fileNamePrefix, fileNameSuffix, fileSignings, quantity, fileId, templateStatus,
                defaultForGoodsOrderDocument, defaultForGoodsOrderEmail, defaultForLatePaymentFineDocument,
                defaultForLatePaymentFineEmail, setDefault);
        this.saveAsNewVersion = saveAsNewVersion;
        this.startDate = startDate;
    }

    @JsonIgnore
    @AssertTrue(message = "templateRequest.startDate-[startDate] Start date must be provided;")
    public boolean isStartDateValid() {
        return !saveAsNewVersion || Objects.nonNull(startDate);
    }

}
