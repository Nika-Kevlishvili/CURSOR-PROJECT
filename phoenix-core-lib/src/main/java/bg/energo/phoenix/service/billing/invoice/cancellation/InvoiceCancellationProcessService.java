package bg.energo.phoenix.service.billing.invoice.cancellation;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellation;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.process.BaseProcessHandler;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceCancellationInvoicesRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceCancellationRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.mapper.ProcessNotificationMapper;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceCancellationProcessService extends BaseProcessHandler {

    private final InvoiceCancellationInvoicesRepository invoiceCancellationInvoicesRepository;
    private final InvoiceCancellationRepository invoiceCancellationRepository;
    private final ProcessRepository processRepository;
    private final ProcessedRecordInfoRepository processedRecordInfoRepository;
    private final InvoiceCancellationService invoiceCancellationService;
    protected final NotificationEventPublisher notificationEventPublisher;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final FileService fileService;
    @Value("${app.cfg.customer.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.customer.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.customer.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    @Override
    public boolean supports(EventType eventType) {
        return EventType.INVOICE_CANCELLATION_PROCESS.equals(eventType);
    }

    @Override
    protected void startFileProcessing(ByteArrayResource file, Long processId) {

    }

    @Override
    @SneakyThrows
    protected void startCancellationProcessing(Long processId) {
        AtomicBoolean isExceptionHandled = new AtomicBoolean(false);

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

        InvoiceCancellation invoiceCancellation = invoiceCancellationRepository.findByProcessId(processId).orElseThrow();
        Pair<ContractTemplateDetail, byte[]> documentTemplateContent = fetchDocumentTemplateContentForCancellation(invoiceCancellation);;
        Map<Long, String> errorMessages = new HashMap<>();
        List<Long> invalidInterims = invoiceCancellationInvoicesRepository.findInvalidInterims(invoiceCancellation.getId());
        for (Long invalidInterim : invalidInterims) {
            if ( !errorMessages.containsKey(invalidInterim)) {
                errorMessages.put(invalidInterim, "Invoice can not be cancelled, because connected invoice for this interim invoice not in the cancellation list;");
            }
        }
        List<InvoiceCancellationShortDto> invalidDebitCreditInvoice = invoiceCancellationInvoicesRepository.findInvalidDebitCreditInvoice(invoiceCancellation.getId());
        for (InvoiceCancellationShortDto invoiceCancellationShortDto : invalidDebitCreditInvoice) {
            if (!invoiceCancellationShortDto.getValidInvoice() && !errorMessages.containsKey(invoiceCancellationShortDto.getBaseInvoiceId())) {
                errorMessages.put(invoiceCancellationShortDto.getBaseInvoiceId(), "Invoice can not be cancelled, because connected manual credit/debit note not in the cancellation list;");
            }
        }
        List<InvoiceCancellationDto> validInvoices = invoiceCancellationInvoicesRepository.findInvoiceToCancel(invoiceCancellation.getId());
        for (InvoiceCancellationDto invoiceCancellationShortDto : validInvoices) {
            if (!invoiceCancellationShortDto.getValidInvoice() && !errorMessages.containsKey(invoiceCancellationShortDto.getBaseInvoiceId())) {
                errorMessages.put(invoiceCancellationShortDto.getBaseInvoiceId(), "Invoice can not be cancelled, because invoice connected to this billing data is not in the cancellation list;");
            }
        }
        Map<Long, List<InvoiceCancellationDto>> validInvoiceMap = validInvoices.stream().collect(Collectors.groupingBy(InvoiceCancellationShortDto::getBaseInvoiceId));

        List<Invoice> invoices = invoiceCancellationInvoicesRepository.findInvoiceByCancellationId(invoiceCancellation.getId());
        int numberOfRows = invoices.size();
        long processStartIndex = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
        Optional<ProcessedRecordInfo> optional = processedRecordInfoRepository.findFirstByProcessIdOrderByRecordIdDesc(processId);
        if (optional.isPresent()) {
            processStartIndex = optional.get().getRecordId() + 1;
        }


        while (processStartIndex < numberOfRows) {
            List<Callable<String>> callables = new ArrayList<>();
            for (long j = processStartIndex; j < processStartIndex + (long) numberOfCallablesPerThread * numberOfRowsPerTsk && j < numberOfRows; j = j + numberOfRowsPerTsk) {
                Long jObject = j;
                Callable<String> callableTask = () -> {
                    List<ProcessedRecordInfo> processedRecordInfos = new ArrayList<>();
                    for (long k = jObject; k < jObject + numberOfRowsPerTsk && k < numberOfRows; k++) {
                        Invoice row = invoices.get((int) k);
                        if (row == null)
                            continue;
                        if(!row.getInvoiceStatus().equals(InvoiceStatus.REAL) )
                        {
                            errorMessages.put(row.getId(),"Invoice With this status can not be cancelled!;");
                        }
                        //TODO add validation check here
                        if(errorMessages.containsKey(row.getId())){
                            ProcessedRecordInfo processedRecordInfo = new ProcessedRecordInfo();
                            processedRecordInfo.setProcessId(processId);
                            long recordInfoId = k + 1;
                            processedRecordInfo.setRecordId(recordInfoId);
                            processedRecordInfo.setRecordIdentifier(row.getInvoiceNumber());
                            processedRecordInfo.setSuccess(false);
                            processedRecordInfo.setErrorMessage(errorMessages.get(row.getId()));
                            isExceptionHandled.set(true);
                            ProcessedRecordInfo savedInfo = processedRecordInfoRepository.save(processedRecordInfo);
                            continue;
                        }
                        ProcessedRecordInfo processedRecordInfo = new ProcessedRecordInfo();
                        processedRecordInfo.setProcessId(processId);
                        long recordInfoId = k + 1;
                        processedRecordInfo.setRecordId(recordInfoId);
                        processedRecordInfo.setRecordIdentifier(row.getInvoiceNumber());
                         processedRecordInfoRepository.save(processedRecordInfo);
                        processedRecordInfo.setSuccess(true);
                        try {
                            invoiceCancellationService.processInvoice(row, invoiceCancellation, process.getDate(), documentTemplateContent,validInvoiceMap.get(row.getId()));
                        } catch (Exception e) {
                            log.debug("Exception while processing row", e);
                            processedRecordInfo.setSuccess(false);
                            processedRecordInfo.setErrorMessage(e.getMessage());
                            isExceptionHandled.set(true);
                        } finally {
                            processedRecordInfos.add(processedRecordInfo);
                        }
                    }
                    processedRecordInfoRepository.saveAll(processedRecordInfos);

                    return "Completed Successfully";
                };
                callables.add(callableTask);
            }
            threadPool.invokeAll(callables);
            processStartIndex += (long) numberOfCallablesPerThread * numberOfRowsPerTsk;

            Process processSaved = processRepository.findById(processId)
                    .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

            if (processSaved.getStatus() == ProcessStatus.CANCELED
                    || processSaved.getStatus() == ProcessStatus.PAUSED) {
                break;
            }
        }
        Process processSaved = processRepository.findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

        if (Objects.equals(processSaved.getStatus(), ProcessStatus.IN_PROGRESS)) {
            processSaved.setStatus(ProcessStatus.COMPLETED);
            processSaved.setProcessCompleteDate(LocalDateTime.now());
            processRepository.save(processSaved);

        }
        if (isExceptionHandled.get()) {
            publishNotifications(process, NotificationState.ERROR);
        }
    }

    protected void publishNotifications(Process process, NotificationState notificationState) {
        ProcessType processType = process.getType();

        NotificationType notificationType = ProcessNotificationMapper.mapToNotificationType(processType, notificationState);

        notificationEventPublisher.publishNotification(new NotificationEvent(process.getId(), notificationType, processRepository, notificationState));
    }

    /**
     * Fetches the template content required for processing an invoice cancellation.
     * Retrieves the relevant contract template details and its associated file content.
     * If the content is cached, it fetches from the cache; otherwise, retrieves from the repositories.
     *
     * @param invoiceCancellation the invoice cancellation object containing necessary details
     *                            to locate the corresponding contract template.
     * @return a pair containing the contract template details and the file content as a byte array.
     * The first element of the pair is the {@code ContractTemplateDetail}, and the second
     * element is a byte array representing the template file content.
     * @throws DomainEntityNotFoundException if the respective template details or file content
     *                                       cannot be found.
     */
    private Pair<ContractTemplateDetail, byte[]> fetchDocumentTemplateContentForCancellation(InvoiceCancellation invoiceCancellation) {
        ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository
                .findRespectiveTemplateDetailsByTemplateIdAndDate(invoiceCancellation.getContractTemplateId(), LocalDate.now())
                .orElseThrow(() -> new DomainEntityNotFoundException("Template detail respective version not found to generate cancellation document"));

        ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective template detail file not found"));

        ByteArrayResource templateFileContent = fileService.downloadFile(contractTemplateFile.getFileUrl());

        return Pair.of(contractTemplateDetail, templateFileContent.getByteArray());
    }
}