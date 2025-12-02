package bg.energo.phoenix.service.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.documentModels.latePaymentFine.FileInfoShortResponse;
import bg.energo.phoenix.model.documentModels.mlo.MloDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.receivable.AutomaticOffsettingService;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerDeposits;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOTemplates;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ManualLiabilityOffsetting;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ReceivableTemplateRequest;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.Reversed;
import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingCalculateRequest;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingCalculateRequestData;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingCreateRequest;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingCustomerRequest;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing.ManualLIabilityOffsettingListingRequest;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing.ManualLiabilityOffsettingListColumns;
import bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing.ManualLiabilityOffsettingSearchByEnums;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositDocumentFileRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.*;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.DepositDocumentCreationService;
import bg.energo.phoenix.service.document.MloDocumentCreationService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.service.receivable.payment.PaymentOffsettingService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;
import org.postgresql.util.PGobject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualLiabilityOffsettingService {
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository;
    private final MLOCustomerDepositsRepository mloCustomerDepositsRepository;
    private final MLOCustomerLiabilitiesRepository mloCustomerLiabilitiesRepository;
    private final MLOCustomerReceivablesRepository mloCustomerReceivablesRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final BlockingReasonRepository blockingReasonRepository;
    private final PermissionService permissionService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final MLOTemplatesRepository mloTemplatesRepository;
    private final DepositRepository depositRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableService customerReceivableService;
    private final AutomaticOffsettingService automaticOffsettingService;
    private final LatePaymentFineService latePaymentFineService;
    private final EmailCommunicationService emailCommunicationService;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final MloDocumentCreationService mloDocumentCreationService;
    private final FileService fileService;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final DepositDocumentCreationService depositDocumentCreationService;
    private final DepositDocumentFileRepository depositDocumentFileRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final DocumentsRepository documentsRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOffsettingService paymentOffsettingService;
    private final MLONegativePaymentsRepository mloNegativePaymentsRepository;
    private final TransactionTemplate transactionTemplate;

    private final MLODocumentFileRepository mLODocumentFileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public static List<ManualLiabilityOffsettingCalculateResponseData> pgObjectToData(Object object) {
        if (object == null) return null;
        String value = ((PGobject) object).getValue();
        return Arrays.asList(EPBJsonUtils.asObject(value, ManualLiabilityOffsettingCalculateResponseData[].class));
    }

    public static List<ManualLiabilityOffsettingCalculateOffsetResponse> pgObjectToOffsetData(Object object) {
        if (object == null) return null;
        String value = ((PGobject) object).getValue();
        return Arrays.asList(EPBJsonUtils.asObject(value, ManualLiabilityOffsettingCalculateOffsetResponse[].class));
    }

    public ManualLiabilityOffsettingResponse customerLiabilityOffsetting(ManualLiabilityOffsettingCustomerRequest request) {
        Long customerDetailId = getCustomerDetailId(request.getCustomerId(), request.getCustomerDetailId());
        List<CustomerCommunicationDataResponse> customerCommunicationDataResponses = customerRepository
                .customerCommunicationDataListForManualOffsetting(
                        customerDetailId,
                        communicationContactPurposeProperties.getBillingCommunicationId()
                );
        if (customerCommunicationDataResponses.isEmpty()) {
            throw new DomainEntityNotFoundException("Customer do not have Communication date for billing;");
        }

        ManualLiabilityOffsettingResponse manualLiabilityOffsettingResponse = mapIntoResponse(request.getCustomerId(), request.getDate());
        manualLiabilityOffsettingResponse.setCommunicationDataResponse(customerCommunicationDataResponses);

        return manualLiabilityOffsettingResponse;
    }

    @Transactional
    public Long create(ManualLiabilityOffsettingCreateRequest request) {
        List<String> errorMessages = new ArrayList<>();
//        ManualLiabilityOffsetting manualLiabilityOffsetting = new ManualLiabilityOffsetting(request);
//        manualLiabilityOffsetting.setReversed(false);

        Long customerDetailId = getCustomerDetailId(request.getCustomerId(), request.getCustomerDetailId());
//        manualLiabilityOffsetting.setCustomerDetailId(customerDetailId);
        List<CustomerCommunicationDataResponse> customerCommunicationDataResponses = customerRepository
                .customerCommunicationDataListForManualOffsetting(
                        customerDetailId,
                        communicationContactPurposeProperties.getBillingCommunicationId()
                );

        List<Long> communicationDataIds = customerCommunicationDataResponses.stream().map(CustomerCommunicationDataResponse::getId).toList();
        if (!communicationDataIds.contains(request.getCustomerCommunicationDataId())) {
            errorMessages.add("customerCommunicationId-customerCommunicationId is not correct;");
        }

        Long mloId;

        if (!CollectionUtils.isEmpty(request.getPayments())) {
            if (!CollectionUtils.isEmpty(request.getLiabilities()) || !CollectionUtils.isEmpty(request.getDeposits())) {
                errorMessages.add("When using negative payments, only receivables can be selected (no liabilities or deposits);");
            }

            if (CollectionUtils.isEmpty(request.getReceivables())) {
                errorMessages.add("When using negative payments, receivables must be selected;");
            }

            if (request.getReceivables().size() != 1 || request.getPayments().size() != 1) {
                errorMessages.add("When using negative payments, only one receivable and one payment can be selected;");
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

            mloId = saveNegativePaymentOffsetting(
                    request.getPayments().get(0).id(),
                    request.getReceivables().get(0).id(),
                    request.getDate(),
                    request.getReceivables().get(0).currentAmount(),
                    Math.toIntExact(request.getPayments().get(0).currencyId()),
                    request.getCustomerId(),
                    request.getCustomerCommunicationDataId(),
                    permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                    permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                    customerDetailId
            );
        } else {

            if (CollectionUtils.isEmpty(request.getLiabilities()) && CollectionUtils.isEmpty(request.getPayments())) {
                errorMessages.add("It is mandatory to selects at least one record in the left side of the section;");
            }

            if (CollectionUtils.isEmpty(request.getReceivables()) && CollectionUtils.isEmpty(request.getDeposits())) {
                errorMessages.add("It is mandatory to selects at least one record in the right side of the section;");
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
//        manualLiabilityOffsetting.setCustomerCommunicationId(request.getCustomerCommunicationDataId());
//        manualLiabilityOffsettingRepository.save(manualLiabilityOffsetting);
            ManualLiabilityOffsettingSaveResponse saveResponse = save(request);
            mloId = saveResponse.getId();
            updateDepositAndCreateLiability(saveResponse);

            if (!CollectionUtils.isEmpty(request.getDeposits())) {
                request.getDeposits().forEach(deposit -> {
                    log.debug("Processing deposit with ID: {}", deposit.id());
                    try {
                        List<Long> emailIds = depositDocumentCreationService.generateDocuments(deposit.id(), saveResponse.getId());
                        log.debug("Successfully generated documents and emails for deposit {}. Generated email IDs: {}", deposit.id(), emailIds);
                    } catch (Exception e) {
                        log.error("Failed to generate documents for deposit {}: {}", deposit.id(), e.getMessage());
                        throw new ClientException("Failed to generate documents for deposit %d: %s".formatted(deposit.id(), e.getMessage()), APPLICATION_ERROR);
                    }
                });

                log.debug("Completed document generation for all deposits");
            }
        }

        saveTemplates(request.getTemplateIds(), mloId, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        generateDocument(mloId);
        return mloId;

//        checkCustomerLiabilitiesAndPaymentFines(request, errorMessages, manualLiabilityOffsetting.getId());
//        checkCustomerReceivablesAndDeposits(request, errorMessages, manualLiabilityOffsetting.getId());

    }

    public void generateDocument(Long mloId) {
        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository.findByNameAndStatusAndIsHardcodedTrue("Offsetting", NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication offsetting not found!"));

        Optional<EmailMailboxes> emailByInvoice = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();

        Long emailBoxId = null;

        if (emailByInvoice.isPresent()) {
            emailBoxId = emailByInvoice.get().getId();
        } else {
            EmailMailboxes emailMailboxes = emailMailboxesRepository.findByName("HardCoded Email").orElseThrow(() -> new DomainEntityNotFoundException("HardCoded Email not found!"));
            emailBoxId = emailMailboxes.getId();
        }

        ManualLiabilityOffsetting manualLiabilityOffsetting = manualLiabilityOffsettingRepository.findById(mloId).orElseThrow(() -> new DomainEntityNotFoundException("MLO not found with id " + mloId));

        Customer customer = customerRepository.findById(manualLiabilityOffsetting.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found!"));
        CustomerDetails customerDetails = customerDetailsRepository.findById(manualLiabilityOffsetting.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("CustomerDetails not found!"));

        List<Object[]> mloTemplates = mloTemplatesRepository.findByMloId(mloId);
        List<Object[]> emailTemplates = mloTemplates.stream().
                filter(template -> ((MLOTemplates) template[0]).getType().equals(ReceivableTemplateType.EMAIL)).toList();
        List<Object[]> documentTemplates = mloTemplates.stream().
                filter(template -> ((MLOTemplates) template[0]).getType().equals(ReceivableTemplateType.DOCUMENT)).toList();

        List<CustomerCommunicationContacts> communicationContacts = customerCommunicationContactsRepository.
                findByCustomerCommIdContactTypesAndStatuses(manualLiabilityOffsetting.getCustomerCommunicationId(),
                        List.of(CustomerCommContactTypes.EMAIL),
                        List.of(Status.ACTIVE));

        boolean isEmailTemplateAttached = !emailTemplates.isEmpty();
        for (Object[] template : emailTemplates) {
            ContractTemplateDetail contractTemplateDetail = (ContractTemplateDetail) template[2];
            if (contractTemplateDetail.getCustomerType() != null
                    && !contractTemplateDetail.getCustomerType().isEmpty()
                    && !contractTemplateDetail.getCustomerType().contains(customer.getCustomerType())) {
                log.info("Skipping , not appropriate customer Type!");
                continue;
            }

            MloDocumentModel model = mloDocumentCreationService.build(mloId);

            FileInfoShortResponse fileInfoShortResponse = mloDocumentCreationService.generateDocument(manualLiabilityOffsetting, model, contractTemplateDetail.getTemplateId(), FileFormat.DOCX, false, manualLiabilityOffsetting.getCustomerDetailId());
            ByteArrayResource byteArrayResource = fileService.downloadFile(fileInfoShortResponse.fileUrl());
            String emailBody = null;
            try {
                emailBody = parseDocx(byteArrayResource.getByteArray());
            } catch (Exception e) {
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }

            StringBuilder emailBuilder = new StringBuilder();

            for (int i = 0; i < communicationContacts.size(); i++) {
                emailBuilder.append(communicationContacts.get(i).getContactValue());
                if (i != communicationContacts.size() - 1) {
                    emailBuilder.append(";");
                }
            }
            Set<Long> attachmentFileIds = new HashSet<>();
            for (Object[] documentTemplate : documentTemplates) {
                ContractTemplateDetail contractTemplateDetailDocument = (ContractTemplateDetail) documentTemplate[2];
                if (contractTemplateDetailDocument.getCustomerType() != null
                        && !contractTemplateDetailDocument.getCustomerType().isEmpty()
                        && !contractTemplateDetailDocument.getCustomerType().contains(customer.getCustomerType())) {
                    log.info("Skipping , not appropriate customer Type!");
                    continue;
                }
                FileInfoShortResponse document = mloDocumentCreationService.generateDocument(manualLiabilityOffsetting, model, contractTemplateDetailDocument.getTemplateId(), FileFormat.PDF, true, customerDetails.getId());
                EmailCommunicationAttachment attachment = new EmailCommunicationAttachment();
                attachment.setName(document.fileName());
                attachment.setFileUrl(document.fileUrl());
                attachment.setStatus(EntityStatus.ACTIVE);
                attachmentFileIds.add(emailCommunicationAttachmentRepository.saveAndFlush(attachment).getId());
            }

            DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest = new DocumentEmailCommunicationCreateRequest();
            documentEmailCommunicationCreateRequest.setCommunicationTopicId(topicOfCommunication.getId());
            documentEmailCommunicationCreateRequest.setEmailBoxId(emailBoxId);
            documentEmailCommunicationCreateRequest.setEmailSubject(contractTemplateDetail.getSubject());
            documentEmailCommunicationCreateRequest.setCustomerDetailId(customerDetails.getId());
            documentEmailCommunicationCreateRequest.setCustomerCommunicationId(manualLiabilityOffsetting.getCustomerCommunicationId());
            documentEmailCommunicationCreateRequest.setEmailBody(emailBody);
            documentEmailCommunicationCreateRequest.setCustomerEmailAddress(emailBuilder.toString());
            documentEmailCommunicationCreateRequest.setAttachmentFileIds(attachmentFileIds);
            emailCommunicationService.createEmailFromDocument(documentEmailCommunicationCreateRequest, true);
        }
        if (!isEmailTemplateAttached) {
            MloDocumentModel model = mloDocumentCreationService.build(mloId);

            StringBuilder emailBuilder = new StringBuilder();

            for (int i = 0; i < communicationContacts.size(); i++) {
                emailBuilder.append(communicationContacts.get(i).getContactValue());
                if (i != communicationContacts.size() - 1) {
                    emailBuilder.append(";");
                }
            }
            Set<Long> attachmentFileIds = new HashSet<>();
            for (Object[] documentTemplate : documentTemplates) {
                ContractTemplateDetail contractTemplateDetailDocument = (ContractTemplateDetail) documentTemplate[2];
                if (contractTemplateDetailDocument.getCustomerType() != null
                        && !contractTemplateDetailDocument.getCustomerType().isEmpty()
                        && !contractTemplateDetailDocument.getCustomerType().contains(customer.getCustomerType())) {
                    log.info("Skipping , not appropriate customer Type!");
                    continue;
                }
                FileInfoShortResponse document = mloDocumentCreationService.generateDocument(manualLiabilityOffsetting, model, contractTemplateDetailDocument.getTemplateId(), FileFormat.PDF, true, customerDetails.getId());
                EmailCommunicationAttachment attachment = new EmailCommunicationAttachment();
                attachment.setName(document.fileName());
                attachment.setFileUrl(document.fileUrl());
                attachment.setStatus(EntityStatus.ACTIVE);
                attachmentFileIds.add(emailCommunicationAttachmentRepository.saveAndFlush(attachment).getId());


                DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest = new DocumentEmailCommunicationCreateRequest();
                documentEmailCommunicationCreateRequest.setCommunicationTopicId(topicOfCommunication.getId());
                documentEmailCommunicationCreateRequest.setEmailBoxId(emailBoxId);
                documentEmailCommunicationCreateRequest.setEmailSubject(" ");
                documentEmailCommunicationCreateRequest.setEmailBody(" ");
                documentEmailCommunicationCreateRequest.setCustomerDetailId(customerDetails.getId());
                documentEmailCommunicationCreateRequest.setCustomerCommunicationId(manualLiabilityOffsetting.getCustomerCommunicationId());
                documentEmailCommunicationCreateRequest.setCustomerEmailAddress(emailBuilder.toString());
                documentEmailCommunicationCreateRequest.setAttachmentFileIds(attachmentFileIds);
                emailCommunicationService.createEmailFromDocument(documentEmailCommunicationCreateRequest, true);

            }
        }

    }

    public Page<ManualLIabilityOffsettingListingResponse> filter(ManualLIabilityOffsettingListingRequest request) {
        if (Objects.nonNull(request.getFromDate()) && Objects.nonNull(request.getToDate()) && request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentsProvidedException("fromDate-From date should not be after to date;");
        }

        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(request.getDirection(), Sort.Direction.DESC), checkSortField(request));

        return manualLiabilityOffsettingRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getFromDate(),
                        request.getToDate(),
                        getReversedList(request.getReversed()),
                        getSearchByEnum(request),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(order)
                        )
                )
                .map(ManualLIabilityOffsettingListingResponse::new);
    }

    public ManualLiabilityOffsettingPreviewResponse view(Long id) {
        log.debug("Fetching Manual Liability Offsetting with ID: {};", id);
        ManualLiabilityOffsetting manualLiabilityOffsetting = manualLiabilityOffsettingRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Manual Liability Offsetting with ID %s not found;".formatted(id)));

        ManualLiabilityOffsettingPreviewResponse previewResponse = new ManualLiabilityOffsettingPreviewResponse();
        previewResponse.setDate(manualLiabilityOffsetting.getManualLiabilityDate());
        previewResponse.setIdNumber(id);
        previewResponse.setSelectedCustomerCommunicationId(manualLiabilityOffsetting.getCustomerCommunicationId());

        Long customerId = manualLiabilityOffsetting.getCustomerId();
        previewResponse.setCustomerId(customerId);
        previewResponse.setReversed(manualLiabilityOffsetting.isReversed());
        List<CustomerCommunicationDataResponse> customerCommunicationDataResponses = customerRepository
                .customerCommunicationDataListForManualOffsetting(
                        manualLiabilityOffsetting.getCustomerDetailId(),
                        communicationContactPurposeProperties.getBillingCommunicationId()
                );
        previewResponse.setCommunicationDataResponse(customerCommunicationDataResponses);

        List<LiabilitiesOffsettingChoice> checkedCustomerLiabilities = mloCustomerLiabilitiesRepository.getLiabilityIdsIdsByManualOffsettingId(id);
        List<LiabilitiesOffsettingChoice> checkedNegativePayments = mloNegativePaymentsRepository.getNegativePaymentsByMLOId(id);

        previewResponse.setCheckedCustomerLiabilities(checkedCustomerLiabilities);
        previewResponse.setNegativePayments(checkedNegativePayments);
        previewResponse.setTemplateResponses(getTemplateResponse(manualLiabilityOffsetting.getId()));
        previewResponse.setCheckedDeposits(mloCustomerDepositsRepository.getDepositIdsIdsByManualOffsettingId(id));
        previewResponse.setCheckedReceivables(mloCustomerReceivablesRepository.getReceivableIdsIdsByManualOffsettingId(id));

        List<FileWithStatusesResponse> files = new ArrayList<>();

        if (!previewResponse.getCheckedDeposits().isEmpty()) {
            files.addAll(documentsRepository.findDocumentsForDeposit(previewResponse.getCheckedDeposits().stream().map(LiabilitiesOffsettingChoice::getId).toList())
                    .stream().map(file -> new FileWithStatusesResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                            .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""))).toList());
        }
        files.addAll(documentsRepository.findDocumentsForMloOffsetting(id)
                .stream()
                .map(
                        file -> new FileWithStatusesResponse(
                                file,
                                accountManagerRepository
                                        .findByUserName(file.getSystemUserId())
                                        .map(
                                                manager -> " ("
                                                        .concat(manager.getDisplayName())
                                                        .concat(")")
                                        )
                                        .orElse("")
                        )
                )
                .toList());

        previewResponse.setFiles(files);

        return previewResponse;
    }

    /**
     * Retrieves the customer detail ID for the given customer ID.
     * If the customer detail ID is not provided, it will use the last customer detail ID associated with the customer.
     * Throws a {@link DomainEntityNotFoundException} if the customer does not have a detail with the provided ID.
     *
     * @param customerId       The ID of the customer.
     * @param customerDetailId The ID of the customer detail, or null to use the last customer detail ID.
     * @return The customer detail ID.
     */
    private Long getCustomerDetailId(Long customerId, Long customerDetailId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isPresent() && Objects.isNull(customerDetailId)) {
            customerDetailId = customer.get().getLastCustomerDetailId();
        }
        if (!customerDetailsRepository.existsByIdAndCustomerId(customerDetailId, customerId)) {
            throw new DomainEntityNotFoundException("Customer do not have Detail with id %s;".formatted(customerDetailId));
        }
        return customerDetailId;
    }

    private ManualLiabilityOffsettingResponse mapIntoResponse(Long customerId, LocalDate date) {

        List<LiabilitiesOffsettingChoice> customerLiabilities = new ArrayList<>(manualLiabilityOffsettingRepository
                .getCustomerLiabilities(customerId, Date.valueOf(date))
                .parallelStream()
                .map(LiabilitiesOffsettingChoice::new)
                .toList());

        List<LiabilitiesOffsettingChoice> receivables = new ArrayList<>(manualLiabilityOffsettingRepository
                .getReceivables(customerId, Date.valueOf(date))
                .parallelStream()
                .map(LiabilitiesOffsettingChoice::new)
                .toList());

        List<LiabilitiesOffsettingChoice> deposits = manualLiabilityOffsettingRepository
                .getDeposits(customerId, Date.valueOf(date))
                .parallelStream()
                .map(LiabilitiesOffsettingChoice::new)
                .toList();

        List<LiabilitiesOffsettingChoice> negativePayments = manualLiabilityOffsettingRepository
                .getPayments(customerId, Date.valueOf(date))
                .parallelStream()
                .map(LiabilitiesOffsettingChoice::new)
                .toList();

        ManualLiabilityOffsettingResponse manualLiabilityOffsettingResponse = new ManualLiabilityOffsettingResponse();
        manualLiabilityOffsettingResponse.setCustomerLiabilities(customerLiabilities);
        manualLiabilityOffsettingResponse.setReceivables(receivables);
        manualLiabilityOffsettingResponse.setDeposits(deposits);
        manualLiabilityOffsettingResponse.setNegativePayments(negativePayments);

        return manualLiabilityOffsettingResponse;
    }

