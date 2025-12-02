package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.reminder.LiabilityForReminder;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModel;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModelMiddleResponse;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.reminder.Reminder;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderLiabilityProcessingMiddleResponse;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderProcessItemRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import io.ebean.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class ReminderDocumentGenerationService extends AbstractDocumentCreationService {

    private static final String REMINDER_LABEL = "Reminder";
    private final ReminderRepository reminderRepository;
    private final CustomerRepository customerRepository;
    private final ReminderProcessItemRepository reminderProcessItemRepository;
    private final CurrencyRepository currencyRepository;
    private final InvoiceRepository invoiceRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final ActionRepository actionRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final DepositRepository depositRepository;

    public ReminderDocumentGenerationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ReminderRepository reminderRepository,
            ReminderProcessItemRepository reminderProcessItemRepository,
            CurrencyRepository currencyRepository,
            InvoiceRepository invoiceRepository,
            LatePaymentFineRepository latePaymentFineRepository,
            ReschedulingRepository reschedulingRepository,
            ActionRepository actionRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            DepositRepository depositRepository,
            CustomerRepository customerRepository
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
        this.reminderRepository = reminderRepository;
        this.reminderProcessItemRepository = reminderProcessItemRepository;
        this.currencyRepository = currencyRepository;
        this.invoiceRepository = invoiceRepository;
        this.latePaymentFineRepository = latePaymentFineRepository;
        this.reschedulingRepository = reschedulingRepository;
        this.actionRepository = actionRepository;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.depositRepository = depositRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Generates a ReminderDocumentModel in JSON format based on the given reminder, liability,
     * customer details, total amount, and communication data.
     *
     * @param reminderId          the unique identifier of the reminder
     * @param liabilityId         the unique identifier of the liability associated with the reminder
     * @param customerDetailId    the unique identifier of the customer details
     * @param totalAmount         the total amount of liabilities in string format
     * @param communicationDataId the unique identifier of the communication data
     * @return a ReminderDocumentModel containing the reminder details in JSON format
     * @throws DomainEntityNotFoundException if the reminder with the given ID is not found
     */
    @Transactional
    public ReminderDocumentModel generateReminderJson(Long reminderId, Long liabilityId, Long customerDetailId, String totalAmount, Long communicationDataId) {
        Reminder reminder = reminderRepository
                .findById(reminderId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reminder not found;"));

        ReminderDocumentModel reminderDocumentModel = buildModelWithSharedInfo(new ReminderDocumentModel());
        reminderDocumentModel.setReminderNumber(reminder.getNumber());
        reminderDocumentModel.setCommunicationNumber(reminder.getNumber());
        reminderDocumentModel.setLiabilitiesTotalAmount(totalAmount);
        reminderDocumentModel.setLiabilities(getLiabilitiesForReminder(liabilityId));

        ReminderDocumentModelMiddleResponse reminderJsonInfoByCustomerId = reminderRepository.getReminderJsonInfoByCustomerDetailId(customerDetailId, communicationDataId);
        reminderDocumentModel.mapToJson(reminderJsonInfoByCustomerId);

        return reminderDocumentModel;
    }

    /**
     * Formats the file name for a document based on various input details.
     *
     * @param reminder           the reminder object containing details such as the create date
     * @param contractTemplateId the contract template detail object which includes template-specific information
     * @param fileId             the unique identifier of the file
     * @return the formatted file name as a string, adhering to length and format constraints
     */
    public String formatDocumentFileName(Reminder reminder, Long contractTemplateId, Long fileId) {
        Optional<ContractTemplateDetail> contractTemplateDetail = contractTemplateDetailsRepository.findByTemplateIdAndVersion(contractTemplateId, null);
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        if (contractTemplateDetail.isPresent()) {
            fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail.get()));
            log.debug("Extracting file name");
            fileNameParts.add(extractFileName(reminder, contractTemplateDetail.get(), fileId));
            log.debug("Extracting file suffix");
            fileNameParts.add(extractFileSuffix(reminder.getCreateDate(), contractTemplateDetail.get()));
            fileNameParts.removeIf(StringUtils::isBlank);
        }

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "MLO" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(FileFormat.PDF.getSuffix()).replaceAll("/", "_");
    }

    /**
     * Extracts a file name based on the given reminder, contract template details, customer detail ID, and file ID.
     * Constructs the file name by iterating through prioritized file name elements in the contract template detail
     * and combining them with relevant data obtained from the provided inputs.
     *
     * @param reminder               The reminder object containing information related to reminders, including the document number.
     * @param contractTemplateDetail The contract template detail object containing file name configurations and priorities.
     * @param fileId                 The ID of the file for which the file name is being generated.
     * @return The generated file name as a string, or the reminder number as a fallback in case of errors or missing data.
     */
    private String extractFileName(Reminder reminder, ContractTemplateDetail contractTemplateDetail, Long fileId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return reminder.getNumber();
            } else {
                List<String> nameParts = new ArrayList<>();
                nameParts.add(REMINDER_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case DOCUMENT_NUMBER -> nameParts.add(reminder.getNumber());
                        case FILE_ID -> nameParts.add(String.valueOf(fileId));
                        case TIMESTAMP -> nameParts.add(reminder.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return reminder.getNumber();
        }
    }

    /**
     * Retrieves a list of customer liabilities associated with a specific reminder.
     *
     * @param reminderId The unique identifier of the reminder.
     * @param customerId The unique identifier of the customer.
     * @return A list of ReminderLiabilityProcessingMiddleResponse objects representing the liabilities for the given reminder and customer.
     */
    public List<ReminderLiabilityProcessingMiddleResponse> getCustomerLiabilitiesByReminder(Long reminderId, Long customerId) {
        return reminderProcessItemRepository.getLiabilitiesForReminderProcessing(reminderId, customerId);
    }

    /**
     * Retrieves and processes the liability specified by the given liability ID for a reminder.
     *
     * @param liabilityId the ID of the liability to be retrieved and processed
     * @return a LiabilityForReminder object containing the processed liability data
     * @throws DomainEntityNotFoundException if no liability is found with the provided ID
     */
    public LiabilityForReminder getLiabilitiesForReminder(Long liabilityId) {
        CustomerLiability customerLiability = customerLiabilityRepository
                .findById(liabilityId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Liability not found;"));

        return processLiability(customerLiability);

    }

    /**
     * Processes the given customer liability and transforms it into a LiabilityForReminder object.
     *
     * @param customerLiability the customer liability to process, containing details about the liability's type, amounts,
     *                          associated documents, and other identifying information.
     * @return a LiabilityForReminder object containing processed liability details such as document number,
     * document prefix, document date, currency name, contract numbers, billing group,
     * and due date.
     */
    private LiabilityForReminder processLiability(CustomerLiability customerLiability) {
        String documentNumber = null;
        String documentPrefix = null;
        LocalDate documentDate = null;
        String billingGroup = null;
        String contractNumbers = reminderRepository.findContractNumbers(customerLiability.getCustomerId());

        BigDecimal initialAmount = customerLiability.getInitialAmount();
        BigDecimal currentAmount = customerLiability.getCurrentAmount();

        if (customerLiability.getOutgoingDocumentType() != null) {
            switch (customerLiability.getOutgoingDocumentType()) {
                case INVOICE -> {
                    Invoice invoice = invoiceRepository
                            .findById(customerLiability.getInvoiceId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                    if (invoice.getBillingId() != null) {
                        billingGroup = reminderRepository.findBillingNumberById(invoice.getBillingId());
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
                            .orElseThrow(() -> new DomainEntityNotFoundException("Late payment not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem()));

                    if (latePaymentFine.getContractBillingGroupId() != null) {
                        billingGroup = reminderRepository.findBillingNumberForLpfById(latePaymentFine.getContractBillingGroupId());
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
            documentNumber = customerLiability.getLiabilityNumber().split("-")[1];
            documentDate = customerLiability.getCreateDate().toLocalDate();
        }

        Currency currency = currencyRepository
                .findById(customerLiability.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find currency with id: %s".formatted(customerLiability.getCurrencyId())));

        return new LiabilityForReminder(
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

    /**
     * Retrieves the customer detail ID associated with the latest liability ID from the provided set of liability IDs.
     * Determines the customer detail ID based on the outgoing document type of the liability.
     *
     * @param liabilityIds a set of liability IDs from which the latest liability ID will be determined
     * @return the customer detail ID associated with the latest liability, or null if it cannot be determined
     * @throws DomainEntityNotFoundException if the latest liability or related entities (e.g., invoice) are not found
     */
    private Long getCustomerDetailIdByOutDoc(Set<Long> liabilityIds) {
        Long latestLiabilityId = Collections.max(liabilityIds);
        Long customerDetailId = null;
        CustomerLiability customerLiability = customerLiabilityRepository
                .findById(latestLiabilityId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Liability not found;"));
        CustomerLiabilitiesOutgoingDocType outgoingDocumentType = customerLiability.getOutgoingDocumentType();
        if (Objects.nonNull(outgoingDocumentType)) {
            if (outgoingDocumentType.equals(CustomerLiabilitiesOutgoingDocType.INVOICE)) {
                Invoice invoice = invoiceRepository
                        .findById(customerLiability.getInvoiceId())
                        .orElseThrow(
                                () -> new DomainEntityNotFoundException("Invoice not found with number " + customerLiability.getOutgoingDocumentFromExternalSystem())
                        );
                customerDetailId = invoice.getCustomerDetailId();
            }
        }
        return customerDetailId;
    }

    @Override
    protected String folderPath() {
        return null;
    }
}
