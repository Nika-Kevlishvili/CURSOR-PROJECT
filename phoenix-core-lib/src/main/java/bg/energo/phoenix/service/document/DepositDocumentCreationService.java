package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.DepositDocumentModel;
import bg.energo.phoenix.model.documentModels.ManagerDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.*;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ManualLiabilityOffsetting;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.model.response.customer.CustomerAddressResponse;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.deposit.*;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;

@Service
@Slf4j
public class DepositDocumentCreationService extends AbstractDocumentCreationService {

    private static final String FOLDER_PATH = "deposit_document";
    private final DepositRepository depositRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final DepositProductContractRepository depositProductContractRepository;
    private final DepositServiceContractRepository depositServiceContractRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ManagerRepository managerRepository;
    private final DepositTemplateRepository depositTemplateRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository;
    private final DepositDocumentFileRepository depositDocumentFileRepository;

    public DepositDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            DepositRepository depositRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            CurrencyRepository currencyRepository,
            CustomerRepository customerRepository,
            CustomerDetailsRepository customerDetailsRepository,
            DepositProductContractRepository depositProductContractRepository,
            DepositServiceContractRepository depositServiceContractRepository,
            ProductContractRepository productContractRepository,
            ServiceContractsRepository serviceContractsRepository,
            ProductContractDetailsRepository productContractDetailsRepository,
            ServiceContractDetailsRepository serviceContractDetailsRepository,
            ManagerRepository managerRepository,
            DepositTemplateRepository depositTemplateRepository,
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            EmailCommunicationService emailCommunicationService,
            TopicOfCommunicationRepository topicOfCommunicationRepository,
            EmailMailboxesRepository emailMailboxesRepository,
            CustomerCommunicationContactsRepository customerCommunicationContactsRepository,
            ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository,
            DepositDocumentFileRepository depositDocumentFileRepository
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
        this.depositRepository = depositRepository;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.currencyRepository = currencyRepository;
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.depositProductContractRepository = depositProductContractRepository;
        this.depositServiceContractRepository = depositServiceContractRepository;
        this.productContractRepository = productContractRepository;
        this.serviceContractsRepository = serviceContractsRepository;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.serviceContractDetailsRepository = serviceContractDetailsRepository;
        this.managerRepository = managerRepository;
        this.depositTemplateRepository = depositTemplateRepository;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.customerCommunicationContactsRepository = customerCommunicationContactsRepository;
        this.manualLiabilityOffsettingRepository = manualLiabilityOffsettingRepository;
        this.depositDocumentFileRepository = depositDocumentFileRepository;
    }

    public List<Long> generateDocuments(Long depositId, Long mloId) {
        DepositDocumentModel depositDocumentModel = buildModelWithSharedInfo(new DepositDocumentModel());
        mapDepositJsonTemplate(depositId, depositDocumentModel);
        String destinationPath = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());

        Deposit deposit = depositRepository
                .findById(depositId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Deposit not found by ID: %s".formatted(depositId)));

        ManualLiabilityOffsetting manualLiabilityOffsetting = manualLiabilityOffsettingRepository
                .findById(mloId)
                .orElseThrow(() -> new DomainEntityNotFoundException("MLO not found with id " + mloId));

        Customer customer = customerRepository
                .findById(manualLiabilityOffsetting.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found!"));

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(manualLiabilityOffsetting.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("CustomerDetails not found!"));

        List<DepositTemplate> depositTemplates = depositTemplateRepository.findByDepositId(deposit.getId());

        Set<Long> attachmentIds = processDocumentTemplates(
                depositTemplates
                        .stream()
                        .filter(template -> template.getType().equals(ReceivableTemplateType.DOCUMENT))
                        .collect(Collectors.toList()),
                depositDocumentModel,
                destinationPath,
                deposit,
                manualLiabilityOffsetting,
                customer,
                FileFormat.PDF
        );

        List<Long> emailIds = processEmailTemplates(
                depositTemplates
                        .stream()
                        .filter(template -> template.getType().equals(ReceivableTemplateType.EMAIL))
                        .collect(Collectors.toList()),
                depositDocumentModel,
                attachmentIds,
                destinationPath,
                deposit,
                manualLiabilityOffsetting,
                customerDetails,
                FileFormat.DOCX,
                depositTemplates
                        .stream()
                        .filter(template -> template.getType().equals(ReceivableTemplateType.DOCUMENT))
                        .map(DepositTemplate::getTemplateId)
                        .collect(Collectors.toSet())
        );

        if (emailIds.isEmpty())
            throw new ClientException("No emails were created", APPLICATION_ERROR);

        return emailIds;
    }

    private Set<Long> processDocumentTemplates(List<DepositTemplate> documentTemplates, DepositDocumentModel model, String destinationPath,
                                               Deposit deposit, ManualLiabilityOffsetting mlo, Customer customer, FileFormat fileFormat) {
        Set<Long> attachmentIds = new HashSet<>();
        List<Document> documentList = new ArrayList<>();
        Map<Long, EmailCommunicationAttachment> documentToAttachmentMap = new HashMap<>();

        for (DepositTemplate documentTemplate : documentTemplates) {
            try {
                DocumentGenerationResult result = generateDocument(documentTemplate, model, destinationPath, deposit, fileFormat, mlo.getCustomerDetailId());

                if (result.templateDetail().getCustomerType() != null
                        && !result.templateDetail().getCustomerType().isEmpty()
                        && !result.templateDetail().getCustomerType().contains(customer.getCustomerType())) {
                    log.info("Skipping, not appropriate customer Type!");
                    continue;
                }

                EmailCommunicationAttachment attachment = new EmailCommunicationAttachment();
                attachment.setName(result.fileName() + "." +result.document.getFileFormat().suffix);
                attachment.setFileUrl(result.document().getSignedFileUrl());
                attachment.setStatus(EntityStatus.ACTIVE);
                attachment = emailCommunicationAttachmentRepository.saveAndFlush(attachment);
                attachmentIds.add(attachment.getId());

                if (result.document() != null) {
                    documentList.add(result.document());
                    documentToAttachmentMap.put(result.document().getId(), attachment);
                }

            } catch (Exception e) {
                log.error("Exception while processing document template {}: {}", documentTemplate.getTemplateId(), e.getMessage());
                throw new ClientException("Exception while processing document template: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        if (!documentList.isEmpty()) {
            signerChainManager.startSign(documentList);

            for (Document document : documentList) {
                EmailCommunicationAttachment attachment = documentToAttachmentMap.get(document.getId());
                if (attachment != null) {
                    attachment.setFileUrl(document.getSignedFileUrl());
                    emailCommunicationAttachmentRepository.saveAndFlush(attachment);
                }
            }
        }

        return attachmentIds;
    }

    private List<Long> processEmailTemplates(
            List<DepositTemplate> emailTemplates,
            DepositDocumentModel model,
            Set<Long> attachmentIds,
            String destinationPath,
            Deposit deposit,
            ManualLiabilityOffsetting mlo,
            CustomerDetails customerDetails,
            FileFormat fileFormat,
            Set<Long> templateIds
    ) {
        List<Long> emailIds = new ArrayList<>();

        List<CustomerCommunicationContacts> communicationContacts = customerCommunicationContactsRepository.findByCustomerCommIdContactTypesAndStatuses(
                mlo.getCustomerCommunicationId(),
                List.of(CustomerCommContactTypes.EMAIL),
                List.of(Status.ACTIVE)
        );

        for (DepositTemplate emailTemplate : emailTemplates) {
            try {
                DocumentGenerationResult result = generateDocument(emailTemplate, model, destinationPath, deposit, fileFormat, mlo.getCustomerDetailId());

                Set<Long> emailAttachmentIds = new HashSet<>();
                for (Long originalAttachmentId : attachmentIds) {
                    EmailCommunicationAttachment originalAttachment = emailCommunicationAttachmentRepository
                            .findById(originalAttachmentId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Attachment not found: " + originalAttachmentId));

                    EmailCommunicationAttachment newAttachment = new EmailCommunicationAttachment();
                    newAttachment.setName(originalAttachment.getName());
                    newAttachment.setFileUrl(originalAttachment.getFileUrl());
                    newAttachment.setStatus(EntityStatus.ACTIVE);
                    emailAttachmentIds.add(emailCommunicationAttachmentRepository.saveAndFlush(newAttachment).getId());
                }

                StringBuilder emailBuilder = new StringBuilder();
                for (int i = 0; i < communicationContacts.size(); i++) {
                    emailBuilder.append(communicationContacts.get(i).getContactValue());
                    if (i != communicationContacts.size() - 1) {
                        emailBuilder.append(";");
                    }
                }

                DocumentEmailCommunicationCreateRequest request = new DocumentEmailCommunicationCreateRequest();
                request.setEmailSubject(result.templateDetail().getSubject());

                ByteArrayResource emailDocument = getFileService().downloadFile(result.documentPaths().docXPath());
                String emailBody = parseDocx(emailDocument.getByteArray());
                request.setEmailBody(emailBody);
                request.setAttachmentFileIds(emailAttachmentIds);
                request.setCustomerDetailId(customerDetails.getId());
                request.setCustomerCommunicationId(mlo.getCustomerCommunicationId());
                request.setCustomerEmailAddress(emailBuilder.toString());
                request.setEmailTemplateId(emailTemplate.getTemplateId());
                request.setTemplateIds(templateIds);

                findAndSetCommunicationTopicAndEmailBox(request);

                log.debug("Creating email from template: {}", emailTemplate.getTemplateId());
                Long emailId = emailCommunicationService.createEmailFromDocument(request, true);
                emailIds.add(emailId);
                log.debug("Email created successfully with id: {}", emailId);

            } catch (Exception e) {
                log.error("Error processing email template {}: {}", emailTemplate.getTemplateId(), e.getMessage());
                throw new ClientException("Error processing email template: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        return emailIds;
    }

    private void findAndSetCommunicationTopicAndEmailBox(DocumentEmailCommunicationCreateRequest request) {
        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue("Deposit fulfill", NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication with name Deposit fulfill was not found"));

        request.setCommunicationTopicId(topicOfCommunication.getId());

        Optional<EmailMailboxes> emailBoxOptional = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();
        if (emailBoxOptional.isPresent()) {
            request.setEmailBoxId(emailBoxOptional.get().getId());
        } else {
            Optional<EmailMailboxes> hardCodedEmail = emailMailboxesRepository.findByName("HardCoded Email");
            hardCodedEmail.ifPresent(emailMailboxes -> request.setEmailBoxId(emailMailboxes.getId()));
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }

    private DocumentGenerationResult generateDocument(
            DepositTemplate template,
            DepositDocumentModel model,
            String destinationPath,
            Deposit deposit,
            FileFormat fileFormat,
            Long customerDetailsId
    ) throws Exception {
        ContractTemplateDetail templateDetail = getContractTemplateLastDetails(template.getTemplateId());

        String fileName = formatDocumentFileName(deposit, templateDetail, customerDetailsId);
        String fullFileName = String.format("%s.%s", fileName, fileFormat.suffix);

        ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);

        log.debug("Generating document for template: {}", template.getTemplateId());
        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                templateFileResource,
                destinationPath,
                fileName,
                model,
                Set.of(fileFormat),
                false
        );
        log.debug("Document was created successfully");

        Document savedDocument = null;
        if (fileFormat == FileFormat.PDF) {
            savedDocument = saveDocuments(
                    fullFileName,
                    documentPathPayloads.pdfPath(),
                    fileFormat,
                    getDocumentSigners(template, templateDetail),
                    deposit.getId(),
                    template.getTemplateId()
            );
        }

        return new DocumentGenerationResult(documentPathPayloads, templateDetail, null, fileName, savedDocument);
    }

    private List<DocumentSigners> getDocumentSigners(DepositTemplate template, ContractTemplateDetail templateDetail) {
        return Optional.ofNullable(templateDetail.getFileSigning())
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }

    private Document saveDocuments(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long depositId,
            Long templateId) {

        if (ftpPath == null) {
            return null;
        }

        Document document = Document.builder()
                .signers(documentSigners)
                .signedBy(new ArrayList<>())
                .name(fileName)
                .unsignedFileUrl(ftpPath)
                .signedFileUrl(ftpPath)
                .fileFormat(fileFormat)
                .templateId(templateId)
                .documentStatus(FileFormat.PDF.equals(fileFormat) ? DocumentStatus.UNSIGNED : DocumentStatus.SIGNED)
                .status(EntityStatus.ACTIVE)
                .build();

        Document savedDoc = documentsRepository.saveAndFlush(document);

        DepositDocumentFile documentFile = depositDocumentFileRepository.save(
                DepositDocumentFile.builder()
                        .name(fileName)
                        .fileUrl(ftpPath)
                        .depositId(depositId)
                        .documentId(savedDoc.getId())
                        .status(EntityStatus.ACTIVE)
                        .build()
        );

        return savedDoc;
    }

    public String formatDocumentFileName(Deposit deposit, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(deposit, contractTemplateDetail, customerDetailId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(deposit.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "DEPOSIT" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    private String extractFileName(Deposit deposit, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (CollectionUtils.isEmpty(fileName)) {
                return deposit.getId().toString();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();

                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> {
                            String identifier = customerRepository.getCustomerIdentifierByCustomerDetailId(customerDetailId);
                            nameParts.add(identifier);
                        }
                        case CUSTOMER_NAME -> {
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(customerDetailId, customerCache);
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
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(customerDetailId, customerCache);
                            Customer customer = customerPair.getKey();
                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(deposit.getId().toString());
                        case FILE_ID -> nameParts.add(String.valueOf(depositDocumentFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(deposit.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return deposit.getId().toString();
        }
    }

    private void mapDepositJsonTemplate(Long depositId, DepositDocumentModel depositDocumentModel) {
        Deposit deposit = depositRepository
                .findByIdAndStatusIn(depositId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Deposit not found by ID: %s;".formatted(depositId)));

        CustomerLiability customerLiability = customerLiabilityRepository
                .findByDepositIdOrderByCreateDateDesc(depositId).stream().findFirst()
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Liability not found by deposit ID: %s;".formatted(depositId)));

        Currency customerLiabilityCurrency = currencyRepository
                .findById(customerLiability.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found by ID: %s".formatted(customerLiability.getCurrencyId())));

        Currency currency = currencyRepository
                .findById(deposit.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found by ID: %s".formatted(deposit.getCurrencyId())));

        Customer customer = customerRepository
                .findById(deposit.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found by ID: %s".formatted(deposit.getCustomerId())));

        CustomerDetails customerDetails = getCustomerDetailsForDeposit(deposit, customer);

        CustomerAddressResponse customerInfo = customerDetailsRepository
                .findCustomerAddressInfo(customerDetails.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer address info not found"));

        List<Manager> managers = managerRepository.findManagersByCustomerDetailId(
                customerDetails.getId(),
                List.of(Status.ACTIVE)
        );

        depositDocumentModel.fillDepositData(
                LocalDateTime.now(),
                customerLiability.getCreateDate(),
                customerLiability.getDueDate(),
                customerLiability.getInitialAmount(),
                customerLiability.getCurrentAmount(),
                customerLiability.getInitialAmountInOtherCurrency(),
                customerLiability.getCurrentAmountInOtherCurrency(),
                customerLiabilityCurrency.getPrintName(),
                deposit.getInitialAmount(),
                deposit.getInitialAmount().multiply(currency.getAltCurrencyExchangeRate()),
                currency.getPrintName(),
                customer.getIdentifier(),
                customerInfo.getCustomerNameComb(),
                customerInfo.getCustomerNameCombTrsl(),
                customerInfo.getCustomerNumber(),
                customerInfo.getAddressComb(),
                customerInfo.getPopulatedPlace(),
                customerInfo.getZipCode(),
                managers.stream()
                        .map(manager -> new ManagerDocumentModel(
                                manager.getTitle().getName(),
                                manager.getName(),
                                manager.getSurname(),
                                manager.getJobPosition()
                        ))
                        .collect(Collectors.toList()),
                deposit.getDepositNumber(),
                deposit.getCreateDate(),
                convertAmountToWords(deposit.getInitialAmount()),
                convertAmountToWords(customerLiability.getInitialAmount())
        );
    }

    private CustomerDetails getCustomerDetailsForDeposit(Deposit deposit, Customer customer) {
        List<DepositProductContract> depositProductContracts = depositProductContractRepository
                .findDepositProductContractByDepositIdAndStatus(deposit.getId(), List.of(EntityStatus.ACTIVE));

        if (!depositProductContracts.isEmpty()) {
            DepositProductContract productContract = depositProductContracts.get(0);
            ProductContract contract = productContractRepository
                    .findById(productContract.getContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find product contract with id: %s".formatted(productContract.getContractId())));

            ProductContractDetails productContractDetails = productContractDetailsRepository
                    .findCurrentProductContractDetails(contract.getId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find product contract details with contract id: %s".formatted(contract.getId())));

            return customerDetailsRepository
                    .findByCustomerIdAndVersionId(customer.getId(), productContractDetails.getCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details not found for customer ID: %s and version: %s".formatted(customer.getId(), productContractDetails.getCustomerDetailId())));
        }

        List<DepositServiceContract> depositServiceContracts = depositServiceContractRepository
                .findDepositServiceContractByContractIdAndStatus(deposit.getId(), List.of(EntityStatus.ACTIVE));

        if (!depositServiceContracts.isEmpty()) {
            DepositServiceContract serviceContract = depositServiceContracts.get(0);
            ServiceContracts contract = serviceContractsRepository
                    .findById(serviceContract.getContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find service contract with id: %s".formatted(serviceContract.getContractId())));

            ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                    .findCurrentServiceContractDetails(contract.getId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find service contract details with contract id: %s".formatted(contract.getId())));

            return customerDetailsRepository
                    .findByCustomerIdAndVersionId(customer.getId(), serviceContractDetails.getCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details not found for customer ID: %s and version: %s".formatted(customer.getId(), serviceContractDetails.getCustomerDetailId())));
        }

        return customerDetailsRepository
                .findById(customer.getLastCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details not found by ID: %s".formatted(customer.getLastCustomerDetailId())));
    }

    private record DocumentGenerationResult(
            DocumentPathPayloads documentPaths,
            ContractTemplateDetail templateDetail,
            Long attachmentId,
            String fileName,
            Document document
    ) {
    }

}
