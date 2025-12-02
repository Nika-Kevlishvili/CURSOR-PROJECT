package bg.energo.phoenix.service.billing.invoice.cancellation;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellation;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationDocument;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationNumber;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.InvoiceCancellationDocumentRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceCancellationNumberRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceCancellationRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.billing.invoice.models.persistance.extractor.InvoiceCancellationDocumentModelExtractor;
import bg.energo.phoenix.service.billing.invoice.models.persistance.model.InvoiceCancellationDocumentModel;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The InvoiceCancellationProcessor class is responsible for handling the
 * cancellation process for invoices. It integrates various services and
 * repositories to validate, retrieve, and process invoice data, generate
 * associated document templates, and communicate the cancellation to the
 * relevant parties through email notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceCancellationProcessor {
    private final Map<Long, Pair<ContractTemplateDetail, byte[]>> templateCache = new HashMap<>();
    private final FileService fileService;
    private final PrefixRepository prefixRepository;
    private final InvoiceRepository invoiceRepository;
    private final SignerChainManager signerChainManager;
    private final DocumentsRepository documentsRepository;
    private final CompanyLogoRepository companyLogoRepository;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final CompanyDetailRepository companyDetailRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final DocumentGenerationService documentGenerationService;
    private final ProcessedRecordInfoRepository processedRecordInfoRepository;
    private final InvoiceCancellationRepository invoiceCancellationRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final InvoiceCancellationNumberRepository invoiceCancellationNumberRepository;
    private final InvoiceCancellationDocumentRepository invoiceCancellationDocumentRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;

    private final String invoiceTopicHardcodedValueName = "Invoice";

    @Value("${invoice-cancellation.document.ftp_directory_path}")
    private String invoiceCancellationDocumentFtpDirectoryPath;

    @Value("${invoice.prefix-type.cancellation}")
    private String invoiceCancellationPrefixType;

    /**
     * Executes the invoice cancellation process for a specified row, permissions, date,
     * and process record information. Validates permissions, fetches and validates relevant
     * invoice cancellation data, retrieves associated invoices, and determines the correct
     * invoice to process cancellation. Generates required templates and processes the
     * cancellation, saving the updated invoice.
     *
     * @param row               The row associated with the invoice to be processed.
     * @param permissions       The set of permissions associated with the current user or process.
     * @param date              The date for the invoice cancellation process.
     * @param processRecordInfo The identifier for the process record information related to the
     *                          cancellation process.
     * @return The ID of the processed and saved invoice as a String.
     * @throws ClientException If there are validation errors, missing information,
     *                         insufficient permissions, or other issues during the
     *                         invoice cancellation process.
     */
    @Transactional
    public String execute(Row row, Set<String> permissions, LocalDate date, Long processRecordInfo) {
        if (!permissions.contains(PermissionEnum.INVOICE_CANCELLATION.getId())) {
            throw new ClientException("Not enough permission for cancelling invoice", ErrorCode.ACCESS_DENIED);
        }

        InvoiceCancellation invoiceCancellation;
        String invoiceNumber = getInvoiceNumber(row);
        Optional<InvoiceCancellation> invoiceCancellationOptional = invoiceCancellationRepository.findByProcessRecordInfo(processRecordInfo);
        if (invoiceCancellationOptional.isPresent()) {
            invoiceCancellation = invoiceCancellationOptional.get();
        } else {
            throw new ClientException("Invoice cancellation object not found for selected invoice number %s".formatted(invoiceNumber), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (Objects.isNull(invoiceCancellation.getTaxEventDate())) {
            throw new ClientException("Tax event date not found for selected invoice number %s".formatted(invoiceNumber), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<Invoice> invoices = getInvoicesByNumber(invoiceNumber);
        if (CollectionUtils.isEmpty(invoices)) {
            throw new ClientException("Invoice with number %s not exists in the system".formatted(invoiceNumber), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        LocalDate invoiceDate = getInvoiceDate(row);

        Optional<Invoice> invoiceDateMatchingInvoice = Optional.empty();
        if (invoiceDate != null) {
            invoiceDateMatchingInvoice = invoices
                    .stream()
                    .filter(invoice -> invoice.getInvoiceDate().isEqual(invoiceDate))
                    .findFirst();
        }

        Pair<ContractTemplateDetail, byte[]> documentTemplateContent = fetchDocumentTemplateContentForCancellation(invoiceCancellation);
        Pair<ContractTemplateDetail, byte[]> emailTemplateContent = fetchEmailTemplateContentForCancellation(invoiceCancellation);
        Invoice invoiceToProcess;
        Long invoiceCancellationId = invoiceCancellation.getId();
        LocalDate taxEventDate = invoiceCancellation.getTaxEventDate();
        if (invoiceDateMatchingInvoice.isPresent()) {
            invoiceToProcess = processInvoiceCancellation(
                    invoiceDateMatchingInvoice.get(),
                    date,
                    taxEventDate,
                    invoiceCancellationId,
                    documentTemplateContent,
                    emailTemplateContent,
                    processRecordInfo
            );
        } else if (invoices.size() == 1) {
            invoiceToProcess = processInvoiceCancellation(
                    invoices.get(0),
                    date,
                    taxEventDate,
                    invoiceCancellationId,
                    documentTemplateContent,
                    emailTemplateContent,
                    processRecordInfo
            );
        } else {
            invoiceToProcess = processInvoiceCancellation(
                    getLatestInvoice(invoices),
                    date,
                    taxEventDate,
                    invoiceCancellationId,
                    documentTemplateContent,
                    emailTemplateContent,
                    processRecordInfo
            );
        }

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoiceToProcess);

        return String.valueOf(savedInvoice.getId());
    }

    /**
     * Retrieves the latest invoice based on the invoice date from a list of invoices.
     *
     * @param invoices the list of invoices to search for the latest one
     * @return the latest invoice from the provided list, determined by the most recent invoice date
     * @throws NullPointerException      if the provided list is null
     * @throws IndexOutOfBoundsException if the provided list is empty
     */
    private Invoice getLatestInvoice(List<Invoice> invoices) {
        invoices.sort(Comparator.comparing(Invoice::getInvoiceDate).reversed());
        return invoices.get(0);
    }

    /**
     * Processes the cancellation of an invoice. This method updates the invoice status,
     * sets relevant dates and cancellation details, generates the cancellation document,
     * and attempts to send an email notification regarding the cancellation. It also handles
     * exceptions that may occur during the email sending process and updates the processed
     * record information if applicable.
     *
     * @param invoice                 the invoice to be cancelled.
     * @param cancellationDate        the date on which the cancellation is to be processed.
     * @param taxEventDate            the date of the tax event associated with the invoice cancellation.
     * @param cancellationId          the unique identifier of the cancellation.
     * @param documentTemplateContent the template and its content to generate the cancellation document.
     * @param emailTemplateContent    the template and its content to generate the email notification.
     * @param processRecordInfoId     the ID of the process record information entry related to this operation.
     * @return the updated invoice object after processing the cancellation.
     * @throws ClientException if the invoice is in a status that does not allow cancellation.
     */
    private Invoice processInvoiceCancellation(Invoice invoice,
                                               LocalDate cancellationDate,
                                               LocalDate taxEventDate,
                                               Long cancellationId,
                                               Pair<ContractTemplateDetail, byte[]> documentTemplateContent,
                                               Pair<ContractTemplateDetail, byte[]> emailTemplateContent,
                                               Long processRecordInfoId) {
        log.debug("Processing cancellation for invoice with id {}", invoice.getId());
        switch (invoice.getInvoiceStatus()) {
            case REAL -> {

                invoice.setTaxEventDate(taxEventDate);
                invoice.setInvoiceStatus(InvoiceStatus.CANCELLED);
                invoice.setInvoiceCancellationId(cancellationId);
                setInvoiceCancellationNumber(invoice, cancellationDate);
                InvoiceCancellationDocumentModel model = createCancellationDocumentModel(invoice, LocalDate.now());
                Document document = generateCancellationDocument(invoice, documentTemplateContent, model);
                try {
                    sendEmailForCancelledInvoice(invoice, document, documentTemplateContent, emailTemplateContent, model);
                } catch (Exception e) {
                    log.error("Exception handled while trying to send email for cancelled invoice with id: [%s]".formatted(invoice.getId()), e);

                    Optional<ProcessedRecordInfo> processedRecordInfoOptional = processedRecordInfoRepository.findById(processRecordInfoId);
                    if (processedRecordInfoOptional.isPresent()) {
                        ProcessedRecordInfo processedRecordInfo = processedRecordInfoOptional.get();
                        processedRecordInfo.setSuccess(false);
                        processedRecordInfo.setErrorMessage(e.getMessage());
                        processedRecordInfoRepository.saveAndFlush(processedRecordInfo);
                    }
                }
                return invoice;
            }

            case DRAFT, DRAFT_GENERATED -> {
                log.error("Invoice with number %s is in draft status and can't be cancelled".formatted(invoice.getInvoiceNumber()));
                throw new ClientException("Invoice with number %s is in draft status and can't be cancelled".formatted(invoice.getInvoiceNumber()), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
            case CANCELLED -> {
                log.error("Invoice with number %s already cancelled".formatted(invoice.getInvoiceNumber()));
                throw new ClientException("Invoice with number %s already cancelled".formatted(invoice.getInvoiceNumber()), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        return invoice;
    }

    /**
     * Sends an email notifying the customer about a cancelled invoice. The method utilizes
     * the provided email template, customer communications data, and the invoice cancellation document model
     * to generate and send the email. It ensures the required topic of communication, email mailbox,
     * and customer communication contacts are retrieved and verified before sending out the email.
     *
     * @param invoice              The cancelled invoice for which the email notification is to be sent.
     * @param emailTemplateContent A pair containing the email template details and the associated binary content.
     * @param model                The model containing additional information about the cancelled invoice,
     *                             used to format the email content.
     */
    private void sendEmailForCancelledInvoice(Invoice invoice,
                                              Document invoiceCancellationDocument,
                                              Pair<ContractTemplateDetail, byte[]> documentTemplateContent,
                                              Pair<ContractTemplateDetail, byte[]> emailTemplateContent,
                                              InvoiceCancellationDocumentModel model) {
        log.debug("Sending email for cancelled invoice with id: {}", invoice.getId());
        ContractTemplateDetail emailTemplate = emailTemplateContent.getKey();

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue(invoiceTopicHardcodedValueName, NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found for type: %s".formatted(invoiceTopicHardcodedValueName)));
        log.debug("Topic of communication for cancelled invoice is: {}", topicOfCommunication.getName());

        EmailMailboxes emailMailboxes = emailMailboxesRepository
                .findByEmailForSendingInvoicesTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mail box not found"));
        log.debug("Email mail box for cancelled invoice is: {}", emailMailboxes.getName());

        Long communicationId = ObjectUtils.firstNonNull(invoice.getCustomerCommunicationId(), invoice.getContractCommunicationId());
        log.debug("Communication id for cancelled invoice is: {}", communicationId);

        CustomerCommunications customerCommunications = customerCommunicationsRepository
                .findById(communicationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer communication not found for id: %s".formatted(communicationId)));

        List<CustomerCommunicationContacts> customerCommunicationContacts = customerCommunicationContactsRepository
                .findByCustomerCommIdAndStatuses(customerCommunications.getId(), List.of(Status.ACTIVE))
                .stream()
                .filter(cc -> Objects.equals(cc.getContactType(), CustomerCommContactTypes.EMAIL))
                .toList();
        log.debug("Number of contacts for cancelled invoice is: {}", customerCommunicationContacts.size());

        if (CollectionUtils.isNotEmpty(customerCommunicationContacts)) {
            EmailBodyContent emailBodyContent = generateEmailBody(invoice, emailTemplateContent, model);

            log.debug("Email body for cancelled invoice is: {}", emailBodyContent.body);
            EmailCommunicationAttachment attachment = emailCommunicationAttachmentRepository
                    .save(
                            EmailCommunicationAttachment
                                    .builder()
                                    .name(invoiceCancellationDocument.getName())
                                    .fileUrl(invoiceCancellationDocument.getSignedFileUrl())
                                    .status(EntityStatus.ACTIVE)
                                    .build()
                    );
            log.debug("Attachment saved successfully");

            for (CustomerCommunicationContacts customerCommunicationContact : customerCommunicationContacts) {
                log.debug("Sending email for customer communication contact: {}", customerCommunicationContact.getContactValue());
                emailCommunicationService.createEmailFromDocument(
                        DocumentEmailCommunicationCreateRequest
                                .builder()
                                .communicationTopicId(topicOfCommunication.getId())
                                .emailBoxId(emailMailboxes.getId())
                                .customerEmailAddress(customerCommunicationContact.getContactValue())
                                .emailSubject(emailTemplate.getSubject())
                                .emailBody(emailBodyContent.body)
                                .customerDetailId(invoice.getCustomerDetailId())
                                .customerCommunicationId(communicationId)
                                .attachmentFileIds(Set.of(attachment.getId()))
                                .emailTemplateId(emailTemplateContent.getKey().getTemplateId())
                                .templateIds(Set.of(documentTemplateContent.getKey().getTemplateId()))
                                .build(),
                        false
                );
            }
        }
    }

    /**
     * Sets the cancellation number for a given invoice, based on the cancellation date and
     * a pre-configured prefix. The cancellation number is generated as a sequence number appended
     * to the prefix, formatted as a zero-padded string.
     *
     * @param invoice          the invoice for which the cancellation number is to be set
     * @param cancellationDate the date when the invoice was cancelled, used to determine the sequence
     */
    private void setInvoiceCancellationNumber(Invoice invoice,
                                              LocalDate cancellationDate) {
        log.debug("Updating cancelled invoice number");

        Prefix prefix = prefixRepository
                .findByInvoicePrefixType(invoiceCancellationPrefixType)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice cancellation prefix not found in the system"));

        log.debug("Prefix from cancelled invoice is: {}", prefix.getName());

        Long latestCancelledInvoiceNumberByCancellationDate = invoiceCancellationNumberRepository
                .findLatestCancelledInvoiceNumberByCancellationDate(cancellationDate);

        Long nextInvoiceCancellationNumber = ObjectUtils.defaultIfNull(latestCancelledInvoiceNumberByCancellationDate, -1L) + 1L;

        log.debug("Next sequence for cancelled invoice is: {}", nextInvoiceCancellationNumber);

        String cancelledInvoiceNumber = "%s-%010d".formatted(prefix.getName(), nextInvoiceCancellationNumber);

        log.debug("Setting invoice number: {}", cancelledInvoiceNumber);

        invoice.setInvoiceCancellationNumber(cancelledInvoiceNumber);
        invoiceRepository.saveAndFlush(invoice);
        invoiceCancellationNumberRepository.save(
                InvoiceCancellationNumber
                        .builder()
                        .number(nextInvoiceCancellationNumber)
                        .cancellationDate(cancellationDate.atStartOfDay())
                        .invoiceId(invoice.getId())
                        .build()
        );
    }

    /**
     * Generates a cancellation document for the given invoice, using the provided template content,
     * and related cancellation document model. The generated document is saved and associated with the
     * invoice cancellation repository.
     *
     * @param invoice                 The invoice to be canceled for which the cancellation document is created.
     * @param documentTemplateContent A pair consisting of the contract template detail and the byte array representing
     *                                the template content.
     * @param model                   The cancellation document model containing additional details required for document
     *                                generation.
     * @throws RuntimeException If an error occurs while generating or saving the cancellation document.
     */
    private Document generateCancellationDocument(Invoice invoice,
                                                  Pair<ContractTemplateDetail, byte[]> documentTemplateContent,
                                                  InvoiceCancellationDocumentModel model) throws RuntimeException {
        LocalDate now = LocalDate.now();

        ContractTemplateDetail templateDetail = documentTemplateContent.getKey();
        String formattedFileName = documentGenerationUtil.formatInvoiceFileName(invoice, templateDetail);
        String destinationPath = "%s/%s".formatted(invoiceCancellationDocumentFtpDirectoryPath, now);

        try {
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    new ByteArrayResource(documentTemplateContent.getValue()),
                    destinationPath,
                    formattedFileName,
                    model,
                    Set.of(FileFormat.PDF),
                    false
            );

            String fileUrl = documentPathPayloads.pdfPath();
            List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail.getFileSigning());
            log.debug("CancellationDocumentSigners {} {}", templateDetail.getId(), templateDetail.getFileSigning());

            Document document = documentsRepository.saveAndFlush(
                    Document.builder()
                            .signers(documentSigners)
                            .signedBy(new ArrayList<>())
                            .name(formattedFileName)
                            .unsignedFileUrl(fileUrl)
                            .signedFileUrl(fileUrl)
                            .fileFormat(FileFormat.PDF)
                            .templateId(templateDetail.getTemplateId())
                            .documentStatus(DocumentStatus.UNSIGNED)
                            .status(EntityStatus.ACTIVE)
                            .build()
            );

            invoiceCancellationDocumentRepository.save(
                    InvoiceCancellationDocument
                            .builder()
                            .name(formattedFileName)
                            .fileUrl(fileUrl)
                            .cancelledInvoiceId(invoice.getId())
                            .documentId(document.getId())
                            .status(EntityStatus.ACTIVE)
                            .build()
            );

            document.setNeedArchive(true);
            document.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_CANCELLATION_DOCUMENT);
            document.setAttributes(
                    List.of(
                            new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_CANCELLATION_DOCUMENT),
                            new Attribute(attributeProperties.getDocumentNumberGuid(), document.getName()),
                            new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                            new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                            new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                            new Attribute(attributeProperties.getSignedGuid(), false)
                    )
            );

            fileArchivationService.archive(document);

            return document;
        } catch (Exception e) {
            log.error("Exception handled while trying to generate invoice cancellation document for invoice with id: [%s]".formatted(invoice.getId()), e);
            throw new ClientException("Exception handled while trying to generate invoice cancellation document for invoice with id: [%s], %s".formatted(invoice.getId(), e.getMessage()), ErrorCode.APPLICATION_ERROR);
        }
    }

    /**
     * Retrieves a list of document signers by extracting them from the provided list of contract template signings.
     *
     * @param signings the list of contract template signings from which document signers are to be extracted;
     *                 may be null, in which case an empty list will be returned.
     * @return a list of document signers extracted from the provided contract template signings.
     */
    private List<DocumentSigners> getDocumentSigners(List<ContractTemplateSigning> signings) {
        return Optional.ofNullable(signings)
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }

    /**
     * Creates an InvoiceCancellationDocumentModel for a given invoice and cancellation date.
     *
     * @param invoice          the invoice to be cancelled
     * @param cancellationDate the date on which the cancellation occurs
     * @return an InvoiceCancellationDocumentModel containing cancellation details, company information, and logo
     */
    private InvoiceCancellationDocumentModel createCancellationDocumentModel(Invoice invoice,
                                                                             LocalDate cancellationDate) {
        InvoiceCancellationDocumentModelExtractor extractor = invoiceCancellationRepository
                .getDocumentModelForCancelledInvoice(invoice.getId());

        InvoiceCancellationDocumentModel model = new InvoiceCancellationDocumentModel();
        model.fillCancellationDocumentDetails(extractor);

        CompanyDetailedInformationModel companyDetailedInformationModel = fetchCompanyDetailedInformationModel(cancellationDate);
        model.fillCompanyDetailedInformation(companyDetailedInformationModel);
        model.CompanyLogo = fetchCompanyLogoContent(companyDetailedInformationModel.getCompanyDetailId());
        return model;
    }

    /**
     * Fetches the content of the company logo as a byte array based on the provided company detail ID.
     *
     * @param detailId the ID of the company detail used to fetch the associated active company logo
     * @return a byte array containing the content of the company logo, or null if the logo is not found or an error occurs
     */
    private byte[] fetchCompanyLogoContent(Long detailId) {
        byte[] companyLogoContent = null;
        try {
            Optional<CompanyLogos> companyLogoOptional = companyLogoRepository
                    .findFirstByCompanyDetailIdAndStatus(detailId, EntityStatus.ACTIVE);
            if (companyLogoOptional.isPresent()) {
                CompanyLogos companyLogo = companyLogoOptional.get();

                companyLogoContent = fileService.downloadFile(companyLogo.getFileUrl()).getContentAsByteArray();
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to download company logo");
        }
        return companyLogoContent;
    }

    /**
     * Generates the body of an email based on the provided invoice, template content, and model.
     * The method creates a document, parses its content, and constructs the email body.
     *
     * @param invoice              the invoice object containing details required for generating the email body
     * @param emailTemplateContent a pair containing the contract template details and byte array of the email template
     * @param model                the invoice cancellation document model used for the document generation
     * @return an {@code EmailBodyContent} object containing the formatted filename, document path, and parsed email body content
     */
    @SneakyThrows
    private EmailBodyContent generateEmailBody(Invoice invoice,
                                               Pair<ContractTemplateDetail, byte[]> emailTemplateContent,
                                               InvoiceCancellationDocumentModel model) {
        log.debug("Generating email body");
        LocalDate currentDate = LocalDate.now();
        String destinationPath = "%s/%s/%s".formatted(invoiceCancellationDocumentFtpDirectoryPath, "email_documents", currentDate);

        ContractTemplateDetail templateDetail = emailTemplateContent.getKey();

        log.debug("Template file cloned successfully.");
        String formattedFileName = documentGenerationUtil.formatInvoiceFileName(invoice, templateDetail);

        DocumentPathPayloads documentPathPayloads = documentGenerationService
                .generateDocument(
                        new ByteArrayResource(emailTemplateContent.getValue()),
                        destinationPath,
                        formattedFileName,
                        model,
                        Set.of(FileFormat.DOCX),
                        false
                );
        log.debug("Document created successfully.");

        ByteArrayResource downloadedFile = fileService.downloadFile(documentPathPayloads.docXPath());

        String body = DocumentParserService.parseDocx(downloadedFile.getContentAsByteArray());

        return new EmailBodyContent(formattedFileName, documentPathPayloads.docXPath(), body);
    }

    /**
     * Retrieves the invoice date from the specified row.
     *
     * @param row the row from which the invoice date is to be extracted
     * @return the extracted invoice date as a LocalDate
     */
    private LocalDate getInvoiceDate(Row row) {
        return EPBExcelUtils.getLocalDateValue(1, row);
    }

    /**
     * Retrieves the invoice number from the specified row.
     *
     * @param row the row from which the invoice number will be extracted
     * @return the invoice number as a string
     */
    private String getInvoiceNumber(Row row) {
        return EPBExcelUtils.getStringValue(0, row);
    }

    /**
     * Retrieves a list of invoices that match the given invoice number pattern.
     *
     * @param invoiceNumber the invoice number or pattern to search for; should not be blank
     * @return a list of invoices matching the invoice number pattern, or null if no matches are found or if the input is blank
     */
    private List<Invoice> getInvoicesByNumber(String invoiceNumber) {
        if (StringUtils.isNotBlank(invoiceNumber)) {
            Optional<List<Invoice>> invoiceOptional = invoiceRepository.findAllByInvoiceNumberLike(EPBStringUtils.fromPromptToQueryParameter(invoiceNumber));
            return invoiceOptional.orElse(null);
        }
        return null;
    }


    /**
     * Fetches the detailed information of a company for the specified billing date.
     *
     * @param billingDate the date for which the detailed information of the company is to be retrieved
     * @return a CompanyDetailedInformationModel containing the detailed information of the company for the given billing date
     */
    private CompanyDetailedInformationModel fetchCompanyDetailedInformationModel(LocalDate billingDate) {
        return companyDetailRepository.getCompanyDetailedInformation(billingDate);
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
        Pair<ContractTemplateDetail, byte[]> templateContent;
        if (templateCache.containsKey(invoiceCancellation.getContractTemplateId())) {
            templateContent = templateCache.get(invoiceCancellation.getContractTemplateId());
        } else {
            ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(invoiceCancellation.getContractTemplateId(), LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template detail respective version not found to generate cancellation document"));

            ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                    .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective template detail file not found"));

            ByteArrayResource templateFileContent = fileService.downloadFile(contractTemplateFile.getFileUrl());

            templateContent = Pair.of(contractTemplateDetail, templateFileContent.getByteArray());

            templateCache.put(invoiceCancellation.getContractTemplateId(), templateContent);
        }
        return templateContent;
    }

    /**
     * Fetches the email template content and its associated metadata for an invoice cancellation.
     * If the template content is cached, it retrieves it from the cache. Otherwise, it fetches
     * the template details and file content from the repository and saves it in the cache.
     *
     * @param invoiceCancellation the invoice cancellation details containing the email template ID
     * @return a Pair containing the ContractTemplateDetail and the corresponding template file content in byte array
     * @throws DomainEntityNotFoundException if the template details or associated file cannot be found
     */
    private Pair<ContractTemplateDetail, byte[]> fetchEmailTemplateContentForCancellation(InvoiceCancellation invoiceCancellation) {
        Pair<ContractTemplateDetail, byte[]> templateContent;
        if (templateCache.containsKey(invoiceCancellation.getEmailTemplateId())) {
            templateContent = templateCache.get(invoiceCancellation.getEmailTemplateId());
        } else {
            ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(invoiceCancellation.getEmailTemplateId(), LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template detail respective version not found to generate cancellation email body"));

            ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                    .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective template detail file not found"));

            ByteArrayResource templateFileContent = fileService.downloadFile(contractTemplateFile.getFileUrl());

            templateContent = Pair.of(contractTemplateDetail, templateFileContent.getByteArray());

            templateCache.put(invoiceCancellation.getEmailTemplateId(), templateContent);
        }
        return templateContent;
    }

    /**
     * Signs the invoice cancellation documents associated with the given process.
     *
     * @param process the process whose associated invoice cancellation documents are to be signed
     */
    public void signInvoiceDocs(Process process) {
        List<Document> cancellation = invoiceCancellationRepository.findInvoiceCancellationDocumentsByProcessId(process.getId());
        log.debug("InvoiceCancellationSignableFiles {}", cancellation.stream().map(Document::getId).toList());
        signerChainManager.startSign(cancellation);
    }

    /**
     * Represents the content of an email body with associated metadata.
     * <p>
     * This record encapsulates information related to the body of an email,
     * along with additional details such as a file name and an FTP path.
     * <p>
     * Immutable by design, this class provides structured storage for email-related content.
     * <p>
     * fileName - Name of the file associated with this email content, if applicable.
     * ftpPath - The FTP path where the file is stored.
     * body - The main body content of the email.
     */
    private record EmailBodyContent(
            String fileName,
            String ftpPath,
            String body
    ) {
    }
}
