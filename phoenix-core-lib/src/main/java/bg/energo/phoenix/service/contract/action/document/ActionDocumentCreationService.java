package bg.energo.phoenix.service.contract.action.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.action.ActionPodModel;
import bg.energo.phoenix.model.documentModels.action.PenaltyDocumentModel;
import bg.energo.phoenix.model.documentModels.action.PenaltyDocumentResponse;
import bg.energo.phoenix.model.documentModels.mlo.Manager;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionSignableDocuments;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyPaymentTerm;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyPaymentTermExclude;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.action.ActionPodRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.action.ActionSignableDocumentRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyPaymentTermRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.AbstractDocumentCreationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@Slf4j
public class ActionDocumentCreationService extends AbstractDocumentCreationService {
    private static final String FOLDER_PATH = "action_files";
    private static final String penaltyTopicHardcodedValueName = "Penalty";
    private final CustomerRepository customerRepository;
    private final ActionRepository actionRepository;
    private final ActionPodRepository actionPodRepository;
    private final PenaltyPaymentTermRepository penaltyPaymentTermRepository;
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ActionSignableDocumentRepository actionSignableDocumentRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final FileArchivationService fileArchivationService;
    private final EDMSAttributeProperties edmsAttributeProperties;

    public ActionDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            CustomerRepository customerRepository,
            ActionRepository actionRepository,
            ActionPodRepository actionPodRepository,
            PenaltyPaymentTermRepository penaltyPaymentTermRepository,
            CalendarRepository calendarRepository,
            HolidaysRepository holidaysRepository,
            CustomerDetailsRepository customerDetailsRepository,
            ActionSignableDocumentRepository actionSignableDocumentRepository,
            EmailCommunicationService emailCommunicationService,
            EmailMailboxesRepository emailMailboxesRepository,
            TopicOfCommunicationRepository topicOfCommunicationRepository,
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            FileArchivationService fileArchivationService,
            EDMSAttributeProperties edmsAttributeProperties
    ) {
        super(
                contractTemplateDetailsRepository,
                contractTemplateRepository,
                documentGenerationService,
                documentGenerationUtil,
                documentsRepository,
                companyDetailRepository,
                companyLogoRepository,
                signerChainManager,
                fileService
        );
        this.customerRepository = customerRepository;
        this.actionRepository = actionRepository;
        this.actionPodRepository = actionPodRepository;
        this.penaltyPaymentTermRepository = penaltyPaymentTermRepository;
        this.calendarRepository = calendarRepository;
        this.holidaysRepository = holidaysRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.actionSignableDocumentRepository = actionSignableDocumentRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
        this.fileArchivationService = fileArchivationService;
        this.edmsAttributeProperties = edmsAttributeProperties;
    }

    @Transactional
    public void generateActionPenaltyDocumentAndSendEmail(Long actionId) {
        PenaltyDocumentResponse penaltyDocumentResponse = actionRepository.fetchPenaltyDocResponseByActionId(actionId);

        List<Manager> managers = customerRepository.getManagersByCustomer(penaltyDocumentResponse.getCustomerDetailId());

        List<ActionPodModel> actionPodModels = new ArrayList<>();
        if (penaltyDocumentResponse.getPcDetailId() != null) {
            actionPodModels = actionPodRepository.fetchActionPodsForPenaltyDoc(actionId, penaltyDocumentResponse.getPcDetailId());
        }

        LocalDate penaltyDueDate = calculateDueDate(penaltyDocumentResponse.getActionExecutionDate(), penaltyDocumentResponse.getPenaltyId(), penaltyDocumentResponse.getPenaltyPaymentDueDate());
        PenaltyDocumentModel penaltyDocumentModel = buildModelWithSharedInfo(new PenaltyDocumentModel());
        penaltyDocumentModel.from(penaltyDocumentResponse, managers, actionPodModels, penaltyDueDate);

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .fetchActionPenaltyDocumentTemplate(actionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("cannot find Action/Penalty template to generate document;"));

        LocalDate currentDate = LocalDate.now();
        String destinationPath = buildFileDestinationPath();
        log.debug("destination path for template file: {}", destinationPath);
        ByteArrayResource templateFileResource;
        Document document;
        Document signableDoc;
        try {
            templateFileResource = createTemplateFileResource(templateDetail);
            log.debug("Clone of template file created");

            String fileName = formatFileName(penaltyDocumentResponse, templateDetail);
            log.debug("File name was formatted: [%s]".formatted(fileName));

            log.debug("Generating document");
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            penaltyDocumentModel,
                            Set.of(FileFormat.PDF),
                            false
                    );
            log.debug("Document was created successfully");

            log.debug("Saving action document entity");

            String generatedDocumentPath = documentGenerationUtil.getGeneratedDocumentPath(FileFormat.PDF, documentPathPayloads);
            document = saveDocuments(String.format("%s.%s", fileName, FileFormat.PDF.suffix),
                    generatedDocumentPath,
                    penaltyDocumentResponse.getLastServiceContractDetailId(),
                    penaltyDocumentResponse.getLastProductContractDetailId(),
                    penaltyDocumentResponse.getActionId(),
                    templateDetail.getTemplateId(),
                    getDocumentSigners(templateDetail));
            setAttributesToDocument(penaltyDocumentResponse, document);
            fileArchivationService.archiveDocument(document);
            documentsRepository.saveAndFlush(document);

        } catch (Exception e) {
            log.error("exception while storing template file: {}", e.getMessage());
            throw new ClientException("exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
        List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
        if (!documentSigners.contains(DocumentSigners.NO)) {
            document.setSignedFile(true);
            List<Attribute> attributes = document.getAttributes();
            if (!attributes.isEmpty()) {
                attributes.stream()
                        .filter(it -> it.getAttributeGuid().equals(edmsAttributeProperties.getSignedGuid()))
                        .findFirst()
                        .ifPresent(it -> it.setValue(true));
                document.setAttributes(attributes);
            }
            signerChainManager.startSign(List.of(document));
            fileArchivationService.archiveDocument(document);
            documentsRepository.saveAndFlush(document);
        }
        sendEmail(document.getSignedFileUrl(), document.getName(), document.getTemplateId(), penaltyDocumentModel, penaltyDocumentResponse, actionId);
    }

    private void setAttributesToDocument(PenaltyDocumentResponse penaltyDocumentResponse, Document document) {
        Optional<Customer> customer = customerRepository
                .findByCustomerDetailIdAndStatusIn(
                        penaltyDocumentResponse.getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                );

        document.setNeedArchive(true);
        document.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT);
        document.setArchivedFileType(EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT.getValue());
        document.setAttributes(
                List.of(
                        new Attribute(edmsAttributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT),
                        new Attribute(edmsAttributeProperties.getDocumentNumberGuid(), document.getName()),
                        new Attribute(edmsAttributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                        new Attribute(edmsAttributeProperties.getCustomerIdentifierGuid(), customer.<Object>map(Customer::getIdentifier).orElse(null)),
                        new Attribute(edmsAttributeProperties.getCustomerNumberGuid(), customer.map(Customer::getCustomerNumber).orElse(null)),
                        new Attribute(edmsAttributeProperties.getSignedGuid(), false)
                )
        );
        document.setSignedFile(false);
    }

    @SneakyThrows
    private void sendEmail(String url, String name,
                           Long documentTemplateId,
                           PenaltyDocumentModel penaltyDocumentModel,
                           PenaltyDocumentResponse documentResponse,
                           Long actionId) {
        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository.fetchActionPenaltyEmailTemplate(actionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("cannot find Action/Penalty email template;"));
        LocalDate currentDate = LocalDate.now();
        String destinationPath = buildFileDestinationPath();
        log.debug("destination path for template file: {}", destinationPath);
        ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
        log.debug("Clone of template file created");
        String fileName = String.format(UUID.randomUUID().toString(), ".docx");
        log.debug("File name was formatted: [%s]".formatted(fileName));
        log.debug("Generating document");
        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                templateFileResource,
                destinationPath,
                fileName,
                penaltyDocumentModel,
                Set.of(FileFormat.DOCX),
                false
        );
        ByteArrayResource downloadedFile = getFileService().downloadFile(documentPathPayloads.docXPath());

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue(penaltyTopicHardcodedValueName, NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found for type: %s".formatted(penaltyTopicHardcodedValueName)));
        log.debug("Topic of communication id: %s".formatted(topicOfCommunication.getId()));

        EmailMailboxes emailMailboxes = emailMailboxesRepository
                .findByEmailForSendingInvoicesTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mail box not found"));

        log.debug("Creating email communication attachment");
        EmailCommunicationAttachment attachment = emailCommunicationAttachmentRepository
                .save(EmailCommunicationAttachment
                        .builder()
                        .name(name)
                        .fileUrl(url)
                        .status(EntityStatus.ACTIVE)
                        .build());
        log.debug("Email communication attachment id: %s".formatted(attachment.getId()));

        log.debug("Creating email communication");
        emailCommunicationService.createEmailFromDocument(
                DocumentEmailCommunicationCreateRequest
                        .builder()
                        .communicationTopicId(topicOfCommunication.getId())
                        .emailBoxId(emailMailboxes.getId())
                        .customerEmailAddress(documentResponse.getEmails())
                        .emailSubject(templateDetail.getSubject())
                        .emailBody(DocumentParserService.parseDocxToHtml(downloadedFile.getContentAsByteArray()))
                        .customerDetailId(documentResponse.getCustomerDetailId())
                        .customerCommunicationId(documentResponse.getCustomerCommunicationId())
                        .attachmentFileIds(Set.of(attachment.getId()))
                        .emailTemplateId(templateDetail.getTemplateId())
                        .templateIds(Set.of(documentTemplateId))
                        .build(),
                false
        );
    }

    private Document saveDocuments(String name, String generatedDocumentPath, Long lastServiceContractDetailId, Long lastProductContractDetailId, Long actionId, Long templateId, List<DocumentSigners> documentSigners) {

        if (generatedDocumentPath == null) return null;

        Document document = Document.builder()
                .signers(documentSigners)
                .signedBy(new ArrayList<>())
                .name(name)
                .unsignedFileUrl(generatedDocumentPath)
                .signedFileUrl(generatedDocumentPath)
                .fileFormat(FileFormat.PDF)
                .templateId(templateId)
                .documentStatus(DocumentStatus.UNSIGNED)
                .status(EntityStatus.ACTIVE)
                .build();
        Document savedDoc = documentsRepository.saveAndFlush(document);
        actionSignableDocumentRepository.save(ActionSignableDocuments.builder()
                .serviceContractDetailId(lastServiceContractDetailId)
                .productContractDetailId(lastProductContractDetailId)
                .documentId(savedDoc.getId())
                .actionId(actionId)
                .status(EntityStatus.ACTIVE)
                .build());
        return savedDoc;
    }

    private String formatFileName(PenaltyDocumentResponse penaltyDocumentResponse, ContractTemplateDetail contractTemplateDetail) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(ObjectUtils.defaultIfNull(contractTemplateDetail.getFileNamePrefix(), ""));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(penaltyDocumentResponse, contractTemplateDetail));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(penaltyDocumentResponse.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "Product_Contract" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    private String extractFileName(PenaltyDocumentResponse penaltyDocumentResponse, ContractTemplateDetail contractTemplateDetail) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return penaltyDocumentResponse.getContractNumber();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();

                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER ->
                                nameParts.add(getCustomer(penaltyDocumentResponse.getCustomerDetailId(), customerCache).getKey().getIdentifier());
                        case CUSTOMER_NAME -> {
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(penaltyDocumentResponse.getCustomerDetailId(), customerCache);
                            Customer customer = customerPair.getKey();
                            CustomerDetails customerDetails = customerPair.getValue();

                            switch (customer.getCustomerType()) {
                                case LEGAL_ENTITY -> {
                                    String customerName = "%s_%s".formatted(
                                            customerDetails.getName(),
                                            "LEGAL_ENTITY"
                                    );
                                    nameParts.add(customerName.substring(0, Math.min(customerName.length(), 64)));
                                }
                                case PRIVATE_CUSTOMER -> {
                                    String customerName = "%s_%s_%s".formatted(
                                            customerDetails.getName(),
                                            customerDetails.getMiddleName(),
                                            customerDetails.getLastName()
                                    );
                                    nameParts.add(customerName.substring(0, Math.min(customerName.length(), 64)));
                                }
                            }
                        }
                        case CUSTOMER_NUMBER -> {
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(penaltyDocumentResponse.getCustomerDetailId(), customerCache);
                            Customer customer = customerPair.getKey();

                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(penaltyDocumentResponse.getContractNumber());
                        case FILE_ID -> nameParts.add(String.valueOf(documentsRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(penaltyDocumentResponse.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return penaltyDocumentResponse.getContractNumber();
        }
    }

    private Pair<Customer, CustomerDetails> getCustomer(Long customerDetailId, Map<Long, Pair<Customer, CustomerDetails>> customerCache) {
        if (!customerCache.containsKey(customerDetailId)) {
            Customer customer = customerRepository
                    .findByCustomerDetailIdAndStatusIn(customerDetailId, Arrays.stream(CustomerStatus.values()).toList())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));
            CustomerDetails customerDetails = customerDetailsRepository.findById(customerDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));

            customerCache.put(customerDetailId, Pair.of(customer, customerDetails));
        }

        return Pair.of(customerCache.get(customerDetailId).getKey(), customerCache.get(customerDetailId).getValue());
    }

    private LocalDate calculateDueDate(LocalDate actionExecutionDate, Long penaltyId, LocalDate penaltyPaymentDueDate) {
        if (Objects.nonNull(penaltyId) && Objects.isNull(penaltyPaymentDueDate)) {
            PenaltyPaymentTerm term = penaltyPaymentTermRepository.findByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Payment term not found for penalty"));
            Integer days = term.getValue();
            LocalDate endDate = actionExecutionDate;
            List<PenaltyPaymentTermExclude> excludes = term.getPenaltyPaymentTermExcludes();
            DueDateChange dueDateChange = term.getDueDateChange();

            Long calendarId = term.getCalendarId();
            Calendar calendar = calendarRepository
                    .findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Presented Payment Term calendar not found;"));
            List<DayOfWeek> weekends = Arrays.stream(
                            Objects.requireNonNullElse(calendar.getWeekends(), "")
                                    .split(";")
                    )
                    .filter(StringUtils::isNotBlank)
                    .map(DayOfWeek::valueOf)
                    .toList();
            List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarId, List.of(HolidayStatus.ACTIVE));

            switch (term.getCalendarType()) {
                case CALENDAR_DAYS -> {
                    weekends = excludes.contains(PenaltyPaymentTermExclude.WEEKENDS) ? weekends : new ArrayList<>();
                    holidays = excludes.contains(PenaltyPaymentTermExclude.HOLIDAYS) ? holidays : new ArrayList<>();
                    endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(days, endDate, weekends, holidays);
                    endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
                }
                case WORKING_DAYS ->
                        endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(days, endDate, weekends, holidays);
                case CERTAIN_DAYS -> {
                    endDate = PaymentTermUtils.calculateEndDateForCertainDays(days, endDate);
                    endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
                }
            }
            return endDate;
        }
        return penaltyPaymentDueDate;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
