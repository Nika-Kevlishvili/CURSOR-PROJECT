package bg.energo.phoenix.model.request.product.product;


import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateSubObjectRequest {
    private Long templateId;
    private ProductServiceTemplateType templateType;
}
