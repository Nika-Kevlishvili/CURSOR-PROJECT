package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PowerSupplyDcnReminderDocFileResponse extends FileWithStatusesResponse {
    private ContractFileType fileType;


    public PowerSupplyDcnReminderDocFileResponse(
            PowerSupplyDcnReminderDocFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public PowerSupplyDcnReminderDocFileResponse(
            Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}