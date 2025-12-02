package bg.energo.phoenix.model.request.receivable.rescheduling;

import bg.energo.phoenix.model.customAnotations.receivable.rescheduling.ReschedulingTemplateRequestValidator;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.service.document.enums.FileFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ReschedulingTemplateRequestValidator
public class ReschedulingTemplateRequest {

    @NotNull(message = "templateId-[templateId] is mandatory;")
    private Long templateId;
    private List<ContractTemplateSigning> signings;
    @Size(min = 1, message = "outputFileFormat-[outputFileFormat] must not be empty;")
    private List<FileFormat> outputFileFormat;
}
