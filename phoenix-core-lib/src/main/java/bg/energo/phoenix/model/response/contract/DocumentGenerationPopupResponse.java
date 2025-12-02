package bg.energo.phoenix.model.response.contract;

import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DocumentGenerationPopupResponse {
    private Long templateId;
    private Integer templateVersionId;
    private String templateName;
    private List<ContractTemplateFileFormat> outputFileFormat;
    private List<ContractTemplateSigning> documentSigners;

    public DocumentGenerationPopupResponse(Long templateId, Integer templateVersionId, String templateName, String outputFileFormat, String documentSigners) {
        this.templateId = templateId;
        this.templateVersionId = templateVersionId;
        this.templateName = templateName;
        this.outputFileFormat = EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateFileFormat.class, outputFileFormat);
        this.documentSigners = EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateSigning.class, documentSigners);
    }
}
