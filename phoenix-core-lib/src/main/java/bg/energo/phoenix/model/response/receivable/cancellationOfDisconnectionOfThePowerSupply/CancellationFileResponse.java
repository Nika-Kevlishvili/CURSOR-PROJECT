package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFiles;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellationFileResponse extends FileWithStatusesResponse {

    private ContractFileType fileType;

    public CancellationFileResponse(PowerSupplyDcnCancellationFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public CancellationFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}
