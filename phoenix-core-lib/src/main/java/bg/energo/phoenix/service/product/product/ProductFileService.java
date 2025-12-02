package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.product.ProductFile;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.response.product.ProductFileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.product.product.ProductFileRepository;
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
@Slf4j
@RequiredArgsConstructor
public class ProductFileService {

    private final ProductFileRepository productFileRepository;

    private final FileService fileService;
    private final AccountManagerRepository accountManagerRepository;

    private final EDMSFileArchivationService edmsFileArchivationService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public FileWithStatusesResponse uploadProductFile(MultipartFile file, List<DocumentFileStatus> fileStatuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileType = file.getContentType();
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "product_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);
        ProductFile productFile = new ProductFile();
        productFile.setLocalFileUrl(url);
        productFile.setName(formattedFileName);
        productFile.setFileType(fileType);
        productFile.setStatus(EntityStatus.ACTIVE);
        productFile.setFileStatuses(fileStatuses);
        var savedEntity = productFileRepository.saveAndFlush(productFile);
        return new FileWithStatusesResponse(savedEntity, accountManagerRepository.findByUserName(savedEntity.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    public void cleanupProductFileData() {
        var productFiles = productFileRepository.findActiveByProductDetailIdNull();
        productFiles.forEach(productFile -> productFile.setStatus(EntityStatus.DELETED));
        productFileRepository.saveAll(productFiles);
    }

    public ProductFileContent downloadFile(Long id) {
        var productFile = productFileRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(productFile.getLocalFileUrl());
        return new ProductFileContent(productFile.getName(), content.getByteArray());
    }

    public ProductFileContent checkForArchivationAndDownload(Long id) throws Exception {
        var productFile = productFileRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product file with ID %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(productFile.getIsArchived())) {
            if (Objects.isNull(productFile.getLocalFileUrl())) {
                ByteArrayResource content = edmsFileArchivationService.downloadArchivedFile(productFile.getDocumentId(), productFile.getFileId());

                return new ProductFileContent(content.getFilename(), content.getContentAsByteArray());
            }
        }

        var content = fileService.downloadFile(productFile.getLocalFileUrl());
        return new ProductFileContent(productFile.getName(), content.getByteArray());
    }
}
