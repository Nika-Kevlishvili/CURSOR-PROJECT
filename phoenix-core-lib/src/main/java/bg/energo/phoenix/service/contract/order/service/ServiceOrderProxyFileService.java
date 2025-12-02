package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxyFile;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyFileContent;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyFileResponse;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderProxyFileRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceOrderProxyFileService {

    private final static String FOLDER_PATH = "service_order_proxy_files";
    private final ServiceOrderProxyFileRepository serviceOrderProxyFileRepository;
    private final FileService fileService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;


    /**
     * Uploads service order proxy file to FTP server and saves it to database.
     * The uploaded file does not have any connection to service order proxy yet.
     *
     * @param file file to be uploaded
     * @return response with uploaded file details
     */
    public ServiceOrderProxyFileResponse upload(MultipartFile file) {
        log.debug("Uploading service order proxy file {}.", file.getName());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Service order proxy file name is null.");
            throw new IllegalArgumentsProvidedException("Service order proxy file name is null.");
        }

        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String path = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String url = fileService.uploadFile(file, path, fileName);

        ServiceOrderProxyFile proxyFile = new ServiceOrderProxyFile();
        proxyFile.setOrderProxyId(null);
        proxyFile.setName(fileName);
        proxyFile.setFileUrl(url);
        proxyFile.setStatus(EntityStatus.ACTIVE);
        serviceOrderProxyFileRepository.save(proxyFile);

        return new ServiceOrderProxyFileResponse(proxyFile);
    }


    /**
     * Downloads service order proxy file from FTP server.
     *
     * @param id id of the file to be downloaded
     * @return response with downloaded file details
     */
    public ServiceOrderProxyFileContent download(Long id) {
        log.debug("Downloading service order proxy file with id {}.", id);

        ServiceOrderProxyFile proxyFile = serviceOrderProxyFileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id %s not found.".formatted(id)));

        return new ServiceOrderProxyFileContent(
                proxyFile.getName(),
                fileService.downloadFile(proxyFile.getFileUrl()).getByteArray()
        );
    }


    /**
     * Sets deleted status to outdated service order files when being called from a scheduled task.
     */
    public void cleanupOutDatedFiles() {
        log.debug("Cleaning up outdated service order files.");
        List<ServiceOrderProxyFile> outdatedFiles = serviceOrderProxyFileRepository.findByOrderProxyIdNullAndStatusIn(List.of(EntityStatus.ACTIVE));
        outdatedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
        serviceOrderProxyFileRepository.saveAll(outdatedFiles);
    }


    /**
     * Attaches preliminarily uploaded files to service order proxy.
     *
     * @param fileIds           list of file ids to be attached
     * @param orderProxyId      id of the service order proxy to which the files will be attached
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void attachFilesToServiceOrderProxy(List<Long> fileIds, Long orderProxyId, List<String> exceptionMessages) {
        log.debug("Attaching files {} to service order proxy with id {}.", fileIds, orderProxyId);

        if (CollectionUtils.isNotEmpty(fileIds)) {
            List<ServiceOrderProxyFile> persistedFiles = serviceOrderProxyFileRepository.findAllByIdInAndStatusIn(fileIds, List.of(EntityStatus.ACTIVE));
            List<Long> persistedFileIds = persistedFiles
                    .stream()
                    .map(ServiceOrderProxyFile::getId)
                    .toList();

            List<ServiceOrderProxyFile> tempList = new ArrayList<>();

            for (Long fileId : fileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    log.error("File with id {} not found.", fileId);
                    exceptionMessages.add("File with id %s not found.".formatted(fileId));
                    continue;
                }

                ServiceOrderProxyFile proxyFile = persistedFiles
                        .stream()
                        .filter(f -> f.getId().equals(fileId))
                        .findFirst()
                        .orElse(null);

                if (proxyFile == null || proxyFile.getOrderProxyId() != null) {
                    log.error("File with id {} is already attached to a service order proxy.", fileId);
                    exceptionMessages.add("File with id %s is already attached to a service order proxy.".formatted(fileId));
                    continue;
                }

                proxyFile.setOrderProxyId(orderProxyId);
                tempList.add(proxyFile);
            }

            if (exceptionMessages.isEmpty()) {
                serviceOrderProxyFileRepository.saveAll(tempList);
            }
        }
    }


    /**
     * Updates files for service order proxy.
     *
     * @param requestFileIds    list of file ids to be attached
     * @param orderProxyId      id of the service order proxy to which the files will be attached
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateFiles(List<Long> requestFileIds, Long orderProxyId, List<String> exceptionMessages) {
        log.debug("Updating files {} for service order proxy with id {}.", requestFileIds, orderProxyId);

        // fetch all potential files for the specific IDs provided in request
        List<ServiceOrderProxyFile> existingFiles = serviceOrderProxyFileRepository.findAllByIdInAndStatusIn(requestFileIds, List.of(EntityStatus.ACTIVE));

        List<ServiceOrderProxyFile> persistedFiles = serviceOrderProxyFileRepository.findByOrderProxyIdAndStatusIn(orderProxyId, List.of(EntityStatus.ACTIVE));
        List<Long> persistedFileIds = persistedFiles
                .stream()
                .map(ServiceOrderProxyFile::getId)
                .toList();

        if (CollectionUtils.isNotEmpty(requestFileIds)) {
            List<Long> existingFileIds = existingFiles.stream().map(ServiceOrderProxyFile::getId).toList();

            List<ServiceOrderProxyFile> tempList = new ArrayList<>();

            for (Long fileId : requestFileIds) {
                if (!persistedFileIds.contains(fileId)) {
                    if (!existingFileIds.contains(fileId)) {
                        log.error("File with id %s not found.".formatted(fileId));
                        exceptionMessages.add("File with id %s not found.".formatted(fileId));
                        continue;
                    }

                    ServiceOrderProxyFile proxyFile = existingFiles
                            .stream()
                            .filter(f -> f.getId().equals(fileId))
                            .findFirst()
                            .orElse(null);

                    if (proxyFile == null || proxyFile.getOrderProxyId() != null) {
                        log.error("File with id %s is already attached to a service order proxy.".formatted(fileId));
                        exceptionMessages.add("File with id %s is already attached to a service order proxy.".formatted(fileId));
                        continue;
                    }

                    proxyFile.setOrderProxyId(orderProxyId);
                    tempList.add(proxyFile);
                }
            }

            for (ServiceOrderProxyFile file : persistedFiles) {
                if (!requestFileIds.contains(file.getId())) {
                    file.setStatus(EntityStatus.DELETED);
                    tempList.add(file);
                }
            }

            if (exceptionMessages.isEmpty()) {
                serviceOrderProxyFileRepository.saveAll(tempList);
            }
        } else {
            persistedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));
            serviceOrderProxyFileRepository.saveAll(persistedFiles);
        }
    }


    /**
     * Retrieves service order proxy files by their ids.
     *
     * @param orderProxyId id of the service order proxy
     * @return list of service order proxy files
     */
    public List<ServiceOrderProxyFileResponse> getFilesByOrderProxyId(Long orderProxyId, List<EntityStatus> statuses) {
        return serviceOrderProxyFileRepository
                .findByOrderProxyIdAndStatusIn(orderProxyId, statuses)
                .stream()
                .map(ServiceOrderProxyFileResponse::new)
                .toList();
    }
}
