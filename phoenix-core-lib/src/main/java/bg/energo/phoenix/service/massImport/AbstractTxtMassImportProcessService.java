package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.process.BaseProcessHandler;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageStatusChangeHistoryRepository;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.mapper.ProcessNotificationMapper;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class AbstractTxtMassImportProcessService extends BaseProcessHandler {
    protected final ProcessRepository processRepository;
    protected final PaymentPackageRepository paymentPackageRepository;
    protected final NotificationEventPublisher notificationEventPublisher;
    protected final CollectionChannelRepository collectionChannelRepository;
    protected final ProcessedRecordInfoRepository processRecordInfoRepository;
    protected final PaymentPackageStatusChangeHistoryRepository paymentPackageStatusChangeHistoryRepository;
    protected final TransactionTemplate transactionTemplate;

    public AbstractTxtMassImportProcessService(NotificationEventPublisher notificationEventPublisher,
                                               ProcessRepository processRepository,
                                               ProcessedRecordInfoRepository processRecordInfoRepository,
                                               PaymentPackageRepository paymentPackageRepository,
                                               CollectionChannelRepository collectionChannelRepository,
                                               PaymentPackageStatusChangeHistoryRepository paymentPackageStatusChangeHistoryRepository,
                                               TransactionTemplate transactionTemplate) {
        this.notificationEventPublisher = notificationEventPublisher;
        this.processRepository = processRepository;
        this.paymentPackageRepository = paymentPackageRepository;
        this.processRecordInfoRepository = processRecordInfoRepository;
        this.collectionChannelRepository = collectionChannelRepository;
        this.paymentPackageStatusChangeHistoryRepository = paymentPackageStatusChangeHistoryRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void startFileProcessing(ByteArrayResource file, Long processId) {
        Process process = processRepository
                .findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));
        try {
            importTxtFile(file, process);
        } catch (Exception e) {
            log.error("Error while importing txt file for process", e);
            publishNotifications(process, NotificationState.ERROR);
        } finally {
            onFinish(process);
        }
    }

    public void importTxtFile(ByteArrayResource file, Process process) throws IOException, InterruptedException {
        Long processId = process.getId();
        int numberOfCallablesPerThread = getNumberOfCallablesPerThread();
        int numberOfRowsPerTsk = getNumberOfRowsPerTask();

        if (file == null) {
            log.debug("Provided file is null, Stopping process!");
            throw new ClientException("Provided file is null;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        log.debug("Process found!");
        Boolean currencyFromCollectionChannel = process.getCurrencyFromCollectionChannel();
        Long collectionChannelId = process.getCollectionChannelId();
        CollectionChannel collectionChannel = collectionChannelRepository.findById(collectionChannelId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Collection channel not found with id " + collectionChannelId));
        Long paymentPackageId = process.getPaymentPackageId();
        log.info("Process Permissions----------------------" + process.getUserPermissions());
        ProcessStatus status = process.getStatus();
        if (status == ProcessStatus.CANCELED
                || status == ProcessStatus.COMPLETED
                || status == ProcessStatus.IN_PROGRESS) {
            log.debug("Status of ");
            throw new ClientException("Process status is " + status.name() + ";", ErrorCode.CONFLICT);
        }
        if (process.getStatus() == ProcessStatus.NOT_STARTED) {
            process.setProcessStartDate(LocalDateTime.now());
        }
        process.setStatus(ProcessStatus.IN_PROGRESS);
        processRepository.save(process);

        byte[] bytes = file.getByteArray();
        String encoding = detectEncoding(bytes);
        String txtFile = new String(bytes, encoding);

        boolean isBankFile = isBankFile(txtFile);
        List<Map<String, String>> parsedData;
        Map<String, String> bankFileMetadata;
        List<String> errorMessages = new ArrayList<>();

        if (isBankFile) {
            bankFileMetadata = parseBankFileMetadata(txtFile, errorMessages);
            validateRequest(errorMessages, bankFileMetadata);
            List<Integer> recordDelimiters = findBankFileRecordDelimiters(txtFile);
            parsedData = parseBankFile(txtFile, recordDelimiters, errorMessages);
            validateRequest(errorMessages, parsedData);
        } else {
            bankFileMetadata = null;
            parsedData = parsePaymentPartnerFile(txtFile);
        }

        LocalDateTime start = LocalDateTime.now();
        int numberOfRows = parsedData.size();
        ExecutorService threadPool = Executors.newFixedThreadPool(getNumberOfThreads());
        long processStartIndex = 0;
        Optional<ProcessedRecordInfo> optional = processRecordInfoRepository.findFirstByProcessIdOrderByRecordIdDesc(processId);
        if (optional.isPresent()) {
            processStartIndex = optional.get().getRecordId();
        }
        String processSysUserId = process.getSystemUserId();
        LocalDate processDate = process.getDate();
        Set<String> permissions = new HashSet<>(Arrays.asList(process.getUserPermissions().split(";")));

        while (processStartIndex < numberOfRows) {
            List<Callable<String>> callables = new ArrayList<>();
            for (long j = processStartIndex; j < processStartIndex + (long) numberOfCallablesPerThread * numberOfRowsPerTsk && j < numberOfRows; j += numberOfRowsPerTsk) {
                long startIndex = j;
                long endIndex = Math.min(j + numberOfRowsPerTsk, numberOfRows);
                Callable<String> callableTask = () -> processRecordBatch(
                        parsedData,
                        startIndex,
                        endIndex,
                        permissions,
                        processSysUserId,
                        processDate,
                        processId,
                        collectionChannelId,
                        isBankFile,
                        paymentPackageId,
                        bankFileMetadata,
                        errorMessages,
                        currencyFromCollectionChannel,
                        collectionChannel
                );
                callables.add(callableTask);
            }

            threadPool.invokeAll(callables);

            processStartIndex += (long) numberOfCallablesPerThread * numberOfRowsPerTsk;

            process = processRepository.findById(processId)
                    .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

            if (process.getStatus() == ProcessStatus.CANCELED
                    || process.getStatus() == ProcessStatus.PAUSED) {
                break;
            }
        }

        System.out.println("rows: " + numberOfRows);
        LocalDateTime end = LocalDateTime.now();
        System.out.println("Duration: " + Duration.between(start, end).toSeconds());


        process = processRepository.findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

        if (process.getStatus() == ProcessStatus.IN_PROGRESS) {
            process.setStatus(ProcessStatus.COMPLETED);
            process.setProcessCompleteDate(LocalDateTime.now());
            processRepository.save(process);
            if (processRecordInfoRepository.existsByProcessIdAndSuccess(processId, true)) {
                PaymentPackage paymentPackage = paymentPackageRepository
                        .findPaymentPackageByIdAndLockStatusIn(paymentPackageId, List.of(PaymentPackageLockStatus.UNLOCKED))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Payment package not found!;"));

                paymentPackage.setLockStatus(PaymentPackageLockStatus.LOCKED);
                paymentPackageRepository.save(paymentPackage);
            } else {
                log.info("Deleting payment package");
                process.setPaymentPackageId(null);
                processRepository.saveAndFlush(process);
                transactionTemplate.executeWithoutResult(y -> {
                    paymentPackageStatusChangeHistoryRepository.deleteAllByPaymentPackageId(paymentPackageId);
                    log.info("History deleted");
                    paymentPackageRepository.deleteById(paymentPackageId);
                    log.info("Payment package deleted");
                });

            }
        }
    }

    private String detectEncoding(byte[] bytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding != null ? encoding : StandardCharsets.UTF_8.name();
    }

    protected <T> void validateRequest(List<String> errorMessages, T request) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty() || !errorMessages.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String error : errorMessages) {
                stringBuilder.append(error);
            }
            for (ConstraintViolation<T> violation : violations) {
                stringBuilder.append(violation.getMessage());
            }
            throw new ClientException(stringBuilder.toString(), ErrorCode.CONFLICT);
        }
    }

    protected abstract boolean isBankFile(String txtFile);

    protected abstract Map<String, String> parseBankFileMetadata(String txtFile, List<String> errorMessages);

    protected abstract List<Integer> findBankFileRecordDelimiters(String txtFile);

    protected abstract List<Map<String, String>> parseBankFile(String txtFile, List<Integer> recordDelimiters, List<String> errorMessages);

    protected abstract List<Map<String, String>> parsePaymentPartnerFile(String txtFile);

    private String processRecordBatch(
            List<Map<String, String>> parsedData,
            long startIndex,
            long endIndex,
            Set<String> permissions,
            String processSysUserId,
            LocalDate processDate,
            Long processId,
            Long collectionChannelId,
            boolean isBankFile,
            Long paymentPackageId,
            Map<String, String> bankFileMetadata,
            List<String> errorMessages,
            Boolean currencyFromCollectionChannel,
            CollectionChannel collectionChannel
    ) {
        List<ProcessedRecordInfo> processedRecordInfos = new ArrayList<>();
        for (long k = startIndex; k < endIndex; k++) {
            Map<String, String> recordData = parsedData.get((int) k);
            ProcessedRecordInfo processedRecordInfo = new ProcessedRecordInfo();
            processedRecordInfo.setProcessId(processId);
            processedRecordInfo.setRecordId(k + 1);

            try {
                String identifier = getIdentifier(recordData, isBankFile);
                processedRecordInfo.setRecordIdentifier(identifier);

                if ("INVALID_61_FIELD".equals(identifier)) {
                    throw new IllegalArgumentException("Invalid :61: field in record");
                }

                ProcessedRecordInfo savedInfo = processRecordInfoRepository.save(processedRecordInfo);

                String recordIdentifierVersion = processRecord(
                        recordData,
                        permissions,
                        processSysUserId,
                        processDate,
                        savedInfo.getId(),
                        collectionChannelId,
                        isBankFile,
                        paymentPackageId,
                        bankFileMetadata,
                        currencyFromCollectionChannel,
                        collectionChannel
                );

                processedRecordInfo.setRecordIdentifierVersion(recordIdentifierVersion);
                processedRecordInfo.setSuccess(true);
            } catch (Exception e) {
                processedRecordInfo.setSuccess(false);
                processedRecordInfo.setErrorMessage(e.getMessage());
                errorMessages.add("Error processing record " + (k + 1) + ": " + e.getMessage());
            } finally {
                processedRecordInfos.add(processedRecordInfo);
            }
        }
        processRecordInfoRepository.saveAll(processedRecordInfos);
        return "Completed with " + errorMessages.size() + " errors";
    }


    protected abstract String processRecord(
            Map<String, String> recordData,
            Set<String> permissions,
            String processSysUserId,
            LocalDate date,
            Long processRecordInfo,
            Long collectionChannelId,
            boolean isBankFile,
            Long paymentPackageId,
            Map<String, String> bankFileMetadata,
            Boolean currencyFromCollectionChannel,
            CollectionChannel collectionChannel
    );

    protected abstract String getIdentifier(Map<String, String> recordData, boolean isBankFile);

    protected abstract int getNumberOfThreads();

    protected abstract int getNumberOfCallablesPerThread();

    protected abstract int getNumberOfRowsPerTask();

    protected void onFinish(Process process) {
        publishNotifications(process, NotificationState.COMPLETION);
    }

    protected void publishNotifications(Process process, NotificationState notificationState) {
        ProcessType processType = process.getType();

        NotificationType notificationType = ProcessNotificationMapper.mapToNotificationType(processType, notificationState);

        notificationEventPublisher.publishNotification(new NotificationEvent(process.getId(), notificationType, processRepository, notificationState));
    }
}
