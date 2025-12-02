package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalDocuments;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractAdditionalDocumentsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
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
public class ServiceContractDocumentService {
    private final ServiceContractAdditionalDocumentsRepository serviceContractAdditionalDocumentsRepository;
    private final EDMSFileArchivationService archivationService;

    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public FileWithStatusesResponse uploadContractFile(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "service_contract_documents", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ServiceContractAdditionalDocuments documentFile = ServiceContractAdditionalDocuments.builder()
                .localFileUrl(url)
                .name(formattedFileName)
                .status(EntityStatus.ACTIVE)
                .fileStatuses(statuses)
                .build();
        ServiceContractAdditionalDocuments savedProxyFile = serviceContractAdditionalDocumentsRepository.saveAndFlush(documentFile);
        return new FileWithStatusesResponse(savedProxyFile, accountManagerRepository.findByUserName(savedProxyFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }


    public FileContent downloadContractFile(Long id) {
        var proxyFile = serviceContractAdditionalDocumentsRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service Contract file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getLocalFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }

    public FileContent checkForArchivationAndDownload(Long id) throws Exception {
        var serviceContractAdditionalDocuments = serviceContractAdditionalDocumentsRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service Contract file with ID %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(serviceContractAdditionalDocuments.getIsArchived())) {
            if (Objects.isNull(serviceContractAdditionalDocuments.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(serviceContractAdditionalDocuments.getDocumentId(), serviceContractAdditionalDocuments.getFileId());

                return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
            }
        }

        var content = fileService.downloadFile(serviceContractAdditionalDocuments.getLocalFileUrl());
        return new FileContent(serviceContractAdditionalDocuments.getName(), content.getByteArray());
    }
}
