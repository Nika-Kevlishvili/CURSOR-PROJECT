package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.process.BaseProcessHandler;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.mapper.ProcessNotificationMapper;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractMassImportProcessService extends BaseProcessHandler {
    protected static String SHEET = "Sheet1";
    protected final ProcessRepository processRepository;
    protected final ProcessedRecordInfoRepository processRecordInfoRepository;
    protected final NotificationEventPublisher notificationEventPublisher;

    public AbstractMassImportProcessService(ProcessRepository processRepository,
                                            ProcessedRecordInfoRepository processRecordInfoRepository,
                                            NotificationEventPublisher notificationEventPublisher) {
        this.processRepository = processRepository;
        this.processRecordInfoRepository = processRecordInfoRepository;
        this.notificationEventPublisher = notificationEventPublisher;
    }


    @Override
    protected void startFileProcessing(ByteArrayResource file, Long processId) {
        try {
            importFile(file, processId);
        } catch (Exception e) {
            log.error("Error while importing file for process", e);
        }
    }

    public void importFile(ByteArrayResource file, Long processId) throws IOException, InterruptedException {
        AtomicBoolean isExceptionHandled = new AtomicBoolean(false);
        int numberOfCallablesPerThread = getNumberOfCallablesPerThread();
        int numberOfRowsPerTsk = getNumberOfRowsPerTask();
        if (file == null) throw new ClientException("Provided file is null;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);

        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));
        log.info("Process Permissions---------------------------------------------" + process.getUserPermissions());
        ProcessStatus status = process.getStatus();
        if (status == ProcessStatus.CANCELED
                || status == ProcessStatus.COMPLETED
                || status == ProcessStatus.IN_PROGRESS) {
            throw new ClientException("Process status is " + status.name() + ";", ErrorCode.CONFLICT);
        }

        if (process.getStatus() == ProcessStatus.NOT_STARTED) {
            process.setProcessStartDate(LocalDateTime.now());
        }
        process.setStatus(ProcessStatus.IN_PROGRESS);
        processRepository.save(process);

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheet(SHEET);
        LocalDateTime start = LocalDateTime.now();
        int numberOfRows = sheet.getPhysicalNumberOfRows();
        ExecutorService threadPool = Executors.newFixedThreadPool(getNumberOfThreads());
        long processStartIndex = 1;
        Optional<ProcessedRecordInfo> optional = processRecordInfoRepository.findFirstByProcessIdOrderByRecordIdDesc(processId);
        if (optional.isPresent()) {
            processStartIndex = optional.get().getRecordId() + 1;
        }
        String processSysUserId = process.getSystemUserId();
        LocalDate processDate = process.getDate();
        Set<String> permissions = Arrays.stream(process.getUserPermissions().split(";")).collect(Collectors.toSet());
        while (processStartIndex < numberOfRows) {

            List<Callable<String>> callables = new ArrayList<>();
            for (long j = processStartIndex; j < processStartIndex + (long) numberOfCallablesPerThread * numberOfRowsPerTsk && j < numberOfRows; j = j + numberOfRowsPerTsk) {
                Long jObject = j;
                Callable<String> callableTask = () -> {
                    List<ProcessedRecordInfo> processedRecordInfos = new ArrayList<>();
                    for (long k = jObject; k < jObject + numberOfRowsPerTsk && k < numberOfRows; k++) {
                        Row row = sheet.getRow((int) k);
                        if (row == null)
                            continue;
                        ProcessedRecordInfo processedRecordInfo = new ProcessedRecordInfo();
                        processedRecordInfo.setProcessId(processId);
                        long recordInfoId = k + 1;
                        processedRecordInfo.setRecordId(recordInfoId);
                        processedRecordInfo.setRecordIdentifier(getIdentifier(row));
                        ProcessedRecordInfo savedInfo = processRecordInfoRepository.save(processedRecordInfo);
                        processedRecordInfo.setSuccess(true);
                        try {
                            String recordIdentifierVersion = processRow(row, permissions, processSysUserId, processDate, savedInfo.getId());
                            processedRecordInfo.setRecordIdentifierVersion(String.valueOf(recordIdentifierVersion));
                        } catch (Exception e) {
                            log.debug("Exception while processing row", e);
                            processedRecordInfo.setSuccess(false);
                            processedRecordInfo.setErrorMessage(e.getMessage());
                            isExceptionHandled.set(true);
                        } finally {
                            processedRecordInfos.add(processedRecordInfo);
                        }
                    }
                    processRecordInfoRepository.saveAll(processedRecordInfos);

                    return "Completed Successfully";
                };
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
        onFinish(process);

        System.out.println("rows: " + numberOfRows);
        LocalDateTime end = LocalDateTime.now();
        System.out.println("Duration: " + Duration.between(start, end).toSeconds());

        process = processRepository.findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

        if (Objects.equals(process.getStatus(), ProcessStatus.IN_PROGRESS)) {
            process.setStatus(ProcessStatus.COMPLETED);
            process.setProcessCompleteDate(LocalDateTime.now());
            processRepository.save(process);

            onCompletion(process);
        }

        if (isExceptionHandled.get()) {
            publishNotifications(process, NotificationState.ERROR);
        }
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

    protected abstract String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo);

    protected abstract String getIdentifier(Row row);

    protected abstract int getNumberOfThreads();

    protected abstract int getNumberOfCallablesPerThread();

    protected abstract int getNumberOfRowsPerTask();

    protected void onFinish(Process process) {

    }

    protected void onCompletion(Process process) {
        log.debug("Starting publishing notification");
        publishNotifications(process, NotificationState.COMPLETION);
    }

    protected void publishNotifications(Process process, NotificationState notificationState) {
        ProcessType processType = process.getType();

        NotificationType notificationType = ProcessNotificationMapper.mapToNotificationType(processType, notificationState);

        notificationEventPublisher.publishNotification(new NotificationEvent(process.getId(), notificationType, processRepository, notificationState));
    }
}
