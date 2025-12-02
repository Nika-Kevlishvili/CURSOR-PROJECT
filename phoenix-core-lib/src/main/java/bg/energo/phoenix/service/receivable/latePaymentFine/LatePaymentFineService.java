package bg.energo.phoenix.service.receivable.latePaymentFine;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.NoRollbackForException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyDetails;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.entity.receivable.AutomaticOffsettingService;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineCommunications;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineInvoices;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.*;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import bg.energo.phoenix.model.request.receivable.latePaymentFine.LatePaymentFineListingRequest;
import bg.energo.phoenix.model.request.receivable.payment.LatePaymentFineForPaymentListingRequest;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.*;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.TemplateFileResponse;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineCommunicationsRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineDocumentFileRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineInvoicesRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.LatePaymentFineDocumentCreationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerLiability.LiabilityDirectOffsettingService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import io.micrometer.core.instrument.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatePaymentFineService {
    public static final String RECEIVABLE_PREFIX = "Receivable-";
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ContractTemplateFileRepository templateFileRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableService customerReceivableService;
    private final LiabilityDirectOffsettingService liabilityDirectOffsettingService;
    private final AutomaticOffsettingService automaticOffsettingService;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final LatePaymentFineDocumentCreationService latePaymentFineDocumentCreationService;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final LatePaymentFineInvoicesRepository latePaymentFineInvoicesRepository;
    private final HolidaysRepository holidaysRepository;
    private final CalendarRepository calendarRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final InvoiceRepository invoiceRepository;
    private final LatePaymentFineCommunicationsRepository latePaymentFineCommunicationsRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final CompanyDetailRepository companyDetailRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final LatePaymentFineDocumentFileRepository latePaymentFineDocumentFileRepository;
    private final PrefixRepository prefixRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final TaskService taskService;
    @Value("${late_payment_fine.prefix.id}")
    private Long latePaymentFinePrefixId;
    private String cachedPrefix;
    @PersistenceContext
    private EntityManager em;

    /**
     * Retrieves a paginated list of late payment fines based on filter criteria.
     *
     * @param request Contains filtering and pagination parameters
     * @return Paginated list of LatePaymentFineListingResponse
     */
    public Page<LatePaymentFineListingResponse> list(LatePaymentFineListingRequest request) {
        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;

        if (request.getDateFrom() != null) {
            dateFrom = toLocalDateTimeToMidnight(request.getDateFrom());
        }
        if (request.getDateTo() != null) {
            dateTo = toLocalDateTimeToUntilMidnight(request.getDateTo());
        }

        return latePaymentFineRepository
                .filter(
                        dateFrom,
                        dateTo,
                        request.getDueDateFrom(),
                        request.getDueDateTo(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getType()),
                        ListUtils.emptyIfNull(request.getCurrencyIds()),
                        getReversed(request.getReversed()),
                        Objects.requireNonNullElse(request.getSearchBy(), LatePaymentFineSearchBy.ALL).name(),
                        request.getTotalAmountFrom(),
                        request.getTotalAmountTo(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSortingField(request.getSortingType()))
                                )
                        )

                )
                .map(LatePaymentFineListingResponse::new);
    }

    /**
     * Reverses a late payment fine, creating a new reversal record and updating the original late payment fine.
     *
     * @param latePaymentFineId The ID of the late payment fine to be reversed.
     * @return The ID of the created reversal record.
     * @throws DomainEntityNotFoundException If the late payment fine with the given ID is not found.
     * @throws OperationNotAllowedException  If the late payment fine is already reversed or is a reversal of another late payment fine.
     */
    @Transactional(noRollbackFor = {NoRollbackForException.class})
    public Long reverse(Long latePaymentFineId, Boolean sendEmail) {
        //The sendEmail should always be false, except when manually called from the late payment fine
        LatePaymentFine latePaymentFine = latePaymentFineRepository.findById(latePaymentFineId)
                .orElseThrow(() -> new DomainEntityNotFoundException("latePaymentFine with id: %s not found!;".formatted(latePaymentFineId)));

        if (latePaymentFine.isReversed()) {
            throw new NoRollbackForException("This latePaymentFine is already reversed");
        }

        if (latePaymentFine.getType().equals(LatePaymentFineType.REVERSAL_OF_LATE_PAYMENT_FINE)) {
            throw new OperationNotAllowedException("It is not possible to reverse reversal of late payment fine");
        }

        LatePaymentFine reversal = mapFromLatePaymentFineIntoReversal(latePaymentFine);
        latePaymentFine.setReversed(true);
        latePaymentFine.setReversalLatePaymentId(reversal.getId());

        Optional<CustomerLiability> customerLiabilityOptional = customerLiabilityRepository.findByLatePaymentFineId(latePaymentFineId);
        if (customerLiabilityOptional.isPresent()) {
            CustomerReceivable receivableIdFromReversalLiability = createReceivableFromLatePaymentFine(latePaymentFine, customerLiabilityOptional.get(), reversal);
            log.info("created receivable from late payment fine with id %s".formatted(receivableIdFromReversalLiability));
            CustomerLiability liability = customerLiabilityOptional.get();
            if (liability.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0 && liability.getCurrentAmount().compareTo(liability.getInitialAmount()) <= 0) {
                executeDirectOffsetting(receivableIdFromReversalLiability, liability.getId());
            }
            customerReceivableService.executeAutomaticOffsetting(receivableIdFromReversalLiability);
        }

        latePaymentFineRepository.saveAndFlush(latePaymentFine);

        if (sendEmail) {
            sendReversalEmails(reversal.getId());
        }

        return reversal.getId();
    }

    /**
     * Sends reversal emails associated with a specific reversal and late payment fine.
     * Utilizes the latePaymentFineDocumentCreationService to generate documents and dispatch emails.
     * Logs the outcome of the email generation and sending process.
     *
     * @param reversalId the ID of the reversal for which emails are to be sent
     */
    private void sendReversalEmails(Long reversalId) {
        try {
            List<Long> emailIds = latePaymentFineDocumentCreationService.generateDocumentAndSendEmailForReversal(reversalId);
            log.info("created emails from late payment fine reversal with id %s".formatted(emailIds.toString()));
        } catch (Exception e) {
            log.error("Failed to create email for late payment fine reversal with id %s: exception: %s".formatted(reversalId, e));
        }
    }

    /**
     * Downloads the file associated with the given ID and returns its content.
     *
     * @param id the unique identifier of the file to be downloaded
     * @return a LatePaymentFineFileContent object containing the name and byte array content of the file
     * @throws DomainEntityNotFoundException if no file with the specified id is found
     */
    public LatePaymentFineFileContent download(Long id) {
        EmailCommunicationAttachment attachment = emailCommunicationAttachmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Attachment with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(attachment.getFileUrl());

        return new LatePaymentFineFileContent(attachment.getName(), resource.getByteArray());
    }

    /**
     * Gets the sorting field value based on the provided sorting type.
     *
     * @param sortingType The type of sorting to apply
     * @return The column name for sorting
     */
    private String getSortingField(LatePaymentFineSortingType sortingType) {
        return sortingType != null ? sortingType.getValue() : LatePaymentFineSortingType.NUMBER.getValue();
    }

    /**
     * Converts LatePaymentFineReversed enum list to string representations.
     *
     * @param reversed List of LatePaymentFineReversed enums
     * @return List of string values ("true"/"false")
     */
    private List<String> getReversed(List<LatePaymentFineReversed> reversed) {
        List<String> reversedStrings = new ArrayList<>();
        if (reversed == null || reversed.isEmpty()) {
            return reversedStrings;
        }
        for (LatePaymentFineReversed rev : reversed) {
            if (rev.equals(LatePaymentFineReversed.YES)) {
                reversedStrings.add("true");
            } else {
                reversedStrings.add("false");
            }
        }
        return reversedStrings;
    }

    /**
     * Finds all late payment fines for a specific customer.
     *
     * @param latePaymentFineForPaymentListingRequest Request containing customer ID and pagination info
     * @return Paginated list of LatePaymentFineShortResponse
     */
    public Page<LatePaymentFineShortResponse> findAllByCustomerId(
            LatePaymentFineForPaymentListingRequest latePaymentFineForPaymentListingRequest
    ) {
        return latePaymentFineRepository.findAllByCustomerId(
                latePaymentFineForPaymentListingRequest.getCustomerId(),
                latePaymentFineForPaymentListingRequest.getPrompt(),
                PageRequest.of(
                        latePaymentFineForPaymentListingRequest.getPage(),
                        latePaymentFineForPaymentListingRequest.getSize()
                )
        );
    }

    /**
     * Converts LocalDate to LocalDateTime at start of day.
     *
     * @param localDate Date to convert
     * @return DateTime at midnight
     */
    private LocalDateTime toLocalDateTimeToMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.MIDNIGHT;
        return LocalDateTime.of(localDate, time);
    }

    /**
     * Converts LocalDate to LocalDateTime at end of day.
     *
     * @param localDate Date to convert
     * @return DateTime at 23:59:59.999999
     */
    private LocalDateTime toLocalDateTimeToUntilMidnight(LocalDate localDate) {
        LocalTime time = LocalTime.of(23, 59, 59, 999999);
        return LocalDateTime.of(localDate, time);
    }

    /**
     * Calculates late payment fine amount for a single liability.
     *
     * @param id   ID of the liability
     * @param date Date to calculate fine for
     * @return Calculated fine amount
     */
    public BigDecimal calculateForSingleLatePaymentFine(Long id, LocalDate date) {
        BigDecimal liabilitiesToFine = customerLiabilityRepository.calculateSingleLatePaymentFine(id, date);

        return liabilitiesToFine;
    }

    /**
     * Retrieves a customer liability by ID.
     *
     * @param id ID of the liability
     * @return CustomerLiability if found, null otherwise
     */
    public CustomerLiability getCustomerLiability(Long id) {
        Optional<CustomerLiability> customerLiabilityOptional = customerLiabilityRepository.findById(id);
        customerLiabilityOptional.ifPresent(liability -> em.refresh(liability));
        return customerLiabilityOptional.orElse(null);
    }

    /**
     * Creates late payment fine and associated liability.
     *
     * @param customerLiability The liable customer
     * @param amount            Fine amount
     * @param errorMessages     List to collect error messages
     * @param result            Calculation result details
     * @return ID of created liability
     */
    public Long createLatePaymentFineAndLiability(
            CustomerLiability customerLiability,
            BigDecimal amount,
            List<String> errorMessages,
            List<ResultItemDTO> result,
            LatePaymentFineOutDocType outDocType,
            LocalDate logicalDate,
            Long reschedulingId
    ) {
        if (customerLiability != null) {
            LatePaymentFine latePaymentFine = createLatePaymentFine(customerLiability, amount, errorMessages, result, outDocType, logicalDate, reschedulingId);

            return createLiability(latePaymentFine.getId());
        } else {
            return null;
        }
    }

    /**
     * Creates a late payment fine and associated liability based on the online payment
     * details for a given customer liability. If the customer liability is not null,
     * the method generates a late payment fine and then creates a liability based on that fine.
     *
     * @param customerLiability The {@link CustomerLiability} object representing the customer's
     *                          liability that will be associated with the late payment fine.
     * @param amount            The {@link BigDecimal} value representing the amount of the late payment fine.
     * @param occurrenceDate    The {@link LocalDate} representing the date the late payment fine
     *                          occurred.
     * @param errorMessages     A list of {@link String} objects to collect error messages in case
     *                          the method encounters issues while processing the payment fine and
     *                          liability creation.
     * @param result            A list of {@link ResultItemDTO} objects that may hold results,
     *                          such as success or failure statuses and other relevant details about
     *                          the operation.
     * @return The ID of the created liability based on the late payment fine. Returns {@code null}
     * if the provided customer liability is {@code null}.
     * @throws NullPointerException If {@code customerLiability} is null, as the operation
     *                              requires a valid liability to create the fine and liability.
     */
    public Long createLatePaymentFineAndLiabilityFromOnlinePayment(
            CustomerLiability customerLiability,
            BigDecimal amount,
            LocalDate occurrenceDate,
            List<String> errorMessages,
            List<ResultItemDTO> result
    ) {
        if (customerLiability != null) {
            LatePaymentFine latePaymentFine = createLatePaymentFineFromOnlinePayment(
                    customerLiability,
                    amount,
                    occurrenceDate,
                    errorMessages,
                    result
            );

            return customerLiabilityService.createLiabilityFromLatePaymentFineOnlinePayment(
                    latePaymentFine.getId()
            );
        } else {
            return null;
        }
    }

    /**
     * Creates a liability linked to a late payment fine.
     *
     * @param id ID of the late payment fine
     * @return ID of created liability
     */
    private Long createLiability(Long id) {
        //TODO liability create logic goes here - doing some mock logic for now
        return customerLiabilityService.createLiabilityFromLatePaymentFine(id);
    }

    /**
     * Retrieves prefix for late payment fine number by ID.
     *
     * @param id ID of prefix
     * @return Prefix string
     * @throws DomainEntityNotFoundException if prefix not found
     */
    private String getSuffixById(Long id) {
        if (cachedPrefix == null) {
            Prefix prefix = prefixRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("prefix not found!;"));
            cachedPrefix = prefix.getName().trim();
        }
        return cachedPrefix;
    }

    /**
     * Creates a new late payment fine record.
     *
     * @param customerLiability The liable customer
     * @param amount            Fine amount
     * @param errorMessages     List to collect error messages
     * @param results           Calculation result details
     * @return Created LatePaymentFine
     */
    private LatePaymentFine createLatePaymentFine(
            CustomerLiability customerLiability,
            BigDecimal amount,
            List<String> errorMessages,
            List<ResultItemDTO> results,
            LatePaymentFineOutDocType outDocType,
            LocalDate logicalDate,
            Long reschedulingId
    ) {
        String prefix = getSuffixById(latePaymentFinePrefixId);
        BigDecimal truncatedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        LatePaymentFine latePaymentFine = new LatePaymentFine();
        latePaymentFine.setCustomerId(customerLiability.getCustomerId()); //TODO should be dinamic
        latePaymentFine.setType(LatePaymentFineType.LATE_PAYMENT_FINE);
        latePaymentFine.setLatePaymentNumber("0000000000");
        latePaymentFine.setAmount(truncatedAmount);
        latePaymentFine.setAmountInOtherCcy(EPBDecimalUtils.roundToTwoDecimalPlaces(getAmountInAltCurrency(truncatedAmount)));
        latePaymentFine.setDueDate(getDueDate(customerLiability));
        latePaymentFine.setCurrencyId(getDefaultCurrency(errorMessages));
        latePaymentFine.setOutDocType(outDocType);
        latePaymentFine.setLogicalDate(logicalDate);
        if (customerLiability.getIncomeAccountNumber() != null) {
            latePaymentFine.setIncomeAccountNumber(customerLiability.getIncomeAccountNumber());
        }
        if (customerLiability.getCostCenterControllingOrder() != null) {
            latePaymentFine.setConstCentreControllingOrder(customerLiability.getCostCenterControllingOrder());
        }
        if (customerLiability.getContractBillingGroupId() != null) {
            latePaymentFine.setContractBillingGroupId(customerLiability.getContractBillingGroupId());
        }
        Optional<Long> lastVersionId = companyDetailRepository.findMaxVersionId();
        if (lastVersionId.isPresent()) {
            Optional<CompanyDetails> companyDetails = companyDetailRepository.findByVersionId(lastVersionId.get());
            companyDetails.ifPresent(companyDetails1 -> latePaymentFine.setIssuer(companyDetails1.getName()));
        }
        latePaymentFine.setTemplateId(
                contractTemplateRepository
                        .findByTemplatePurposeAndDefaultForLatePaymentFineEmail(
                                ContractTemplatePurposes.LATE_PAYMENT_FINE,
                                true
                        )
                        .map(ContractTemplate::getId)
                        .orElse(null)
        );
        latePaymentFine.setDocumentTemplateId(
                contractTemplateRepository
                        .findByTemplatePurposeAndDefaultForLatePaymentFineDocument(
                                ContractTemplatePurposes.LATE_PAYMENT_FINE,
                                true
                        ).map(ContractTemplate::getId)
                        .orElse(null)
        );
        latePaymentFine.setReversed(false);
        latePaymentFine.setParentLiabilityId(customerLiability.getId());
        latePaymentFine.setReschedulingId(reschedulingId);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        latePaymentFineRepository.saveAndFlush(latePaymentFine);
        latePaymentFine.setLatePaymentNumber(generateNextLatePaymentNumber(prefix));

        createLatePaymentFineInvoice(results, latePaymentFine.getId(), customerLiability.getInvoiceId());

        updateLiability(customerLiability, latePaymentFine.getId());
        return latePaymentFineRepository.save(latePaymentFine);
    }

    /**
     * Creates a LatePaymentFine from an online payment, including updating
     * necessary fields and persisting the fine.
     *
     * @param customerLiability the liability of the customer related to the payment
     * @param amount            the amount of the late payment fine
     * @param errorMessages     a list of error messages to be used for logging or exception handling
     * @param results           a list of ResultItemDTO objects possibly impacted or used during fine creation
     * @return the created and persisted LatePaymentFine
     */
    private LatePaymentFine createLatePaymentFineFromOnlinePayment(
            CustomerLiability customerLiability,
            BigDecimal amount,
            LocalDate occurrenceDate,
            List<String> errorMessages,
            List<ResultItemDTO> results
    ) {
        String prefix = getSuffixById(latePaymentFinePrefixId);
        BigDecimal truncatedAmount = amount.setScale(2, RoundingMode.DOWN);

        LatePaymentFine latePaymentFine = new LatePaymentFine();
        latePaymentFine.setType(LatePaymentFineType.LATE_PAYMENT_FINE);
        latePaymentFine.setCustomerId(customerLiability.getCustomerId());
        latePaymentFine.setLatePaymentNumber("0000000000");
        latePaymentFine.setAmount(truncatedAmount);
        latePaymentFine.setReversed(false);
        latePaymentFine.setAmountInOtherCcy(getAmountInAltCurrency(truncatedAmount));
        latePaymentFine.setDueDate(getDueDate(customerLiability));
        latePaymentFine.setLogicalDate(occurrenceDate);
        latePaymentFine.setCurrencyId(getDefaultMainCurrency(errorMessages));
        latePaymentFine.setIncomeAccountNumber(customerLiability.getIncomeAccountNumber());
        latePaymentFine.setConstCentreControllingOrder(customerLiability.getCostCenterControllingOrder());
        latePaymentFine.setContractBillingGroupId(customerLiability.getContractBillingGroupId());
        latePaymentFine.setParentLiabilityId(customerLiability.getId());
        latePaymentFine.setIssuer(
                companyDetailRepository
                        .findMaxVersionId()
                        .flatMap(
                                versionId -> companyDetailRepository
                                        .findByVersionId(versionId)
                                        .map(CompanyDetails::getName)
                        )
                        .orElse(null)
        );

        latePaymentFine.setTemplateId(
                contractTemplateRepository
                        .findByTemplatePurposeAndDefaultForLatePaymentFineEmail(
                                ContractTemplatePurposes.LATE_PAYMENT_FINE,
                                true
                        )
                        .map(ContractTemplate::getId)
                        .orElse(null)
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        latePaymentFine.setSystemUserId("system.admin");
        latePaymentFineRepository.saveAndFlush(latePaymentFine);
        latePaymentFine.setLatePaymentNumber(generateNextLatePaymentNumber(prefix));
        latePaymentFine.setOutDocType(LatePaymentFineOutDocType.ONLINE_PAYMENT);
        createLatePaymentFineInvoiceFromOnlinePayment(
                results,
                latePaymentFine.getId(),
                customerLiability.getInvoiceId()
        );

        latePaymentFine.setDocumentTemplateId(
                contractTemplateRepository
                        .findByTemplatePurposeAndDefaultForLatePaymentFineDocument(
                                ContractTemplatePurposes.LATE_PAYMENT_FINE,
                                true
                        ).map(ContractTemplate::getId)
                        .orElse(null)
        );

        updateLiability(customerLiability, latePaymentFine.getId());
        return latePaymentFineRepository.save(latePaymentFine);
    }

    /**
     * Updates customer liability with reference to late payment fine.
     *
     * @param customerLiability Liability to update
     * @param latePaymentId     ID of late payment fine
     */
    private void updateLiability(CustomerLiability customerLiability, Long latePaymentId) {
        customerLiability.setChildLatePaymentFineId(latePaymentId);
        customerLiabilityRepository.save(customerLiability);
    }

    /**
     * Creates invoice records for late payment fine.
     *
     * @param results       Calculation result details
     * @param latePaymentId ID of late payment fine
     * @param invoiceId     ID of original invoice
     */
    public void createLatePaymentFineInvoice(List<ResultItemDTO> results, Long latePaymentId, Long invoiceId) {
        List<LatePaymentFineInvoices> latePaymentFineInvoices = new ArrayList<>();
        if (results != null) {
            for (ResultItemDTO result : results) {
                String invoiceNumber = null;
                if (invoiceId != null) {
                    Optional<Invoice> invoice = invoiceRepository.findById(invoiceId);
                    invoiceNumber = invoice.map(Invoice::getInvoiceNumber).orElse(null);
                }
                LocalDate startDate = result.getPeriodFrom();
                LocalDate endDate = result.getPeriodTo();
                Long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                LatePaymentFineInvoices latePaymentFineInvoice = new LatePaymentFineInvoices();
                latePaymentFineInvoice.setLatePaymentFineId(latePaymentId);
                latePaymentFineInvoice.setLatePaidAmount(result.getIntervalAmount());
                latePaymentFineInvoice.setOverdueStartDate(result.getPeriodFrom());
                latePaymentFineInvoice.setOverdueEndDate(result.getPeriodTo());
                latePaymentFineInvoice.setNumberOfDays(daysBetween);
                latePaymentFineInvoice.setInvoiceId(invoiceId);
                latePaymentFineInvoice.setInvoiceNumber(invoiceNumber);
                latePaymentFineInvoice.setPercentage(result.getApplicableInterestRate());
                latePaymentFineInvoice.setTotalAmount(result.getCalculatedInterest());
                latePaymentFineInvoice.setFee(result.getFee());
                latePaymentFineInvoice.setCurrencyId(result.getCurrencyId());
                latePaymentFineInvoices.add(latePaymentFineInvoice);
            }
        }
        latePaymentFineInvoicesRepository.saveAll(latePaymentFineInvoices);
    }

    /**
     * Creates and saves a list of LatePaymentFineInvoices from the given online payment results.
     * For each result item, this method calculates the late payment details and constructs
     * a corresponding LatePaymentFineInvoice, which is then saved to the repository.
     *
     * @param results       a list of ResultItemDTO objects containing information about payment periods and amounts
     * @param latePaymentId the unique identifier for the late payment fine
     * @param invoiceId     the identifier of the invoice related to the late payment fine
     */
    public void createLatePaymentFineInvoiceFromOnlinePayment(List<ResultItemDTO> results, Long latePaymentId, Long invoiceId) {
        List<LatePaymentFineInvoices> latePaymentFineInvoices = new ArrayList<>();
        if (results != null) {
            for (ResultItemDTO result : results) {
                String invoiceNumber = null;
                if (invoiceId != null) {
                    Optional<Invoice> invoice = invoiceRepository.findById(invoiceId);
                    invoiceNumber = invoice.map(Invoice::getInvoiceNumber).orElse(null);
                }
                LocalDate startDate = result.getPeriodFrom();
                LocalDate endDate = result.getPeriodTo();
                Long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                LatePaymentFineInvoices latePaymentFineInvoice = new LatePaymentFineInvoices();
                latePaymentFineInvoice.setLatePaymentFineId(latePaymentId);
                latePaymentFineInvoice.setLatePaidAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(result.getIntervalAmount()));
                latePaymentFineInvoice.setOverdueStartDate(result.getPeriodFrom());
                latePaymentFineInvoice.setOverdueEndDate(result.getPeriodTo());
                latePaymentFineInvoice.setNumberOfDays(daysBetween);
                latePaymentFineInvoice.setInvoiceId(invoiceId);
                latePaymentFineInvoice.setInvoiceNumber(invoiceNumber);
                latePaymentFineInvoice.setPercentage(result.getApplicableInterestRate());
                latePaymentFineInvoice.setTotalAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(result.getCalculatedInterest()));
                latePaymentFineInvoice.setFee(EPBDecimalUtils.roundToTwoDecimalPlaces(result.getFee()));
                latePaymentFineInvoice.setCurrencyId(result.getCurrencyId());
                latePaymentFineInvoice.setSystemUserId("system.admin");
                latePaymentFineInvoices.add(latePaymentFineInvoice);
            }
        }
        latePaymentFineInvoicesRepository.saveAll(latePaymentFineInvoices);
    }

    /**
     * Generates next sequential number for late payment fine.
     *
     * @param prefix Prefix to use
     * @return Generated number
     */
    public String generateNextLatePaymentNumber(String prefix) {
        return latePaymentFineRepository.getLatestNumberOfTheLatePaymentNumber()
                .map(lastNumber -> incrementLatePaymentNumber(lastNumber, prefix))
                .orElse(String.format("%s-0000000001", prefix));
    }

    /**
     * Increments the last used late payment number.
     *
     * @param lastNumber Previous number
     * @param prefix     Prefix to use
     * @return Incremented number
     */
    private String incrementLatePaymentNumber(String lastNumber, String prefix) {
        long nextNumber = Long.parseLong(lastNumber) + 1;
        return String.format("%s-%010d", prefix, nextNumber);
    }

    /**
     * Calculates amount in alternate currency.
     *
     * @param amount Original amount
     * @return Converted amount using default currency exchange rate
     */
    private BigDecimal getAmountInAltCurrency(BigDecimal amount) {
        Optional<Currency> defaultCurrency = currencyRepository.findByDefaultSelectionIsTrue();
        if (defaultCurrency.isPresent()) {
            BigDecimal altExchangeRate = defaultCurrency.get().getAltCurrencyExchangeRate();
            if (altExchangeRate != null) {
                return amount.multiply(altExchangeRate);
            }
        }
        return amount;
    }

    /**
     * Gets ID of default currency.
     *
     * @param errorMessages List to collect error messages
     * @return ID of default currency
     */
    private Long getDefaultCurrency(List<String> errorMessages) {
        Optional<Currency> defaultCurrency = currencyRepository.findByDefaultSelectionIsTrue();
        if (defaultCurrency.isPresent()) {
            return defaultCurrency.get().getId();
        }
        errorMessages.add("Can't find default currency");
        return null;
    }

    /**
     * Retrieves the ID of the default main currency if present.
     * If the default main currency is not available, an error message is added to the provided list.
     *
     * @param errorMessages a list to which an error message is added if the default currency cannot be found
     * @return the ID of the default main currency, or null if it cannot be found
     */
    private Long getDefaultMainCurrency(List<String> errorMessages) {
        Optional<Currency> defaultCurrency = currencyRepository.findMainCurrencyNowAndActive();
        if (defaultCurrency.isPresent()) {
            return defaultCurrency.get().getId();
        }
        errorMessages.add("Can't find default currency");
        return null;
    }

    /**
     * Retrieves detailed view of a late payment fine.
     *
     * @param latePaymentFineId ID of late payment fine
     * @return Detailed response containing fine information
     * @throws DomainEntityNotFoundException if fine not found
     */
    public LatePaymentFineResponse view(Long latePaymentFineId) {
        log.info("Previewing Late PaymentFine with id: %s".formatted(latePaymentFineId));
        LatePaymentFine latePaymentFine = latePaymentFineRepository
                .findById(latePaymentFineId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Late payment fine with id: %s not found".formatted(latePaymentFineId))
                );

        LatePaymentFineResponse latePaymentFineResponse = new LatePaymentFineResponse(latePaymentFine);
        Currency currency = currencyRepository
                .findCurrencyByIdAndStatuses(latePaymentFine.getCurrencyId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("%s - Currency not found;".formatted(latePaymentFine.getCurrencyId()))
                );

        CurrencyShortResponse currencyShortResponse = new CurrencyShortResponse(currency);
        latePaymentFineResponse.setCurrencyShortResponse(currencyShortResponse);
        LatePaymentCustomerShortResponse customerShortResponse = customerDetailsRepository.findCustomerForLiabilityShortResponse(latePaymentFine.getCustomerId());
        latePaymentFineResponse.setCustomerShortResponse(customerShortResponse);

        if (latePaymentFine.getContractBillingGroupId() != null) {
            ContractBillingGroup contractBillingGroup = contractBillingGroupRepository
                    .findById(latePaymentFine.getContractBillingGroupId())
                    .orElseThrow(
                            () -> new DomainEntityNotFoundException("Billing group not found with id: %s".formatted(latePaymentFine.getContractBillingGroupId()))
                    );

            latePaymentFineResponse.setContractBillingGroupShortResponse(new ShortResponse(contractBillingGroup.getId(), contractBillingGroup.getGroupNumber()));
            latePaymentFineResponse.setContractBillingGroupId(contractBillingGroup.getId());
        }

        Optional<List<Long>> contractDetailIds = latePaymentFineRepository.getCustomerDetailId(latePaymentFine.getId());
        contractDetailIds.ifPresent(latePaymentFineResponse::setContractDetailIds);

        Optional<Long> lastVersionId = companyDetailRepository.findMaxVersionId();
        latePaymentFineResponse.setIssuerName(latePaymentFine.getIssuer());
        lastVersionId.ifPresent(latePaymentFineResponse::setIssuerVersionId);

        List<LatePaymentFineInvoices> latePaymentFineInvoices = latePaymentFineInvoicesRepository.findAllByLatePaymentFineId(latePaymentFine.getId());
        List<LatePaymentFineInvoiceTableResponse> latePaymentFineInvoiceTableResponses = latePaymentFineInvoices
                .stream()
                .map(LatePaymentFineInvoiceTableResponse::new)
                .toList();

        latePaymentFineResponse.setTableResponse(latePaymentFineInvoiceTableResponses);
        latePaymentFineResponse.setLatePaymentFineReversalShortResponse(getLatePaymentFineShortResponse(latePaymentFine.getReversalLatePaymentId()));
        latePaymentFineResponse.setParentLatePaymentFineShortResponse(getLatePaymentFineShortResponse(latePaymentFine.getParentLpfId()));
        setCommunicationShortResponse(latePaymentFine.getId(), latePaymentFineResponse);
        setParentLiabilityShortResponse(latePaymentFine.getParentLiabilityId(), latePaymentFineResponse);
        setReschedulingShortResponse(latePaymentFine.getReschedulingId(), latePaymentFineResponse);
        latePaymentFineResponse.setOutDocType(latePaymentFine.getOutDocType());

        if (Objects.nonNull(latePaymentFine.getDocumentTemplateId())) {
            ContractTemplateDetail contractTemplateDetail = getContractTemplateDetail(latePaymentFine.getDocumentTemplateId());
            latePaymentFineResponse.setTemplateShortResponse(new ShortResponse(contractTemplateDetail.getTemplateId(), contractTemplateDetail.getName()));
        }

        latePaymentFineResponse.setTasks(getTasks(latePaymentFine.getId()));
        return latePaymentFineResponse;
    }

    /**
     * Retrieves the contract template detail for the specified template ID.
     *
     * @param templateId the unique identifier of the contract template
     * @return the ContractTemplateDetail associated with the given template ID
     * @throws DomainEntityNotFoundException if no contract template is found with the provided ID
     *                                       or if no contract template detail is associated with the last template detail ID
     */
    private ContractTemplateDetail getContractTemplateDetail(Long templateId) {
        ContractTemplate contractTemplate = contractTemplateRepository
                .findById(templateId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template not found by given id: %s".formatted(templateId)));

        return contractTemplateDetailsRepository
                .findById(contractTemplate.getLastTemplateDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Template detail not found by given id: %s".formatted(contractTemplate.getLastTemplateDetailId())));
    }

    /**
     * Calculates the due date for a customer liability based on the invoice payment terms.
     *
     * @param customerLiability the customer liability for which to calculate the due date
     * @return the due date for the customer liability, or the current date if the invoice ID is null
     */
    private LocalDate getDueDate(CustomerLiability customerLiability) {
        Long invoiceId = customerLiability.getInvoiceId();
        if (invoiceId != null) {
            Long invoicePaymentTermsId = latePaymentFineRepository.findPaymentTermIdByInvoiceId(customerLiability.getInvoiceId());
            if (invoicePaymentTermsId != null) {
                InvoicePaymentTerms invoicePaymentTerms = invoicePaymentTermsRepository.findById(invoicePaymentTermsId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Invoice Payment Terms with id: %s".formatted(invoicePaymentTermsId)));
                return calculateDueDate(invoicePaymentTerms);
            }
        }
        return LocalDate.now();
    }

    /**
     * Calculates the due date for an invoice payment based on the provided invoice payment terms.
     * The due date is calculated by adding the specified number of days to the current date, while
     * optionally excluding weekends and/or holidays based on the configured calendar.
     *
     * @param invoicePaymentTerms The invoice payment terms object containing the configuration for
     *                            calculating the due date.
     * @return The calculated due date.
     */
    private LocalDate calculateDueDate(InvoicePaymentTerms invoicePaymentTerms) {
        Boolean excludeHolidays = invoicePaymentTerms.getExcludeHolidays();
        Boolean excludeWeekends = invoicePaymentTerms.getExcludeWeekends();

        Calendar calendar = calendarRepository
                .findById(invoicePaymentTerms.getCalendarId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar with id: [%s] not found".formatted(invoicePaymentTerms.getCalendarId())));

        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "").split(";"))
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();

        List<LocalDate> holidayDates = holidaysRepository.findAllByCalendarId(calendar.getId())
                .stream()
                .map(Holiday::getHoliday)
                .map(LocalDateTime::toLocalDate)
                .toList();

        LocalDate targetDay = LocalDate.now();
        int daysAdded = 0;

        // Determine if we're counting working days or calendar days
        boolean countWorkingDays = Boolean.TRUE.equals(excludeWeekends) || Boolean.TRUE.equals(excludeHolidays);

        int daysToAdd = 0;
        if (invoicePaymentTerms.getValue() != null) {
            daysToAdd = invoicePaymentTerms.getValue();
        }

        while (daysAdded < daysToAdd) {
            targetDay = targetDay.plusDays(1);

            if (countWorkingDays) {
                if ((!Boolean.TRUE.equals(excludeWeekends) || !weekends.contains(targetDay.getDayOfWeek())) &&
                        (!Boolean.TRUE.equals(excludeHolidays) || !holidayDates.contains(targetDay))) {
                    daysAdded++;
                }
            } else {
                daysAdded++;
            }
        }

        // Adjust the final day if it falls on a weekend or holiday
        if (countWorkingDays) {
            int stopFlag = 0;
            while (stopFlag < 1000) {
                if ((Boolean.TRUE.equals(excludeHolidays) && holidayDates.contains(targetDay)) ||
                        (Boolean.TRUE.equals(excludeWeekends) && weekends.contains(targetDay.getDayOfWeek()))) {
                    switch (invoicePaymentTerms.getDueDateChange()) {
                        case PREVIOUS_WORKING_DAY -> targetDay = targetDay.minusDays(1);
                        case NEXT_WORKING_DAY -> targetDay = targetDay.plusDays(1);
                    }
                    stopFlag++;
                } else {
                    return targetDay;
                }
            }
            throw new IllegalArgumentException("Cannot calculate due date");
        }

        return targetDay;
    }

    /**
     * Maps a late payment fine into a reversal late payment fine.
     *
     * @param latePaymentFineOld the existing late payment fine to be reversed
     * @return the ID of the created reversal late payment fine
     */
    public LatePaymentFine mapFromLatePaymentFineIntoReversal(LatePaymentFine latePaymentFineOld) {
        String prefix = getSuffixById(latePaymentFinePrefixId);

        LatePaymentFine latePaymentFine = new LatePaymentFine();
        latePaymentFine.setCustomerId(latePaymentFineOld.getCustomerId());
        latePaymentFine.setType(LatePaymentFineType.REVERSAL_OF_LATE_PAYMENT_FINE);
        latePaymentFine.setLatePaymentNumber("0000000000");
        latePaymentFine.setAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFineOld.getAmount().negate()));
        latePaymentFine.setAmountInOtherCcy(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFineOld.getAmountInOtherCcy().negate()));
        latePaymentFine.setDueDate(LocalDate.now());
        latePaymentFine.setCurrencyId(latePaymentFineOld.getCurrencyId());
        latePaymentFine.setIncomeAccountNumber(latePaymentFineOld.getIncomeAccountNumber());
        latePaymentFine.setConstCentreControllingOrder(latePaymentFineOld.getConstCentreControllingOrder());
        latePaymentFine.setContractBillingGroupId(latePaymentFineOld.getContractBillingGroupId());
        latePaymentFine.setIssuer(latePaymentFineOld.getIssuer());
        latePaymentFine.setTemplateId(latePaymentFineOld.getTemplateId());
        latePaymentFine.setDocumentTemplateId(latePaymentFineOld.getDocumentTemplateId());
        latePaymentFine.setReversed(true);
        latePaymentFine.setLogicalDate(LocalDate.now());
        latePaymentFine.setFileUrl(latePaymentFineOld.getFileUrl());
        latePaymentFine.setParentLiabilityId(latePaymentFineOld.getParentLiabilityId());
        latePaymentFine.setParentLpfId(latePaymentFineOld.getId());
        latePaymentFineRepository.saveAndFlush(latePaymentFine);
        latePaymentFine.setLatePaymentNumber(generateNextLatePaymentNumber(prefix));

        List<LatePaymentFineInvoices> latePaymentFineInvoices = latePaymentFineInvoicesRepository.findAllByLatePaymentFineId(latePaymentFineOld.getId());
        createReversalInvoice(latePaymentFineInvoices, latePaymentFine.getId());

        return latePaymentFineRepository.saveAndFlush(latePaymentFine);
    }

    /**
     * Creates reversal invoices for the provided list of late payment fine invoices.
     *
     * @param latePaymentFineInvoices the list of late payment fine invoices to create reversals for
     * @param latePaymentId           the ID of the late payment fine to associate the reversal invoices with
     */
    public void createReversalInvoice(List<LatePaymentFineInvoices> latePaymentFineInvoices, Long latePaymentId) {
        List<LatePaymentFineInvoices> reversalInvoices = new ArrayList<>();
        if (latePaymentFineInvoices != null) {
            for (LatePaymentFineInvoices oldInvoice : latePaymentFineInvoices) {
                LatePaymentFineInvoices latePaymentFineInvoice = new LatePaymentFineInvoices();
                latePaymentFineInvoice.setLatePaymentFineId(latePaymentId);
                latePaymentFineInvoice.setLatePaidAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(oldInvoice.getLatePaidAmount()));
                latePaymentFineInvoice.setOverdueStartDate(oldInvoice.getOverdueStartDate());
                latePaymentFineInvoice.setOverdueEndDate(oldInvoice.getOverdueEndDate());
                latePaymentFineInvoice.setNumberOfDays(oldInvoice.getNumberOfDays());
                latePaymentFineInvoice.setInvoiceId(oldInvoice.getInvoiceId());
                latePaymentFineInvoice.setInvoiceNumber(oldInvoice.getInvoiceNumber());
                latePaymentFineInvoice.setPercentage(oldInvoice.getPercentage());
                latePaymentFineInvoice.setTotalAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(oldInvoice.getTotalAmount().negate()));
                latePaymentFineInvoice.setFee(EPBDecimalUtils.roundToTwoDecimalPlaces(oldInvoice.getFee()));
                latePaymentFineInvoice.setCurrencyId(oldInvoice.getCurrencyId());
                reversalInvoices.add(latePaymentFineInvoice);
            }
        }
        latePaymentFineInvoicesRepository.saveAll(reversalInvoices);
    }

    /**
     * Creates a new customer receivable from a late payment fine.
     *
     * @param latePaymentFine the late payment fine to create the receivable from
     * @return the ID of the created customer receivable
     */
    private CustomerReceivable createReceivableFromLatePaymentFine(
            LatePaymentFine latePaymentFine,
            CustomerLiability customerLiability,
            LatePaymentFine reversal
    ) {
        CustomerReceivable customerReceivable = new CustomerReceivable();
        Optional<CacheObject> accountingPeriodsByDate = accountingPeriodsRepository.findAccountingPeriodsByDate(LocalDateTime.now());
        accountingPeriodsByDate.ifPresent(cacheObject -> customerReceivable.setAccountPeriodId(cacheObject.getId()));
        customerReceivable.setInitialAmount(latePaymentFine.getAmount());
        customerReceivable.setCurrencyId(latePaymentFine.getCurrencyId());
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(latePaymentFine.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (currencyOptional.isPresent()) {
            BigDecimal exchangeRate = currencyOptional.get().getAltCurrencyExchangeRate();
            BigDecimal amountInOtherCurrency = null;
            if (exchangeRate != null) {
                amountInOtherCurrency = latePaymentFine.getAmount().multiply(exchangeRate);
                amountInOtherCurrency = amountInOtherCurrency.setScale(2, RoundingMode.HALF_UP);
            }
            customerReceivable.setInitialAmountInOtherCurrency(amountInOtherCurrency);
            customerReceivable.setCurrentAmountInOtherCurrency(amountInOtherCurrency);
        }
        LocalDate dueDate = null;
        if (customerLiability.getInvoiceId() != null) {
            dueDate = getDueDate(customerLiability);

        } else {
            dueDate = LocalDate.now();
        }
        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LPF_RECEIVABLES.name());
        if (incomeAccountNumber == null) {
            throw new DomainEntityNotFoundException("Default income account not found for receivables!;");
        }
        customerReceivable.setOccurrenceDate(LocalDate.now());
        customerReceivable.setCurrentAmount(latePaymentFine.getAmount());
        customerReceivable.setCustomerId(latePaymentFine.getCustomerId());
        customerReceivable.setCostCenterControllingOrder(latePaymentFine.getConstCentreControllingOrder());
        customerReceivable.setBillingGroupId(latePaymentFine.getContractBillingGroupId());
        customerReceivable.setOutgoingDocumentType(OutgoingDocumentType.LATE_PAYMENT_FINE);
        customerReceivable.setOutgoingDocumentFromExternalSystem(reversal.getLatePaymentNumber());
        customerReceivable.setLatePaymentFineId(reversal.getId());
        customerReceivable.setReceivableNumber("TEMP");
        customerReceivable.setDueDate(dueDate);
        customerReceivable.setIncomeAccountNumber(incomeAccountNumber);
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        CustomerReceivable customerReceivableDb = customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
//        em.detach(customerReceivableDb);
        return customerReceivableDb;
    }

    /**
     * Executes a direct offsetting of a receivable against a liability.
     *
     * @param receivable  the receivable to be offset
     * @param liabilityId the ID of the liability to be offset
     */
    private void executeDirectOffsetting(CustomerReceivable receivable, Long liabilityId) {
        em.refresh(receivable);
        liabilityDirectOffsettingService.directOffsetting(
                DirectOffsettingSourceType.RECEIVABLE,
                receivable.getId(),
                liabilityId,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(), null,
                null, null
        );
    }

    /**
     * Executes the automatic offsetting of a customer receivable.
     *
     * @param customerReceivable the customer receivable to be offset
     */
    public void executeAutomaticOffsetting(CustomerReceivable customerReceivable) {
        em.detach(customerReceivable);

        automaticOffsettingService.offsetOfLiabilityAndReceivable(
                customerReceivable.getId(),
                null,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        );
    }

    /**
     * Retrieves a short response object for a late payment fine based on its ID.
     *
     * @param lpfId the ID of the late payment fine to retrieve. If null or not found, the method will return null.
     * @return a LatePaymentFineReversalShortResponse object containing a subset of details about the late payment fine
     * or null if the provided ID is null or the fine is not found.
     */
    private LatePaymentFineReversalShortResponse getLatePaymentFineShortResponse(Long lpfId) {
        if (Objects.nonNull(lpfId)) {
            Optional<LatePaymentFine> LpfOptional = latePaymentFineRepository.findById(lpfId);
            if (LpfOptional.isPresent()) {
                LatePaymentFine latePaymentFine = LpfOptional.get();
                LatePaymentFineReversalShortResponse latePaymentFineReversalShortResponse = new LatePaymentFineReversalShortResponse();
                latePaymentFineReversalShortResponse.setId(latePaymentFine.getId());
                latePaymentFineReversalShortResponse.setDate(latePaymentFine.getCreateDate().toLocalDate());
                latePaymentFineReversalShortResponse.setLatePaymentNumber(latePaymentFine.getLatePaymentNumber());
                return latePaymentFineReversalShortResponse;
            }
        }
        return null;
    }

    /**
     * Sets the short communication responses for a late payment fine.
     * If the list of communications related to the fine is empty, it initializes
     * and assigns short communication responses to the provided response object.
     *
     * @param latePaymentFineId       the identifier of the late payment fine
     * @param latePaymentFineResponse the response object to add the communication details to
     */
    private void setCommunicationShortResponse(Long latePaymentFineId, LatePaymentFineResponse latePaymentFineResponse) {
        List<LatePaymentFineCommunications> latePaymentFineCommunications = latePaymentFineCommunicationsRepository.findByLatePaymentId(latePaymentFineId);
        if (!CollectionUtils.isEmpty(latePaymentFineCommunications)) {
            List<CommunicationShortResponse> communicationShortResponseList = new ArrayList<>();
            List<TemplateFileResponse> fileResponse = new ArrayList<>();
            for (LatePaymentFineCommunications latePaymentFineCommunication : latePaymentFineCommunications) {
                CommunicationShortResponse communicationShortResponse = new CommunicationShortResponse();
                communicationShortResponse.setId(latePaymentFineCommunication.getEmailCommunicationId());
                communicationShortResponse.setDate(latePaymentFineCommunication.getCreateDate().toLocalDate());
                communicationShortResponseList.add(communicationShortResponse);

                List<EmailCommunicationAttachment> allActiveAttachmentsByEmailCommunicationId = emailCommunicationAttachmentRepository
                        .findAllActiveAttachmentsByEmailCommunicationId(latePaymentFineCommunication.getEmailCommunicationId());
                for (EmailCommunicationAttachment attachment : allActiveAttachmentsByEmailCommunicationId) {
                    fileResponse.add(new TemplateFileResponse(attachment.getId(), attachment.getName()));
                }
            }
            latePaymentFineResponse.setFileResponse(fileResponse);
            latePaymentFineResponse.setCommunicationShortResponse(communicationShortResponseList);
        }
    }

    private void setParentLiabilityShortResponse(Long liabilityId, LatePaymentFineResponse latePaymentFineResponse) {
        if (liabilityId != null) {
            Optional<CustomerLiability> liability = customerLiabilityRepository.findById(liabilityId);
            liability.ifPresent(customerLiability -> latePaymentFineResponse.setParentLiabilityShortResponse(
                    new ShortResponse(liabilityId, customerLiability.getLiabilityNumber())
            ));
        }
    }

    private void setReschedulingShortResponse(Long reschedulingId, LatePaymentFineResponse latePaymentFineResponse) {
        if (reschedulingId != null) {
            Optional<Rescheduling> rescheduling = reschedulingRepository.findById(reschedulingId);
            rescheduling.ifPresent(resch -> latePaymentFineResponse.setReschedulingShortResponse(
                    new ShortResponse(reschedulingId, resch.getReschedulingNumber())
            ));
        }
    }

    public List<TaskShortResponse> getTasks(Long latePaymentFineId) {
        return taskService.getTasksByLatePaymentFineId(latePaymentFineId);
    }

}
