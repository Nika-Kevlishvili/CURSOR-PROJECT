package bg.energo.phoenix.model.response.contract;

import bg.energo.phoenix.model.entity.contract.action.ActionFile;
import bg.energo.phoenix.model.entity.contract.product.ProductContractFile;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractFiles;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContractFileResponse extends FileWithStatusesResponse {

    private ContractFileType fileType;
    private String status;

    public ContractFileResponse(ProductContractFile file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ContractFileResponse(ServiceContractFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ContractFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }

    public ContractFileResponse(ActionFile file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ContractFileResponse(DocumentStatus status, Document document, boolean notSigned) {
        super(status.toString(), document, notSigned);
        this.status = status.toString();
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}
