package bg.energo.phoenix.service.billing.invoice.cancellation;

import bg.energo.phoenix.event.Event;
import bg.energo.phoenix.event.EventFactory;
import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.model.entity.billing.invoice.*;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScale;
import bg.energo.phoenix.model.entity.receivable.ReverseOffsettingService;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.billing.invoice.CreateInvoiceCancellationRequest;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.receivable.payment.PaymentCancelRequest;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceCancellationFileResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceCancellationResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceConnectionsShortResponse;
import bg.energo.phoenix.model.response.receivable.OffsettingObject;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityTransactionsResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.process.service.ProcessService;
import bg.energo.phoenix.repository.InvoiceCancellationDocumentRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingByProfileRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingByScaleRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.billing.invoice.models.persistance.extractor.InvoiceCancellationDocumentModelExtractor;
import bg.energo.phoenix.service.billing.invoice.models.persistance.model.InvoiceCancellationDocumentModel;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.service.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingService;
import bg.energo.phoenix.service.receivable.payment.PaymentService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.kafka.RabbitMQProducerService;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.model.entity.EntityStatus.ACTIVE;
import static bg.energo.phoenix.process.model.enums.ProcessStatus.NOT_STARTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceCancellationService {

    private final static String FOLDER_PATH = "InvoiceCancellationTemplate";
    private final InvoiceCancellationRepository invoiceCancellationRepository;
    private final InvoiceRepository invoiceRepository;
    private final PrefixRepository prefixRepository;
    private final InvoiceCancellationNumberRepository invoiceCancellationNumberRepository;
    private final CompanyLogoRepository companyLogoRepository;
    private final CompanyDetailRepository companyDetailRepository;
    private final FileService fileService;
    private final FileArchivationService fileArchivationService;
    private final InvoiceCancellationDocumentRepository invoiceCancellationDocumentRepository;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final DocumentGenerationService documentGenerationService;
    private final DocumentsRepository documentsRepository;
    private final EDMSAttributeProperties attributeProperties;
    private final InvoiceCancellationFileRepository invoiceCancellationFileRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ReverseOffsettingService reverseOffsettingService;
    private final TemplateRepository templateRepository;
    private final ProcessService processService;
    private final PermissionService permissionService;
    private final PaymentService paymentService;
    private final CustomerReceivableService customerReceivableService;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final ManualLiabilityOffsettingService manualLiabilityOffsettingService;
    private final LatePaymentFineService latePaymentFineService;
    private final EventFactory eventFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ContractTemplateRepository contractTemplateRepository;
    private final InvoiceCancellationInvoicesRepository invoiceCancellationInvoicesRepository;
    private final TransactionTemplate transactionTemplate;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final BillingByScaleRepository billingByScaleRepository;
    private final BillingByProfileRepository billingByProfileRepository;
    private final BillingDataByProfileRepository billingDataByProfileRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final SignerChainManager signerChainManager;

    private final RabbitMQProducerService rabbitMQProducerService;
    @Value("${invoice.prefix-type.cancellation}")
    private String invoiceCancellationPrefixType;
    @Value("${invoice-cancellation.document.ftp_directory_path}")
    private String invoiceCancellationDocumentFtpDirectoryPath;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public InvoiceCancellationResponse createInvoiceCancellation(CreateInvoiceCancellationRequest request) {

        Pair<Process, InvoiceCancellation> execute = transactionTemplate.execute((x) -> {
            Process process = createProcess();
            InvoiceCancellation invoiceCancellation = createInvoiceCancellation(process.getId(), request.getTaxEventDate());
            saveData(request, invoiceCancellation.getId());
            validateAndSetTemplate(request.getTemplateId(), invoiceCancellation);
            return Pair.of(process, invoiceCancellation);
        });


        Process key = execute.getKey();
        startProcess(key);

        return new InvoiceCancellationResponse(
                execute.getValue()
                        .getId(),
                key.getId()
        );
    }


    public InvoiceCancellationFileResponse upload(MultipartFile file) {
        log.debug("Invoice cancellation file {}.", file.getName());
        EPBExcelUtils.validateFileFormat(file);

        Template template = templateRepository
                .findById(EPBFinalFields.INVOICE_CANCELLATION_TEMPLATE_ID)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for invoice cancellation not found;"));

        EPBExcelUtils.validateFileContent(
                file,
                fileService.downloadFile(template.getFileUrl())
                        .getByteArray(),
                1
        );

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Invoice cancellation file name is null");
            throw new IllegalArgumentsProvidedException("Invoice cancellation file name is null.");
        }

        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String fileUrl = fileService.uploadFile(file, path(), fileName);

        InvoiceCancellationFile invoiceCancellationFile = createInvoiceCancellationFile(fileName, fileUrl);
        return new InvoiceCancellationFileResponse(invoiceCancellationFile);
    }

    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = PermissionContextEnum.INVOICE,
                            permissions = {PermissionEnum.INVOICE_CANCELLATION}
                    )
            }
    )
    public byte[] downloadTemplate(String id) {
        try {
            Template template = templateRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template for invoice cancellation not found;"));

            return fileService.downloadFile(template.getFileUrl())
                    .getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch invoice cancellation template", exception);
            throw new ClientException("Could not fetch invoice cancellation template", APPLICATION_ERROR);
        }
    }

    @Transactional
    public void processInvoice(
            Invoice invoiceToCancel,
            InvoiceCancellation cancellation,
            LocalDate cancellationDate,
            Pair<ContractTemplateDetail, byte[]> documentTemplateContent,
            List<InvoiceCancellationDto> validInvoices
    ) {
        if (!CollectionUtils.isEmpty(validInvoices) && !invoiceToCancel.getInvoiceType()
                .equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
            for (InvoiceCancellationDto validInvoice : validInvoices) {
                if (StringUtils.equals(validInvoice.getType(), "PROFILE")) {
                    BillingByProfile billingByProfile = billingByProfileRepository
                            .findById(validInvoice.getBillingDataId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Billing profile not found with id %s".formatted(
                                    validInvoice.getBillingDataId())));
                    if (validInvoice.getShouldDelete()) {
                        billingDataByProfileRepository.deleteByBillingByProfileId(billingByProfile.getId());
                        billingByProfileRepository.deleteById(billingByProfile.getId());
                    } else {
                        billingByProfile.setInvoiced(false);
                        billingByProfileRepository.save(billingByProfile);
                    }
                } else if (StringUtils.equals(validInvoice.getType(), "SCALE")) {
                    BillingByScale billingByScale = billingByScaleRepository
                            .findById(validInvoice.getBillingDataId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Billing Scale not found with id %s".formatted(
                                    validInvoice.getBillingDataId())));
                    billingByScale.setInvoiced(false);
                    billingByScaleRepository.save(billingByScale);
                }
            }
        }
        if (invoiceToCancel.getInvoiceType()
                .equals(InvoiceType.MANUAL) && (invoiceToCancel.getInvoiceDocumentType()
                .equals(InvoiceDocumentType.CREDIT_NOTE) || invoiceToCancel.getInvoiceDocumentType()
                .equals(InvoiceDocumentType.DEBIT_NOTE))) {
            //Check if is created from interim and interim is deducted on this invoice only make interim dedacted false
            List<Invoice> interimInvoices = invoiceCancellationRepository.findDeductedInvoicesForManual(invoiceToCancel.getId());
            for (Invoice interimInvoice : interimInvoices) {
                interimInvoice.setIsDeducted(false);
                invoiceRepository.save(interimInvoice);
            }
        }
        if (invoiceToCancel.getInvoiceType()
                .equals(InvoiceType.STANDARD)) {
            List<Invoice> interimInvoices = invoiceCancellationRepository.findDeductedForStandard(invoiceToCancel.getId());
            for (Invoice interimInvoice : interimInvoices) {
                interimInvoice.setIsDeducted(false);
                invoiceRepository.save(interimInvoice);
            }
        }
        if (invoiceToCancel.getInvoiceType()
                .equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
            invoiceToCancel.setIsDeducted(false);
        }
        if (invoiceToCancel.getGoodsOrderId() != null) {
            GoodsOrder goodsOrder = goodsOrderRepository.findById(invoiceToCancel.getGoodsOrderId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Goods order not found!;"));
            goodsOrder.setOrderStatus(GoodsOrderStatus.CONFIRMED);
            goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED);
            goodsOrderRepository.save(goodsOrder);
        } else if (invoiceToCancel.getServiceOrderId() != null) {
            ServiceOrder serviceOrder = serviceOrderRepository.findById(invoiceToCancel.getServiceOrderId())
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "Goods order not found!;"));
            serviceOrder.setOrderStatus(ServiceOrderStatus.CONFIRMED);
            serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED);
            serviceOrderRepository.save(serviceOrder);
        }
        accountingPeriodsRepository.findByIdAndStatus(invoiceToCancel.getAccountPeriodId(), AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentsProvidedException(
                        "Accounting period is closed for this invoice!;"));
        invoiceToCancel.setTaxEventDate(cancellation.getTaxEventDate());
        invoiceToCancel.setInvoiceStatus(InvoiceStatus.CANCELLED);
        invoiceToCancel.setInvoiceCancellationId(cancellation.getId());
        setInvoiceCancellationNumber(invoiceToCancel, cancellationDate);
        InvoiceCancellationDocumentModel model = createCancellationDocumentModel(invoiceToCancel, LocalDate.now());
        generateCancellationDocument(invoiceToCancel, documentTemplateContent, model);

        switch (invoiceToCancel.getInvoiceDocumentType()) {
            case PROFORMA_INVOICE -> processProformaInvoice(invoiceToCancel);
            case CREDIT_NOTE -> processCreditNoteInvoice(invoiceToCancel);
            default -> processDebitNoteInvoice(invoiceToCancel);
        }

    }

    /**
     * Processes the proforma invoice by handling customer liabilities and receivables based on the provided invoice details.
     *
     * @param invoice the invoice object containing the details of the proforma invoice to be processed
     */
    void processProformaInvoice(Invoice invoice) {
        List<InvoiceConnectionsShortResponse> liabilitiesByInvoiceId = customerLiabilityRepository.findAllLiabilityByInvoiceId(invoice.getId());
        if (CollectionUtils.isNotEmpty(liabilitiesByInvoiceId)) {
            for (InvoiceConnectionsShortResponse invoiceLiability : liabilitiesByInvoiceId) {
                if (invoiceLiability.initialAmount()
                        .compareTo(invoiceLiability.currentAmount()) > 0) {
                    customerReceivableService.createReceivableForInvoiceCancellation(
                            invoice,
                            invoiceLiability.initialAmount()
                                    .subtract(invoiceLiability.currentAmount())
                    );
                }
            }
            customerLiabilityRepository.updateAllStatusByLiabilityIds(EPBListUtils.transform(
                    liabilitiesByInvoiceId,
                    InvoiceConnectionsShortResponse::id
            ));
        }
    }

    /**
     * Processes a debit note associated with the given invoice, performing necessary validations
     * and handling associated reversals, cancellations, or other actions required.
     *
     * @param invoice the invoice object that represents the debit note to be processed.
     *                This invoice will be used to determine associated liabilities and perform
     *                various operations such as cancellations, offsetting reversals, and late payment fine reversals.
     */
    void processDebitNoteInvoice(Invoice invoice) {
        List<InvoiceConnectionsShortResponse> liabilitiesByInvoiceId = customerLiabilityRepository.findAllLiabilityByInvoiceId(invoice.getId());
        for (InvoiceConnectionsShortResponse invoiceLiability : liabilitiesByInvoiceId) {
            if (invoiceLiability.initialAmount().compareTo(invoiceLiability.currentAmount()) != 0) {
                List<Long> reschedulingId = customerLiabilityRepository.getConnectedReschedulingId(invoiceLiability.id());
                if (!CollectionUtils.isEmpty(reschedulingId)) {
                    throw new OperationNotAllowedException(
                            "Invoice can not be cancelled because connected liability is used for rescheduling");
                }

                List<Long> manualLOIds = customerLiabilityRepository.getConnectedMLOId(invoiceLiability.id());
                if (!CollectionUtils.isEmpty(manualLOIds)) {
                    for (Long manualLOId : manualLOIds) {
                        manualLiabilityOffsettingService.reversal(manualLOId);
                    }
                }

                List<CustomerLiabilityTransactionsResponse> paymentId = customerLiabilityRepository.findTransactionAndDestIdByLiabilityId(
                        invoiceLiability.id(),
                        OffsettingObject.PAYMENT.name(),
                        "APO"
                );
                if (!CollectionUtils.isEmpty(paymentId)) {
                    for (CustomerLiabilityTransactionsResponse offsetting : paymentId) {
                        paymentService.cancel(new PaymentCancelRequest(offsetting.getSourceId(), null, null, null, null, null));
                    }
                }

                List<CustomerLiabilityTransactionsResponse> automaticOffsettingId = customerLiabilityRepository.findTransactionAndDestIdByLiabilityId(
                        invoiceLiability.id(),
                        OffsettingObject.RECEIVABLE.name(),
                        "ALO"
                );
                if (!CollectionUtils.isEmpty(automaticOffsettingId)) {
                    automaticOffsettingId.forEach(offsetting -> reverseOffsettingService.reverseOffsetting(
                            offsetting.getId(), LocalDateTime.now(),
                            permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                            permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
                    ));
                }

                List<Long> latePaymentFineIds = customerLiabilityRepository.getConnectedLPFIds(invoiceLiability.id());
                if (!CollectionUtils.isEmpty(latePaymentFineIds)) {
                    for (Long latePaymentFineId : latePaymentFineIds) {
                        latePaymentFineService.reverse(latePaymentFineId, true);
                    }
                }
            }

            customerReceivableService.createReceivableForInvoiceCancellation(invoice, invoiceLiability.initialAmount());
        }
    }

    /**
     * Processes a credit note invoice by handling the associated invoice connections, liabilities,
     * and potential reversals. This method evaluates the current state of the receivables connected
     * to the invoice and performs the necessary handling for liabilities, reversals, and related
     * operations as per the business rules.
     *
     * @param invoice The credit note invoice to be processed. Contains the relevant data for operations
     *                such as liabilities, receivables, and connected transactions.
     */
    void processCreditNoteInvoice(Invoice invoice) {
        List<InvoiceConnectionsShortResponse> receivablesByInvoiceId = customerReceivableRepository.findAllReceivableByInvoiceId(
                invoice.getId());
        for (InvoiceConnectionsShortResponse invoiceReceivable : receivablesByInvoiceId) {
            if (invoiceReceivable.initialAmount()
                    .compareTo(invoiceReceivable.currentAmount()) != 0) {
                List<Long> manualLOIds = customerReceivableRepository.getConnectedMLOId(invoiceReceivable.id());
                if (!CollectionUtils.isEmpty(manualLOIds)) {
                    for (Long manualLOId : manualLOIds) {
                        manualLiabilityOffsettingService.reversal(manualLOId);
                    }
                }

                List<CustomerLiabilityTransactionsResponse> automaticOffsettingId = customerReceivableRepository.findTransactionAndDestIdByReceivableId(
                        invoiceReceivable.id(),
                        OffsettingObject.RECEIVABLE.name(),
                        "ALO"
                );
                if (!CollectionUtils.isEmpty(automaticOffsettingId)) {
                    for (CustomerLiabilityTransactionsResponse offsetting : automaticOffsettingId) {
                        List<Long> reschedulingId = customerLiabilityRepository.getConnectedReschedulingId(offsetting.getSourceId());
                        if (!CollectionUtils.isEmpty(reschedulingId)) {
                            throw new OperationNotAllowedException(
                                    "Invoice can not be cancelled because connected liability is used for rescheduling");
                        }

                        reverseOffsettingService.reverseOffsetting(
                                offsetting.getId(), LocalDateTime.now(),
                                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
                        );

                        List<Long> latePaymentFineIds = customerLiabilityRepository.getConnectedLPFIds(offsetting.getSourceId());
                        if (!CollectionUtils.isEmpty(latePaymentFineIds)) {
                            for (Long latePaymentFineId : latePaymentFineIds) {
                                latePaymentFineService.reverse(latePaymentFineId, true);
                            }
                        }
                    }
                }
            }
            customerLiabilityService.createLiability(
                    invoice,
                    invoiceReceivable.initialAmount(),
                    LiabilityOrReceivableCreationSource.INVOICE_CANCELLATION,
                    true
            );
        }
    }

    private InvoiceCancellationFile createInvoiceCancellationFile(String fileName, String fileUrl) {
        InvoiceCancellationFile invoiceCancellationFile = new InvoiceCancellationFile();
        invoiceCancellationFile.setName(fileName);
        invoiceCancellationFile.setFileUrl(fileUrl);
        invoiceCancellationFile.setStatus(ACTIVE);
        return invoiceCancellationFileRepository.saveAndFlush(invoiceCancellationFile);
    }

    private String path() {
        return String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
    }


    private void saveData(CreateInvoiceCancellationRequest request, Long cancellationId) {
        Set<String> invoiceNumbers = request.getInvoices() == null ? new HashSet<>() : Arrays.stream(request.getInvoices()
                        .split(","))
                .collect(Collectors.toSet());
        StringBuilder errorMessages = new StringBuilder();
        Map<String, String> numberWithDateMap = new HashMap<>();

        List<InvoiceReversalModel> invoices = new ArrayList<>();
        if (request.getFileId() != null) {
            invoiceCancellationFileRepository.findById(request.getFileId())
                    .ifPresent(x -> {
                        try {
                            Workbook workbook = new XSSFWorkbook(fileService.downloadFile(x.getFileUrl())
                                    .getInputStream());
                            Sheet sheet = workbook.getSheetAt(0);
                            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                                Row cells = sheet.getRow(i);
                                if (cells == null) {
                                    break;
                                }
                                String fileInvoiceNumber = EPBExcelUtils.getStringValue(0, cells);
                                if (fileInvoiceNumber == null) {
                                    continue;
                                }
                                if (invoiceNumbers.contains(fileInvoiceNumber)) {
                                    errorMessages.append(
                                            "Invoice Number: %s is already provided in text block. Row: %s".formatted(
                                                    fileInvoiceNumber,
                                                    cells.getRowNum()
                                            ));
                                    continue;
                                }
                                LocalDate localDateValue = EPBExcelUtils.getLocalDateValue(1, cells);
                                if (localDateValue == null) {
                                    invoiceNumbers.add(fileInvoiceNumber);
                                } else {
                                    if (numberWithDateMap.containsKey(fileInvoiceNumber)) {
                                        errorMessages.append(
                                                "Duplicate value found in file for invoice: %s, row: %s".formatted(
                                                        fileInvoiceNumber,
                                                        cells.getRowNum()
                                                ));
                                    } else {
                                        Optional<InvoiceReversalModel> invoiceNumberAndDate = invoiceRepository.findAllByInvoiceNumberAndDate(
                                                fileInvoiceNumber,
                                                localDateValue
                                        );
                                        if (invoiceNumberAndDate.isEmpty()) {
                                            errorMessages.append(
                                                    "Invoice with number: %s not found or is reversed!!;".formatted(
                                                            fileInvoiceNumber));
                                            continue;
                                        }
                                        invoices.add(invoiceNumberAndDate.get());
                                        numberWithDateMap.put(fileInvoiceNumber, localDateValue.toString());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

        }

        invoices.addAll(invoiceRepository.findAllByNumberForCancellation(invoiceNumbers));

        for (InvoiceReversalModel invoice : invoices) {
            numberWithDateMap.remove(invoice.getInvoiceNumber());
            invoiceNumbers.remove(invoice.getInvoiceNumber());
        }
        invoiceNumbers.addAll(numberWithDateMap.keySet());
        for (String invoiceNumber : invoiceNumbers) {
            errorMessages.append("Invoice with number: %s not found or is reversed!;".formatted(invoiceNumber));
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalArgumentsProvidedException(errorMessages.toString());
        }

        invoiceCancellationInvoicesRepository.saveAll(invoices.stream()
                .map(x -> new InvoiceCancellationInvoice(
                        null,
                        x.getInvoiceId(),
                        cancellationId
                ))
                .toList());
    }


    private Process createProcess() {
        List<String> permissions = permissionService.getPermissionsFromContext(PermissionContextEnum.INVOICE);
        return processService.createProcess(
                ProcessType.PROCESS_INVOICE_CANCELLATION,
                NOT_STARTED,
                "",
                String.join(";", permissions),
                LocalDate.now(), null, null, false
        );
    }

    private void startProcess(Process process) {
//        applicationEventPublisher.publishEvent(getProcessCreatedEvent(process));
        rabbitMQProducerService.publishProcessEvent( eventFactory.createProcessCreatedEvent(
                EventType.INVOICE_CANCELLATION_PROCESS,
                process
        ));
    }

    private Event getProcessCreatedEvent(Process process) {
        return eventFactory.createProcessCreatedEvent(EventType.INVOICE_CANCELLATION_PROCESS, process);
    }

    private void updateInvoiceCancellationFile(InvoiceCancellationFile invoiceCancellationFile, Long invoiceCancellationId) {
        invoiceCancellationFile.setInvoiceCancellationId(invoiceCancellationId);
        invoiceCancellationFileRepository.saveAndFlush(invoiceCancellationFile);
    }

    private InvoiceCancellation createInvoiceCancellation(Long processId, LocalDate taxEventDate) {
        InvoiceCancellation invoiceCancellation = new InvoiceCancellation();
        invoiceCancellation.setTaxEventDate(taxEventDate);
        invoiceCancellation.setProcessId(processId);
        return invoiceCancellationRepository.saveAndFlush(invoiceCancellation);
    }

    private void validateAndSetTemplate(Long templateId, InvoiceCancellation cancellation) {
        if (Objects.equals(templateId, cancellation.getContractTemplateId())) {
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(
                templateId,
                ContractTemplatePurposes.INVOICE_CANCEL,
                ContractTemplateType.DOCUMENT,
                LocalDate.now()
        )) {
            throw new DomainEntityNotFoundException("templateId-Template with id %s do not exist!;".formatted(templateId));
        }
        cancellation.setContractTemplateId(templateId);
    }


    /**
     * Sets the cancellation number for a given invoice, based on the cancellation date and
     * a pre-configured prefix. The cancellation number is generated as a sequence number appended
     * to the prefix, formatted as a zero-padded string.
     *
     * @param invoice          the invoice for which the cancellation number is to be set
     * @param cancellationDate the date when the invoice was cancelled, used to determine the sequence
     */
    private void setInvoiceCancellationNumber(
            Invoice invoice,
            LocalDate cancellationDate
    ) {
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
     * Creates an InvoiceCancellationDocumentModel for a given invoice and cancellation date.
     *
     * @param invoice          the invoice to be cancelled
     * @param cancellationDate the date on which the cancellation occurs
     * @return an InvoiceCancellationDocumentModel containing cancellation details, company information, and logo
     */
    private InvoiceCancellationDocumentModel createCancellationDocumentModel(
            Invoice invoice,
            LocalDate cancellationDate
    ) {
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
     * Fetches the detailed information of a company for the specified billing date.
     *
     * @param billingDate the date for which the detailed information of the company is to be retrieved
     * @return a CompanyDetailedInformationModel containing the detailed information of the company for the given billing date
     */
    private CompanyDetailedInformationModel fetchCompanyDetailedInformationModel(LocalDate billingDate) {
        return companyDetailRepository.getCompanyDetailedInformation(billingDate);
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

                companyLogoContent = fileService.downloadFile(companyLogo.getFileUrl())
                        .getContentAsByteArray();
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to download company logo");
        }
        return companyLogoContent;
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
    private Document generateCancellationDocument(
            Invoice invoice,
            Pair<ContractTemplateDetail, byte[]> documentTemplateContent,
            InvoiceCancellationDocumentModel model
    ) throws RuntimeException {
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

            signerChainManager.startSign(List.of(document));

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
                            new Attribute(
                                    attributeProperties.getDocumentTypeGuid(),
                                    EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_CANCELLATION_DOCUMENT
                            ),
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
            log.error(
                    "Exception handled while trying to generate invoice cancellation document for invoice with id: [%s]".formatted(
                            invoice.getId()), e
            );
            throw new ClientException(
                    "Exception handled while trying to generate invoice cancellation document for invoice with id: [%s], %s".formatted(
                            invoice.getId(),
                            e.getMessage()
                    ),
                    ErrorCode.APPLICATION_ERROR
            );
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
}
