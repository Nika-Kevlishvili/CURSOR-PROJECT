package bg.energo.phoenix.model.response.shared;

import bg.energo.phoenix.model.entity.activity.SystemActivityFile;
import bg.energo.phoenix.model.entity.contract.action.ActionFile;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDocument;
import bg.energo.phoenix.model.entity.contract.product.ProductContractFile;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalDocuments;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractFiles;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationFile;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationFiles;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.product.product.ProductFile;
import bg.energo.phoenix.model.entity.product.service.ServiceFile;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgSubFiles;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFiles;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentFiles;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositDocumentFile;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsFile;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgFiles;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyFiles;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingFiles;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.service.document.enums.FileFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@NoArgsConstructor
public class FileWithStatusesResponse {
    private Long id;
    private String fileName;
    private String displayName;
    private String fileInfo;

    public FileWithStatusesResponse(PowerSupplyDcnCancellationFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ReconnectionOfThePowerSupplyFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(DisconnectionPowerSupplyRequestsFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(SmsCommunicationFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(EmailCommunicationFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ObjectionWithdrawalToChangeOfCbgFiles file, String fileInfo) {
        this.id = file.getId();
        String name = file.getName();
        int extensionPartIndex = name.lastIndexOf('.');

        String statusesPart = createStatusesPart(file.getFileStatuses());
        this.fileName = createFileName(name, statusesPart, extensionPartIndex);
        this.displayName = name.concat(" ").concat(statusesPart);
        this.fileInfo = createFileInfo(file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ObjectionToChangeOfCbgSubFiles file, String fileInfo) {
        this.id = file.getId();
        String name = file.getName();
        int extensionPartIndex = name.lastIndexOf('.');

        String statusesPart = createStatusesPart(file.getFileStatuses());
        this.fileName = createFileName(name, statusesPart, extensionPartIndex);
        this.displayName = name.concat(" ").concat(statusesPart);
        this.fileInfo = createFileInfo(file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(CustomerAssessmentFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ProductFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ServiceFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(SystemActivityFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ActionFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ProductContractDocument file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ProductContractFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(Document file, String fileInfo) {
        this.id = file.getId();
        String name = file.getName();
        int extensionPartIndex = name.lastIndexOf('.');

        String statusesPart = !FileFormat.PDF.equals(file.getFileFormat()) ||
                CollectionUtils.emptyIfNull(file.getSigners()).contains(DocumentSigners.NO) ? "" : "(Signed)";
        this.fileName = createFileName(name, statusesPart, extensionPartIndex);
        this.displayName = name.concat(" ").concat(statusesPart);
        this.fileInfo = createFileInfo(file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(String status, Document file, boolean notSigned) {
        this.id = file.getId();
        String name = file.getName();
        int extensionPartIndex = name.lastIndexOf('.');

        String statusesPart = "(%s)".formatted(notSigned ? "NOT SIGNED" : status);
        this.fileName = createFileName(name, statusesPart, extensionPartIndex);
        this.displayName = name.concat(" ").concat(statusesPart);
    }

    public void updateFileInfo(Document file, String fileInfo) {
        this.fileInfo = createFileInfo(file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ServiceContractFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ServiceContractAdditionalDocuments file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(ReschedulingFiles file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(DepositDocumentFile file, String fileInfo) {
        initializeFields(file.getId(), file.getName(), file.getFileStatuses(), file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    public FileWithStatusesResponse(PowerSupplyDcnReminderDocFiles file, String fileInfo) {
        this.id = file.getId();
        this.fileName = file.getFileName();
        this.displayName = file.getFileName();
        this.fileInfo = createFileInfo(file.getCreateDate(), file.getSystemUserId(), fileInfo);
    }

    private void initializeFields(Long id, String name, List<DocumentFileStatus> fileStatuses, LocalDateTime createDate, String systemUserId, String additionalFileInfo) {
        this.id = id;
        int extensionPartIndex = name.lastIndexOf('.');

        String statusesPart = createStatusesPart(fileStatuses);
        this.fileName = createFileName(name, statusesPart, extensionPartIndex);
        this.displayName = name.concat(" ").concat(statusesPart);
        this.fileInfo = createFileInfo(createDate, systemUserId, additionalFileInfo);
    }


    private String createStatusesPart(List<DocumentFileStatus> fileStatuses) {
        return ("(")
                .concat(CollectionUtils.emptyIfNull(fileStatuses).contains(DocumentFileStatus.DRAFT) ? "Draft, " : "")
                .concat(CollectionUtils.emptyIfNull(fileStatuses).contains(DocumentFileStatus.SIGNED) ? "Signed" : "Not Signed")
                .concat(")");
    }

    private String createFileName(String name, String statusesPart, int extensionPartIndex) {
        return name.substring(0, extensionPartIndex > 0 ? extensionPartIndex : name.length())
                .concat(statusesPart)
                .concat(extensionPartIndex > 0 ? name.substring(extensionPartIndex) : "");
    }

    private String createFileInfo(LocalDateTime createDate, String systemUserId, String additionalFileInfo) {
        return createDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                .concat(" ")
                .concat(systemUserId)
                .concat(additionalFileInfo);
    }
}