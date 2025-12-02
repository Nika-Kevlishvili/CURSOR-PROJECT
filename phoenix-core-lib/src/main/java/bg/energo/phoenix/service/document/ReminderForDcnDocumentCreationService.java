package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.documentModels.remiderForDcn.LiabilityForReminderForDcn;
import bg.energo.phoenix.model.documentModels.remiderForDcn.ReminderForDcnDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.ReminderForDcnDocumentMiddleResponse;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFileRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@Slf4j
public class ReminderForDcnDocumentCreationService extends AbstractDocumentCreationService {
    private static final String FOLDER_PATH = "reminder_for_dcn_document";
    private static final String REMINDER_FOR_DISCONNECTION_LABEL = "Reminder_for_Disconnection";
    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final CurrencyRepository currencyRepository;
    private final InvoiceRepository invoiceRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final DepositRepository depositRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final ActionRepository actionRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final PowerSupplyDcnReminderDocFileRepository powerSupplyDcnReminderDocFileRepository;
    private final CustomerRepository customerRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;

    public ReminderForDcnDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository,
            CurrencyRepository currencyRepository,
            InvoiceRepository invoiceRepository,
            LatePaymentFineRepository latePaymentFineRepository,
            DepositRepository depositRepository,
            ReschedulingRepository reschedulingRepository,
            ActionRepository actionRepository,
            ContractTemplateFileRepository contractTemplateFileRepository,
            PowerSupplyDcnReminderDocFileRepository powerSupplyDcnReminderDocFileRepository,
            CustomerRepository customerRepository,
            ContractBillingGroupRepository contractBillingGroupRepository
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
        this.powerSupplyDisconnectionReminderRepository = powerSupplyDisconnectionReminderRepository;
        this.currencyRepository = currencyRepository;
        this.invoiceRepository = invoiceRepository;
        this.latePaymentFineRepository = latePaymentFineRepository;
        this.depositRepository = depositRepository;
        this.reschedulingRepository = reschedulingRepository;
        this.actionRepository = actionRepository;
        this.contractTemplateFileRepository = contractTemplateFileRepository;
        this.powerSupplyDcnReminderDocFileRepository = powerSupplyDcnReminderDocFileRepository;
        this.customerRepository = customerRepository;
        this.contractBillingGroupRepository = contractBillingGroupRepository;
    }

    public Pair<ReminderForDcnDocumentModel, PowerSupplyDcnReminderDocFiles> generateDocument(Long reminderDcnId, Long templateId, Long customerId, FileFormat fileFormat, boolean isGenerated) {
        ReminderForDcnDocumentModel model = buildModelWithSharedInfo(new ReminderForDcnDocumentModel());

        ContractTemplate contractTemplate = contractTemplateRepository
                .findById(templateId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Email Template with id:%s not found;".formatted(templateId))
                );

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .findById(contractTemplate.getLastTemplateDetailId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details with id: %s".formatted(contractTemplate.getLastTemplateDetailId()))
                );

        PowerSupplyDisconnectionReminder reminderForDcn = powerSupplyDisconnectionReminderRepository
                .findById(reminderDcnId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find Reminder for disconnection with id: %s;".formatted(reminderDcnId))
                );

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Customer with id: %s not found".formatted(customerId))
                );

        PowerSupplyDcnReminderDocFiles document = PowerSupplyDcnReminderDocFiles.builder()
                .fileUrl("fileUrl")
                .templateId(templateDetail.getTemplateId())
                .fileName("fileName")
                .status(EntityStatus.ACTIVE)
                .reminderForDcnId(reminderForDcn.getId())
                .customerId(customerId)
                .build();

        String fileName = formatDocumentFileName(reminderForDcn, templateDetail, customer.getLastCustomerDetailId(), powerSupplyDcnReminderDocFileRepository.getNextIdValue());
        document.setFileName(fileName);
        List<ReminderForDcnDocumentMiddleResponse> reminderForDcnDocumentFields = powerSupplyDisconnectionReminderRepository.getReminderForDcnDocumentFields(reminderDcnId);
        for (ReminderForDcnDocumentMiddleResponse response : reminderForDcnDocumentFields) {
            //for testing purposes needed to check only one record. bulk actions will be taken in next task
            if (Long.valueOf(response.getCustomerId()).equals(customerId)) {
                mapDataIntoModel(model, reminderDcnId, Long.valueOf(response.getCustomerId()), response, reminderForDcn);
            }
        }

        try {
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);

            log.debug("Generating document for format: {}", fileFormat);
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            buildFileDestinationPath(),
                            fileName,
                            model,
                            Set.of(fileFormat),
                            false
                    );

            String fileUrl = fileFormat.equals(FileFormat.PDF) ? documentPathPayloads.pdfPath() : documentPathPayloads.docXPath();
            document.setFileUrl(fileUrl);

            if (!isGenerated) {
                List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
                Document savedDocument = saveDocument(
                        fileName,
                        fileUrl,
                        fileFormat,
                        documentSigners,
                        templateDetail.getTemplateId()
                );

                if (FileFormat.PDF.equals(fileFormat)) {
                    signerChainManager.startSign(List.of(savedDocument));
                }
                document.setFileUrl(savedDocument.getSignedFileUrl());
                powerSupplyDcnReminderDocFileRepository.saveAndFlush(document);
            }

            return Pair.of(model, document);

        } catch (Exception e) {
            log.error("Exception while storing template files: {}", e.getMessage());
            throw new ClientException("Exception while storing template files: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }


    private void mapDataIntoModel(ReminderForDcnDocumentModel model, Long reminderDcnId, Long customerId, ReminderForDcnDocumentMiddleResponse response, PowerSupplyDisconnectionReminder reminderForDcn) {
        Currency currency = null;

        String reminderNum = reminderForDcn.getReminderNumber();
        BigDecimal reminderLiabilityTotalAmount = powerSupplyDisconnectionReminderRepository.getReminderLiabilityTotalAmount(reminderForDcn.getId(), customerId);
        if (reminderForDcn.getCurrencyId() != null) {
            currency = currencyRepository
                    .findById(reminderForDcn.getCurrencyId())
                    .orElseThrow(
                            () -> new DomainEntityNotFoundException("Can't find currency with id: %s".formatted(reminderForDcn.getCurrencyId()))
                    );
        } else {
            currency = currencyRepository
                    .findByDefaultSelectionIsTrue()
                    .orElseThrow(
                            () -> new DomainEntityNotFoundException("Can't find currency with id: %s".formatted(reminderForDcn.getCurrencyId()))
                    );
        }

        model.setAdditionalFields(reminderNum, reminderNum, reminderForDcn.getDisconnectionDate(), reminderLiabilityTotalAmount.toString(), currency.getPrintName());
        model.from(response);

        List<LiabilityForReminderForDcn> liabilitiesForReminderForDcn = getLiabilitiesForReminderForDcn(reminderDcnId, customerId);
        model.mapLiabilities(liabilitiesForReminderForDcn);
    }

    public List<LiabilityForReminderForDcn> getLiabilitiesForReminderForDcn(Long reminderForDcnId, Long customerId) {
        List<CustomerLiability> liabilitiesPerCustomer = powerSupplyDisconnectionReminderRepository.getLiabilitiesPerCustomer(reminderForDcnId, customerId);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        try {
            List<CompletableFuture<LiabilityForReminderForDcn>> futures = liabilitiesPerCustomer
                    .stream()
                    .map(customerLiability -> CompletableFuture.supplyAsync(() -> processLiability(customerLiability), executorService))
                    .toList();

            return futures
                    .stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } finally {
            executorService.shutdown();
        }
    }

    private LiabilityForReminderForDcn processLiability(CustomerLiability customerLiability) {
        String documentNumber = null;
        String documentPrefix = null;
        LocalDate documentDate = null;
        String billingGroup = null;
        String contractNumbers = powerSupplyDisconnectionReminderRepository.findContractNumbers(customerLiability.getCustomerId());

        BigDecimal initialAmount = customerLiability.getInitialAmount();
        BigDecimal currentAmount = customerLiability.getCurrentAmount();

        if (customerLiability.getOutgoingDocumentType() != null) {
            switch (customerLiability.getOutgoingDocumentType()) {
                case INVOICE -> {
                    Invoice invoice = invoiceRepository
                            .findById(customerLiability.getInvoiceId())
                            .orElseThrow(
                                    () -> new DomainEntityNotFoundException("Invoice not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                            );

                    if (invoice.getBillingId() != null) {
                        billingGroup = powerSupplyDisconnectionReminderRepository.findBillingNumberById(invoice.getBillingId());
                    }

                    if (invoice.getInvoiceNumber() != null) {
                        String[] split = invoice.getInvoiceNumber().split("-");
                        if (split.length == 2) {
                            documentNumber = split[1];
                            documentPrefix = split[0];
                            documentDate = invoice.getInvoiceDate();
                        } else {
                            log.info("Length is not 2 , incorrect data!");
                        }
                    }
                }
                case LATE_PAYMENT_FINE -> {
                    LatePaymentFine latePaymentFine = latePaymentFineRepository
                            .findById(customerLiability.getLatePaymentFineId())
                            .orElseThrow(
                                    () -> new DomainEntityNotFoundException("Late payment not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                            );

                    if (latePaymentFine.getContractBillingGroupId() != null) {
                        billingGroup = powerSupplyDisconnectionReminderRepository.findBillingNumberForLpfById(latePaymentFine.getContractBillingGroupId());
                    }

                    String[] split = latePaymentFine.getLatePaymentNumber().split("-");
                    if (split.length == 2) {
                        documentNumber = split[1];
                        documentPrefix = split[0];
                        documentDate = latePaymentFine.getCreateDate().toLocalDate();
                    } else {
                        log.info("Length is not 2 , incorrect data!");
                    }
                }
                case DEPOSIT -> {
                    Deposit deposit = depositRepository
                            .findById(customerLiability.getDepositId())
                            .orElseThrow(
                                    () -> new DomainEntityNotFoundException("Deposit not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                            );

                    String[] split = deposit.getDepositNumber().split("-");
                    if (split.length == 2) {
                        documentNumber = split[1];
                        documentPrefix = split[0];
                        documentDate = deposit.getCreateDate().toLocalDate();
                    } else {
                        log.info("Length is not 2 , incorrect data!");
                    }
                }
                case RESCHEDULING -> {
                    Rescheduling rescheduling = reschedulingRepository
                            .findByNumber(customerLiability.getOutgoingDocumentFromExternalSystem())
                            .orElseThrow(
                                    () -> new DomainEntityNotFoundException("Rescheduling not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                            );

                    String[] split = rescheduling.getReschedulingNumber().split("-");
                    if (split.length == 2) {
                        documentNumber = split[1];
                        documentPrefix = split[0];
                        documentDate = rescheduling.getCreateDate().toLocalDate();
                    } else {
                        log.info("Length is not 2 , incorrect data!");
                    }
                }
                case ACTION -> {
                    Action action = actionRepository
                            .findById(customerLiability.getActionId())
                            .orElseThrow(
                                    () -> new DomainEntityNotFoundException("Action not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                            );
                    documentNumber = String.valueOf(action.getId());
                    documentDate = action.getCreateDate().toLocalDate();
                }
            }
        } else {
            documentPrefix = customerLiability.getLiabilityNumber().split("-")[0];
            documentNumber = customerLiability.getLiabilityNumber().split("-")[1];
            documentDate = customerLiability.getCreateDate().toLocalDate();
        }

        Currency currency = currencyRepository
                .findById(customerLiability.getCurrencyId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find currency with id: %s".formatted(customerLiability.getCurrencyId()))
                );

        if (billingGroup == null) {
            Optional<ContractBillingGroup> contractBillingGroup = contractBillingGroupRepository.findById(customerLiability.getContractBillingGroupId());
            if(contractBillingGroup.isPresent()) {
                billingGroup = contractBillingGroup.get().getGroupNumber();
            }
        }

        return new LiabilityForReminderForDcn(
                documentNumber,
                documentPrefix,
                documentDate,
                initialAmount,
                currentAmount,
                currency.getName(),
                contractNumbers,
                billingGroup,
                customerLiability.getDueDate()
        );
    }

    private String getTemplateFileLocalPath(ContractTemplateDetail contractTemplateDetail) {
        ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("Contract Template File with id: [%s] not found or DELETED;".formatted(contractTemplateDetail.getTemplateFileId())));

        String templateFileLocalPath;
        try {
            log.debug("Start downloading template from FTP path: [%s]".formatted(contractTemplateFile.getFileUrl()));
            ByteArrayResource templateFileResource = fileService.downloadFile(contractTemplateFile.getFileUrl());
            Path tempFile = Files.createTempFile("cur", ".docx");
            try {
                Files.write(tempFile, templateFileResource.getByteArray());
                templateFileLocalPath = tempFile.toAbsolutePath().toString();
                log.debug("Template downloaded, local path: [%s]".formatted(templateFileLocalPath));
            } catch (Exception e) {
                throw new ClientException("Exception while reading file: [%s]".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        } catch (Exception e) {
            throw new ClientException("Error handled while trying to download invoice template file;", APPLICATION_ERROR);
        }

        return templateFileLocalPath;
    }

    public String formatDocumentFileName(PowerSupplyDisconnectionReminder reminderForDcn, ContractTemplateDetail contractTemplateDetail, Long customerDetailId, Long fileId) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(reminderForDcn, contractTemplateDetail, customerDetailId, fileId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(reminderForDcn.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "MLO" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(FileFormat.PDF.getSuffix()).replaceAll("/", "_");
    }

    private String extractFileName(PowerSupplyDisconnectionReminder reminderForDcn, ContractTemplateDetail contractTemplateDetail, Long customerDetailId, Long fileId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return reminderForDcn.getReminderNumber();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();
                nameParts.add(REMINDER_FOR_DISCONNECTION_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> {
                            if (customerDetailId != null){
                                String identifier = customerRepository.getCustomerIdentifierByCustomerDetailId(customerDetailId);
                                nameParts.add(identifier);
                            }
                        }
                        case CUSTOMER_NAME -> {
                            if (customerDetailId != null){
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
                        }
                        case CUSTOMER_NUMBER -> {
                            if (customerDetailId != null){
                                Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(customerDetailId, customerCache);
                                Customer customer = customerPair.getKey();

                                nameParts.add(String.valueOf(customer.getCustomerNumber()));
                            }
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(reminderForDcn.getReminderNumber());
                        case FILE_ID -> nameParts.add(String.valueOf(fileId));
                        case TIMESTAMP -> nameParts.add(reminderForDcn.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return reminderForDcn.getReminderNumber();
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
