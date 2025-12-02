package bg.energo.phoenix.service.crm.smsCommunication;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.crm.smsCommunication.*;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderProcessItem;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.crm.massSmsCommunication.MassSMSSaveAs;
import bg.energo.phoenix.model.enums.crm.smsCommunication.*;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.crm.smsCommunication.*;
import bg.energo.phoenix.model.response.crm.emailCommunication.MassCommunicationFileProcessedResult;
import bg.energo.phoenix.model.response.crm.emailCommunication.MassCommunicationFileProcessedResultProjection;
import bg.energo.phoenix.model.response.crm.emailCommunication.MassCommunicationImportProcessResult;
import bg.energo.phoenix.model.response.crm.smsCommunication.MassSMSCommunicationViewResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.MassSMSCustomerAndContractResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.SmsCommunicationListingResponse;
import bg.energo.phoenix.model.response.crm.smsCommunication.SmsCommunicationViewResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.*;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.SmsSendingNumberRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.crm.massSmsCommunication.MassSMSCommunicationActivityService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentCreationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentRequest;
import bg.energo.phoenix.service.document.ReminderDocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.permissions.PermissionContextEnum.MASS_SMS_COMMUNICATION;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SMS_COMMUNICATION;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;

/**
 * Service class responsible for handling business logic related to SMS communications.
 * It provides methods for creating, listing, and managing SMS communications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCommunicationService {
    private static final long MAX_SIZE_COMMUNICATION_FILES = 50 * 1024 * 1024;
    private static final String REQUEST_ID_PREFIX = "EPROES";
    private final static String FOLDER_PATH = "SMSCommunicationFiles";
    private final static int numberOfThreads = 10;
    private final ContractTemplateRepository contractTemplateRepository;
    private final SmsCommunicationRepository smsCommunicationRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final SmsSendingNumberRepository smsSendingNumberRepository;
    private final CustomerRepository customerRepository;
    private final PermissionService permissionService;
    private final SmsCommunicationRelatedCustomersRepository smsCommunicationRelatedCustomersRepository;
    private final SmsCommunicationFilesRepository smsCommunicationFilesRepository;
    private final FileService fileService;
    private final TemplateRepository templateRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final SmsCommunicationCustomersRepository smsCommunicationCustomersRepository;
    private final SmsCommunicationCustomerContactsRepository smsCommunicationCustomerContactsRepository;
    private final ContactPurposeRepository contactPurposeRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final MassSMSCommunicationActivityService massSMSCommunicationActivityService;
    private final TaskService taskService;
    private final SmsCommunicationSendHelperService smsCommunicationSendHelperService;
    private final TransactionTemplate transactionTemplate;
    private final SmsCommunicationActivityService smsCommunicationActivityService;
    private final SmsCommunicationContactPurposeRepository smsCommunicationContactPurposeRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final DocumentGenerationService documentGenerationService;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final EmailAndSmsDocumentCreationService emailAndSmsDocumentCreationService;
    private final FileArchivationService fileArchivationService;
    private final ReminderDocumentGenerationService reminderDocumentGenerationService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Retrieves a paginated list of SMS communication records based on the provided request parameters.
     *
     * @param request the request object containing the search criteria
     * @return a page of SMS communication listing responses
     */
    public Page<SmsCommunicationListingResponse> list(SmsCommunicationListingRequest request) {
        List<SmsCommStatus> communicationStatuses;
        List<SmsCommStatus> massSmsCommunicationStatuses;
        if (request.getCommunicationStatuses() != null) {
            massSmsCommunicationStatuses = checkListingPermissionsForMassSms(request);
            communicationStatuses = checkListingPermissionsOfCommunicationStatuses(request);
        } else {
            communicationStatuses = getCommunicationStatuses();
            massSmsCommunicationStatuses = getMassSmsCommunicationStatuses();
        }
        String sortBy = null;
        if (request.getSortBy() != null && request.getSortBy().equals(SmsCommunicationListingSortBy.CONTACT_PURPOSE)) {
            sortBy = request.getContactPurposeDirection() == null || request.getContactPurposeDirection().equals(Sort.Direction.ASC)
                    ? "contactPurposeAsc" : "contactPurposeDesc";
        } else if (request.getSortBy() != null && request.getSortBy().equals(SmsCommunicationListingSortBy.ACTIVITY)) {
            sortBy = request.getActivityDirection() == null || request.getActivityDirection().equals(Sort.Direction.ASC)
                    ? "activityAsc" : "activityDesc";
        }
        return smsCommunicationRepository.filter(
                ListUtils.emptyIfNull(request.getCreatorEmployee()),
                ListUtils.emptyIfNull(request.getSenderEmployee()),
                request.getDateFrom(),
                request.getDateTo(),
                ListUtils.emptyIfNull(request.getContactPurposes()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getCommunicationTypes()),
                ListUtils.emptyIfNull(request.getActivities()),
                ListUtils.emptyIfNull(request.getTasks()),
                ListUtils.emptyIfNull(request.getTopicOfCommunications()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getKindOfCommunications()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(communicationStatuses).isEmpty() ? List.of("") : EPBListUtils.convertEnumListIntoStringListIfNotNull(communicationStatuses),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(massSmsCommunicationStatuses).isEmpty() ? List.of("") : EPBListUtils.convertEnumListIntoStringListIfNotNull(massSmsCommunicationStatuses),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                Objects.requireNonNullElse(request.getSmsCommunicationSearchBy(), SmsCommunicationSearchBy.ALL).name(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(getEntityStatuses()),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getSortingDirection(), sortBy != null ? sortBy : getSortByEnum(request.getSortBy()))
                        )
                )
        ).map(sms -> new SmsCommunicationListingResponse(sms, request.getActivityDirection(), request.getContactPurposeDirection()));
    }

    /**
     * Retrieves a list of SMS communication statuses that the user has permission to view for mass SMS communications.
     * The list includes the 'DRAFT' status if the user has the 'MASS_SMS_COMMUNICATION_VIEW_DRAFT' permission,
     * and the 'SENT' status if the user has the 'MASS_SMS_COMMUNICATION_VIEW_SENT' permission.
     *
     * @return a list of {@link SmsCommStatus} objects representing the available statuses for mass SMS communications
     */
    private List<SmsCommStatus> getMassSmsCommunicationStatuses() {
        List<SmsCommStatus> communicationStatuses = new ArrayList<>();
        if (checkOnPermission(MASS_SMS_COMMUNICATION, List.of(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_DRAFT))) {
            communicationStatuses.add(SmsCommStatus.DRAFT);
        }
        if (checkOnPermission(MASS_SMS_COMMUNICATION, List.of(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_SENT))) {
            communicationStatuses.add(SmsCommStatus.SENT);
        }
        return communicationStatuses;
    }

    /**
     * Checks the listing permissions for the provided mass SMS communication statuses.
     *
     * @param request the SMS communication listing request containing the communication statuses to check
     * @return a new list of communication statuses that the user has permission to view for mass SMS communications
     */
    private List<SmsCommStatus> checkListingPermissionsForMassSms(SmsCommunicationListingRequest request) {
        List<SmsCommStatus> communicationStatuses = request.getCommunicationStatuses();
        List<SmsCommStatus> newCommunicationStatuses = new ArrayList<>();

        if (permissionService.getPermissionsFromContext(MASS_SMS_COMMUNICATION).contains(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_SENT.getId()) && communicationStatuses.contains(SmsCommStatus.SENT)) {
            newCommunicationStatuses.add(SmsCommStatus.SENT);
        }

        if (permissionService.getPermissionsFromContext(MASS_SMS_COMMUNICATION).contains(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_DRAFT.getId()) && communicationStatuses.contains(SmsCommStatus.DRAFT)) {
            newCommunicationStatuses.add(SmsCommStatus.DRAFT);
        }
        return newCommunicationStatuses;
    }

    private String getSortByEnum(SmsCommunicationListingSortBy sortBy) {
        return sortBy != null ? sortBy.getValue() : SmsCommunicationListingSortBy.ID.getValue();
    }

    /**
     * Retrieves a list of entity statuses based on the user's permissions.
     * If the user has the 'SMS_COMMUNICATION_VIEW_DELETE' permission, the list will include both 'ACTIVE' and 'DELETED' statuses.
     * Otherwise, the list will only include the 'ACTIVE' status.
     *
     * @return a list of {@link EntityStatus} objects representing the available statuses
     */
    private List<EntityStatus> getEntityStatuses() {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        entityStatuses.add(EntityStatus.ACTIVE);
        if (permissionService.getPermissionsFromContext(SMS_COMMUNICATION).contains(PermissionEnum.SMS_COMMUNICATION_VIEW_DELETE.getId())) {
            entityStatuses.add(EntityStatus.DELETED);
        }
        return entityStatuses;
    }


    /**
     * Checks the listing permissions for the provided SMS communication status list.
     *
     * @param request the SMS communication listing request containing the communication statuses to check
     * @return a new list of communication statuses that the user permission to view
     */
    private List<SmsCommStatus> checkListingPermissionsOfCommunicationStatuses(SmsCommunicationListingRequest request) {
        List<SmsCommStatus> communicationStatuses = request.getCommunicationStatuses();
        List<SmsCommStatus> newCommunicationStatuses = new ArrayList<>();

        if (permissionService.getPermissionsFromContext(SMS_COMMUNICATION).contains(PermissionEnum.SMS_COMMUNICATION_VIEW_SEND.getId())) {
            if (communicationStatuses.contains(SmsCommStatus.SENT)) {
                newCommunicationStatuses.add(SmsCommStatus.SENT);
            }
            if (communicationStatuses.contains(SmsCommStatus.SEND_FAILED)) {
                newCommunicationStatuses.add(SmsCommStatus.SEND_FAILED);
            }
            if (communicationStatuses.contains(SmsCommStatus.SENT_SUCCESSFULLY)) {
                newCommunicationStatuses.add(SmsCommStatus.SENT_SUCCESSFULLY);
            }
            if (communicationStatuses.contains(SmsCommStatus.RECEIVED)) {
                newCommunicationStatuses.add(SmsCommStatus.RECEIVED);
            }
            if (communicationStatuses.contains(SmsCommStatus.IN_PROGRESS)) {
                newCommunicationStatuses.add(SmsCommStatus.IN_PROGRESS);
            }
        }
        if (permissionService.getPermissionsFromContext(SMS_COMMUNICATION).contains(PermissionEnum.SMS_COMMUNICATION_VIEW_DRAFT.getId())) {
            if (communicationStatuses.contains(SmsCommStatus.DRAFT)) {
                newCommunicationStatuses.add(SmsCommStatus.DRAFT);
            }
        }

        return newCommunicationStatuses;
    }


    /**
     * Creates a new SMS communication based on the provided request data.
     *
     * @param request SmsCommunicationBaseRequest object containing the necessary data for creating an SMS communication
     * @return Long representing the ID of the newly created SMS communication
     */
    @Transactional
    public Long create(SmsCommunicationBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        SmsCommunication smsCommunication = new SmsCommunication();
        smsCommunication.setSmsBody(request.getSmsBody());
        smsCommunication.setAllCustomersWithActiveContract(false);
        smsCommunication.setCommunicationAsInstitution(request.isCommunicationAsInstitution());
        smsCommunication.setCommunicationType(request.getCommunicationType());
        smsCommunication.setStatus(EntityStatus.ACTIVE);
        smsCommunication.setCommunicationChannel(SmsCommunicationChannel.SMS);
        validateAndSetTemplate(smsCommunication, request.getTemplateId(), ContractTemplatePurposes.SMS);
        checkPermissions(request, smsCommunication);
        SmsSendingNumber smsSendingNumber = checkAndSetExchangeCode(request.getExchangeCodeId(), errorMessages, smsCommunication);
        validateRequest(errorMessages, smsCommunication, request.getTopicOfCommunicationId(), request.getExchangeCodeId(), request.getRelatedCustomerIds(), request.getCommunicationFileIds());

        SmsCommunicationCustomers smsCommunicationCustomers = validateAndSetCommunicationData(smsCommunication, request, errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
            SmsSendParamBase smsSendParamSingle = createSmsSendParam(smsCommunicationCustomers.getId(), request.getCustomerPhoneNumber(), smsCommunicationCustomers);
            smsCommunicationSendHelperService.send(smsSendParamSingle, request.getSmsBody(), smsSendingNumber.getSmsNumber(), null);
        }

        archiveFiles(smsCommunication);
        return smsCommunicationCustomers.getId();
    }

    /**
     * Archives the files associated with the provided SMS communication.
     * <p>
     * This method retrieves all active SMS communication files associated with the given SMS communication,
     * downloads the file content, and archives the files using the archivationService. The archived files
     * are stored with the following attributes:
     * <p>
     * - Document Type: SMS Communication File
     * - Document Number: "SMS Communication/{smsCommunication.id}/{smsCommunicationFile.id}"
     * - Document Date: Current date and time
     * - Customer Identifier: Empty
     * - Customer Number: Empty
     * - Signed: False
     * <p>
     * If any errors occur during the archiving process, they are logged but do not prevent the method from
     * continuing to archive the remaining files.
     *
     * @param smsCommunication The SMS communication for which to archive the associated files.
     */
    private void archiveFiles(SmsCommunication smsCommunication) {
        List<SmsCommunicationFiles> smsCommunicationFiles = smsCommunicationFilesRepository.findAllActiveSmsCommunicationFileBySmsCommunicationId(smsCommunication.getId());
        if (CollectionUtils.isNotEmpty(smsCommunicationFiles)) {
            for (SmsCommunicationFiles smsCommunicationFile : smsCommunicationFiles) {
                try {
                    smsCommunicationFile.setNeedArchive(true);
                    smsCommunicationFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SMS_COMMUNICATION_FILE);
                    smsCommunicationFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SMS_COMMUNICATION_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), "%s/%s/%s".formatted("SMS Communication", smsCommunication.getId(), smsCommunicationFile.getId())),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(smsCommunicationFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: [%s]".formatted(smsCommunicationFile.getLocalFileUrl()), e);
                }
            }
        }
    }

    /**
     * Creates a mass SMS communication and sends it to the specified customers.
     * <p>
     * This method creates a new SMS communication with the provided request details, including the SMS body, communication channel, and related customers. It validates the request parameters, such as the template ID, exchange code, and contact purposes, and saves the SMS communication to the database.
     * <p>
     * If the request is set to "send", the method also validates the contact purposes and customers, generates a report of any errors, and sends the SMS communication to the eligible customers. If no customers are eligible, the method throws a `ClientException` with the error message.
     *
     * @param request The request object containing the details for the mass SMS communication.
     * @return The ID of the created SMS communication.
     * @throws ClientException if there are any errors during the creation or sending of the mass SMS communication.
     */
    public Long createMassSms(MassSmsCreateRequest request) {
        List<String> errorMessages = new ArrayList<>();
        AtomicReference<SmsSendingNumber> smsSendingNumber = new AtomicReference<>();
        SmsCommunication smsComm = transactionTemplate.execute(
                sms -> {
                    SmsCommunication smsCommunication = new SmsCommunication();
                    smsCommunication.setSmsBody(request.getSmsBody());
                    smsCommunication.setCommunicationAsInstitution(request.isCommunicationAsInstitution());
                    smsCommunication.setCommunicationType(CommunicationType.OUTGOING);
                    smsCommunication.setStatus(EntityStatus.ACTIVE);
                    smsCommunication.setAllCustomersWithActiveContract(request.isAllCustomersWithActiveContract());
                    smsCommunication.setCommunicationChannel(SmsCommunicationChannel.MASS_SMS);
                    validateAndSetTemplate(smsCommunication, request.getTemplateId(), ContractTemplatePurposes.SMS);
                    checkPermissionsForMassSMS(request.getSaveAs(), smsCommunication);

                    checkAndSetTopicOfCommunication(request.getTopicOfCommunicationId(), errorMessages, smsCommunication);
                    smsSendingNumber.set(checkAndSetExchangeCode(request.getExchangeCodeId(), errorMessages, smsCommunication));
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

                    smsCommunication = smsCommunicationRepository.save(smsCommunication);

                    validateAndSetRelatedCustomers(smsCommunication, request.getRelatedCustomerIds(), errorMessages);
                    validateAndSetCommunicationFiles(smsCommunication.getId(), request.getCommunicationFileIds(), errorMessages);
                    validateContactPurposes(request.getContactPurposeIds(), errorMessages, smsCommunication);
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                    return smsCommunication;
                }
        );

        Set<MassSMSCustomerRequest> customers = request.isAllCustomersWithActiveContract() ? customerRepository.fetchActiveContractsAndAssociatedCustomersForMassCommunication().stream().map(MassSMSCustomerRequest::new).collect(Collectors.toSet())
                : request.getCustomers();
        boolean send = request.getSaveAs().equals(MassSMSSaveAs.SEND);
        AtomicBoolean atLeastOne = new AtomicBoolean(false);
        Map<String, String> reportMap = validateAndSetContactPurposesAndCustomers(customers, smsComm, send, request.getContactPurposeIds(), smsSendingNumber.get().getSmsNumber(), atLeastOne, request.getTemplateId(), request.getSmsBody(), request.isAllCustomersWithActiveContract());
        if (!atLeastOne.get()) {
            smsComm.setStatus(EntityStatus.DELETED);
            smsCommunicationSendHelperService.saveSmsComm(smsComm);
            throw new ClientException(constructMessage(reportMap), ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!reportMap.isEmpty()) {
            try {
                generateReport(reportMap, smsComm, send);
            } catch (Exception e) {
                log.debug("report generation failed");
                throw new ClientException("Report generation failed!", APPLICATION_ERROR);
            }
        }
        return smsComm.getId();

    }

    private String constructMessage(Map<String, String> reportMap) {
        StringBuilder messages = new StringBuilder();
        for (String customer : reportMap.keySet()) {
            messages.append(reportMap.get(customer) + ", for customer : " + customer);
        }
        return messages.toString();
    }

    /**
     * Generates a report in the form of an Excel file for the provided customer data and error messages.
     *
     * @param reportMap        a map containing customer identifiers as keys and error messages as values
     * @param smsCommunication the SMS communication object associated with the report
     * @param send             a boolean indicating whether the report is being generated after the SMS communication has been sent
     * @throws IOException if there is an error writing the Excel file
     */
    private void generateReport(Map<String, String> reportMap, SmsCommunication smsCommunication, boolean send) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Customer Data");
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("customer_identifier");
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("errorMessages");

        int rowNum = 1;
        for (String identifier : reportMap.keySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(identifier);
            row.createCell(1).setCellValue(reportMap.get(identifier));
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] bArray = bos.toByteArray();
        String randomName = UUID.randomUUID().toString();
        MultipartFile multipartFile = new ByteMultiPartFile(randomName, bArray);
        String fileUrl = fileService.uploadFile(multipartFile, String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now()), randomName + ".xlsx");
        SmsCommunicationFiles smsCommunicationFiles = new SmsCommunicationFiles();
        smsCommunicationFiles.setReport(true);
        smsCommunicationFiles.setSmsCommunicationId(smsCommunication.getId());
        smsCommunicationFiles.setLocalFileUrl(fileUrl);
        smsCommunicationFiles.setAfterSendReport(send);
        smsCommunicationFiles.setName(randomName);
        smsCommunicationFiles.setStatus(EntityStatus.ACTIVE);
        smsCommunicationFilesRepository.save(smsCommunicationFiles);
    }

    /**
     * Validates the provided contact purposes and associates them with the given SMS communication.
     *
     * @param contactPurposes  the set of contact purpose IDs to validate and associate
     * @param errorMessages    a list to store any error messages encountered during validation
     * @param smsCommunication the SMS communication to associate the contact purposes with
     */
    public void validateContactPurposes(Set<Long> contactPurposes, List<String> errorMessages, SmsCommunication smsCommunication) {
        Map<Long, ContactPurpose> collect = contactPurposeRepository.findByIdsIn(contactPurposes).stream()
                .collect(Collectors.toMap(ContactPurpose::getId, y -> y));
        List<SmsCommunicationContactPurpose> toSave = new ArrayList<>();
        for (Long i : contactPurposes) {
            if (!collect.containsKey(i)) {
                errorMessages.add("contact purpose doesn't exist with id " + i);
            }
        }
        for (Long i : collect.keySet()) {
            SmsCommunicationContactPurpose smsCommunicationContactPurpose = new SmsCommunicationContactPurpose();
            smsCommunicationContactPurpose.setContactPurposeId(i);
            smsCommunicationContactPurpose.setSmsCommunicationId(smsCommunication.getId());
            smsCommunicationContactPurpose.setStatus(EntityStatus.ACTIVE);
            toSave.add(smsCommunicationContactPurpose);
        }
        smsCommunicationContactPurposeRepository.saveAll(toSave);
    }

    /**
     * Updates the contact purposes associated with the given SMS communication.
     *
     * @param contactPurposes  the set of contact purpose IDs to update
     * @param errorMessages    a list to store any error messages encountered during the update
     * @param smsCommunication the SMS communication to update the contact purposes for
     */
    public void updateContactPurposes(Set<Long> contactPurposes, List<String> errorMessages, SmsCommunication smsCommunication) {
        Map<Long, ContactPurpose> collect = contactPurposeRepository.findByIdsIn(contactPurposes).stream()
                .collect(Collectors.toMap(ContactPurpose::getId, y -> y));
        for (Long i : contactPurposes) {
            if (!collect.containsKey(i)) {
                errorMessages.add("contact purpose doesn't exist with id " + i);
            }
        }

        Set<SmsCommunicationContactPurpose> contactPurposeIdsBySmsCommunicationId = smsCommunicationContactPurposeRepository.findContactPurposeBySmsCommunicationId(smsCommunication.getId());
        Set<Long> fetchedPurposes = contactPurposeIdsBySmsCommunicationId.stream().map(SmsCommunicationContactPurpose::getContactPurposeId).collect(Collectors.toSet());
        for (SmsCommunicationContactPurpose purpose : contactPurposeIdsBySmsCommunicationId) {
            if (!contactPurposes.contains(purpose.getContactPurposeId())) {
                purpose.setStatus(EntityStatus.DELETED);
            }
        }

        for (Long purpose : contactPurposes) {
            if (!fetchedPurposes.contains(purpose)) {
                SmsCommunicationContactPurpose smsCommunicationContactPurpose = new SmsCommunicationContactPurpose();
                smsCommunicationContactPurpose.setContactPurposeId(purpose);
                smsCommunicationContactPurpose.setSmsCommunicationId(smsCommunication.getId());
                smsCommunicationContactPurpose.setStatus(EntityStatus.ACTIVE);
                smsCommunicationContactPurposeRepository.save(smsCommunicationContactPurpose);
            }
        }

    }

    /**
     * Validates the provided customers and contact purposes, and processes the customers for SMS communication.
     *
     * @param customers        the set of customers to process
     * @param smsCommunication the SMS communication to associate the customers with
     * @param send             whether to actually send the SMS messages or just store the communication details
     * @param contactPurposes  the set of contact purpose IDs to associate with the SMS communication
     * @param smsSendingNumber the phone number to use for sending the SMS messages
     * @param atLeastOne       an AtomicBoolean flag to indicate if at least one customer was processed
     * @return a report of the processing results, keyed by customer identifier
     * @throws InterruptedException if the thread pool is interrupted during execution
     */
    @SneakyThrows
    public Map<String, String> validateAndSetContactPurposesAndCustomers(Set<MassSMSCustomerRequest> customers, SmsCommunication smsCommunication, boolean send, Set<Long> contactPurposes, String smsSendingNumber, AtomicBoolean atLeastOne, Long templateId, String body, boolean allCustomersWithActiveContract) {
        int batchSize = customers.size() / numberOfThreads + 1;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Void>> callables = new ArrayList<>();
        ConcurrentHashMap<String, String> report = new ConcurrentHashMap<>();
        List<List<MassSMSCustomerRequest>> partitions = ListUtils.partition(customers.stream().toList(), batchSize);
        for (List<MassSMSCustomerRequest> partition : partitions) {
            callables.add(() -> {
                processCustomers(partition, report, smsCommunication, send, contactPurposes, smsSendingNumber, atLeastOne, templateId, body, allCustomersWithActiveContract);
                return null;
            });
        }

        executorService.invokeAll(callables);
        executorService.shutdown();
        return report;
    }

    /**
     * Processes a list of customers for SMS communication, validating the customer details and contact purposes, and creating the necessary records in the database.
     *
     * @param customers        the list of customers to process
     * @param report           a map to store any error messages encountered during processing, keyed by customer identifier
     * @param smsCommunication the SMS communication to associate the customers with
     * @param send             whether to actually send the SMS messages or just store the communication details
     * @param contactPurposes  the set of contact purpose IDs to associate with the SMS communication
     * @param smsSendingNumber the phone number to use for sending the SMS messages
     * @param atLeastOne       an AtomicBoolean flag to indicate if at least one customer was processed
     * @throws InterruptedException if the thread pool is interrupted during execution
     */
    public void processCustomers(List<MassSMSCustomerRequest> customers, Map<String, String> report, SmsCommunication smsCommunication, boolean send, Set<Long> contactPurposes, String smsSendingNumber, AtomicBoolean atLeastOne, Long templateId, String body, boolean allCustomersWithActiveContract) {
        List<SmsSendParamBase> smsToSend = new ArrayList<>();

        transactionTemplate.executeWithoutResult(process -> {
            List<SmsCommunicationCustomers> smsCommunicationCustomersResult = new ArrayList<>();

            for (MassSMSCustomerRequest customerRequest : customers) {
                StringBuilder errorMessages = new StringBuilder();
                Optional<CustomerDetails> customerDetails;
                Long productContractDetailId = null;
                Long serviceContractId = null;
                String contractNumber = null;
                Long contractId = null;
                if (customerRequest.getVersion() != null) {
                    customerDetails = customerDetailsRepository.findByCustomerIdentifierAndVersionId(customerRequest.getCustomerIdentifier(), customerRequest.getVersion());
                } else {
                    customerDetails = customerDetailsRepository.findLastCustomerDetail(customerRequest.getCustomerIdentifier());
                }

                if (!allCustomersWithActiveContract) {
                    if (customerDetails.isPresent()) {
                        if (customerRequest.getProductContractDetailId() != null) {
                            Optional<ProductContract> contractOptional = productContractDetailsRepository.checkCustomerAndFetchContractNumber(customerDetails.get().getCustomerId(), customerRequest.getProductContractDetailId());
                            if (contractOptional.isEmpty()) {
                                errorMessages.append(String.format("Product contract with id %s is not attached to customer with id %s", customerRequest.getProductContractDetailId(), customerDetails.get().getCustomerId()));
                            } else {
                                productContractDetailId = customerRequest.getProductContractDetailId();
                                contractNumber = contractOptional.get().getContractNumber();
                                contractId = contractOptional.get().getId();
                            }
                        }
                        if (customerRequest.getServiceContractDetailId() != null) {
                            Optional<ServiceContracts> contractNumberOptional = serviceContractsRepository.checkContractAttachedToCustomerAndFetchContractNumber(customerDetails.get().getCustomerId(), customerRequest.getServiceContractDetailId());
                            if (contractNumberOptional.isEmpty()) {
                                errorMessages.append(String.format("Service contract detail with id %s is not attached to customer with id %s", customerRequest.getServiceContractDetailId(), customerDetails.get().getCustomerId()));
                            } else {
                                serviceContractId = customerRequest.getServiceContractDetailId();
                                contractNumber = contractNumberOptional.get().getContractNumber();
                                contractId = contractNumberOptional.get().getId();
                            }

                        }
                    }
                }

                if (customerDetails.isPresent()) {
                    if (!send) {
                        SmsCommunicationCustomers smsCommunicationCustomer = createSmsCommunicationCustomer(smsCommunication.getId(), customerDetails.get(), customerRequest.getProductContractDetailId(), customerRequest.getServiceContractDetailId());
                        smsCommunicationCustomersResult.add(smsCommunicationCustomer);
                        atLeastOne.set(true);
                    } else {
                        List<Object[]> customerCommunicationsByCustomerDetailIdAndPurpose = customerCommunicationsRepository.findCustomerCommunicationsByCustomerDetailIdAndPurpose(customerDetails.get().getId(), contactPurposes);
                        Set<Long> check = customerCommunicationsByCustomerDetailIdAndPurpose.stream().map(com -> (Long) com[2]).collect(Collectors.toSet());
                        if (customerCommunicationsByCustomerDetailIdAndPurpose.isEmpty()) {
                            errorMessages.append("customer communication data not found!;");
                        } else {
                            for (Long i : contactPurposes) {
                                if (!check.contains(i)) {
                                    errorMessages.append("customer communication data not found! with purpose ").append(i).append(";");
                                    log.debug("customer communication data not found with purpose {}", i);
                                }
                            }
                            if (allCustomersWithActiveContract) {
                                if (customerRequest.getServiceContractDetailIds() != null) {
                                    for (Long id : customerRequest.getServiceContractDetailIds()) {
                                        Set<Long> checkComm = new HashSet<>();
                                        Pair<Long, String> contractNumberServiceContract = fetchServiceContractNumber(id);
                                        createCommunicationCustomer(smsCommunication, atLeastOne, customerCommunicationsByCustomerDetailIdAndPurpose, checkComm, customerDetails,
                                                smsToSend, id, null, contractNumberServiceContract.getRight(), contractNumberServiceContract.getLeft());
                                        log.info("saved service contract ");
                                    }
                                }
                                if (customerRequest.getProductContractDetailIds() != null) {
                                    for (Long id : customerRequest.getProductContractDetailIds()) {
                                        Set<Long> checkComm = new HashSet<>();
                                        Pair<Long, String> contractNumberProductContract = fetchProductContractNumber(id);
                                        createCommunicationCustomer(smsCommunication, atLeastOne, customerCommunicationsByCustomerDetailIdAndPurpose, checkComm, customerDetails,
                                                smsToSend, null, id, contractNumberProductContract.getRight(), contractNumberProductContract.getLeft());
                                    }
                                }
                            } else {
                                Set<Long> checkComm = new HashSet<>();
                                createCommunicationCustomer(smsCommunication, atLeastOne, customerCommunicationsByCustomerDetailIdAndPurpose, checkComm,
                                        customerDetails, smsToSend, serviceContractId, productContractDetailId, contractNumber, contractId);
                            }
                        }
                    }
                } else {
                    errorMessages.append("customer details not found!;");
                    log.debug("customer details not found");
                }
                if (!errorMessages.isEmpty()) {
                    report.put(customerRequest.getCustomerIdentifier(), errorMessages.toString());
                }
            }
            smsCommunicationCustomersRepository.saveAll(smsCommunicationCustomersResult);
        });
//        smsCommunicationSendHelperService.sendBatch(smsToSend, smsCommunication.getSmsBody(), smsSendingNumber, null);
    }

    public Pair<Long, String> fetchServiceContractNumber(Long id) {
        List<Object[]> idAndNumber = serviceContractsRepository.fetchServiceContractNumberById(id);
        Long contractId = (Long) idAndNumber.get(0)[0];
        String contractNumber = (String) idAndNumber.get(0)[1];
        return Pair.of(contractId, contractNumber);
    }

    public Pair<Long, String> fetchProductContractNumber(Long id) {
        List<Object[]> idAndNumber = productContractDetailsRepository.fetchProductContractNumberByDetailId(id);
        Long contractId = (Long) idAndNumber.get(0)[0];
        String contractNumber = (String) idAndNumber.get(0)[1];
        return Pair.of(contractId, contractNumber);
    }

    private void createCommunicationCustomer(SmsCommunication smsCommunication, AtomicBoolean atLeastOne, List<Object[]> customerCommunicationsByCustomerDetailIdAndPurpose, Set<Long> checkComm, Optional<CustomerDetails> customerDetails, List<SmsSendParamBase> smsToSend, Long serviceContractDetailId, Long productContractDetailId, String contractNumber, Long contractId) {
        for (Object[] cc : customerCommunicationsByCustomerDetailIdAndPurpose) {
            CustomerCommunications customerCommunications = (CustomerCommunications) cc[0];
            if (!checkComm.contains(customerCommunications.getId())) {
                CustomerCommunicationContacts customerCommunicationContacts = (CustomerCommunicationContacts) cc[1];
                SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
                smsCommunicationCustomers.setCustomerCommunicationId(customerCommunications.getId());
                smsCommunicationCustomers.setSmsCommunicationId(smsCommunication.getId());
                smsCommunicationCustomers.setCustomerDetailId(customerDetails.get().getId());
                smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
                smsCommunicationCustomers.setContractNumber(contractNumber);
                smsCommunicationCustomers.setContractId(contractId);
                smsCommunicationCustomers.setServiceContractDetailid(serviceContractDetailId);
                smsCommunicationCustomers.setProductContractDetailId(productContractDetailId);
                smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);
                log.info("SAVED SMS COMMUNICATION CUSTOMER, id {}", smsCommunicationCustomers.getId());
                log.info("SMS COMMUNICATION ID {}", smsCommunicationCustomers.getSmsCommunicationId());
                SmsCommunicationCustomerContacts smsCommunicationCustomerContacts = new SmsCommunicationCustomerContacts();
                smsCommunicationCustomerContacts.setCustomerCommunicationContactId(customerCommunicationContacts.getId());
                smsCommunicationCustomerContacts.setSmsCommunicationCustomerId(smsCommunicationCustomers.getId());
                smsCommunicationCustomerContacts.setPhoneNumber(customerCommunicationContacts.getContactValue());
                smsCommunicationCustomerContactsRepository.saveAndFlush(smsCommunicationCustomerContacts);
                checkComm.add(customerCommunications.getId());

//                SmsSendParamBase smsSendParamMass = createSmsSendParam(smsCommunicationCustomers.getId(), customerCommunicationContacts.getContactValue(), smsCommunicationCustomers);
//                smsToSend.add(smsSendParamMass);
                atLeastOne.set(true);
            }
        }
    }

    public SmsSendParamBase createSmsSendParam(Long id, String contactValue, SmsCommunicationCustomers smsCommunicationCustomers) {
        SmsSendParamBase smsSendParamBase = new SmsSendParamBase(constructRequestId(id), contactValue, smsCommunicationCustomers);
        log.info("Request id of sms is : {}", smsSendParamBase.getRequestId());
        return smsSendParamBase;
    }

    private String constructRequestId(Long id) {
        return REQUEST_ID_PREFIX + id;
    }

    private SmsCommunicationCustomers createSmsCommunicationCustomer(Long smsCommunicationId, CustomerDetails customerDetails, Long productContractDetailId, Long serviceContractId) {
        SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
        smsCommunicationCustomers.setSmsCommunicationId(smsCommunicationId);
        smsCommunicationCustomers.setProductContractDetailId(productContractDetailId);
        smsCommunicationCustomers.setServiceContractDetailid(serviceContractId);
        smsCommunicationCustomers.setCustomerDetailId(customerDetails.getId());
        return smsCommunicationCustomers;
    }

    private void validateRequest(List<String> errorMessages, SmsCommunication smsCommunication, Long topicOfCommunicationId, Long exchangeCodeId, Set<Long> relatedCustomerIds, Set<Long> communicationFileIds) {
        checkAndSetTopicOfCommunication(topicOfCommunicationId, errorMessages, smsCommunication);
//        checkAndSetExchangeCode(exchangeCodeId,errorMessages,smsCommunication);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        smsCommunicationRepository.saveAndFlush(smsCommunication);

        validateAndSetRelatedCustomers(smsCommunication, relatedCustomerIds, errorMessages);
        validateAndSetCommunicationFiles(smsCommunication.getId(), communicationFileIds, errorMessages);
    }

    /**
     * Checks and sets the exchange code for the given SMS communication.
     *
     * @param exchangeCodeId   the ID of the exchange code to check and set
     * @param errorMessages    a list to store any error messages encountered during the process
     * @param smsCommunication the SMS communication object to update with the exchange code
     * @return the SMS sending number associated with the exchange code, or null if the exchange code is not found or not active
     */
    private SmsSendingNumber checkAndSetExchangeCode(Long exchangeCodeId, List<String> errorMessages, SmsCommunication smsCommunication) {
        Optional<SmsSendingNumber> exchangeCode = smsSendingNumberRepository.findById(exchangeCodeId);

        if (!Objects.equals(exchangeCodeId, smsCommunication.getSmsSendingNumberId())) {
            if (exchangeCode.isEmpty()) {
                errorMessages.add("Exchange code not found");
            } else {
                SmsSendingNumber smsSendingNumber = exchangeCode.get();
                if (smsSendingNumber.getStatus() != NomenclatureItemStatus.ACTIVE) {
                    errorMessages.add("exchange code status is not active");
                }
                smsCommunication.setSmsSendingNumberId(exchangeCodeId);

                return smsSendingNumber;
            }
        }
        return exchangeCode.orElse(null);
    }

    private void checkAndSetTopicOfCommunication(Long topicOfCommunicationId, List<String> errorMessages, SmsCommunication smsCommunication) {
        if (!Objects.equals(topicOfCommunicationId, smsCommunication.getCommunicationTopicId())) {
            Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByIdAndStatus(topicOfCommunicationId, NomenclatureItemStatus.ACTIVE);
            if (topicOfCommunicationOptional.isEmpty()) {
                errorMessages.add("Topic of communication not found or not active");
            }
            smsCommunication.setCommunicationTopicId(topicOfCommunicationId);
        }

    }

    /**
     * Validates and sets the communication data for an SMS communication.
     *
     * @param smsCommunication the SMS communication object
     * @param request          the SMS communication base requestMass
     * @param errorMessages    a list to store any error messages encountered during the process
     * @return the created or updated SMS communication customer object, or null if there are any errors
     */
    private SmsCommunicationCustomers validateAndSetCommunicationData(SmsCommunication smsCommunication, SmsCommunicationBaseRequest request, List<String> errorMessages) {
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findById(request.getCustomerDetailId());
        if (customerDetailsOptional.isPresent()) {
            CustomerDetails customerDetails = customerDetailsOptional.get();
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(customerDetails.getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isPresent()) {
                Optional<CustomerCommunications> customerCommunicationOptional = customerCommunicationsRepository.findByIdAndCustomerDetailsIdAndStatus(request.getCustomerCommunicationId(), customerDetails.getId(), Status.ACTIVE);
                if (customerCommunicationOptional.isPresent()) {
                    Optional<CustomerCommunicationContacts> customerCommunicationContacts = customerCommunicationContactsRepository.findMobileContactByCommunicationId(customerCommunicationOptional.get().getId());
                    if (customerCommunicationContacts.isPresent()) {
                        CustomerCommunicationContacts get = customerCommunicationContacts.get();
                        SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
                        smsCommunicationCustomers.setCustomerCommunicationId(customerCommunicationOptional.get().getId());
                        smsCommunicationCustomers.setSmsCommunicationId(smsCommunication.getId());
                        smsCommunicationCustomers.setCustomerDetailId(customerDetails.getId());
                        setStatus(smsCommunicationCustomers, request);
                        smsCommunicationCustomers = smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);

                        if (request.getTemplateId() != null && request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
                            generateAndSetSmsBody(request, errorMessages, smsCommunicationCustomers, customerOptional.get());
                        }

                        SmsCommunicationCustomerContacts smsCommunicationCustomerContacts = new SmsCommunicationCustomerContacts();
                        smsCommunicationCustomerContacts.setCustomerCommunicationContactId(get.getId());
                        smsCommunicationCustomerContacts.setSmsCommunicationCustomerId(smsCommunicationCustomers.getId());
                        smsCommunicationCustomerContacts.setPhoneNumber(request.getCustomerPhoneNumber());
                        smsCommunicationCustomerContactsRepository.saveAndFlush(smsCommunicationCustomerContacts);
                        return smsCommunicationCustomers;
                    } else {
                        errorMessages.add("customer communication not found with send SMS!;");
                    }
                } else {
                    errorMessages.add("customer communication not found with such id!;");
                }
            } else {
                errorMessages.add("active customer not found with with customer detail id!;");
            }
        } else {
            errorMessages.add("customer details not found!;");
        }
        return null;
    }

    private void generateAndSetSmsBody(SmsCommunicationBaseRequest request, List<String> errorMessages, SmsCommunicationCustomers smsCommunicationCustomers, Customer customer) {
        EmailAndSmsDocumentModel emailAndSmsDocumentModel = emailAndSmsDocumentCreationService.generateDocumentJsonModel(
                new EmailAndSmsDocumentRequest(
                        null, null,
                        smsCommunicationCustomers.getId(),
                        null, null, null
                )
        );

        String destinationPath = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        ContractTemplate template = contractTemplateRepository.findById(request.getTemplateId()).orElseThrow(
                () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(request.getTemplateId()))
        );

        ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository.findById(template.getLastTemplateDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Can't find template details with id: %s".formatted(template.getLastTemplateDetailId())));
        String fileName = UUID.randomUUID().toString();
        try {
            ByteArrayResource templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(contractTemplateDetail)).toPath()));
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            emailAndSmsDocumentModel,
                            Set.of(FileFormat.DOCX),
                            false
                    );

            ByteArrayResource byteArrayResource = fileService.downloadFile(documentPathPayloads.docXPath());
            String smsBody = null;
            try {
                smsBody = parseDocx(byteArrayResource.getByteArray());
            } catch (Exception e) {
                log.error("Can't parse docx file", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            smsCommunicationCustomers.setSmsBody(smsBody);
            smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);
        } catch (Exception e) {
            log.error("Exception while storing template file: {}", e.getMessage());
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    public String generateAndSetSmsBodyForReminder(Long templateId, ReminderProcessItem reminderProcessItemRow, SmsCommunicationCustomers smsCommunicationCustomers, Long customerDetailId) {
        ReminderDocumentModel reminderDocumentModel = reminderDocumentGenerationService.generateReminderJson(
                reminderProcessItemRow.getReminderId(),
                reminderProcessItemRow.getLiabilityId(),
                customerDetailId,
                reminderProcessItemRow.getTotalAmount(),
                reminderProcessItemRow.getCommunicationId());

        String destinationPath = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        ContractTemplate template = contractTemplateRepository.findById(templateId).orElseThrow(
                () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
        );

        ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository.findById(template.getLastTemplateDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Can't find template details with id: %s".formatted(template.getLastTemplateDetailId())));
        String fileName = UUID.randomUUID().toString();
        try {
            ByteArrayResource templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(contractTemplateDetail)).toPath()));
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            reminderDocumentModel,
                            Set.of(FileFormat.DOCX),
                            false
                    );

            ByteArrayResource byteArrayResource = fileService.downloadFile(documentPathPayloads.docXPath());
            String smsBody = null;
            try {
                smsBody = parseDocx(byteArrayResource.getByteArray());
            } catch (Exception e) {
                log.error("Can't parse docx file", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            smsCommunicationCustomers.setSmsBody(smsBody);
            smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);
            return smsBody;
        } catch (Exception e) {
            log.error("Exception while storing template file: {}", e.getMessage());
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    private void setStatus(SmsCommunicationCustomers smsCommunicationCustomers, SmsCommunicationBaseRequest request) {
        if (request.getSaveAs().equals(CommunicationSave.SAVE_AS_DRAFT)) {
            smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.DRAFT);
        } else if (request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
            smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
        } else {
            smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.RECEIVED);
        }
    }

    private boolean isCommunicationDataChanged(SmsCommunicationCustomers smsCommunicationCustomers, SmsCommunicationBaseRequest request) {
        return !(Objects.equals(smsCommunicationCustomers.getCustomerDetailId(), request.getCustomerDetailId())
                && Objects.equals(smsCommunicationCustomers.getCustomerCommunicationId(), request.getCustomerCommunicationId()));
    }

    private SmsCommunicationCustomers getActiveSmsCommunicationCustomer(Long smsCommunicationId) {
        return smsCommunicationCustomersRepository.findBySmsCommunicationId(smsCommunicationId).orElseThrow(() -> new DomainEntityNotFoundException("Not found active sms communication customer!;"));
    }

    public SmsCommunicationCustomers validateAndUpdateCustomerData(SmsCommunication smsCommunication, SmsCommunicationBaseRequest request, List<String> errorMessages) {
        SmsCommunicationCustomers smsCommunicationCustomers = getActiveSmsCommunicationCustomer(smsCommunication.getId());
        if (isCommunicationDataChanged(smsCommunicationCustomers, request)) {
            smsCommunicationCustomerContactsRepository.deleteAllBySmsCommunicationCustomerId(smsCommunicationCustomers.getId());
            smsCommunicationCustomersRepository.deleteById(smsCommunicationCustomers.getId());
            return validateAndSetCommunicationData(smsCommunication, request, errorMessages);
        } else {
            Optional<SmsCommunicationCustomerContacts> customerContactsOptional = smsCommunicationCustomerContactsRepository.getContactBySmsCommunicationId(smsCommunication.getId());
            Customer customer = customerRepository.findByCustomerDetailIdAndStatusIn(smsCommunicationCustomers.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE)).orElseThrow(() -> new DomainEntityNotFoundException("Customer not found!;"));
            if (customerContactsOptional.isEmpty()) {
                errorMessages.add("Sms Communication Customer contact not found!;");
            } else {
                SmsCommunicationCustomerContacts customerContacts = customerContactsOptional.get();
                customerContacts.setPhoneNumber(request.getCustomerPhoneNumber());
                setStatus(smsCommunicationCustomers, request);
                if (request.getTemplateId() != null && request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
                    generateAndSetSmsBody(request, errorMessages, smsCommunicationCustomers, customer);
                }
                return smsCommunicationCustomers;
            }

        }
        return null;
    }

    /**
     * Validates and sets the communication files associated with an SMS communication.
     *
     * @param smsCommunicationId the ID of the SMS communication
     * @param communicationFiles the set of file IDs to be associated with the SMS communication
     * @param errorMessages      a list to store any error messages encountered during the validation process
     */
    private void validateAndSetCommunicationFiles(Long smsCommunicationId, Set<Long> communicationFiles, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(communicationFiles)) {
            for (Long current : communicationFiles) {
                Optional<SmsCommunicationFiles> smsCommunicationFilesOptional = smsCommunicationFilesRepository.findByIdAndStatus(current, EntityStatus.ACTIVE);
                if (smsCommunicationFilesOptional.isPresent()) {
                    SmsCommunicationFiles smsCommunicationFiles = smsCommunicationFilesOptional.get();
                    smsCommunicationFiles.setSmsCommunicationId(smsCommunicationId);
                    smsCommunicationFilesRepository.saveAndFlush(smsCommunicationFiles);
                } else {
                    errorMessages.add("communicationFileIds-[communicationFileIds] active file with id: %s can't be found;".formatted(current));
                }
            }

            if (CollectionUtils.isEmpty(errorMessages)) {
                List<String> fileUrls = smsCommunicationFilesRepository.findAllActiveFileBySmsCommunicationId(smsCommunicationId);

                long sum = fileUrls
                        .stream()
                        .map(fileService::downloadFile)
                        .mapToLong(byteArrayResource -> byteArrayResource.getByteArray().length)
                        .sum();

                if (sum > MAX_SIZE_COMMUNICATION_FILES) {
                    errorMessages.add("communicationFileIds-[communicationFileIds] you have reached max file size witch is 50 mb;");
                }
            }

        }
    }

    private void validateAndSetRelatedCustomers(SmsCommunication smsCommunication, Set<Long> relatedCustomers, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(relatedCustomers)) {
            relatedCustomers.forEach(current -> {
                Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(current, List.of(CustomerStatus.ACTIVE));
                if (customerOptional.isPresent()) {
                    var relatedCustomer = new SmsCommunicationRelatedCustomers();
                    relatedCustomer.setCustomerId(customerOptional.get().getId());
                    relatedCustomer.setSmsCommunicationId(smsCommunication.getId());
                    relatedCustomer.setStatus(EntityStatus.ACTIVE);
                    smsCommunicationRelatedCustomersRepository.saveAndFlush(relatedCustomer);
                } else {
                    errorMessages.add("relatedCustomerIds-[relatedCustomerIds] active customer with id: %s can't be found;".formatted(current));
                }
            });
        }
    }

    private void checkPermissions(SmsCommunicationBaseRequest request, SmsCommunication smsCommunication) {
        if (request.getSaveAs().equals(CommunicationSave.SAVE_AS_DRAFT)) {
            checkPermission(PermissionEnum.SMS_COMMUNICATION_CREATE_DRAFT);
        } else if (request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
            AccountManager accountManager = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));
            smsCommunication.setSenderEmployeeId(accountManager.getId());
            checkPermission(PermissionEnum.SMS_COMMUNICATION_CREATE_AND_SEND);
            smsCommunication.setSentDate(LocalDateTime.now());
        } else {
            checkPermission(PermissionEnum.SMS_COMMUNICATION_CREATE_SAVE);
            smsCommunication.setSentDate(request.getDateAndTime());
        }
    }

    private void checkPermissionsForMassSMS(MassSMSSaveAs saveAS, SmsCommunication smsCommunication) {
        if (saveAS.equals(MassSMSSaveAs.DRAFT)) {
            checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_CREATE_DRAFT);
            smsCommunication.setCommunicationStatus(SmsCommStatus.DRAFT);
        } else {
            AccountManager accountManager = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));
            checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_CREATE_SEND);
            smsCommunication.setCommunicationStatus(SmsCommStatus.SENT);
            smsCommunication.setSentDate(LocalDateTime.now());
            smsCommunication.setSenderEmployeeId(accountManager.getId());
        }
    }

    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(SMS_COMMUNICATION).contains(permission.getId()))
            throw new ClientException("You don't have appropriate permission: %s;".formatted(permission.name()), ErrorCode.OPERATION_NOT_ALLOWED);
    }

    private void checkPermissionForMass(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(MASS_SMS_COMMUNICATION).contains(permission.getId()))
            throw new ClientException("You don't have appropriate permission: %s;".formatted(permission.name()), ErrorCode.OPERATION_NOT_ALLOWED);
    }

    /**
     * Uploads a file for SMS communication and saves it to the database.
     *
     * @param file     The file to be uploaded.
     * @param statuses The list of document file statuses for the uploaded file.
     * @return A response containing the uploaded file and its associated statuses.
     * @throws IllegalArgumentsProvidedException if the file size exceeds the maximum allowed size or if the file name is empty or null.
     */

    public FileWithStatusesResponse uploadFile(MultipartFile file, List<DocumentFileStatus> statuses) {
        log.debug("Sms communication file {}.", file.getName());

        if (checkCommunicationFileSize(file)) {
            log.error("You have reached max file size witch is 50 mb;");
            throw new IllegalArgumentsProvidedException("You have reached max file size witch is 50 mb;");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Sms communication file name is empty or null");
            throw new IllegalArgumentsProvidedException("Email communication file name is empty or null");
        }

        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = getFileName(formattedFileName);
        String fileUrl = fileService.uploadFile(file, String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now()), fileName);

        var smsCommunicationFiles = new SmsCommunicationFiles();
        smsCommunicationFiles.setLocalFileUrl(fileUrl);
        smsCommunicationFiles.setStatus(EntityStatus.ACTIVE);
        smsCommunicationFiles.setName(formattedFileName);
        smsCommunicationFiles.setReport(false);
        smsCommunicationFiles.setFileStatuses(statuses);
        smsCommunicationFilesRepository.saveAndFlush(smsCommunicationFiles);
        return new FileWithStatusesResponse(smsCommunicationFiles, accountManagerRepository.findByUserName(smsCommunicationFiles.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    private boolean checkCommunicationFileSize(MultipartFile file) {
        return (file.getSize() > MAX_SIZE_COMMUNICATION_FILES);
    }

    private String getFileName(String originalFileName) {
        return String.format("%s_%s", UUID.randomUUID(), originalFileName.replaceAll("\\s+", ""));
    }


    private boolean checkOnPermission(PermissionContextEnum permissionContext, List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(permissionContext, requiredPermissions);
    }

    private List<SmsCommStatus> getCommunicationStatuses() {
        List<SmsCommStatus> communicationStatuses = new ArrayList<>();
        if (checkOnPermission(SMS_COMMUNICATION, List.of(PermissionEnum.SMS_COMMUNICATION_VIEW_DRAFT))) {
            communicationStatuses.add(SmsCommStatus.DRAFT);
        }
        if (checkOnPermission(SMS_COMMUNICATION, List.of(PermissionEnum.SMS_COMMUNICATION_VIEW_SEND))) {
            communicationStatuses.add(SmsCommStatus.SENT);
            communicationStatuses.add(SmsCommStatus.SENT_SUCCESSFULLY);
            communicationStatuses.add(SmsCommStatus.SEND_FAILED);
            communicationStatuses.add(SmsCommStatus.RECEIVED);
            communicationStatuses.add(SmsCommStatus.IN_PROGRESS);
        }
        return communicationStatuses;
    }

    /**
     * Updates an existing SMS communication.
     *
     * @param request            the request object containing the updated SMS communication details
     * @param smsCommunicationId the ID of the SMS communication to be updated
     * @return the ID of the updated SMS communication
     * @throws DomainEntityNotFoundException if the SMS communication is not found
     * @throws ClientException               if there are any errors during the update process
     */
    @Transactional
    public Long update(SmsCommunicationBaseRequest request, Long smsCommunicationId) {
        List<String> errorMessages = new ArrayList<>();
        SmsCommunicationCustomers smsCommunicationCustomersInitial = smsCommunicationCustomersRepository.findById(smsCommunicationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found;"));

        SmsCommunication smsCommunication = smsCommunicationRepository.findBySmsCommunicationCustomerId(smsCommunicationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found;"));
        validateUpdateActionByStatuses(smsCommunication, smsCommunicationCustomersInitial);
        checkPermissionsForEdit(request, smsCommunication);
        smsCommunication.setSmsBody(request.getSmsBody());
        smsCommunication.setCommunicationAsInstitution(request.isCommunicationAsInstitution());

        if (!smsCommunication.getCommunicationType().equals(request.getCommunicationType())) {
            throw new OperationNotAllowedException("It is not possible to change Type of Communication");
        }
        validateAndSetTemplate(smsCommunication, request.getTemplateId(), ContractTemplatePurposes.SMS);
        checkAndSetTopicOfCommunication(request.getTopicOfCommunicationId(), errorMessages, smsCommunication);
        SmsSendingNumber smsSendingNumber = checkAndSetExchangeCode(request.getExchangeCodeId(), errorMessages, smsCommunication);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        validateAndUpdateRelatedCustomers(smsCommunication, request.getRelatedCustomerIds(), errorMessages);
        validateAndUpdateCommunicationFiles(smsCommunication.getId(), request.getCommunicationFileIds(), errorMessages);
        SmsCommunicationCustomers smsCommunicationCustomers = validateAndUpdateCustomerData(smsCommunication, request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
            SmsSendParamBase smsSendParamSingle = createSmsSendParam(smsCommunicationCustomers.getId(), request.getCustomerPhoneNumber(), smsCommunicationCustomers);
            smsCommunicationSendHelperService.send(smsSendParamSingle, request.getSmsBody(), smsSendingNumber.getSmsNumber(), null);
        }

        archiveFiles(smsCommunication);

        return smsCommunicationCustomers.getId();
    }

    /**
     * Updates an existing mass SMS communication.
     *
     * @param request            the request object containing the updated mass SMS communication details
     * @param smsCommunicationId the ID of the mass SMS communication to be updated
     * @return the ID of the updated mass SMS communication
     * @throws DomainEntityNotFoundException if the mass SMS communication is not found
     * @throws ClientException               if there are any errors during the update process
     */
    public Long updateMassSms(MassSmsCreateRequest request, Long smsCommunicationId) {
        List<String> errorMessages = new ArrayList<>();
        AtomicReference<SmsSendingNumber> smsSendingNumber = new AtomicReference<>();
        List<Long> smsCommunicationCustomersBySmsCommunicationId = smsCommunicationCustomersRepository.findSmsCommunicationCustomersBySmsCommunicationId(smsCommunicationId);

        SmsCommunication smsComm = transactionTemplate.execute(sms -> {
            SmsCommunication smsCommunication = smsCommunicationRepository.findMassById(smsCommunicationId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Mass SMS communication not found with id" + smsCommunicationId));
            validateMassSMSUpdate(smsCommunication);
            checkPermissionForEditMass(request, smsCommunication);
            smsCommunication.setSmsBody(request.getSmsBody());
            smsCommunication.setAllCustomersWithActiveContract(request.isAllCustomersWithActiveContract());
            smsCommunication.setCommunicationAsInstitution(request.isCommunicationAsInstitution());
            smsCommunication.setCommunicationType(CommunicationType.OUTGOING);
            validateAndSetTemplate(smsCommunication, request.getTemplateId(), ContractTemplatePurposes.SMS);

            checkAndSetTopicOfCommunication(request.getTopicOfCommunicationId(), errorMessages, smsCommunication);
            smsSendingNumber.set(checkAndSetExchangeCode(request.getExchangeCodeId(), errorMessages, smsCommunication));
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

            smsCommunicationRepository.saveAndFlush(smsCommunication);

            validateAndUpdateRelatedCustomers(smsCommunication, request.getRelatedCustomerIds(), errorMessages);
            validateAndUpdateCommunicationFiles(smsCommunicationId, request.getCommunicationFileIds(), errorMessages);
            updateContactPurposes(request.getContactPurposeIds(), errorMessages, smsCommunication);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);


            Set<MassSMSCustomerRequest> customers = request.isAllCustomersWithActiveContract() ? customerRepository.fetchActiveContractsAndAssociatedCustomersForMassCommunication().stream().map(MassSMSCustomerRequest::new).collect(Collectors.toSet())
                    : request.getCustomers();
            AtomicBoolean atLeastOne = new AtomicBoolean(false);
            Map<String, String> reportMap = validateAndSetContactPurposesAndCustomers(customers, smsCommunication, request.getSaveAs().equals(MassSMSSaveAs.SEND), request.getContactPurposeIds(), smsSendingNumber.get().getSmsNumber(), atLeastOne, request.getTemplateId(), request.getSmsBody(), request.isAllCustomersWithActiveContract());
            if (!atLeastOne.get()) {
                throw new ClientException(constructMessage(reportMap), ErrorCode.UNSUPPORTED_OPERATION);
            }
            smsCommunicationCustomersRepository.deleteAllByIdIn(smsCommunicationCustomersBySmsCommunicationId);
            if (!reportMap.isEmpty()) {
                try {
                    generateReport(reportMap, smsCommunication, request.getSaveAs().equals(MassSMSSaveAs.SEND));
                } catch (Exception e) {
                    log.debug("report generation failed");
                    throw new ClientException("Report generation failed!", APPLICATION_ERROR);
                }
            }
            return smsCommunication;
        });
        return smsComm.getId();

    }

    /**
     * Validates and updates the related customers for the given SMS communication.
     *
     * @param smsCommunication   the SMS communication to update
     * @param relatedCustomerIds the IDs of the related customers to add or remove
     * @param errorMessages      a list to store any error messages encountered during the update
     */

    private void validateAndUpdateRelatedCustomers(SmsCommunication smsCommunication, Set<Long> relatedCustomerIds, List<String> errorMessages) {
        Map<Long, SmsCommunicationRelatedCustomers> existingMap = EPBListUtils.transformToMap(smsCommunicationRelatedCustomersRepository.findAllBySmsCommunicationIdAndStatus(smsCommunication.getId(), EntityStatus.ACTIVE), SmsCommunicationRelatedCustomers::getId);

        if (CollectionUtils.isNotEmpty(relatedCustomerIds)) {
            Set<Long> added = relatedCustomerIds
                    .stream()
                    .filter(x -> !existingMap.containsKey(x))
                    .collect(Collectors.toSet());

            validateAndSetRelatedCustomers(smsCommunication, added, errorMessages);
            validateAndDeleteRelatedCustomers(existingMap, relatedCustomerIds);
        } else {
            validateAndDeleteRelatedCustomers(existingMap, SetUtils.emptySet());
        }
    }

    /**
     * Validates the update action based on the status of the SMS communication and its related customers.
     * <p>
     * This method checks the status of the SMS communication and its related customers to ensure that the update action is allowed. If the SMS communication is in the 'ACTIVE' status and the related customers have a status of 'SENT', 'IN_PROGRESS', 'SEND_FAILED', or 'SENT_SUCCESSFULLY', or if the SMS communication is a 'MASS_SMS' communication, the method will throw an 'OperationNotAllowedException'. If the SMS communication is in the 'DELETED' status, the method will also throw an 'OperationNotAllowedException'.
     *
     * @param smsCommunication          the SMS communication to validate
     * @param smsCommunicationCustomers the related customers of the SMS communication
     * @throws OperationNotAllowedException if the update action is not allowed based on the status of the SMS communication and its related customers
     */
    private void validateUpdateActionByStatuses(SmsCommunication smsCommunication, SmsCommunicationCustomers smsCommunicationCustomers) {
        switch (smsCommunication.getStatus()) {
            case ACTIVE -> {
                if (Objects.requireNonNull(smsCommunicationCustomers.getSmsCommStatus()) == SmsCommStatus.SENT
                        || Objects.requireNonNull(smsCommunicationCustomers.getSmsCommStatus()) == SmsCommStatus.IN_PROGRESS ||
                        Objects.requireNonNull(smsCommunicationCustomers.getSmsCommStatus()) == SmsCommStatus.SEND_FAILED ||
                        Objects.requireNonNull(smsCommunicationCustomers.getSmsCommStatus()) == SmsCommStatus.SENT_SUCCESSFULLY) {
                    log.error("You cannot update sent sms communication.");
                    throw new OperationNotAllowedException("You cannot update sent sms communication.");
                }
                if (Objects.requireNonNull(smsCommunication.getCommunicationChannel()) == SmsCommunicationChannel.MASS_SMS) {
                    log.error("You cannot update mass email communication.");
                    throw new OperationNotAllowedException("You cannot update mass email communication.");
                }
            }
            case DELETED -> {
                log.error("You cannot update deleted email communication.");
                throw new OperationNotAllowedException("You cannot update deleted email communication.");
            }
        }
    }

    /**
     * Validates the update action for a MASS SMS communication based on its status.
     * <p>
     * This method checks the status of the MASS SMS communication to ensure that the update action is allowed. If the MASS SMS communication is in the 'SENT' status, the method will throw an 'OperationNotAllowedException'. If the MASS SMS communication is in the 'DELETED' status, the method will also throw an 'OperationNotAllowedException'.
     *
     * @param smsCommunication the MASS SMS communication to validate
     * @throws OperationNotAllowedException if the update action is not allowed based on the status of the MASS SMS communication
     */

    private void validateMassSMSUpdate(SmsCommunication smsCommunication) {
        switch (smsCommunication.getStatus()) {
            case ACTIVE -> {
                if (Objects.requireNonNull(smsCommunication.getCommunicationStatus()) == SmsCommStatus.SENT) {
                    log.error("You cannot update sent MASS sms communication.");
                    throw new OperationNotAllowedException("You cannot update sent MASS sms communication.");
                }
            }
            case DELETED -> {
                log.error("You cannot update deleted Mass SMS communication.");
                throw new OperationNotAllowedException("You cannot update deleted MASS sms communication.");
            }
        }
    }

    /**
     * Checks the permissions and sets the communication status for an SMS communication based on the save action requested.
     * <p>
     * If the save action is 'SAVE_AS_DRAFT', the communication status is set to 'DRAFT'.
     * If the save action is 'SAVE_AND_SEND', the method checks the 'SMS_COMMUNICATION_CREATE_AND_SEND' permission, sets the communication status to 'SENT', and sets the sender employee ID and sent date.
     * If the save action is neither 'SAVE_AS_DRAFT' nor 'SAVE_AND_SEND', the communication status is set to 'RECEIVED' and the sent date is set to the requested date and time.
     *
     * @param request          the SMS communication request containing the save action
     * @param smsCommunication the SMS communication to update
     */
    private void checkPermissionsForEdit(SmsCommunicationBaseRequest request, SmsCommunication smsCommunication) {
        if (request.getSaveAs().equals(CommunicationSave.SAVE_AS_DRAFT)) {
            smsCommunication.setCommunicationStatus(SmsCommStatus.DRAFT);
        } else if (request.getSaveAs().equals(CommunicationSave.SAVE_AND_SEND)) {
            checkPermission(PermissionEnum.SMS_COMMUNICATION_CREATE_AND_SEND);
            smsCommunication.setCommunicationStatus(SmsCommStatus.SENT);
            AccountManager accountManager = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));
            smsCommunication.setSenderEmployeeId(accountManager.getId());
            smsCommunication.setSentDate(LocalDateTime.now());
        } else {
            smsCommunication.setCommunicationStatus(SmsCommStatus.RECEIVED);
            smsCommunication.setSentDate(request.getDateAndTime());
        }
    }

    private void checkPermissionForEditMass(MassSmsCreateRequest request, SmsCommunication smsCommunication) {
        if (request.getSaveAs().equals(MassSMSSaveAs.DRAFT)) {
            smsCommunication.setCommunicationStatus(SmsCommStatus.DRAFT);
        } else {
            checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_CREATE_SEND);
            AccountManager accountManager = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));
            smsCommunication.setCommunicationStatus(SmsCommStatus.SENT);
            smsCommunication.setSentDate(LocalDateTime.now());
            smsCommunication.setSenderEmployeeId(accountManager.getId());
        }
    }

    private void validateAndDeleteRelatedCustomers(Map<Long, SmsCommunicationRelatedCustomers> existingMap, Set<Long> requestedCustomerIds) {
        List<SmsCommunicationRelatedCustomers> deleted = existingMap
                .entrySet()
                .stream()
                .filter(relatedCustomer -> !requestedCustomerIds.contains(relatedCustomer.getKey()))
                .map(Map.Entry::getValue)
                .peek(relatedCustomer -> relatedCustomer.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            smsCommunicationRelatedCustomersRepository.saveAllAndFlush(deleted);
        }
    }


    private void validateAndUpdateCommunicationFiles(Long smsCommunicationId, Set<Long> fileIds, List<String> errorMessages) {
        Map<Long, SmsCommunicationFiles> existingMap = EPBListUtils.transformToMap(smsCommunicationFilesRepository.findAllActiveFileByCommunicationId(smsCommunicationId), SmsCommunicationFiles::getId);
        if (CollectionUtils.isNotEmpty(fileIds)) {
            Set<Long> added = fileIds
                    .stream()
                    .filter(current -> !existingMap.containsKey(current))
                    .collect(Collectors.toSet());

            validateAndSetCommunicationFiles(smsCommunicationId, added, errorMessages);
            validateAndDeleteCommunicationFiles(existingMap, fileIds);
        } else {
            validateAndDeleteCommunicationFiles(existingMap, SetUtils.emptySet());
        }
    }

    private void validateAndDeleteCommunicationFiles(Map<Long, SmsCommunicationFiles> existingMap, Set<Long> files) {
        List<SmsCommunicationFiles> deleted = existingMap
                .entrySet()
                .stream()
                .filter(entry -> !files.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .peek(emailCommunicationFile -> emailCommunicationFile.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            smsCommunicationFilesRepository.saveAllAndFlush(deleted);
        }
    }

    /**
     * Deletes an SMS communication with the given ID.
     *
     * @param id the ID of the SMS communication to delete
     * @throws DomainEntityNotFoundException if the SMS communication or its associated customers are not found
     * @throws OperationNotAllowedException  if the SMS communication is already deleted or its status is not in the allowed states (draft or received)
     */

    @Transactional
    public void delete(Long id) {
        log.info("Deleting sms communication with id {}", id);
        var smsCommunication = smsCommunicationRepository.findBySmsCommunicationCustomerId(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found!"));
        SmsCommunicationCustomers smsCommunicationCustomers = smsCommunicationCustomersRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found!"));

        if (smsCommunication.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("Can not delete sms communication with status deleted!");
        } else {
            if (!smsCommunicationCustomers.getSmsCommStatus().equals(SmsCommStatus.DRAFT) && !smsCommunicationCustomers.getSmsCommStatus().equals(SmsCommStatus.RECEIVED)) {
                throw new OperationNotAllowedException("Sms communication delete is only allowed when it's sent!");
            }

        }
        smsCommunication.setStatus(EntityStatus.DELETED);
    }

    /**
     * Deletes a mass SMS communication with the given ID.
     *
     * @param id the ID of the mass SMS communication to delete
     * @throws DomainEntityNotFoundException if the mass SMS communication is not found
     * @throws OperationNotAllowedException  if the mass SMS communication is already deleted or its status is not in the allowed state (draft)
     */
    @Transactional
    public void deleteMassSms(Long id) {
        var smsCommunication = smsCommunicationRepository.findMassById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Mass sms communication not found!"));
        if (smsCommunication.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("Can not delete mass sms with status deleted!;");
        } else {
            if (smsCommunication.getCommunicationStatus() != SmsCommStatus.DRAFT) {
                throw new OperationNotAllowedException("Mass sms communication delete is only allowed when status is draft!;");
            }
        }
        smsCommunication.setStatus(EntityStatus.DELETED);
    }

    /**
     * Retrieves the details of an SMS communication with the given ID.
     *
     * @param id the ID of the SMS communication to retrieve
     * @return a {@link SmsCommunicationViewResponse} containing the details of the SMS communication
     * @throws DomainEntityNotFoundException if the SMS communication or its associated customers are not found
     */
    public SmsCommunicationViewResponse view(Long id) {
        var smsCommunicationCustomers = smsCommunicationCustomersRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer communications not found!"));
        var smsCommunication = smsCommunicationRepository.findBySmsCommunicationCustomerId(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found!"));
        checkPermission(smsCommunication, smsCommunicationCustomers);
        var view = from(smsCommunication, smsCommunicationCustomers);
        view.setCommunicationStatus(smsCommunicationCustomers.getSmsCommStatus());
        view.setSmsCommunicationChannel(SmsCommunicationChannel.SMS);
        CustomerCommunicationDataResponse shortResponse = smsCommunicationCustomersRepository.getCustomerCommunicationDataBySmsCommunicationCustomerId(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer communication not found !;"));
        view.setCustomerCommunicationDataShortResponse(shortResponse);
        view.setCreatorEmployeeShortResponse(getCreatorEmployeeShortResponse(smsCommunication.getSystemUserId()));
        ShortResponse topicOfCommunicationShortResponse = getTopicOfCommunicationShortResponse(smsCommunication.getCommunicationTopicId());
        view.setTopicOfCommunication(topicOfCommunicationShortResponse);
        view.setRelatedCustomersShortResponse(smsCommunicationRelatedCustomersRepository.findBySmsCommunicationId(smsCommunication.getId()));
        view.setFilesShortResponse(getEmailCommunicationFilesShortResponse(smsCommunication.getId()));
        view.setTaskShortResponse(getTasks(smsCommunicationCustomers.getId()));
        view.setActivityShortResponse(smsCommunicationActivityService.getActivitiesByConnectedObjectId(smsCommunicationCustomers.getId()));
        var customer = smsCommunicationCustomersRepository.getBySmsCommunicationCustomerId(id).orElseThrow(() -> new DomainEntityNotFoundException("Customer not found!"));
        view.setCustomerShortResponse(customer);
        view.setExchangeCode(getSmsSendingNumber(smsCommunication.getSmsSendingNumberId()));
        view.setPhoneNumber(getPhoneNumber(smsCommunicationCustomers.getId()));
        view.setSmsBody(smsCommunication.getSmsBody() == null ? smsCommunicationCustomers.getSmsBody() : smsCommunication.getSmsBody());
        if (smsCommunication.getSenderEmployeeId() != null) {
            view.setSenderEmployeeShortResponse(Objects.equals(topicOfCommunicationShortResponse.name(), "Reminder for disconnection") ? new ShortResponse(null, "System") : getSenderEmployee(smsCommunication.getSenderEmployeeId()));
        }
        Set<Long> contactPurposesOfSms = getContactPurposesOfSms(smsCommunication.getId());
        String purposes = contactPurposesOfSms == null || contactPurposesOfSms.isEmpty() ? customerRepository.getConcatPurposeFromCustomerCommunicationData(shortResponse.getId()) : customerRepository.getContactPurposeFromCustomerCommunicationDataAndPurposeIds(shortResponse.getId(), contactPurposesOfSms);
        view.getCustomerCommunicationDataShortResponse().setConcatPurposes(purposes);
        contractTemplateRepository.findTemplateResponseById(smsCommunication.getTemplateId(), LocalDate.now()).ifPresent(view::setTemplateResponse);
        return view;
    }

    public Set<Long> getContactPurposesOfSms(Long smsCommId) {
        return smsCommunicationContactPurposeRepository.findContactPurposeIdsBySmsCommunicationId(smsCommId);
    }

    private String getPhoneNumber(Long smsCommunicationCustomerId) {
        return smsCommunicationCustomerContactsRepository.getPhoneNumberBySmsCommunicationCustomerId(smsCommunicationCustomerId);
    }

    /**
     * Retrieves the details of a mass SMS communication with the given ID.
     *
     * @param id the ID of the mass SMS communication to retrieve
     * @return a {@link MassSMSCommunicationViewResponse} containing the details of the mass SMS communication
     * @throws DomainEntityNotFoundException if the mass SMS communication or its associated data are not found
     */
    public MassSMSCommunicationViewResponse massSmsView(Long id) {
        var smsCommunication = smsCommunicationRepository.findMassById(id).orElseThrow(() -> new DomainEntityNotFoundException("Mass communication not found with id" + id));
        checkPermissionsForView(smsCommunication);
        List<MassSMSCustomerAndContractResponse> customers = smsCommunication.isAllCustomersWithActiveContract() ? List.of() : smsCommunicationCustomersRepository.getMassCustomers(id);
        var view = fromMass(smsCommunication);
        view.setCreatorEmployeeShortResponse(getCreatorEmployeeShortResponse(smsCommunication.getSystemUserId()));
        view.setTopicOfCommunication(getTopicOfCommunicationShortResponse(smsCommunication.getCommunicationTopicId()));
        view.setRelatedCustomers(smsCommunicationRelatedCustomersRepository.findBySmsCommunicationId(smsCommunication.getId()));
        view.setFilesShortResponse(getEmailCommunicationFilesShortResponse(smsCommunication.getId()));
        view.setTaskShortResponse(getTasksForMassSms(smsCommunication.getId()));
        view.setActivityShortResponse(massSMSCommunicationActivityService.getActivitiesByConnectedObjectId(id));
        view.setCustomers(customers);
        view.setAllCustomersWithActiveContract(smsCommunication.isAllCustomersWithActiveContract());
        view.setReports(smsCommunicationFilesRepository.findReportsBySmsCommunicationId(smsCommunication.getId()));
        view.setExchangeCode(getSmsSendingNumber(smsCommunication.getSmsSendingNumberId()));
        if (smsCommunication.getSenderEmployeeId() != null) {
            view.setSenderEmployeeShortResponse(getSenderEmployee(smsCommunication.getSenderEmployeeId()));
        }
        List<ShortResponse> contactPurposesBySmsCommunicationId = smsCommunicationContactPurposeRepository.findContactPurposesBySmsCommunicationId(smsCommunication.getId());
        view.setContactPurposes(contactPurposesBySmsCommunicationId);
        contractTemplateRepository.findTemplateResponseById(smsCommunication.getTemplateId(), LocalDate.now()).ifPresent(view::setTemplateResponse);
        return view;
    }


    public ShortResponse getSenderEmployee(Long senderEmployeeId) {
        AccountManager accountManager = accountManagerRepository.findById(senderEmployeeId).orElseThrow(() -> new DomainEntityNotFoundException("Sender employee not found!"));
        return new ShortResponse(accountManager.getId(), accountManager.getDisplayName());
    }

    public ShortResponse getSmsSendingNumber(Long exchangeCodeId) {
        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository.findById(exchangeCodeId).orElseThrow(() -> new DomainEntityNotFoundException("Exchange code not found!"));
        return new ShortResponse(smsSendingNumber.getId(), smsSendingNumber.getName());
    }

    private List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksBySmsCommunicationId(id);
    }

    private List<TaskShortResponse> getTasksForMassSms(Long id) {
        return taskService.getTasksByMassSmsCommunicationId(id);
    }

    private List<FileWithStatusesResponse> getEmailCommunicationFilesShortResponse(Long smsCommunicationId) {
        return smsCommunicationFilesRepository.findAllActiveFileByCommunicationId(smsCommunicationId).stream().map(emailCommunicationFile -> new FileWithStatusesResponse(emailCommunicationFile, accountManagerRepository.findByUserName(emailCommunicationFile.getSystemUserId()).map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""))).toList();
    }

    private ShortResponse getCreatorEmployeeShortResponse(String systemUserId) {
        if (Objects.equals(systemUserId, "system")) {
            return new ShortResponse(null, "system");
        }

        return accountManagerRepository.findByUserName(systemUserId).map(accountManager -> new ShortResponse(accountManager.getId(), accountManager.getDisplayName())).orElseThrow(() -> new DomainEntityNotFoundException("Unable to find employee with username %s;".formatted(systemUserId)));
    }

    private SmsCommunicationViewResponse from(SmsCommunication smsCommunication, SmsCommunicationCustomers smsCommunicationCustomers) {
        var smsCommunicationViewResponse = new SmsCommunicationViewResponse();
        smsCommunicationViewResponse.setId(smsCommunicationCustomers.getId());
        smsCommunicationViewResponse.setCommunicationType(smsCommunication.getCommunicationType());
        smsCommunicationViewResponse.setCommunicationAsInstitution(smsCommunication.isCommunicationAsInstitution());
        smsCommunicationViewResponse.setSendDate(smsCommunication.getSentDate());
        smsCommunicationViewResponse.setEntityStatus(smsCommunication.getStatus());
        smsCommunicationViewResponse.setSmsCommunicationChannel(smsCommunication.getCommunicationChannel());
        return smsCommunicationViewResponse;
    }

    private MassSMSCommunicationViewResponse fromMass(SmsCommunication smsCommunication) {
        var smsCommunicationViewResponse = new MassSMSCommunicationViewResponse();
        smsCommunicationViewResponse.setId(smsCommunication.getId());
        smsCommunicationViewResponse.setCommunicationStatus(smsCommunication.getCommunicationStatus());
        smsCommunicationViewResponse.setMessageBody(smsCommunication.getSmsBody());
        smsCommunicationViewResponse.setCommunicationType(smsCommunication.getCommunicationType());
        smsCommunicationViewResponse.setCommunicationAsInstitution(smsCommunication.isCommunicationAsInstitution());
        smsCommunicationViewResponse.setDateAndTime(smsCommunication.getSentDate());
        smsCommunicationViewResponse.setEntityStatus(smsCommunication.getStatus());
        smsCommunicationViewResponse.setCommunicationChannel(smsCommunication.getCommunicationChannel());
        return smsCommunicationViewResponse;
    }

    private ShortResponse getTopicOfCommunicationShortResponse(Long topicOfCommunicationId) {
        return topicOfCommunicationRepository.findById(topicOfCommunicationId).map(topicOfCommunication -> new ShortResponse(topicOfCommunication.getId(), topicOfCommunication.getName())).orElseThrow(() -> new DomainEntityNotFoundException("Active Topic of communication does not exists with given id [%s];".formatted(topicOfCommunicationId)));
    }

    private void checkPermissionsForView(SmsCommunication smsCommunication) {
        if (smsCommunication.getStatus().equals(EntityStatus.DELETED)) {
            if (smsCommunication.getCommunicationChannel().equals(SmsCommunicationChannel.MASS_SMS)) {
                checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_DELETED);
            } else {
                checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_DELETE);
            }
        } else {
            if (smsCommunication.getCommunicationStatus().equals(SmsCommStatus.DRAFT) || smsCommunication.getCommunicationStatus().equals(SmsCommStatus.RECEIVED)) {
                if (smsCommunication.getCommunicationChannel().equals(SmsCommunicationChannel.MASS_SMS)) {
                    checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_DRAFT);
                } else {
                    checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_DRAFT);

                }
            } else {
                if (smsCommunication.getCommunicationChannel().equals(SmsCommunicationChannel.MASS_SMS)) {
                    checkPermissionForMass(PermissionEnum.MASS_SMS_COMMUNICATION_VIEW_SENT);
                } else {
                    checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_SEND);
                }
            }
        }
    }

    private void checkPermission(SmsCommunication smsCommunication, SmsCommunicationCustomers smsCommunicationCustomers) {
        if (smsCommunication.getStatus().equals(EntityStatus.DELETED)) {
            checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_DELETE);
        } else {
            if (smsCommunicationCustomers.getSmsCommStatus().equals(SmsCommStatus.DRAFT) || smsCommunicationCustomers.getSmsCommStatus().equals(SmsCommStatus.RECEIVED)) {
                checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_DRAFT);
            } else {
                checkPermission(PermissionEnum.SMS_COMMUNICATION_VIEW_SEND);
            }
        }
    }

    public FileContent downloadProxyFile(Long id) {
        var proxyFile = smsCommunicationFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getLocalFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }

    public FileContent checkForArchivationAndDownload(Long id) throws Exception {
        var smsCommunicationFiles = smsCommunicationFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(smsCommunicationFiles.getIsArchived())) {
            if (Objects.isNull(smsCommunicationFiles.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(smsCommunicationFiles.getDocumentId(), smsCommunicationFiles.getFileId());

                return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
            }
        }

        var content = fileService.downloadFile(smsCommunicationFiles.getLocalFileUrl());
        return new FileContent(smsCommunicationFiles.getName(), content.getByteArray());
    }

    public FileContent downloadReport(Long reportId) {
        var proxyFile = smsCommunicationFilesRepository.findByReportId(reportId, EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("Report not found!"));
        var content = fileService.downloadFile(proxyFile.getLocalFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }

    public byte[] getTemplate(MassSMSImportType importType) {
        var template = templateRepository.findById(importType.name()).orElseThrow(() -> new DomainEntityNotFoundException("Template not found !;"));
        try {
            return fileService.downloadFile(template.getFileUrl()).getByteArray();
        } catch (Exception e) {
            throw new ClientException("File fetch failed!;", ErrorCode.APPLICATION_ERROR);
        }
    }


    /**
     * Parses a multi-part file containing SMS customer or contract data, and returns a set of parsed SMS customers.
     *
     * @param file       The multi-part file to be parsed.
     * @param importType The type of import, either MASS_SMS_CUSTOMER or MASS_SMS_CONTRACT.
     * @return A set of parsed SMS customers.
     * @throws DomainEntityNotFoundException If the template for the given import type is not found.
     * @throws ClientException               If an exception occurs while trying to parse the uploaded template.
     */
    public MassCommunicationImportProcessResult parse(MultipartFile file, MassSMSImportType importType) {
        EPBExcelUtils.validateFileFormat(file);
        Template template = templateRepository.findById(importType.name()).orElseThrow(() -> new DomainEntityNotFoundException("Template not found!"));

        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(template.getFileUrl()).getByteArray(), 1);

        List<MassCommunicationFileProcessedResult> temp = new ArrayList<>();
        List<MassCommunicationFileProcessedResult> result = Collections.synchronizedList(temp);
        StringBuffer errors = new StringBuffer();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Row> rows = new ArrayList<>();
            Iterator<Row> iterator = sheet.iterator();
            if (iterator.hasNext()) iterator.next();

            while (iterator.hasNext()) {
                rows.add(iterator.next());
            }

            int batchSize = rows.size() / numberOfThreads + 1;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            List<Callable<Void>> callables = new ArrayList<>();
            List<List<Row>> partitions = ListUtils.partition(rows, batchSize);

            for (List<Row> partition : partitions) {
                callables.add(() -> {
                    for (Row row : partition) {
                        if (importType.equals(MassSMSImportType.MASS_SMS_CUSTOMER)) {
                            processCustomerImport(row, result, errors);
                        } else {
                            processContractImport(row, result, errors);
                        }
                    }
                    return null;
                });
            }
            executorService.invokeAll(callables);
            executorService.shutdown();

        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }

        return new MassCommunicationImportProcessResult(new HashSet<>(result), errors.toString().isBlank() ? null : errors.toString());
    }

    private void processCustomerImport(Row row, List<MassCommunicationFileProcessedResult> result, StringBuffer errors) {
        String customerIdentifier = EPBExcelUtils.getStringValue(0, row);
        if (Objects.nonNull(customerIdentifier)) {
            String versionId = EPBExcelUtils.getStringValue(1, row);
            if (Objects.nonNull(versionId)) {
                result.add(new MassCommunicationFileProcessedResult(customerIdentifier, Long.parseLong(versionId), null, null));
            } else {
                Long versionIdByIdentifierAndStatus = customerRepository.findLastVersionIdByIdentifierAndStatus(customerIdentifier, CustomerStatus.ACTIVE);
                if (Objects.nonNull(versionIdByIdentifierAndStatus)) {
                    result.add(new MassCommunicationFileProcessedResult(customerIdentifier, Long.parseLong(versionId), null, null));
                } else {
                    errors.append(String.format("Customer with identifier %s Does not exists!;", customerIdentifier));
                }
            }
        }
    }

    public void processContractImport(Row row, List<MassCommunicationFileProcessedResult> result, StringBuffer errors) {
        String contractNumber = EPBExcelUtils.getStringValue(0, row);
        if (Objects.nonNull(contractNumber)) {
            String versionId = EPBExcelUtils.getStringValue(1, row);
            if (Objects.nonNull(versionId)) {
                processContractInfoVersionId(contractNumber, Long.parseLong(versionId), result, errors);
            } else {
                processContractInfoByLatestVersionId(contractNumber, result, errors);
            }
        }
    }

    public void processContractInfoVersionId(String contractNumber, Long versionId, List<MassCommunicationFileProcessedResult> result, StringBuffer errors) {
        if (Objects.nonNull(versionId)) {
            boolean found = false;
            MassCommunicationFileProcessedResultProjection serviceContractProjection = serviceContractsRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(contractNumber, versionId);
            if (Objects.nonNull(serviceContractProjection)) {
                result.add(new MassCommunicationFileProcessedResult(serviceContractProjection));
                found = true;
            } else {
                MassCommunicationFileProcessedResultProjection productContractProjection = productContractRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(contractNumber, versionId);
                if (Objects.nonNull(productContractProjection)) {
                    result.add(new MassCommunicationFileProcessedResult(productContractProjection));
                    found = true;
                }
            }
            if (!found) {
                errors.append(String.format("Contract with number %s and version %s doesn't exist!;", contractNumber, versionId));
            }
        }
    }

    public void processContractInfoByLatestVersionId(String contractNumber, List<MassCommunicationFileProcessedResult> result, StringBuffer errors) {
        if (Objects.nonNull(contractNumber)) {
            boolean found = false;
            Long productContractLatestVersionId = productContractRepository.findLatestProductContractDetailVersionId(contractNumber);
            if (Objects.nonNull(productContractLatestVersionId)) {
                MassCommunicationFileProcessedResultProjection productContractProjection = productContractRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(contractNumber, productContractLatestVersionId);
                if (Objects.nonNull(productContractProjection)) {
                    result.add(new MassCommunicationFileProcessedResult(productContractProjection));
                    found = true;
                }
            } else {
                Long serviceContractLatestVersionId = serviceContractsRepository.findLatestServiceContractDetailVersionId(contractNumber);
                if (Objects.nonNull(serviceContractLatestVersionId)) {
                    MassCommunicationFileProcessedResultProjection serviceContractProjection = serviceContractsRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(contractNumber, serviceContractLatestVersionId);
                    if (Objects.nonNull(serviceContractProjection)) {
                        result.add(new MassCommunicationFileProcessedResult(serviceContractProjection));
                        found = true;
                    }
                }
            }
            if (!found) {
                errors.append(String.format("Contract with number %s doesn't exist!;", contractNumber));
            }
        }
    }


    private void validateAndSetTemplate(SmsCommunication smsCommunication, Long templateId, ContractTemplatePurposes purposes) {
        if (Objects.equals(smsCommunication.getTemplateId(), templateId)) return;
        if (templateId == null) {
            smsCommunication.setTemplateId(null);
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, purposes, ContractTemplateType.SMS, LocalDate.now())) {
            throw new DomainEntityNotFoundException("templateId-Template with id %s does not exist or has different purpose!;".formatted(templateId));
        }
        smsCommunication.setTemplateId(templateId);
    }


    public Long resend(Long id) {

        SmsCommunicationCustomers originalSmsCommunicationCustomers = smsCommunicationCustomersRepository.findById(id).orElseThrow(() -> new DomainEntityNotFoundException("Sms Communication Customer not found with id:" + id));

        SmsCommunication originalSmsCommunication = smsCommunicationRepository.findById(originalSmsCommunicationCustomers.getSmsCommunicationId()).orElseThrow(() -> new DomainEntityNotFoundException("SMS Communication not found with id: " + originalSmsCommunicationCustomers.getSmsCommunicationId()));

        if (originalSmsCommunication.getCommunicationType().equals(CommunicationType.INCOMING)) {
            throw new ClientException("Can't resend incoming sms!", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (originalSmsCommunicationCustomers.getSmsCommStatus().equals(SmsCommStatus.DRAFT)) {
            throw new ClientException("Can't resend draft SMS!", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (originalSmsCommunication.getSentDate().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new ClientException("SMS can only be resent within 30 days of original sending", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long smsCommunicationId = originalSmsCommunication.getId();

        if (!originalSmsCommunication.getCommunicationChannel().equals(SmsCommunicationChannel.MASS_SMS)) {
            SmsCommunication newSmsCommunication = new SmsCommunication();

            newSmsCommunication.setCommunicationAsInstitution(originalSmsCommunication.isCommunicationAsInstitution());
            newSmsCommunication.setCommunicationChannel(originalSmsCommunication.getCommunicationChannel());
            newSmsCommunication.setCommunicationTopicId(originalSmsCommunication.getCommunicationTopicId());
            newSmsCommunication.setCommunicationType(CommunicationType.OUTGOING);
            newSmsCommunication.setSmsSendingNumberId(originalSmsCommunication.getSmsSendingNumberId());
            newSmsCommunication.setStatus(EntityStatus.ACTIVE);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            String additionalInfo = String.format(" This message was sent on %s.", LocalDateTime.now().format(formatter));
            newSmsCommunication.setSentDate(LocalDateTime.parse(LocalDateTime.now().format(formatter), formatter));
            newSmsCommunication.setSmsBody(originalSmsCommunication.getSmsBody());

            newSmsCommunication.setTemplateId(originalSmsCommunication.getTemplateId());
            AccountManager currentUser = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId()).orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!"));

            newSmsCommunication.setSenderEmployeeId(currentUser.getId());
            SmsCommunication savedSmsCommunication = smsCommunicationRepository.save(newSmsCommunication);
            smsCommunicationId = savedSmsCommunication.getId();
        }


        SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
        smsCommunicationCustomers.setCustomerDetailId(originalSmsCommunicationCustomers.getCustomerDetailId());
        smsCommunicationCustomers.setSmsCommunicationId(smsCommunicationId);
        smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
        smsCommunicationCustomers.setCustomerCommunicationId(originalSmsCommunicationCustomers.getCustomerCommunicationId());
        smsCommunicationCustomers.setSmsBody(originalSmsCommunicationCustomers.getSmsBody());
        smsCommunicationCustomers.setProductContractDetailId(originalSmsCommunicationCustomers.getProductContractDetailId());
        smsCommunicationCustomers.setContractNumber(originalSmsCommunicationCustomers.getContractNumber());
        smsCommunicationCustomers.setServiceContractDetailid(originalSmsCommunicationCustomers.getServiceContractDetailid());

        SmsCommunicationCustomers savedSmsCommunicationCustomers = smsCommunicationCustomersRepository.save(smsCommunicationCustomers);


        String phoneNumber = smsCommunicationCustomerContactsRepository.getPhoneNumberBySmsCommunicationCustomerId(originalSmsCommunicationCustomers.getId());
        SmsCommunicationCustomerContacts smsCommunicationCustomerContacts = new SmsCommunicationCustomerContacts();
        smsCommunicationCustomerContacts.setPhoneNumber(phoneNumber);
        smsCommunicationCustomerContacts.setSmsCommunicationCustomerId(savedSmsCommunicationCustomers.getId());
        smsCommunicationCustomerContactsRepository.save(smsCommunicationCustomerContacts);

//        SmsSendingNumber smsSendingNumber = smsSendingNumberRepository.findById(originalSmsCommunication.getSmsSendingNumberId())
//                .orElseThrow(() -> new DomainEntityNotFoundException("SMS Sending Number not found with id: " + originalSmsCommunication.getSmsSendingNumberId()));


//        SmsSendParamBase smsSendParamBase = createSmsSendParam(savedSmsCommunicationCustomers.getId(), phoneNumber, savedSmsCommunicationCustomers);

//        smsCommunicationSendHelperService.send(
//                smsSendParamBase,
//                savedSmsCommunication.getSmsBody(),
//                smsSendingNumber.getSmsNumber(),
//                null
//        );

        return smsCommunicationCustomers.getId();
    }

    /**
     * Creates an SMS communication from a document.
     *
     * @param request the request containing the SMS communication details
     * @param send    whether to send the SMS immediately
     * @return the ID of the created SMS communication
     */
    @Transactional
    public Long createSmsFromDocument(DocumentSmsCommunicationCreateRequest request, boolean send) {
        log.debug("Creating SMS from document");
        List<String> errorMessages = new ArrayList<>();

        try {
            SmsCommunication smsCommunication = new SmsCommunication();
            smsCommunication.setSmsBody(request.getSmsBody());
            smsCommunication.setCommunicationAsInstitution(false);
            smsCommunication.setCommunicationType(CommunicationType.OUTGOING);
            smsCommunication.setStatus(EntityStatus.ACTIVE);
            smsCommunication.setCommunicationChannel(SmsCommunicationChannel.SMS);
            smsCommunication.setAllCustomersWithActiveContract(false);
            smsCommunication.setTemplateId(request.getSmsTemplateId());
            smsCommunication.setSystemUserId("system");

            if (request.getCommunicationTopicId() != null) {
                Optional<TopicOfCommunication> topicOfCommunication = topicOfCommunicationRepository.findByIdAndStatus(request.getCommunicationTopicId(), NomenclatureItemStatus.ACTIVE);

                if (topicOfCommunication.isEmpty()) {
                    errorMessages.add("Topic of communication not found or not active");
                }

                smsCommunication.setCommunicationTopicId(request.getCommunicationTopicId());
            } else {
                errorMessages.add("Communication topic ID is required");
            }

            if (request.getSmsNumberId() != null) {
                Optional<SmsSendingNumber> smsSendingNumber = smsSendingNumberRepository.findById(request.getSmsNumberId());

                if (smsSendingNumber.isEmpty() || smsSendingNumber.get().getStatus() != NomenclatureItemStatus.ACTIVE) {
                    errorMessages.add("SMS sending number not found or not active");
                }

                smsCommunication.setSmsSendingNumberId(request.getSmsNumberId());
            } else {
                errorMessages.add("SMS number ID is required");
            }

            smsCommunication = smsCommunicationRepository.saveAndFlush(smsCommunication);

            SmsCommunicationCustomers smsCommunicationCustomers = new SmsCommunicationCustomers();
            smsCommunicationCustomers.setCustomerDetailId(request.getCustomerDetailId());
            smsCommunicationCustomers.setCustomerCommunicationId(request.getCustomerCommunicationId());
            smsCommunicationCustomers.setSmsCommunicationId(smsCommunication.getId());
            smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);

            if (!Objects.equals(smsCommunication.getSmsBody(), request.getSmsBody())) {
                smsCommunicationCustomers.setSmsBody(request.getSmsBody());
            }

            smsCommunicationCustomers = smsCommunicationCustomersRepository.saveAndFlush(smsCommunicationCustomers);

            SmsCommunicationCustomerContacts smsCommunicationCustomerContacts = new SmsCommunicationCustomerContacts();
            smsCommunicationCustomerContacts.setSmsCommunicationCustomerId(smsCommunicationCustomers.getId());
            smsCommunicationCustomerContacts.setPhoneNumber(request.getCustomerPhoneNumber());

            smsCommunicationCustomerContactsRepository.saveAndFlush(smsCommunicationCustomerContacts);

            if (send) {
                try {
                    SmsSendingNumber smsSendingNumber = smsSendingNumberRepository.findById(smsCommunication.getSmsSendingNumberId()).orElseThrow(() -> new DomainEntityNotFoundException("SMS sending number not found"));

                    SmsSendParamBase smsSendParam = new SmsSendParamBase(REQUEST_ID_PREFIX + smsCommunicationCustomers.getId(), request.getCustomerPhoneNumber(), smsCommunicationCustomers);

                    smsCommunicationSendHelperService.send(smsSendParam, request.getSmsBody(), smsSendingNumber.getSmsNumber(), null);
                } catch (Exception e) {
                    log.error("Error sending SMS: {}", e.getMessage(), e);
                    smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.SEND_FAILED);
                    smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
                }
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            smsCommunicationRepository.saveAndFlush(smsCommunication);
            archiveFiles(smsCommunication);
            log.debug("SMS communication created with ID: {}", smsCommunicationCustomers.getId());
            return smsCommunicationCustomers.getId();
        } catch (Exception e) {
            log.error("Error occurred while creating sms communication for document: {}", request.getCustomerDetailId(), e);
            throw e;
        }
    }
}