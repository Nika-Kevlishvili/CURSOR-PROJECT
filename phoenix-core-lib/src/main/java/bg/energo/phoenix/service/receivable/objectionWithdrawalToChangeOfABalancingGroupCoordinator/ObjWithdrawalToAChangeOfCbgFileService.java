package bg.energo.phoenix.service.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgDocFile;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgFiles;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgDocFileRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgFilesRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjWithdrawalToAChangeOfCbgFileService {
    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    private final ObjectionWithdrawalToChangeOfCbgFilesRepository objectionWithdrawalToChangeOfCbgFilesRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private final ObjectionWithdrawalToCbgDocFileRepository withdrawalDocumentRepository;
    private final DocumentsRepository documentsRepository;


    private static final String FOLDER_PATH = "obj_withdrawal_to_change_of_cbg_files";
    private static final long MAX_SIZE = 50 * 1024 * 1024;

    @Transactional
    public FileWithStatusesResponse uploadFile(MultipartFile file, List<DocumentFileStatus> fileStatuses) {
        if (file.getSize() > MAX_SIZE) {
            log.error("The size of the file exceeds 50 mb;");
            throw new IllegalArgumentsProvidedException("The size of the file exceeds 50 mb;");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");

        String fileUrl = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String name = UUID.randomUUID().toString().concat(formattedFileName.substring(formattedFileName.lastIndexOf('.')));
        String url = fileService.uploadFile(file, fileUrl, name);
        ObjectionWithdrawalToChangeOfCbgFiles cbgFiles = new ObjectionWithdrawalToChangeOfCbgFiles();
        cbgFiles.setFileUrl(url);
        cbgFiles.setName(formattedFileName);

        cbgFiles.setStatus(EntityStatus.ACTIVE);
        cbgFiles.setFileStatuses(fileStatuses);
        var savedEntity = objectionWithdrawalToChangeOfCbgFilesRepository.saveAndFlush(cbgFiles);
        return new FileWithStatusesResponse(savedEntity, accountManagerRepository.findByUserName(savedEntity.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));

    }

    public FileContent downloadFile(Long id) {
        ObjectionWithdrawalToChangeOfCbgFiles files = objectionWithdrawalToChangeOfCbgFilesRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("file with id %s not found".formatted(id)));
        var content = fileService.downloadFile(files.getFileUrl());
        return new FileContent(files.getName(), content.getByteArray());
    }

    public FileContent downloadDocument(Long id) {
        ObjectionWithdrawalToCbgDocFile document = withdrawalDocumentRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("document with id %s not found".formatted(id)));
        var content = fileService.downloadFile(document.getFileUrl());
        return new FileContent(document.getFileName(), content.getByteArray());
    }

    public FileContent downloadDocumentByDocId(Long id) {
        Document document = documentsRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("document with id %s not found".formatted(id)));
        var content = fileService.downloadFile(document.getSignedFileUrl());
        return new FileContent(document.getName(), content.getByteArray());
    }

}
