package bg.energo.phoenix.service.receivable.reminder;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomerContacts;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomers;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.receivable.reminder.Reminder;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderDocumentFile;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderProcessItem;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.crm.smsCommunication.SmsSendParamBase;
import bg.energo.phoenix.process.BaseProcessHandler;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationTemplateRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomerContactsRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomersRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.SmsSendingNumberRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderDocumentFileRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderProcessItemRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationSenderService;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.crm.smsCommunication.SmsCommunicationSendHelperService;
import bg.energo.phoenix.service.crm.smsCommunication.SmsCommunicationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.ReminderDocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.documentMergerService.DocumentMergerService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.mapper.ProcessNotificationMapper;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ReminderProcessService extends BaseProcessHandler {

    private static final String FOLDER_PATH = "mass_email_document";
    private static final String REQUEST_ID_PREFIX = "EPROES";
    protected final ProcessRepository processRepository;
    protected final ProcessedRecordInfoRepository processRecordInfoRepository;
    private final ReminderProcessItemRepository reminderProcessItemRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final ReminderRepository reminderRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final ReminderDocumentFileRepository reminderDocumentFileRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final SmsCommunicationService smsCommunicationService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final EmailCommunicationSenderService emailCommunicationSenderService;
    private final SmsSendingNumberRepository smsSendingNumberRepository;
    private final SmsCommunicationRepository smsCommunicationRepository;
    private final SmsCommunicationCustomerContactsRepository smsCommunicationCustomerContactsRepository;
    private final SmsCommunicationCustomersRepository smsCommunicationCustomersRepository;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final DocumentMergerService documentMergerService;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final SmsCommunicationSendHelperService smsCommunicationSendHelperService;
    private final PermissionService permissionService;
    private final AccountManagerRepository accountManagerRepository;
    private final ReminderDocumentGenerationService reminderDocumentGenerationService;
    private final DocumentGenerationService documentGenerationService;
    private final InvoiceRepository invoiceRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    @Value("${ftp.server.base.path}")
    protected String ftpBasePath;
    @Value("${app.cfg.reminder.process.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.reminder.process.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.reminder.process.numberOfThreads}")
    private Integer numberOfThreads;

    public ReminderProcessService(ProcessRepository processRepository,
                                  ProcessedRecordInfoRepository processRecordInfoRepository,
                                  ReminderProcessItemRepository reminderProcessItemRepository,
                                  NotificationEventPublisher notificationEventPublisher, EmailMailboxesRepository emailMailboxesRepository,
                                  TopicOfCommunicationRepository topicOfCommunicationRepository,
                                  ReminderRepository reminderRepository,
                                  ContractTemplateRepository contractTemplateRepository,
                                  EmailCommunicationRepository emailCommunicationRepository,
                                  EmailCommunicationTemplateRepository emailCommunicationTemplateRepository,
                                  EmailCommunicationService emailCommunicationService,
                                  SmsCommunicationService smsCommunicationService,
                                  CustomerLiabilityRepository customerLiabilityRepository,
                                  CustomerCommunicationsRepository customerCommunicationsRepository,
                                  EmailCommunicationSenderService emailCommunicationSenderService,
                                  SmsSendingNumberRepository smsSendingNumberRepository,
                                  SmsCommunicationRepository smsCommunicationRepository,
                                  SmsCommunicationCustomersRepository smsCommunicationCustomersRepository,
                                  SmsCommunicationCustomerContactsRepository smsCommunicationCustomerContactsRepository,
                                  ProductContractDetailsRepository productContractDetailsRepository,
                                  CustomerDetailsRepository customerDetailsRepository,
                                  CustomerCommunicationContactsRepository customerCommunicationContactsRepository,
                                  AccountManagerRepository accountManagerRepository,
                                  SmsCommunicationSendHelperService smsCommunicationSendHelperService,
                                  InvoiceRepository invoiceRepository,
                                  ReminderDocumentGenerationService reminderDocumentGenerationService,
                                  PermissionService permissionService,
                                  DocumentGenerationUtil documentGenerationUtil,
                                  ServiceContractDetailsRepository serviceContractDetailsRepository,
                                  ReminderDocumentFileRepository reminderDocumentFileRepository,
                                  ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                  DocumentMergerService documentMergerService,
                                  DocumentGenerationService documentGenerationService
    ) {
        this.processRepository = processRepository;
        this.processRecordInfoRepository = processRecordInfoRepository;
        this.reminderProcessItemRepository = reminderProcessItemRepository;
        this.notificationEventPublisher = notificationEventPublisher;
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.reminderRepository = reminderRepository;
        this.contractTemplateRepository = contractTemplateRepository;
        this.emailCommunicationRepository = emailCommunicationRepository;
        this.reminderDocumentFileRepository = reminderDocumentFileRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.smsCommunicationService = smsCommunicationService;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.customerCommunicationsRepository = customerCommunicationsRepository;
        this.emailCommunicationSenderService = emailCommunicationSenderService;
        this.smsSendingNumberRepository = smsSendingNumberRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.accountManagerRepository = accountManagerRepository;
        this.smsCommunicationCustomersRepository = smsCommunicationCustomersRepository;
        this.smsCommunicationCustomerContactsRepository = smsCommunicationCustomerContactsRepository;
        this.smsCommunicationRepository = smsCommunicationRepository;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.customerCommunicationContactsRepository = customerCommunicationContactsRepository;
        this.smsCommunicationSendHelperService = smsCommunicationSendHelperService;
        this.reminderDocumentGenerationService = reminderDocumentGenerationService;
        this.permissionService = permissionService;
        this.invoiceRepository = invoiceRepository;
        this.documentMergerService = documentMergerService;
        this.documentGenerationUtil = documentGenerationUtil;
        this.contractTemplateDetailsRepository = contractTemplateDetailsRepository;
        this.serviceContractDetailsRepository = serviceContractDetailsRepository;
        this.documentGenerationService = documentGenerationService;
    }

    @Override
    public boolean supports(EventType eventType) {
        return eventType.equals(EventType.REMINDER_PROCESS);
    }

    @Override
    protected void startFileProcessing(ByteArrayResource file, Long processId) {

    }

    @Override
    protected void startReminderProcessing(Long reminderId, Long processId) {
        try {
            processReminder(reminderId, processId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void processReminder(Long reminderId, Long processId) throws InterruptedException {
        AtomicBoolean isExceptionHandled = new AtomicBoolean(false);
        Process process = getProcessById(processId);
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reminder not found!"));
        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository.findByNameAndStatusAndIsHardcodedTrue("Reminder", NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication REMINDER not found!"));

        List<ReminderProcessItem> allByReminderIdAndProcessId = reminderProcessItemRepository.findAllByReminderIdAndProcessIdOrderByRecordIndex(reminderId, processId);
        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository.findByDefaultSelectionTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Default sms number not found!"));

        if (checkAndConflictProcess(process.getStatus())) {
            throw new ClientException("Process status is " + process.getStatus().name() + ";", ErrorCode.CONFLICT);
        }

        checkAndInProgressProcess(process);

        LocalDateTime start = LocalDateTime.now();

        ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
        int numberOfRows = allByReminderIdAndProcessId.size();
        long processStartIndex = 0;
        Optional<ProcessedRecordInfo> optional = processRecordInfoRepository.findFirstByProcessIdOrderByRecordIdDesc(processId);
        if (optional.isPresent()) {
            processStartIndex = optional.get().getRecordId() + 1;
        }

        String processSysUserId = process.getSystemUserId();
        LocalDate processDate = process.getDate();

        while (processStartIndex < numberOfRows) {
            List<Callable<String>> callables = new ArrayList<>();
            for (long j = processStartIndex;
                 j < processStartIndex + (long) numberOfCallablesPerThread * numberOfRowsPerTsk && j < numberOfRows;
                 j = j + numberOfRowsPerTsk
            ) {
                Long jObject = j;
                Callable<String> callableTask = () -> {
                    List<ProcessedRecordInfo> processedRecordInfos = new ArrayList<>();
                    for (long k = jObject; k < jObject + numberOfRowsPerTsk && k < numberOfRows; k++) {
                        ReminderProcessItem reminderProcessItemRow = allByReminderIdAndProcessId.get((int) k);
                        if (reminderProcessItemRow == null)
                            continue;
                        ProcessedRecordInfo processedRecordInfo = new ProcessedRecordInfo();
                        processedRecordInfo.setProcessId(processId);
                        long recordInfoId = k + 1;
                        processedRecordInfo.setRecordId(recordInfoId);
                        processedRecordInfo.setRecordIdentifier(String.valueOf(reminderProcessItemRow.getId()));
                        ProcessedRecordInfo savedInfo = processRecordInfoRepository.save(processedRecordInfo);
                        try {
                            String recordIdentifierVersion = processRow(reminderProcessItemRow, processSysUserId, processDate, savedInfo.getId(), reminder, smsSendingNumber, topicOfCommunication);
                            processedRecordInfo.setRecordIdentifierVersion(String.valueOf(recordIdentifierVersion));
                            processedRecordInfo.setSuccess(true);
                        } catch (Exception e) {
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

            if (checkAndBreakProcess(processId)) {
                break;
            }
        }

        System.out.println("rows: " + numberOfRows);
        LocalDateTime end = LocalDateTime.now();
        System.out.println("Duration: " + Duration.between(start, end).toSeconds());

        checkAndCompleteProcess(processId);

        if (isExceptionHandled.get()) {
            publishNotifications(process, NotificationState.ERROR);
        }

        onFinish(process);
    }

    private void checkAndCompleteProcess(Long processId) {
        Process process = getProcessById(processId);

        if (process.getStatus() == ProcessStatus.IN_PROGRESS) {
            process.setStatus(ProcessStatus.COMPLETED);
            process.setProcessCompleteDate(LocalDateTime.now());
            processRepository.save(process);
        }
    }

    private void checkAndInProgressProcess(Process process) {
        if (process.getStatus() == ProcessStatus.NOT_STARTED) {
            process.setProcessStartDate(LocalDateTime.now());
        }
        process.setStatus(ProcessStatus.IN_PROGRESS);
        processRepository.save(process);
    }

    private boolean checkAndBreakProcess(Long processId) {
        ProcessStatus status = getProcessById(processId).getStatus();
        return status == ProcessStatus.CANCELED || status == ProcessStatus.PAUSED;
    }

    private boolean checkAndConflictProcess(ProcessStatus status) {
        return status == ProcessStatus.CANCELED || status == ProcessStatus.COMPLETED || status == ProcessStatus.IN_PROGRESS;
    }

    protected Process getProcessById(Long processId) {
        return processRepository
                .findById(processId)
                .orElseThrow(() -> new ClientException("Process not found;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));
    }

    protected String getIdentifier(ReminderProcessItem reminderProcessItem) {
        return String.valueOf(reminderProcessItem.getId());
    }

    protected String processRow(ReminderProcessItem reminderProcessItemRow, String processSysUserId, LocalDate date, Long processRecordInfo, Reminder reminder, SmsSendingNumber smsSendingNumber, TopicOfCommunication topicOfCommunication) {
        Long customerDetailId = customerDetailsRepository.findLastCustomerDetailIdByCustomerId(reminderProcessItemRow.getCustomerId());

        if (CustomerCommContactTypes.MOBILE_NUMBER.equals(reminderProcessItemRow.getContactType())) {
            createSms(reminderProcessItemRow, reminder, topicOfCommunication, smsSendingNumber, customerDetailId, processSysUserId);
        }

        if (CustomerCommContactTypes.EMAIL.equals(reminderProcessItemRow.getContactType())) {
            emailCommunicationService.createReminderEmail(reminderProcessItemRow, customerDetailId, topicOfCommunication, reminder.getEmailTemplateId());
        }

        if (CustomerCommContactTypes.OTHER_PLATFORM.equals(reminderProcessItemRow.getContactType())) {
            createDocumentFile(reminderProcessItemRow, customerDetailId, reminder.getDocumentTemplateId());
        }

        reminderProcessItemRow.setSentDate(LocalDateTime.now());
        reminderProcessItemRow.setSentStatus("SENT");
        reminderProcessItemRepository.saveAndFlush(reminderProcessItemRow);
        return String.valueOf(reminderProcessItemRow.getId());
    }

    private void createSms(ReminderProcessItem reminderProcessItem, Reminder reminder, TopicOfCommunication topicOfCommunication, SmsSendingNumber smsSendingNumber, Long customerDetailId, String processSysUserId) {
        Long communicationId = reminderProcessItem.getCommunicationId();
        CustomerCommunicationContacts customerCommunicationContacts = customerCommunicationContactsRepository.findMobileContactByCommunicationId(communicationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Mobile contact not found!"));
        SmsCommunication smsCommunication = saveSms(reminder.getSmsTemplateId(), topicOfCommunication, smsSendingNumber, processSysUserId);
        SmsCommunicationCustomers smsCommunicationCustomers = createSmsCommunicationCustomers(customerDetailId, communicationId, smsCommunication);
        createSmsCommunicationContact(customerCommunicationContacts, smsCommunicationCustomers);
        SmsSendParamBase smsSendParamBase = new SmsSendParamBase(REQUEST_ID_PREFIX + smsCommunicationCustomers.getId(), customerCommunicationContacts.getContactValue(), smsCommunicationCustomers);

        String body = smsCommunicationService.generateAndSetSmsBodyForReminder(reminder.getSmsTemplateId(), reminderProcessItem, smsCommunicationCustomers, customerDetailId);
        smsCommunication.setSmsBody(body);
        smsCommunicationRepository.saveAndFlush(smsCommunication);

        smsCommunicationSendHelperService.send(smsSendParamBase, body, smsSendingNumber.getSmsNumber(), null);
    }

    private void createDocumentFile(ReminderProcessItem reminderProcessItem, Long customerDetailId, Long templateId) {
        ContractTemplate template = contractTemplateRepository
                .findById(templateId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
                );
        log.debug("Found contract template: {}", template);

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .findById(template.getLastTemplateDetailId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details with id: %s".formatted(template.getLastTemplateDetailId()))
                );
        log.debug("Found contract template detail: {}", templateDetail);

        String destinationPath = destinationPath();

        ByteArrayResource templateFileResource = null;
        try {
            templateFileResource = new ByteArrayResource(
                    Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath())
            );

            ReminderDocumentModel reminderDocumentModel = reminderDocumentGenerationService.generateReminderJson(
                    reminderProcessItem.getReminderId(),
                    reminderProcessItem.getLiabilityId(),
                    customerDetailId,
                    reminderProcessItem.getTotalAmount(),
                    reminderProcessItem.getCommunicationId());

            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            UUID.randomUUID().toString(),
                            reminderDocumentModel,
                            Set.of(FileFormat.PDF),
                            false
                    );

            ReminderDocumentFile reminderDocumentFile = new ReminderDocumentFile();
            reminderDocumentFile.setName(reminderProcessItem.getId().toString());
            reminderDocumentFile.setFileUrl(documentPathPayloads.pdfPath());
            reminderDocumentFile.setReminderId(reminderProcessItem.getReminderId());
            reminderDocumentFile.setStatus(EntityStatus.ACTIVE);
            reminderDocumentFileRepository.saveAndFlush(reminderDocumentFile);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String destinationPath() {
        return String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
    }

    private void createSmsCommunicationContact(CustomerCommunicationContacts customerCommunicationContacts, SmsCommunicationCustomers smsCommunicationCustomers) {
        SmsCommunicationCustomerContacts smsCommunicationCustomerContacts = new SmsCommunicationCustomerContacts();
        smsCommunicationCustomerContacts.setCustomerCommunicationContactId(customerCommunicationContacts.getId());
        smsCommunicationCustomerContacts.setSmsCommunicationCustomerId(smsCommunicationCustomers.getId());
        smsCommunicationCustomerContacts.setPhoneNumber(customerCommunicationContacts.getContactValue());
        smsCommunicationCustomerContactsRepository.saveAndFlush(smsCommunicationCustomerContacts);
    }

    private SmsCommunicationCustomers createSmsCommunicationCustomers(Long customerDetailId, Long communicationId, SmsCommunication smsCommunication) {
        SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
        smsCommunicationCustomers.setCustomerCommunicationId(communicationId);
        smsCommunicationCustomers.setSmsCommunicationId(smsCommunication.getId());
        smsCommunicationCustomers.setCustomerDetailId(customerDetailId);
        smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
        return smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);
    }

    private SmsCommunication saveSms(Long templateId, TopicOfCommunication topicOfCommunication, SmsSendingNumber smsSendingNumber, String processSysUserId) {
        AccountManager accountManager = accountManagerRepository.findByUserName(processSysUserId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));

        SmsCommunication smsCommunication = new SmsCommunication();
        smsCommunication.setAllCustomersWithActiveContract(false);
        smsCommunication.setCommunicationAsInstitution(false);
        smsCommunication.setCommunicationType(CommunicationType.OUTGOING);
        smsCommunication.setCommunicationTopicId(topicOfCommunication.getId());
        smsCommunication.setStatus(EntityStatus.ACTIVE);
        smsCommunication.setSentDate(LocalDateTime.now());
        smsCommunication.setSmsSendingNumberId(smsSendingNumber.getId());
        smsCommunication.setSenderEmployeeId(accountManager.getId());
        smsCommunication.setCommunicationChannel(SmsCommunicationChannel.SMS);
        smsCommunication.setSystemUserId(processSysUserId);
        smsCommunication.setTemplateId(templateId);
        return smsCommunicationRepository.saveAndFlush(smsCommunication);
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

    protected void onFinish(Process process) {
        documentMergerService.mergeReminderDocuments(process.getReminderId(), process.getId());
        publishNotifications(process, NotificationState.COMPLETION);
    }

    protected void publishNotifications(Process process, NotificationState notificationState) {
        ProcessType processType = process.getType();

        NotificationType notificationType = ProcessNotificationMapper.mapToNotificationType(processType, notificationState);

        notificationEventPublisher.publishNotification(new NotificationEvent(process.getId(), notificationType, processRepository, notificationState));
    }
}
