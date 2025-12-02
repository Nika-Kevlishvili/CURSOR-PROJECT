package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractFile;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractFileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractFileRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractFilesService {
    private final FileService fileService;
    private final ProductContractFileRepository productContractFileRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final EDMSFileArchivationService archivationService;
    private final DocumentsRepository documentsRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Uploads product contract file
     *
     * @param file - Product Contract File
     * @return - {@link ProductContractFile} - details (id, file name)
     */
    @Transactional
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "product_contract_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ProductContractFile productContractFile = new ProductContractFile();
        productContractFile.setLocalFileUrl(url);
        productContractFile.setName(formattedFileName);
        productContractFile.setStatus(EntityStatus.ACTIVE);
        productContractFile.setFileStatuses(statuses);

        ProductContractFile saved = productContractFileRepository.saveAndFlush(productContractFile);
        return new FileWithStatusesResponse(
                saved,
                accountManagerRepository.findByUserName(saved.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")"))
                        .orElse("")
        );
    }

    /**
     * Downloads product contract file with presented id
     *
     * @param id - {@link ProductContractFile#id}
     * @return - {@link ProductContractFileContent} - product contract file name and file content
     */
    public ProductContractFileContent download(Long id) {
        ProductContractFile productContractFile = productContractFileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found"));

        ByteArrayResource resource = fileService.downloadFile(productContractFile.getLocalFileUrl());

        return new ProductContractFileContent(productContractFile.getName(), resource.getByteArray());
    }

    public ProductContractFileContent checkForArchivationAndDownload(Long id, ContractFileType contractFileType, DocumentStatus status) {
        if (contractFileType == ContractFileType.UPLOADED_FILE) {
            ProductContractFile productContractFile = productContractFileRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found"));

            if (Boolean.TRUE.equals(productContractFile.getIsArchived())) {
                if (Objects.isNull(productContractFile.getLocalFileUrl())) {
                    ByteArrayResource fileContent = archivationService.downloadArchivedFile(productContractFile.getDocumentId(), productContractFile.getFileId());

                    return new ProductContractFileContent(productContractFile.getName(), fileContent.getByteArray());
                }
            }

            ByteArrayResource resource = fileService.downloadFile(productContractFile.getLocalFileUrl());

            return new ProductContractFileContent(productContractFile.getName(), resource.getByteArray());
        } else {
            Document document = documentsRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found"));
            ByteArrayResource resource = fileService.downloadFile(DocumentStatus.SIGNED.equals(status) ? document.getSignedFileUrl() : document.getUnsignedFileUrl());

            return new ProductContractFileContent(document.getName(), resource.getByteArray());
        }

    }

    /**
     * Marks unused product contract files as deleted
     *
     * @see ProductContractFileCleanerService
     */
    public void cleanupOutDatedFiles() {
        List<ProductContractFile> outdatedFiles = productContractFileRepository
                .findActiveByProductContractIdNull();

        outdatedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));

        productContractFileRepository.saveAll(outdatedFiles);
    }

    /**
     * Adding product contract associated files to newly created contract
     *
     * @param contractDetails   - Product Contract Details
     * @param ids               - File ids
     * @param exceptionMessages - Handled exceptions list while trying to add files
     */
    @Transactional
    public void assignProductContractFilesToProductContract(ProductContractDetails contractDetails, List<Long> ids, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<ProductContractFile> productContractFiles = productContractFileRepository
                    .findAllByIdInAndStatusIn(ids, List.of(EntityStatus.ACTIVE));
            List<Long> existingProductContractFileIds = productContractFiles.stream().map(ProductContractFile::getId).toList();
            List<Long> notExistingProductContractFileIds = ids.stream().filter(id -> !existingProductContractFileIds.contains(id)).toList();

            if (CollectionUtils.isNotEmpty(notExistingProductContractFileIds)) {
                notExistingProductContractFileIds.forEach(id -> exceptionMessages.add("basicParameters.files[%s]-File with presented id [%s] not found;".formatted(ids.indexOf(id), id)));
            } else {
                for (ProductContractFile file : productContractFiles) {
                    file.setContractDetailId(contractDetails.getId());
                }
                productContractFileRepository.saveAll(productContractFiles);
            }
        }
    }

    @Transactional
    public void updateProductContractFilesOnProductContract(ProductContractDetails initialProductContractDetails,
                                                            ProductContractDetails newProductContractDetails,
                                                            ProductContractUpdateRequest request,
                                                            List<String> exceptionMessages) {
        List<Long> requestFileIds = request.getBasicParameters().getFiles();

        if (Boolean.TRUE.equals(request.isSavingAsNewVersion())) {
            for (int i = 0; i < requestFileIds.size(); i++) {
                Long id = requestFileIds.get(i);
                Optional<ProductContractFile> productContractFileOptional = productContractFileRepository.findById(id);
                if (productContractFileOptional.isEmpty()) {
                    exceptionMessages.add("basicParameters.files[%s]-File with presented id [%s] not found;".formatted(i, id));
                } else {
                    ProductContractFile productContractFile = productContractFileOptional.get();

                    if (Boolean.TRUE.equals(productContractFile.getIsArchived())) {
                        try {
                            ByteArrayResource archivedFile = archivationService.downloadArchivedFile(productContractFile.getDocumentId(), productContractFile.getFileId());

                            FileWithStatusesResponse localFile = upload(new ByteMultiPartFile(productContractFile.getName(), archivedFile.getContentAsByteArray()), productContractFile.getFileStatuses());

                            ProductContractFile file = productContractFileRepository
                                    .findById(localFile.getId())
                                    .orElseThrow(() -> new DomainEntityNotFoundException("File with id: [%s] not found;".formatted(localFile.getId())));

                            file.setContractDetailId(newProductContractDetails.getId());
                            productContractFileRepository.save(file);
                        } catch (Exception e) {
                            log.error("Exception handled while trying to archive file in new version: %s".formatted(productContractFile.getName()), ErrorCode.APPLICATION_ERROR);
                            throw new ClientException("Exception handled while trying to archive file in new version: %s".formatted(productContractFile.getName()), ErrorCode.APPLICATION_ERROR);
                        }
                    } else {
                        productContractFileRepository.save(
                                ProductContractFile
                                        .builder()
                                        .name(productContractFile.getName())
                                        .localFileUrl(productContractFile.getLocalFileUrl())
                                        .contractDetailId(newProductContractDetails.getId())
                                        .fileStatuses(productContractFile.getFileStatuses())
                                        .status(EntityStatus.ACTIVE)
                                        .build()
                        );
                    }
                }
            }
        } else {
            Map<Long, List<ProductContractFile>> productContractFilesMap = productContractFileRepository
                    .findAllByContractDetailIdAndStatusIn(initialProductContractDetails.getId(), List.of(EntityStatus.ACTIVE))
                    .stream()
                    .collect(Collectors.groupingBy(ProductContractFile::getId));

            for (int i = 0; i < requestFileIds.size(); i++) {
                Long id = requestFileIds.get(i);
                if (!productContractFilesMap.containsKey(id)) {
                    Optional<ProductContractFile> productContractFileOptional = productContractFileRepository.findById(id);

                    if (productContractFileOptional.isEmpty()) {
                        exceptionMessages.add("basicParameters.files[%s]-File with presented id [%s] not found;".formatted(i, id));
                    } else {
                        ProductContractFile productContractFile = productContractFileOptional.get();

                        productContractFile.setContractDetailId(initialProductContractDetails.getId());
                    }
                }
            }

            for (Map.Entry<Long, List<ProductContractFile>> productContractFilesEntry : productContractFilesMap.entrySet()) {
                if (!requestFileIds.contains(productContractFilesEntry.getKey())) {
                    List<ProductContractFile> files = productContractFilesEntry.getValue();

                    files.forEach(file -> file.setStatus(EntityStatus.DELETED));
                }
            }
        }
    }
}
