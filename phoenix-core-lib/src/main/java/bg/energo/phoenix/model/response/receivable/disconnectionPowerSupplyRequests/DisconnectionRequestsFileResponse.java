package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsFile;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisconnectionRequestsFileResponse extends FileWithStatusesResponse {
    private ContractFileType fileType;

    public DisconnectionRequestsFileResponse(DisconnectionPowerSupplyRequestsFile file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public DisconnectionRequestsFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}
