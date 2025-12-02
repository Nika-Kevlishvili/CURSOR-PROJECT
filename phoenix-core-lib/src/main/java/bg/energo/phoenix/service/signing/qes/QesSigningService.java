package bg.energo.phoenix.service.signing.qes;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.QesDocument;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.template.QesDocumentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSManualArchivationService;
import bg.energo.phoenix.service.archivation.edms.SignedDocumentFileArchivation;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.qes.entities.*;
import bg.energo.phoenix.service.signing.qes.repositories.QesAliasRepository;
import bg.energo.phoenix.service.signing.qes.repositories.QesDocumentDetailsRepository;
import bg.energo.phoenix.service.signing.qes.repositories.QesSignSessionObjectsRepository;
import bg.energo.phoenix.service.signing.qes.repositories.QesSignSessionRepository;
import bg.energo.phoenix.service.signing.qes.request.QesAliasRequest;
import bg.energo.phoenix.service.signing.qes.request.QesCancelRequest;
import bg.energo.phoenix.service.signing.qes.request.QesStartSigningRequest;
import bg.energo.phoenix.service.signing.qes.request.QesStopRequest;
import bg.energo.phoenix.service.signing.qes.response.QesAliasResponse;
import bg.energo.phoenix.service.signing.qes.response.QesSigningResponse;
import bg.energo.phoenix.service.signing.qes.response.QesSigningStatusUpdateResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QesSigningService {


    private final QesDocumentDetailsRepository qesDocumentDetailsRepository;
    private final QesDocumentRepository qesDocumentRepository;
    private final QesSignSessionRepository qesSignSessionRepository;
    private final QesSignSessionObjectsRepository qesSignSessionObjectsRepository;
    private final DocumentsRepository documentsRepository;
    private final SignedDocumentFileArchivation signedDocumentFileArchivation;
    private final EDMSManualArchivationService edmsManualArchivationService;
    private final FileService fileService;
    private final QesSocketSender messagingTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PermissionService permissionService;
    private final QesAliasRepository qesAliasRepository;
    @Value("${qes.jsign.url}")
    private String jsignUrl;
    @Value("${qes.sharedFolderPath}")
    private String sharedFolderPath;
    private String sharedFolderGlobalPath = "\\\\dfs-p01-fs-01.bulgaria.eon.net\\bg_epro_team$\\140\\";
    @Value("${qes.ftpPath}")
    private String ftpPath;
    private Integer batchSize = 2;
    private Integer threadCount = 1;

    private final String signingUpdateQueue="/queue/signing-updates-%s";
    private final String signFinishQueue="/queue/sign-finish-%s";
    private final String transferFinishQueue="/queue/transfer-finish-%s";


    @Transactional
    @SneakyThrows
    /**
     * Moves signed documents from the shared folder to the appropriate locations.
     * This method is responsible for processing all QesSignSession records, retrieving the signed documents from the shared folder,
     * saving them to the database, and updating the signing status for each session.
     * It also handles sending updates to the client via WebSocket and cleaning up the shared folder.
     */
    public void moveFromShared() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddHHmm");
            String date = LocalDateTime.now().format(formatter);
            MDC.put("qesSigningLog", date);
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            //get folders
            List<QesSignSession> all = qesSignSessionRepository.findAll();
            log.debug("Sessions found {}", all.size());
            log.debug("Sessions {}", all.stream().map(QesSignSession::getSessionId).collect(Collectors.joining(",")));
            File sharedFolder = new File(sharedFolderPath);
            List<Callable<Boolean>> callables = new ArrayList<>();
            List<Document> documents = Collections.synchronizedList(new ArrayList<>());
            log.debug("Starting signing for sessions");
            for (QesSignSession qesSignSession : all) {
                MDC.put("qesMSessionId", qesSignSession.getSessionId());
                log.debug("Total count {}", qesSignSession.getTotalCount());
                log.debug("Sign count {}", qesSignSession.getSignCount());
                File signFolderPath = new File(sharedFolder, qesSignSession.getSignedPath());
                File[] files = signFolderPath.listFiles();
                if (files == null || files.length == 0) {
                    log.debug("Sign session folder is empty!;");
                    continue;
                }
                log.debug("File size {}", files.length);
                AtomicInteger atomicInteger = new AtomicInteger(qesSignSession.getSignCount());
                for (File file : files) {
                    callables.add(() -> {
                        MDC.put("qesSigningLog", date);
                        MDC.put("qesMSessionId", qesSignSession.getSessionId());
                        if (file.isFile() && file.canRead() && file.length() > 0) {
                            log.debug("Started saving doc {}", file.getName());
                            boolean savedDoc = saveSignedDoc(qesSignSession, file, documents);
                            log.debug("Saved doc? {}", savedDoc);
                            if (savedDoc) {
                                int signedCount = atomicInteger.incrementAndGet();
                                if (signedCount % batchSize == 0 || signedCount == files.length) {
                                    messagingTemplate.convertAndSend(signingUpdateQueue.formatted(qesSignSession.getSessionId()), new QesSigningStatusUpdateResponse(-1, -1, signedCount, qesSignSession.getTotalCount()));
                                    log.debug("Sent count to session {}", qesSignSession.getSessionId());
                                }
                            }

                        }

                        MDC.remove("qesMSessionId");
                        MDC.remove("qesSigningLog");
                        return true;
                    });
                }
                log.debug("Callable size {}", callables.size());
                executorService.invokeAll(callables);
                qesSignSession.setSignCount(atomicInteger.get());
                qesSignSessionRepository.save(qesSignSession);
                boolean existsSessionObjects = qesSignSessionObjectsRepository.existsByQesSignSessionId(qesSignSession.getId());
                log.debug("Session objects exist {}", existsSessionObjects);
                if (!existsSessionObjects) {
                    messagingTemplate.convertAndSend(signFinishQueue.formatted(qesSignSession.getSessionId()), new QesSigningStatusUpdateResponse(-1, -1, atomicInteger.get(), qesSignSession.getTotalCount()));
                    log.debug("Sending finish");
                }
                MDC.remove("qesMSessionId");
            }

            fileCleanup();
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            MDC.remove("qesSigningLog");
        }
    }

    @SneakyThrows
    public void moveToShared(QesStartSigningRequest request) {


        MDC.put("QesSessionId", request.getSessionId());

        boolean invalidDocuments = false;
        if (request.isAllSelected()) {
            invalidDocuments = qesDocumentDetailsRepository.findInvalidDocumentsWithFilter(
                    request.getExcludedIds(),
                    request.getStatus() == null ? List.of(QesStatus.ACTIVE.toString()) : request.getStatus().stream().map(Enum::toString).toList(),
                    request.getSigningStatuses() == null ? new ArrayList<>() : request.getSigningStatuses().stream().map(Enum::toString).toList(),
                    request.getPurposes() == null ? new ArrayList<>() : request.getPurposes().stream().map(Enum::toString).toList(),
                    request.getProductIds(),
                    request.getServiceIds(),
                    request.getPodIds(),
                    request.getSegments(),
                    request.getSaleChannels(),
                    request.getCreatedBy(),
                    request.getUpdatedFrom(),
                    request.getUpdatedTo(),
                    request.getSearchFilter() == null ? null : request.getSearchFilter().toString(),
                    request.getPrompt(),
                    LocalDateTime.now().minusDays(30)
            );
        } else {
            invalidDocuments = qesDocumentDetailsRepository.findInvalidDocuments(request.getDocumentIds());
        }
        if (invalidDocuments) {
            throw new IllegalArgumentsProvidedException("Some documents can not be signed!;");
        }
        Optional<QesAlias> alias = qesAliasRepository.findById(permissionService.getLoggedInUserId());
        if (alias.isEmpty() || alias.get().getAlias() == null) {
            throw new IllegalArgumentsProvidedException("alias-Alias is not configured!;");
        }
        new Thread(() -> startTransfer(request, alias.get().getAlias())).start();
    }

    @Transactional
    public void stopProcess(QesStopRequest request) {
        QesSignSession qesSignSession = qesSignSessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow();

        File sharedFolder = new File(sharedFolderPath);

        File signedFolder = new File(sharedFolder, qesSignSession.getSignedPath());
        File unsignedFolder = new File(sharedFolder, qesSignSession.getUnsignedPath());
        FileSystemUtils.deleteRecursively(signedFolder);
        FileSystemUtils.deleteRecursively(unsignedFolder);
        undoQesDetailObjects(List.of(qesSignSession.getId()));

        qesSignSessionObjectsRepository.deleteAllByQesSignSessionId(qesSignSession.getId());
        qesSignSessionRepository.delete(qesSignSession);
    }

    @Transactional
    public void cancelQesDetails(QesCancelRequest request) {

        boolean invalidDocuments = false;


        if (request.isAllSelected()) {
            invalidDocuments = qesDocumentDetailsRepository.findInvalidDocumentToCancelFilter(
                    request.getExcludedIds(),
                    request.getStatus() == null ? List.of(QesStatus.ACTIVE.toString()) : request.getStatus().stream().map(Enum::toString).toList(),
                    request.getSigningStatuses() == null ? new ArrayList<>() : request.getSigningStatuses().stream().map(Enum::toString).toList(),
                    request.getPurposes() == null ? new ArrayList<>() : request.getPurposes().stream().map(Enum::toString).toList(),
                    request.getProductIds(),
                    request.getServiceIds(),
                    request.getPodIds(),
                    request.getSegments(),
                    request.getSaleChannels(),
                    request.getCreatedBy(),
                    request.getUpdatedFrom(),
                    request.getUpdatedTo(),
                    request.getSearchFilter() == null ? null : request.getSearchFilter().toString(),
                    request.getPrompt()
            );

        } else {
            invalidDocuments = qesDocumentDetailsRepository.findInvalidDocumentsForCancel(request.getQesDocumentIds());
        }
        if (invalidDocuments) {
            throw new IllegalArgumentsProvidedException("Some documents can not be Cancelled!;");
        }


        List<QesDocument> qesDocuments;
        if (request.isAllSelected()) {
            qesDocuments = qesDocumentRepository.findQesDocumentsByFilter(
                    request.getExcludedIds(),
                    request.getStatus() == null ? List.of(QesStatus.ACTIVE.toString()) : request.getStatus().stream().map(Enum::toString).toList(),
                    request.getSigningStatuses() == null ? new ArrayList<>() : request.getSigningStatuses().stream().map(Enum::toString).toList(),
                    request.getPurposes() == null ? new ArrayList<>() : request.getPurposes().stream().map(Enum::toString).toList(),
                    request.getProductIds(),
                    request.getServiceIds(),
                    request.getPodIds(),
                    request.getSegments(),
                    request.getSaleChannels(),
                    request.getCreatedBy(),
                    request.getUpdatedFrom(),
                    request.getUpdatedTo(),
                    request.getSearchFilter() == null ? null : request.getSearchFilter().toString(),
                    request.getPrompt(),
                    LocalDateTime.now().minusDays(30)
            );
        } else {
            qesDocuments = qesDocumentRepository.findDocumentIdsByRequest(request.getQesDocumentIds());
        }
        List<Long> qesDocumentIds = qesDocuments.stream().map(QesDocument::getId).toList();

        qesDocumentDetailsRepository.deleteAllExceptOldestByQesDocumentIds(qesDocumentIds);

        List<QesDocumentDetails> documentDetails = qesDocumentDetailsRepository.findByQesDocumentIds(qesDocumentIds);

        for (QesDocumentDetails documentDetail : documentDetails) {
            documentDetail.setActive(true);
            qesDocumentDetailsRepository.save(documentDetail);
        }


        List<Long> documentIds = new ArrayList<>();

        for (QesDocument qesDocument : qesDocuments) {
            qesDocument.setSigningStatus(QesSigningStatus.TO_BE_SIGNED);
            qesDocument.setSignedQuantity(0);
            qesDocumentRepository.save(qesDocument);

            documentIds.add(qesDocument.getDocument_id());
        }
        Map<Long, Long> qesDocDocMap = qesDocuments.stream().collect(Collectors.toMap(QesDocument::getId, QesDocument::getDocument_id));

        List<Document> docs = documentsRepository.findDocumentsByIds(documentIds);
        Map<Long, Document> docMap = docs.stream().collect(Collectors.toMap(Document::getId, j -> j));
        for (QesDocumentDetails documentDetail : documentDetails) {
            Long docId = qesDocDocMap.get(documentDetail.getQesDocumentId());
            if (docId == null) {
                continue;
            }
            Document document = docMap.get(docId);
            if (document == null) {
                continue;
            }
            document.setDocumentStatus(DocumentStatus.UNSIGNED);
            document.setSignedFileUrl(documentDetail.getFtpPath());
            document.getSignedBy().remove(DocumentSigners.QES);
            documentsRepository.save(document);

            Long archiveDocumentId = document.getDocumentId();
            if (archiveDocumentId != null) {
                edmsManualArchivationService.deleteDocument(archiveDocumentId);
            }
        }
    }

    @Transactional
    public void updateQesAlias(QesAliasRequest request) {
        String loggedInUserId = permissionService.getLoggedInUserId();
        Optional<QesAlias> qesAliasOptional = qesAliasRepository.findById(loggedInUserId);
        if (qesAliasOptional.isPresent()) {
            QesAlias qesAlias = qesAliasOptional.get();
            qesAlias.setAlias(request.getAlias());
            qesAliasRepository.save(qesAlias);
        } else {
            QesAlias qesAlias = new QesAlias(loggedInUserId, request.getAlias());
            qesAliasRepository.save(qesAlias);
        }
    }

    public QesAliasResponse getQesAlias() {
        String loggedInUserId = permissionService.getLoggedInUserId();
        Optional<QesAlias> qesAlias = qesAliasRepository.findById(loggedInUserId);
        QesAliasResponse qesAliasResponse = new QesAliasResponse();
        qesAlias.ifPresent(x -> qesAliasResponse.setAlias(x.getAlias()));
        return qesAliasResponse;
    }

    private boolean saveSignedDoc(QesSignSession qesSignSession, File file, List<Document> documentIds) {
        AtomicReference<Document> documentAtomicReference = new AtomicReference<>();
        TransactionStatus execute = transactionTemplate.execute((x) -> {
            try {
                Long qesDocumentDetailsId = Long.valueOf(file.getName().substring(0, file.getName().lastIndexOf('.')));
                QesDocumentDetails qesDocumentDetails = qesDocumentDetailsRepository.findById(qesDocumentDetailsId)
                        .orElseThrow(() -> {
                            log.debug("Qes document details was not found {}!;", qesDocumentDetailsId);
                            return new ClientException(ErrorCode.APPLICATION_ERROR);
                        });
                QesDocument qesDocument = qesDocumentRepository.findById(qesDocumentDetails.getQesDocumentId()).orElseThrow();
                Document document = documentsRepository.findById(qesDocument.getDocument_id()).orElseThrow();
                documentAtomicReference.set(document);
                log.debug("Starting uploading file!;");
                String ftpFilePath = fileService.uploadFile(file, ftpPath, UUID.randomUUID() + ".pdf");
                log.debug("Upload successful;");
                qesDocumentDetails.setStatus(QesDocumentDetailsStatus.SIGNED);
                qesDocumentDetails.setFtpPath(ftpFilePath);
                Integer quantityToSign = qesDocument.getQuantityToSign();
                qesDocument.setSignedQuantity(qesDocument.getSignedQuantity() + 1);
                if (quantityToSign.equals(qesDocument.getSignedQuantity())) {
                    document.setDocumentStatus(DocumentStatus.SIGNED);
                    document.setSignedFileUrl(ftpFilePath);
                    qesDocument.setStatus(QesStatus.FINISHED);
                    document.getSignedBy().add(DocumentSigners.QES);
                    qesDocument.setSigningStatus(QesSigningStatus.FULLY_SIGNED);
                    signedDocumentFileArchivation.archiveSignedFile(document);
                } else {
                    qesDocument.setStatus(QesStatus.ACTIVE);
                    qesDocument.setSigningStatus(QesSigningStatus.PARTLY_SIGNED);
                }
                log.debug("Saving docs");

                qesDocumentRepository.save(qesDocument);
                qesDocumentDetailsRepository.save(qesDocumentDetails);
                qesSignSessionObjectsRepository.deleteByQesSignSessionIdAndQesDocumentDetailsId(qesSignSession.getId(), qesDocumentDetails.getId());
                documentsRepository.saveAndFlush(document);
                log.debug("Deleting file;");
                file.delete();
                log.debug("Deleted");
            } catch (Exception e) {
                log.debug("Exception was thrown, rollback");
                x.setRollbackOnly();
            }
            return x;
        });
        boolean isSuccess = !execute.isRollbackOnly();
        if (isSuccess) {
            documentIds.add(documentAtomicReference.get());
        }
        return isSuccess;
    }

    private void fileCleanup() {
        deleteFinishedFiles();
        deleteStuckFiles();
    }

    private void deleteStuckFiles() {
        //Todo delete stuck qes document details
        LocalDateTime dateTime = LocalDateTime.now().minusMinutes(7);
        log.debug("StuckDate: {}", dateTime);
        List<QesSignSession> stuckSessions = qesSignSessionRepository.findStuckSessions(dateTime);
        log.debug("StuckSessions found {}", stuckSessions.size());
        File sharedFolder = new File(sharedFolderPath);
        for (QesSignSession stuckSession : stuckSessions) {
            log.debug("StuckSessionStuckDate: {}", stuckSession.getModifyDate());
            File signedFolder = new File(sharedFolder, stuckSession.getSignedPath());
            File unsignedFolder = new File(sharedFolder, stuckSession.getUnsignedPath());
            FileSystemUtils.deleteRecursively(signedFolder);
            FileSystemUtils.deleteRecursively(unsignedFolder);

        }
        List<Long> stuckSessionIds = stuckSessions.stream().map(QesSignSession::getId).toList();
        undoQesDetailObjects(stuckSessionIds);
        qesSignSessionObjectsRepository.deleteAllByQesSignSessionIdIn(stuckSessionIds);
        qesSignSessionRepository.deleteAll(stuckSessions);
    }

    private void undoQesDetailObjects(List<Long> stuckSessionIds) {
        List<QesDocument> sessionDocuments = qesSignSessionRepository.findSessionDocuments(stuckSessionIds);
        qesSignSessionObjectsRepository.deleteInProgressQesDetails(stuckSessionIds);
        List<QesDocumentDetails> newestSignedDocs = qesDocumentDetailsRepository.findNewestSignedDocs(sessionDocuments.stream().map(QesDocument::getId).toList());
        for (QesDocumentDetails newestSignedDoc : newestSignedDocs) {
            newestSignedDoc.setActive(true);
            qesDocumentDetailsRepository.save(newestSignedDoc);
        }
        for (QesDocument sessionDocument : sessionDocuments) {
            sessionDocument.setStatus(QesStatus.ACTIVE);
            qesDocumentRepository.save(sessionDocument);
        }
    }

    private void deleteFinishedFiles() {
        List<QesSignSession> qesSignSessions = qesSignSessionRepository.findFinishedSessions();
        log.debug("QesSignSessionCount {}", qesSignSessions.size());
        File sharedFolder = new File(sharedFolderPath);
        for (QesSignSession qesSignSession : qesSignSessions) {
            File file = new File(sharedFolder, qesSignSession.getSignedPath());
            log.debug("QesSignSession folder found to delete!;");
            file.delete();
            log.debug("Folder deleted;");
            File unsignedFolder = new File(sharedFolder, qesSignSession.getUnsignedPath());
            FileSystemUtils.deleteRecursively(unsignedFolder);
        }
        qesSignSessionRepository.deleteAll(qesSignSessions);
    }


    /**
     * Starts the file transfer process for the QES signing request.
     *
     * @param request the QES signing request containing the necessary details
     * @param alias   the alias to be used for the signing process
     */
    @SneakyThrows
    private void startTransfer(QesStartSigningRequest request, String alias) {
        MDC.put("QesSessionId", request.getSessionId());
        log.debug("Request {}", request);

        String processId = String.format("%07d", qesDocumentDetailsRepository.nextProcessId());
        SaveSessionDto execute = transactionTemplate.execute((x) -> {
            log.debug("Starting saving qes sign session;");
            File sharedFolder = new File(sharedFolderPath);
            String signedFolderPathString = processId + "_signed";
            File signedPath = new File(sharedFolder, signedFolderPathString);
            signedPath.mkdir();
            log.debug("Signed folder path {}", signedFolderPathString);


            File unsignedPath = new File(sharedFolder, processId + "_unsigned");
            unsignedPath.mkdir();


            QesSignSession qesSignSession = new QesSignSession(null, signedPath.getName(), unsignedPath.getName(), request.getSessionId(), 0, 0);
            qesSignSessionRepository.saveAndFlush(qesSignSession);
            log.debug("Saved session {}", qesSignSession.getSessionId());
            return new SaveSessionDto(x, qesSignSession, unsignedPath, signedPath);
        });
        if (execute == null || execute.getTransactionStatus().isRollbackOnly()) {
            log.debug("execute is null or transaction status is rollbackOnly {}", execute == null);
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<QesDocumentDetails> obj = null;

        if (request.isAllSelected()) {
            obj = qesDocumentDetailsRepository.findByDocumentIdsAndFilter(
                    request.getExcludedIds(),
                    request.getStatus() == null ? List.of(QesStatus.ACTIVE.toString()) : request.getStatus().stream().map(Enum::toString).toList(),
                    request.getSigningStatuses() == null ? new ArrayList<>() : request.getSigningStatuses().stream().map(Enum::toString).toList(),
                    request.getPurposes() == null ? new ArrayList<>() : request.getPurposes().stream().map(Enum::toString).toList(),
                    request.getProductIds(),
                    request.getServiceIds(),
                    request.getPodIds(),
                    request.getSegments(),
                    request.getSaleChannels(),
                    request.getCreatedBy(),
                    request.getUpdatedFrom(),
                    request.getUpdatedTo(),
                    request.getSearchFilter() == null ? null : request.getSearchFilter().toString(),
                    request.getPrompt()
            );
        } else {
            obj = qesDocumentDetailsRepository.findByDocumentIds(request.getDocumentIds());
        }


        log.debug("documentDetailCount {}", obj.size());
        QesSignSession qesSignSession = execute.getQesSignSession();
        List<Callable<Boolean>> callables = getCallables(obj, qesSignSession, execute.getUnsignedPath(), processId);
        log.debug("Starting invoking callables {}", callables.size());
        List<Future<Boolean>> finishedTasks = executorService.invokeAll(callables);
        List<Future<Boolean>> list = finishedTasks.stream().filter(x -> {
            try {
                return Boolean.TRUE.equals(x.get());
            } catch (Exception e) {
                return false;
            }
        }).toList();

        log.debug("Success count {}", list.size());
        if (list.isEmpty()) {
            log.debug("All transfer failed");
            messagingTemplate.convertAndSend(signFinishQueue.formatted(qesSignSession.getSessionId()), new QesSigningStatusUpdateResponse(0, 0, 0, 0));
        }

        transactionTemplate.executeWithoutResult((x) -> {
            log.debug("Saving total sign count for session {}", qesSignSession.getSessionId());
            qesSignSession.setTotalCount(qesSignSessionObjectsRepository.countAllByQesSignSessionId(qesSignSession.getId()));
            qesSignSessionRepository.save(qesSignSession);
        });
        QesSigningResponse qesSigningResponse = new QesSigningResponse(jsignUrl.formatted(
                URLEncoder.encode(sharedFolderGlobalPath + execute.getUnsignedPath().getName() + "/", StandardCharsets.UTF_8),
                URLEncoder.encode(sharedFolderGlobalPath + execute.getSignedPath().getName(), StandardCharsets.UTF_8),
                URLEncoder.encode(alias, StandardCharsets.UTF_8)
        ));

        log.debug("qesUrl {}", qesSigningResponse.getSignerUrl());
        log.debug("absolutePath {}", execute.getUnsignedPath().getAbsolutePath());
        messagingTemplate.convertAndSend(signingUpdateQueue.formatted(qesSignSession.getSessionId()), new QesSigningStatusUpdateResponse(list.size(), obj.size(), 0, 0));
        messagingTemplate.convertAndSend(transferFinishQueue.formatted(request.getSessionId()), qesSigningResponse);
    }

//    private List<QesDocumentDetails> saveSessionObjects(List<Object[]> documentDetails, QesSignSession qesSignSession, Long processId) {
//        List<QesDocumentDetails> newDocDetails = new ArrayList<>();
//        for (Object[] objects : documentDetails) {
//            QesDocumentDetails documentDetail = (QesDocumentDetails) objects[0];
//            String ftpPath = documentDetail.getFtpPath();
//            QesDocumentDetails qesDocumentDetails = new QesDocumentDetails(null, documentDetail.getQesDocumentId(), String.format("%07d", processId), QesDocumentDetailsStatus.IN_PROGRESS, ftpPath, true);
//            qesDocumentDetailsRepository.saveAndFlush(qesDocumentDetails);
//            qesSignSessionObjectsRepository.save(new QesSignSessionObjects(null, qesSignSession.getId(), qesDocumentDetails));
//
//            documentDetail.setActive(false);
//            qesDocumentDetailsRepository.save(documentDetail);
//            newDocDetails.add(qesDocumentDetails);
//        }
//        return newDocDetails;
//    }


    /**
     * Generates a list of Callable tasks that can be used to process a list of document details.
     * It downloads the document from the FTP server, signs it, and saves it to the signed folder.
     * It also updates the QesDocumentDetails table with the status of the processing.
     * It also creates QesSignSessionObjects for each document detail that should be managed for current sign session;
     *
     * @param documentDetails the list of document details to process
     * @param qesSignSession  the QesSignSession associated with the processing
     * @param unsignedPath    the path where unsigned documents will be written
     * @param processId       the ID of the process associated with the processing
     * @return a list of Callable tasks that can be used to process the document details
     */
    private List<Callable<Boolean>> getCallables(List<QesDocumentDetails> documentDetails, QesSignSession qesSignSession, File unsignedPath, String processId) {
        List<Callable<Boolean>> callables = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        String qesSessionId = MDC.get("QesSessionId");
        for (QesDocumentDetails oldDocumentDetail : documentDetails) {
            callables.add(() -> {


                TransactionStatus execute = transactionTemplate.execute((x) -> {
                    MDC.put("QesSessionId", qesSessionId);
                    log.debug("Saving Qes document details and qes document;");
                    QesDocument qesDocument = qesDocumentRepository.findById(oldDocumentDetail.getQesDocumentId()).orElseThrow();
                    qesDocument.setStatus(QesStatus.IN_PROGRESS);
                    qesDocumentRepository.save(qesDocument);
                    log.debug("Updated qes document to IN Progress");
                    String ftpPath = oldDocumentDetail.getFtpPath();
                    QesDocumentDetails newDocumentDetail = new QesDocumentDetails(null, oldDocumentDetail.getQesDocumentId(), processId, QesDocumentDetailsStatus.IN_PROGRESS, ftpPath, true);
                    qesDocumentDetailsRepository.saveAndFlush(newDocumentDetail);
                    qesSignSessionObjectsRepository.save(new QesSignSessionObjects(null, qesSignSession.getId(), newDocumentDetail));
                    log.debug("Saved new qes document detail");
                    oldDocumentDetail.setActive(false);
                    qesDocumentDetailsRepository.save(oldDocumentDetail);
                    log.debug("Updated old qes document detail");

                    //Finished status should be implemented
                    log.debug("Starting file transfer");
                    ByteArrayResource byteArrayResource = fileService.downloadFile(newDocumentDetail.getFtpPath());
                    try {
                        Files.write(new File(unsignedPath, newDocumentDetail.getId().toString() + ".pdf").toPath(), byteArrayResource.getByteArray(), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        x.setRollbackOnly();
                        log.debug("Failed to write document in folder!;");
                    }
                    return x;
                });
                log.debug("File transfer completed!;");
                if (execute != null && !execute.isRollbackOnly()) {
                    int transferredCount = successCount.incrementAndGet();
                    if (transferredCount % batchSize == 0) {
                        QesSigningStatusUpdateResponse payload = new QesSigningStatusUpdateResponse(transferredCount, documentDetails.size(), 0, 0);
                        messagingTemplate.convertAndSend(signingUpdateQueue.formatted(qesSignSession.getSessionId()), payload);
                        log.debug("Sent signing update {}", payload);
                    }
                    return true;
                }
                return false;

            });
        }
        return callables;
    }

    public void deleteUntrackedFolders() {
        File sharedFolder = new File(sharedFolderPath);
        Set<String> trackedFolders = qesSignSessionRepository.findAll().stream().flatMap(x -> Stream.of(x.getSignedPath(), x.getUnsignedPath())).collect(Collectors.toSet());
        File[] files = sharedFolder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!trackedFolders.contains(file.getName())) {
                FileSystemUtils.deleteRecursively(file);
            }
        }
    }


    @Getter
    @AllArgsConstructor
    private static class SaveSessionDto {
        private final TransactionStatus transactionStatus;
        private final QesSignSession qesSignSession;
        private final File unsignedPath;
        private final File signedPath;

    }


}
