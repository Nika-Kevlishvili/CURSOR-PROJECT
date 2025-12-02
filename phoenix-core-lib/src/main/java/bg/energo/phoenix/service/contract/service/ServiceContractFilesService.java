package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractFiles;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractFilesRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceContractFilesService {
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final EDMSFileArchivationService edmsFileArchivationService;

    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    private final DocumentsRepository documentsRepository;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public FileWithStatusesResponse uploadContractFile(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "service_contract_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ServiceContractFiles serviceContractFile = serviceContractFilesRepository.saveAndFlush(
                ServiceContractFiles.builder()
                        .localFileUrl(url)
                        .name(formattedFileName)
                        .status(EntityStatus.ACTIVE)
                        .fileStatuses(statuses)
                        .build()
        );
        return new FileWithStatusesResponse(serviceContractFile, accountManagerRepository.findByUserName(serviceContractFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }


    public FileContent downloadContractFile(Long id) {
        ServiceContractFiles serviceContractFile = serviceContractFilesRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service Contract file with ID %s not found;".formatted(id)));
        ByteArrayResource content = fileService.downloadFile(serviceContractFile.getLocalFileUrl());
        return new FileContent(serviceContractFile.getName(), content.getByteArray());
    }

    public FileContent checkForArchivationAndDownload(Long id, ContractFileType contractFileType, DocumentStatus status) throws Exception {
        if (contractFileType == ContractFileType.UPLOADED_FILE) {
            ServiceContractFiles serviceContractFile = serviceContractFilesRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Service Contract file with ID %s not found;".formatted(id)));

            if (Boolean.TRUE.equals(serviceContractFile.getIsArchived())) {
                if (Objects.isNull(serviceContractFile.getLocalFileUrl())) {
                    ByteArrayResource fileContent = edmsFileArchivationService.downloadArchivedFile(serviceContractFile.getDocumentId(), serviceContractFile.getFileId());

                    return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
                }
            }

            ByteArrayResource content = fileService.downloadFile(serviceContractFile.getLocalFileUrl());
            return new FileContent(serviceContractFile.getName(), content.getByteArray());
        } else {
            Document document = documentsRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found"));
            ByteArrayResource resource = fileService.downloadFile(DocumentStatus.SIGNED.equals(status) ? document.getSignedFileUrl() : document.getUnsignedFileUrl());

            return new FileContent(document.getName(), resource.getByteArray());
        }
    }
}
