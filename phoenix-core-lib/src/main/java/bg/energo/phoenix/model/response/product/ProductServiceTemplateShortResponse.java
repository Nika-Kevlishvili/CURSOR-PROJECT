package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ProductServiceTemplateShortResponse extends ContractTemplateShortResponse {

    private ProductServiceTemplateType templateType;

    public ProductServiceTemplateShortResponse(Long templateId, Long detailId, String name, Long fileId, String fileName, ProductServiceTemplateType templateType) {
        super(templateId, detailId, name, fileId, fileName);
        this.templateType = templateType;
    }

    public ProductServiceTemplateShortResponse(Long id, Long detailId, String name, Long fileId, String fileName,
                                               ProductServiceTemplateType type,
                                               String outputFileFormat,
                                               String fileSigning) {
        super(id, detailId, name, fileId, fileName, outputFileFormat, fileSigning);
        this.templateType = type;
    }
}
