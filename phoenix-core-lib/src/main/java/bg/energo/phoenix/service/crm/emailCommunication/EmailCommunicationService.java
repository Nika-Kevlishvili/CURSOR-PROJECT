package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.mass_comm.models.Attachment;
import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.*;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderProcessItem;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.*;
import bg.energo.phoenix.model.enums.crm.massEmailCommunication.EmailCreateType;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.crm.emailCommunication.*;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.crm.emailCommunication.*;
import bg.energo.phoenix.model.response.customer.communicationData.CustomerEmailCommDataMiddleResponse;
import bg.energo.phoenix.model.response.customer.communicationData.CustomerEmailCommunicationPurposeKeyPair;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.*;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentCreationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentRequest;
import bg.energo.phoenix.service.document.ReminderDocumentGenerationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.EMAIL_COMMUNICATION;
import static bg.energo.phoenix.permissions.PermissionContextEnum.MASS_EMAIL_COMMUNICATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCommunicationService {
    private final static String FOLDER_PATH = "EmailCommunicationFiles";
    private final static String ATTACHMENT_FOLDER_PATH = "EmailCommunicationAttachmentFiles";
    private static final long MAX_SIZE_COMMUNICATION_FILES = 50 * 1024 * 1024;
    private static final long MAX_SIZE_ATTACHMENT_FILES = 25 * 1024 * 1024;
    private static final int THREAD_SIZE = 10;
    private static final String SHEET = "Customer Data";
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContactPurposeRepository contactPurposeRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailCommunicationCustomerContactRepository emailCommunicationCustomerContactRepository;
    private final EmailCommunicationRelatedCustomerRepository emailCommunicationRelatedCustomerRepository;
    private final EmailCommunicationContactPurposeRepository emailCommunicationContactPurposeRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final EmailCommunicationCustomerAttachmentRepository emailCommunicationCustomerAttachmentRepository;
    private final EmailCommunicationCustomerRepository emailCommunicationCustomerRepository;
    private final EmailCommunicationActivityService emailCommunicationActivityService;
    private final EmailCommunicationFileRepository emailCommunicationFileRepository;
    private final EmailCommunicationSenderService emailCommunicationSenderService;
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final EmailCommunicationMapper emailCommunicationMapper;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final EmailCommunicationHelper emailCommunicationHelper;
    private final EmailAndSmsDocumentCreationService emailAndSmsDocumentCreationService;
    private final ReminderDocumentGenerationService reminderDocumentGenerationService;
    private final MassEmailCommunicationActivityService massEmailCommunicationActivityService;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ProductContractRepository productContractRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final TransactionTemplate transactionTemplate;
    private final TemplateRepository templateRepository;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final EmailCommunicationTemplateRepository emailCommunicationTemplateRepository;
    private final TaskService taskService;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Creates an email communication based on the provided request.
     * This method handles the creation of an email communication entity by validating and setting various attributes
     * such as email box, topic of communication, communication files, attachments, and customer data.
     * It logs the creation operation and throws exceptions if any validation errors occur during the process.
     *
     * @param request the request object containing data to create the email communication
     * @return the ID of the created email communication
     * @throws ClientException       if validation e.
     *                               rrors occur during the creation process
     * @throws IllegalStateException if repository operations fail
     */
    @Transactional
    public Long create(EmailCommunicationCreateRequest request) {
        log.info("Create email communication: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();
        EmailCommunication emailCommunication = emailCommunicationMapper.fromCreateRequestToEntity(request);

        validateAndSetSendingStatusByPermissionOnCreate(emailCommunication, request.getEmailCreateType());
        validateAndSetEmailBox(emailCommunication, request.getEmailBoxId(), errorMessages);
        validateAndSetTopicOfCommunication(emailCommunication, request.getCommunicationTopicId(), errorMessages);
        validateAndSetTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, emailCommunication, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        emailCommunicationRepository.saveAndFlush(emailCommunication);
        saveTemplates(request.getTemplateIds(), emailCommunication.getId(), errorMessages);
        validateAndSetCommunicationFiles(emailCommunication.getId(), request.getCommunicationFileIds(), errorMessages);
        validateAndSetAttachments(emailCommunication.getId(), request.getAttachmentFileIds(), errorMessages);
        validateAndSetRelatedCustomers(emailCommunication, request.getRelatedCustomerIds(), errorMessages);
        validateAndSetCustomerData(
                emailCommunication,
                request.getCustomerDetailId(),
                request.getCustomerCommunicationId(),
                request.getCustomerEmailAddress(),
                errorMessages
        );

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        validateAndSetEmailBodyByTemplate(emailCommunication);
        validateStatusAndSend(emailCommunication, true);

        emailCommunicationRepository.saveAndFlush(emailCommunication);

        archiveFiles(emailCommunication);

        return emailCommunication.getId();
    }

    /**
     * Creates an email communication for a document based on the provided request.
     *
     * @param request The request containing the necessary details to create the email communication.
     * @return The ID of the created email communication.
     */
    @Transactional
    public Long createEmailFromDocument(DocumentEmailCommunicationCreateRequest request, boolean needPermission) {
        log.info("Create email communication for document: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        try {
            EmailCommunication emailCommunication = emailCommunicationMapper.fromDocumentRequestToEntity(request);
            log.info("Mapped EmailCommunication entity: {}", emailCommunication);

            validateAndSetTopicOfCommunication(emailCommunication, request.getCommunicationTopicId(), errorMessages);
            log.info("Topic set for communication: {}", request.getCommunicationTopicId());

            validateAndSetEmailBox(emailCommunication, request.getEmailBoxId(), errorMessages);
            log.info("Email box set for communication: {}", emailCommunication.getEmailBoxId());

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

            emailCommunicationRepository.saveAndFlush(emailCommunication);
            log.info("Email communication saved with ID: {}", emailCommunication.getId());

            saveTemplatesForDocument(request.getTemplateIds(), emailCommunication.getId(), errorMessages);
            log.info("Templates saved for email communication with ID: {}", emailCommunication.getId());

            validateAndSetAttachments(emailCommunication.getId(), request.getAttachmentFileIds(), errorMessages);
            log.info("Attachments validated and set for email communication with ID: {}", emailCommunication.getId());

            validateAndSetCustomerData(
                    emailCommunication,
                    request.getCustomerDetailId(),
                    request.getCustomerCommunicationId(),
                    request.getCustomerEmailAddress(),
                    errorMessages
            );

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

            validateStatusAndSend(emailCommunication, needPermission);
            log.info("Email communication status validated and prepared for sending: {}", emailCommunication.getEmailCommunicationStatus());

            emailCommunicationRepository.saveAndFlush(emailCommunication);
            archiveFiles(emailCommunication);
            return emailCommunication.getId();
        } catch (Exception e) {
            log.error("Error occurred while creating email communication for document: {}", request.getCustomerDetailId(), e);
            throw e;
        }
    }

    /**
     * Updates an email communication entity based on the provided edit request.
     * This method performs the following steps in a transactional context:
     * Logs the update action with the details of the request.
     * Initializes an empty list to collect error messages encountered during validation and update.
     * Retrieves the existing email communication entity by its ID.
     * Validates the update action based on the statuses of the email communication entity.
     * Maps the data from the edit request to update the email communication entity.
     * Validates and updates communication files associated with the email communication.
     * Validates and updates attachments associated with the email communication.
     * Validates and updates customer data associated with the email communication.
     * Validates and updates the email box ID associated with the email communication.
     * Validates and updates related customers associated with the email communication.
     * Validates and updates the topic of communication associated with the email communication.
     * Throws an exception if any error messages were collected during validation and update.
     * Validates and prepares the email communication entity for sending.
     * Returns the ID of the updated email communication entity.
     *
     * @param id      The ID of the email communication entity to be updated.
     * @param request The edit request containing updated data for the email communication.
     * @return The ID of the updated email communication entity after successful update.
     * @throws ClientException If any error messages were collected during validation and update.
     */
    @Transactional
    public Long update(Long id, EmailCommunicationEditRequest request) {
        log.info("Update email communication: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        EmailCommunication emailCommunicationEntity = findEmailCommunicationById(id);
        validateUpdateActionByStatuses(emailCommunicationEntity);
        validateUpdateByCommunicationType(emailCommunicationEntity.getEmailCommunicationType(), request.getEmailCommunicationType());
        validateAndSetSendingStatusByPermissionOnUpdate(emailCommunicationEntity, request.getEmailCreateType());
        validateAndSetTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, emailCommunicationEntity, errorMessages);
        emailCommunicationMapper.fromEditRequestToEntity(emailCommunicationEntity, request);

        validateAndUpdateTopicOfCommunication(request.getCommunicationTopicId(), emailCommunicationEntity, errorMessages);
        validateAndUpdateRelatedCustomers(emailCommunicationEntity, request.getRelatedCustomerIds(), errorMessages);
        validateAndUpdateAndSetEmailBox(request.getEmailBoxId(), emailCommunicationEntity, errorMessages);
        validateAndUpdateCustomerData(emailCommunicationEntity, request, errorMessages);
        validateAndUpdateAttachments(id, request.getAttachmentFileIds(), errorMessages);
        validateAndUpdateCommunicationFiles(id, request.getCommunicationFileIds(), errorMessages);
        updateTemplates(request.getTemplateIds(), emailCommunicationEntity.getId(), errorMessages);
        emailCommunicationRepository.saveAndFlush(emailCommunicationEntity);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        validateAndSetEmailBodyByTemplate(emailCommunicationEntity);
        validateStatusAndSend(emailCommunicationEntity, true);

        emailCommunicationRepository.saveAndFlush(emailCommunicationEntity);

        archiveFiles(emailCommunicationEntity);

        return emailCommunicationEntity.getId();
    }

    /**
     * Deletes an email communication with the specified ID.
     * This method marks the email communication entity with the given ID as deleted by updating its status.
     * It performs checks to ensure the email communication exists and is eligible for deletion based on its status.
     * Throws exceptions if the email communication is not found, is already deleted, or has a status of 'sent'.
     *
     * @param id the ID of the email communication to delete
     * @return the ID of the deleted email communication
     * @throws DomainEntityNotFoundException if no email communication is found with the specified ID
     * @throws OperationNotAllowedException  if the email communication is already deleted or has a status of 'sent'
     * @throws IllegalStateException         if repository operations fail
     */
    @Transactional
    public Long delete(Long id) {
        log.info("Deleting email communication with id: {}", id);
        EmailCommunication emailCommunication = findEmailCommunicationById(id);
        validateDeleteActionByStatuses(emailCommunication);

        emailCommunication.setEntityStatus(EntityStatus.DELETED);
        emailCommunicationRepository.saveAndFlush(emailCommunication);
        return id;
    }

    /**
     * Resends an email communication identified by the given ID.
     * This method retrieves the original email communication, validates the resend action based on
     * certain criteria (status, communication channel, send date), and creates a new instance of
     * {@link EmailCommunication} for the resend operation. The new email communication is saved with
     * updated status and attributes related to the original communication.
     * Upon successful validation and creation, this method also handles setting the topic, email box,
     * attachments, customer data, and performs additional validations before saving the resent email.
     * If any validation checks fail, appropriate error messages are accumulated and thrown as an
     * {@link EPBChainedExceptionTriggerUtil} if necessary.
     * The method is transactional, ensuring that all database operations within this method are
     * committed or rolled back as a single unit of work.
     *
     * @param id The ID of the email communication to be resent.
     * @return The ID of the newly created {@link EmailCommunication} instance for the resend operation.
     * @throws EPBChainedExceptionTriggerUtil Thrown if there are validation errors that prevent the resend operation.
     */
    @Transactional
    public Long resend(Long id) {
        log.info("Resend email communication wih ID: %s ".formatted(id));
        List<String> errorMessages = new ArrayList<>();

        EmailCommunication emailCommunication = findEmailCommunicationById(id);

        validateResendActionByStatuses(emailCommunication.getEmailCommunicationStatus(), emailCommunication.getEntityStatus());
        validateResendByCommunicationChannel(emailCommunication.getCommunicationChannel());
        validateResendByCommunicationType(emailCommunication.getEmailCommunicationType());
        validateResendBySendDate(emailCommunication.getSentDate());

        EmailCommunication resendEmail = emailCommunicationMapper.fromEntityToResendEntity(emailCommunication);
        validateAndSetTopicOfCommunicationOnResend(resendEmail, emailCommunication.getCommunicationTopicId(), errorMessages);
        validateAndSetEmailBoxOnResend(resendEmail, emailCommunication.getEmailBoxId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        emailCommunicationRepository.saveAndFlush(resendEmail);

        String emailAddress = String.join(
                ";",
                emailCommunicationCustomerContactRepository.findAllEmailAddressesByEmailCommunicationId(emailCommunication.getId())
        );
        EmailCommunicationCustomer emailCommunicationCustomer = getActiveEmailCommunicationCustomerOrElseNull(emailCommunication.getId());
        validateAndSetCustomerData(
                resendEmail,
                emailCommunicationCustomer == null ? null : emailCommunicationCustomer.getCustomerDetailId(),
                emailCommunicationCustomer == null ? null : emailCommunicationCustomer.getCustomerCommunicationId(),
                emailAddress,
                errorMessages
        );
        attachDocumentTemplatesAndGeneratedAttachmentsToResendEmail(
                id,
                resendEmail.getId()
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        validateStatusAndSend(resendEmail, true);

        emailCommunicationRepository.saveAndFlush(resendEmail);
        return resendEmail.getId();
    }

    /**
     * Copies active document templates and attachments from an existing email communication
     * to a new email communication. This is useful when resending an email while keeping
     * its associated templates and attachments intact.
     * The method performs the following steps:
     * Retrieves all active email templates linked to the original email communication.</li>
     * Creates new copies of those templates associated with the new email communication.</li>
     * Retrieves all active attachments linked to the original email communication.</li>
     * Creates new copies of those attachments associated with the new email communication.</li>
     * Saves the copied templates and attachments in the database.</li>
     *
     * @param emailCommunicationIdToResend The ID of the original email communication
     *                                     from which templates and attachments are copied.
     * @param emailCommunicationIdToAttach The ID of the new email communication
     *                                     to which templates and attachments are attached.
     */
    private void attachDocumentTemplatesAndGeneratedAttachmentsToResendEmail(
            Long emailCommunicationIdToResend,
            Long emailCommunicationIdToAttach
    ) {
        List<EmailCommunicationTemplates> templatesToAttach = emailCommunicationTemplateRepository.findAllByEmailCommIdAndStatus(
                emailCommunicationIdToResend,
                EntityStatus.ACTIVE
        );

        if (CollectionUtils.isNotEmpty(templatesToAttach)) {
            List<EmailCommunicationTemplates> attachedTemplates = templatesToAttach
                    .stream()
                    .map(it -> {
                             return EmailCommunicationTemplates
                                     .builder()
                                     .emailCommId(emailCommunicationIdToAttach)
                                     .templateId(it.getTemplateId())
                                     .status(it.getStatus())
                                     .build();
                         }
                    )
                    .collect(Collectors.toList());

            emailCommunicationTemplateRepository.saveAll(attachedTemplates);
        }

        List<EmailCommunicationAttachment> attachmentsToAttach = emailCommunicationAttachmentRepository.findAllActiveAttachmentsByEmailCommunicationId(
                emailCommunicationIdToResend
        );
        if (CollectionUtils.isNotEmpty(attachmentsToAttach)) {
            List<EmailCommunicationAttachment> attachedAttachments = attachmentsToAttach
                    .stream()
                    .map(it -> {
                             return EmailCommunicationAttachment
                                     .builder()
                                     .emailCommunicationId(emailCommunicationIdToAttach)
                                     .fileUrl(it.getFileUrl())
                                     .status(it.getStatus())
                                     .name(it.getName())
                                     .build();
                         }
                    )
                    .toList();

            emailCommunicationAttachmentRepository.saveAll(attachedAttachments);
        }
    }

    /**
     * Retrieves and prepares an {@link EmailCommunicationResponse} for the specified email communication ID.
     * This method performs the following steps:
     * Logs the attempt to view the email communication.
     * Fetches the {@link EmailCommunication} entity from the repository using the provided ID.
     * Checks the status of the email communication to ensure it is valid for viewing.
     * Maps the {@link EmailCommunication} entity to an {@link EmailCommunicationResponse}.
     * Enriches the response with additional information such as topic, email box, attachments, files, related customers, customer, and tasks.
     *
     * @param id          the ID of the email communication to view
     * @param channelType the type of the email communication channel to view
     * @return an {@link EmailCommunicationResponse} containing the details of the email communication
     * @throws DomainEntityNotFoundException if the email communication with the specified ID is not found
     */
    public EmailCommunicationResponse view(Long id, EmailCommunicationChannelType channelType) {
        log.info("View email communication wih ID: %s and channel Type: %s".formatted(id, channelType));
        EmailCommunication entity;
        EmailCommunicationCustomer emailCommunicationCustomerEntity = null;
        EmailCommunicationResponse response;

        if (EmailCommunicationChannelType.MASS_EMAIL.equals(channelType)) {
            entity = findEmailCommunicationByCustomerId(id);
            emailCommunicationCustomerEntity = findEmailCommunicationCustomerById(id);
        } else {
            entity = findEmailCommunicationById(id);
        }

        checkOnEmailCommunicationPermissionStatusView(entity.getEmailCommunicationStatus(), entity.getEntityStatus());

        if (EmailCommunicationChannelType.MASS_EMAIL.equals(channelType)) {
            response = emailCommunicationMapper.fromMassEntityToSinglePreviewResponse(entity);

            if (Objects.nonNull(emailCommunicationCustomerEntity)) {
                List<AttachmentShortResponse> shared = new ArrayList<>();
                List<AttachmentShortResponse> attachments = getEmailCommunicationCustomerAttachmentsShortResponse(emailCommunicationCustomerEntity.getId());
                List<AttachmentShortResponse> emailCommunicationAttachmentsShortResponse = getEmailCommunicationAttachmentsShortResponse(entity.getId());
                shared.addAll(emailCommunicationAttachmentsShortResponse);
                shared.addAll(attachments);

                response.setAttachmentsShortResponse(shared);
                response.setEmailBody(Objects.isNull(emailCommunicationCustomerEntity.getEmailBody()) ? entity.getEmailBody() : emailCommunicationCustomerEntity.getEmailBody());
            }
            response.setCustomerCommunicationDataShortResponse(getCustomerCommunicationDataShortResponseByEmailCommunicationCustomerId(id));
            response.setCustomerShortResponse(getCustomerShortResponseByEmailCommunicationCustomerId(id));
            response.setCustomerEmailAddress(getCustomerEmailAddressesByEmailCommunicationCustomerId(id));
        } else {
            response = emailCommunicationMapper.fromEntityToPreviewResponse(entity);

            response.setCustomerCommunicationDataShortResponse(getCustomerCommunicationDataShortResponseByEmailCommunicationId(id));
            response.setCustomerShortResponse(getCustomerShortResponseByEmailCommunicationId(id));
            response.setCustomerEmailAddress(getCustomerEmailAddressesByEmailCommunicationId(id));

            response.setActivityShortResponse(emailCommunicationActivityService.getActivitiesByConnectedObjectId(id));
            response.setFilesShortResponse(getEmailCommunicationFilesShortResponse(id));
            response.setIsResendActive(isResendActive(entity.getSentDate()) && EmailCommunicationType.OUTGOING.equals(entity.getEmailCommunicationType()));
            response.setTaskShortResponse(getTasks(id));
            response.setAttachmentsShortResponse(getEmailCommunicationAttachmentsShortResponse(entity.getId()));
        }

        ShortResponse topicOfCommunicationShortResponse = getTopicOfCommunicationShortResponse(entity.getCommunicationTopicId());

        if (Objects.nonNull(entity.getSenderEmployeeId())) {
            response.setSenderEmployeeShortResponse(Objects.equals(topicOfCommunicationShortResponse.name(), "Reminder for disconnection") ?
                                                            new ShortResponse(null, "System") : getSenderEmployeeShortResponse(entity.getSenderEmployeeId()));
        }
        response.setTopicOfCommunicationShortResponse(topicOfCommunicationShortResponse);
        response.setCreatorEmployeeShortResponse(getCreatorEmployeeShortResponse(entity.getSystemUserId()));
        response.setRelatedCustomersShortResponse(getRelatedCustomerShortResponse(entity.getId()));
        response.setEmailBoxShortResponse(getEmailBoxShortResponse(entity.getEmailBoxId()));
        response.setTemplateResponses(findTemplatesForContract(entity.getId()));
        contractTemplateRepository
                .findTemplateResponseById(entity.getEmailTemplateId(), LocalDate.now())
                .ifPresent(response::setEmailTemplateResponse);
        return response;
    }

    /**
     * Creates a mass email communication based on the provided request and performs various validations and actions related to it.
     * <p>
     * This method handles the creation of a mass email communication entity, performs validation on various aspects like topics,
     * email boxes, communication files, and related customers, and generates reports based on the validation results. It also
     * handles the association of customers with the email communication and ensures that at least one customer is associated
     * before finalizing the creation.
     * </p>
     *
     * @param request the details required to create a mass email communication. This includes information about the type of
     *                email creation, topics, email boxes, communication files, contact purposes, attachments, related customers,
     *                and customer details.
     * @return the ID of the newly created mass email communication.
     * @throws ClientException          if the mass email communication creation fails, if there are validation errors, or if report
     *                                  generation fails. Specific error messages and error codes are provided based on the context of the failure.
     * @throws IllegalArgumentException if the provided request is invalid or if the request does not meet the necessary
     *                                  criteria for creating a mass email communication.
     * @see MassEmailCreateRequest
     * @see EmailCommunication
     * @see EmailCommunicationStatus
     * @see EmailCreateType
     * @see EntityStatus
     * @see ClientException
     * @see EPBChainedExceptionTriggerUtil
     */
    public Long createMassEmail(MassEmailCreateRequest request) {
        List<String> errorMessages = new ArrayList<>();
        EmailCommunication emailCommunication = transactionTemplate.execute(
                status -> {
                    EmailCommunication emailCommunicationTransaction = emailCommunicationMapper.fromMassEmailCreateRequestToEntity(request);
                    validateAndSetTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, emailCommunicationTransaction, errorMessages);
                    validateAndSetSendingStatusByPermissionOnMassCreate(emailCommunicationTransaction, request.getCreateType());

                    validateAndSetTopicOfCommunication(emailCommunicationTransaction, request.getTopicOfCommunicationId(), errorMessages);
                    validateAndSetEmailBox(emailCommunicationTransaction, request.getEmailBoxId(), errorMessages);
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

                    emailCommunicationTransaction = emailCommunicationRepository.saveAndFlush(emailCommunicationTransaction);

                    saveTemplates(request.getTemplateIds(), emailCommunicationTransaction.getId(), errorMessages);
                    validateAndSetCommunicationFiles(emailCommunicationTransaction.getId(), request.getCommunicationFileIds(), errorMessages);
                    validateAndSetContactPurposes(emailCommunicationTransaction.getId(), request.getContactPurposeIds(), errorMessages);
                    validateAndSetAttachments(emailCommunicationTransaction.getId(), request.getAttachmentFileIds(), errorMessages);
                    validateAndSetRelatedCustomers(emailCommunicationTransaction, request.getRelatedCustomerIds(), errorMessages);

                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                    return emailCommunicationTransaction;
                }
        );

        if (Objects.isNull(emailCommunication)) {
            throw new ClientException("Mass email communication create failed!", APPLICATION_ERROR);
        }

        Set<MassCommunicationCustomerRequest> customers = selectCustomersByRequestType(request.isAllCustomersWithActiveContract(), request.getCustomers());

        Map<String, String> reportMap;

        if (EmailCreateType.SEND.equals(request.getCreateType()) && EmailCommunicationStatus.SENT.equals(emailCommunication.getEmailCommunicationStatus())) {
            reportMap = validateAndSetCustomersOnSend(request.getContactPurposeIds(), customers, emailCommunication);
        } else {
            reportMap = validateAndSetCustomers(customers, emailCommunication);
        }

        //Check if at least one user has created
        if (!emailCommunicationCustomerRepository.existsByEmailCommunicationId(emailCommunication.getId())) {
            emailCommunication.setEntityStatus(EntityStatus.DELETED);
            emailCommunicationHelper.saveAndFlush(emailCommunication);
            throw new ClientException(
                    MapUtils.isNotEmpty(reportMap) ? constructReportErrorMessage(reportMap) : "Customer creation failed!",
                    ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
            );
        }

        //Check if report map contains any errors
        if (MapUtils.isNotEmpty(reportMap)) {
            try {
                generateReport(reportMap, emailCommunication);
            } catch (Exception e) {
                log.debug("Report generation failed");
                throw new ClientException("Report generation failed!", APPLICATION_ERROR);
            }
        }

        return emailCommunication.getId();
    }

    /**
     * Updates an existing mass email communication based on the provided ID and update request.
     * This method handles the update of a mass email communication entity, performing validations and updates on various aspects
     * such as topics, email boxes, communication files, contact purposes, attachments, and related customers. It also manages
     * the association of customers with the email communication and generates reports based on the validation results.
     *
     * @param id      the ID of the mass email communication entity to be updated.
     * @param request contains the details required to update the mass email communication. This includes information about
     *                the type of email creation, topics, email boxes, communication files, contact purposes, attachments,
     *                and related customers.
     * @return the ID of the updated mass email communication.
     * @throws ClientException          if the update fails, if validation errors occur, or if report generation fails. Specific error
     *                                  messages and error codes are provided based on the context of the failure.
     * @throws IllegalArgumentException if the provided request is invalid or if it does not meet the necessary criteria
     *                                  for updating a mass email communication.
     * @see MassEmailEditRequest
     * @see EmailCommunication
     * @see EmailCommunicationStatus
     * @see EmailCreateType
     * @see EntityStatus
     * @see ClientException
     * @see EPBChainedExceptionTriggerUtil
     */
    @Transactional
    public Long updateMassEmail(Long id, MassEmailEditRequest request) {
        log.info("Update mass email communication: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        EmailCommunication emailCommunicationEntity = findEmailCommunicationById(id);
        validateMassEmailUpdateActionByStatuses(emailCommunicationEntity);
        validateAndSetSendingStatusByPermissionOnMassUpdate(emailCommunicationEntity, request.getCreateType());
        validateAndSetTemplate(request.getEmailTemplateId(), ContractTemplatePurposes.EMAIL, emailCommunicationEntity, errorMessages);
        updateTemplates(request.getTemplateIds(), emailCommunicationEntity.getId(), errorMessages);
        emailCommunicationMapper.fromMassEditRequestToEntity(emailCommunicationEntity, request);

        validateAndUpdateTopicOfCommunication(request.getTopicOfCommunicationId(), emailCommunicationEntity, errorMessages);
        validateAndUpdateRelatedCustomers(emailCommunicationEntity, request.getRelatedCustomerIds(), errorMessages);
        validateAndUpdateAndSetEmailBox(request.getEmailBoxId(), emailCommunicationEntity, errorMessages);
        validateAndUpdateCommunicationFiles(id, request.getCommunicationFileIds(), errorMessages);
        validateAndUpdateContactPurposes(id, request.getContactPurposeIds(), errorMessages);
        validateAndUpdateAttachments(id, request.getAttachmentFileIds(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        emailCommunicationRepository.saveAndFlush(emailCommunicationEntity);

        Set<MassCommunicationCustomerRequest> customers = selectCustomersByRequestType(request.isAllCustomersWithActiveContract(), request.getCustomers());

        Map<String, String> reportMap;
        if (EmailCreateType.SEND.equals(request.getCreateType()) && EmailCommunicationStatus.SENT.equals(emailCommunicationEntity.getEmailCommunicationStatus())) {
            reportMap = validateAndUpdateCustomerOnSend(request.getContactPurposeIds(), customers, emailCommunicationEntity);
        } else {
            reportMap = validateAndUpdateCustomers(customers, emailCommunicationEntity);
        }

        if (MapUtils.isNotEmpty(reportMap)) {
            try {
                generateReportOnUpdate(reportMap, emailCommunicationEntity);
            } catch (Exception e) {
                throw new ClientException("Report generation failed!", APPLICATION_ERROR);
            }
        }

        return emailCommunicationEntity.getId();
    }

    private Set<MassCommunicationCustomerRequest> selectCustomersByRequestType(
            Boolean isAllCustomersWithActiveContract,
            Set<MassCommunicationCustomerRequest> requestCustomers
    ) {
        return isAllCustomersWithActiveContract ? customerRepository
                .fetchActiveContractsAndAssociatedCustomersForMassCommunication()
                .stream()
                .map(MassCommunicationCustomerRequest::new)
                .collect(Collectors.toSet()) : requestCustomers;
    }

    /**
     * Deletes a mass email communication by setting its status to DELETED.
     * This method retrieves the EmailCommunication entity with the specified ID,
     * validates the delete action based on its status, sets the entity status to DELETED,
     * and saves the updated entity to the database.
     *
     * @param id The ID of the mass email communication to delete.
     * @return The ID of the deleted mass email communication.
     * @throws EntityNotFoundException If the mass email communication with the given ID is not found.
     * @throws AccessDeniedException   If the delete action is not allowed based on the current status of the email communication.
     */
    @Transactional
    public Long deleteMassEmail(Long id) {
        log.info("Deleting mass email communication with id: {}", id);
        EmailCommunication emailCommunication = findEmailCommunicationById(id);
        validateDeleteActionByStatuses(emailCommunication);

        emailCommunication.setEntityStatus(EntityStatus.DELETED);
        emailCommunicationRepository.saveAndFlush(emailCommunication);
        return id;
    }

    /**
     * Retrieves detailed information about a mass email communication based on its ID.
     * Constructs and returns a MassEmailCommunicationResponse object containing various attributes
     * of the email communication entity and related entities.
     *
     * @param id The ID of the mass email communication to view.
     * @return A MassEmailCommunicationResponse object containing detailed information about the mass email communication.
     * @throws EntityNotFoundException If the mass email communication with the given ID is not found.
     * @throws AccessDeniedException   If access to view the mass email communication is denied based on its status.
     */
    public MassEmailCommunicationResponse viewMassEmail(Long id) {
        EmailCommunication emailCommunication = findEmailCommunicationById(id);

        checkOnMassEmailCommunicationStatusView(emailCommunication.getEmailCommunicationStatus(), emailCommunication.getEntityStatus());
        List<ShortResponse> customersShortResponse = emailCommunicationCustomerRepository.getMassEmailCustomers(id);

        MassEmailCommunicationResponse response = new MassEmailCommunicationResponse();
        contractTemplateRepository.findTemplateResponseById(emailCommunication.getEmailTemplateId(), LocalDate.now())
                .ifPresent(response::setEmailTemplateResponse);
        response.setCommunicationAsInstitution(emailCommunication.getCommunicationAsAnInstitution());
        response.setEmailCommunicationStatus(emailCommunication.getEmailCommunicationStatus());
        response.setCommunicationChannelType(emailCommunication.getCommunicationChannel());
        response.setCommunicationType(emailCommunication.getEmailCommunicationType());
        response.setEmailSubject(emailCommunication.getEmailSubject());
        response.setEntityStatus(emailCommunication.getEntityStatus());
        response.setEmailBody(emailCommunication.getEmailBody());
        response.setSendDate(emailCommunication.getSentDate());
        response.setId(id);
        response.setTemplateResponses(findTemplatesForContract(response.getId()));

        response.setTopicOfCommunication(getTopicOfCommunicationShortResponse(emailCommunication.getCommunicationTopicId()));
        response.setCreatorEmployeeShortResponse(getCreatorEmployeeShortResponse(emailCommunication.getSystemUserId()));
        response.setActivityShortResponse(massEmailCommunicationActivityService.getActivitiesByConnectedObjectId(id));
        response.setEmailBoxShortResponse(getEmailBoxShortResponse(emailCommunication.getEmailBoxId()));
        response.setAttachmentFilesShortResponse(getEmailCommunicationAttachmentsShortResponse(id));
        response.setReportFileShortResponse(getMassEmailCommunicationReportFileShortResponse(id));
        response.setContactPurposesShortResponse(getRelatedContactPurposesShortResponse(id));
        response.setRelatedCustomersShortResponse(getRelatedCustomerShortResponse(id));
        response.setFilesShortResponse(getEmailCommunicationFilesShortResponse(id));
        response.setTaskShortResponse(getTasks(id));
        response.setCustomersShortResponse(customersShortResponse);

        if (Objects.nonNull(emailCommunication.getSenderEmployeeId())) {
            response.setSenderEmployeeShortResponse(getSenderEmployeeShortResponse(emailCommunication.getSenderEmployeeId()));
        }
        return response;
    }

    /**
     * Validates and updates contact purposes for a mass email communication identified by {@code id}.
     * This method validates and updates the contact purposes associated with a mass email communication
     * identified by {@code id} based on the provided {@code contactPurposeIds}. It compares the existing
     * contact purposes with the new ones to determine additions and deletions. It performs validation
     * checks and updates accordingly, appending any validation errors to the {@code errorMessages} list.
     *
     * @param id                The ID of the mass email communication entity.
     * @param contactPurposeIds The set of contact purpose IDs to update or associate with the mass email.
     * @param errorMessages     The list to append validation error messages.
     */
    private void validateAndUpdateContactPurposes(Long id, Set<Long> contactPurposeIds, List<String> errorMessages) {
        Map<Long, EmailCommunicationContactPurpose> existingMap = EPBListUtils.transformToMap(emailCommunicationContactPurposeRepository.findAllActiveContactPurposeByEmailCommunicationId(id), EmailCommunicationContactPurpose::getId);
        if (CollectionUtils.isNotEmpty(contactPurposeIds)) {
            Set<Long> added = contactPurposeIds
                    .stream()
                    .filter(current -> !existingMap.containsKey(current))
                    .collect(Collectors.toSet());

            validateAndSetContactPurposes(id, added, errorMessages);
            validateAndDeleteContactPurposes(existingMap, contactPurposeIds);
        } else {
            validateAndDeleteContactPurposes(existingMap, SetUtils.emptySet());
        }
    }

    /**
     * Validates contact purposes and processes customers concurrently for a mass email communication.
     * This method validates contact purposes and processes customers concurrently for a mass email communication.
     * It divides the customers into batches based on a calculated batch size and uses a thread pool to execute
     * processing tasks in parallel. Each batch of customers is processed using the {@code processCustomers} method,
     * which updates a {@code report} concurrently with any validation or processing errors encountered.
     *
     * @param customers          The set of customer requests to validate and process.
     * @param emailCommunication The email communication entity associated with the customers.
     * @return A concurrent map containing customer identifiers as keys and error messages as values, if any errors occurred during processing.
     * The map is populated concurrently as processing tasks complete.
     */
    @SneakyThrows
    public Map<String, String> validateAndSetCustomers(
            Set<MassCommunicationCustomerRequest> customers,
            EmailCommunication emailCommunication
    ) {
        ConcurrentHashMap<String, String> report = new ConcurrentHashMap<>();
        EPBBatchUtils.processItemsInBatches(List.copyOf(customers), batch -> processCustomers(batch, report, emailCommunication.getId()));
        return report;
    }

    /**
     * Validates the customer details and processes them for sending email communications in batches.
     * This method processes the given set of customers, validates their communication details,
     * and then processes the batch for sending emails. The processing is done in batches using
     * the utility method.
     * After processing, a report containing any error messages for the customers is returned.
     *
     * @param contactPurposes    A set of contact purpose IDs that are used to filter customer communication data.
     * @param customers          A set of {@link MassCommunicationCustomerRequest} objects, each representing a customer
     *                           for whom the email communication is being processed.
     * @param emailCommunication The {@link EmailCommunication} object representing the email communication
     *                           to be sent to the customers.
     * @return A map containing error messages for customers. The key is the customer identifier, and the value
     * is a string containing the error messages for that customer, if any.
     */
    @SneakyThrows
    public Map<String, String> validateAndSetCustomersOnSend(
            Set<Long> contactPurposes,
            Set<MassCommunicationCustomerRequest> customers,
            EmailCommunication emailCommunication
    ) {
        ConcurrentHashMap<String, String> report = new ConcurrentHashMap<>();
        EPBBatchUtils.processItemsInBatches(List.copyOf(customers), batch -> processCustomersAndSend(contactPurposes, batch, report, emailCommunication));
        return report;
    }

    /**
     * Validates and updates the customers associated with a given mass email communication.
     * <p>
     * This method first removes any existing customer associations for the specified mass email communication entity.
     * Then it validates and sets the new customer associations based on the provided set of customer requests.
     * </p>
     *
     * @param customers          a set of {@link MassCommunicationCustomerRequest} objects representing the new customer associations to be
     *                           validated and updated for the mass email communication.
     * @param emailCommunication the {@link EmailCommunication} entity for which the customer associations are being updated.
     * @return a map where each key is a customer identifier and each value is an error message associated with that customer,
     * if any validation errors occur. An empty map indicates that the validation passed successfully.
     * @see MassCommunicationCustomerRequest
     * @see EmailCommunication
     */
    public Map<String, String> validateAndUpdateCustomers(
            Set<MassCommunicationCustomerRequest> customers,
            EmailCommunication emailCommunication
    ) {
        deleteOldCustomersByEmailCommunicationId(emailCommunication.getId());
        return validateAndSetCustomers(customers, emailCommunication);
    }

    /**
     * Validates and updates the customers associated with a given mass email communication specifically for sending scenarios.
     * <p>
     * This method first removes any existing customer associations for the specified mass email communication entity.
     * Then it validates and sets new customer associations based on the provided customer requests and contact purposes,
     * tailored for scenarios where the mass email communication is being prepared for sending.
     * </p>
     *
     * @param contactPurposes    a set of {@link Long} identifiers representing the contact purposes that are relevant to the mass
     *                           email communication. These identifiers are used to validate the customer associations.
     * @param customers          a set of {@link MassCommunicationCustomerRequest} objects representing the new customer associations to be
     *                           validated and updated for the mass email communication.
     * @param emailCommunication the {@link EmailCommunication} entity for which the customer associations are being updated.
     * @return a map where each key is a customer identifier and each value is an error message associated with that customer,
     * if any validation errors occur. An empty map indicates that the validation passed successfully.
     * @see MassCommunicationCustomerRequest
     * @see EmailCommunication
     */
    public Map<String, String> validateAndUpdateCustomerOnSend(
            Set<Long> contactPurposes,
            Set<MassCommunicationCustomerRequest> customers,
            EmailCommunication emailCommunication
    ) {
        deleteOldCustomersByEmailCommunicationId(emailCommunication.getId());
        return validateAndSetCustomersOnSend(contactPurposes, customers, emailCommunication);
    }

    /**
     * Generates a report based on the provided report map and associates it with the given EmailCommunication.
     * This method performs the following operations:
     * 1. Creates an Excel Workbook using the provided reportMap data.
     * 2. Writes the workbook content to a ByteArrayOutputStream.
     * 3. Generates a unique file name for the report.
     * 4. Creates a MultipartFile containing the report data.
     * 5. Uploads the report file to a file service with a specific directory structure based on the current date.
     * 6. Creates an EmailCommunicationFile entity representing the uploaded file and saves it.
     *
     * @param reportMap          A map containing data to populate the report.
     * @param emailCommunication The EmailCommunication entity to associate with the generated report.
     * @throws IOException If an IO error occurs during workbook creation or file handling.
     */
    private void generateReport(
            Map<String, String> reportMap,
            EmailCommunication emailCommunication
    ) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        createReportWorkBook(reportMap, byteArrayOutputStream);

        String fileName = getFileName(UUID.randomUUID().toString());
        MultipartFile multipartFile = new ByteMultiPartFile(fileName, byteArrayOutputStream.toByteArray());
        String fileUrl = fileService.uploadFile(multipartFile, remotePath(), fileName);

        byteArrayOutputStream.close();

        EmailCommunicationFile emailCommunicationFile = emailCommunicationMapper.emailCommunicationFile(fileUrl, fileName, true, null);
        emailCommunicationFile.setEmailCommunicationId(emailCommunication.getId());
        emailCommunicationFileRepository.save(emailCommunicationFile);
    }

    /**
     * Updates an existing Excel workbook with new data from the provided report map.
     * This method opens an existing Excel workbook from the provided {@link ByteArrayResource}, adds new rows to the specified
     * sheet by appending data from the {@code reportMap}, and writes the updated workbook to the provided {@code OutputStream}.
     * Each entry in the report map is added as a new row in the sheet, with the key as the first cell and the value as the second cell.
     *
     * @param reportMap a map where each key is an identifier and each value is the corresponding data to be added to the report.
     * @param {@link    ByteArrayResource} containing the existing Excel workbook to be updated.
     * @param {@link    OutputStream} where the updated workbook will be written. This stream must be closed by
     *                  the caller after the method completes.
     * @throws IOException if an I/O error occurs while reading the existing workbook, updating it, or writing the updated
     *                     workbook to the output stream.
     * @see Workbook
     * @see Sheet
     * @see Row
     * @see Cell
     * @see WorkbookFactory
     */
    private void generateReportOnUpdate(
            Map<String, String> reportMap,
            EmailCommunication emailCommunication
    ) throws IOException {
        Optional<EmailCommunicationFile> emailCommunicationFileOptional = emailCommunicationFileRepository.findReportFileByEmailCommunicationId(
                emailCommunication.getId(),
                EntityStatus.ACTIVE
        );

        if (emailCommunicationFileOptional.isPresent()) {
            EmailCommunicationFile emailCommunicationFile = emailCommunicationFileOptional.get();

            ByteArrayResource byteArrayResource = fileService.downloadFile(emailCommunicationFile.getLocalFileUrl());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            updateReportWorkBook(reportMap, byteArrayResource, byteArrayOutputStream);

            MultipartFile multipartFile = new ByteMultiPartFile(emailCommunicationFile.getName(), byteArrayOutputStream.toByteArray());
            String fileUrl = fileService.uploadFile(multipartFile, remotePath(), emailCommunicationFile.getName());

            byteArrayOutputStream.close();

            emailCommunicationFile.setLocalFileUrl(fileUrl);
            emailCommunicationFileRepository.save(emailCommunicationFile);
        } else {
            generateReport(reportMap, emailCommunication);
        }
    }

    /**
     * Creates an Excel workbook populated with data from the provided report map.
     * This method generates a workbook with a single sheet titled "Customer Data".
     * It adds headers for "customer_identifier" and "errorMessages" in the first row.
     * Each entry in the report map is written to subsequent rows, with identifiers in column 0
     * and corresponding error messages in column 1.
     * Column widths are automatically adjusted to fit content.
     *
     * @param reportMap A map containing data to populate the Excel workbook.
     * @throws IOException If an IO error occurs during workbook creation or manipulation.
     */
    private void createReportWorkBook(Map<String, String> reportMap, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET);
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
            workbook.write(outputStream);
        }
    }

    /**
     * Updates an existing Excel workbook with new data from the provided report map.
     * This method reads an existing Excel workbook from the provided {@link ByteArrayResource}, appends new rows of data to the
     * specified sheet based on the entries in the {@code reportMap}, and writes the updated workbook to the provided
     * {@code OutputStream}. Each entry in the report map is added as a new row, with the map key in the first cell and the map
     * value in the second cell.
     *
     * @param reportMap         a map where each key is an identifier (e.g., customer ID) and each value is the corresponding data (e.g., error message)
     *                          to be added to the report. Each entry is appended as a new row in the Excel sheet.
     * @param byteArrayResource a {@link ByteArrayResource} containing the existing Excel workbook to be updated. The workbook is read
     *                          from this resource.
     * @param outputStream      an {@link OutputStream} where the updated workbook will be written. The caller is responsible for closing this stream
     *                          after the method completes.
     * @throws IOException if an I/O error occurs while reading the existing workbook, updating it with new data, or writing the updated
     *                     workbook to the output stream.
     * @see Workbook
     * @see Sheet
     * @see Row
     * @see Cell
     * @see WorkbookFactory
     */
    private void updateReportWorkBook(
            Map<String, String> reportMap,
            ByteArrayResource byteArrayResource,
            OutputStream outputStream
    ) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(byteArrayResource.getInputStream())) {
            Sheet sheet = workbook.getSheet(SHEET);

            int lastRowNum = sheet.getLastRowNum();
            int newRowNum = lastRowNum + 1;

            for (String identifier : reportMap.keySet()) {
                Row row = sheet.createRow(newRowNum++);
                row.createCell(0).setCellValue(identifier);
                row.createCell(1).setCellValue(reportMap.get(identifier));
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            workbook.write(outputStream);
        }
    }

    /**
     * Processes a list of customer requests for a mass email communication.
     * This method iterates through the provided {@code customers} list and attempts to retrieve
     * customer details for each request. If customer details are found, it saves the email communication
     * customer association. If customer details are not found, it updates the {@code report} map with
     * an error message indicating that customer details were not found.
     *
     * @param customers            The list of customer requests to process.
     * @param report               A map where error messages are collected for customers whose details were not found.
     *                             Key is the customer identifier and value is the error message.
     * @param emailCommunicationId The ID of the email communication associated with the customers.
     */
    public void processCustomers(
            List<MassCommunicationCustomerRequest> customers,
            Map<String, String> report,
            Long emailCommunicationId
    ) {
        for (MassCommunicationCustomerRequest customerRequest : customers) {
            String identifier = customerRequest.getCustomerIdentifier();
            Long version = customerRequest.getVersion();

            Optional<CustomerDetails> customerDetails = fetchCustomerDetailsByIdentifierOrVersionId(identifier, version);

            if (customerDetails.isPresent()) {
                saveEmailCommunicationCustomer(emailCommunicationId, customerDetails.get().getId(), null);
            } else {
                report.put(identifier, " Customer details not found!");
            }
        }
    }

    /**
     * Fetches customer details based on the customer identifier and optionally the version ID.
     * Retrieves customer details from the repository based on the provided {@code customerIdentifier}.
     * If {@code versionId} is provided, it retrieves details for that specific version. Otherwise, it
     * retrieves the most recent customer details.
     *
     * @param customerIdentifier The identifier of the customer whose details are to be fetched.
     * @param versionId          The version ID of the customer details to fetch, or {@code null} to fetch the latest details.
     * @return An {@code Optional} containing the {@code CustomerDetails} if found, or empty if not found.
     */
    private Optional<CustomerDetails> fetchCustomerDetailsByIdentifierOrVersionId(
            String customerIdentifier,
            Long versionId
    ) {
        if (Objects.nonNull(versionId)) {
            return customerDetailsRepository.findByCustomerIdentifierAndVersionId(customerIdentifier, versionId);
        } else {
            return customerDetailsRepository.findLastCustomerDetail(customerIdentifier);
        }
    }

    /**
     * Saves an EmailCommunicationCustomer entity with the specified IDs.
     * Creates an EmailCommunicationCustomer entity using the provided IDs for email communication,
     * customer details, and customer communication (if available). Saves the entity to the repository
     * and flushes changes to the database.
     *
     * @param emailCommunicationId The ID of the email communication associated with the customer.
     * @param customerDetailsId    The ID of the customer details associated with the email communication.
     * @param customerCommId       The ID of the customer communication associated with the email communication,
     *                             or {@code null} if not applicable.
     * @return The saved EmailCommunicationCustomer entity.
     */
    private EmailCommunicationCustomer saveEmailCommunicationCustomer(
            Long emailCommunicationId,
            Long customerDetailsId,
            Long customerCommId
    ) {
        EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationMapper.emailCommunicationCustomer(
                emailCommunicationId,
                customerDetailsId,
                customerCommId
        );
        emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);
        return emailCommunicationCustomer;
    }

    /**
     * Saves an EmailCommunicationCustomer entity with the specified IDs.
     * Creates an EmailCommunicationCustomer entity using the provided IDs for email communication,
     * customer details, and customer communication (if available). Saves the entity to the repository
     * and flushes changes to the database.
     *
     * @param emailCommunicationId The ID of the email communication associated with the customer.
     * @param customerDetailsId    The ID of the customer details associated with the email communication.
     * @param customerCommId       The ID of the customer communication associated with the email communication,
     *                             or {@code null} if not applicable.
     * @param contactPurposeId     The ID of the contact purpose associated with the email communication,
     *                             or {@code null} if not applicable.
     * @return The saved EmailCommunicationCustomer entity.
     */
    private EmailCommunicationCustomer saveEmailCommunicationCustomer(
            Long emailCommunicationId,
            Long customerDetailsId,
            Long customerCommId,
            Long contactPurposeId
    ) {
        EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationMapper.emailCommunicationCustomer(
                emailCommunicationId,
                customerDetailsId,
                customerCommId,
                contactPurposeId
        );
        emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);
        return emailCommunicationCustomer;
    }

    /**
     * Processes a batch of customer email communications and sends them within a transactional context.
     * This method wraps the email communication processing logic within a transaction using the
     * {@link TransactionTemplate}. It processes the customer emails in batches and reports any
     * errors encountered during the process. The method executes the
     * {@link #processCustomersEmailsCreationBatchInTransaction(Collection, List, Map, EmailCommunication)} method
     * to handle the batch processing of customers and their email communications.
     *
     * @param contactPurposes    A collection of contact purpose IDs used to filter customer communication data.
     * @param customers          A list of {@link MassCommunicationCustomerRequest} objects representing customers for whom
     *                           email communications are being processed.
     * @param report             A map to collect error messages. The key is the customer identifier, and the value is a string
     *                           containing the error messages encountered for that customer.
     * @param emailCommunication The {@link EmailCommunication} object representing the email communication to be processed.
     */
    private void processCustomersAndSend(
            Collection<Long> contactPurposes,
            List<MassCommunicationCustomerRequest> customers,
            Map<String, String> report,
            EmailCommunication emailCommunication
    ) {
        transactionTemplate.executeWithoutResult(
                transactionStatus -> processCustomersEmailsCreationBatchInTransaction(
                        contactPurposes,
                        customers,
                        report,
                        emailCommunication
                )
        );
    }

    /**
     * Checks whether a customer is attached to a specific contract based on the contract type.
     * This method verifies whether the given customer is associated with a contract detail based on
     * the provided contract type (either PRODUCT_CONTRACT or SERVICE_CONTRACT). It queries the
     * respective repositories to check the attachment of the customer to the product or service contract.
     *
     * @param contractType            The type of contract to check (either {@link ContractType#PRODUCT_CONTRACT} or {@link ContractType#SERVICE_CONTRACT}).
     * @param customerId              The ID of the customer to check for contract attachment.
     * @param productContractDetailId The ID of the contract detail to check for attachment to the customer.
     *                                This is used for both product and service contracts.
     * @return {@code true} if the customer is attached to the contract detail, {@code false} otherwise.
     */
    private boolean isCustomerAttachedToContract(
            ContractType contractType,
            Long customerId,
            Long productContractDetailId
    ) {
        if (Objects.requireNonNull(contractType) == ContractType.PRODUCT_CONTRACT) {
            return productContractDetailsRepository.isCustomerAttachedToContractDetail(customerId, productContractDetailId);
        } else if (contractType == ContractType.SERVICE_CONTRACT) {
            Long count = serviceContractsRepository.checkContractAttachedToCustomer(customerId, productContractDetailId);
            return count != null && count > 0;
        }
        return false;
    }

    /**
     * Processes a batch of customer email communications in a transactional manner. For each customer request,
     * this method checks if the customer details exist, retrieves associated customer communication data,
     * validates the data, and saves it along with any related contract details (either service or product contract).
     * <p>
     * The method handles various contract types and ensures that the customer is attached to the relevant contract
     * before saving the communication and contract data. If any errors occur during the process, they are collected
     * and stored in the provided report map with the customer identifier as the key.
     *
     * @param contactPurposes    A collection of contact purpose IDs that are used to filter customer communication data.
     * @param customers          A list of {@link MassCommunicationCustomerRequest} objects, each representing a customer for whom
     *                           email communication is to be processed.
     * @param report             A map to collect error messages, where the key is the customer identifier, and the value is a string
     *                           containing the error messages encountered for that customer.
     * @param emailCommunication The {@link EmailCommunication} object representing the email communication to be processed.
     */
    private void processCustomersEmailsCreationBatchInTransaction(
            Collection<Long> contactPurposes,
            List<MassCommunicationCustomerRequest> customers,
            Map<String, String> report,
            EmailCommunication emailCommunication
    ) {
        for (MassCommunicationCustomerRequest customerRequest : customers) {
            StringBuilder errorMessages = new StringBuilder();

            String identifier = customerRequest.getCustomerIdentifier();
            Long version = customerRequest.getVersion();
            Optional<CustomerDetails> customerDetailsOptional = fetchCustomerDetailsByIdentifierOrVersionId(identifier, version);

            if (customerDetailsOptional.isPresent()) {
                CustomerDetails customerDetails = customerDetailsOptional.get();
                Long customerId = customerDetails.getCustomerId();

                List<CustomerEmailCommDataMiddleResponse> customerCommunicationData = customerCommunicationsRepository
                        .findCustomerCommunicationsAndContactsByCustomerDetailIdAndPurpose(customerDetails.getId(), new HashSet<>(contactPurposes));

                if (customerCommunicationData.isEmpty()) {
                    errorMessages.append(String.format("Customer communication data not found for purpose ids %s;", contactPurposes));
                } else {
                    Set<Long> customerPurposeIds = EPBListUtils.transform(
                            customerCommunicationData,
                            CustomerEmailCommDataMiddleResponse::getContactPurposeId,
                            Collectors.toSet()
                    );

                    for (Long current : contactPurposes) {
                        if (!customerPurposeIds.contains(current)) {
                            errorMessages.append(String.format("Customer communication data not found for purpose id %s;", current));
                        }
                    }

                    Map<CustomerEmailCommunicationPurposeKeyPair, Map<String, Long>> groupedDistinctData = customerCommunicationData
                            .stream()
                            .collect(Collectors.groupingBy(
                                             CustomerEmailCommunicationPurposeKeyPair::new,
                                             Collectors.toMap(
                                                     CustomerEmailCommDataMiddleResponse::getContactValue,
                                                     CustomerEmailCommDataMiddleResponse::getContactId,
                                                     (existingId, newId) -> existingId // Keep the first contactId if contactValue is duplicate
                                             )
                                     )
                            );

                    if (CollectionUtils.isNotEmpty(customerRequest.getProductContractDetailIds())) {
                        List<Long> productContractDetailIds = customerRequest.getProductContractDetailIds();
                        for (Long current : productContractDetailIds) {
                            saveCollectedCustomerCommunicationAndContractData(
                                    groupedDistinctData,
                                    emailCommunication.getId(),
                                    customerDetails.getId(),
                                    null,
                                    current
                            );
                        }
                    } else if
                    (CollectionUtils.isNotEmpty(customerRequest.getServiceContractDetailIds())) {
                        List<Long> serviceContractIds = customerRequest.getServiceContractDetailIds();
                        for (Long current : serviceContractIds) {
                            saveCollectedCustomerCommunicationAndContractData(
                                    groupedDistinctData,
                                    emailCommunication.getId(),
                                    customerDetails.getId(),
                                    current,
                                    null
                            );
                        }
                    } else if
                    (Objects.nonNull(customerRequest.getProductContractDetailId()) && customerRequest.getProductContractDetailId() > 0) {
                        Long productContractDetailId = customerRequest.getProductContractDetailId();
                        if (isCustomerAttachedToContract(ContractType.PRODUCT_CONTRACT, customerId, productContractDetailId)) {
                            saveCollectedCustomerCommunicationAndContractData(
                                    groupedDistinctData,
                                    emailCommunication.getId(),
                                    customerDetails.getId(),
                                    null,
                                    productContractDetailId
                            );
                        } else {
                            errorMessages.append(
                                    String.format("Product contract with id %s is not attached to customer with id %s", productContractDetailId, customerId)
                            );
                        }
                    } else if
                    (Objects.nonNull(customerRequest.getServiceContractDetailId()) && customerRequest.getServiceContractDetailId() > 0) {
                        Long serviceContractDetailId = customerRequest.getServiceContractDetailId();
                        if (isCustomerAttachedToContract(ContractType.SERVICE_CONTRACT, customerId, serviceContractDetailId)) {
                            saveCollectedCustomerCommunicationAndContractData(
                                    groupedDistinctData,
                                    emailCommunication.getId(),
                                    customerDetails.getId(),
                                    serviceContractDetailId,
                                    null
                            );
                        } else {
                            errorMessages.append(
                                    String.format("Service contract with id %s is not attached to customer with id %s", serviceContractDetailId, customerId)
                            );
                        }
                    } else {
                        saveCollectedCustomerCommunicationAndContractData(
                                groupedDistinctData,
                                emailCommunication.getId(),
                                customerDetails.getId(),
                                null,
                                null
                        );
                    }
                }
            } else {
                errorMessages.append("Customer details not found!;");
            }

            if (!errorMessages.isEmpty()) {
                report.put(customerRequest.getCustomerIdentifier(), errorMessages.toString());
            }
        }
    }

    /**
     * Saves the customer communication and contract data for a given email communication.
     * This method processes the provided map of communication purposes and contacts,
     * creates and saves corresponding `EmailCommunicationCustomer` entities, and associates them
     * with the given email communication, customer details, service contract, and product contract.
     * <p>
     * For each communication purpose and its associated contacts, this method creates an
     * `EmailCommunicationCustomer` object, sets relevant contract details, and saves it to
     * the repository. It then iterates through the contact information and saves the contact
     * associations for each communication customer.
     *
     * @param communicationPurposeKeyPairMap A map where each key is a {@link CustomerEmailCommunicationPurposeKeyPair}
     *                                       containing communication and contact purpose IDs, and each value is another map
     *                                       where the key is a contact value and the value is a contact ID.
     * @param emailCommunicationId           The ID of the email communication being saved.
     * @param customerDetailsId              The ID of the customer associated with the email communication.
     * @param serviceContractDetailsId       The ID of the service contract details to be associated with the communication.
     * @param productContractDetailsId       The ID of the product contract details to be associated with the communication.
     */
    private void saveCollectedCustomerCommunicationAndContractData(
            Map<CustomerEmailCommunicationPurposeKeyPair, Map<String, Long>> communicationPurposeKeyPairMap,
            Long emailCommunicationId,
            Long customerDetailsId,
            Long serviceContractDetailsId,
            Long productContractDetailsId
    ) {
        communicationPurposeKeyPairMap.forEach((communicationPurposeKeyPair, contactKeyPair) -> {

                                                   EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationMapper.emailCommunicationCustomer(
                                                           emailCommunicationId,
                                                           customerDetailsId,
                                                           communicationPurposeKeyPair.getCommunicationId(),
                                                           communicationPurposeKeyPair.getContactPurposeId()
                                                   );

                                                   emailCommunicationCustomer.setServiceContractDetailId(serviceContractDetailsId);
                                                   emailCommunicationCustomer.setProductContractDetailId(productContractDetailsId);
                                                   emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.IN_PROGRESS);

                                                   emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);

                                                   contactKeyPair.forEach((contactValue, contactId) ->
                                                                                  saveEmailCommunicationCustomerContact(
                                                                                          contactId,
                                                                                          emailCommunicationCustomer.getId(),
                                                                                          contactValue
                                                                                  )
                                                   );
                                               }
        );
    }

    /**
     * Constructs a detailed error message based on the provided report map, which contains customer-specific error information.
     * This method iterates over the entries in the report map, which associates customer identifiers with error messages. It
     * formats and concatenates these entries into a single, readable string that includes each error message along with the
     * corresponding customer identifier.
     *
     * @param reportMap a map where each entry associates a customer identifier (key) with an error message (value).
     *                  This map is used to generate a comprehensive error report that details what went wrong for each customer.
     * @return a string that contains a formatted error message for each customer, including the customer identifier and
     * corresponding error message. The resulting string will be formatted such that each entry is on a new line.
     */
    private String constructReportErrorMessage(Map<String, String> reportMap) {
        StringBuilder messages = new StringBuilder();

        for (Map.Entry<String, String> entry : reportMap.entrySet()) {
            String customer = entry.getKey();
            String message = entry.getValue();

            messages.append(String.format("%s, For customer: %s%n", message, customer));
        }

        return messages.toString().trim();
    }

    /**
     * Fetches and processes the attachments associated with a specific email communication.
     * This method retrieves all attachments linked to the given email communication ID from the repository,
     * downloads each attachment file using the file service, and then converts the downloaded file into
     * {@link Attachment} objects. The resulting list of attachments is returned.
     *
     * @param emailCommunicationId the unique identifier of the email communication for which attachments are to be fetched.
     * @return a list of {@link Attachment} objects representing the attachments associated with the specified email communication.
     * The list may be empty if no attachments are found for the given email communication ID.
     * @see EmailCommunicationAttachmentRepository
     * @see FileService
     * @see Attachment
     */
    private List<Attachment> fetchAttachmentsForEmail(Long emailCommunicationId) {
        return emailCommunicationAttachmentRepository
                .findAllByEmailCommunicationIdAndStatus(emailCommunicationId)
                .stream()
                .map(fileService::downloadFile)
                .map(this::createAttachmentForEmail)
                .toList();
    }

    /**
     * Creates an {@link Attachment} object from a given {@link ByteArrayResource}.
     * This method takes a {@code ByteArrayResource} containing the file data, extracts the file name,
     * determines the content type based on the file name, and constructs an {@code Attachment} object with the
     * file's name, content type, size, and byte data.
     *
     * @param byteArrayResource a {@link ByteArrayResource} containing the file data. The resource must include
     *                          the file's byte data and file name.
     * @return an {@link Attachment} object representing the file with its name, content type, size, and data.
     * @see ByteArrayResource
     * @see Attachment
     * @see URLConnection#guessContentTypeFromName(String)
     */
    private Attachment createAttachmentForEmail(ByteArrayResource byteArrayResource) {
        String fileName = byteArrayResource.getFilename();
        return new Attachment(
                fileName,
                URLConnection.guessContentTypeFromName(fileName),
                byteArrayResource.getByteArray().length,
                byteArrayResource.getByteArray()
        );
    }

    /**
     * Validates and sets the email box for a given email communication.
     * This method checks if the provided email box ID is non-null and active.
     * If the email box is found and active, it sets the email box ID in the given email communication object.
     * If the email box is not found or is inactive, an error message is added to the {@code errorMessages} list.
     *
     * @param emailCommunication the email communication object to be associated with the email box
     * @param emailBoxId         the ID of the email box to be validated and set
     * @param errorMessages      a list to hold any error messages encountered during the process
     * @throws IllegalArgumentException if the provided emailCommunication is null
     * @throws IllegalStateException    if repository operations fail
     */
    private void validateAndSetEmailBox(
            EmailCommunication emailCommunication,
            Long emailBoxId,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(emailBoxId)) {
            Optional<EmailMailboxes> emailMailboxOptional = emailMailboxesRepository.findByIdAndStatuses(emailBoxId, List.of(NomenclatureItemStatus.ACTIVE));
            if (emailMailboxOptional.isPresent()) {
                emailCommunication.setEmailBoxId(emailMailboxOptional.get().getId());
            } else {
                errorMessages.add("emailBoxId-[emailBoxId] active email box with: %s can't be found;".formatted(emailBoxId));
            }
        }
    }

    /**
     * Validates the provided email box ID and sets it on the given email communication object.
     * If the email box ID is not null, it checks for the existence of the email box with the specified ID
     * in either the ACTIVE or INACTIVE status. If found, the email box ID is set on the email communication.
     * If not found, an error message is added to the provided list of error messages.
     *
     * @param emailCommunication the EmailCommunication object to update with the email box ID
     * @param emailBoxId         the ID of the email box to validate and set
     * @param errorMessages      a list to which error messages will be added if the email box cannot be found
     */
    private void validateAndSetEmailBoxOnResend(
            EmailCommunication emailCommunication,
            Long emailBoxId,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(emailBoxId)) {
            Optional<EmailMailboxes> emailMailboxOptional = emailMailboxesRepository.findByIdAndStatuses(emailBoxId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (emailMailboxOptional.isPresent()) {
                emailCommunication.setEmailBoxId(emailMailboxOptional.get().getId());
            } else {
                errorMessages.add("emailBoxId-[emailBoxId] active email box with: %s can't be found;".formatted(emailBoxId));
            }
        }
    }

    /**
     * Validates and sets the topic of communication for a given email communication.
     * This method checks if the provided topic of communication ID is non-null and active.
     * If the topic is found and active, it sets the communication topic ID in the given email communication object.
     * If the topic is not found or is inactive, an error message is added to the {@code errorMessages} list.
     *
     * @param emailCommunication     the email communication object to be associated with the topic of communication
     * @param topicOfCommunicationId the ID of the topic of communication to be validated and set
     * @param errorMessages          a list to hold any error messages encountered during the process
     * @throws IllegalArgumentException if the provided emailCommunication is null
     * @throws IllegalStateException    if repository operations fail
     */
    private void validateAndSetTopicOfCommunication(
            EmailCommunication emailCommunication,
            Long topicOfCommunicationId,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(topicOfCommunicationId)) {
            Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByIdAndStatus(topicOfCommunicationId, NomenclatureItemStatus.ACTIVE);
            if (topicOfCommunicationOptional.isPresent()) {
                emailCommunication.setCommunicationTopicId(topicOfCommunicationOptional.get().getId());
            } else {
                errorMessages.add("communicationTopicId-[communicationTopicId] active topic of communication with: %s can't be found;".formatted(topicOfCommunicationId));
            }
        }
    }

    /**
     * Validates and sets the topic of communication on an email communication object during the resend process.
     * This method checks if the provided topicOfCommunicationId is not null. If it is not null, it attempts to
     * find a corresponding {@link TopicOfCommunication} that is either ACTIVE or INACTIVE. If found, the
     * communication topic ID of the given {@link EmailCommunication} is set. If not found, an error message is
     * added to the provided list of error messages.</p>
     *
     * @param emailCommunication     The email communication object to update with the topic of communication ID.
     * @param topicOfCommunicationId The ID of the topic of communication to validate and set.
     * @param errorMessages          A list to which error messages will be added if the topic of communication cannot be found.
     */
    private void validateAndSetTopicOfCommunicationOnResend(
            EmailCommunication emailCommunication,
            Long topicOfCommunicationId,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(topicOfCommunicationId)) {
            Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByIdAndStatusIn(topicOfCommunicationId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (topicOfCommunicationOptional.isPresent()) {
                emailCommunication.setCommunicationTopicId(topicOfCommunicationOptional.get().getId());
            } else {
                errorMessages.add("communicationTopicId-[communicationTopicId] active topic of communication with: %s can't be found;".formatted(topicOfCommunicationId));
            }
        }
    }

    /**
     * Validates and sets the customer data for a given email communication.
     * This method validates the existence and status of customer-related entities based on the provided request.
     * It ensures the customer, customer details, and customer communication are active and associates them with the given email communication.
     * If all entities are valid, it creates and saves an {@code EmailCommunicationCustomer} and its corresponding contacts.
     * If any entity is not found or inactive, an error message is added to the {@code errorMessages} list.
     *
     * @param emailCommunication      the email communication object to be associated with the customer data
     * @param customerDetailId        the ID of the customer details
     * @param customerCommunicationId the ID of the customer communication
     * @param customerEmailAddress    the email address of the customer
     * @param errorMessages           the list to store error messages encountered during the validation process
     * @throws IllegalArgumentException if any of the provided parameters are null
     * @throws IllegalStateException    if repository operations fail
     */
    public void validateAndSetCustomerData(
            EmailCommunication emailCommunication,
            Long customerDetailId,
            Long customerCommunicationId,
            String customerEmailAddress,
            List<String> errorMessages
    ) {
        log.info("Validating and setting customer data for email communication ID: {}.", emailCommunication.getId());

        CustomerDetails customerDetail = getActiveCustomerDetailsOrElseNull(customerDetailId);
        if (customerDetail != null) {
            log.info("Found active customer details for customerDetailId: {}", customerDetailId);

            CustomerCommunications customerCommunication = getActiveCustomerCommunicationOrElseNull(customerCommunicationId, customerDetailId);
            if (customerCommunication != null) {
                log.info("Found active customer communication for customerCommunicationId: {}", customerCommunicationId);

                EmailCommunicationCustomer emailCommunicationCustomer = saveEmailCommunicationCustomer(
                        emailCommunication.getId(),
                        customerDetail.getId(),
                        customerCommunication.getId()
                );
                log.info("Email communication customer saved with ID: {}", emailCommunicationCustomer.getId());

                CustomerCommunicationContacts contact = getActiveCustomerCommunicationEmailContactOrElseNull(customerCommunicationId);

                if (contact != null) {
                    log.info("Found active customer communication contact for customerCommunicationId: {}", customerCommunicationId);

                    Set<String> emailAddress = splitEmail(customerEmailAddress);
                    emailAddress.forEach(
                            currentEmail -> saveEmailCommunicationCustomerContact(contact.getId(), emailCommunicationCustomer.getId(), currentEmail)
                    );
                } else {
                    String errorMessage = "customerCommunicationId-[customerCommunicationId] active customer communication contact with customer communication id : %s can't be found;".formatted(customerCommunicationId);
                    errorMessages.add(errorMessage);
                    log.error(errorMessage);
                }

            } else {
                String errorMessage = "customerCommunicationId-[customerCommunicationId] active customer communication with id : %s can't be found;".formatted(customerCommunicationId);
                errorMessages.add(errorMessage);
                log.error(errorMessage);
            }
        } else {
            String errorMessage = "customerDetailId-[customerDetailId] active customer detail with id: %s can't be found;".formatted(customerDetailId);
            errorMessages.add(errorMessage);
            log.error(errorMessage);
        }
    }

    /**
     * Validates if the email communication has been sent and has a valid email template.
     * If both conditions are met, this method retrieves the associated customer ID and
     * generates the email body using the provided template, then sets it in the given
     * {@link EmailCommunication} object.
     *
     * @param emailCommunication The {@link EmailCommunication} object containing the email
     *                           communication details, including its status and template ID.
     * @throws DomainEntityNotFoundException If no customer is found associated with the given
     *                                       email communication ID.
     */
    private void validateAndSetEmailBodyByTemplate(EmailCommunication emailCommunication) {
        if (SENT.equals(emailCommunication.getEmailCommunicationStatus())) {

            EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationCustomerRepository
                    .findByEmailCommunicationId(emailCommunication.getId())
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "Customer not found with given email communication id [%s];".formatted(emailCommunication.getId()))
                    );

            List<EmailCommunicationTemplates> notificationTemplates = emailCommunicationTemplateRepository.findAllByEmailCommIdAndStatus(emailCommunication.getId(), EntityStatus.ACTIVE);
            Long bodyTemplateId = emailCommunication.getEmailTemplateId();

            if (Objects.nonNull(bodyTemplateId) || CollectionUtils.isNotEmpty(notificationTemplates)) {
                EmailAndSmsDocumentRequest documentRequest = new EmailAndSmsDocumentRequest();
                documentRequest.setEmailCommunicationCustomerId(emailCommunicationCustomer.getId());
                EmailAndSmsDocumentModel emailAndSmsDocumentModel = emailAndSmsDocumentCreationService.generateDocumentJsonModel(documentRequest);

                if (Objects.nonNull(emailCommunication.getEmailTemplateId())) {
                    if (Objects.nonNull(emailAndSmsDocumentModel)) {
                        String generatedBody = emailAndSmsDocumentCreationService.generateSingleEmailBody(emailCommunication.getEmailTemplateId(), emailAndSmsDocumentModel);
                        emailCommunication.setEmailBody(generatedBody);
                    }
                }

                if (CollectionUtils.isNotEmpty(notificationTemplates)) {
                    for (EmailCommunicationTemplates curr : notificationTemplates) {
                        emailAndSmsDocumentCreationService.generateAndSaveEmailAttachment(
                                curr.getTemplateId(),
                                emailCommunicationCustomer.getCustomerDetailId(),
                                emailCommunication.getId(),
                                emailAndSmsDocumentModel
                        );
                    }
                }
            }
        }
    }

    private void validateAndSetEmailBodyByTemplateForReminder(
            EmailCommunication emailCommunication,
            ReminderProcessItem reminderProcessItemRow,
            Long customerDetailId
    ) {
        Long bodyTemplateId = emailCommunication.getEmailTemplateId();

        if (Objects.nonNull(bodyTemplateId)) {
            ReminderDocumentModel reminderDocumentModel = reminderDocumentGenerationService.generateReminderJson(
                    reminderProcessItemRow.getReminderId(),
                    reminderProcessItemRow.getLiabilityId(),
                    customerDetailId,
                    reminderProcessItemRow.getTotalAmount(),
                    reminderProcessItemRow.getCommunicationId()
            );
            log.info("Creating email for reminder , head quarter type : {}", reminderDocumentModel.getHeadquarterStrBlvdType());
            if (Objects.nonNull(emailCommunication.getEmailTemplateId())) {
                if (Objects.nonNull(reminderDocumentModel)) {
                    Pair<String, String> pair = emailAndSmsDocumentCreationService.generateSingleEmailBodyForReminder(emailCommunication.getEmailTemplateId(), reminderDocumentModel);
                    emailCommunication.setEmailBody(pair.getLeft());
                    log.info("Sms body set {}", pair.getLeft());
                    emailCommunication.setEmailSubject(pair.getRight());
                }
            }
        }
    }

    /**
     * Validates and updates the customer data associated with the given email communication.
     * This method performs the following steps:
     * Retrieves the active email communication customer entity associated with the given email communication ID.
     * Checks if the customer data has changed by comparing the existing data with the data provided in the request.
     * If the customer data has changed:
     * Deletes all email communication customer contacts associated with the existing email communication customer entity.
     * Deletes the email communication customer entity itself.
     * Validates and sets the new customer data provided in the request.
     * If the customer data has not changed:
     * Splits the provided customer email addresses into a set.
     * Retrieves all email communication customer contacts associated with the email communication customer entity.
     * Transforms these contacts into a map for easy lookup by email address.
     * Deletes any contacts that are no longer present in the new set of email addresses.
     * Retrieves the active customer communication email contact associated with the customer communication ID.
     * If the customer communication contact is found:
     * Saves new email communication customer contacts for any new email addresses not already present in the contact map.
     * If the customer communication contact is not found:
     * Adds an error message indicating that the active and related customer communication contact could not be found.
     *
     * @param emailCommunication The email communication entity containing information about the email communication.
     * @param request            The request object containing the edited customer data, including customer detail ID, customer communication ID, and customer email addresses.
     * @param errorMessages      A list to collect error messages encountered during the validation and update process.
     */
    private void validateAndUpdateCustomerData(
            EmailCommunication emailCommunication,
            EmailCommunicationEditRequest request,
            List<String> errorMessages
    ) {
        EmailCommunicationCustomer emailCommCustomerEntity = getActiveEmailCommunicationCustomerOrElseNull(emailCommunication.getId());
        if (Objects.isNull(emailCommCustomerEntity)) {
            throw new DomainEntityNotFoundException("actიve email communication customer not found with id : %s;");
        }

        if (isCustomerDataChanged(emailCommCustomerEntity, request)) {
            emailCommunicationCustomerContactRepository.deleteAllByEmailCommunicationCustomerId(emailCommCustomerEntity.getId());
            emailCommunicationCustomerRepository.deleteById(emailCommCustomerEntity.getId());
            validateAndSetCustomerData(
                    emailCommunication,
                    request.getCustomerDetailId(),
                    request.getCustomerCommunicationId(),
                    request.getCustomerEmailAddress(),
                    errorMessages
            );
        } else {
            Set<String> emailAddress = splitEmail(request.getCustomerEmailAddress());
            Set<EmailCommunicationCustomerContact> contacts = emailCommunicationCustomerContactRepository.getAllByEmailCommunicationId(emailCommCustomerEntity.getEmailCommunicationId());
            Map<String, EmailCommunicationCustomerContact> map = EPBListUtils.transformToMap(List.copyOf(contacts), EmailCommunicationCustomerContact::getEmailAddress);

            contacts
                    .stream()
                    .filter(c -> !emailAddress.contains(c.getEmailAddress()))
                    .forEach(c -> emailCommunicationCustomerContactRepository.deleteById(c.getId()));

            CustomerCommunicationContacts contact = getActiveCustomerCommunicationEmailContactOrElseNull(emailCommCustomerEntity.getCustomerCommunicationId());
            if (contact != null) {
                emailAddress
                        .stream()
                        .filter(current -> !map.containsKey(current))
                        .forEach(
                                currentEmail -> saveEmailCommunicationCustomerContact(contact.getId(), emailCommCustomerEntity.getId(), currentEmail)
                        );
            } else {
                errorMessages.add("customerCommunicationId-[customerCommunicationId] active and related customer communication contact with id : %s can't be found;".formatted(request.getCustomerCommunicationId()));
            }
        }
    }

    /**
     * Saves a new email communication customer contact with the specified details.
     * This method creates a new {@link EmailCommunicationCustomerContact} entity using the provided customer communication
     * contact ID, email communication contact ID, and email address. The entity is then saved and flushed to the repository.
     *
     * @param customerComContactId         The ID of the customer communication contact.
     * @param emailCommunicationCustomerId The ID of the email communication customer id.
     * @param emailAddress                 The email address to be associated with the customer contact.
     */
    private EmailCommunicationCustomerContact saveEmailCommunicationCustomerContact(
            Long customerComContactId,
            Long emailCommunicationCustomerId,
            String emailAddress
    ) {
        EmailCommunicationCustomerContact emailCommunicationCustomerContact = emailCommunicationMapper.emailCommunicationCustomerContact(
                customerComContactId,
                emailCommunicationCustomerId,
                emailAddress
        );
        emailCommunicationCustomerContactRepository.saveAndFlush(emailCommunicationCustomerContact);
        return emailCommunicationCustomerContact;
    }

    /**
     * Validates the related customer IDs provided in the request and sets them in the given EmailCommunication object.
     * If any of the related customer IDs do not correspond to an active customer, an error message is added to the errorMessages list.
     *
     * @param emailCommunication The EmailCommunication object to which related customers will be associated.
     * @param relatedCustomerIds The set of IDs of related customers to validate and associate.
     * @param errorMessages      A list to collect error messages if any related customer IDs do not correspond to active customers.
     */
    private void validateAndSetRelatedCustomers(
            EmailCommunication emailCommunication,
            Set<Long> relatedCustomerIds,
            List<String> errorMessages
    ) {
        if (CollectionUtils.isNotEmpty(relatedCustomerIds)) {
            relatedCustomerIds.forEach(current -> {
                Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(current, List.of(CustomerStatus.ACTIVE));
                if (customerOptional.isPresent()) {
                    EmailCommunicationRelatedCustomer relatedCustomer = emailCommunicationMapper.emailCommunicationRelatedCustomer(emailCommunication.getId(), current);
                    emailCommunicationRelatedCustomerRepository.saveAndFlush(relatedCustomer);
                } else {
                    errorMessages.add("relatedCustomerIds-[relatedCustomerIds] active customer with id: %s can't be found;".formatted(current));
                }
            });
        }
    }

    /**
     * Validates and updates related customers for the given email communication.
     * This method first retrieves the existing related customers associated with the email communication
     * and transforms them into a map for easy lookup by customer ID. It then processes the requested customer IDs
     * to determine which customers need to be added or deleted:
     * If {@code requestedCustomerIds} is not empty:
     * Finds customer IDs that are in {@code requestedCustomerIds} but not in {@code existingMap}
     * (indicating new customers to add).
     * Validates and sets these new customers using {@link #validateAndSetRelatedCustomers(EmailCommunication, Set, List)}.
     * Validates and deletes related customers that are in {@code existingMap} but not in
     * {@code requestedCustomerIds} using {@link #validateAndDeleteRelatedCustomers(Map, Set)}.
     * If {@code requestedCustomerIds} is empty:
     * Validates and deletes all existing related customers using {@link #validateAndDeleteRelatedCustomers(Map, Set)}.
     *
     * @param emailCommunication   The email communication entity for which related customers are being updated.
     * @param requestedCustomerIds The set of customer IDs requested to be associated with the email communication.
     * @param errorMessages        A list to collect error messages encountered during validation and updating of related customers.
     */
    private void validateAndUpdateRelatedCustomers(
            EmailCommunication emailCommunication,
            Set<Long> requestedCustomerIds,
            List<String> errorMessages
    ) {
        Map<Long, EmailCommunicationRelatedCustomer> existingMap = EPBListUtils.transformToMap(
                emailCommunicationRelatedCustomerRepository.findAllByEmailCommunicationIdAndStatus(emailCommunication.getId(), EntityStatus.ACTIVE),
                EmailCommunicationRelatedCustomer::getCustomerId
        );
        if (CollectionUtils.isNotEmpty(requestedCustomerIds)) {
            Set<Long> added = requestedCustomerIds
                    .stream()
                    .filter(current -> !existingMap.containsKey(current))
                    .collect(Collectors.toSet());

            validateAndSetRelatedCustomers(emailCommunication, added, errorMessages);
            validateAndDeleteRelatedCustomers(existingMap, requestedCustomerIds);
        } else {
            validateAndDeleteRelatedCustomers(existingMap, SetUtils.emptySet());
        }
    }

    /**
     * Validates and deletes related customers that are no longer associated with the email communication.
     * This method compares the existing related customers (from {@code existingMap}) with the set of
     * {@code requestedCustomerIds} to determine which related customers should be deleted:
     * Filters out related customers whose IDs are not present in {@code requestedCustomerIds} (indicating deletion).
     * Sets the status of these filtered related customers to {@link EntityStatus#DELETED}.
     * Saves the modified related customers to the repository, marking them as deleted.
     *
     * @param existingMap          A map containing existing related customers associated with the email communication (keyed by customer ID).
     * @param requestedCustomerIds The set of customer IDs that should remain associated with the email communication.
     */
    private void validateAndDeleteRelatedCustomers(
            Map<Long, EmailCommunicationRelatedCustomer> existingMap,
            Set<Long> requestedCustomerIds
    ) {
        List<EmailCommunicationRelatedCustomer> deleted = existingMap
                .entrySet()
                .stream()
                .filter(relatedCustomer -> !requestedCustomerIds.contains(relatedCustomer.getKey()))
                .map(Map.Entry::getValue)
                .peek(relatedCustomer -> relatedCustomer.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            emailCommunicationRelatedCustomerRepository.saveAllAndFlush(deleted);
        }
    }

    /**
     * Validates and sets contact purposes for an email communication.
     * Validates each contact purpose ID in {@code contactPurposeIds} against the active statuses
     * in the repository. If a contact purpose is found, it creates and saves an EmailCommunicationContactPurpose
     * entity for the specified {@code emailCommunicationId}. If a contact purpose is not found, it adds an error
     * message to the {@code errorMessages} list indicating that the contact purpose could not be found.
     *
     * @param emailCommunicationId The ID of the email communication for which contact purposes are being set.
     * @param contactPurposeIds    The set of contact purpose IDs to validate and set.
     * @param errorMessages        The list to which error messages are appended if a contact purpose is not found.
     */
    private void validateAndSetContactPurposes(
            Long emailCommunicationId,
            Set<Long> contactPurposeIds,
            List<String> errorMessages
    ) {
        if (CollectionUtils.isNotEmpty(contactPurposeIds)) {
            for (Long current : contactPurposeIds) {
                Optional<ContactPurpose> contactPurposeOptional = contactPurposeRepository.findByIdAndStatuses(current, List.of(NomenclatureItemStatus.ACTIVE));
                if (contactPurposeOptional.isPresent()) {
                    emailCommunicationContactPurposeRepository.saveAndFlush(
                            emailCommunicationMapper.emailCommunicationContactPurpose(emailCommunicationId, contactPurposeOptional.get().getId())
                    );
                } else {
                    errorMessages.add("contactPurposeIds-[contactPurposeIds] active contact purpose with id: %s can't be found;".formatted(current));
                }
            }
        }
    }

    /**
     * Validates and deletes obsolete contact purposes associated with an email communication.
     * Checks the existing map of {@code EmailCommunicationContactPurpose} entities against
     * the provided {@code contactPurposes} set. If any contact purpose ID in the existing map
     * is not present in the {@code contactPurposes} set, it marks the corresponding
     * {@code EmailCommunicationContactPurpose} entity as deleted by setting its status to {@code DELETED}.
     * The deleted entities are then saved and flushed to the repository.
     *
     * @param existingMap     The map of existing {@code EmailCommunicationContactPurpose} entities
     *                        associated with the email communication.
     * @param contactPurposes The set of contact purpose IDs that should remain associated with the email communication.
     */
    private void validateAndDeleteContactPurposes(
            Map<Long, EmailCommunicationContactPurpose> existingMap,
            Set<Long> contactPurposes
    ) {
        List<EmailCommunicationContactPurpose> deleted = existingMap
                .entrySet()
                .stream()
                .filter(entry -> !contactPurposes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .peek(contactPurpose -> contactPurpose.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            emailCommunicationContactPurposeRepository.saveAllAndFlush(deleted);
        }
    }

    /**
     * Validates and sets the communication files for a given email communication.
     * This method checks if the provided communication files are active and associated with the given email communication ID.
     * If a file is found and is active, it sets the email communication ID to the file and saves it.
     * If any file is not found or is inactive, an error message is added to the errorMessages list.
     * Additionally, if no errors are found, the method calculates the total size of the files and checks if it exceeds the maximum allowed size.
     * If the total size exceeds the limit, an error message is added to the errorMessages list.
     *
     * @param emailCommunicationId the ID of the email communication to which the files should be associated
     * @param communicationFiles   a set of IDs representing the communication files to be validated and set
     * @param errorMessages        a list to hold any error messages encountered during the process
     * @throws IllegalArgumentException if the provided emailCommunicationId is null
     * @throws IllegalStateException    if the file service fails to download a file or if the repository operations fail
     */
    private void validateAndSetCommunicationFiles(
            Long emailCommunicationId,
            Set<Long> communicationFiles,
            List<String> errorMessages
    ) {
        if (CollectionUtils.isNotEmpty(communicationFiles)) {
            for (Long current : communicationFiles) {
                Optional<EmailCommunicationFile> emailCommunicationFileOptional = emailCommunicationFileRepository.findCommunicationFileByIdAndStatus(current);
                if (emailCommunicationFileOptional.isPresent()) {
                    EmailCommunicationFile emailCommunicationFile = emailCommunicationFileOptional.get();
                    emailCommunicationFile.setEmailCommunicationId(emailCommunicationId);
                    emailCommunicationFileRepository.saveAndFlush(emailCommunicationFile);
                } else {
                    errorMessages.add("communicationFileIds-[communicationFileIds] active file with id: %s can't be found;".formatted(current));
                }
            }
            validateCommunicationFileSizeSum(emailCommunicationId, errorMessages);
        }
    }

    /**
     * Validates and sets attachments for the given email communication.
     * This method performs the following steps:
     * Iterates over the set of attachment IDs.
     * For each attachment ID, it checks if the attachment exists and is active.
     * If the attachment is valid, it sets the email communication ID for the attachment and saves the changes.
     * If the attachment is not found or inactive, it adds an error message to the provided list.
     * If there are no errors, it calculates the total size of all attachments associated with the email communication.
     * If the total size exceeds the maximum allowed size, it adds an error message to the provided list.
     *
     * @param emailCommunicationId the ID of the email communication to which attachments are to be associated
     * @param attachments          a set of attachment IDs to be validated and associated with the email communication
     * @param errorMessages        a list to collect error messages encountered during the validation process
     */
    private void validateAndSetAttachments(
            Long emailCommunicationId,
            Set<Long> attachments,
            List<String> errorMessages
    ) {
        if (CollectionUtils.isNotEmpty(attachments)) {
            for (Long current : attachments) {
                Optional<EmailCommunicationAttachment> emailCommunicationAttachmentOptional = emailCommunicationAttachmentRepository.findByIdAndStatus(current, EntityStatus.ACTIVE);
                if (emailCommunicationAttachmentOptional.isPresent()) {
                    EmailCommunicationAttachment emailCommunicationAttachment = emailCommunicationAttachmentOptional.get();
                    emailCommunicationAttachment.setEmailCommunicationId(emailCommunicationId);
                    emailCommunicationAttachmentRepository.saveAndFlush(emailCommunicationAttachment);
                } else {
                    errorMessages.add("attachmentFileIds-[attachmentFileIds] active file with id: %s can't be found;".formatted(current));
                }
            }
            validateAttachmentFileSizeSum(emailCommunicationId, errorMessages);
        }
    }

    /**
     * Validates and updates attachments for the specified email communication.
     * This method retrieves existing attachments associated with the email communication and processes
     * the requested attachment IDs to determine which attachments should be added or deleted:
     * Retrieves existing attachments and transforms them into a map for easy lookup by attachment ID.
     * If {@code attachmentsIds} is not empty:
     * Finds attachment IDs that are in {@code attachmentsIds} but not in {@code existingMap} (indicating new attachments to add).
     * Validates and sets these new attachments using {@link #validateAndSetAttachments(Long, Set, List)}.
     * Validates and deletes attachments that are in {@code existingMap} but not in {@code attachmentsIds} using {@link #validateAndDeleteAttachments(Map, Set)}.
     * If {@code attachmentsIds} is empty:
     * Validates and deletes all existing attachments associated with the email communication.
     *
     * @param emailCommunicationId The ID of the email communication for which attachments are being updated.
     * @param attachmentsIds       The set of attachment IDs requested to be associated with the email communication.
     * @param errorMessages        A list to collect error messages encountered during validation and updating of attachments.
     */
    private void validateAndUpdateAttachments(
            Long emailCommunicationId,
            Set<Long> attachmentsIds,
            List<String> errorMessages
    ) {
        Map<Long, EmailCommunicationAttachment> existingMap = EPBListUtils.transformToMap(emailCommunicationAttachmentRepository.findAllActiveAttachmentsByEmailCommunicationId(emailCommunicationId), EmailCommunicationAttachment::getId);
        if (CollectionUtils.isNotEmpty(attachmentsIds)) {
            Set<Long> added = attachmentsIds
                    .stream()
                    .filter(current -> !existingMap.containsKey(current))
                    .collect(Collectors.toSet());

            validateAndSetAttachments(emailCommunicationId, added, errorMessages);
            validateAndDeleteAttachments(existingMap, attachmentsIds);
        } else {
            validateAndDeleteAttachments(existingMap, SetUtils.emptySet());
        }
    }

    /**
     * Validates and updates files for the specified email communication.
     * This method retrieves existing files associated with the email communication and processes
     * the requested file IDs to determine which files should be added or deleted:
     * Retrieves existing files and transforms them into a map for easy lookup by file ID.
     * If {@code fileIds} is not empty:
     * Finds file IDs that are in {@code attachmentsIds} but not in {@code existingMap} (indicating new attachments to add).
     * Validates and sets these new attachments using {@link #validateAndSetAttachments(Long, Set, List)}.
     * Validates and deletes files that are in {@code existingMap} but not in {@code fileIds} using {@link #validateAndDeleteCommunicationFiles(Map, Set)}.
     * If {@code fileIds} is empty:
     * Validates and deletes all existing files associated with the email communication.
     *
     * @param emailCommunicationId The ID of the email communication for which attachments are being updated.
     * @param fileIds              The set of file IDs requested to be associated with the email communication.
     * @param errorMessages        A list to collect error messages encountered during validation and updating of files.
     */
    private void validateAndUpdateCommunicationFiles(
            Long emailCommunicationId,
            Set<Long> fileIds,
            List<String> errorMessages
    ) {
        Map<Long, EmailCommunicationFile> existingMap = EPBListUtils.transformToMap(emailCommunicationFileRepository.findAllActiveFileByEmailCommunicationId(emailCommunicationId), EmailCommunicationFile::getId);
        if (CollectionUtils.isNotEmpty(fileIds)) {
            Set<Long> added = fileIds
                    .stream()
                    .filter(current -> !existingMap.containsKey(current))
                    .collect(Collectors.toSet());

            validateAndSetCommunicationFiles(emailCommunicationId, added, errorMessages);
            validateAndDeleteCommunicationFiles(existingMap, fileIds);
        } else {
            validateAndDeleteCommunicationFiles(existingMap, SetUtils.emptySet());
        }
    }

    /**
     * Validates and deletes attachments that are no longer associated with the email communication.
     * This method compares the existing attachments (from {@code existingMap}) with the set of {@code attachments}
     * to determine which attachments should be deleted:
     * Filters out attachments whose IDs are not present in {@code attachments} (indicating deletion).
     * Sets the status of these filtered attachments to {@link EntityStatus#DELETED}.
     * Saves the modified attachments to the repository, marking them as deleted.
     *
     * @param existingMap A map containing existing attachments associated with the email communication (keyed by attachment ID).
     * @param attachments The set of attachment IDs that should remain associated with the email communication.
     */
    private void validateAndDeleteAttachments(
            Map<Long, EmailCommunicationAttachment> existingMap,
            Set<Long> attachments
    ) {
        List<EmailCommunicationAttachment> deleted = existingMap
                .entrySet()
                .stream()
                .filter(entry -> !attachments.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .peek(emailCommunicationAttachment -> emailCommunicationAttachment.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            emailCommunicationAttachmentRepository.saveAllAndFlush(deleted);
        }
    }

    /**
     * Validates and deletes files that are no longer associated with the email communication.
     * This method compares the existing files (from {@code existingMap}) with the set of {@code files}
     * to determine which files should be deleted:
     * Filters out files whose IDs are not present in {@code files} (indicating deletion).
     * Sets the status of these filtered files to {@link EntityStatus#DELETED}.
     * Saves the modified files to the repository, marking them as deleted.
     *
     * @param existingMap A map containing existing files associated with the email communication (keyed by file ID).
     * @param files       The set of file IDs that should remain associated with the email communication.
     */
    private void validateAndDeleteCommunicationFiles(Map<Long, EmailCommunicationFile> existingMap, Set<Long> files) {
        List<EmailCommunicationFile> deleted = existingMap
                .entrySet()
                .stream()
                .filter(entry -> !files.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .peek(emailCommunicationFile -> emailCommunicationFile.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            emailCommunicationFileRepository.saveAllAndFlush(deleted);
        }
    }

    /**
     * Validates the total size of attachments associated with the specified email communication.
     * This method calculates the total size of all attachments associated with the email communication
     * identified by {@code emailCommunicationId}. If the total size exceeds {@link #MAX_SIZE_ATTACHMENT_FILES},
     * an error message indicating the maximum file size limit is added to {@code errorMessages}.
     * Note: This method assumes that attachments are represented by URLs or paths accessible via onAttachmentRepository.
     * It retrieves each attachment, calculates its size,
     * and sums them up to determine the total size.
     *
     * @param emailCommunicationId The ID of the email communication whose attachments' total size is to be validated.
     * @param errorMessages        A list to collect error messages encountered during validation of attachment file size.
     */
    private void validateAttachmentFileSizeSum(Long emailCommunicationId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(errorMessages)) {
            List<String> fileUrls = emailCommunicationAttachmentRepository.findAllByEmailCommunicationIdAndStatus(emailCommunicationId);

            long sum = fileUrls
                    .stream()
                    .map(fileService::downloadFile)
                    .mapToLong(byteArrayResource -> byteArrayResource.getByteArray().length)
                    .sum();

            if (sum > MAX_SIZE_ATTACHMENT_FILES) {
                errorMessages.add("attachmentFileIds-[attachmentFileIds] you have reached max file size witch is 25 mb;");
            }
        }
    }

    /**
     * Validates the total size of files associated with the specified email communication.
     * This method calculates the total size of all files associated with the email communication
     * identified by {@code emailCommunicationId}. If the total size exceeds {@link #MAX_SIZE_COMMUNICATION_FILES},
     * an error message indicating the maximum file size limit is added to {@code errorMessages}.
     * Note: This method assumes that files are represented by URLs or paths accessible via onFileRepository.
     * It retrieves each file, calculates its size,
     * and sums them up to determine the total size.
     *
     * @param emailCommunicationId The ID of the email communication whose files' total size is to be validated.
     * @param errorMessages        A list to collect error messages encountered during validation of file size.
     */
    private void validateCommunicationFileSizeSum(Long emailCommunicationId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(errorMessages)) {
            List<String> fileUrls = emailCommunicationFileRepository.findAllByEmailCommunicationIdAndStatus(emailCommunicationId);

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

    /**
     * Uploads a file for email communication and returns a proxy response containing file details.
     * This method uploads the provided multipart file to a specified location using a file service.
     * It performs checks on the file size and ensures the file name is not empty or null before proceeding with the upload.
     * If the file size exceeds the maximum allowed limit (50 MB), an exception is thrown.
     *
     * @param file the multipart file to be uploaded
     * @return a {@link ProxyFileResponse} containing details of the uploaded file
     * @throws IllegalArgumentsProvidedException if the file size exceeds 50 MB or if the original filename is empty or null
     * @throws IllegalStateException             if file upload or repository operations fail
     */
    public FileWithStatusesResponse uploadFile(MultipartFile file, List<DocumentFileStatus> statuses) {
        log.debug("Email communication file {}.", file.getName());

        if (checkCommunicationFileSize(file)) {
            log.error("You have reached max file size witch is 50 mb;");
            throw new IllegalArgumentsProvidedException("You have reached max file size witch is 50 mb;");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Email communication file name is empty or null");
            throw new IllegalArgumentsProvidedException("Email communication file name is empty or null");
        }

        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = getFileName(formattedFileName);
        String fileUrl = fileService.uploadFile(file, remotePath(), fileName);

        EmailCommunicationFile emailCommunicationFile = emailCommunicationMapper.emailCommunicationFile(fileUrl, formattedFileName, false, statuses);
        emailCommunicationFileRepository.saveAndFlush(emailCommunicationFile);
        return new FileWithStatusesResponse(
                emailCommunicationFile, accountManagerRepository.findByUserName(emailCommunicationFile.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")
        );
    }

    /**
     * Uploads an attachment for email communication and returns a proxy response containing attachment details.
     * This method uploads the provided multipart file attachment to a specified location using a file service.
     * It performs checks on the attachment size and ensures the attachment name is not empty or null before proceeding with the upload.
     * If the attachment size exceeds the maximum allowed limit (25 MB), an exception is thrown.
     *
     * @param file the multipart file attachment to be uploaded
     * @return a {@link ProxyFileResponse} containing details of the uploaded attachment
     * @throws IllegalArgumentsProvidedException if the attachment size exceeds 25 MB or if the original filename is empty or null
     * @throws IllegalStateException             if file upload or repository operations fail
     */
    public ProxyFileResponse uploadAttachment(MultipartFile file) {
        log.debug("Email communication attachment {}.", file.getName());

        if (checkAttachmentFileSize(file)) {
            log.error("You have reached max file size witch is 25 mb;");
            throw new IllegalArgumentsProvidedException("You have reached max file size witch is 25 mb;");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Email communication attachment name is empty or null");
            throw new IllegalArgumentsProvidedException("Email communication attachment name is empty or null");
        }

        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = getFileName(formattedFileName);
        String fileUrl = fileService.uploadFile(file, remoteAttachmentPath(), fileName);

        EmailCommunicationAttachment emailCommunicationAttachment = emailCommunicationMapper.emailCommunicationAttachment(fileUrl, formattedFileName);
        emailCommunicationAttachmentRepository.saveAndFlush(emailCommunicationAttachment);
        return new ProxyFileResponse(emailCommunicationAttachment);
    }

    /**
     * Validates and sets the sending status of an email communication based on the requested status and user permissions.
     * This method checks if the user has permission to set the email communication status to 'DRAFT' or 'SENT'.
     * If the requested status is 'DRAFT' and the user has permission, the email communication status is set to 'DRAFT'.
     * If the requested status is 'SENT' and the user has permission, the email communication status is set to 'SENT'.
     * If the user does not have the required permission for the requested status, an AccessDeniedException is thrown.
     *
     * @param emailCommunication the email communication object to update its sending status
     * @param emailCreateType    the requested status to set for the email communication ('DRAFT' or 'SENT')
     * @throws AccessDeniedException if the user does not have permission to create draft or send email
     */
    public void validateAndSetSendingStatusByPermissionOnCreate(
            EmailCommunication emailCommunication,
            EmailCreateType emailCreateType
    ) {
        EmailCommunicationStatus emailCommunicationStatus = null;

        switch (emailCreateType) {
            case DRAFT -> {
                if (hasCreateDraftPermission()) {
                    emailCommunicationStatus = EmailCommunicationStatus.DRAFT;
                } else {
                    log.error("You do not have permission to create draft email.");
                    throw new AccessDeniedException("You do not have permission to create draft email.");
                }
            }
            case SEND -> {
                if (hasCreateAndSendPermission()) {
                    emailCommunicationStatus = EmailCommunicationStatus.SENT;
                } else {
                    log.error("You do not have permission to create and send email.");
                    throw new AccessDeniedException("You do not have permission to create and send email.");
                }
            }
        }

        if (emailCommunicationStatus == null) {
            throw new AccessDeniedException("You do not have permission to create and send email.");
        }

        emailCommunication.setEmailCommunicationStatus(emailCommunicationStatus);
    }

    /**
     * Validates the permission to create and set the sending status for a MassEmailCreateType on the given EmailCommunication.
     * Depending on the MassEmailCreateType (DRAFT or SEND), checks the corresponding permission and sets the email communication status accordingly.
     *
     * @param emailCommunication The EmailCommunication entity to set the sending status on.
     * @param emailCreateType    The MassEmailCreateType indicating whether the email should be created as a draft or sent immediately.
     * @throws AccessDeniedException If the user does not have permission to perform the specified mass email creation action.
     */
    public void validateAndSetSendingStatusByPermissionOnMassCreate(
            EmailCommunication emailCommunication,
            EmailCreateType emailCreateType
    ) {
        EmailCommunicationStatus emailCommunicationStatus = null;

        switch (emailCreateType) {
            case DRAFT -> {
                if (hasCreateDraftMassEmailPermission()) {
                    emailCommunicationStatus = DRAFT;
                } else {
                    log.error("You do not have permission to create draft mass email.");
                    throw new AccessDeniedException("You do not have permission to create draft mass email.");
                }
            }
            case SEND -> {
                if (hasCreateAndSendMassEmailPermission()) {
                    emailCommunicationStatus = SENT;
                } else {
                    log.error("You do not have permission to create and send mass email.");
                    throw new AccessDeniedException("You do not have permission to create and send mass email.");
                }
            }
        }

        if (emailCommunicationStatus == null) {
            throw new AccessDeniedException("You do not have permission to create and send mass email.");
        }

        emailCommunication.setEmailCommunicationStatus(emailCommunicationStatus);
    }

    /**
     * Validates and sets the sending status of an {@link EmailCommunication} entity based on the provided {@link EmailCreateType} and user permissions.
     * This method determines the appropriate status to set on the {@code emailCommunication} based on the {@code createType} parameter. If the
     * {@code createType} is {@link EmailCreateType#SEND} and the user has permission to send emails, the status is set to {@link EmailCommunicationStatus#SENT}.
     * If the {@code createType} is {@link EmailCreateType#DRAFT}, the status is set to {@link EmailCommunicationStatus#DRAFT}. If the user lacks permission
     * to send emails but the requested status is {@link EmailCommunicationStatus#SENT}, an {@link AccessDeniedException} is thrown.
     *
     * @param emailCommunication the {@link EmailCommunication} entity whose status needs to be updated.
     * @param createType         the {@link EmailCreateType} indicating the desired status for the email communication.
     * @throws AccessDeniedException if the {@code createType} is {@link EmailCreateType#SEND} and the user does not have permission to send emails.
     * @see EmailCreateType
     * @see EmailCommunicationStatus
     * @see EmailCommunication
     * @see AccessDeniedException
     */
    public void validateAndSetSendingStatusByPermissionOnUpdate(
            EmailCommunication emailCommunication,
            EmailCreateType createType
    ) {
        EmailCommunicationStatus emailCommunicationStatus = null;
        EmailCommunicationStatus requestedStatus = null;

        if (Objects.requireNonNull(createType) == EmailCreateType.SEND) {
            requestedStatus = SENT;
        } else if (createType == EmailCreateType.DRAFT) {
            requestedStatus = DRAFT;
        }

        if (Objects.requireNonNull(requestedStatus) == EmailCommunicationStatus.SENT) {
            if (hasCreateAndSendPermission()) {
                emailCommunicationStatus = EmailCommunicationStatus.SENT;
            } else {
                log.error("You do not have permission to create and send email.");
                throw new AccessDeniedException("You do not have permission to create and send email.");
            }
        }

        if (emailCommunicationStatus != null) {
            emailCommunication.setEmailCommunicationStatus(emailCommunicationStatus);
        }
    }

    /**
     * Validates and sets the sending status of an email communication based on permission for mass updates.
     * Determines the requested sending status ({@code SENT} or {@code DRAFT}) based on the provided {@code createType}.
     * Checks if the user has permission to create and send mass emails. If the requested status is {@code SENT} and
     * the user has permission, sets the email communication status to {@code SENT}. Otherwise, throws an
     * {@code AccessDeniedException} indicating insufficient permissions.
     *
     * @param emailCommunication The EmailCommunication entity for which the sending status is validated and set.
     * @param createType         The type of mass email creation (send or draft).
     * @throws AccessDeniedException If the user does not have permission to create and send mass emails when attempting to set the status to {@code SENT}.
     */
    public void validateAndSetSendingStatusByPermissionOnMassUpdate(
            EmailCommunication emailCommunication,
            EmailCreateType createType
    ) {
        EmailCommunicationStatus emailCommunicationStatus = null;
        EmailCommunicationStatus requestedStatus = null;

        if (Objects.requireNonNull(createType) == EmailCreateType.SEND) {
            requestedStatus = SENT;
        } else if (createType == EmailCreateType.DRAFT) {
            requestedStatus = DRAFT;
        }

        if (Objects.requireNonNull(requestedStatus) == EmailCommunicationStatus.SENT) {
            if (hasCreateAndSendMassEmailPermission()) {
                emailCommunicationStatus = EmailCommunicationStatus.SENT;
            } else {
                log.error("You do not have permission to create and send mass email.");
                throw new AccessDeniedException("You do not have permission to create and send mass email.");
            }
        }

        if (emailCommunicationStatus != null) {
            emailCommunication.setEmailCommunicationStatus(emailCommunicationStatus);
        }
    }

    /**
     * Retrieves the short response data for customer communication associated
     * with a specific email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication to look up
     * @return an instance of {@link EmailConnectedCustomerCommunicationDataShortResponse}
     * containing the customer communication data
     * @throws DomainEntityNotFoundException if no customer communication data
     *                                       is found for the given email communication ID
     */
    private EmailConnectedCustomerCommunicationDataShortResponse getCustomerCommunicationDataShortResponseByEmailCommunicationId(
            Long emailCommunicationId
    ) {
        return emailCommunicationCustomerRepository
                .getCustomerCommunicationDataByEmailCommunicationId(emailCommunicationId)
                .map(shortResponse -> new EmailConnectedCustomerCommunicationDataShortResponse(
                             shortResponse.id(),
                             shortResponse.name(),
                             customerRepository.getConcatPurposeFromCustomerCommunicationData(shortResponse.id())
                     )
                )
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer communication data not found with given email communication id [%s];".formatted(emailCommunicationId))
                );
    }

    /**
     * Retrieves the short response data for customer communication associated
     * with a specific email communication customer ID.
     *
     * @param emailCommunicationCustomerId the ID of the email communication customer to look up
     * @return an instance of {@link EmailConnectedCustomerCommunicationDataShortResponse}
     * containing the customer communication data
     * @throws DomainEntityNotFoundException if no customer communication data
     *                                       is found for the given email communication customer ID
     */
    private EmailConnectedCustomerCommunicationDataShortResponse getCustomerCommunicationDataShortResponseByEmailCommunicationCustomerId(
            Long emailCommunicationCustomerId
    ) {
        return emailCommunicationCustomerRepository
                .getCustomerCommunicationDataByEmailCommunicationCustomerId(emailCommunicationCustomerId)
                .map(shortResponse -> new EmailConnectedCustomerCommunicationDataShortResponse(
                             shortResponse.id(),
                             shortResponse.name(),
                             customerRepository.getConcatPurposeFromCustomerCommunicationData(shortResponse.id())
                     )
                )
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer communication data not found with given email communication customer id [%s];".formatted(emailCommunicationCustomerId))
                );
    }

    /**
     * Retrieves a ShortResponse for the customer details associated with the given email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication
     * @return a ShortResponse object containing customer details
     * @throws DomainEntityNotFoundException if no customer details are found for the given email communication ID
     */
    private EmailConnectedCustomerShortResponse getCustomerShortResponseByEmailCommunicationId(Long emailCommunicationId) {
        return emailCommunicationCustomerRepository
                .getByEmailCommunicationId(emailCommunicationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found with given email communication id [%s];".formatted(emailCommunicationId)));
    }

    /**
     * Retrieves the short response information of a customer based on the email communication customer ID.
     *
     * @param emailCommunicationCustomerId The ID used to identify the customer in the email communication system.
     * @return A {@link ShortResponse} object containing the short response information of the customer.
     * @throws DomainEntityNotFoundException if no customer details are found for the provided email communication customer ID.
     */
    private EmailConnectedCustomerShortResponse getCustomerShortResponseByEmailCommunicationCustomerId(Long emailCommunicationCustomerId) {
        return emailCommunicationCustomerRepository
                .getByEmailCommunicationCustomerId(emailCommunicationCustomerId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found with given email communication customer id [%s];".formatted(emailCommunicationCustomerId)));
    }

    /**
     * Retrieves a list of ShortResponse objects for the related customers associated with the given email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication
     * @return a list of ShortResponse objects containing related customer details
     */
    private List<ShortResponse> getRelatedCustomerShortResponse(Long emailCommunicationId) {
        return emailCommunicationRelatedCustomerRepository.findByEmailCommunicationId(emailCommunicationId);
    }

    /**
     * Retrieves related contact purposes as ShortResponse objects for a given email communication ID.
     *
     * @param emailCommunicationId The ID of the email communication for which to retrieve related contact purposes.
     * @return A list of ShortResponse objects representing related contact purposes.
     */
    private List<ShortResponse> getRelatedContactPurposesShortResponse(Long emailCommunicationId) {
        return emailCommunicationContactPurposeRepository.findContactPurposesByEmailCommunicationId(emailCommunicationId);
    }

    /**
     * Retrieves a ShortResponse for the topic of communication associated with the given topic ID.
     *
     * @param topicOfCommunicationId the ID of the topic of communication
     * @return a ShortResponse object containing topic of communication details
     * @throws DomainEntityNotFoundException if no topic of communication is found for the given ID
     */
    private ShortResponse getTopicOfCommunicationShortResponse(Long topicOfCommunicationId) {
        return topicOfCommunicationRepository
                .findById(topicOfCommunicationId)
                .map(topicOfCommunication -> new ShortResponse(topicOfCommunication.getId(), topicOfCommunication.getName()))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active Topic of communication does not exists with given id [%s];".formatted(topicOfCommunicationId)));
    }

    /**
     * Retrieves a ShortResponse for the email box associated with the given email box ID.
     *
     * @param emailBoxId the ID of the email box
     * @return a ShortResponse object containing email box details
     * @throws DomainEntityNotFoundException if no email box is found for the given ID
     */
    private ShortResponse getEmailBoxShortResponse(Long emailBoxId) {
        return emailMailboxesRepository
                .findById(emailBoxId)
                .map(emailMailboxes -> new ShortResponse(emailMailboxes.getId(), emailMailboxes.getName()))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active Email box does not exists with given id [%s];".formatted(emailBoxId)));
    }

    /**
     * Retrieves a list of ShortResponse objects for the files associated with the given email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication
     * @return a list of ShortResponse objects containing file details
     */
    private List<FileWithStatusesResponse> getEmailCommunicationFilesShortResponse(Long emailCommunicationId) {
        return emailCommunicationFileRepository
                .findAllActiveFileByEmailCommunicationId(emailCommunicationId)
                .stream()
                .map(emailCommunicationFile -> new FileWithStatusesResponse(
                             emailCommunicationFile,
                             accountManagerRepository
                                     .findByUserName(emailCommunicationFile.getSystemUserId())
                                     .map(manager -> " (".concat(manager.getDisplayName()).concat(")"))
                                     .orElse("")
                     )
                )
                .toList();
    }

    /**
     * Retrieves a list of ShortResponse objects for the attachments associated with the given email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication
     * @return a list of ShortResponse objects containing attachment details
     */
    private List<AttachmentShortResponse> getEmailCommunicationAttachmentsShortResponse(Long emailCommunicationId) {
        return emailCommunicationAttachmentRepository
                .findAllActiveAttachmentsByEmailCommunicationId(emailCommunicationId)
                .stream()
                .map(attachment -> new AttachmentShortResponse(attachment.getId(), attachment.getName(), false))
                .toList();
    }

    private List<AttachmentShortResponse> getEmailCommunicationCustomerAttachmentsShortResponse(Long emailCommunicationCustomerId) {
        return emailCommunicationCustomerAttachmentRepository
                .findAllActiveAttachmentsByEmailCommunicationCustomerId(emailCommunicationCustomerId)
                .stream()
                .map(attachment -> new AttachmentShortResponse(attachment.getId(), attachment.getName(), true))
                .toList();
    }

    /**
     * Retrieves a ShortResponse object representing an active report file associated with a specific email communication.
     *
     * @param emailCommunicationId The ID of the email communication for which to retrieve the active report file.
     * @return A ShortResponse object containing the ID and a shortened name of the active report file,
     * or null if no active report file is found.
     */
    private ShortResponse getMassEmailCommunicationReportFileShortResponse(Long emailCommunicationId) {
        return emailCommunicationFileRepository
                .findReportFileByEmailCommunicationId(emailCommunicationId, EntityStatus.ACTIVE)
                .map(emailCommunicationFile -> new ShortResponse(emailCommunicationFile.getId(), emailCommunicationFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME)))
                .orElse(null);
    }

    /**
     * Retrieves the customer email addresses associated with a given email communication ID.
     *
     * @param emailCommunicationId the ID of the email communication.
     * @return a semicolon-separated string of customer email addresses.
     */
    private String getCustomerEmailAddressesByEmailCommunicationId(Long emailCommunicationId) {
        return String.join(";", emailCommunicationCustomerContactRepository.findAllEmailAddressesByEmailCommunicationId(emailCommunicationId));
    }

    /**
     * Retrieves a semicolon-separated string of customer email addresses associated with the provided email communication customer ID.
     *
     * @param emailCommunicationCustomerId The ID of the email communication customer for whom email addresses are to be retrieved.
     * @return A semicolon-separated string containing all email addresses associated with the specified customer ID.
     */
    private String getCustomerEmailAddressesByEmailCommunicationCustomerId(Long emailCommunicationCustomerId) {
        return String.join(";", emailCommunicationCustomerContactRepository.findAllEmailAddressesByEmailCommunicationCustomerId(emailCommunicationCustomerId));
    }

    /**
     * Retrieves a short response containing the ID and display name of an active account manager
     * based on the given system user ID.
     * This method queries the account manager repository for an account manager with the provided
     * system user ID and an active status. If such an account manager is found, a {@link ShortResponse}
     * object is created and returned. If no matching account manager is found, a {@link DomainEntityNotFoundException}
     * is thrown with a message indicating the inability to find the employee.
     *
     * @param systemUserId the system user ID of the account manager to be retrieved
     * @return a {@link ShortResponse} object containing the ID and display name of the account manager
     * @throws DomainEntityNotFoundException if no active account manager is found with the given system user ID
     */
    private ShortResponse getCreatorEmployeeShortResponse(String systemUserId) {
        if (Objects.equals(systemUserId, "system")) {
            return new ShortResponse(null, "system");
        }

        return accountManagerRepository
                .findByUserName(systemUserId)
                .map(accountManager -> new ShortResponse(accountManager.getId(), accountManager.getDisplayName()))
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find employee with username %s;".formatted(systemUserId)));
    }

    /**
     * Retrieves sender employee details by their ID from the account manager repository.
     *
     * @param senderEmployeeId The ID of the sender employee to retrieve.
     * @return A ShortResponse object containing the ID and display name of the sender employee.
     * @throws DomainEntityNotFoundException If the sender employee with the given ID is not found in the repository.
     */
    public ShortResponse getSenderEmployeeShortResponse(Long senderEmployeeId) {
        if (senderEmployeeId == null) {
            return new ShortResponse(null, "system");
        }

        return accountManagerRepository
                .findById(senderEmployeeId)
                .map(accountManager -> new ShortResponse(accountManager.getId(), accountManager.getDisplayName()))
                .orElseThrow(() -> new DomainEntityNotFoundException("Sender employee not found! with id %s;".formatted(senderEmployeeId)));
    }

    /**
     * Retrieves a list of tasks associated with the specified email communication ID.
     * This method queries the {@code taskService} to obtain a list of {@link TaskShortResponse} objects
     * that are linked to the provided email communication ID. It returns all tasks related to the specified
     * email communication.
     *
     * @param id The ID of the email communication for which tasks are to be retrieved.
     *           This should be a valid identifier associated with the email communication in the system.
     * @return A {@link List} of {@link TaskShortResponse} objects representing the tasks associated
     * with the specified email communication ID. If no tasks are found, an empty list is returned.
     * @throws IllegalArgumentException if the provided ID is {@code null} or invalid.
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByEmailCommunicationId(id);
    }

    /**
     * Retrieves a list of system activities associated with the specified ID and communication channel type.
     * This method queries the appropriate service based on the {@code type} of email communication channel.
     * If the {@code type} is {@link EmailCommunicationChannelType#MASS_EMAIL}, it uses {@code massEmailCommunicationActivityService}
     * to obtain a list of {@link SystemActivityShortResponse} objects associated with the given ID.
     * For other types, it defaults to using {@code emailCommunicationActivityService}.
     *
     * @param id   The ID of the connected object for which activities are to be retrieved.
     *             This should be a valid identifier related to the system activities.
     * @param type The type of email communication channel that determines which service to use for retrieving activities.
     *             This must be one of the defined values in {@link EmailCommunicationChannelType}.
     * @return A {@link List} of {@link SystemActivityShortResponse} objects representing the activities
     * associated with the specified ID and channel type. If no activities are found, an empty list is returned.
     * @throws IllegalArgumentException if the provided ID is {@code null} or invalid,
     *                                  or if the {@code type} is {@code null}.
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id, EmailCommunicationChannelType type) {
        if (EmailCommunicationChannelType.MASS_EMAIL.equals(type)) {
            return massEmailCommunicationActivityService.getActivitiesByConnectedObjectId(id);
        } else {
            return emailCommunicationActivityService.getActivitiesByConnectedObjectId(id);
        }
    }

    /**
     * Checks the status of an email communication and validates the user's permission to view it based on the entity status.
     *
     * @param emailCommunicationStatus the status of the email communication (e.g., DRAFT, SENT)
     * @param entityStatus             the status of the entity (e.g., ACTIVE, DELETED)
     * @throws AccessDeniedException if the user does not have the necessary permissions to view the email communication
     */
    private void checkOnEmailCommunicationPermissionStatusView(
            EmailCommunicationStatus emailCommunicationStatus,
            EntityStatus entityStatus
    ) {
        switch (entityStatus) {
            case ACTIVE -> {
                switch (emailCommunicationStatus) {
                    case DRAFT -> {
                        if (!hasViewActiveDraftPermission()) {
                            log.error("You do not have permission to view draft email communication.");
                            throw new AccessDeniedException("You do not have permission to view draft email communication.");
                        }
                    }
                    case SENT, SENT_SUCCESSFULLY, SENT_FAILED, RECEIVED -> {
                        if (!hasViewActiveSendPermission()) {
                            log.error("You do not have permission to view send email communication.");
                            throw new AccessDeniedException("You do not have permission to view send email communication.");
                        }
                    }
                }
            }
            case DELETED -> {
                if (!hasViewDeletedPermission()) {
                    log.error("You do not have permission to view deleted email communication.");
                    throw new AccessDeniedException("You do not have permission to view deleted email communication.");
                }
            }
        }
    }

    /**
     * Checks permissions to view a mass email communication based on its status and entity status.
     * Depending on the current entity status (ACTIVE or DELETED) and the email communication status (DRAFT or SENT),
     * this method validates whether the user has the necessary permissions to view the communication.
     *
     * @param emailCommunicationStatus The status of the email communication (DRAFT or SENT).
     * @param entityStatus             The status of the entity (ACTIVE or DELETED).
     * @throws AccessDeniedException If the user does not have permission to view the specified type of mass email communication.
     */
    private void checkOnMassEmailCommunicationStatusView(
            EmailCommunicationStatus emailCommunicationStatus,
            EntityStatus entityStatus
    ) {
        switch (entityStatus) {
            case ACTIVE -> {
                switch (emailCommunicationStatus) {
                    case DRAFT -> {
                        if (!hasViewMassActiveDraftPermission()) {
                            log.error("You do not have permission to view draft mass email communication.");
                            throw new AccessDeniedException("You do not have permission to view draft mass email communication.");
                        }
                    }
                    case SENT, SENT_SUCCESSFULLY, SENT_FAILED, RECEIVED -> {
                        if (!hasViewMassActiveSendPermission()) {
                            log.error("You do not have permission to view send mass email communication.");
                            throw new AccessDeniedException("You do not have permission to view send mass email communication.");
                        }
                    }
                }
            }
            case DELETED -> {
                if (!hasViewMassDeletedPermission()) {
                    log.error("You do not have permission to view deleted mass email communication.");
                    throw new AccessDeniedException("You do not have permission to view deleted mass email communication.");
                }
            }
        }
    }

    /**
     * Lists email communications based on the provided request parameters.
     *
     * @param request the request parameters for filtering and paginating the email communications
     * @return a page of email communication listing responses
     */
    public Page<EmailCommunicationListingResponse> list(EmailCommunicationListingRequest request) {
        log.info("Calling Email Communication listing with request: {}", request);

        List<EntityStatus> statuses = getStatuses();
        List<EmailCommunicationStatus> communicationStatuses = getCommunicationStatuses(request);
        checkPermissionsForRequestedStatuses(request.getCommunicationStatus());

        Optional<List<EmailCommunicationChannelType>> communicationChannelTypesOpt = getCommunicationChannelTypes(request);

        if (communicationChannelTypesOpt.isEmpty())
            return Page.empty(PageRequest.of(request.getPage(), request.getSize()));

        List<EmailCommunicationChannelType> communicationChannelTypes = communicationChannelTypesOpt.get();

        return emailCommunicationRepository
                .filter(
                        ListUtils.emptyIfNull(request.getCreatorEmployeeId()),
                        ListUtils.emptyIfNull(request.getSenderEmployeeId()),
                        request.getCreateDateFrom(),
                        request.getCreateDateTo(),
                        ListUtils.emptyIfNull(request.getContactPurposeId()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getCommunicationType()),
                        ListUtils.emptyIfNull(request.getActivityId()),
                        ListUtils.emptyIfNull(request.getTaskId()),
                        ListUtils.emptyIfNull(request.getCommunicationTopicId()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(communicationChannelTypes),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(communicationStatuses),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        Objects.requireNonNullElse(request.getSearchBy(), EmailCommunicationSearchFields.ALL).name(),
                        Objects.requireNonNullElse(request.getActivityDirection(), Sort.Direction.ASC).name(),
                        Objects.requireNonNullElse(request.getContactPurposeDirection(), Sort.Direction.ASC).name(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getSortDirection(), getSortByEnum(request.getSortColumn()))
                                )
                        )
                )
                .map(EmailCommunicationListingResponse::new);
    }

    /**
     * Retrieves the template for the specified mass email communication import type.
     *
     * @param massEmailCommunicationImportType the mass email communication import type for which to retrieve the template
     * @return the byte array representing the template file
     * @throws DomainEntityNotFoundException if the template path cannot be found
     * @throws ClientException               if there is an error fetching the template
     */
    public byte[] getTemplate(MassEmailCommunicationImportType massEmailCommunicationImportType) {
        try {
            var templatePath = templateRepository.findById(massEmailCommunicationImportType.name()).orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path"));
            log.info("template path ->>>> :" + templatePath.getFileUrl());
            return fileService.downloadFile(templatePath.getFileUrl()).getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch %s template".formatted(massEmailCommunicationImportType.name()), exception);
            throw new ClientException("Could not fetch %s template".formatted(massEmailCommunicationImportType.name()), APPLICATION_ERROR);
        }
    }

    /**
     * Uploads a file and processes the data based on the provided `MassEmailCommunicationImportType`.
     *
     * @param file           The file to be uploaded.
     * @param massImportType The type of mass email communication import.
     * @return A list of `MassEmailCommunicationParsedFile` objects representing the processed data.
     * @throws DomainEntityNotFoundException     If the template for the specified `MassEmailCommunicationImportType` is not found.
     * @throws IllegalArgumentsProvidedException If illegal arguments are provided in the file.
     * @throws ClientException                   If an exception occurs while trying to parse the uploaded template.
     */
    public MassCommunicationImportProcessResult upload(
            MultipartFile file,
            MassEmailCommunicationImportType massImportType
    ) {
        EPBExcelUtils.validateFileFormat(file);

        Template template = templateRepository
                .findById(massImportType.name())
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for %s not found;".formatted(massImportType.name())));

        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(template.getFileUrl()).getByteArray(), 1);

        List<MassCommunicationFileProcessedResult> temp = new ArrayList<>();
        List<MassCommunicationFileProcessedResult> result = Collections.synchronizedList(temp);
        StringBuffer errors = new StringBuffer();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Row> rows = new ArrayList<>();
            Iterator<Row> iterator = sheet.iterator();
            if (iterator.hasNext()) iterator.next(); // Skip header row

            while (iterator.hasNext()) {
                rows.add(iterator.next());
            }

            int BATCH_SIZE = rows.size() / THREAD_SIZE + 1;

            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_SIZE);
            List<Callable<Void>> callables = new ArrayList<>();

            List<List<Row>> partitions = ListUtils.partition(rows, BATCH_SIZE);

            for (List<Row> batch : partitions) {
                callables.add(() -> {
                                  for (Row row : batch) {
                                      if (MassEmailCommunicationImportType.MASS_IMPORT_OF_CUSTOMERS.equals(massImportType)) {
                                          processCustomerImport(row, result, errors);
                                      } else if (MassEmailCommunicationImportType.MASS_IMPORT_OF_CONTRACTS.equals(massImportType)) {
                                          processContractImport(row, result, errors);
                                      }
                                  }
                                  return null;
                              }
                );
            }

            executorService.invokeAll(callables);
            executorService.shutdown();

        } catch (IllegalArgumentsProvidedException e) {
            log.error("Illegal arguments provided in file", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }

        return new MassCommunicationImportProcessResult(new HashSet<>(result), errors.toString().isBlank() ? null : errors.toString());
    }

    /**
     * Processes a row from a mass import of product contracts, adding the customer identifier and version ID to the result list if valid contract information is found, or adding an error message to the error messages list if the contract number is null.
     *
     * @param row    The row from the mass import file to process.
     * @param result The list to add the parsed customer identifier and version ID to.
     */
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

    public void processContractInfoVersionId(
            String contractNumber,
            Long versionId,
            List<MassCommunicationFileProcessedResult> result,
            StringBuffer errors
    ) {
        if (Objects.nonNull(versionId)) {
            boolean found = false;
            MassCommunicationFileProcessedResultProjection serviceContractProjection = serviceContractsRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(
                    contractNumber,
                    versionId
            );
            if (Objects.nonNull(serviceContractProjection)) {
                result.add(new MassCommunicationFileProcessedResult(serviceContractProjection));
                found = true;
            } else {
                MassCommunicationFileProcessedResultProjection productContractProjection = productContractRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(
                        contractNumber,
                        versionId
                );
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

    public void processContractInfoByLatestVersionId(
            String contractNumber,
            List<MassCommunicationFileProcessedResult> result,
            StringBuffer errors
    ) {
        if (Objects.nonNull(contractNumber)) {
            boolean found = false;
            Long productContractLatestVersionId = productContractRepository.findLatestProductContractDetailVersionId(contractNumber);
            if (Objects.nonNull(productContractLatestVersionId)) {
                MassCommunicationFileProcessedResultProjection productContractProjection = productContractRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(
                        contractNumber,
                        productContractLatestVersionId
                );
                if (Objects.nonNull(productContractProjection)) {
                    result.add(new MassCommunicationFileProcessedResult(productContractProjection));
                    found = true;
                }
            } else {
                Long serviceContractLatestVersionId = serviceContractsRepository.findLatestServiceContractDetailVersionId(contractNumber);
                if (Objects.nonNull(serviceContractLatestVersionId)) {
                    MassCommunicationFileProcessedResultProjection serviceContractProjection = serviceContractsRepository.findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(
                            contractNumber,
                            serviceContractLatestVersionId
                    );
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

    /**
     * Processes a customer import row from an Excel file, extracting the customer identifier and version ID.
     * If the version ID is not provided in the row, it attempts to find the last active version ID for the customer.
     * The processed information is added to the `result` list, or an error message is added to the `errorRows` list if the customer identifier is null.
     *
     * @param row    the Excel row containing the customer import data
     * @param result the list to add the processed customer information to
     */
    private void processCustomerImport(
            Row row,
            List<MassCommunicationFileProcessedResult> result,
            StringBuffer errors
    ) {
        String customerIdentifier = EPBExcelUtils.getStringValue(0, row);
        if (Objects.nonNull(customerIdentifier)) {
            String versionId = EPBExcelUtils.getStringValue(1, row);
            if (Objects.nonNull(versionId)) {
                result.add(
                        new MassCommunicationFileProcessedResult(
                                customerIdentifier,
                                Long.parseLong(versionId),
                                null,
                                null
                        )
                );
            } else {
                Long versionIdByIdentifierAndStatus = customerRepository.findLastVersionIdByIdentifierAndStatus(customerIdentifier, CustomerStatus.ACTIVE);
                if (Objects.nonNull(versionIdByIdentifierAndStatus)) {
                    result.add(
                            new MassCommunicationFileProcessedResult(
                                    customerIdentifier,
                                    versionIdByIdentifierAndStatus,
                                    null,
                                    null
                            )
                    );
                } else {
                    errors.append(String.format("Customer with identifier %s Does not exists!;", customerIdentifier));
                }
            }
        }
    }

    /**
     * Downloads a report file associated with the given EmailCommunication ID.
     * Retrieves the active EmailCommunicationFile entity corresponding to the emailCommunicationId,
     * downloads the file content using a file service, and returns it wrapped in a ProxyFileContent object.
     *
     * @param emailCommunicationId The ID of the EmailCommunication entity for which to download the report.
     * @return A ProxyFileContent object containing the name and byte content of the downloaded report file.
     * @throws DomainEntityNotFoundException If the report file associated with the emailCommunicationId is not found or not active.
     */
    public FileContent downloadReport(Long emailCommunicationId) {
        EmailCommunicationFile emailCommunicationFile = emailCommunicationFileRepository
                .findReportFileByEmailCommunicationId(emailCommunicationId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Report not found!"));

        ByteArrayResource content = fileService.downloadFile(emailCommunicationFile.getLocalFileUrl());
        return new FileContent(emailCommunicationFile.getName(), content.getByteArray());
    }

    /**
     * Downloads a proxy file based on the given ID.
     * Retrieves the active EmailCommunicationFile entity corresponding to the provided ID,
     * downloads the file content using a file service, and returns it wrapped in a ProxyFileContent object.
     *
     * @param id The ID of the proxy file to download.
     * @return A ProxyFileContent object containing the name and byte content of the downloaded proxy file.
     * @throws DomainEntityNotFoundException If the proxy file with the given ID is not found or is not active.
     */
    public FileContent downloadProxyFile(Long id) {
        EmailCommunicationFile emailCommunicationFile = emailCommunicationFileRepository
                .findCommunicationFileByIdAndStatus(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active proxy file with ID %s not found;".formatted(id)));

        ByteArrayResource content = fileService.downloadFile(emailCommunicationFile.getLocalFileUrl());
        return new FileContent(emailCommunicationFile.getName(), content.getByteArray());
    }

    public FileContent checkForArchivationAndDownload(Long id) throws Exception {
        EmailCommunicationFile emailCommunicationFile = emailCommunicationFileRepository
                .findCommunicationFileByIdAndStatus(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active proxy file with ID %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(emailCommunicationFile.getIsArchived())) {
            if (Objects.isNull(emailCommunicationFile.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(emailCommunicationFile.getDocumentId(), emailCommunicationFile.getFileId());

                return new FileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
            }
        }

        ByteArrayResource content = fileService.downloadFile(emailCommunicationFile.getLocalFileUrl());
        return new FileContent(emailCommunicationFile.getName(), content.getByteArray());
    }

    /**
     * Downloads a attachment file based on the given ID.
     * Retrieves the active EmailCommunicationAttachment entity corresponding to the provided ID,
     * downloads the file content using a file service, and returns it wrapped in a ProxyFileContent object.
     *
     * @param id The ID of the proxy file to download.
     * @return A ProxyFileContent object containing the name and byte content of the downloaded proxy file.
     * @throws DomainEntityNotFoundException If the proxy file with the given ID is not found or is not active.
     */
    public FileContent downloadAttachmentFile(Long id, Boolean singleFromMass) {
        String fileName;
        String fileUrl;
        if (singleFromMass) {
            EmailCommunicationCustomerAttachment emailCommunicationCustomerAttachment = emailCommunicationCustomerAttachmentRepository
                    .findByIdAndStatus(id, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Active customer attachment file with ID %s not found;".formatted(id)));

            fileName = emailCommunicationCustomerAttachment.getName();
            fileUrl = emailCommunicationCustomerAttachment.getFileUrl();
        } else {
            EmailCommunicationAttachment emailCommunicationAttachment = emailCommunicationAttachmentRepository
                    .findByIdAndStatus(id, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Active attachment file with ID %s not found;".formatted(id)));

            fileName = emailCommunicationAttachment.getName();
            fileUrl = emailCommunicationAttachment.getFileUrl();
        }

        ByteArrayResource content = fileService.downloadFile(fileUrl);
        return new FileContent(fileName, content.getByteArray());
    }

    /**
     * Retrieves the list of entity statuses that the current user has permission to view.
     * If the user has permission to view active or draft entities, the 'ACTIVE' status is included.
     * If the user has permission to view deleted entities, the 'DELETED' status is included.
     *
     * @return the list of entity statuses the user can view
     */
    private List<EntityStatus> getStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (hasViewActiveSendPermission() || hasViewActiveDraftPermission()) {
            statuses.add(EntityStatus.ACTIVE);
        }
        if (hasViewDeletedPermission()) {
            statuses.add(EntityStatus.DELETED);
        }
        return statuses;
    }

    /**
     * Retrieves the list of email communication statuses based on the provided request.
     * If the request does not specify any communication statuses, the method will add the appropriate statuses based on the user's permissions.
     *
     * @param request the email communication listing request
     * @return the list of email communication statuses
     */
    private List<EmailCommunicationStatus> getCommunicationStatuses(EmailCommunicationListingRequest request) {
        List<EmailCommunicationStatus> communicationStatuses = request.getCommunicationStatus();

        if (communicationStatuses == null || communicationStatuses.isEmpty()) {
            communicationStatuses = new ArrayList<>();
            if (hasViewActiveDraftPermission()) {
                communicationStatuses.add(DRAFT);
            }
            if (hasViewActiveSendPermission()) {
                communicationStatuses.add(SENT);
                communicationStatuses.add(SENT_SUCCESSFULLY);
                communicationStatuses.add(SENT_FAILED);
                communicationStatuses.add(RECEIVED);
            }
        }

        return communicationStatuses;
    }

    private Optional<List<EmailCommunicationChannelType>> getCommunicationChannelTypes(EmailCommunicationListingRequest request) {
        List<EmailCommunicationChannelType> requestedTypes = request.getKindOfCommunication();

        if (requestedTypes == null || requestedTypes.isEmpty()) {
            List<EmailCommunicationChannelType> allowedTypes = getAllowedChannelTypes();
            return Optional.of(allowedTypes);
        } else {
            List<EmailCommunicationChannelType> allowedTypes = requestedTypes.stream()
                    .filter(this::isAllowed)
                    .collect(Collectors.toList());
            return allowedTypes.isEmpty() ? Optional.empty() : Optional.of(allowedTypes);
        }
    }

    private List<EmailCommunicationChannelType> getAllowedChannelTypes() {
        List<EmailCommunicationChannelType> allowedTypes = new ArrayList<>(2);
        if (hasEmailViewPermissions()) {
            allowedTypes.add(EmailCommunicationChannelType.EMAIL);
        }
        if (hasMassEmailViewPermissions()) {
            allowedTypes.add(EmailCommunicationChannelType.MASS_EMAIL);
        }
        return allowedTypes;
    }

    private boolean isAllowed(EmailCommunicationChannelType type) {
        return switch (type) {
            case EMAIL -> hasEmailViewPermissions();
            case MASS_EMAIL -> hasMassEmailViewPermissions();
        };
    }

    private boolean hasEmailViewPermissions() {
        return hasViewActiveSendPermission() || hasViewActiveDraftPermission() || hasViewDeletedPermission();
    }

    private boolean hasMassEmailViewPermissions() {
        return hasViewMassActiveDraftPermission() || hasViewMassActiveSendPermission() || hasViewMassDeletedPermission();
    }

    /**
     * Checks the user's permissions for the requested email communication statuses.
     * If the user does not have the necessary permissions, a `ClientException` is thrown with the appropriate error message and code.
     *
     * @param requestedStatuses the list of email communication statuses to check permissions for
     * @throws ClientException if the user does not have the necessary permissions to view the requested statuses
     */
    private void checkPermissionsForRequestedStatuses(List<EmailCommunicationStatus> requestedStatuses) {
        if (requestedStatuses != null) {
            for (EmailCommunicationStatus status : requestedStatuses) {
                switch (status) {
                    case DRAFT:
                        if (!hasViewActiveDraftPermission()) {
                            throw new ClientException("You do not have permission to view draft communications;", ErrorCode.ACCESS_DENIED);
                        }
                        break;
                    case SENT:
                    case SENT_SUCCESSFULLY:
                    case SENT_FAILED:
                    case RECEIVED:
                        if (!hasViewActiveSendPermission()) {
                            throw new ClientException("You do not have permission to view sent or received communications;", ErrorCode.ACCESS_DENIED);
                        }
                        break;
                }
            }
        }
    }

    /**
     * Validates if the resend action is allowed based on the email communication status and entity status.
     *
     * @param emailCommunicationStatus the status of the email communication to check.
     * @param entityStatus             the status of the entity to check.
     * @throws OperationNotAllowedException if the resend action is not allowed for the given statuses.
     */
    private void validateResendActionByStatuses(
            EmailCommunicationStatus emailCommunicationStatus,
            EntityStatus entityStatus
    ) {
        switch (entityStatus) {
            case ACTIVE -> {
                if (Objects.requireNonNull(emailCommunicationStatus) == EmailCommunicationStatus.DRAFT) {
                    log.error("You cannot resend draft email communication.");
                    throw new OperationNotAllowedException("You cannot resend draft email communication.");
                }
            }
            case DELETED -> {
                log.error("You cannot resend deleted email communication.");
                throw new OperationNotAllowedException("You cannot resend deleted email communication.");
            }
        }
    }

    /**
     * Validates if the resend action is allowed for the given communication channel type.
     *
     * @param communicationChannelType the type of communication channel to check.
     * @throws OperationNotAllowedException if the communication channel type is MASS_EMAIL.
     */
    private void validateResendByCommunicationChannel(EmailCommunicationChannelType communicationChannelType) {
        if (EmailCommunicationChannelType.MASS_EMAIL.equals(communicationChannelType)) {
            throw new OperationNotAllowedException("You cannot resend email communication with channel type MASS_EMAIL.");
        }
    }

    /**
     * Validates the communication type for resending an email.
     *
     * @param emailCommunicationType the type of email communication to validate
     * @throws OperationNotAllowedException if the communication type is INCOMING,
     *                                      as resending is not allowed for incoming emails.
     */
    private void validateResendByCommunicationType(EmailCommunicationType emailCommunicationType) {
        if (EmailCommunicationType.INCOMING.equals(emailCommunicationType)) {
            throw new OperationNotAllowedException("You cannot resend email communication with type INCOMING.");
        }
    }

    /**
     * Validates that an email communication type update is allowed.
     * This method checks if the current email communication type is the same
     * as the requested type. If they are different, an exception is thrown.
     *
     * @param emailCommunicationType          the current email communication type
     * @param requestedEmailCommunicationType the requested email communication type to be updated to
     * @throws OperationNotAllowedException if the current type is not equal to the requested type,
     *                                      indicating that the update operation is not permitted.
     */
    private void validateUpdateByCommunicationType(
            EmailCommunicationType emailCommunicationType,
            EmailCommunicationType requestedEmailCommunicationType
    ) {
        if (!Objects.equals(emailCommunicationType, requestedEmailCommunicationType)) {
            throw new OperationNotAllowedException(
                    "You cannot change email communication %s type to %s.".formatted(emailCommunicationType, requestedEmailCommunicationType)
            );
        }
    }

    /**
     * Validates whether the resend operation is allowed based on the send date.
     *
     * @param sendDate The LocalDateTime representing the date when the email was originally sent.
     * @throws OperationNotAllowedException If the resend operation is not allowed because 30 days have passed since the send date.
     */
    private void validateResendBySendDate(LocalDateTime sendDate) {
        if (!isResendActive(sendDate)) {
            throw new OperationNotAllowedException("Cannot resend email: 30 days have passed since the send date.");
        }
    }

    /**
     * Retrieves an email communication entity by its ID.
     * This method attempts to find and return the {@link EmailCommunication} entity
     * associated with the provided {@code id} from the repository.
     * If no entity with the given ID is found, a {@link DomainEntityNotFoundException}
     * is thrown with an appropriate error message indicating the ID that was not found.
     *
     * @param id The ID of the email communication entity to retrieve.
     * @return The {@link EmailCommunication} entity associated with the provided ID.
     * @throws DomainEntityNotFoundException If no email communication entity is found with the given ID.
     */
    private EmailCommunication findEmailCommunicationById(Long id) {
        return emailCommunicationRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find email communication with id: %s;".formatted(id)));
    }

    /**
     * Retrieves an {@link EmailCommunication} object based on the customer ID.
     *
     * @param customerId The ID of the customer associated with the email communication.
     * @return The {@link EmailCommunication} object corresponding to the provided customer ID.
     * @throws DomainEntityNotFoundException if no email communication object is found for the given customer ID.
     */
    private EmailCommunication findEmailCommunicationByCustomerId(Long customerId) {
        return emailCommunicationRepository
                .findByEmailCommunicationCustomerId(customerId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find email communication object with email communication customer id: %s;".formatted(customerId)));
    }

    private EmailCommunicationCustomer findEmailCommunicationCustomerById(Long emailCommunicationCustomerId) {
        return emailCommunicationCustomerRepository
                .findById(emailCommunicationCustomerId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find email communication customer object with email communication customer id: %s;".formatted(emailCommunicationCustomerId)));
    }

    /**
     * Validates and updates the email box ID associated with the email communication.
     * This method checks if the provided {@code emailBoxId} is not null and differs from the current
     * email box ID of the {@code emailCommunication}. If so, it attempts to find an active {@link EmailMailboxes}
     * entity with the matching ID and updates the {@code emailCommunication} with the found ID.
     * If no active {@link EmailMailboxes} entity is found with the provided ID, an error message
     * is added to {@code errorMessages} indicating the failure to find the active email box.
     *
     * @param emailBoxId         The ID of the email box to associate with the email communication, or null to skip updating.
     * @param emailCommunication The {@link EmailCommunication} entity to update with the new email box ID.
     * @param errorMessages      A list to collect error messages encountered during validation and updating of the email box ID.
     */
    private void validateAndUpdateAndSetEmailBox(
            Long emailBoxId,
            EmailCommunication emailCommunication,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(emailBoxId) && !Objects.equals(emailCommunication.getEmailBoxId(), emailBoxId)) {
            Optional<EmailMailboxes> emailMailboxOptional = emailMailboxesRepository.findByIdAndStatuses(emailBoxId, List.of(NomenclatureItemStatus.ACTIVE));
            if (emailMailboxOptional.isPresent()) {
                emailCommunication.setEmailBoxId(emailMailboxOptional.get().getId());
            } else {
                errorMessages.add("emailBoxId-[emailBoxId] active email box with: %s can't be found;".formatted(emailBoxId));
            }
        }
    }

    /**
     * Validates and updates the topic of communication ID associated with the email communication.
     * This method checks if the provided {@code topicOfCommunicationId} is not null and differs from the current
     * topic of communication ID of the {@code emailCommunication}. If so, it attempts to find an active {@link TopicOfCommunication}
     * entity with the matching ID and updates the {@code emailCommunication} with the found ID.
     * If no active {@link TopicOfCommunication} entity is found with the provided ID, an error message
     * is added to {@code errorMessages} indicating the failure to find the active topic of communication.
     *
     * @param topicOfCommunicationId The ID of the topic of communication to associate with the email communication, or null to skip updating.
     * @param emailCommunication     The {@link EmailCommunication} entity to update with the new topic of communication ID.
     * @param errorMessages          A list to collect error messages encountered during validation and updating of the topic of communication ID.
     */
    private void validateAndUpdateTopicOfCommunication(
            Long topicOfCommunicationId,
            EmailCommunication emailCommunication,
            List<String> errorMessages
    ) {
        if (Objects.nonNull(topicOfCommunicationId) && !Objects.equals(emailCommunication.getCommunicationTopicId(), topicOfCommunicationId)) {
            Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByIdAndStatus(topicOfCommunicationId, NomenclatureItemStatus.ACTIVE);
            if (topicOfCommunicationOptional.isPresent()) {
                emailCommunication.setCommunicationTopicId(topicOfCommunicationOptional.get().getId());
            } else {
                errorMessages.add("communicationTopicId-[communicationTopicId] active topic of communication with: %s can't be found;".formatted(topicOfCommunicationId));
            }
        }
    }

    /**
     * Validates whether an update action can be performed based on the status of the email communication.
     * This method checks the {@link EmailCommunication}'s {@link EntityStatus} and performs the following validations:
     * If the status is {@link EntityStatus#ACTIVE}:
     * Throws {@link OperationNotAllowedException} if the {@link EmailCommunicationStatus} is {@link EmailCommunicationStatus#SENT}.
     * Throws {@link OperationNotAllowedException} if the {@link EmailCommunicationChannelType} is {@link EmailCommunicationChannelType#MASS_EMAIL}.
     * If the status is {@link EntityStatus#DELETED},
     * throws {@link OperationNotAllowedException} indicating that updates are not allowed for deleted email communications.
     *
     * @param emailCommunication The {@link EmailCommunication} entity to validate update actions for.
     * @throws OperationNotAllowedException If the update action violates the business rules based on the email communication's status.
     */
    private void validateUpdateActionByStatuses(EmailCommunication emailCommunication) {
        switch (Objects.requireNonNull(emailCommunication.getEntityStatus())) {
            case ACTIVE -> {
                if (Objects.requireNonNull(emailCommunication.getEmailCommunicationType()) == EmailCommunicationType.OUTGOING) {
                    if (Objects.requireNonNull(emailCommunication.getEmailCommunicationStatus()) != DRAFT) {
                        log.error("You cannot update send email communication.");
                        throw new OperationNotAllowedException("You cannot update send email communication.");
                    }
                }

                if (Objects.requireNonNull(emailCommunication.getCommunicationChannel()) == EmailCommunicationChannelType.MASS_EMAIL) {
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
     * Validates whether an update action can be performed based on the status of the mass email communication.
     * This method checks the {@link EmailCommunication}'s {@link EntityStatus} and performs the following validations:
     * If the status is {@link EntityStatus#ACTIVE}:
     * Throws {@link OperationNotAllowedException} if the {@link EmailCommunicationStatus} is {@link EmailCommunicationStatus#SENT}.
     * Throws {@link OperationNotAllowedException} if the {@link EmailCommunicationChannelType} is {@link EmailCommunicationChannelType#EMAIL}.
     * If the status is {@link EntityStatus#DELETED},
     * throws {@link OperationNotAllowedException} indicating that updates are not allowed for deleted mass email communications.
     *
     * @param emailCommunication The {@link EmailCommunication} entity to validate update actions for.
     * @throws OperationNotAllowedException If the update action violates the business rules based on the mass email communication's status.
     */
    private void validateMassEmailUpdateActionByStatuses(EmailCommunication emailCommunication) {
        switch (emailCommunication.getEntityStatus()) {
            case ACTIVE -> {
                if (Objects.requireNonNull(emailCommunication.getEmailCommunicationStatus()) != EmailCommunicationStatus.DRAFT) {
                    log.error("You cannot update send mass email communication.");
                    throw new OperationNotAllowedException("You cannot update send mass email communication.");
                }
                if (Objects.requireNonNull(emailCommunication.getCommunicationChannel()) == EmailCommunicationChannelType.EMAIL) {
                    log.error("You cannot update email communication.");
                    throw new OperationNotAllowedException("You cannot update email communication.");
                }
            }
            case DELETED -> {
                log.error("You cannot update deleted mass email communication.");
                throw new OperationNotAllowedException("You cannot update deleted mass email communication.");
            }
        }
    }

    /**
     * Validates whether an {@link EmailCommunication} entity can be deleted based on its current status.
     * This method performs the following checks:
     * If the {@code emailCommunication} has an {@link EntityStatus#DELETED} status, indicating that it is already deleted,
     * an {@link OperationNotAllowedException} is thrown.
     * If the {@code emailCommunication} does not have an {@link EmailCommunicationStatus#DRAFT} status, meaning it is not in a
     * draft state, an {@link OperationNotAllowedException} is thrown.
     *
     * @param emailCommunication the {@link EmailCommunication} entity to validate for deletion.
     * @throws OperationNotAllowedException if the {@code emailCommunication} is already deleted or if its status is not {@link EmailCommunicationStatus#DRAFT}.
     * @see EmailCommunication
     * @see EntityStatus
     * @see EmailCommunicationStatus
     * @see OperationNotAllowedException
     */
    private void validateDeleteActionByStatuses(EmailCommunication emailCommunication) {
        if (emailCommunication.getEntityStatus().equals(EntityStatus.DELETED)) {
            log.error("Email communication object with id: {} is already deleted", emailCommunication.getId());
            throw new OperationNotAllowedException("Email communication object is already deleted;");
        }

        if (emailCommunication.getEmailCommunicationType() == EmailCommunicationType.OUTGOING) {
            if (!EmailCommunicationStatus.DRAFT.equals(emailCommunication.getEmailCommunicationStatus())) {
                log.error("You cannot delete communication object with status: {}", emailCommunication.getEmailCommunicationStatus());
                throw new OperationNotAllowedException("You cannot delete communication object with status; ".concat(emailCommunication.getEmailCommunicationStatus().toString()));
            }
        }
    }

    /**
     * Splits a string containing email addresses separated by ';' into a set of individual email addresses.
     * This method takes a string {@code emailAddress} and splits it into multiple email addresses if it contains ';'.
     * Each email address is trimmed to remove leading and trailing whitespace before being added to the resulting set.
     * If the string does not contain ';', it assumes there is only one email address and returns a set containing that address.
     *
     * @param emailAddress the string containing email addresses separated by ';'
     * @return a set of individual email addresses extracted from {@code emailAddress}
     */
    private Set<String> splitEmail(String emailAddress) {
        if (emailAddress.contains(";")) {
            return Arrays
                    .stream(emailAddress.split(";"))
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toSet());
        } else {
            return Set.of(emailAddress);
        }
    }

    /**
     * Checks whether the customer data associated with an email communication has changed.
     * This method compares the customer detail ID and customer communication ID of the existing
     * email communication customer entity with the corresponding IDs provided in the request.
     * If either of these IDs are different, the method returns {@code true}, indicating that the
     * customer data has changed.
     *
     * @param emailCommCustomerEntity The existing email communication customer entity containing the current customer data.
     * @param request                 The request object containing the new customer data to be compared against the existing data.
     * @return {@code true} if the customer data has changed (i.e., if the customer detail ID or customer communication ID
     * in the request is different from the existing entity); {@code false} otherwise.
     */
    private boolean isCustomerDataChanged(
            EmailCommunicationCustomer emailCommCustomerEntity,
            EmailCommunicationEditRequest request
    ) {
        return !(
                Objects.equals(emailCommCustomerEntity.getCustomerDetailId(), request.getCustomerDetailId())
                        &&
                        Objects.equals(emailCommCustomerEntity.getCustomerCommunicationId(), request.getCustomerCommunicationId())
        );
    }

    private CustomerDetails getActiveCustomerDetailsOrElseNull(Long customerDetailsId) {
        return customerDetailsRepository
                .findById(customerDetailsId)
                .orElse(null);
    }

    private CustomerCommunications getActiveCustomerCommunicationOrElseNull(
            Long customerCommunicationId,
            Long customerDetailsId
    ) {
        return customerCommunicationsRepository
                .findByIdAndCustomerDetailsIdAndStatus(customerCommunicationId, customerDetailsId, Status.ACTIVE)
                .orElse(null);
    }

    private CustomerCommunicationContacts getActiveCustomerCommunicationEmailContactOrElseNull(Long customerCommunicationId) {
        return customerCommunicationContactsRepository
                .findByCustomerCommIdContactTypesAndStatuses(customerCommunicationId, List.of(CustomerCommContactTypes.EMAIL), List.of(Status.ACTIVE))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<CustomerCommunicationContacts> getActiveCustomerCommunicationEmailContacts(Long customerCommunicationId) {
        return customerCommunicationContactsRepository
                .findByCustomerCommIdContactTypesAndStatuses(customerCommunicationId, List.of(CustomerCommContactTypes.EMAIL), List.of(Status.ACTIVE));
    }

    private EmailCommunicationCustomer getActiveEmailCommunicationCustomerOrElseNull(Long emailCommunicationId) {
        return emailCommunicationCustomerRepository
                .findByEmailCommunicationId(emailCommunicationId)
                .orElse(null);
    }

    private void deleteOldCustomersByEmailCommunicationId(Long emailCommunicationId) {
        emailCommunicationCustomerRepository.deleteAllByEmailCommunicationId(emailCommunicationId);
    }

    /**
     * Generates a unique file name based on the original file name.
     * his method creates a unique file name by concatenating a UUID and the original file name
     * without any whitespace.
     *
     * @param originalFileName The original file name to generate a unique name for.
     * @return A unique file name combining a UUID and the original file name without whitespace.
     */
    private String getFileName(String originalFileName) {
        return String.format("%s_%s", UUID.randomUUID(), originalFileName.replaceAll("\\s+", ""));
    }

    /**
     * Constructs and returns the remote path for storing files on an FTP server.
     *
     * @return A string representing the remote path in the format "{@code ftpBasePath}/{@code FOLDER_PATH}/{@code LocalDate.now()}".
     * This path is used to determine the destination directory for uploading files.
     */
    private String remotePath() {
        return String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
    }

    /**
     * Constructs and returns the remote path for storing attachments on an FTP server.
     *
     * @return A string representing the remote path in the format "{@code ftpBasePath}/{@code ATTACHMENT_FOLDER_PATH}/{@code LocalDate.now()}".
     * This path is used to determine the destination directory for uploading attachments.
     */
    private String remoteAttachmentPath() {
        return String.format("%s/%s/%s", ftpBasePath, ATTACHMENT_FOLDER_PATH, LocalDate.now());
    }

    /**
     * Checks if the size of the provided MultipartFile exceeds the maximum allowed size for communication files.
     *
     * @param file The MultipartFile to check the size of.
     * @return {@code true} if the file size exceeds the maximum allowed size, otherwise {@code false}.
     */
    private boolean checkCommunicationFileSize(MultipartFile file) {
        return (file.getSize() > MAX_SIZE_COMMUNICATION_FILES);
    }

    /**
     * Determines if the resend feature is active based on the send date.
     * Checks if the difference in days between the given send date and the current date
     * is within 30 days to determine the activation status of the resend feature.
     *
     * @param sendDate The LocalDateTime representing the date when the message was last sent.
     * @return {@code true} if the resend feature is active (send date is within the last 30 days), otherwise {@code false}.
     */
    private Boolean isResendActive(LocalDateTime sendDate) {
        boolean resendActive = false;
        if (Objects.nonNull(sendDate)) {
            LocalDateTime currentDate = LocalDateTime.now();
            long daysBetween = ChronoUnit.DAYS.between(sendDate, currentDate);
            resendActive = (daysBetween <= 30);
        }
        return resendActive;
    }

    private boolean checkAttachmentFileSize(MultipartFile file) {
        return (file.getSize() > MAX_SIZE_ATTACHMENT_FILES);
    }

    private boolean hasCreateDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.EMAIL_COMMUNICATION_CREATE_DRAFT));
    }

    private boolean hasCreateAndSendPermission() {
        return checkOnPermission(List.of(PermissionEnum.EMAIL_COMMUNICATION_CREATE_AND_SEND));
    }

    private boolean hasViewActiveDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.EMAIL_COMMUNICATION_VIEW_DRAFT));
    }

    private boolean hasViewActiveSendPermission() {
        return checkOnPermission(List.of(PermissionEnum.EMAIL_COMMUNICATION_VIEW_SEND));
    }

    private boolean hasViewDeletedPermission() {
        return checkOnPermission(List.of(PermissionEnum.EMAIL_COMMUNICATION_VIEW_DELETED));
    }

    private boolean hasViewMassDeletedPermission() {
        return checkOnMassEmailPermission(List.of(PermissionEnum.MASS_EMAIL_COMMUNICATION_VIEW_DELETED));
    }

    private boolean hasViewMassActiveSendPermission() {
        return checkOnMassEmailPermission(List.of(PermissionEnum.MASS_EMAIL_COMMUNICATION_VIEW_SEND));
    }

    private boolean hasViewMassActiveDraftPermission() {
        return checkOnMassEmailPermission(List.of(PermissionEnum.MASS_EMAIL_COMMUNICATION_VIEW_DRAFT));
    }

    private boolean hasCreateDraftMassEmailPermission() {
        return checkOnMassEmailPermission(List.of(PermissionEnum.MASS_EMAIL_COMMUNICATION_CREATE_DRAFT));
    }

    private boolean hasCreateAndSendMassEmailPermission() {
        return checkOnMassEmailPermission(List.of(PermissionEnum.MASS_EMAIL_COMMUNICATION_CREATE_AND_SEND));
    }

    private boolean checkOnPermission(List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(EMAIL_COMMUNICATION, requiredPermissions);
    }

    private boolean checkOnMassEmailPermission(List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(MASS_EMAIL_COMMUNICATION, requiredPermissions);
    }

    /**
     * Validates the status of an {@link EmailCommunication} entity and sends it if its status is {@link EmailCommunicationStatus#SENT}.
     * This method checks if the {@code emailCommunication} has a status of {@link EmailCommunicationStatus#SENT}.
     * If it does, the method triggers the sending process by calling the {@link EmailCommunicationSenderService#sendSingle(Long)}
     * method with the ID of the email communication.
     *
     * @param emailCommunication the {@link EmailCommunication} entity to validate and potentially send.
     * @see EmailCommunication
     * @see EmailCommunicationStatus
     * @see EmailCommunicationSenderService#sendSingle(Long)
     */
    private void validateStatusAndSend(EmailCommunication emailCommunication, boolean needPermission) {
        if (EmailCommunicationType.INCOMING.equals(emailCommunication.getEmailCommunicationType())) {
            emailCommunication.setEmailCommunicationStatus(EmailCommunicationStatus.RECEIVED);
        } else {
            if (SENT.equals(emailCommunication.getEmailCommunicationStatus())) {
                emailCommunicationSenderService.sendSingle(emailCommunication.getId());

                if (needPermission) {
                    AccountManager accountManager = accountManagerRepository
                            .findByUserName(permissionService.getLoggedInUserId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));

                    emailCommunication.setSenderEmployeeId(accountManager.getId());
                }
                emailCommunication.setSentDate(LocalDateTime.now());
            }
        }
    }

    public void saveTemplates(Set<Long> templateIds, Long emailCommunicationId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndLanguages(
                templateIds,
                ContractTemplatePurposes.EMAIL,
                List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN),
                List.of(ContractTemplateType.DOCUMENT), ContractTemplateStatus.ACTIVE, LocalDate.now()
        );

        List<EmailCommunicationTemplates> cbgTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            cbgTemplates.add(new EmailCommunicationTemplates(templateId, emailCommunicationId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        emailCommunicationTemplateRepository.saveAll(cbgTemplates);
    }

    public void updateTemplates(Set<Long> templateIds, Long emailCommunicationTemplates, List<String> errorMessages) {
        if (templateIds == null) {
            templateIds = new HashSet<>();
        }

        Map<Long, EmailCommunicationTemplates> templateMap = emailCommunicationTemplateRepository
                .findByProductDetailId(emailCommunicationTemplates)
                .stream()
                .collect(
                        Collectors.toMap(EmailCommunicationTemplates::getTemplateId, j -> j)
                );

        List<EmailCommunicationTemplates> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();

        int i = 0;
        for (Long templateId : templateIds) {
            EmailCommunicationTemplates remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new EmailCommunicationTemplates(templateId, emailCommunicationTemplates));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }

        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndLanguages(
                templateIds,
                ContractTemplatePurposes.EMAIL,
                List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN),
                List.of(ContractTemplateType.DOCUMENT), ContractTemplateStatus.ACTIVE, LocalDate.now()
        );

        templatesToCheck.forEach((key, value) -> {
                                     if (!allIdByIdAndStatus.contains(key)) {
                                         errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
                                     }
                                 }
        );

        if (!errorMessages.isEmpty()) {
            return;
        }

        Collection<EmailCommunicationTemplates> values = templateMap.values();
        for (EmailCommunicationTemplates value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        emailCommunicationTemplateRepository.saveAll(templatesToSave);

    }

    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return emailCommunicationTemplateRepository.findForContract(productDetailId, LocalDate.now());
    }

    private void validateAndSetTemplate(
            Long templateId,
            ContractTemplatePurposes purposes,
            EmailCommunication object,
            List<String> messages
    ) {
        if (Objects.equals(templateId, object.getEmailTemplateId()))
            return;
        if (templateId == null) {
            object.setEmailTemplateId(null);
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, purposes, ContractTemplateType.EMAIL, LocalDate.now())) {
            messages.add("emailTemplateId-Template with id %s do not exist!;".formatted(templateId));
        }
        object.setEmailTemplateId(templateId);
    }

    /**
     * Retrieves the sort by value for the given EmailCommunicationListColumns enum.
     *
     * @param sortBy The EmailCommunicationListColumns enum to get the sort by value for.
     * @return The sort by value for the given EmailCommunicationListColumns enum, or the value of EmailCommunicationListColumns.ID if sortBy is null.
     */
    private String getSortByEnum(EmailCommunicationListColumns sortBy) {
        return sortBy != null ? sortBy.getValue() : EmailCommunicationListColumns.ID.getValue();
    }

    private void archiveFiles(EmailCommunication emailCommunication) {
        List<EmailCommunicationFile> emailCommunicationFiles = emailCommunicationFileRepository.findAllActiveFileByEmailCommunicationId(emailCommunication.getId());
        if (CollectionUtils.isNotEmpty(emailCommunicationFiles)) {
            for (EmailCommunicationFile emailCommunicationFile : emailCommunicationFiles) {
                try {
                    emailCommunicationFile.setNeedArchive(true);
                    emailCommunicationFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_EMAIL_FILE);
                    emailCommunicationFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_EMAIL_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), "%s/%s/%s".formatted("Email Communication", emailCommunication.getId(), emailCommunicationFile.getId())),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(emailCommunicationFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: [%s]".formatted(emailCommunicationFile.getLocalFileUrl()), e);
                }
            }
        }
    }

    public void sendContractTermination(
            Long customerCommunicationId, Long customerDetailId, Long emailTemplateId,
            String subject, String body, Pair<String, String> documentNameAndUrl
    ) {
        Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByNameAndStatusAndIsHardcodedTrue("Contract Termination", NomenclatureItemStatus.ACTIVE);
        if (topicOfCommunicationOptional.isEmpty()) {
            log.error("Contract Termination Topic of communication nomenclature not found");
            return;
        }

        Optional<EmailMailboxes> emailMailboxOptional = emailMailboxesRepository.findByDefaultSelectionTrue();
        if (emailMailboxOptional.isEmpty()) {
            log.error("Email box default nomenclature not found");
            return;
        }

        EmailCommunication emailCommunication = new EmailCommunication();
        emailCommunication.setCommunicationTopicId(topicOfCommunicationOptional.get().getId());
        emailCommunication.setEmailCommunicationType(EmailCommunicationType.OUTGOING);
        emailCommunication.setEmailBoxId(emailMailboxOptional.get().getId());
        emailCommunication.setEmailSubject(subject);
        emailCommunication.setEmailBody(body);
        emailCommunication.setCommunicationAsAnInstitution(false);
        emailCommunication.setCommunicationChannel(EmailCommunicationChannelType.EMAIL);
        emailCommunication.setEmailTemplateId(emailTemplateId);
        emailCommunicationRepository.save(emailCommunication);

        EmailCommunicationCustomer emailCommunicationCustomer = new EmailCommunicationCustomer();
        emailCommunicationCustomer.setEmailCommunicationId(emailCommunication.getId());
        emailCommunicationCustomer.setCustomerCommunicationId(customerCommunicationId);
        emailCommunicationCustomer.setCustomerDetailId(customerDetailId);
        emailCommunicationCustomerRepository.save(emailCommunicationCustomer);

        List<CustomerCommunicationContacts> customerCommunicationContacts = getActiveCustomerCommunicationEmailContacts(customerDetailId);
        customerCommunicationContacts.forEach(it -> {
            EmailCommunicationCustomerContact emailCommunicationCustomerContact = new EmailCommunicationCustomerContact();
            emailCommunicationCustomerContact.setEmailAddress(it.getContactValue());
            emailCommunicationCustomerContact.setEmailCommunicationCustomerId(emailCommunicationCustomer.getId());
            emailCommunicationCustomerContact.setCustomerCommunicationContactId(it.getId());
            emailCommunicationCustomerContactRepository.save(emailCommunicationCustomerContact);
        });
        if (Objects.nonNull(documentNameAndUrl)) {
            emailCommunicationAttachmentRepository.save(EmailCommunicationAttachment.builder()
                                                                .name(documentNameAndUrl.getLeft())
                                                                .fileUrl(documentNameAndUrl.getRight())
                                                                .status(EntityStatus.ACTIVE)
                                                                .emailCommunicationId(emailCommunication.getId())
                                                                .build());
        }
        emailCommunicationSenderService.sendSingle(emailCommunication.getId());

    }

    public void createReminderEmail(
            ReminderProcessItem reminderProcessItemRow,
            Long customerDetailId,
            TopicOfCommunication topicOfCommunication,
            Long templateId
    ) {
        Optional<EmailMailboxes> emailMailboxOptional = emailMailboxesRepository.findByDefaultSelectionTrue();
        if (emailMailboxOptional.isEmpty()) {
            log.error("Email box default nomenclature not found");
            return;
        }

        EmailCommunication emailCommunication = new EmailCommunication();
        emailCommunication.setCommunicationTopicId(topicOfCommunication.getId());
        emailCommunication.setEmailCommunicationType(EmailCommunicationType.OUTGOING);
        emailCommunication.setEmailBoxId(emailMailboxOptional.get().getId());
        emailCommunication.setEmailTemplateId(templateId);
        emailCommunication.setCommunicationAsAnInstitution(false);
        emailCommunication.setCommunicationChannel(EmailCommunicationChannelType.EMAIL);
        emailCommunication.setCreationType(CreationType.AUTOMATIC);
        emailCommunication.setEmailCommunicationStatus(EmailCommunicationStatus.SENT);
        emailCommunication.setEntityStatus(EntityStatus.ACTIVE);
        validateAndSetEmailBodyByTemplateForReminder(emailCommunication, reminderProcessItemRow, customerDetailId);
        emailCommunicationRepository.saveAndFlush(emailCommunication);

        EmailCommunicationCustomer emailCommunicationCustomer = new EmailCommunicationCustomer();
        emailCommunicationCustomer.setEmailCommunicationId(emailCommunication.getId());
        emailCommunicationCustomer.setCustomerCommunicationId(reminderProcessItemRow.getCommunicationId());
        emailCommunicationCustomer.setCustomerDetailId(customerDetailId);
        emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);

        List<CustomerCommunicationContacts> customerCommunicationContacts = getActiveCustomerCommunicationEmailContacts(reminderProcessItemRow.getCommunicationId());
        List<EmailCommunicationCustomerContact> emailCommunicationCustomerContacts = new ArrayList<>();
        customerCommunicationContacts.forEach(it -> {
            EmailCommunicationCustomerContact emailCommunicationCustomerContact = new EmailCommunicationCustomerContact();
            emailCommunicationCustomerContact.setEmailAddress(it.getContactValue());
            emailCommunicationCustomerContact.setEmailCommunicationCustomerId(emailCommunicationCustomer.getId());
            emailCommunicationCustomerContact.setCustomerCommunicationContactId(it.getId());
            emailCommunicationCustomerContacts.add(emailCommunicationCustomerContact);
        });
        emailCommunicationCustomerContactRepository.saveAllAndFlush(emailCommunicationCustomerContacts);

        emailCommunicationSenderService.sendSingle(emailCommunication.getId());
    }

    /**
     * Saves the specified email communication templates for a deposit.
     *
     * @param templateIds          the IDs of the contract templates to save
     * @param emailCommunicationId the ID of the email communication
     * @param errorMessages        a list to store any error messages encountered
     */
    public void saveTemplatesForDocument(
            Set<Long> templateIds, Long
                    emailCommunicationId, List<String> errorMessages
    ) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        List<Long> templates = contractTemplateRepository.findAllById(templateIds).stream().map(ContractTemplate::getId).toList();

        List<EmailCommunicationTemplates> emailTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!templates.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found;".formatted(i, templateId));
            }
            emailTemplates.add(new EmailCommunicationTemplates(templateId, emailCommunicationId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        emailCommunicationTemplateRepository.saveAll(emailTemplates);
    }

}

