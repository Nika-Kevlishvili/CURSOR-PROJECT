package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContractTemplateShortResponse {

    private Long id;
    private Long detailId;
    private String name;
    private String displayName;
    private Long fileId;
    private String fileName;
    private List<ContractTemplateFileFormat> outputFileFormat;
    private List<ContractTemplateSigning> fileSigning;

    public ContractTemplateShortResponse(Long id, Long detailId, String name,Long fileId,String fileName) {
        this.id = id;
        this.detailId = detailId;
        this.name = name;
        this.displayName = String.format("%s (%s)", name, id);
        this.fileId=fileId;
        this.fileName=fileName;
    }

    public ContractTemplateShortResponse(Long id, Long detailId, String name, Long fileId, String fileName,
                                         String outputFileFormat,
                                         String fileSigning) {
        this.id = id;
        this.detailId = detailId;
        this.name = name;
        this.displayName = String.format("%s (%s)", name, id);
        this.fileId = fileId;
        this.fileName = fileName;
        this.outputFileFormat = EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateFileFormat.class, outputFileFormat);
        this.fileSigning = EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateSigning.class, fileSigning);
    }
}
