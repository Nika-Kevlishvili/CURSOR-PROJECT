package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceFile;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.service.ServiceFileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.product.service.ServiceFileRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceFileService {
    private final ServiceFileRepository serviceFileRepository;

    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final EDMSFileArchivationService edmsFileArchivationService;

    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileType = file.getContentType();
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "service_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ServiceFile savedFile = serviceFileRepository.saveAndFlush(
                ServiceFile
                        .builder()
                        .name(formattedFileName)
                        .localFileUrl(url)
                        .fileType(fileType)
                        .fileStatuses(statuses)
                        .status(EntityStatus.ACTIVE)
                        .build()
        );

        return new FileWithStatusesResponse(savedFile, accountManagerRepository.findByUserName(savedFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    public ServiceFileContent download(Long id) {
        ServiceFile serviceFile = serviceFileRepository.findById(id).orElseThrow(
                () -> new DomainEntityNotFoundException("id-File with presented id not found;"));

        return new ServiceFileContent(serviceFile.getName(), fileService.downloadFile(serviceFile.getLocalFileUrl()).getByteArray());
    }

    public ServiceFileContent checkForArchivationAndDownload(Long id) throws Exception {
        ServiceFile serviceFile = serviceFileRepository.findById(id).orElseThrow(
                () -> new DomainEntityNotFoundException("id-File with presented id not found;"));

        if (Boolean.TRUE.equals(serviceFile.getIsArchived())) {
            if (Objects.isNull(serviceFile.getLocalFileUrl())) {
                ByteArrayResource content = edmsFileArchivationService.downloadArchivedFile(serviceFile.getDocumentId(), serviceFile.getFileId());

                return new ServiceFileContent(content.getFilename(), content.getContentAsByteArray());
            }
        }

        return new ServiceFileContent(serviceFile.getName(), fileService.downloadFile(serviceFile.getLocalFileUrl()).getByteArray());
    }

    public void cleanupOutDatedServiceFiles() {
        List<ServiceFile> outdatedServiceFiles = serviceFileRepository.findActiveByServiceDetailIdNull();
        outdatedServiceFiles.forEach(serviceFile -> serviceFile.setStatus(EntityStatus.DELETED));
        serviceFileRepository.saveAll(outdatedServiceFiles);
    }

    @Transactional
    public void assignServiceFilesToServiceDetails(List<Long> serviceFileIds, ServiceDetails serviceDetails, List<String> exceptionMessages, boolean isUpdate) {
        if (CollectionUtils.isNotEmpty(serviceFileIds)) {
            List<ServiceFile> existingServiceFiles = serviceFileRepository.findAllByIdAndStatusIn(serviceFileIds, List.of(EntityStatus.ACTIVE));
            List<Long> existingServiceFileIds = existingServiceFiles.stream().map(ServiceFile::getId).toList();

            List<Long> nonMatchingFileId = serviceFileIds.stream().filter(id -> !existingServiceFileIds.contains(id)).toList();
            nonMatchingFileId.forEach(id -> {
                log.error("Service File with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.serviceFiles[%s]-Service File with presented id [%s] not found;", serviceFileIds.indexOf(id), id));
            });

            List<ServiceFile> updatedServiceFiles = new ArrayList<>();
            if (isUpdate) {
                for (ServiceFile requestedFile : existingServiceFiles) {
                    if (Objects.isNull(requestedFile.getLocalFileUrl())) {
                        try {
                            ByteArrayResource archivedFile = archivationService.downloadArchivedFile(requestedFile.getDocumentId(), requestedFile.getFileId());

                            FileWithStatusesResponse localFile = upload(new ByteMultiPartFile(requestedFile.getName(), archivedFile.getContentAsByteArray(), requestedFile.getFileType()), requestedFile.getFileStatuses());

                            ServiceFile serviceFile = serviceFileRepository
                                    .findById(localFile.getId())
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Uploaded file with id: [%s] not found;".formatted(localFile.getId())));

                            serviceFile.setServiceDetailId(serviceDetails.getId());

                            updatedServiceFiles.add(serviceFile);
                        } catch (Exception e) {
                            log.error("Exception handled while trying to archive product file: %s".formatted(requestedFile.getName()), e);
                            exceptionMessages.add("Exception handled while trying to archive product file: %s".formatted(requestedFile.getName()));
                        }
                    } else {
                        ServiceFile newVersionServiceFile = ServiceFile
                                .builder()
                                .name(requestedFile.getName())
                                .localFileUrl(requestedFile.getLocalFileUrl())
                                .serviceDetailId(serviceDetails.getId())
                                .fileType(requestedFile.getFileType())
                                .fileStatuses(requestedFile.getFileStatuses())
                                .status(requestedFile.getStatus())
                                .build();

                        updatedServiceFiles.add(newVersionServiceFile);
                    }
                }
            } else {
                for (ServiceFile serviceFile : existingServiceFiles) {
                    if (serviceFile.getServiceDetailId() != null) {
                        log.error("Service File with presented id [{}] is assigned to service detail with id [{}]", serviceFile.getId(), serviceDetails.getId());
                        exceptionMessages.add(String.format("basicSettings.serviceFiles[%s]-Service File with presented id [%s] is assigned to service detail with id [%s];", serviceFileIds.indexOf(serviceFile.getId()), serviceFile.getId(), serviceFile.getServiceDetailId()));
                    } else {
                        serviceFile.setServiceDetailId(serviceDetails.getId());
                        updatedServiceFiles.add(serviceFile);
                    }
                }
            }

            if (exceptionMessages.isEmpty()) {
                serviceFileRepository.saveAll(updatedServiceFiles);
            }
        }
    }

    @Transactional
    public void updateServiceFiles(List<Long> serviceFileIds, ServiceDetails updatedServiceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(serviceFileIds)) {
            List<ServiceFile> existingServiceFiles = serviceFileRepository.findAllByIdAndStatusIn(serviceFileIds, List.of(EntityStatus.ACTIVE));
            List<Long> existingServiceFileIds = existingServiceFiles.stream().map(ServiceFile::getId).toList();

            List<Long> nonMatchingFileId = serviceFileIds.stream().filter(id -> !existingServiceFileIds.contains(id)).toList();
            nonMatchingFileId.forEach(id -> {
                log.error("Service File with presented id not found [{}]", id);
                exceptionMessages.add(String.format("basicSettings.serviceFiles[%s]-Service File with presented id [%s] not found;", serviceFileIds.indexOf(id), id));
            });

            existingServiceFiles
                    .stream()
                    .filter(serviceFile ->
                            (serviceFile.getServiceDetailId() != null)
                            &&
                            (!serviceFile.getServiceDetailId().equals(updatedServiceDetails.getId())))
                    .forEach(serviceFile -> {
                        log.error("Service File with presented id: [{}] is related with service details with id [{}]", serviceFile.getId(), serviceFile.getServiceDetailId());
                        exceptionMessages.add(String.format("basicSettings.serviceFiles[%s]-Service File with presented id: [%s] is related with service details with id [%s]", serviceFileIds.indexOf(serviceFile.getId()), serviceFile.getId(), serviceFile.getServiceDetailId()));
                    });

            if (exceptionMessages.isEmpty()) {
                List<ServiceFile> assignedServiceFiles = updatedServiceDetails.getServiceFiles();
                assignedServiceFiles
                        .stream()
                        .filter(assignedServiceFile -> !serviceFileIds.contains(assignedServiceFile.getId()))
                        .forEach(assignedServiceFile -> assignedServiceFile.setStatus(EntityStatus.DELETED));
                serviceFileRepository.saveAll(assignedServiceFiles);

                for (ServiceFile serviceFile : existingServiceFiles) {
                    serviceFile.setServiceDetailId(updatedServiceDetails.getId());
                }
                serviceFileRepository.saveAll(existingServiceFiles);
            }
        } else {
            List<ServiceFile> assignedServiceFiles = updatedServiceDetails.getServiceFiles();
            assignedServiceFiles.forEach(assignedServiceFile -> {
                assignedServiceFile.setStatus(EntityStatus.DELETED);
            });
            serviceFileRepository.saveAll(assignedServiceFiles);
        }
    }
}
