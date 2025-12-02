package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgSubFiles;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectionToCbgFileResponse extends FileWithStatusesResponse {
    private ContractFileType fileType;

    public ObjectionToCbgFileResponse(ObjectionToChangeOfCbgSubFiles file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.UPLOADED_FILE;
    }

    public ObjectionToCbgFileResponse(Document file, String fileInfo) {
        super(file, fileInfo);
        this.fileType = ContractFileType.GENERATED_DOCUMENT;
    }

}
