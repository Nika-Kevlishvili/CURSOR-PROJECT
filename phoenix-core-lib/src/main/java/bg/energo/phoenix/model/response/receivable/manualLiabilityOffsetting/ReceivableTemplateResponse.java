package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReceivableTemplateResponse  extends ContractTemplateShortResponse {
   private ReceivableTemplateType templateType;

    public ReceivableTemplateResponse(Long id, Long detailId, String name, Long fileId, String fileName, ReceivableTemplateType templateType) {
        super(id, detailId, name, fileId, fileName);
        this.templateType = templateType;
    }

    public ReceivableTemplateResponse(Long id, Long detailId, String name, Long fileId, String fileName, ReceivableTemplateType templateType,
                                      String outputFileFormat, String fileSigning) {
        super(id, detailId, name, fileId, fileName, outputFileFormat, fileSigning);
        this.templateType = templateType;
    }
}
