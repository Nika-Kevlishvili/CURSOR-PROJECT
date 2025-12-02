package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunOutdatedDocumentService {
    private final DocumentsRepository documentRepository;
    private final FileService fileService;

    @ExecutionTimeLogger
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteOutdatedDocuments(List<Document> documents) {
        new Thread(() -> {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            log.debug("Deleting outdated documents: {}", documents.size());
            for (Document document : documents) {
                executorService.submit(() -> {
                            log.debug("Deleting outdated document: {}", document);
                            try {
                                log.debug("Setting invoice status to DELETED");
                                document.setStatus(EntityStatus.DELETED);

                                try {
                                    log.debug("Deleting file on path: {}", document.getSignedFileUrl());
                                    fileService.deleteOnPath(document.getSignedFileUrl());
                                    document.setSignedFileUrl(null);
                                } catch (Exception e) {
                                    log.error("Exception while deleting signed file", e);
                                }

                                try {
                                    log.debug("Deleting file on path: {}", document.getUnsignedFileUrl());
                                    fileService.deleteOnPath(document.getUnsignedFileUrl());
                                    document.setUnsignedFileUrl(null);
                                } catch (Exception e) {
                                    log.error("Exception while deleting unsigned file", e);
                                }

                                log.debug("Saving document: {}", document);
                                documentRepository.save(document);
                            } catch (Exception e) {
                                log.error("Exception while deleting outdated documents", e);
                            }
                        }
                );
            }
            executorService.shutdown();
        }).start();
    }
}
