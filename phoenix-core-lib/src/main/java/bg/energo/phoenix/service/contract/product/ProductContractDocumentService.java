package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDocument;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractDocumentContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractDocumentRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
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
public class ProductContractDocumentService {
    private final FileService fileService;
    private final ProductContractDocumentRepository productContractDocumentRepository;
    private final AccountManagerRepository accountManagerRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private final EDMSFileArchivationService archivationService;

    /**
     * Uploads product contract document
     *
     * @param file - Product Contract Document
     * @return - {@link ProductContractDocument} - details (id, file name)
     */
    @Transactional
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }

        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "product_contract_documents", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ProductContractDocument productContractDocument = ProductContractDocument
                .builder()
                .name(formattedFileName)
                .localFileUrl(url)
                .fileStatuses(statuses)
                .status(EntityStatus.ACTIVE)
                .build();

        ProductContractDocument saved = productContractDocumentRepository.saveAndFlush(productContractDocument);
        return new FileWithStatusesResponse(saved, accountManagerRepository.findByUserName(saved.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Downloads product contract document with presented id
     *
     * @param id - {@link ProductContractDocument#id}
     * @return - {@link ProductContractDocumentContent} - product contract document name and file content
     */
    public ProductContractDocumentContent download(Long id) {
        ProductContractDocument productContractDocument = productContractDocumentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found"));
        ByteArrayResource resource = fileService.downloadFile(productContractDocument.getLocalFileUrl());
        return new ProductContractDocumentContent(productContractDocument.getName(), resource.getByteArray());
    }

    public ProductContractDocumentContent checkForArchivationAndDownload(Long id) {
        ProductContractDocument productContractDocument = productContractDocumentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found"));

        if (Boolean.TRUE.equals(productContractDocument.getIsArchived())) {
            if (Objects.isNull(productContractDocument.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(productContractDocument.getDocumentId(), productContractDocument.getFileId());

                return new ProductContractDocumentContent(productContractDocument.getName(), fileContent.getByteArray());
            }
        }

        ByteArrayResource resource = fileService.downloadFile(productContractDocument.getLocalFileUrl());
        return new ProductContractDocumentContent(productContractDocument.getName(), resource.getByteArray());
    }

    /**
     * Marks unused product contract files as deleted
     *
     * @see ProductContractFileCleanerService
     */
    public void cleanupOutDatedFiles() {
        List<ProductContractDocument> outdatedFiles = productContractDocumentRepository
                .findActiveByProductContractIdNull();

        outdatedFiles.forEach(file -> file.setStatus(EntityStatus.DELETED));

        productContractDocumentRepository.saveAll(outdatedFiles);
    }

    /**
     * Adding product contract associated documents to newly created contract
     *
     * @param contractDetails   - Product Contract Details
     * @param ids               - Document ids
     * @param exceptionMessages - Handled exceptions list while trying to add files
     */
    @Transactional
    public void assignProductContractDocumentsToProductContract(ProductContractDetails contractDetails, List<Long> ids, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<ProductContractDocument> productContractDocuments = productContractDocumentRepository
                    .findAllByIdInAndStatusIn(ids, List.of(EntityStatus.ACTIVE));
            List<Long> existingProductContractDocumentIds = productContractDocuments.stream().map(ProductContractDocument::getId).toList();
            List<Long> notExistingProductContractDocumentIds = ids.stream().filter(id -> !existingProductContractDocumentIds.contains(id)).toList();

            if (CollectionUtils.isNotEmpty(notExistingProductContractDocumentIds)) {
                for (Long id : notExistingProductContractDocumentIds) {
                    exceptionMessages.add("basicParameters.documents[%s]-Document with presented id [%s] not found;".formatted(ids.indexOf(id), id));
                }
            } else {
                for (ProductContractDocument file : productContractDocuments) {
                    file.setContractDetailId(contractDetails.getId());
                }
                productContractDocumentRepository.saveAll(productContractDocuments);
            }
        }
    }

    @Transactional
    public void updateProductContractDocumentsOnProductContract(ProductContractDetails initialProductContractDetails,
                                                                ProductContractDetails newProductContractDetails,
                                                                ProductContractUpdateRequest request,
                                                                List<String> exceptionMessages) {
        List<Long> requestDocumentIds = request.getBasicParameters().getDocuments();

        if (Boolean.TRUE.equals(request.isSavingAsNewVersion())) {
            for (int i = 0; i < requestDocumentIds.size(); i++) {
                Long id = requestDocumentIds.get(i);

                Optional<ProductContractDocument> productContractDocumentOptional = productContractDocumentRepository.findById(id);
                if (productContractDocumentOptional.isEmpty()) {
                    exceptionMessages.add("basicParameters.documents[%s]-File with presented id [%s] not found;".formatted(i, id));
                } else {
                    ProductContractDocument productContractDocument = productContractDocumentOptional.get();

                    if (Boolean.TRUE.equals(productContractDocument.getIsArchived())) {
                        try {
                            ByteArrayResource archivedFile = archivationService.downloadArchivedFile(productContractDocument.getDocumentId(), productContractDocument.getFileId());

                            FileWithStatusesResponse localFile = upload(new ByteMultiPartFile(productContractDocument.getName(), archivedFile.getContentAsByteArray()), productContractDocument.getFileStatuses());

                            ProductContractDocument document = productContractDocumentRepository
                                    .findById(localFile.getId())
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with id: [%s] not found".formatted(localFile.getId())));

                            document.setContractDetailId(newProductContractDetails.getId());
                            productContractDocumentRepository.save(document);
                        } catch (Exception e) {
                            log.error("Exception handled while trying to archive document in new version: %s".formatted(productContractDocument.getName()), ErrorCode.APPLICATION_ERROR);
                            throw new ClientException("Exception handled while trying to archive document in new version: %s".formatted(productContractDocument.getName()), ErrorCode.APPLICATION_ERROR);
                        }
                    } else {
                        productContractDocumentRepository.save(
                                ProductContractDocument
                                        .builder()
                                        .name(productContractDocument.getName())
                                        .localFileUrl(productContractDocument.getLocalFileUrl())
                                        .contractDetailId(newProductContractDetails.getId())
                                        .fileStatuses(productContractDocument.getFileStatuses())
                                        .status(EntityStatus.ACTIVE)
                                        .build()
                        );
                    }
                }
            }
        } else {
            Map<Long, List<ProductContractDocument>> productContractDocumentsMap = productContractDocumentRepository
                    .findAllByContractDetailIdAndStatusIn(initialProductContractDetails.getId(), List.of(EntityStatus.ACTIVE))
                    .stream()
                    .collect(Collectors.groupingBy(ProductContractDocument::getId));

            for (int i = 0; i < requestDocumentIds.size(); i++) {
                Long id = requestDocumentIds.get(i);
                if (!productContractDocumentsMap.containsKey(id)) {
                    Optional<ProductContractDocument> productContractDocumentOptional = productContractDocumentRepository.findById(id);

                    if (productContractDocumentOptional.isEmpty()) {
                        exceptionMessages.add("basicParameters.documents[%s]-File with presented id [%s] not found;".formatted(i, id));
                    } else {
                        ProductContractDocument productContractDocument = productContractDocumentOptional.get();

                        productContractDocument.setContractDetailId(initialProductContractDetails.getId());
                    }
                }
            }

            for (Map.Entry<Long, List<ProductContractDocument>> productContractDocumentEntry : productContractDocumentsMap.entrySet()) {
                if (!requestDocumentIds.contains(productContractDocumentEntry.getKey())) {
                    List<ProductContractDocument> documents = productContractDocumentEntry.getValue();

                    documents.forEach(doc -> doc.setStatus(EntityStatus.DELETED));
                }
            }
        }
    }
}
