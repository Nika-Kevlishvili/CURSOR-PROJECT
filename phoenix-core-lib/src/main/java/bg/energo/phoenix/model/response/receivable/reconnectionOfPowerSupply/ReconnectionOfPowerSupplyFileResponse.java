package bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyFiles;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconnectionOfPowerSupplyFileResponse extends FileWithStatusesResponse {
    private ContractFileType fileType;

    public ReconnectionOfPowerSupplyFileResponse(ReconnectionOfThePowerSupplyFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ReconnectionOfPowerSupplyFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}
