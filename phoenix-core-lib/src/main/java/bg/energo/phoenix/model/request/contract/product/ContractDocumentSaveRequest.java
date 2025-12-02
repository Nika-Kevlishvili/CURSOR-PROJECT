package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.ContractDocumentSaveRequestValidator;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.service.document.enums.FileFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
public class ContractDocumentSaveRequest {
    @NotNull(message = "contractDocumentSaveRequest.contractId-[contractId] is mandatory;")
    private Long contractId;
    @NotNull(message = "contractDocumentSaveRequest.versionId-[versionId] is mandatory;")
    private Long versionId;
    private List<@Valid DocumentSaveRequestModels> documents;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ContractDocumentSaveRequestValidator
    public static class DocumentSaveRequestModels {
        @NotNull(message = "templateId-[templateId] is mandatory;")
        private Long templateId;
        private List<ContractTemplateSigning> signings;
        @Size(min = 1, message = "outputFileFormat-[outputFileFormat] must not be empty;")
        private List<FileFormat> outputFileFormat;
        private boolean deletePreviousFiles;
    }
}
