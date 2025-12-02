package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.entity.template.ProductTemplate;
import bg.energo.phoenix.model.entity.template.ServiceTemplate;
import bg.energo.phoenix.model.enums.billing.billings.SendingAnInvoice;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceConnectionType;
import bg.energo.phoenix.model.enums.contract.billing.BillingGroupSendingInvoice;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.template.*;
import bg.energo.phoenix.service.billing.billingRun.errors.InvoiceErrorShortObject;
import bg.energo.phoenix.service.billing.model.impl.BillingRunDocumentModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.AbstractDocumentCreationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class BillingRunEmailSenderService extends AbstractDocumentCreationService {
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final ServiceTemplateRepository serviceTemplateRepository;
    private final DocumentGenerationService documentGenerationService;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final CustomerCommunicationContactsRepository communicationContactsRepository;
    private final BillingRunDocumentDataCreationService billingRunDocumentDataCreationService;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;

    private final String invoiceTopicHardcodedValueName = "Invoice";
    private static final String FOLDER_PATH = "email_body";

    public BillingRunEmailSenderService(ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                        ContractTemplateRepository contractTemplateRepository,
                                        DocumentGenerationService documentGenerationService,
                                        DocumentGenerationUtil documentGenerationUtil,
                                        DocumentsRepository documentsRepository,
                                        CompanyDetailRepository companyDetailRepository,
                                        CompanyLogoRepository companyLogoRepository,
                                        SignerChainManager signerChainManager,
                                        FileService fileService,
                                        EmailMailboxesRepository emailMailboxesRepository,
                                        ProductDetailsRepository productDetailsRepository,
                                        ServiceDetailsRepository serviceDetailsRepository,
                                        ProductTemplateRepository productTemplateRepository,
                                        EmailCommunicationService emailCommunicationService,
                                        ServiceTemplateRepository serviceTemplateRepository,
                                        DocumentGenerationService documentGenerationService1,
                                        ContractTemplateFileRepository contractTemplateFileRepository,
                                        TopicOfCommunicationRepository topicOfCommunicationRepository,
                                        ContractBillingGroupRepository contractBillingGroupRepository,
                                        CustomerCommunicationsRepository customerCommunicationsRepository,
                                        ProductContractDetailsRepository productContractDetailsRepository,
                                        ServiceContractDetailsRepository serviceContractDetailsRepository,
                                        CustomerCommunicationContactsRepository communicationContactsRepository,
                                        BillingRunDocumentDataCreationService billingRunDocumentDataCreationService,
                                        EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository) {
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
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.productDetailsRepository = productDetailsRepository;
        this.serviceDetailsRepository = serviceDetailsRepository;
        this.productTemplateRepository = productTemplateRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.serviceTemplateRepository = serviceTemplateRepository;
        this.documentGenerationService = documentGenerationService1;
        this.contractTemplateFileRepository = contractTemplateFileRepository;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.contractBillingGroupRepository = contractBillingGroupRepository;
        this.customerCommunicationsRepository = customerCommunicationsRepository;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.serviceContractDetailsRepository = serviceContractDetailsRepository;
        this.communicationContactsRepository = communicationContactsRepository;
        this.billingRunDocumentDataCreationService = billingRunDocumentDataCreationService;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createEmailFromInvoiceAndSend(Invoice invoice, BillingRun billingRun, List<InvoiceErrorShortObject> exceptionContext) {
        SendingAnInvoice sendingAnInvoice = billingRun.getSendingAnInvoice();
        log.debug("Sending an invoice: %s".formatted(sendingAnInvoice));

        boolean sendingAnInvoiceIsOnPaper = !List.of(SendingAnInvoice.EMAIL, SendingAnInvoice.ACCORDING_TO_THE_CONTRACT).contains(sendingAnInvoice);
        if (sendingAnInvoiceIsOnPaper) {
            log.debug("Sending and invoice is: {}, not need to send emails", sendingAnInvoice);
            return;
        }

        ContractTemplateDetail emailTemplate = defineEmailTemplate(billingRun, invoice);
        log.debug("Email template id: %s".formatted(emailTemplate.getId()));

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue(invoiceTopicHardcodedValueName, NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found for type: %s".formatted(invoiceTopicHardcodedValueName)));
        log.debug("Topic of communication id: %s".formatted(topicOfCommunication.getId()));

        EmailMailboxes emailMailboxes = emailMailboxesRepository
                .findByEmailForSendingInvoicesTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mail box not found"));
        log.debug("Email mail box id: %s".formatted(emailMailboxes.getId()));

        InvoiceConnectionType invoiceType = invoice.type();
        log.debug("Invoice type: %s".formatted(invoiceType));

        log.debug("Defining customer communication contacts");
        Map<Long, List<CustomerCommunicationContacts>> customerAndCommunicationContactsMap = findEmailCommunicationContactsFromInvoice(invoice, sendingAnInvoice, exceptionContext);

        log.debug("Sending emails for communication contacts, num: {}", customerAndCommunicationContactsMap.size());
        sendMailsOnCommunicationContacts(invoice, emailTemplate, topicOfCommunication, emailMailboxes, customerAndCommunicationContactsMap);
    }

    private void sendMailsOnCommunicationContacts(Invoice invoice,
                                                  ContractTemplateDetail emailTemplate,
                                                  TopicOfCommunication topicOfCommunication,
                                                  EmailMailboxes emailMailboxes,
                                                  Map<Long, List<CustomerCommunicationContacts>> customerAndCommunicationContactsMap) {
        try {
            ContractTemplateFiles emailTemplateFile = contractTemplateFileRepository
                    .findByIdAndStatus(emailTemplate.getTemplateFileId(), EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template file with id: %s not found".formatted(emailTemplate.getTemplateFileId())));
            CompanyDetailedInformationModel companyDetailedInformation = fetchCompanyDetailedInformationModel(LocalDate.now());
            byte[] companyLogoContent = fetchCompanyLogoContent(companyDetailedInformation);

            log.debug("Getting invoice template detail with id: %s".formatted(invoice.getTemplateDetailId()));
            ContractTemplateDetail invoiceTemplateDetail = contractTemplateDetailsRepository
                    .findById(invoice.getTemplateDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template detail with id: %s not found".formatted(invoice.getTemplateDetailId())));

            Document document = documentsRepository
                    .findById(invoice.getInvoiceDocumentId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Invoice document not found with id: %s".formatted(invoice.getInvoiceDocumentId())));
            log.debug("Invoice document id: %s".formatted(document.getId()));

            log.debug("Downloading document");
            ByteArrayResource documentContent = fileService.downloadFile(document.getSignedFileUrl());
            log.debug("Document content size: %s".formatted(documentContent.contentLength()));

            BillingRunDocumentModelImpl documentModel = billingRunDocumentDataCreationService
                    .generateBillingRunDocumentModel(invoice, companyDetailedInformation, companyLogoContent);
            log.debug("Document model is created");

            ByteArrayResource emailTemplateFileContent = fileService.downloadFile(emailTemplateFile.getFileUrl());
            log.debug("Email template file content size: %s".formatted(emailTemplateFileContent.contentLength()));

            DocumentPathPayloads generatedEmailBody = documentGenerationService
                    .generateDocument(
                            emailTemplateFileContent,
                            buildFileDestinationPath(),
                            UUID.randomUUID().toString(),
                            documentModel,
                            Set.of(FileFormat.DOCX),
                            false
                    );
            log.debug("Email template file is generated");

            ByteArrayResource emailBodyContent = fileService.downloadFile(generatedEmailBody.docXPath());
            log.debug("Email body content size: %s".formatted(emailBodyContent.contentLength()));

            for (Map.Entry<Long, List<CustomerCommunicationContacts>> entry : customerAndCommunicationContactsMap.entrySet()) {
                Long customerDetailId = entry.getKey();
                List<CustomerCommunicationContacts> customerCommunicationContacts = entry.getValue();

                for (CustomerCommunicationContacts emailContract : customerCommunicationContacts) {
                    log.debug("Sending mail to customer with email: %s".formatted(emailContract.getContactValue()));

                    log.debug("Creating email communication attachment");
                    EmailCommunicationAttachment attachment = emailCommunicationAttachmentRepository
                            .save(EmailCommunicationAttachment
                                    .builder()
                                    .name(document.getName())
                                    .fileUrl(document.getSignedFileUrl())
                                    .status(EntityStatus.ACTIVE)
                                    .build());
                    log.debug("Email communication attachment id: %s".formatted(attachment.getId()));

                    log.debug("Creating email communication");
                    emailCommunicationService.createEmailFromDocument(
                            DocumentEmailCommunicationCreateRequest
                                    .builder()
                                    .communicationTopicId(topicOfCommunication.getId())
                                    .emailBoxId(emailMailboxes.getId())
                                    .customerEmailAddress(emailContract.getContactValue())
                                    .emailSubject(emailTemplate.getSubject())
                                    .emailBody(DocumentParserService.parseDocxToHtml(emailBodyContent.getContentAsByteArray()))
                                    .customerDetailId(customerDetailId)
                                    .customerCommunicationId(emailContract.getCustomerCommunicationsId())
                                    .attachmentFileIds(Set.of(attachment.getId()))
                                    .emailTemplateId(emailTemplate.getTemplateId())
                                    .templateIds(Set.of(invoiceTemplateDetail.getTemplateId()))
                                    .build(),
                            false
                    );
                    log.debug("Email communication is created, id: %s".formatted(emailContract.getCustomerCommunicationsId()));
                }
            }
        } catch (Exception e) {
            log.error("Exception handled while sending email", e);
        }
    }

    private ContractTemplateDetail defineEmailTemplate(BillingRun billingRun,
                                                       Invoice invoice) {
        log.debug("Defining email template");
        if (Objects.nonNull(billingRun.getEmailTemplateId())) {
            log.debug("Billing run email template id: %s".formatted(billingRun.getEmailTemplateId()));
            return contractTemplateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(billingRun.getEmailTemplateId(), LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective email template not found for template with id: %s".formatted(billingRun.getEmailTemplateId())));
        } else {
            log.debug("Invoice type: %s".formatted(invoice.type()));
            switch (invoice.type()) {
                case PRODUCT_CONTRACT -> {
                    ProductContractDetails productContractDetails = productContractDetailsRepository
                            .findById(invoice.getProductContractDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Product contract detail with id %s not found".formatted(invoice.getProductContractDetailId())));
                    log.debug("Product contract detail id: %s".formatted(productContractDetails.getId()));

                    ProductDetails productDetails = productDetailsRepository
                            .findById(productContractDetails.getProductDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Product detail with id %s not found".formatted(productContractDetails.getProductDetailId())));
                    log.debug("Product detail id: %s".formatted(productDetails.getId()));

                    ProductTemplate productTemplate = productTemplateRepository
                            .findByProductDetailId(productDetails.getId())
                            .stream()
                            .filter(pt -> Objects.equals(pt.getType(), ProductServiceTemplateType.EMAIL_TEMPLATE))
                            .toList()
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new DomainEntityNotFoundException("Product contract email template not found"));
                    log.debug("Product template id: %s".formatted(productTemplate.getId()));

                    return contractTemplateDetailsRepository
                            .findRespectiveTemplateDetailsByTemplateIdAndDate(productTemplate.getTemplateId(), LocalDate.now())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Respective email template not found for template with id: %s".formatted(productTemplate.getTemplateId())));
                }
                case SERVICE_CONTRACT -> {
                    log.debug("Service contract detail id: %s".formatted(invoice.getServiceContractDetailId()));
                    ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                            .findById(invoice.getServiceContractDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service contract detail with id %s not found".formatted(invoice.getServiceContractDetailId())));

                    log.debug("Service detail id: %s".formatted(serviceContractDetails.getServiceDetailId()));
                    ServiceDetails serviceDetails = serviceDetailsRepository
                            .findById(serviceContractDetails.getServiceDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service detail with id %s not found".formatted(serviceContractDetails.getServiceDetailId())));

                    ServiceTemplate serviceTemplate = serviceTemplateRepository
                            .findByServiceDetailId(serviceDetails.getId())
                            .stream()
                            .filter(st -> Objects.equals(st.getType(), ProductServiceTemplateType.EMAIL_TEMPLATE))
                            .toList()
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service contract email template not found"));
                    log.debug("Service template id: %s".formatted(serviceTemplate.getId()));

                    return contractTemplateDetailsRepository
                            .findRespectiveTemplateDetailsByTemplateIdAndDate(serviceTemplate.getTemplateId(), LocalDate.now())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Respective email template not found for template with id: %s".formatted(serviceTemplate.getTemplateId())));
                }
                default -> throw new RuntimeException("Invoice with invalid type was provided in billing run");
            }
        }
    }

    private Map<Long, List<CustomerCommunicationContacts>> findEmailCommunicationContactsFromInvoice(Invoice invoice,
                                                                                                     SendingAnInvoice sendingAnInvoice,
                                                                                                     List<InvoiceErrorShortObject> exceptionContext) {
        Map<Long, List<CustomerCommunicationContacts>> customerAndCommunicationContactsMap = new HashMap<>();

        switch (sendingAnInvoice) {
            case ACCORDING_TO_THE_CONTRACT -> {
                log.debug("Sending an invoice is {} for invoice with id: {}", sendingAnInvoice, invoice.getId());
                Long contractBillingGroupId = invoice.getContractBillingGroupId();

                log.debug("Contract billing group id: %s".formatted(contractBillingGroupId));
                if (Objects.nonNull(contractBillingGroupId)) {
                    Optional<ContractBillingGroup> contractBillingGroupOptional = contractBillingGroupRepository.findById(invoice.getContractBillingGroupId());

                    if (contractBillingGroupOptional.isPresent()) {
                        ContractBillingGroup contractBillingGroup = contractBillingGroupOptional.get();
                        log.debug("Contract billing group id: %s".formatted(contractBillingGroup.getId()));

                        if (Objects.equals(contractBillingGroup.getSendingInvoice(), BillingGroupSendingInvoice.EMAIL)) {
                            Long alternativeRecipientCustomerDetailId = invoice.getAlternativeRecipientCustomerDetailId();
                            Long contractBillingGroupBillingCustomerCommunicationId = contractBillingGroup.getBillingCustomerCommunicationId();
                            if (Objects.nonNull(contractBillingGroupBillingCustomerCommunicationId)) {
                                List<CustomerCommunicationContacts> customerCommunicationContacts = extractContactFromCommunications(invoice, exceptionContext, contractBillingGroupBillingCustomerCommunicationId);
                                customerAndCommunicationContactsMap.put(alternativeRecipientCustomerDetailId, customerCommunicationContacts);
                            }
                        }
                    } else {
                        log.debug("Contract billing group not found with id: %s".formatted(contractBillingGroupId));
                        exceptionContext.add(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), "Contract billing group not found with id: %s".formatted(invoice.getContractBillingGroupId())));
                    }
                }
            }
            case EMAIL -> {
                if (invoice.getCustomerCommunicationId() != null) {
                    List<CustomerCommunicationContacts> customerCommunicationContacts = extractContactFromCommunications(invoice, exceptionContext, invoice.getCustomerCommunicationId());

                    customerAndCommunicationContactsMap.put(invoice.getCustomerDetailId(), customerCommunicationContacts);
                } else if (invoice.getContractCommunicationId() != null) {
                    List<CustomerCommunicationContacts> customerCommunicationContacts = extractContactFromCommunications(invoice, exceptionContext, invoice.getContractCommunicationId());

                    customerAndCommunicationContactsMap.put(invoice.getAlternativeRecipientCustomerDetailId(), customerCommunicationContacts);
                } else {
                    exceptionContext.add(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), "Customer communication id and contract communication id are null"));
                }
            }
        }

        return customerAndCommunicationContactsMap;
    }

    private List<CustomerCommunicationContacts> extractContactFromCommunications(Invoice invoice, List<InvoiceErrorShortObject> exceptionContext, Long contractBillingGroupBillingCustomerCommunicationId) {
        log.debug("Extracting contacts from communication with id: %s".formatted(contractBillingGroupBillingCustomerCommunicationId));
        Optional<CustomerCommunications> customerCommunicationsOptional = customerCommunicationsRepository
                .findByIdAndStatuses(contractBillingGroupBillingCustomerCommunicationId, List.of(Status.ACTIVE));

        if (customerCommunicationsOptional.isPresent()) {
            log.debug("Customer communication id: %s".formatted(contractBillingGroupBillingCustomerCommunicationId));
            CustomerCommunications customerCommunications = customerCommunicationsOptional.get();

            List<CustomerCommunicationContacts> customerCommunicationContacts = communicationContactsRepository
                    .findByCustomerCommIdAndStatuses(customerCommunications.getId(), List.of(Status.ACTIVE))
                    .stream()
                    .filter(cc -> Objects.equals(cc.getContactType(), CustomerCommContactTypes.EMAIL))
                    .toList();
            log.debug("Customer communication contacts count: %s".formatted(customerCommunicationContacts.size()));

            return customerCommunicationContacts;
        } else {
            log.debug("Customer communication not found with id: %s".formatted(contractBillingGroupBillingCustomerCommunicationId));
            exceptionContext.add(new InvoiceErrorShortObject(invoice.getInvoiceNumber(), "Customer communication not found with id: %s".formatted(contractBillingGroupBillingCustomerCommunicationId)));
        }
        return new ArrayList<>();
    }
}