//    private void checkCustomerLiabilitiesAndPaymentFines(ManualLiabilityOffsettingCreateRequest request, List<String> errorMessages, Long manualLiabilityOffsettingId) {
//        List<Long> liabilitiesFromRequest = request.getCustomerLiabilities();
//        List<MLOCustomerLiabilities> mloCustomerLiabilitiesList = liabilitiesFromRequest.stream()
//                .filter(Objects::nonNull)
//                .map(customerLiabilityId -> new MLOCustomerLiabilities(customerLiabilityId, manualLiabilityOffsettingId))
//                .collect(Collectors.toList());
//
//        List<Long> customerLiabilities = manualLiabilityOffsettingRepository
//                .getCustomerLiabilitiesIds(request.getCustomerId());
//
//        if (CollectionUtils.isNotEmpty(liabilitiesFromRequest)) {
//            if (hasDuplicates(liabilitiesFromRequest)) {
//                errorMessages.add("customerLiabilities-customerLiabilities must contain unique elements;");
//            }
//
//            if (new HashSet<>(customerLiabilities).containsAll(liabilitiesFromRequest)) {
//                mloCustomerLiabilitiesRepository.saveAll(mloCustomerLiabilitiesList);
//            } else {
//                errorMessages.add("customerLiabilities-customerLiabilities in not correct;");
//            }
//        }
//
//        List<Long> latePaymentFinesFromRequest = request.getLatePaymentFines();
//        List<MLOCustomerLiabilities> mloLatePaymentFinesList = latePaymentFinesFromRequest.stream()
//                .filter(Objects::nonNull)
//                .map(latePaymentFineId -> new MLOCustomerLiabilities(latePaymentFineId, manualLiabilityOffsettingId))
//                .collect(Collectors.toList());
//
//        List<Long> latePaymentFines = manualLiabilityOffsettingRepository
//                .getLatePaymentFinesIds(request.getCustomerId());
//
//        if (CollectionUtils.isNotEmpty(latePaymentFinesFromRequest)) {
//            if (hasDuplicates(liabilitiesFromRequest)) {
//                errorMessages.add("latePaymentFines-latePaymentFines must contain unique elements;");
//            }
//
//            if (new HashSet<>(latePaymentFines).containsAll(latePaymentFinesFromRequest)) {
//                mloCustomerLiabilitiesRepository.saveAll(mloLatePaymentFinesList);
//            } else {
//                errorMessages.add("latePaymentFines-latePaymentFines in not correct;");
//            }
//        }
//    }
//
//    private void checkCustomerReceivablesAndDeposits(ManualLiabilityOffsettingCreateRequest request, List<String> errorMessages, Long manualLiabilityOffsettingId) {
//        List<Long> receivablesFromRequest = request.getReceivables();
//        List<MLOCustomerReceivables> mloCustomerReceivablesList = receivablesFromRequest.stream()
//                .filter(Objects::nonNull)
//                .map(customerLiabilitiesId -> new MLOCustomerReceivables(customerLiabilitiesId, manualLiabilityOffsettingId))
//                .collect(Collectors.toList());
//
//        List<Long> receivables = manualLiabilityOffsettingRepository
//                .getReceivablesIds(request.getCustomerId());
//        if (CollectionUtils.isNotEmpty(receivablesFromRequest)) {
//            if (hasDuplicates(receivablesFromRequest)) {
//                errorMessages.add("receivables-receivables must contain unique elements;");
//            }
//
//            if (new HashSet<>(receivables).containsAll(receivablesFromRequest)) {
//                mloCustomerReceivablesRepository.saveAll(mloCustomerReceivablesList);
//            } else {
//                errorMessages.add("receivables-receivables in not correct;");
//            }
//        }
//
//        List<Long> depositsFromRequest = request.getDeposits();
//        List<MLOCustomerDeposits> mloCustomerDepositsList = depositsFromRequest.stream()
//                .filter(Objects::nonNull)
//                .map(customerDepositId -> new MLOCustomerDeposits(customerDepositId, manualLiabilityOffsettingId))
//                .collect(Collectors.toList());
//
//        List<Long> deposits = manualLiabilityOffsettingRepository
//                .getDepositsIds(request.getCustomerId());
//
//        if (CollectionUtils.isNotEmpty(depositsFromRequest)) {
//            if (hasDuplicates(depositsFromRequest)) {
//                errorMessages.add("deposits-deposits must contain unique elements;");
//            }
//
//            if (new HashSet<>(deposits).containsAll(depositsFromRequest)) {
//                mloCustomerDepositsRepository.saveAll(mloCustomerDepositsList);
//            } else {
//                errorMessages.add("deposits-deposits in not correct;");
//            }
//        }
//    }

    private String checkSortField(ManualLIabilityOffsettingListingRequest request) {
        if (request.getSortBy() == null) {
            return ManualLiabilityOffsettingListColumns.ID.getValue();
        } else
            return request.getSortBy().getValue();
    }

    private String getSearchByEnum(ManualLIabilityOffsettingListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else
            searchByField = ManualLiabilityOffsettingSearchByEnums.ALL.getValue();
        return searchByField;
    }

    private List<String> getReversedList(List<Reversed> reversed) {
        List<String> reversedList = new ArrayList<>();
        if (Objects.nonNull(reversed)) {
            reversed.forEach(rev -> {
                if (rev.equals(Reversed.NO)) {
                    reversedList.add("false");
                } else {
                    reversedList.add("true");
                }
            });
        }
        return reversedList;
    }

    private boolean hasDuplicates(List<Long> list) {
        Set<Long> uniques = new HashSet<>(list);
        return uniques.size() != list.size();
    }

    @Transactional
    public ManualLiabilityOffsettingCalculateResponse calculate(ManualLiabilityOffsettingCalculateRequest request) {
        String liabilities = "[]";
        String receivables = "[]";
        String deposits = "[]";

        if (request.payments() != null && !request.payments().isEmpty()) {
            if ((request.liabilities() != null && !request.liabilities().isEmpty()) ||
                    (request.deposits() != null && !request.deposits().isEmpty())) {
                throw new ClientException("When using negative payments, only receivables can be selected. Liabilities and deposits should be empty.", ErrorCode.APPLICATION_ERROR);
            }

            if (request.receivables() == null || request.receivables().isEmpty()) {
                throw new ClientException("When using negative payments, receivables must be selected.", ErrorCode.APPLICATION_ERROR);
            }

            if (request.receivables().size() != 1 || request.payments().size() != 1) {
                throw new ClientException("Only one payment and one receivable can be processed.", ErrorCode.APPLICATION_ERROR);
            }

            if (request.receivables().get(0).currentAmount().negate().compareTo(request.payments().get(0).currentAmount()) != 0) {
                throw new ClientException("Negative payment and receivable amounts should be equal.", ErrorCode.APPLICATION_ERROR);
            }

            return calculateNegativePaymentOffset(request.payments().get(0), request.receivables().get(0), LocalDate.now().toString());
        }

        // Regular MLO calculation flow
        if (request.deposits() != null && !request.deposits().isEmpty()) {
            deposits = EPBJsonUtils.asJsonString(request.deposits());
        }
        if (request.liabilities() != null && !request.liabilities().isEmpty()) {
            liabilities = EPBJsonUtils.asJsonString(request.liabilities());
        }
        if (request.receivables() != null && !request.receivables().isEmpty()) {
            receivables = EPBJsonUtils.asJsonString(request.receivables());
        }

        ManualLiabilityOffsettingCalculateResponse response = calculateMlo(liabilities, receivables, deposits, LocalDate.now().toString());
        if (!Objects.equals(response.getMessage(), "OK")) {
            throw new ClientException(response.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        return response;
    }

    public ManualLiabilityOffsettingCalculateResponse calculateMlo(
            String liabilities,
            String receivables,
            String deposits,
            String calculateDate
    ) {
        ManualLiabilityOffsettingCalculateResponse output = new ManualLiabilityOffsettingCalculateResponse();

        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.mlo_calculate(?::jsonb,?::jsonb,?::jsonb,to_date(?, 'yyyy-MM-dd'),?,?,?,?,?)");

                callableStatement.setString(1, liabilities);
                callableStatement.setString(2, receivables);
                callableStatement.setString(3, deposits);
                callableStatement.setString(4, calculateDate);

                callableStatement.registerOutParameter(5, Types.OTHER); // o_liab
                callableStatement.registerOutParameter(6, Types.OTHER); // o_recv
                callableStatement.registerOutParameter(7, Types.OTHER); // o_dep
                callableStatement.registerOutParameter(8, Types.OTHER); // o_offsets
                callableStatement.registerOutParameter(9, Types.VARCHAR); // o_message

                callableStatement.execute();

                output.setLiabilities(pgObjectToData(callableStatement.getObject(5)));
                output.setReceivables(pgObjectToData(callableStatement.getObject(6)));
                output.setDeposits(pgObjectToData(callableStatement.getObject(7)));
                output.setOffsets(pgObjectToOffsetData(callableStatement.getObject(8)));
                output.setMessage(callableStatement.getString(9));
            });
        } catch (Exception e) {
            throw new ClientException("Some error happened in mlo_calculate procedure", ErrorCode.APPLICATION_ERROR);
        }
        return output;
    }

    private ManualLiabilityOffsettingCalculateResponse calculateNegativePaymentOffset(
            ManualLiabilityOffsettingCalculateRequestData payment,
            ManualLiabilityOffsettingCalculateRequestData receivable,
            String calculationDate
    ) {
        ManualLiabilityOffsettingCalculateResponse response = new ManualLiabilityOffsettingCalculateResponse();

        try {
            Session session = entityManager.unwrap(Session.class);

            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.mlo_calculate_neg(?, ?, to_date(?, 'yyyy-MM-dd'), ?, ?, ?)"
                );

                callableStatement.setLong(1, payment.id());
                callableStatement.setLong(2, receivable.id());
                callableStatement.setString(3, calculationDate);

                callableStatement.registerOutParameter(4, Types.NUMERIC);
                callableStatement.registerOutParameter(5, Types.INTEGER);
                callableStatement.registerOutParameter(6, Types.VARCHAR);

                callableStatement.execute();

                BigDecimal offsetAmount = callableStatement.getBigDecimal(4);
                Integer offsetCurrencyId = callableStatement.getInt(5);
                String message = callableStatement.getString(6);
                response.setMessage(message);

                if ("OK".equals(message)) {
                    ManualLiabilityOffsettingCalculateResponseData paymentResponse = new ManualLiabilityOffsettingCalculateResponseData();
                    paymentResponse.setId(payment.id());
                    paymentResponse.setCurrencyId((long) offsetCurrencyId);
                    paymentResponse.setCurrentAmount(offsetAmount);

                    ManualLiabilityOffsettingCalculateResponseData receivableResponse = new ManualLiabilityOffsettingCalculateResponseData();
                    receivableResponse.setId(receivable.id());
                    receivableResponse.setCurrencyId((long) offsetCurrencyId);
                    receivableResponse.setCurrentAmount(offsetAmount);

                    response.setPayments(Collections.singletonList(paymentResponse));
                    response.setReceivables(Collections.singletonList(receivableResponse));
                }
            });
        } catch (Exception e) {
            log.error("Error calculating offset: {}", e.getMessage());
            throw new ClientException("Error in mlo_calculate_neg procedure: " + e.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        if (!"OK".equals(response.getMessage())) {
            throw new ClientException("Failed to calculate negative payment offsetting: " + response.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        return response;
    }

    @Transactional
    public ManualLiabilityOffsettingSaveResponse save(ManualLiabilityOffsettingCreateRequest request) {
        String liabilities = "[]";
        String receivables = "[]";
        String deposits = "[]";
        String receivedLiabilities = "[]";
        String receivedReceivables = "[]";
        String receivedDeposits = "[]";
        String receivedOffsets = "[]";

        if (request.getDeposits() != null && !request.getDeposits().isEmpty()) {
            deposits = EPBJsonUtils.asJsonString(request.getDeposits());
        }

        if (request.getReceivedDeposits() != null && !request.getReceivedDeposits().isEmpty()) {
            receivedDeposits = EPBJsonUtils.asJsonString(request.getReceivedDeposits());
        }

        if (request.getLiabilities() != null && !request.getLiabilities().isEmpty()) {
            liabilities = EPBJsonUtils.asJsonString(request.getLiabilities());
        }

        if (request.getReceivedLiabilities() != null && !request.getReceivedLiabilities().isEmpty()) {
            receivedLiabilities = EPBJsonUtils.asJsonString(request.getReceivedLiabilities());
        }

        if (request.getReceivables() != null && !request.getReceivables().isEmpty()) {
            receivables = EPBJsonUtils.asJsonString(request.getReceivables());
        }

        if (request.getReceivedReceivables() != null && !request.getReceivedReceivables().isEmpty()) {
            receivedReceivables = EPBJsonUtils.asJsonString(request.getReceivedReceivables());
        }

        if (request.getReceivedOffsets() != null && !request.getReceivedOffsets().isEmpty()) {
            receivedOffsets = EPBJsonUtils.asJsonString(request.getReceivedOffsets());
        }

        ManualLiabilityOffsettingSaveResponse response = saveMlo(liabilities,
                receivables,
                deposits,
                receivedLiabilities,
                receivedReceivables,
                receivedDeposits,
                receivedOffsets,
                request.getDate().toString(),
                request.getCustomerId(),
                request.getCustomerCommunicationDataId(),
                request.getCustomerDetailId(),
                permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId()
        );
        if (!Objects.equals(response.getMessage(), "OK")) {
            throw new ClientException(response.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        return response;
    }

    public ManualLiabilityOffsettingSaveResponse saveMlo(
            String liabilities,
            String receivables,
            String deposits,
            String receivedLiabilities,
            String receivedReceivables,
            String receivedDeposits,
            String receivedOffsets,
            String calculateDate,
            Long customerId,
            Long customerCommunicationIdForBilling,
            Long customerDetailId,
            String systemUserId,
            String modifySystemUserId
    ) {
        ManualLiabilityOffsettingSaveResponse output = new ManualLiabilityOffsettingSaveResponse();

        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.mlo_save(?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,to_date(?, 'yyyy-MM-dd'),?::bigint,?::bigint,?,?,?::bigint,?,?,?,?,?,?)");

                callableStatement.setString(1, liabilities);
                callableStatement.setString(2, receivables);
                callableStatement.setString(3, deposits);
                callableStatement.setString(4, receivedLiabilities);
                callableStatement.setString(5, receivedReceivables);
                callableStatement.setString(6, receivedDeposits);
                callableStatement.setString(7, receivedOffsets);
                callableStatement.setString(8, calculateDate);
                callableStatement.setString(9, customerId.toString());
                callableStatement.setString(10, customerCommunicationIdForBilling.toString());
                callableStatement.setString(11, systemUserId);
                callableStatement.setString(12, modifySystemUserId);
                callableStatement.setString(13, customerDetailId.toString());

                callableStatement.registerOutParameter(14, Types.OTHER); // o_liab
                callableStatement.registerOutParameter(15, Types.OTHER); // o_recv
                callableStatement.registerOutParameter(16, Types.OTHER); // o_dep
                callableStatement.registerOutParameter(17, Types.OTHER); // o_offsets
                callableStatement.registerOutParameter(18, Types.VARCHAR); // o_message
                callableStatement.registerOutParameter(19, Types.BIGINT); // o_mlo_id

                callableStatement.execute();

                output.setLiabilities(pgObjectToData(callableStatement.getObject(14)));
                output.setReceivables(pgObjectToData(callableStatement.getObject(15)));
                output.setDeposits(pgObjectToData(callableStatement.getObject(16)));
                output.setOffsets(pgObjectToOffsetData(callableStatement.getObject(17)));
                output.setMessage(callableStatement.getString(18));
                output.setId(callableStatement.getLong(19));
            });
        } catch (Exception e) {
            throw new ClientException("Some error happened in mlo_save procedure;", ErrorCode.APPLICATION_ERROR);
        }
        return output;
    }

    private Long saveNegativePaymentOffsetting(
            Long paymentId,
            Long receivableId,
            LocalDate calculationDate,
            BigDecimal offsetAmount,
            Integer offsetCurrencyId,
            Long customerId,
            Long customerCommunicationIdForBilling,
            String systemUserId,
            String modifySystemUserId,
            Long customerDetailId
    ) {
        ManualLiabilityOffsettingSaveResponse output = new ManualLiabilityOffsettingSaveResponse();

        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.mlo_save_neg(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );

                callableStatement.setLong(1, paymentId);
                callableStatement.setLong(2, receivableId);
                callableStatement.setDate(3, Date.valueOf(calculationDate));
                callableStatement.setBigDecimal(4, offsetAmount);
                callableStatement.setInt(5, offsetCurrencyId);
                callableStatement.setLong(6, customerId);
                callableStatement.setLong(7, customerCommunicationIdForBilling);
                callableStatement.setString(8, systemUserId);
                callableStatement.setString(9, modifySystemUserId);
                callableStatement.setLong(10, customerDetailId);

                callableStatement.registerOutParameter(11, Types.VARCHAR);
                callableStatement.registerOutParameter(12, Types.BIGINT);

                callableStatement.execute();

                output.setMessage(callableStatement.getString(11));
                output.setId(callableStatement.getLong(12));
            });
        } catch (Exception e) {
            throw new ClientException("Error in mlo_save_neg procedure: " + e.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        if (!"OK".equals(output.getMessage())) {
            throw new ClientException("Failed to save negative payment offsetting: " + output.getMessage(), ErrorCode.APPLICATION_ERROR);
        }

        return output.getId();
    }

    @Transactional(noRollbackFor = {NoRollbackForException.class})
    public void reversal(Long mloId) {
        ManualLiabilityOffsetting manualLiabilityOffsetting = manualLiabilityOffsettingRepository.findById(mloId).orElseThrow(
                () -> new DomainEntityNotFoundException("Manual liability offsetting with this id [%s] not found;".formatted(mloId))
        );

        if (manualLiabilityOffsetting.isReversed()) {
            throw new ClientException("Manual liability offsetting already reversed;", ErrorCode.APPLICATION_ERROR);
        }

        List<LiabilitiesOffsettingChoice> negativePayments = mloNegativePaymentsRepository.getNegativePaymentsByMLOId(mloId);
        if (!negativePayments.isEmpty()) {
            throw new ClientException("Negative payments was used for this manual liability offsetting, it isn't possible to reverse such manual liability offsetting;", ErrorCode.APPLICATION_ERROR);
        }

        List<LiabilitiesOffsettingChoice> mloLiabilities = mloCustomerLiabilitiesRepository.getLiabilityIdsIdsByManualOffsettingId(mloId);
        mloLiabilities.forEach(it -> {
            customerLiabilityRepository.findById(it.getId())
                    .map(CustomerLiability::getChildLatePaymentFineId)
                    .ifPresent(childLatePaymentFineId -> {
                        try {
                            latePaymentFineService.reverse(childLatePaymentFineId, false);
                        } catch (NoRollbackForException ignored) {
                        }
                    });
        });

        List<MLOCustomerDeposits> deposits = mloCustomerDepositsRepository.getDepositInfoByManualOffsettingId(mloId);
        if (!deposits.isEmpty()) {
            for (MLOCustomerDeposits deposit : deposits) {
                if (deposit.getLiabilityId() != null) {
                    Optional<CustomerLiability> liability = customerLiabilityRepository.findById(deposit.getLiabilityId());
                    if (liability.isPresent()) {
                        CustomerLiability customerLiability = liability.get();
                        customerLiability.setBlockedForCalculationOfLatePayment(true);
                        customerLiability.setBlockedForCalculationOfLatePaymentFromDate(customerLiability.getCreateDate().toLocalDate());
                        customerLiability.setBlockedForCalculationOfLatePaymentBlockingReasonId(blockingReasonRepository.findByNameAndHardCodedTrue("От системата"));
                        customerLiability.setManualLiabilityOffsettingId(mloId);
                        customerLiabilityRepository.saveAndFlush(customerLiability);
                        entityManager.refresh(customerLiability);

                        CustomerReceivable customerReceivable = customerReceivableService.createFromLiabilityFromManualLiabilityOffsettingReversal(customerLiability);
                        deposit.setReversalReceivableId(customerReceivable.getId());

                        String systemUserId = permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId();
                        if (customerLiability.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                            directOffsetting(
                                    "receivable",
                                    customerReceivable.getId(),
                                    customerLiability.getId(),
                                    systemUserId,
                                    systemUserId
                            );
                        } else {
                            if (customerLiability.getChildLatePaymentFineId() != null) {
                                latePaymentFineService.reverse(customerLiability.getChildLatePaymentFineId(), false);
                            }
                        }

                        entityManager.refresh(customerLiability);
                        entityManager.refresh(customerReceivable);

                        if (customerReceivable.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                            automaticOffsettingService.offsetOfLiabilityAndReceivable(
                                    customerReceivable.getId(),
                                    null,
                                    systemUserId,
                                    systemUserId
                            );
                        }
                    }
                }
            }
        }

        mloCustomerDepositsRepository.saveAllAndFlush(deposits);
        mloReversal(mloId);
    }

    @Transactional
    public void mloReversal(Long mloId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall("CALL receivable.mlo_reverse(?::bigint, to_date(?, 'yyyy-MM-dd'), ?, ?)");
                callableStatement.setString(1, mloId.toString());
                callableStatement.setString(2, LocalDate.now().toString());
                String systemUserId = permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId();
                callableStatement.setString(3, systemUserId);
                callableStatement.setString(4, systemUserId);
                callableStatement.execute();
            });
        } catch (Exception e) {
            throw new ClientException("Some error happened in mlo_reverse procedure [%s];".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
        }
    }

    @Transactional
    public String directOffsetting(
            String sourceType,
            Long sourceId,
            Long liabilityId,
            String systemUserId,
            String modifySystemUserId
    ) {
        AtomicReference<String> message = new AtomicReference<>("UNKNOWN");
        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                CallableStatement callableStatement = connection.prepareCall(
                        "CALL receivable.direct_liability_offsetting(?,?,?,?,?,?,?,?,?,?,?)");

                callableStatement.setString(1, sourceType);
                callableStatement.setLong(2, sourceId);
                callableStatement.setLong(3, liabilityId);
                callableStatement.setString(4, systemUserId);
                callableStatement.setString(5, modifySystemUserId);

                callableStatement.registerOutParameter(6, Types.BIGINT);
                callableStatement.registerOutParameter(7, Types.VARCHAR);

                callableStatement.setNull(8, Types.NUMERIC);
                callableStatement.setNull(9, Types.INTEGER);
                callableStatement.setDate(10, Date.valueOf(LocalDate.now()));
                callableStatement.setString(11, "MLO");

                callableStatement.execute();
                message.set(callableStatement.getString(7));
            });
        } catch (Exception e) {
            throw new ClientException("Some error happened in direct_liability_offsetting procedure [%s]".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
        }
        return message.get();
    }

    public void saveTemplates(
            Set<ReceivableTemplateRequest> templateRequests,
            Long productDetailId,
            List<String> errorMessages
    ) {
        if (CollectionUtils.isEmpty(templateRequests)) {
            return;
        }
        Map<ReceivableTemplateType, List<Long>> requestMap = new HashMap<>();

        for (ReceivableTemplateRequest templateRequest : templateRequests) {
            if (!requestMap.containsKey(templateRequest.getTemplateType())) {
                List<Long> value = new ArrayList<>();
                value.add(templateRequest.getTemplateId());
                requestMap.put(templateRequest.getTemplateType(), value);
            } else {
                requestMap.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
            }
        }

        List<MLOTemplates> productContractTemplates = new ArrayList<>();
        createNewProductTemplates(productDetailId, errorMessages, requestMap, productContractTemplates);
        if (!errorMessages.isEmpty()) {
            return;
        }
        mloTemplatesRepository.saveAll(productContractTemplates);
    }

    private void createNewProductTemplates(
            Long mloId,
            List<String> errorMessages,
            Map<ReceivableTemplateType, List<Long>> requestMap,
            List<MLOTemplates> mloTemplates
    ) {
        AtomicInteger i = new AtomicInteger(0);
        requestMap.forEach((key, value) -> {
            Set<Long> allIdByIdAndLanguages = contractTemplateRepository.findAllIdByIdAndLanguages(value, ContractTemplatePurposes.MANUAL_LIABILITY_OFFSET, List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN), List.of(key.getTemplateType()), ContractTemplateStatus.ACTIVE, LocalDate.now());
            for (Long l : value) {
                if (!allIdByIdAndLanguages.contains(l)) {
                    errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), l));
                    continue;
                }
                mloTemplates.add(new MLOTemplates(l, mloId, key));
            }
        });
    }

    private List<ReceivableTemplateResponse> getTemplateResponse(Long mloId) {
        return mloTemplatesRepository.findForContract(mloId, LocalDate.now());
    }

    /**
     * Updates the deposit amount and creates a liability based on the provided `ManualLiabilityOffsettingSaveResponse`.
     * <p>
     * This method retrieves the `MLOCustomerResultForLiability` entities associated with the provided `saveResponse.getId()`.
     * For each `MLOCustomerResultForLiability` where the `offsetAmountInDepositCurrency` is greater than zero, it:
     * 1. Finds the corresponding `Deposit` entity with an `ACTIVE` status.
     * 2. Updates the `currentAmount` of the `Deposit` by adding the `offsetAmountInDepositCurrency`.
     * 3. Creates a new `Liability` entity using the `customerLiabilityService.createLiabilityFromDeposit()` method.
     * 4. Saves the updated `Deposit` entities.
     *
     * @param saveResponse the `ManualLiabilityOffsettingSaveResponse` containing the necessary information to update the deposits and create liabilities
     */
    private void updateDepositAndCreateLiability(ManualLiabilityOffsettingSaveResponse saveResponse) {
        List<MLOCustomerDeposits> mloCustomerDepositsByManualLiabilityOffsettingId = mloCustomerDepositsRepository.findMLOCustomerDepositsByManualLiabilityOffsettingId(saveResponse.getId());
        mloCustomerDepositsByManualLiabilityOffsettingId.forEach(mloCustomerDeposit -> {
            Long liabilityFromDeposit = customerLiabilityService.createLiabilityFromDeposit(mloCustomerDeposit.getCustomerDepositId(),
                    mloCustomerDeposit.getBeforeCurrentAmount().subtract(mloCustomerDeposit.getAfterCurrentAmount()), new ArrayList<>(), true, saveResponse.getId());
            log.info("Created liability from deposit with id : %s".formatted(liabilityFromDeposit));

            mloCustomerDeposit.setLiabilityId(liabilityFromDeposit);
        });

        mloCustomerDepositsRepository.saveAllAndFlush(mloCustomerDepositsByManualLiabilityOffsettingId);
    }

    public FileContent downloadFile(Long id) {
        Document file = documentsRepository.findById(id)
                .orElseThrow(
                        () ->
                                new DomainEntityNotFoundException(
                                        "file with id %s not found".formatted(id)
                                )
                );
        var content = fileService.downloadFile(
                file.getSignedFileUrl()
        );
        return new FileContent(
                file.getName(),
                content.getByteArray()
        );
    }
}
