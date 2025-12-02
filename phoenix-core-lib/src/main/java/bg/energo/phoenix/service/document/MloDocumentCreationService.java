package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.latePaymentFine.FileInfoShortResponse;
import bg.energo.phoenix.model.documentModels.mlo.Manager;
import bg.energo.phoenix.model.documentModels.mlo.MloDocumentModel;
import bg.energo.phoenix.model.documentModels.mlo.ReceivableOrLiability;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerLiabilities;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerReceivables;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLODocumentFile;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ManualLiabilityOffsetting;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.MloCustomerMiddleResponse;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.MLODocumentFileRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;


@Service
@Slf4j
public class MloDocumentCreationService extends AbstractDocumentCreationService {
    private final ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final DepositRepository depositRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final ActionRepository actionRepository;
    private final MLODocumentFileRepository mloDocumentFileRepository;
    private static final String FOLDER_PATH = "mlo_document";

    public MloDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            LatePaymentFineRepository latePaymentFineRepository,
            DepositRepository depositRepository,
            ReschedulingRepository reschedulingRepository,
            ActionRepository actionRepository,
            MLODocumentFileRepository mloDocumentFileRepository
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
        this.manualLiabilityOffsettingRepository = manualLiabilityOffsettingRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.latePaymentFineRepository = latePaymentFineRepository;
        this.depositRepository = depositRepository;
        this.reschedulingRepository = reschedulingRepository;
        this.actionRepository = actionRepository;
        this.mloDocumentFileRepository = mloDocumentFileRepository;
    }

    @Transactional
    public FileInfoShortResponse generateDocument(ManualLiabilityOffsetting mlo, MloDocumentModel mloDocumentModel, Long templateId, FileFormat fileFormat, boolean saveFile, Long customerDetailId) {
        ContractTemplateDetail templateDetail = getContractTemplateLastDetails(templateId);
        String fileName = formatDocumentFileName(mlo, templateDetail, customerDetailId);
        try {
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    templateFileResource,
                    buildFileDestinationPath(),
                    fileName,
                    mloDocumentModel,
                    Set.of(fileFormat),
                    false
            );

            String fileUrl = fileFormat.equals(FileFormat.PDF) ? documentPathPayloads.pdfPath() : documentPathPayloads.docXPath();

            if (saveFile) {
                List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
                Document document = saveDocument(
                        fileName,
                        fileUrl,
                        fileFormat,
                        documentSigners,
                        templateDetail.getTemplateId()
                );
                MLODocumentFile savedFile = mloDocumentFileRepository
                        .saveAndFlush(new MLODocumentFile(
                                        null,
                                        fileName,
                                        documentPathPayloads.pdfPath(),
                                        mlo.getId(),
                                        null,
                                        EntityStatus.ACTIVE)
//                                MLODocumentFile
//                                .builder()
//                                .name(fileName).fileUrl(documentPathPayloads.pdfPath())
//                                .mloId(mlo.getId())
//                                .status(EntityStatus.ACTIVE).build()
                        );

                if (FileFormat.PDF.equals(fileFormat)) {
                    signerChainManager.startSign(List.of(document));
                }
                savedFile.setFileUrl(document.getSignedFileUrl());
                savedFile.setDocumentId(document.getId());
                fileUrl = document.getSignedFileUrl();
            }
            return new FileInfoShortResponse(fileName, fileUrl);
        } catch (Exception e) {
            log.error("Exception while storing template file: {}", e.getMessage());
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    public String formatDocumentFileName(ManualLiabilityOffsetting manualLiabilityOffsetting, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(manualLiabilityOffsetting, contractTemplateDetail, customerDetailId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(manualLiabilityOffsetting.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "MLO" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(FileFormat.PDF.getSuffix()).replaceAll("/", "_");
    }

    private String extractFileName(ManualLiabilityOffsetting manualLiabilityOffsetting, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return manualLiabilityOffsetting.getId().toString();
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
                        case DOCUMENT_NUMBER -> nameParts.add(manualLiabilityOffsetting.getId().toString());
                        case FILE_ID -> nameParts.add(String.valueOf(mloDocumentFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(manualLiabilityOffsetting.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return manualLiabilityOffsetting.getId().toString();
        }
    }

    public MloDocumentModel build(long mloId) {
        ManualLiabilityOffsetting manualLiabilityOffsetting = manualLiabilityOffsettingRepository
                .findById(mloId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Mlo with id " + mloId + " not found"));

        MloDocumentModel mloDocumentModel = buildModelWithSharedInfo(new MloDocumentModel());
        CustomerDetails customerDetails = customerDetailsRepository
                .findById(manualLiabilityOffsetting.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found!"));

        List<Manager> managersByCustomer = customerRepository.getManagersByCustomer(customerDetails.getId());
        MloCustomerMiddleResponse mloCustomerMiddleResponse = manualLiabilityOffsettingRepository
                .getMloInfo(mloId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Couldn't fetch info about mlo!"));

        List<ReceivableOrLiability> liabilitiesForDocument = new ArrayList<>();
        List<ReceivableOrLiability> receivablesForDocument = new ArrayList<>();

        List<Object[]> liabilities = manualLiabilityOffsettingRepository.findLiabilities(mloId);
        for (Object[] liability : liabilities) {
            String documentNumber = null;
            String documentPrefix = null;
            LocalDate documentDate = null;

            CustomerLiability customerLiability = (CustomerLiability) liability[0];
            BigDecimal initialAmount = customerLiability.getInitialAmount();
            BigDecimal currentAmount = customerLiability.getCurrentAmount();
            MLOCustomerLiabilities mloCustomerLiabilities = (MLOCustomerLiabilities) liability[1];
            BigDecimal amountAfter = mloCustomerLiabilities.getAfterCurrentAmount();
            if (customerLiability.getOutgoingDocumentType() != null) {
                switch (customerLiability.getOutgoingDocumentType()) {
                    case INVOICE -> {
                        Invoice invoice = invoiceRepository
                                .findById(customerLiability.getInvoiceId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                        String[] split = invoice.getInvoiceNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = invoice.getInvoiceDate();
                        }

                    }
                    case LATE_PAYMENT_FINE -> {
                        LatePaymentFine latePaymentFine = latePaymentFineRepository
                                .findById(customerLiability.getLatePaymentFineId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Late payment not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                        String[] split = latePaymentFine.getLatePaymentNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = latePaymentFine.getCreateDate().toLocalDate();
                        }

                    }
                    case DEPOSIT -> {
                        Deposit deposit = depositRepository
                                .findById(customerLiability.getDepositId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Deposit not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                        String[] split = deposit.getDepositNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = deposit.getCreateDate().toLocalDate();
                        }
                    }
                    case RESCHEDULING -> {
                        Rescheduling rescheduling = reschedulingRepository
                                .findByNumber(customerLiability.getOutgoingDocumentFromExternalSystem())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Rescheduling not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                        String[] split = rescheduling.getReschedulingNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = rescheduling.getCreateDate().toLocalDate();
                        }
                    }
                    case ACTION -> {
                        Action action = actionRepository
                                .findById(customerLiability.getActionId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                        documentNumber = String.valueOf(action.getId());
                        documentDate = action.getCreateDate().toLocalDate();
                    }
                }
            } else {
                documentNumber = customerLiability.getLiabilityNumber();
                documentDate = customerLiability.getCreateDate().toLocalDate();

            }
            ReceivableOrLiability receivableOrLiability = new ReceivableOrLiability(documentNumber, documentPrefix, documentDate, initialAmount, currentAmount, amountAfter);
            liabilitiesForDocument.add(receivableOrLiability);
        }

        List<Object[]> receivables = manualLiabilityOffsettingRepository.findReceivables(mloId);

        for (Object[] receivable : receivables) {
            String documentNumber = null;
            String documentPrefix = null;
            LocalDate documentDate = null;

            CustomerReceivable customerReceivable = (CustomerReceivable) receivable[0];
            BigDecimal initialAmount = customerReceivable.getInitialAmount();
            BigDecimal currentAmount = customerReceivable.getCurrentAmount();
            MLOCustomerReceivables mloCustomerLiabilities = (MLOCustomerReceivables) receivable[1];
            BigDecimal amountAfter = mloCustomerLiabilities.getAfterCurrentAmount();
            if (customerReceivable.getOutgoingDocumentType() != null) {
                switch (customerReceivable.getOutgoingDocumentType()) {
                    case DEPOSIT -> {
                        Deposit deposit = depositRepository
                                .findByDepositNumber(customerReceivable.getOutgoingDocumentFromExternalSystem())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Deposit not found with number " + customerReceivable.getOutgoingDocumentFromExternalSystem()));

                        String[] split = deposit.getDepositNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = deposit.getCreateDate().toLocalDate();
                        }
                    }
                    case CREDIT_NOTE -> {
                        Invoice invoice = invoiceRepository
                                .findById(customerReceivable.getInvoiceId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with number " + customerReceivable.getOutgoingDocumentFromExternalSystem()));

                        String[] split = invoice.getInvoiceNumber().split("-");

                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = invoice.getCreateDate().toLocalDate();
                        }
                    }
                    case LATE_PAYMENT_FINE -> {
                        LatePaymentFine latePaymentFine = latePaymentFineRepository
                                .findByNumber(customerReceivable.getOutgoingDocumentFromExternalSystem())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Late payment not found with number " + customerReceivable.getOutgoingDocumentFromExternalSystem()));

                        String[] split = latePaymentFine.getLatePaymentNumber().split("-");
                        if (split.length != 2) {
                            log.info("Length is not 2 , incorrect data!");
                        } else {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = latePaymentFine.getCreateDate().toLocalDate();
                        }
                    }
                }
            } else {
                documentNumber = customerReceivable.getReceivableNumber();
                documentDate = customerReceivable.getCreateDate().toLocalDate();
            }
            ReceivableOrLiability receivableOrLiability = new ReceivableOrLiability(documentNumber, documentPrefix, documentDate, initialAmount, currentAmount, amountAfter);
            receivablesForDocument.add(receivableOrLiability);
        }
        mloDocumentModel.from(mloCustomerMiddleResponse, managersByCustomer, liabilitiesForDocument, receivablesForDocument);
        return mloDocumentModel;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
