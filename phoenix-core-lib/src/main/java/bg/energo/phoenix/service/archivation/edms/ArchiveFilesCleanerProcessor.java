package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFilesCleanerProcessor {
    private final FileService fileService;
    private final EDMSFileArchivationService fileArchivationService;

    @Transactional
    public <T extends FileArchivation> void deleteFiles(ExecutorService executor,
                                                        FileExpiration<T> expiredFile) {
        try {
            log.debug("Deleting expired files");
            List<T> expiredFiles = expiredFile.findExpiredFiles();
            log.debug("Total number of expired files: {}", expiredFiles.size());

            for (T file : expiredFiles) {
                executor.submit(() -> {
                    try {
                        if (Boolean.TRUE.equals(file.getIsArchived())) {
                            String localFileUrl = file.getLocalFileUrl();

                            ByteArrayResource localFileContent = fileService.downloadFile(localFileUrl);
                            ByteArrayResource archivedFileContent = fileArchivationService.downloadArchivedFile(file.getDocumentId(), file.getFileId());

                            byte[] localFileBytes = localFileContent.getContentAsByteArray();
                            byte[] archivedFileBytes = archivedFileContent.getContentAsByteArray();

                            boolean isFilesSame = Arrays.equals(localFileBytes, archivedFileBytes);

                            if (isFilesSame) {
                                if (!expiredFile.isFileUsedInOtherEntities(file.getId(), file.getLocalFileUrl())) {
                                    fileService.deleteOnPath(file.getLocalFileUrl());
                                }

                                file.setLocalFileUrl(null);

                                expiredFile.save(file);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Cannot delete file ", e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Cannot delete file ", e);
        }
    }
}
