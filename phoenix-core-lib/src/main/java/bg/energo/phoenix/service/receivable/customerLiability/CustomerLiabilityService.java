package bg.energo.phoenix.service.receivable.customerLiability;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.InvoiceCompensation;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPostRequest;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.receivable.AutomaticOffsettingService;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityListColumns;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityListingRequest;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityRequest;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilitySearchFields;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityListingResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingInstallment;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingLiabilityResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableMapperService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER_LIABILITY;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLiabilityService {
    private static final String CUSTOMER_LIABILITY_PREFIX = "Liability-";
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final InterestRateRepository interestRateRepository;
    private final CustomerRepository customerRepository;
    private final BankRepository bankRepository;
    private final PermissionService permissionService;
    private final CustomerLiabilityMapperService customerLiabilityMapperService;
    private final InvoiceRepository invoiceRepository;
    private final ActionRepository actionRepository;
    private final DepositRepository depositRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final AutomaticOffsettingService automaticOffsettingService;
    private final CustomerReceivableService customerReceivableService;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final CustomerReceivableMapperService customerReceivableMapperService;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final LiabilityDirectOffsettingService liabilityDirectOffsettingService;

    @PersistenceContext
    private EntityManager em;

    /**
     * Creates a new customer liability record in the system.
     *
     * @param request     The request object containing the details of the customer liability to be created.
     * @param permissions The set of permissions associated with the current user.
     * @return The ID of the newly created customer liability.
     */
    @Transactional
    public Long create(CustomerLiabilityRequest request, Set<String> permissions) {
        log.info("Creating customer liability with request: {}", request);
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = customerLiabilityMapperService.mapParametersForCreate(request);
        validateCustomerLiability(request, errorMessages);
        if (request.getOccurrenceDate()
                .isAfter(request.getDueDate())) {
            errorMessages.add("Occurrence date should be before due date!;");
        }
        checkAndSetAdditionalParameters(request, customerLiability, errorMessages, permissions, false);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);

        executeAutomaticOffsetting(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Executes automatic offsetting for the specified customer liability.
     * <p>
     * This method calls the `offsetOfLiabilityAndReceivable` method of the `AutomaticOffsettingService` to perform automatic offsetting
     * for the given customer liability. The logged-in user ID is used as the offsetting user ID, or "system.admin" if the logged-in
     * user ID is null.
     *
     * @param customerLiability The ID of the customer liability to execute automatic offsetting for.
     */
    private void executeAutomaticOffsetting(CustomerLiability customerLiability) {
        em.detach(customerLiability);

        automaticOffsettingService.offsetOfLiabilityAndReceivable(
                null,
                customerLiability.getId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        );
    }

    /**
     * Creates a customer liability record based on the provided invoice.
     * <p>
     * This method checks the invoice status and document type to ensure they are valid for liability creation.
     * It then generates any additional liabilities based on the VAT base, and creates the main liability if the calculated amount is positive.
     * If the calculated amount is negative, it creates a receivable instead of a liability.
     *
     * @param invoiceId The ID of the invoice to create the liability for.
     * @return The ID of the newly created main liability, or null if no liability was created.
     * @throws DomainEntityNotFoundException     if the invoice is not found.
     * @throws IllegalArgumentsProvidedException if the invoice status or document type is not valid for liability creation.
     */
    @Transactional
    public Long createLiabilityFromInvoice(Long invoiceId, LiabilityOrReceivableCreationSource source) {
        log.debug("Creating customer liability from source: {}", source);

        log.info("Creating customer liability via invoice with id: {}", invoiceId);
        List<String> errorMessages = new ArrayList<>();
        Invoice invoice = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found by ID %s;".formatted(invoiceId)));
        if (!invoice.getInvoiceStatus()
                .equals(InvoiceStatus.REAL) &&
                !invoice.getInvoiceDocumentType()
                        .equals(InvoiceDocumentType.DEBIT_NOTE)) {
            log.error("To generate liability, invoice must be in status [REAL] and must not be a [DEBIT_NOTE]");
            throw new IllegalArgumentsProvidedException(
                    "To generate liability, invoice must be in status [REAL] and must not be a [DEBIT_NOTE], provided invoice: number [%s], status [%s], document type [%s]".formatted(
                            invoice.getInvoiceNumber(),
                            invoice.getInvoiceStatus(),
                            invoice.getInvoiceDocumentType()
                    ));
        }

        log.debug("Generating vat base liabilities for invoice with number [{}]", invoice.getInvoiceNumber());
        List<CustomerLiability> vatBaseLiabilities = customerLiabilityMapperService
                .generateAdditionalLiabilitiesForVatBase(
                        invoice
                );

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        log.debug("Creating main liability for invoice with number [{}]", invoice.getInvoiceNumber());
        CustomerLiability customerLiability = createLiability(invoice, invoice.getTotalAmountIncludingVat(), source, false);

        if (CollectionUtils.isNotEmpty(vatBaseLiabilities)) {
            BigDecimal remainingLiabilityAmount = customerLiability.getInitialAmount();
            for (CustomerLiability vatBaseLiability : vatBaseLiabilities) {
                log.debug("Creating receivable for customer by vat base");
                CustomerReceivable customerReceivable = customerReceivableService
                        .createReceivableDependingOnCustomerVatBase(invoice, vatBaseLiability.getInitialAmount());

                vatBaseLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + vatBaseLiability.getId());

                customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);

                BigDecimal differenceBetweenRemainingAmountAndReceivable = remainingLiabilityAmount.subtract(customerReceivable.getCurrentAmount());

                switch (differenceBetweenRemainingAmountAndReceivable.signum()) {
                    case 1 -> {
                        liabilityDirectOffsettingService.directOffsetting(
                                DirectOffsettingSourceType.RECEIVABLE,
                                customerReceivable.getId(),
                                customerLiability.getId(),
                                ObjectUtils.defaultIfNull(permissionService.getLoggedInUserId(), "system.admin"),
                                ObjectUtils.defaultIfNull(permissionService.getLoggedInUserId(), "system.admin"),
                                OperationContext.DLO.name(),
                                customerReceivable.getCurrentAmount(),
                                customerReceivable.getCurrencyId()
                        );

                        remainingLiabilityAmount = remainingLiabilityAmount.subtract(customerReceivable.getCurrentAmount());
                    }
                    case -1 -> {
                        liabilityDirectOffsettingService.directOffsetting(
                                DirectOffsettingSourceType.RECEIVABLE,
                                customerReceivable.getId(),
                                customerLiability.getId(),
                                ObjectUtils.defaultIfNull(permissionService.getLoggedInUserId(), "system.admin"),
                                ObjectUtils.defaultIfNull(permissionService.getLoggedInUserId(), "system.admin"),
                                OperationContext.DLO.name(),
                                null,
                                null
                        );

                        remainingLiabilityAmount = BigDecimal.ZERO;
                    }
                }
            }
        }

        executeAutomaticOffsetting(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Creates a customer liability for a given invoice, with the total amount including VAT.
     *
     * @param invoice The invoice to create the liability for.
     * @return The created customer liability.
     */
    @Transactional
    public CustomerLiability createLiabilityForReversal(Invoice invoice) {

        return createLiability(invoice, invoice.getTotalAmountIncludingVat(), null, true);
    }

    /**
     * Creates a receivable for an invoice based on the VAT base amount.
     * <p>
     * This method generates additional receivables for the VAT base amount of an invoice, and then creates a main receivable
     * for the remaining amount. It handles cases where the calculated receivable amount is negative (in which case a liability
     * is created instead), zero, or positive.
     *
     * @param invoice The invoice to create the receivable for.
     */
    @Transactional
    public void createReceivableForReversal(Invoice invoice) {
        List<CustomerReceivable> receivablesForVatBase = customerReceivableMapperService.generateAdditionalReceivablesForVatBase(
                invoice);
        BigDecimal vatBaseLiabilityAmountSummary = EPBDecimalUtils.calculateSummary(receivablesForVatBase.stream()
                .map(CustomerReceivable::getInitialAmount)
                .toList());

        BigDecimal receivableCalculatedAmount = invoice.getTotalAmountIncludingVat()
                .subtract(vatBaseLiabilityAmountSummary);
        if (receivableCalculatedAmount.signum() == -1) {
            receivablesForVatBase.forEach(customerReceivableRepository::saveAndFlush);
            receivablesForVatBase.forEach(liability -> liability.setReceivableNumber(CustomerReceivableService.RECEIVABLE_PREFIX + liability.getId()));
            receivablesForVatBase.forEach(customerReceivableService::executeAutomaticOffsetting);

            // no liability creation needed, need to create receivable
            createLiability(invoice, receivableCalculatedAmount.negate(), null, true);
        } else if (receivableCalculatedAmount.signum() == 0) {
            receivablesForVatBase.forEach(customerReceivableRepository::saveAndFlush);
            receivablesForVatBase.forEach(liability -> liability.setReceivableNumber(CustomerReceivableService.RECEIVABLE_PREFIX + liability.getId()));
            receivablesForVatBase.forEach(customerReceivableService::executeAutomaticOffsetting);
        } else {
            log.debug("Generating main Receivable for invoice");
            customerReceivableService.createReceivableDependingOnCustomerVatBase(invoice, receivableCalculatedAmount);
            log.debug("Main Receivable was generated for invoice");

            receivablesForVatBase.forEach(customerReceivableRepository::saveAndFlush);
            receivablesForVatBase.forEach(liability -> liability.setReceivableNumber(CustomerReceivableService.RECEIVABLE_PREFIX + liability.getId()));
            receivablesForVatBase.forEach(customerReceivableService::executeAutomaticOffsetting);

        }

    }

    /**
     * Creates a customer liability based on an invoice and a calculated liability amount.
     *
     * @param invoice                           The invoice to create the liability for.
     * @param customerLiabilityCalculatedAmount The calculated amount for the customer liability.
     * @param source
     * @return The created customer liability.
     */
    public CustomerLiability createLiability(
            Invoice invoice,
            BigDecimal customerLiabilityCalculatedAmount,
            LiabilityOrReceivableCreationSource source,
            boolean withAutomaticOffsetting
    ) {
        List<String> errorMessages = new ArrayList<>();
        CustomerLiability mainLiability = customerLiabilityMapperService.mapLiabilityFromInvoice(
                invoice,
                customerLiabilityCalculatedAmount,
                errorMessages
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        mainLiability = customerLiabilityRepository.saveAndFlush(mainLiability);
        mainLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + mainLiability.getId());
        mainLiability.setOccurrenceDate(invoice.getInvoiceDate());
        if (invoice.getNoInterestOnOverdueDebts() != null && invoice.getNoInterestOnOverdueDebts()) {
            mainLiability.setApplicableInterestRateId(null);
        }
        if (source != null) {
            mainLiability.setIncomeAccountNumber(getIncomeAccountNumberBasedOnSource(invoice, source));
        }

        if (LiabilityOrReceivableCreationSource.INVOICE_CANCELLATION.equals(source)) {
            mainLiability.setOccurrenceDate(invoice.getInvoiceDate());
            mainLiability.setDueDate(LocalDate.now());
        }
        mainLiability = customerLiabilityRepository.saveAndFlush(mainLiability);

        if (withAutomaticOffsetting) {
            executeAutomaticOffsetting(mainLiability);
        }

        return mainLiability;
    }

    private String getIncomeAccountNumberBasedOnSource(Invoice invoice, LiabilityOrReceivableCreationSource source) {
        return switch (invoice.getInvoiceDocumentType()) {
            case PROFORMA_INVOICE ->
                    incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_PRO_FORMA_INVOICE.name());
            case CREDIT_NOTE -> {
                String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
                if (source.equals(LiabilityOrReceivableCreationSource.BILLING_RUN) && incomeAccountNumber == null) {
                    throw new ClientException(
                            "Unable to find default liability account number for invoice with number [%s];".formatted(invoice.getInvoiceNumber()),
                            ErrorCode.DOMAIN_ENTITY_NOT_FOUND
                    );
                }
                yield incomeAccountNumber;
            }
            default ->
                    incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
        };
    }

    /**
     * Creates a customer liability based on an action.
     *
     * @param actionId the ID of the action to create the liability from
     * @return the ID of the created customer liability
     * @throws DomainEntityNotFoundException if the action with the given ID is not found
     */
    @Transactional
    public Long createLiabilityFromAction(Long actionId) {
        log.info("Creating customer liability via action with id: {}", actionId);
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found by ID %s;".formatted(
                        actionId)));
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = customerLiabilityMapperService.mapLiabilityFromAction(action, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
        executeAutomaticOffsetting(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Creates a customer liability based on a late payment fine.
     *
     * @param latePaymentFineId the ID of the late payment fine to create the liability from
     * @return the ID of the created customer liability
     * @throws DomainEntityNotFoundException if the late payment fine with the given ID is not found
     */
    @Transactional
    public Long createLiabilityFromLatePaymentFine(Long latePaymentFineId) {
        log.info("Creating customer liability via late payment fine with id: {}", latePaymentFineId);
        LatePaymentFine latePaymentFine = latePaymentFineRepository.findById(latePaymentFineId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Late Payment Fine not found by ID %s;".formatted(
                                latePaymentFineId)));
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = customerLiabilityMapperService.mapLiabilityFromLatePaymentFine(
                latePaymentFine,
                errorMessages
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);

        executeAutomaticOffsetting(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Creates a customer liability based on a late payment fine for an online payment.
     *
     * @param latePaymentFineId the ID of the late payment fine to create the liability from
     * @return the ID of the created customer liability
     * @throws DomainEntityNotFoundException if the late payment fine with the given ID is not found
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createLiabilityFromLatePaymentFineOnlinePayment(Long latePaymentFineId) {
        log.info("Online payment Creating customer liability via late payment fine with id: {}", latePaymentFineId);
        LatePaymentFine latePaymentFine = latePaymentFineRepository.findById(latePaymentFineId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Late Payment Fine not found by ID %s;".formatted(
                                latePaymentFineId)));
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = customerLiabilityMapperService.mapLiabilityFromLatePaymentFine(
                latePaymentFine,
                errorMessages
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiability.setSystemUserId("system.admin");
        customerLiability.setOccurrenceDate(latePaymentFine.getLogicalDate());
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Creates a customer liability based on a deposit.
     *
     * @param depositId     the ID of the deposit to create the liability from
     * @param initialAmount the initial amount to use for the created liability
     * @param errorMessages a list to store any error messages that occur during the operation
     * @return the ID of the created customer liability
     * @throws DomainEntityNotFoundException if the deposit with the given ID is not found
     */
    @Transactional
    public Long createLiabilityFromDeposit(
            Long depositId,
            BigDecimal initialAmount,
            List<String> errorMessages,
            Boolean isDepositWithdrawal,
            Long mloId
    ) {
        log.info("Creating customer liability via deposit with id: {}", depositId);
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Deposit not found by ID %s;".formatted(
                        depositId)));

        CustomerLiability customerLiability = customerLiabilityMapperService.mapLiabilityFromDeposit(
                deposit,
                isDepositWithdrawal,
                initialAmount,
                mloId
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
        executeAutomaticOffsetting(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Creates a customer liability based on a rescheduling installment.
     *
     * @param reschedulingId the ID of the rescheduling to create the liability from
     * @param installment    the rescheduling installment to create the liability from
     * @param currencyId     the ID of the currency to use for the created liability
     * @param customerId     the ID of the customer to create the liability for
     */
    @Transactional
    public Long createLiabilityFromRescheduling(
            Long reschedulingId,
            ReschedulingInstallment installment,
            Long currencyId,
            Long customerId
    ) {
        log.info("Creating customer liability via rescheduling with id: {}", reschedulingId);
        List<String> errorMessages = new ArrayList<>();
        Rescheduling rescheduling = reschedulingRepository.findById(reschedulingId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Rescheduling not found by ID %s;".formatted(reschedulingId)));

        CustomerDetails customerDetails = customerDetailsRepository.findFirstByCustomerId(
                        customerId,
                        Sort.by(Sort.Direction.DESC, "versionId")
                )
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "customerDetails-Customer details not found!;"));

        CustomerLiability customerLiability = customerLiabilityMapperService.mapLiabilityFromReschedulingInstallment(
                installment,
                currencyId,
                customerId,
                reschedulingId,
                rescheduling.getReschedulingNumber(),
                rescheduling.getInterestRateIdForInstallments(),
                customerDetails.getDirectDebit(),
                errorMessages
        );

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);

        executeAutomaticOffsetting(customerLiability);
        return customerLiability.getId();
    }

    /**
     * Updates an existing customer liability.
     *
     * @param id          the ID of the customer liability to update
     * @param request     the request containing the updated customer liability details
     * @param permissions the set of permissions the user has
     * @return the ID of the updated customer liability
     * @throws DomainEntityNotFoundException if the customer liability with the given ID is not found
     * @throws ClientException               if the customer liability was created automatically or the update request is invalid
     */
    @Transactional
    public Long update(Long id, CustomerLiabilityRequest request, Set<String> permissions) {
        log.info("Updating customer liability with id: {}", id);
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = customerLiabilityRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer liability not found by ID %s;".formatted(id)));

        AccountingPeriods accountingPeriods = accountingPeriodsRepository.findById(customerLiability.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period not found with given id: %s".formatted(
                                customerLiability.getAccountPeriodId())));

        if (request.getOccurrenceDate() != null && !request.getOccurrenceDate()
                .equals(customerLiability.getOccurrenceDate()) && customerLiabilityRepository.hasParticipatedInOffsetting(id)) {
            throw new OperationNotAllowedException("Can't change occurrence date , liability has participated in offsetting!");
        }

        List<CustomerOffsettingResponse> customerLiabilityOffsettingResponseList = customerLiabilityMapperService.getCustomerLiabilityOffsettingResponseList(id);
        boolean isAutomaticCreation = customerLiability.getCreationType().equals(CreationType.AUTOMATIC);
        boolean isAfterOffsetting = customerLiability.getFullOffsetDate() != null ||
                !customerLiabilityOffsettingResponseList.isEmpty() ||
                accountingPeriods.getStatus().equals(AccountingPeriodStatus.CLOSED);

        if (isAfterOffsetting || isAutomaticCreation) {
            if (accountingPeriods.getStatus()
                    .equals(AccountingPeriodStatus.OPEN)) {
                if (request.getApplicableInterestRateId() != null) {
                    interestRateRepository.findByIdAndStatusIn(
                                    request.getApplicableInterestRateId(),
                                    List.of(InterestRateStatus.ACTIVE)
                            )
                            .orElseThrow(() -> new DomainEntityNotFoundException(
                                    "interest rate with given id: %s not found".formatted(request.getApplicableInterestRateId())));
                }

                customerLiability.setApplicableInterestRateId(request.getApplicableInterestRateId());
                customerLiability.setApplicableInterestRateDateFrom(request.getInterestDateFrom());
                customerLiability.setApplicableInterestRateDateTo(request.getInterestDateTo());
                validateInterestDates(request, errorMessages);
            }
            if (request.getOccurrenceDate() != null && !request.getOccurrenceDate()
                    .equals(customerLiability.getOccurrenceDate()) && request.getOccurrenceDate()
                    .isAfter(customerLiability.getDueDate())) {
                errorMessages.add("Occurrence date should be before due date!;");
            }

            checkUpdateRequestWhenFieldsAreRestricted(
                    request,
                    accountingPeriods.getStatus(),
                    errorMessages,
                    isAutomaticCreation && !isAfterOffsetting
            );
            customerLiabilityMapperService.checkAndSetBlockedFields(request, customerLiability, errorMessages, permissions);
        } else {
            Set<ConstraintViolation<CustomerLiabilityRequest>> validationErrors = validator.validate(
                    request,
                    CustomerLiabilityPostRequest.class
            );

            for (ConstraintViolation<CustomerLiabilityRequest> violation : validationErrors) {
                String message = violation.getMessage();
                if (!message.isEmpty()) {
                    errorMessages.add(message);
                }
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            if (request.getOccurrenceDate() != null && !request.getOccurrenceDate()
                    .equals(customerLiability.getOccurrenceDate()) && request.getOccurrenceDate()
                    .isAfter(request.getDueDate())) {
                errorMessages.add("Occurrence date should be before due date!;");
            }
            customerLiabilityMapperService.mapParametersForUpdate(customerLiability, request);
            validateCustomerLiability(request, errorMessages);
            checkAndSetAdditionalParameters(request, customerLiability, errorMessages, permissions, true);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        Optional.ofNullable(request.getOccurrenceDate())
                .ifPresent(customerLiability::setOccurrenceDate);
        customerLiabilityRepository.save(customerLiability);
        return customerLiability.getId();
    }

    /**
     * Retrieves a customer liability by its ID.
     *
     * @param id the ID of the customer liability to retrieve
     * @return the customer liability response
     * @throws DomainEntityNotFoundException if the customer liability is not found
     * @throws ClientException               if the user does not have the necessary permissions to view the customer liability
     */
    @Transactional(readOnly = true)
    public CustomerLiabilityResponse view(Long id) {
        log.info("Previewing Customer Liability with id: %s".formatted(id));

        CustomerLiability customerLiability = customerLiabilityRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Customer Liability with id: %s;".formatted(id)));

        if (customerLiability.getStatus().equals(EntityStatus.DELETED)) {
            if (!hasDeletedPermission()) {
                throw new ClientException("You don't have View deleted Customer Liability Permission;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!hasViewPermission()) {
                throw new ClientException("You don't have View Customer Liability Permission;", ErrorCode.ACCESS_DENIED);
            }
        }

        return customerLiabilityMapperService.mapToCustomerLiabilityResponse(customerLiability);
    }

    /**
     * Lists customer liabilities based on the provided request parameters.
     *
     * @param request the request parameters for filtering and sorting the customer liabilities
     * @return a page of customer liability listing responses
     */
    public Page<CustomerLiabilityListingResponse> list(CustomerLiabilityListingRequest request) {
        List<EntityStatus> statuses = new ArrayList<>();

        if (hasViewPermission() && hasDeletedPermission()) {
            statuses = Arrays.asList(EntityStatus.values());
        } else if (hasDeletedPermission()) {
            statuses = List.of(EntityStatus.DELETED);
        } else if (hasViewPermission()) {
            statuses = List.of(EntityStatus.ACTIVE);
        }

        List<Long> currencyIds = request.getCurrencyIds();
        if (currencyIds != null && currencyIds.equals(Collections.singletonList(0L))) {
            return Page.empty();
        }

        Page<CustomerLiabilityListingMiddleResponse> middleResponses = customerLiabilityRepository.filter(
                getSearchByEnum(request.getSearchFields()),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getDueDateFrom(),
                request.getDueDateTo(),
                request.getInitialAmountFrom(),
                request.getInitialAmountTo(),
                request.getCurrentAmountFrom(),
                request.getCurrentAmountTo(),
                request.getBlockedForPayments(),
                request.getBlockedForReminderLetters(),
                request.getBlockedForInterestCalculation(),
                request.getBlockedForLiabilityOffsetting(),
                request.getBlockedForSupplyTermination(),
                ListUtils.emptyIfNull(currencyIds),
                request.getBillingGroup(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses),
                request.getOccurrenceDateFrom(),
                request.getOccurrenceDateTo(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns()))
                        )
                )
        );

        return mapCustomerLiabilityListingPage(middleResponses);
    }

    /**
     * Deletes a customer liability with the given ID.
     *
     * @param id the ID of the customer liability to delete
     * @return the ID of the deleted customer liability
     * @throws DomainEntityNotFoundException if the customer liability with the given ID is not found
     * @throws ClientException               if the customer liability has a closed accounting period or was created automatically
     */
    public Long delete(Long id) {
        log.info("Deleting Customer Liability with id: %s".formatted(id));

        CustomerLiability customerLiability = customerLiabilityRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find Customer Liability with id: %s;".formatted(id))
                );

        AccountingPeriods accountingPeriods = accountingPeriodsRepository
                .findById(customerLiability.getAccountPeriodId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Accounting period not found with given id: %s".formatted(customerLiability.getAccountPeriodId()))
                );

        if (accountingPeriods.getStatus().equals(AccountingPeriodStatus.CLOSED)) {
            throw new ClientException(
                    "It is not possible to delete Customer Liability which has closed accounting period.",
                    ErrorCode.OPERATION_NOT_ALLOWED
            );
        }

        if (customerLiability.getCreationType().equals(CreationType.AUTOMATIC)) {
            throw new ClientException(
                    "It is not possible to delete automatically created Customer Liability.",
                    ErrorCode.OPERATION_NOT_ALLOWED
            );
        }

        customerLiability.setStatus(EntityStatus.DELETED);
        customerLiabilityRepository.save(customerLiability);

        return customerLiability.getId();
    }

    /**
     * Retrieves a list of rescheduling liabilities for the given customer and liability IDs.
     *
     * @param customerId  the ID of the customer
     * @param liabilityId the ID of the liability
     * @return a list of {@link ReschedulingLiabilityResponse} objects representing the rescheduling liabilities
     */
    public List<ReschedulingLiabilityResponse> getReschedulingLiabilitiesByCustomerId(Long customerId, Long liabilityId) {
        return customerLiabilityRepository.getReschedulingLiabilitiesByCustomerId(customerId, liabilityId)
                .stream()
                .map(ReschedulingLiabilityResponse::new)
                .toList();
    }

    public List<ReschedulingLiabilityResponse> getReschedulingLiabilitiesByLiabilityIds(List<Long> liabilityIds) {
        return customerLiabilityRepository.getReschedulingLiabilitiesByLiabilityIds(liabilityIds)
                .stream()
                .map(ReschedulingLiabilityResponse::new)
                .toList();
    }

    public boolean existsReschedulingLiabilitiesByCustomerIdAndLiabilityIds(Long customerId, List<Long> liabilityIds) {
        return customerLiabilityRepository.existsReschedulingLiabilityByCustomerIdAndLiabilityId(customerId, liabilityIds);
    }

    /**
     * Maps a page of {@link CustomerLiabilityListingMiddleResponse} objects to a page of {@link CustomerLiabilityListingResponse} objects.
     *
     * @param middlePage the page of middle response objects to map
     * @return a new page of customer liability listing response objects
     */
    private Page<CustomerLiabilityListingResponse> mapCustomerLiabilityListingPage(Page<CustomerLiabilityListingMiddleResponse> middlePage) {
        List<CustomerLiabilityListingResponse> responseList = middlePage.getContent()
                .stream()
                .map(customerLiabilityMapperService::mapCustomerLiabilityListing)
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, middlePage.getPageable(), middlePage.getTotalElements());
    }

    /**
     * Retrieves the search field value from the provided {@link CustomerLiabilitySearchFields} enum.
     * If the search fields are null, it returns the value of the {@link CustomerLiabilitySearchFields#ALL} enum.
     *
     * @param searchFields the {@link CustomerLiabilitySearchFields} enum to retrieve the value from
     * @return the value of the provided search fields, or the value of {@link CustomerLiabilitySearchFields#ALL} if the search fields are null
     */
    private String getSearchByEnum(CustomerLiabilitySearchFields searchFields) {
        return searchFields != null ? searchFields.getValue() : CustomerLiabilitySearchFields.ALL.getValue();
    }

    /**
     * Retrieves the sort field value from the provided {@link CustomerLiabilityListColumns} enum.
     * If the sort column is null, it returns the value of the {@link CustomerLiabilityListColumns#ID} enum.
     *
     * @param sortByColumn the {@link CustomerLiabilityListColumns} enum to retrieve the value from
     * @return the value of the provided sort column, or the value of {@link CustomerLiabilityListColumns#ID} if the sort column is null
     */
    private String getSorByEnum(CustomerLiabilityListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : CustomerLiabilityListColumns.ID.getValue();
    }

    /**
     * Validates the customer liability request by checking the following:
     * - The accounting period specified in the request exists and is open.
     * - The accounting period start date is not in the future.
     * - The applicable interest rate specified in the request exists and is active.
     * - The customer specified in the request exists and is active.
     * - The bank specified in the request exists and is active.
     *
     * @param request       the customer liability request to validate
     * @param errorMessages a list to store any error messages encountered during validation
     */
    private void validateCustomerLiability(CustomerLiabilityRequest request, List<String> errorMessages) {
        Optional<AccountingPeriods> accountingPeriodsOptional = accountingPeriodsRepository.findByIdAndStatus(
                request.getAccountingPeriodId(),
                AccountingPeriodStatus.OPEN
        );
        if (accountingPeriodsOptional.isEmpty()) {
            errorMessages.add("accountingPeriodId-[accountingPeriodId] accounting period must exist and be open;");
        } else if (accountingPeriodsOptional.get()
                .getStartDate()
                .isAfter(LocalDateTime.now())) {
            errorMessages.add("accountingPeriodId-[accountingPeriodId] accounting period can't be in the future;");
        }

        if (request.getApplicableInterestRateId() != null && !interestRateRepository.existsByIdAndStatusIn(
                request.getApplicableInterestRateId(),
                List.of(InterestRateStatus.ACTIVE)
        )) {
            errorMessages.add("applicableInterestRateId-[applicableInterestRateId] interest rate not found;");
        }

        if (!customerRepository.existsByIdAndStatusIn(request.getCustomerId(), List.of(CustomerStatus.ACTIVE))) {
            errorMessages.add("customerId-[customerId] customer not found;");
        }

        if (request.getBankId() != null && !bankRepository.existsByIdAndStatusIn(
                request.getBankId(),
                List.of(NomenclatureItemStatus.ACTIVE)
        )) {
            errorMessages.add("bankId-[bankId] bank  not found;");
        }

    }

    /**
     * Checks and sets additional parameters for a customer liability request, including:
     * - Checks the currency and sets the initial and current amounts
     * - Checks and sets parameters related to blocking for payment, reminder letters, late payment calculation, liabilities offsetting, and supply termination
     * - Checks and sets the billing group and alternative invoice recipient customer
     *
     * @param request           the customer liability request
     * @param customerLiability the customer liability entity
     * @param errorMessages     a list to store any error messages encountered during validation
     * @param permissions       a set of permissions to check against
     */
    private void checkAndSetAdditionalParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, boolean update
    ) {
        customerLiabilityMapperService.checkCurrencyAndSetAmounts(
                request.getCurrencyId(),
                request.getInitialAmount(),
                request.getInitialAmount(),
                customerLiability,
                errorMessages
        );

        if (!update || customerLiabilityMapperService.hasBlockedForPaymentChanged(request, customerLiability)) {
            customerLiabilityMapperService.checkAndSetBlockedForPaymentParameters(
                    request,
                    customerLiability,
                    errorMessages,
                    permissions,
                    update ? PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT : PermissionEnum.CUSTOMER_LIABILITY_BLOCKED_FOR_PAYMENT
            );
        }

        if (!update || customerLiabilityMapperService.hasBlockedForReminderLettersChanged(request, customerLiability)) {
            customerLiabilityMapperService.checkAndSetBlockedForReminderLettersParameters(
                    request,
                    customerLiability,
                    errorMessages,
                    permissions,
                    update ? PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS : PermissionEnum.CUSTOMER_LIABILITY_BLOCKED_FOR_REMINDER_LETTERS
            );
        }

        if (!update || customerLiabilityMapperService.hasBlockedForCalculationOfLatePaymentChanged(request, customerLiability)) {
            customerLiabilityMapperService.checkAndSetBlockedForCalculationOfLatePaymentParameters(
                    request,
                    customerLiability,
                    errorMessages,
                    permissions,
                    update ? PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS : PermissionEnum.CUSTOMER_LIABILITY_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS
            );
        }

        if (!update || customerLiabilityMapperService.hasBlockedForLiabilitiesOffsettingChanged(request, customerLiability)) {
            customerLiabilityMapperService.checkAndSetBlockedForLiabilitiesOffsettingParameters(
                    request,
                    customerLiability,
                    errorMessages,
                    permissions,
                    update ? PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING : PermissionEnum.CUSTOMER_LIABILITY_BLOCKED_FOR_LIABILITIES_OFFSETTING
            );
        }

        if (!update || customerLiabilityMapperService.hasBlockedForSupplyTerminationChanged(request, customerLiability)) {
            customerLiabilityMapperService.checkAndSetBlockedForSupplyTerminationParameters(
                    request,
                    customerLiability,
                    errorMessages,
                    permissions,
                    update ? PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION : PermissionEnum.CUSTOMER_LIABILITY_BLOCKED_FOR_SUPPLY_TERMINATION
            );
        }

        customerLiabilityMapperService.checkAndSetBillingGroup(request, customerLiability, errorMessages);
        customerLiabilityMapperService.checkAndSetAlternativeInvoiceRecipientCustomer(request, customerLiability, errorMessages);
        customerLiabilityMapperService.checkCurrencyAndSetAmountWithoutInterestInOtherCurrency(
                request.getCurrencyId(),
                request.getAmountWithoutInterest(),
                customerLiability,
                errorMessages
        );
    }

    /**
     * Checks if the current user has the necessary permissions to delete a customer liability.
     *
     * @return true if the user has the CUSTOMER_LIABILITY_VIEW_DELETE permission, false otherwise
     */
    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(
                CUSTOMER_LIABILITY,
                List.of(PermissionEnum.CUSTOMER_LIABILITY_VIEW_DELETE)
        );
    }

    /**
     * Checks if the current user has the necessary permissions to view a customer liability.
     *
     * @return true if the user has the CUSTOMER_LIABILITY_VIEW permission, false otherwise
     */
    private boolean hasViewPermission() {
        return permissionService.permissionContextContainsPermissions(
                CUSTOMER_LIABILITY,
                List.of(PermissionEnum.CUSTOMER_LIABILITY_VIEW)
        );
    }

    /**
     * Checks and validates the request parameters when the offsetting process has already finished,
     * the accounting period is closed, or for automatically created liabilities.
     *
     * @param request                the customer liability request
     * @param accountingPeriodStatus the status of the accounting period
     * @param errorMessages          a list to store any error messages encountered during validation
     * @param isAutomaticCreation    whether the liability was automatically created
     */
    private void checkUpdateRequestWhenFieldsAreRestricted(
            CustomerLiabilityRequest request,
            AccountingPeriodStatus accountingPeriodStatus,
            List<String> errorMessages,
            boolean isAutomaticCreation
    ) {
        String reason = isAutomaticCreation ?
                "liability was created automatically" :
                "liability was involved in offsetting process";

        if (request.getOccurrenceDate() != null) {
            errorMessages.add(
                    "occurrenceDate-[occurrenceDate] occurrence date should be disabled when " + reason + ";");
        }

        if (request.getAccountingPeriodId() != null) {
            errorMessages.add(
                    "accountingPeriodId-[accountingPeriodId] accounting period should be disabled when " + reason + ";");
        }

        if (request.getDueDate() != null) {
            errorMessages.add("dueDate-[dueDate] due date should be disabled when " + reason + ";");
        }

        if (request.getInitialAmount() != null) {
            errorMessages.add(
                    "initialAmount-[initialAmount] initial amount should be disabled when " + reason + ";");
        }

        if (request.getCurrencyId() != null) {
            errorMessages.add("currencyId-[currencyId] currency should be disabled when " + reason + ";");
        }

        if (request.getOutgoingDocumentFromExternalSystem() != null) {
            errorMessages.add(
                    "outgoingDocumentFromExternalSystem-[outgoingDocumentFromExternalSystem] outgoing document from external system should be disabled when " + reason + ";");
        }

        if (request.getBasisForIssuing() != null) {
            errorMessages.add(
                    "basisForIssuing-[basisForIssuing] basis for issuing should be disabled when " + reason + ";");
        }

        if (request.getNumberOfIncomeAccount() != null) {
            errorMessages.add(
                    "numberOfIncomeAccount-[numberOfIncomeAccount] number of income account should be disabled when " + reason + ";");
        }

        if (request.getCostCenterControllingOrder() != null) {
            errorMessages.add(
                    "costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order should be disabled when " + reason + ";");
        }

        if (request.getBankId() != null) {
            errorMessages.add("bankId-[bankId] bank should be disabled when " + reason + ";");
        }

        if (request.getBankAccount() != null) {
            errorMessages.add("bankAccount-[bankAccount] bank account should be disabled when " + reason + ";");
        }

        if (request.getAdditionalLiabilityInformation() != null) {
            errorMessages.add(
                    "additionalLiabilityInformation-[additionalLiabilityInformation] additional liability information should be disabled when " + reason + ";");
        }

        if (request.getBillingGroupId() != null) {
            errorMessages.add(
                    "billingGroupId-[billingGroupId] billing group should be disabled when " + reason + ";");
        }

        if (request.getAlternativeInvoiceRecipientCustomerId() != null) {
            errorMessages.add(
                    "alternativeInvoiceRecipientCustomerId-[alternativeInvoiceRecipientCustomerId] alternative invoice recipient customer should be disabled when " + reason + ";");
        }

        if (request.getCustomerId() != null) {
            errorMessages.add("customerId-[customerId] customer should be disabled when " + reason + ";");
        }

        if (request.getAmountWithoutInterest() != null) {
            errorMessages.add(
                    "amountWithoutInterest-[amountWithoutInterest] amount without interest should be disabled when " + reason + ";");
        }

        if (accountingPeriodStatus.equals(AccountingPeriodStatus.CLOSED)) {
            if (request.getApplicableInterestRateId() != null) {
                errorMessages.add(
                        "applicableInterestRateId-[applicableInterestRateId] applicable interest rate should be disabled when accounting period is closed;");
            }

            if (request.getInterestDateFrom() != null) {
                errorMessages.add(
                        "interestDateFrom-[interestDateFrom] interest rate date from should be disabled when accounting period is closed;");
            }

            if (request.getInterestDateTo() != null) {
                errorMessages.add(
                        "interestDateTo-[interestDateTo] interest rate date to should be disabled when accounting period is closed;");
            }
        }
    }

    /**
     * Validates the interest date range provided in the {@link CustomerLiabilityRequest}.
     * If the `interestDateFrom` is after the `interestDateTo`, an error message is added to the provided `errorMessages` list.
     *
     * @param request       the {@link CustomerLiabilityRequest} containing the interest date range to validate
     * @param errorMessages the list of error messages to add to if the interest date range is invalid
     */
    private void validateInterestDates(CustomerLiabilityRequest request, List<String> errorMessages) {
        LocalDate interestDateFrom = request.getInterestDateFrom();
        LocalDate interestDateTo = request.getInterestDateTo();
        if (interestDateFrom != null && interestDateTo != null && interestDateFrom.isAfter(interestDateTo)) {
            errorMessages.add("interestDateFrom must be less than or equal to interestDateTo;");
        }
    }

    /**
     * Checks if a liability has been generated for the specified action ID.
     *
     * @param id the ID of the action to check for a generated liability
     * @return true if a liability has been generated for the specified action ID, false otherwise
     */
    public boolean isLiabilityGeneratedForAction(Long id) {
        return customerLiabilityRepository.existsByActionIdAndStatus(id, EntityStatus.ACTIVE);
    }

    /**
     * Retrieves the liability associated with the specified action ID.
     *
     * @param id the ID of the action to retrieve the liability for
     * @return a {@link ShortResponse} containing the liability information
     */
    public ShortResponse getActionLiability(Long id) {
        return customerLiabilityRepository.getActionLiability(id);
    }

    /**
     * Creates a new {@link CustomerLiability} from the invoiced compensation data.
     *
     * <p>This method uses the provided {@link InvoiceCompensation} to map the relevant
     * information (such as compensation receipt ID, currency ID, invoice ID, and compensation
     * amount) into a new {@link CustomerLiability} entity. The created liability is then
     * saved to the database with a generated liability number.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the data used
     *                            to create the customer liability.
     * @return A {@link CustomerLiability} object that has been created and saved to the database.
     * @throws DomainEntityNotFoundException If any of the related entities (such as currency,
     *                                       invoice, or account) cannot be found during the mapping process.
     */
    @Transactional
    public CustomerLiability createLiabilityFromInvoicedCompensation(InvoiceCompensation invoiceCompensation) {
        CustomerLiability customerLiability = customerLiabilityMapperService.mapFromInvoiceCompensation(
                invoiceCompensation.getCompensationReceiptId(),
                invoiceCompensation.getCurrencyId(),
                invoiceCompensation.getInvoiceId(),
                invoiceCompensation.getCompAmount()
        );

        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
        return customerLiability;
    }

    /**
     * Creates a new {@link CustomerLiability} from the reverse compensation data.
     *
     * <p>This method uses the provided {@link InvoiceCompensation} to map the relevant
     * information (such as customer ID, currency ID, invoice ID, and compensation amount)
     * into a new {@link CustomerLiability} entity for reverse compensation scenarios.
     * The created liability is then saved to the database with a generated liability number.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the data used
     *                            to create the customer liability.
     * @return A {@link CustomerLiability} object that has been created and saved to the database.
     * @throws DomainEntityNotFoundException If any of the related entities (such as currency,
     *                                       invoice, or account) cannot be found during the mapping process.
     */
    @Transactional
    public CustomerLiability createLiabilityFromReverseCompensation(InvoiceCompensation invoiceCompensation) {
        CustomerLiability customerLiability = customerLiabilityMapperService.mapFromInvoiceCompensation(
                invoiceCompensation.getCustomerId(),
                invoiceCompensation.getCurrencyId(),
                invoiceCompensation.getInvoiceId(),
                invoiceCompensation.getCompAmount()
        );

        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
        return customerLiability;
    }
}
