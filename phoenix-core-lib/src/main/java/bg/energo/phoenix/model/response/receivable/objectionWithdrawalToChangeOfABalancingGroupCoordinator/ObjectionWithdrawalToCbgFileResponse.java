package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgFiles;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectionWithdrawalToCbgFileResponse extends FileWithStatusesResponse {
    private ContractFileType fileType;

    public ObjectionWithdrawalToCbgFileResponse(ObjectionWithdrawalToChangeOfCbgFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ObjectionWithdrawalToCbgFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }
}
