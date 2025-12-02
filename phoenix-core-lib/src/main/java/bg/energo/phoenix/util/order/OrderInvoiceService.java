package bg.energo.phoenix.util.order;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocumentFile;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunDocumentDataCreationService;
import bg.energo.phoenix.service.billing.invoice.cancellation.InvoiceCancellationService;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.billing.model.impl.BillingRunDocumentModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.BillingRunDocumentModel;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentSummeryDataDAO;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInvoiceService {
    private final InvoiceDetailedDataRepository invoiceDetailedDataRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final InvoiceRepository invoiceRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final BillingRunDocumentDataCreationService billingRunDocumentDataCreationService;
    private final InvoiceDocumentDataRepository invoiceDocumentDataRepository;
    private final FileService fileService;
    private final DocumentGenerationService documentGenerationService;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final DocumentsRepository documentsRepository;
    private final SignerChainManager signerChainManager;
    private final InvoiceDocumentFileRepository invoiceDocumentFileRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final String invoiceTopicHardcodedValueName = "Invoice";
    private final TransactionTemplate transactionTemplate;
    private final InvoiceNumberService invoiceNumberService;
    private final InvoiceCancellationService invoiceCancellationService;
    @PersistenceContext
    private EntityManager entityManager;
    @Value("${invoice.document.ftp_directory_path}")
    private String invoiceDocumentFtpDirectoryPath;

    /**
     * extracts invoice ids from invoices and hard deletes all connected entities
     *
     * @param invoices list of invoices to delete
     */
    @Transactional
    public void extractInvoicesAndDeleteRelatedEntities(List<Invoice> invoices) {
        List<Long> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        List<InvoiceDetailedData> invoiceDetailedDataList = invoiceDetailedDataRepository.findAllByInvoiceIdIn(invoiceIds);
        List<InvoiceVatRateValue> invoiceVatRateValues = invoiceVatRateValueRepository.findAllByInvoiceIdIn(invoiceIds);
        List<CustomerLiability> liabilities = customerLiabilityRepository.findAllByInvoiceIdIn(invoiceIds);
        List<InvoiceDocumentFile> invoiceDocuments = invoiceDocumentFileRepository.findAllByInvoiceIdIn(invoiceIds);
        customerLiabilityRepository.deleteAll(liabilities);
        invoiceDetailedDataRepository.deleteAll(invoiceDetailedDataList);
        invoiceVatRateValueRepository.deleteAll(invoiceVatRateValues);
        invoiceDocumentFileRepository.deleteAll(invoiceDocuments);
        invoiceRepository.deleteAll(invoices);
    }


    /**
     * calculates vat rate values for each invoice detailed data
     * and sets appropriate values in invoice
     *
     * @param invoiceDetailedDataList list of invoice detailed data
     * @param invoice                 current invoice
     * @return List of newly created entities for invoice vat rate values
     */
    public List<InvoiceVatRateValue> calculateVatRateValuesForOrderInvoices(List<InvoiceDetailedData> invoiceDetailedDataList, Invoice invoice) {
        List<InvoiceDetailedDataAmountModel> amountModels = invoiceDetailedDataList
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(
                        model.getVatRatePercent(),
                        model.getValue(),
                        Boolean.TRUE,
                        BigDecimal.ONE
                )).toList();
        List<InvoiceVatRateValue> uncommittedInvoiceVatRates = new ArrayList<>();
        List<InvoiceVatRateResponse> invoiceVatRateResponses = groupByVatRates(amountModels);
        if (!invoiceVatRateResponses.isEmpty()) {
            uncommittedInvoiceVatRates.addAll(mapVatRates(invoice.getId(), invoiceVatRateResponses));
        }
        calculateTotalAmountsAndSetToInvoice(invoice, amountModels, invoiceVatRateResponses);
        return uncommittedInvoiceVatRates;
    }


    /**
     * Groups a list of {@link InvoiceDetailedDataAmountModel} instances by their VAT rate percent and calculates the total amount for each group.
     *
     * @param models the list of {@link InvoiceDetailedDataAmountModel} instances to group
     * @return a list of {@link InvoiceVatRateResponse} instances, each representing a group of models with the same VAT rate percent and the total amount for that group
     */
    private List<InvoiceVatRateResponse> groupByVatRates(List<InvoiceDetailedDataAmountModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            return new ArrayList<>();
        }

        List<InvoiceVatRateResponse> context = new ArrayList<>();

        Map<BigDecimal, List<InvoiceDetailedDataAmountModel>> groupedByVatRate = models
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(Objects.requireNonNullElse(model.vatRatePercent(), BigDecimal.ZERO), model.pureAmount(), model.isMainCurrency(), model.alternativeCurrencyExchangeRate()))
                .collect(Collectors.groupingBy(InvoiceDetailedDataAmountModel::vatRatePercent));

        for (Map.Entry<BigDecimal, List<InvoiceDetailedDataAmountModel>> entry : groupedByVatRate.entrySet()) {
            BigDecimal vatRatePercent = entry.getKey();
            List<InvoiceDetailedDataAmountModel> amountModels = entry.getValue();
            BigDecimal totalAmount = EPBDecimalUtils
                    .calculateSummary(amountModels
                            .stream()
                            .map(InvoiceDetailedDataAmountModel::pureAmount)
                            .toList()
                    );
            context.add(new InvoiceVatRateResponse(vatRatePercent, totalAmount));
        }

        return context;
    }

    /**
     * Calculates the total amounts for an invoice and sets them on the invoice object.
     *
     * @param invoice                 The invoice to calculate the total amounts for.
     * @param amountModels            A list of {@link InvoiceDetailedDataAmountModel} instances representing the detailed data for the invoice.
     * @param invoiceVatRateResponses A list of {@link InvoiceVatRateResponse} instances representing the VAT rates and amounts for the invoice.
     */
    private void calculateTotalAmountsAndSetToInvoice(Invoice invoice,
                                                      List<InvoiceDetailedDataAmountModel> amountModels,
                                                      List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        BigDecimal totalAmountExcludingVat = EPBDecimalUtils.convertToCurrencyScale(
                EPBDecimalUtils
                        .calculateSummary(amountModels
                                .stream()
                                .map(model -> {
                                    if (Boolean.TRUE.equals(model.isMainCurrency())) {
                                        return model.pureAmount();
                                    } else {
                                        return model.pureAmount().multiply(model.alternativeCurrencyExchangeRate());
                                    }
                                })
                                .toList())
        );
        BigDecimal totalAmountOfVat = EPBDecimalUtils.convertToCurrencyScale(EPBDecimalUtils.calculateSummary(invoiceVatRateResponses.stream().map(InvoiceVatRateResponse::valueOfVat).toList()));
        BigDecimal totalAmountIncludingVat = EPBDecimalUtils.convertToCurrencyScale(totalAmountExcludingVat.add(totalAmountOfVat));
        BigDecimal totalAmountIncludingVatInOtherCurrency = EPBDecimalUtils.convertToCurrencyScale(calculateTotalAmountIncludingVatInOtherCurrency(invoice, totalAmountIncludingVat));

        invoice.setTotalAmountExcludingVat(totalAmountExcludingVat);
        invoice.setTotalAmountOfVat(totalAmountOfVat);
        invoice.setTotalAmountIncludingVat(totalAmountIncludingVat);
        invoice.setTotalAmountIncludingVatInOtherCurrency(totalAmountIncludingVatInOtherCurrency);
    }

    /**
     * Calculates the total amount including VAT in the invoice's other currency.
     *
     * @param invoice                                The invoice object.
     * @param totalAmountIncludingVatInOtherCurrency The total amount including VAT in the other currency.
     * @return The calculated total amount including VAT in the other currency.
     */
    private BigDecimal calculateTotalAmountIncludingVatInOtherCurrency(Invoice invoice, BigDecimal totalAmountIncludingVatInOtherCurrency) {
        return totalAmountIncludingVatInOtherCurrency.multiply(Objects.requireNonNullElse(invoice.getCurrencyExchangeRateOnInvoiceCreation(), BigDecimal.ONE));
    }

    /**
     * Maps a list of {@link InvoiceVatRateResponse} instances to a list of {@link InvoiceVatRateValue} instances.
     *
     * @param invoiceId               The ID of the invoice.
     * @param invoiceVatRateResponses The list of {@link InvoiceVatRateResponse} instances to map.
     * @return A list of {@link InvoiceVatRateValue} instances representing the mapped VAT rate values.
     */
    private List<InvoiceVatRateValue> mapVatRates(Long invoiceId, List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        return invoiceVatRateResponses
                .stream()
                .map(vat -> InvoiceVatRateValue
                        .builder()
                        .invoiceId(invoiceId)
                        .valueOfVat(vat.valueOfVat())
                        .amountExcludingVat(vat.amountExcludingVat())
                        .vatRatePercent(vat.vatRatePercent())
                        .build()).toList();
    }

    public List<String> checkAndUpdatePaidProformaInvoices() {
        List<Pair<Invoice, Document>> result = transactionTemplate.execute(x -> savePaidInvoices());
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        transactionTemplate.executeWithoutResult((x) -> {
            for (Pair<Invoice, Document> longInvoiceDocumentPair : result) {
                Document value = longInvoiceDocumentPair.getSecond();
                if (value.getName().contains("%s")) {
                    Invoice inv = longInvoiceDocumentPair.getFirst();
                    entityManager.refresh(inv);
                    log.debug("OrderNumber {}", inv.getInvoiceNumber());
                    value.setName(value.getName().replace("%s", inv.getInvoiceStatus() == InvoiceStatus.CANCELLED ? inv.getInvoiceCancellationNumber() : inv.getInvoiceNumber()));
                }
                documentsRepository.save(value);
            }
        });

        return result.stream().map(x -> x.getFirst().getInvoiceNumber() + ", ").toList();
    }


    public List<Pair<Invoice, Document>> savePaidInvoices() {
        List<Pair<Invoice, Document>> response = new ArrayList<>();
        List<Object[]> invoiceDetails = invoiceRepository.findByIdAndLiabilityAmountZero();
        for (Object[] obj : invoiceDetails) {
            Invoice invoice = (Invoice) obj[0];
            if (Objects.nonNull(invoice)) {
                GoodsOrder goodsOrder = (GoodsOrder) obj[1];
                ServiceOrder serviceOrder = (ServiceOrder) obj[2];
                if (ObjectUtils.anyNotNull(goodsOrder, serviceOrder)) {
                    if (Objects.nonNull(goodsOrder)) {
                        goodsOrder.setOrderStatus(GoodsOrderStatus.PAID);
                        goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_INVOICE_GENERATED);
                    } else if (Objects.nonNull(serviceOrder) && serviceOrderRepository.allInvoicesPaid(serviceOrder.getId())
                            && serviceOrder.getOrderStatus().equals(ServiceOrderStatus.AWAITING_PAYMENT)) {
                        serviceOrder.setOrderStatus(ServiceOrderStatus.PAID);
                        serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_INVOICE_GENERATED);
                    }
                    invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
                    invoice.setAccountPeriodId(accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                            .orElse(null));
                    invoice.setInvoiceDate(LocalDate.now());
                    invoice.setPaymentDeadline(LocalDate.now());
                    invoiceNumberService.fillInvoiceNumber(invoice);
                    Document invoiceDocument = generateDocumentForInvoice(invoice, Objects.nonNull(goodsOrder));
                    if (Objects.nonNull(goodsOrder)) {
                        sendMailForGoodsOrder(invoice, goodsOrder, invoiceDocument);
                    } else if (Objects.nonNull(serviceOrder)) {
                        sendMailForServiceOrder(invoice, serviceOrder, invoiceDocument);
                    }
                    response.add(Pair.of(invoice, invoiceDocument));

                }
            }
        }
        return response;
    }

    public Document generateDocumentForInvoice(Invoice invoice, boolean isGoodsOrder) {
        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository.findById(invoice.getTemplateDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Contract Template detail not found with id: %s".formatted(invoice.getTemplateDetailId())));

        LocalDate date = LocalDate.now();
        String destinationPath = "%s/%s".formatted(invoiceDocumentFtpDirectoryPath, date.toString());
        log.debug("Starting fetching respective company detailed information");

        BillingRunDocumentModelImpl documentModel = generateInvoiceDocumentModel(invoice, isGoodsOrder);

        log.debug("Starting generation on Invoice with number: [%s]".formatted(invoice.getInvoiceNumber()));
        ByteArrayResource templateFileResource;
        try {
            templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath()));
            log.debug("Clone of template file created");

            String fileName = documentGenerationUtil.formatInvoiceFileName(invoice, templateDetail);
            log.debug("File name was formatted: [%s]".formatted(fileName));

            log.debug("Generating document");
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            documentModel,
                            Set.of(FileFormat.PDF), // hardcoded output file format
                            false
                    );
            log.debug("Documents was created successfully");
            boolean needSigningWithSystemCertificate = CollectionUtils
                    .emptyIfNull(templateDetail.getFileSigning())
                    .contains(ContractTemplateSigning.SIGNING_WITH_SYSTEM_CERTIFICATE);
            List<DocumentSigners> documentSigners = new ArrayList<>();

            if (needSigningWithSystemCertificate) {
                documentSigners.add(ContractTemplateSigning.SIGNING_WITH_SYSTEM_CERTIFICATE.getDocumentSigners());
            } else {
                documentSigners.add(ContractTemplateSigning.NO.getDocumentSigners());
            }
            log.debug("Saving invoice document entity");
            Document document = Document
                    .builder()
                    .signers(documentSigners)
                    .signedBy(new ArrayList<>())
                    .name(fileName)
                    .unsignedFileUrl(documentPathPayloads.pdfPath())
                    .signedFileUrl(documentPathPayloads.pdfPath())
                    .fileFormat(FileFormat.PDF)
                    .templateId(templateDetail.getTemplateId())
                    .documentStatus(DocumentStatus.UNSIGNED)
                    .status(EntityStatus.ACTIVE)
                    .build();
            log.debug("Invoice document entity was created with id: [%s]".formatted(document.getId()));
            documentsRepository.saveAndFlush(document);
            InvoiceDocumentFile invoiceDocumentFile = new InvoiceDocumentFile(null, document.getId(), invoice.getId());
            invoiceDocumentFileRepository.save(invoiceDocumentFile);
            signerChainManager.startSign(List.of(document));

            return document;
        } catch (Exception e) {
            log.error("exception while storing template file: {}", e.getMessage());
            throw new ClientException("exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }

    }

    /**
     * Generates a {@link BillingRunDocumentModelImpl} instance for an invoice.
     *
     * @param invoice    The invoice to generate the document model for.
     * @param goodsOrder Indicates whether the invoice is for a goods order or not.
     * @return A {@link BillingRunDocumentModelImpl} instance containing the invoice document model.
     */
    public BillingRunDocumentModelImpl generateInvoiceDocumentModel(Invoice invoice, boolean goodsOrder) {
        CompanyDetailedInformationModel companyDetailedInformation = billingRunDocumentDataCreationService.fetchCompanyDetailedInformationModel(LocalDate.now());
        byte[] companyLogoContent = billingRunDocumentDataCreationService.fetchCompanyLogoContent(companyDetailedInformation);
        BillingRunDocumentModel documentSpecificData = invoiceRepository.getInvoiceDocumentModel(invoice.getId());

        List<BillingRunDocumentSummeryDataDAO> summaryDataModels;
        List<BillingRunDocumentDetailedDataDAO> detailedDataModels = Collections.emptyList();
        if (goodsOrder) {
            summaryDataModels = invoiceDocumentDataRepository.getGoodsOrderSummaryData(invoice.getId())
                    .stream()
                    .map(BillingRunDocumentSummeryDataDAO::new)
                    .toList();
        } else {
            summaryDataModels = invoiceDocumentDataRepository.getServiceOrderSummaryDataForDocument(invoice.getId())
                    .stream()
                    .toList();
            detailedDataModels = invoiceDocumentDataRepository.getServiceOrderInvoiceDetailedData(invoice.getId())
                    .stream().map(BillingRunDocumentDetailedDataDAO::new)
                    .toList();
        }

        BillingRunDocumentModelImpl documentModel = new BillingRunDocumentModelImpl();
        documentModel.fillCompanyDetailedInformation(companyDetailedInformation);
        documentModel.fillInvoiceData(documentSpecificData);
        documentModel.fillSummaryDataForOrders(summaryDataModels);
        documentModel.fillDetailedData(detailedDataModels, Collections.emptyList(),Collections.emptyList());
        documentModel.CompanyLogo = companyLogoContent;
        return documentModel;
    }

    @SneakyThrows
    public String generateEmailBody(ContractTemplateDetail templateDetail, Invoice invoice, boolean fromGoodsOrder) {
        LocalDate currentDate = LocalDate.now();
        MDC.put("OrderInvoiceId", UUID.randomUUID().toString());
        String destinationPath = "%s/%s/%s".formatted(invoiceDocumentFtpDirectoryPath, "email_documents", currentDate);

        ByteArrayResource templateFileResource = new ByteArrayResource(
                Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath())
        );
        log.debug("Template file cloned successfully.");
        String formattedFileName = documentGenerationUtil.formatInvoiceFileName(invoice, templateDetail);

        BillingRunDocumentModelImpl model = generateInvoiceDocumentModel(invoice, fromGoodsOrder);

        DocumentPathPayloads documentPathPayloads = documentGenerationService
                .generateDocument(
                        templateFileResource,
                        destinationPath,
                        formattedFileName,
                        model,
                        Set.of(FileFormat.DOCX),
                        false
                );
        log.debug("Document created successfully.");

        ByteArrayResource downloadedFile = fileService.downloadFile(documentPathPayloads.docXPath());
        return DocumentParserService.parseDocx(downloadedFile.getByteArray());
    }


    public void sendMailForGoodsOrder(Invoice invoice, GoodsOrder goodsOrder, Document document) {
        ContractTemplateDetail emailTemplate = contractTemplateDetailsRepository.fetchGoodsOrderEmailTemplate(goodsOrder.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Email template not found for goods order %s;".formatted(goodsOrder.getId())));
        List<CustomerCommunicationContacts> contacts = customerCommunicationContactsRepository.findByCustomerCommIdContactTypesAndStatuses(goodsOrder.getCustomerCommunicationIdForBilling(), List.of(CustomerCommContactTypes.EMAIL), List.of(Status.ACTIVE));
        sendMailForOrder(invoice, emailTemplate, contacts, document, goodsOrder.getCustomerDetailId());
    }

    public void sendMailForServiceOrder(Invoice invoice, ServiceOrder serviceOrder, Document document) {
        ContractTemplateDetail emailTemplate = contractTemplateDetailsRepository.fetchServiceOrderEmailTemplate(serviceOrder.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Email template not found for service order %s;".formatted(serviceOrder.getId())));
        List<CustomerCommunicationContacts> contacts = customerCommunicationContactsRepository.findByCustomerCommIdContactTypesAndStatuses(serviceOrder.getCustomerCommunicationIdForBilling(), List.of(CustomerCommContactTypes.EMAIL), List.of(Status.ACTIVE));
        sendMailForOrder(invoice, emailTemplate, contacts, document, serviceOrder.getCustomerDetailId());
    }

    @SneakyThrows
    private void sendMailForOrder(Invoice invoice,
                                  ContractTemplateDetail emailTemplate,
                                  List<CustomerCommunicationContacts> customerCommunicationContacts,
                                  Document document, Long customerDetailId) {
        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue(invoiceTopicHardcodedValueName, NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found for type: %s".formatted(invoiceTopicHardcodedValueName)));

        EmailMailboxes emailMailboxes = emailMailboxesRepository
                .findByEmailForSendingInvoicesTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Email mail box not found"));


        for (CustomerCommunicationContacts emailContract : customerCommunicationContacts) {

            ByteArrayResource documentContent = fileService.downloadFile(document.getSignedFileUrl());

            Long attachmentId = emailCommunicationAttachmentRepository
                    .save(EmailCommunicationAttachment
                            .builder()
                            .name(document.getName())
                            .fileUrl(document.getSignedFileUrl())
                            .status(EntityStatus.ACTIVE)
                            .build()).getId();
            log.debug("CommunicationContactId {}", emailContract.getId());
            log.debug("CustomerDetailId {}", invoice.getCustomerDetailId());
            log.debug("CustomerCommunicationId {}", emailContract.getCustomerCommunicationsId());

            emailCommunicationService.createEmailFromDocument(
                    DocumentEmailCommunicationCreateRequest
                            .builder()
                            .communicationTopicId(topicOfCommunication.getId())
                            .emailBoxId(emailMailboxes.getId())
                            .customerEmailAddress(emailContract.getContactValue())
                            .emailSubject(emailTemplate.getSubject())
                            .emailBody(DocumentParserService.parsePdf(documentContent.getContentAsByteArray()))
                            .customerDetailId(customerDetailId)
                            .customerCommunicationId(emailContract.getCustomerCommunicationsId())
                            .attachmentFileIds(Set.of(attachmentId))
                            .emailTemplateId(emailTemplate.getTemplateId())
                            .templateIds(Set.of(document.getTemplateId()))
                            .build(),
                    false
            );
        }
    }


}
