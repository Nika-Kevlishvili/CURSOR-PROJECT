package bg.energo.phoenix.service.billing.billingRun.actions.startGeneration;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.billing.billingRun.BillingRunDocumentCreationService;
import bg.energo.phoenix.service.billing.billingRun.BillingRunDocumentDataCreationService;
import bg.energo.phoenix.service.billing.billingRun.actions.BillingRunActionInvoker;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingRunErrorService;
import bg.energo.phoenix.service.billing.billingRun.errors.InvoiceErrorShortObject;
import bg.energo.phoenix.service.billing.invoice.reversal.ReversalValidationObject;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.signing.SignerChainManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStartGenerationInvokeService implements BillingRunActionInvoker {
    private final FileService fileService;
    private final InvoiceRepository invoiceRepository;
    private final DocumentsRepository documentsRepository;
    private final TransactionTemplate transactionTemplate;
    private final BillingRunRepository billingRunRepository;
    private final BillingRunErrorService billingRunErrorService;
    private final FileArchivationService fileArchivationService;
    private final EDMSAttributeProperties edmsAttributeProperties;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ContractTemplateDetailsRepository templateDetailsRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final BillingRunDocumentCreationService billingRunDocumentCreationService;
    private final BillingRunDocumentDataCreationService billingRunDocumentDataCreationService;
    private final SignerChainManager signerChainManager;
    private final int queryBatchSize = 20_000;
    @Value("${invoice.document.generation.max_thread_count}")
    private Integer maxThreadCount;
    @Value("${invoice.document.generation.batch_size}")
    private Integer batchSize;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invoke(BillingRun billingRun) {
        LocalDate billingDate = LocalDate.now();

        Pair<String, ContractTemplateDetail> contractTemplateData = billingRunDocumentCreationService.downloadBillingRunDefaultTemplate(billingDate, billingRun);

        Long billingRunId = billingRun.getId();

        CompanyDetailedInformationModel companyDetailedInformation = billingRunDocumentDataCreationService.fetchCompanyDetailedInformationModel(billingDate);
        byte[] companyLogoContent = billingRunDocumentDataCreationService.fetchCompanyLogoContent(companyDetailedInformation);

        MDC.put("billingNumber", billingRun.getBillingNumber());

        if (Objects.equals(BillingType.INVOICE_REVERSAL, billingRun.getType())) {
            try {
                List<ReversalValidationObject> objectForDraft = invoiceRepository.findAllValidObjectForDraft(billingRunId);
                List<InvoiceErrorShortObject> errorMessages = new ArrayList<>();
                List<Long> invalidInvoices = new ArrayList<>();
                invalidInvoices.add(-1L);
                for (ReversalValidationObject reversalValidationObject : objectForDraft) {
                    if (reversalValidationObject.getReversalId() == null) {
                        errorMessages.add(new InvoiceErrorShortObject(reversalValidationObject.getInvoiceNumber(), "Invoice with number %s should be marked for pdf generation!;".formatted(reversalValidationObject.getInvoiceNumber())));
                        invalidInvoices.add(reversalValidationObject.getInvoiceId());
                    }
                }
                List<Invoice> allValidInvoicesForGeneratingPDFDocument = invoiceRepository.findAllValidInvoicesForGeneratingPDFReversalDocument(billingRunId, invalidInvoices);

                if (allValidInvoicesForGeneratingPDFDocument.isEmpty()) {
                    publishNotification(billingRunId, NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
                    log.error("No valid invoices found to generate in billing run;");
                    throw new IllegalArgumentsProvidedException("No valid invoices found to generate in billing run;");
                }

                int threadCount = Math.min((allValidInvoicesForGeneratingPDFDocument.size() / batchSize) + 1, maxThreadCount);

                log.debug("Creating executor service with: batchSize [%s] and thread count: [%s]".formatted(batchSize, threadCount));

                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

                for (Invoice invoice : allValidInvoicesForGeneratingPDFDocument) {
                    executorService.submit(() -> {
                                try {
                                    return generatePdf(billingRun, contractTemplateData, invoice, billingDate, companyDetailedInformation, companyLogoContent);
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                    billingRunErrorService.publishBillingErrors(List.of(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), e.getMessage())), billingRunId, billingRun.getStatus());
                                    return Pair.of(false, e.getMessage());
                                }
                            }
                    );
                }

                finalizeExecutorService(executorService);

                if (!errorMessages.isEmpty()) {
                    billingRunErrorService.publishBillingErrors(errorMessages, billingRunId, billingRun.getStatus());
                    publishNotification(billingRunId, NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
                }
            } catch (Exception e) {
                log.error("Exception handled while trying to generate INVOICE_REVERSAL documents", e);
                publishNotification(billingRunId, NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
            }
        } else {
            long draftInvoiceCount = invoiceRepository.countInvoicesForDocumentGeneration(billingRunId);
            log.debug("Counting draft invoices: [%s]".formatted(draftInvoiceCount));

            int threadCount = Math.min((int) ((draftInvoiceCount / batchSize) + 1), maxThreadCount);

            if (draftInvoiceCount > 0) {
                log.debug("Creating executor service with: batchSize [%s] and thread count: [%s]".formatted(batchSize, threadCount));

                Queue<Callable<Pair<Boolean, String>>> callableContext = new LinkedList<>();
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

                try {
                    List<Invoice> invoicesForDocumentGenerationContext = new ArrayList<>();

                    long numberOfPages = (draftInvoiceCount / queryBatchSize) + 1;

                    for (int i = 0; i < numberOfPages; i++) {
                        List<Invoice> invoices = invoiceRepository
                                .findInvoicesForDocumentGeneration(
                                        billingRunId,
                                        PageRequest.of(i, queryBatchSize)
                                ).stream()
                                .toList();

                        if (CollectionUtils.isNotEmpty(invoices)) {
                            invoicesForDocumentGenerationContext.addAll(invoices);
                        }
                    }

                    if (billingRun.getType().equals(BillingType.STANDARD_BILLING) && billingRun.getApplicationModelType().contains(ApplicationModelType.INTERIM_AND_ADVANCE_PAYMENT)) {
                        invoicesForDocumentGenerationContext = getValidInvoices(billingRun, invoicesForDocumentGenerationContext);
                    }
                    log.debug("Total number of invoices for generation: {}", invoicesForDocumentGenerationContext.size());

                    for (Invoice invoice : invoicesForDocumentGenerationContext) {
                        callableContext.add(() -> {
                                    try {
                                        return generatePdf(billingRun, contractTemplateData, invoice, billingDate, companyDetailedInformation, companyLogoContent);
                                    } catch (Exception e) {
                                        log.error(e.getMessage());
                                        billingRunErrorService.publishBillingErrors(List.of(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), e.getMessage())), billingRunId, billingRun.getStatus());
                                        return Pair.of(false, e.getMessage());
                                    }
                                }
                        );

                        log.debug("Batch added to execution queue");
                    }

                    log.debug("Executing generation process");
                    try {
                        List<Future<Pair<Boolean, String>>> futures = executorService.invokeAll(callableContext);
                        int doneFuturesSize = futures.stream().filter(Future::isDone).toList().size();
                        log.debug("Total callables number was: {}, Processed callables number: {}, Done Futures size: {}", callableContext.size(), futures.size(), doneFuturesSize);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        billingRunErrorService.publishBillingErrors(List.of(new InvoiceErrorShortObject(null, e.getMessage())), billingRunId, billingRun.getStatus());
                    }
                    log.debug("Generation process finished");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    billingRunErrorService.publishBillingErrors(List.of(new InvoiceErrorShortObject(null, e.getMessage())), billingRunId, billingRun.getStatus());
                } finally {
                    finalizeExecutorService(executorService);
                }
            }
        }

        MDC.popByKey("billingNumber");
    }

    private List<Invoice> getValidInvoices(BillingRun billingRun, List<Invoice> invoicesForDocumentGenerationContext) {
        List<Invoice> validInvoices = new ArrayList<>();
        Set<Long> invoiceIdSet = invoicesForDocumentGenerationContext.stream().map(Invoice::getId).collect(Collectors.toSet());
        List<InvoiceErrorShortObject> errors = new ArrayList<>();
        for (Invoice invoice : invoicesForDocumentGenerationContext) {
            if (invoice.getInvoiceType().equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT) && invoice.getInterimCalculatedFromInvoiceId() != null && !invoiceIdSet.contains(invoice.getInterimCalculatedFromInvoiceId())) {
                Optional<String> interimErrorObject = invoiceRepository.getInterimErrorObject(invoice.getId());
                errors.add(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), "PDF is not generated for interim (%s) for this contract %s".formatted(invoice.getInvoiceNumber(),interimErrorObject.orElse("") )));
            } else {
                validInvoices.add(invoice);
            }
        }
        billingRunErrorService.publishBillingErrors(errors, billingRun.getId(), billingRun.getStatus());
        return validInvoices;
    }

    private void finalizeExecutorService(ExecutorService executorService) {
        log.debug("Shutting down executor service");
        executorService.shutdown();

        log.debug("Waiting for executor to finish");
        try {
            boolean finished = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            log.debug("Executor finished: {}", finished);
        } catch (InterruptedException e) {
            log.error("Exception handled while waiting for executor to finish", e);
        }
    }

    private Pair<Boolean, String> generatePdf(BillingRun billingRun,
                                              Pair<String, ContractTemplateDetail> contractTemplateData,
                                              Invoice invoice,
                                              LocalDate billingDate,
                                              CompanyDetailedInformationModel companyDetailedInformation,
                                              byte[] companyLogoContent) throws Exception {
        Pair<Invoice, Document> generated = billingRunDocumentCreationService
                .generate(
                        billingDate,
                        billingRun,
                        invoice,
                        contractTemplateData,
                        companyDetailedInformation,
                        companyLogoContent
                );
        transactionTemplate.executeWithoutResult(x -> {
            log.debug("GenerationFinishedDocName: {}", generated.getValue().getName());
            Document value = generated.getValue();
            if (value.getName().contains("%s")) {
                Invoice inv = invoiceRepository.findById(generated.getKey().getId()).orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found!;"));
                entityManager.refresh(inv);
                log.debug("GenerationFinishedInvoiceNum: {}", inv.getInvoiceNumber());
                String replacement = inv.getInvoiceStatus() == InvoiceStatus.CANCELLED ? inv.getInvoiceCancellationNumber() : inv.getInvoiceNumber();
                log.debug("GenerationFinishedReplacement: {}", replacement);
                value.setName(value.getName().replace("%s", replacement));
                log.debug("GenerationFinishedFinal: {}", value.getName());
            }
            documentsRepository.saveAndFlush(value);
        });
        return Pair.of(true, "Success");
    }

    protected void publishNotification(Long billingRunId, NotificationType notificationType, NotificationState notificationState) {
        notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, notificationType, billingRunRepository, notificationState));
    }
}
