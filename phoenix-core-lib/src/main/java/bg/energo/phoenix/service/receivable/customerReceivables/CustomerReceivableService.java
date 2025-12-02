package bg.energo.phoenix.service.receivable.customerReceivables;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.InvoiceCompensation;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivablePostGroup;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.entity.receivable.AutomaticOffsettingService;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByReceivable;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositPaymentDeadlineAfterWithdrawal;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.payment.Payment;
import bg.energo.phoenix.model.entity.receivable.payment.PaymentReceivableOffsetting;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.model.enums.receivable.customerReceivable.CustomerReceivableListColumns;
import bg.energo.phoenix.model.enums.receivable.customerReceivable.CustomerReceivableSearchBy;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.request.receivable.customerReceivable.CustomerReceivableListingRequest;
import bg.energo.phoenix.model.request.receivable.customerReceivable.CustomerReceivableRequest;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.customerReceivable.CustomerReceivableListingResponse;
import bg.energo.phoenix.model.response.receivable.customerReceivable.CustomerReceivableResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByReceivableRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositPaymentDeadlineAfterWithdrawalRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.receivable.ObjectOffsettingService;
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
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER_RECEIVABLE;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

/**
 * Creates a new customer receivable and performs automatic offsetting.
 *
 * @param request     the request object containing the details of the customer receivable to be created
 * @param permissions the set of permissions the user has for the operation
 * @return the ID of the newly created customer receivable
 * @throws ClientException if the user does not have the appropriate permission to create a customer receivable that is blocked for offsetting
 * @throws DomainEntityNotFoundException if the accounting period associated with the customer receivable is not found
 * @throws EPBChainedExceptionTriggerUtil.EPBChainedExceptionTriggerException if there are any validation errors
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerReceivableService {
    public static final String RECEIVABLE_PREFIX = "Receivable-";
    private final CustomerReceivableRepository customerReceivableRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final CurrencyRepository currencyRepository;
    private final BankRepository bankRepository;
    private final CustomerRepository customerRepository;
    private final ContractBillingGroupRepository billingGroupRepository;
    private final PermissionService permissionService;
    private final BlockingReasonRepository blockingReasonRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final InvoiceRepository invoiceRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final DepositRepository depositRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerReceivableMapperService customerReceivableMapperService;
    private final DepositPaymentDeadlineAfterWithdrawalRepository depositPaymentDeadlineAfterWithdrawalRepository;
    private final CustomerLiabilityPaidByReceivableRepository customerLiabilityPaidByReceivableRepository;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final AutomaticOffsettingService automaticOffsettingService;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ReceivableDirectOffsettingService receivableDirectOffsettingService;
    private final ObjectOffsettingService objectOffsettingService;

    @PersistenceContext
    private EntityManager em;

    /**
     * Creates a new customer receivable and performs automatic offsetting.
     *
     * @param request     the request object containing the details of the customer receivable to be created
     * @param permissions the set of permissions the user has for the operation
     * @return the ID of the newly created customer receivable
     * @throws ClientException                                                    if the user does not have the appropriate permission to create a customer receivable that is blocked for offsetting
     * @throws DomainEntityNotFoundException                                      if the accounting period associated with the customer receivable is not found
     * @throws EPBChainedExceptionTriggerUtil.EPBChainedExceptionTriggerException if there are any validation errors
     */
    @Transactional
    public Long create(CustomerReceivableRequest request, Set<String> permissions) {
        if (request.isBlockedForOffsetting()) {
            if (CollectionUtils.isEmpty(permissions)) {
                checkPermission(PermissionEnum.BLOCKED_FOR_LIABILITIES_OFFSETTING);
            } else {
                if (!permissions.contains(PermissionEnum.BLOCKED_FOR_LIABILITIES_OFFSETTING.getId())) {
                    throw new ClientException(
                            "You don't have appropriate permission: %s;".formatted(BLOCKED_FOR_LIABILITIES_OFFSETTING.name()),
                            ErrorCode.OPERATION_NOT_ALLOWED
                    );
                }
            }
        }
        List<String> errorMessages = new ArrayList<>();
        CustomerReceivable customerReceivable = new CustomerReceivable();
        BigDecimal scaledInitialAmount = EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount());
        request.setInitialAmount(scaledInitialAmount);
        validateCustomerReceivableCreateRequest(request, errorMessages, customerReceivable);
        setRestFields(request, customerReceivable);
        customerReceivable.setReceivableNumber("TEMP");
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setCreationType(CreationType.MANUAL);
        customerReceivable.setCreateDate(LocalDateTime.now());
        customerReceivable.setOccurrenceDate(request.getOccurrenceDate());
        customerReceivable.setDueDate(request.getDueDate());
        customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);

        executeAutomaticOffsetting(customerReceivable);

        return customerReceivable.getId();
    }

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
     * Updates an existing customer receivable.
     *
     * @param id          the ID of the customer receivable to update
     * @param request     the request object containing the updated details of the customer receivable
     * @param permissions the set of permissions the user has for the operation
     * @return the ID of the updated customer receivable
     * @throws DomainEntityNotFoundException                                      if the customer receivable or accounting period is not found
     * @throws ClientException                                                    if the user does not have the appropriate permission to update a customer receivable that is blocked for offsetting, or if the customer receivable is automatically created
     * @throws EPBChainedExceptionTriggerUtil.EPBChainedExceptionTriggerException if there are any validation errors
     */
    @Transactional
    public Long update(Long id, CustomerReceivableRequest request, Set<String> permissions) {
        CustomerReceivable customerReceivable = customerReceivableRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer receivable not found with id : " + id));
        if (CollectionUtils.isEmpty(permissions)) {
            if (request.isBlockedForOffsetting() && haveBlockedForOffsettingFieldsChanged(request, customerReceivable)) {
                checkPermission(PermissionEnum.BLOCKED_FOR_LIABILITIES_OFFSETTING);
            }
        } else {
            if (request.isBlockedForOffsetting() && haveBlockedForOffsettingFieldsChanged(
                    request,
                    customerReceivable
            ) && !permissions.contains(BLOCKED_FOR_LIABILITIES_OFFSETTING.getId())) {
                throw new ClientException(
                        "You don't have appropriate permission: %s;".formatted(BLOCKED_FOR_LIABILITIES_OFFSETTING.name()),
                        ErrorCode.OPERATION_NOT_ALLOWED
                );
            }
        }
        List<String> errorMessages = new ArrayList<>();
        AccountingPeriods accountingPeriods = accountingPeriodsRepository.findById(customerReceivable.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period with id " + customerReceivable.getAccountPeriodId() + " not found"));
        request.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        boolean isAutomaticCreation = !customerReceivable.getCreationType().equals(CreationType.MANUAL);
        boolean hasParticipatedInOffsetting =
                customerLiabilityPaidByReceivableRepository
                        .existsByCustomerReceivableIdAndStatus(customerReceivable.getId(), EntityStatus.ACTIVE) ||
                        !getOffsettingResponseList(id).isEmpty();
        boolean isClosedPeriod = accountingPeriods.getStatus().equals(AccountingPeriodStatus.CLOSED);
        if (hasParticipatedInOffsetting || isClosedPeriod || isAutomaticCreation) {
            validateForNulls(request, errorMessages);
            checkBlockedForOffsetting(request, errorMessages, customerReceivable);
        } else {
            Set<ConstraintViolation<CustomerReceivableRequest>> validationErrors = validator.validate(
                    request,
                    CustomerReceivablePostGroup.class
            );
            for (ConstraintViolation<CustomerReceivableRequest> violation : validationErrors) {
                errorMessages.add(violation.getMessage());
            }
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            validateCustomerReceivableCreateRequest(request, errorMessages, customerReceivable);
            setRestFields(request, customerReceivable);
        }

        if (request.getOccurrenceDate() != null && customerReceivable.getOccurrenceDate() != null && !request.getOccurrenceDate()
                .isEqual(
                        customerReceivable.getOccurrenceDate())) {
            List<PaymentReceivableOffsetting> paymentReceivableOffsetting = customerReceivableRepository.findPaymentReceivableOffsetting(
                    id);
            List<CustomerLiabilityPaidByReceivable> customerLiabilityPaidByReceivables = customerReceivableRepository.findLiabilityPaidByReceivable(
                    id);
            if (!CollectionUtils.isEmpty(paymentReceivableOffsetting) && !CollectionUtils.isEmpty(customerLiabilityPaidByReceivables)) {
                errorMessages.add("occurrenceDate-occurrenceDate can not be changed after offsetting");
            }
        }

        Optional.ofNullable(request.getOccurrenceDate())
                .ifPresent(customerReceivable::setOccurrenceDate);
        Optional.ofNullable(request.getDueDate())
                .ifPresent(customerReceivable::setDueDate);
        CustomerReceivable updatedCustomerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);
        executeAutomaticOffsetting(updatedCustomerReceivable);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return customerReceivable.getId();
    }

    /**
     * Deletes a customer receivable by setting its status to DELETED.
     *
     * @param id the ID of the customer receivable to delete
     * @throws DomainEntityNotFoundException if the customer receivable or accounting period is not found
     * @throws ClientException               if the customer receivable is automatically created or the accounting period is closed
     */
    @Transactional
    public void delete(Long id) {
        CustomerReceivable customerReceivable = customerReceivableRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer Receivable not found with id : " + id));
        if (customerReceivable.getCreationType()
                .equals(CreationType.AUTOMATIC)) {
            throw new ClientException(
                    "It's not possible to delete automatically created customer receivable",
                    ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
            );
        }
        AccountingPeriods accountingPeriod = accountingPeriodsRepository.findById(customerReceivable.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period with id " + customerReceivable.getAccountPeriodId() + " not found"));
        if (accountingPeriod.getStatus()
                .equals(AccountingPeriodStatus.CLOSED)) {
            throw new ClientException("Accounting period closed,receivable is not editable", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        customerReceivable.setStatus(EntityStatus.DELETED);
    }

    /**
     * Retrieves a paginated list of customer receivables based on the provided filtering criteria.
     *
     * @param request the request object containing the filtering criteria
     * @return a page of customer receivable listing responses
     */
    public Page<CustomerReceivableListingResponse> listing(CustomerReceivableListingRequest request) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (permissionCheck(CUSTOMER_RECEIVABLE_VIEW_DELETE)) {
            entityStatuses.add(EntityStatus.DELETED);
        }
        if (permissionCheck(CUSTOMER_RECEIVABLE_VIEW)) {
            entityStatuses.add(EntityStatus.ACTIVE);
        }
        if (request.getDueDateFrom() != null && request.getDueDateTo() != null && request.getDueDateFrom().isAfter(request.getDueDateTo())) {
            throw new ClientException("DueDateFrom must be before or equal to dueDateTo", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        if (request.getOccurrenceDateFrom() != null && request.getOccurrenceDateTo() != null && request.getOccurrenceDateFrom().isAfter(request.getOccurrenceDateTo())) {
            throw new ClientException(
                    "OccurrenceDateFrom must be before or equal to occurrenceDateTo",
                    ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED
            );
        }

        boolean sizeOneAndZero = request.getCurrencyIds() != null && request.getCurrencyIds()
                .size() == 1 && request.getCurrencyIds()
                .get(0)
                .equals(0L);

        return sizeOneAndZero ? Page.empty() : customerReceivableRepository
                .listing(
                        request.getInitialAmountFrom(),
                        request.getInitialAmountTo(),
                        request.getCurrentAmountFrom(),
                        request.getCurrentAmountTo(),
                        request.getBlockedForOffsetting(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getSearchByEnum(request.getCustomerReceivableSearchBy()),
                        ListUtils.emptyIfNull(request.getCurrencyIds()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(entityStatuses),
                        request.getBillingGroup(),
                        request.getOccurrenceDateFrom(),
                        request.getOccurrenceDateTo(),
                        request.getDueDateFrom(),
                        request.getDueDateTo(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSortByEnum(request.getSortBy()))
                                )
                        )
                )
                .map(CustomerReceivableListingResponse::from);
    }

    /**
     * Creates a customer receivable from an invoice.
     *
     * @param invoice the invoice to create the customer receivable from
     * @return the created customer receivable
     * @throws ClientException if the invoice type or status is invalid
     */
    @Transactional
    public CustomerReceivable createFromInvoice(Invoice invoice, LiabilityOrReceivableCreationSource source) {
        log.debug("Creating customer receivable from source: {}", source);

        if (!invoice.getInvoiceStatus()
                .equals(InvoiceStatus.REAL) || !invoice.getInvoiceDocumentType()
                .equals(InvoiceDocumentType.CREDIT_NOTE)) {
            throw new ClientException("Invalid invoice type or status!", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        CustomerReceivable customerReceivable = customerReceivableMapperService.mapReceivableFromInvoice(invoice, source);
        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
        executeAutomaticOffsetting(customerReceivable);

        return customerReceivable;
    }

    @Transactional
    public void createReceivableForInvoiceCancellation(Invoice invoice, BigDecimal amountToReceive) {
        CustomerReceivable customerReceivable = customerReceivableMapperService.mapReceivableFromInvoiceByVatBase(
                invoice,
                amountToReceive
        );
        customerReceivable.setOccurrenceDate(invoice.getInvoiceDate());
        customerReceivable.setDueDate(invoice.getPaymentDeadline());
        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_RECEIVABLES.name());
        if (incomeAccountNumber == null) {
            throw new DomainEntityNotFoundException("Default income account not found for receivables!;");
        }
        customerReceivable.setIncomeAccountNumber(incomeAccountNumber);
        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
        executeAutomaticOffsetting(customerReceivable);
    }

    /**
     * Creates a customer receivable from a deposit.
     *
     * @param deposit       the deposit to create the customer receivable from
     * @param errorMessages a list to store any error messages that occur during the creation process
     * @param initialAmount the initial amount to use for the customer receivable
     * @return the created customer receivable
     */
    @Transactional
    public CustomerReceivable createFromDeposit(Deposit deposit, List<String> errorMessages, BigDecimal initialAmount) {
        log.info("Creating customer receivable via deposit with id: {}", deposit.getId());

        CustomerReceivable customerReceivable = mapFromDeposit(deposit, initialAmount, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
        executeAutomaticOffsetting(customerReceivable);

        return customerReceivable;
    }

    /**
     * Creates a customer receivable record from a given payment and handles related processing.
     *
     * @param payment                             the payment object used to create the customer receivable
     * @param paymentCurrentAmountAfterOffsetting the current amount of the payment after offsetting
     * @return the ID of the newly created customer receivable record
     */
    @Transactional
    public Long createFromPayment(Payment payment, BigDecimal paymentCurrentAmountAfterOffsetting, boolean needAutomaticOffsetting) {
        log.info("Creating customer receivable from payment with id: {}", payment.getId());

        CustomerReceivable customerReceivable = mapFromPayment(payment, paymentCurrentAmountAfterOffsetting);
        customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);

        if (needAutomaticOffsetting) {
            executeAutomaticOffsetting(customerReceivable);
        }

        return customerReceivable.getId();
    }

    @Transactional
    public CustomerReceivable createFromLiabilityFromManualLiabilityOffsettingReversal(CustomerLiability liability) {
        log.info("Creating customer receivable via liability with id: {}", liability.getId());
        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Current month accounting period not found;"));

        CustomerReceivable customerReceivable = new CustomerReceivable();
        customerReceivable.setReceivableNumber("TEMPORARY_NUMBER");
        customerReceivable.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerReceivable.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(liability.getInitialAmount()));
        customerReceivable.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(liability.getInitialAmount()));
        customerReceivable.setCurrencyId(liability.getCurrencyId());
        if (liability.getCostCenterControllingOrder() != null) {
            customerReceivable.setCostCenterControllingOrder(liability.getCostCenterControllingOrder());
        }
        customerReceivable.setCustomerId(liability.getCustomerId());
        if (liability.getContractBillingGroupId() != null) {
            customerReceivable.setBillingGroupId(liability.getContractBillingGroupId());
        }
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        customerReceivable.setOccurrenceDate(LocalDate.now());

        String number = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_DEPOSIT.name());
        if (number == null) {
            throw new DomainEntityNotFoundException("Default income account number for deposit not found;");
        }

        customerReceivable.setIncomeAccountNumber(number);

        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());

        return customerReceivable;
    }

    private CustomerReceivable mapFromDeposit(Deposit deposit, BigDecimal initialAmount, List<String> errorMessages) {
        CustomerReceivable customerReceivable = new CustomerReceivable();
        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period not found for current month;"));

        DepositPaymentDeadlineAfterWithdrawal withdrawal = depositPaymentDeadlineAfterWithdrawalRepository
                .findByDepositIdAndStatusIn(deposit.getId(), List.of(EntityStatus.ACTIVE, EntityStatus.DELETED))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("PaymentDeadlineAfterWithdrawal not found with Deposit id: %s;".formatted(
                                deposit.getId())));


        customerReceivable.setReceivableNumber("TEMPORARY_NUMBER");
        customerReceivable.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerReceivable.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmount));
        customerReceivable.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmount));
        customerReceivable.setCurrencyId(deposit.getCurrencyId());

        Currency currency = currencyRepository.findCurrencyByIdAndStatuses(
                        deposit.getCurrencyId(),
                        List.of(
                                NomenclatureItemStatus.ACTIVE,
                                NomenclatureItemStatus.INACTIVE
                        )
                )
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id: %s;".formatted(
                        deposit.getCurrencyId())));
        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();

        if (exchangeRate != null) {
            customerReceivable.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(customerReceivable.getInitialAmount()
                    .multiply(
                            currency.getAltCurrencyExchangeRate())));
            customerReceivable.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(customerReceivable.getInitialAmount()
                    .multiply(
                            currency.getAltCurrencyExchangeRate())));
        }
        customerReceivable.setOutgoingDocumentFromExternalSystem(deposit.getDepositNumber());
        customerReceivable.setCustomerId(deposit.getCustomerId());
        customerReceivable.setOutgoingDocumentType(OutgoingDocumentType.DEPOSIT);
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setAlternativeRecipientOfInvoiceId(deposit.getCustomerId());
        customerReceivable.setCostCenterControllingOrder(deposit.getCostCenter());


        String number = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_DEPOSIT.name());
        if (number == null) {
            throw new DomainEntityNotFoundException("Income account number not found default for deposit!;");
        }

        customerReceivable.setIncomeAccountNumber(number);

        customerReceivable.setOccurrenceDate(LocalDate.now());
        customerReceivable.setDueDate(LocalDate.now());
        return customerReceivable;
    }

    /**
     * Maps the given payment details and initial amount into a new CustomerReceivable object.
     *
     * @param payment       the Payment object containing information about the payment to be mapped.
     * @param initialAmount the initial monetary amount associated with the CustomerReceivable.
     * @return a new CustomerReceivable object populated with data derived from the provided payment details and initial amount.
     */
    private CustomerReceivable mapFromPayment(Payment payment, BigDecimal initialAmount) {
        CustomerReceivable customerReceivable = new CustomerReceivable();

        customerReceivable.setReceivableNumber("TEMPORARY_NUMBER");
        customerReceivable.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmount));
        customerReceivable.setCurrentAmount(BigDecimal.ZERO);
        customerReceivable.setOccurrenceDate(payment.getPaymentDate());
        customerReceivable.setDueDate(payment.getPaymentDate());
        customerReceivable.setCurrencyId(payment.getCurrencyId());

        Currency currency = currencyRepository.findCurrencyByIdAndStatuses(
                        payment.getCurrencyId(),
                        List.of(
                                NomenclatureItemStatus.ACTIVE,
                                NomenclatureItemStatus.INACTIVE
                        )
                )
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id: %s;".formatted(
                        payment.getCurrencyId())));
        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();

        if (exchangeRate != null) {
            customerReceivable.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(customerReceivable.getInitialAmount()
                    .multiply(
                            currency.getAltCurrencyExchangeRate())));
            customerReceivable.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(customerReceivable.getInitialAmount()
                    .multiply(
                            currency.getAltCurrencyExchangeRate())));
        }

        CacheObject cacheObject = accountingPeriodsRepository.findAccountingPeriodsByDate(LocalDateTime.now())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period for current month not found!;"));
        customerReceivable.setAccountPeriodId(cacheObject.getId());

        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_RECEIVABLES.name());
        if (incomeAccountNumber == null) {
            throw new DomainEntityNotFoundException("Default income account not found for receivables!;");
        }

        customerReceivable.setIncomeAccountNumber(incomeAccountNumber);
        customerReceivable.setOutgoingDocumentFromExternalSystem(payment.getPaymentNumber());
        customerReceivable.setCustomerId(payment.getCustomerId());
        customerReceivable.setOutgoingDocumentType(OutgoingDocumentType.PAYMENT);
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setAlternativeRecipientOfInvoiceId(payment.getCustomerId());

        return customerReceivable;
    }

    private String getSearchByEnum(CustomerReceivableSearchBy searchFields) {
        return searchFields != null ? searchFields.getValue() : CustomerReceivableSearchBy.ALL.getValue();
    }

    private String getSortByEnum(CustomerReceivableListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : CustomerReceivableListColumns.ID.getValue();
    }

    private void validateForNulls(CustomerReceivableRequest request, List<String> errorMessages) {
        if (request.getAccountingPeriodId() != null) {
            errorMessages.add("accountingPeriodId-[accountingPeriodId] is disabled");
        }

        if (request.getInitialAmount() != null) {
            errorMessages.add("initialAmount-[initialAmount] initial amount is disabled;");
        }
        if (request.getCurrencyId() != null) {
            errorMessages.add("currencyId-[currencyId] currencyId is disabled;");
        }
        if (request.getOutgoingDocumentForAnExternalSystem() != null) {
            errorMessages.add(
                    "outgoingDocumentForAnExternalSystem-[outgoingDocumentForAnExternalSystem] outgoing document for an external system is disabled;");
        }
        if (request.getBasisForIssuing() != null) {
            errorMessages.add("basisForIssuing-[basisForIssuing] basis for issuing is disabled");
        }
        if (request.getNumberOfIncomeAccount() != null) {
            errorMessages.add("numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is disabled;");
        }
        if (request.getCostCenterControllingOrder() != null) {
            errorMessages.add("costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is disabled;");
        }
        if (request.getBankAccount() != null) {
            errorMessages.add("bankAccount-[bankAccount] bank account is disabled;");
        }
        if (request.getAdditionalReceivableInformation() != null) {
            errorMessages.add(
                    "additionalReceivableInformation-[additionalReceivableInformation] additional receivable information is disabled;");
        }
        if (request.getCustomerId() != null) {
            errorMessages.add("customer-[customerId] customerId is disabled;");
        }
        if (request.getAlternativeInvoiceRecipientCustomerId() != null) {
            errorMessages.add(
                    "alternativeRecipientOfInvoiceId-[alternativeRecipientOfInvoiceId] alternative recipient of invoice id is disabled;");
        }

        if (request.getBillingGroupId() != null) {
            errorMessages.add("billingGroupId-[billingGroupId] groupId is disabled;");
        }

        if (request.getBankId() != null) {
            errorMessages.add("bankId-[bankId] BankId is disabled;");
        }

        if (request.getOccurrenceDate() != null) {
            errorMessages.add("occurrenceDate-[occurrenceDate] occurrence date is disabled;");
        }

        if (request.getDueDate() != null) {
            errorMessages.add("dueDate-[dueDate] due date is disabled;");
        }
    }

    /**
     * Checks if the blocked for offsetting fields have changed
     */
    private boolean haveBlockedForOffsettingFieldsChanged(CustomerReceivableRequest request, CustomerReceivable customerReceivable) {
        return booleanChanged(request.isBlockedForOffsetting(), customerReceivable.getBlockedForPayment()) ||
                !Objects.equals(request.getBlockedFromDate(), customerReceivable.getBlockedForPaymentFromDate()) ||
                !Objects.equals(request.getBlockedToDate(), customerReceivable.getBlockedForPaymentToDate()) ||
                !Objects.equals(request.getReasonId(), customerReceivable.getBlockedForPaymentBlockingReasonId()) ||
                !Objects.equals(request.getAdditionalInformation(), customerReceivable.getBlockedForPaymentAdditionalInformation());
    }

    /**
     * Safely compares booleans, handling null values
     */
    private boolean booleanChanged(Boolean requestValue, Boolean entityValue) {
        boolean requestBool = requestValue != null ? requestValue : false;
        boolean entityBool = entityValue != null ? entityValue : false;
        return requestBool != entityBool;
    }

    /**
     * Retrieves a {@link CustomerReceivableResponse} for the given {@code id}.
     * <p>
     * If the {@link CustomerReceivable} has a status of {@link EntityStatus#DELETED}, the user must have the
     * {@code CUSTOMER_RECEIVABLE_VIEW_DELETE} permission to view it.
     * <p>
     * The method retrieves the related {@link PaymentReceivableOffsetting} and {@link CustomerLiabilityPaidByReceivable}
     * entities, and sets them in the {@link CustomerReceivableResponse} before returning it.
     *
     * @param id the ID of the {@link CustomerReceivable} to retrieve
     * @return the {@link CustomerReceivableResponse} for the given {@code id}
     * @throws DomainEntityNotFoundException if the {@link CustomerReceivable} is not found
     */
    public CustomerReceivableResponse view(Long id) {
        CustomerReceivable customerReceivable = customerReceivableRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Receivable not found with id :" + id));

        if (customerReceivable.getStatus() == EntityStatus.DELETED) {
            checkPermission(CUSTOMER_RECEIVABLE_VIEW_DELETE);
        }
        List<PaymentReceivableOffsetting> paymentReceivableOffsetting = customerReceivableRepository.findPaymentReceivableOffsetting(id);
        List<CustomerLiabilityPaidByReceivable> customerLiabilityPaidByReceivables = customerReceivableRepository.findLiabilityPaidByReceivable(id);
        CustomerReceivableResponse response = CustomerReceivableResponse.from(customerReceivable);
        setSubResponses(response, customerReceivable, paymentReceivableOffsetting, customerLiabilityPaidByReceivables);
        return response;
    }

    private void setSubResponses(
            CustomerReceivableResponse response,
            CustomerReceivable customerReceivable,
            List<PaymentReceivableOffsetting> paymentReceivableOffsettings,
            List<CustomerLiabilityPaidByReceivable> customerLiabilityPaidByReceivables
    ) {
        AccountingPeriods accountingPeriods = accountingPeriodsRepository.findById(customerReceivable.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting Periods not found with id :" + customerReceivable.getId()));
        AccountingPeriodsResponse accountingPeriodsResponse = new AccountingPeriodsResponse(accountingPeriods);
        response.setAccountingPeriodResponse(accountingPeriodsResponse);

        Currency currency = currencyRepository.findById(customerReceivable.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id :" + customerReceivable.getCurrencyId()));
        ShortResponse currencyShortResponse = new ShortResponse(currency.getId(), currency.getName());
        response.setCurrencyResponse(currencyShortResponse);

        if (customerReceivable.getBankId() != null) {
            Bank bank = bankRepository.findById(customerReceivable.getBankId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank not found with id :" + customerReceivable.getBankId()));
            BankResponse bankResponse = new BankResponse(bank);
            response.setBankResponse(bankResponse);
        }

        if (customerReceivable.getBlockedForPaymentBlockingReasonId() != null) {
            BlockingReason blockingReason = blockingReasonRepository.findById(customerReceivable.getBlockedForPaymentBlockingReasonId())
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "BlockingReason not found with id :" + customerReceivable.getBlockedForPaymentBlockingReasonId()));
            ShortResponse blockingReasonShortResponse = new ShortResponse(blockingReason.getId(), blockingReason.getName());
            response.setReason(blockingReasonShortResponse);
        }

        CustomerDetailsShortResponse customerDetailsShortResponse = getCustomerDetailsShortResponse(
                customerReceivable.getCustomerId()
        );
        response.setCustomerResponse(customerDetailsShortResponse);

        CustomerDetailsShortResponse alternativeRecipientCustomerDetailsShortResponse = null;

        if (customerReceivable.getBillingGroupId() != null) {
            ContractBillingGroup contractBillingGroup = billingGroupRepository.findById(customerReceivable.getBillingGroupId())
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "Billing group not found with id : " + customerReceivable.getBillingGroupId()));
            BillingGroupListingResponse billingGroupListingResponse = new BillingGroupListingResponse(
                    contractBillingGroup.getId(),
                    contractBillingGroup.getGroupNumber()
            );
            response.setBillingGroupResponse(billingGroupListingResponse);
            if (contractBillingGroup.getAlternativeRecipientCustomerDetailId() != null) {
                CustomerDetails customerDetails = customerDetailsRepository.findById(contractBillingGroup.getAlternativeRecipientCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException(
                                "Customer detail not found with id : " + contractBillingGroup.getAlternativeRecipientCustomerDetailId()));
                Customer customer = customerRepository.findById(customerDetails.getCustomerId())
                        .orElseThrow(() -> new DomainEntityNotFoundException(
                                "Customer not found with id :" + customerDetails.getCustomerId()));
                alternativeRecipientCustomerDetailsShortResponse = new CustomerDetailsShortResponse(
                        customer.getId(),
                        customerDetails.getId(),
                        customerDetails.getName(),
                        customer.getCustomerType(),
                        customerDetails.getBusinessActivity()
                );
            }
        }
        if (customerReceivable.getAlternativeRecipientOfInvoiceId() != null) {
            alternativeRecipientCustomerDetailsShortResponse = getCustomerDetailsShortResponse(
                    customerReceivable.getAlternativeRecipientOfInvoiceId()
            );
        }
        response.setAlternativeRecipientOfAnInvoiceResponse(alternativeRecipientCustomerDetailsShortResponse);

        if (customerReceivable.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository
                    .findById(customerReceivable.getInvoiceId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with id :" + customerReceivable.getInvoiceId()));

            String invoiceNumber;
            if (InvoiceDocumentType.CREDIT_NOTE.equals(invoice.getInvoiceDocumentType())) {
                invoiceNumber = "Credit Note-".concat(invoice.getInvoiceNumber());
            } else {
                invoiceNumber = "Invoice-".concat(invoice.getInvoiceNumber());
            }
            InvoiceShortResponse invoiceShortResponse = new InvoiceShortResponse(
                    invoice.getId(),
                    invoiceNumber
            );
            response.setInvoiceResponse(invoiceShortResponse);
        }
        response.setActionId(response.getActionId());
        response.setCustomerOffsettingResponseList(getOffsettingResponseList(customerReceivable.getId()));

        String outgoingDocumentFromExternalSystem = customerReceivable.getOutgoingDocumentFromExternalSystem();
        boolean isOutDocPresent = !Objects.isNull(outgoingDocumentFromExternalSystem) && !outgoingDocumentFromExternalSystem.isEmpty();
        if (OutgoingDocumentType.LATE_PAYMENT_FINE.equals(customerReceivable.getOutgoingDocumentType())) {
            if (isOutDocPresent) {
                Optional<LatePaymentFine> latePaymentNumber = latePaymentFineRepository.findByLatePaymentNumber(outgoingDocumentFromExternalSystem);
                if (latePaymentNumber.isPresent()) {
                    ShortResponse shortResponse = new ShortResponse(
                            latePaymentNumber.get().getId(),
                            "Fine-".concat(latePaymentNumber.get().getLatePaymentNumber())
                    );
                    response.setLatePaymentFineShortResponse(shortResponse);
                }
            }
        } else if (OutgoingDocumentType.DEPOSIT.equals(customerReceivable.getOutgoingDocumentType())) {
            if (isOutDocPresent) {
                Optional<Deposit> deposit = depositRepository.findByDepositNumber(outgoingDocumentFromExternalSystem);
                if (deposit.isPresent()) {
                    ShortResponse shortResponse = new ShortResponse(
                            deposit.get().getId(),
                            deposit.get().getDepositNumber()
                    );
                    response.setDepositShortResponse(shortResponse);
                }
            }
        } else if (OutgoingDocumentType.PAYMENT.equals(customerReceivable.getOutgoingDocumentType())) {
            if (isOutDocPresent) {
                Optional<Payment> payment = paymentRepository.findByPaymentNumber(outgoingDocumentFromExternalSystem);
                if (payment.isPresent()) {
                    ShortResponse shortResponse = new ShortResponse(
                            payment.get().getId(),
                            payment.get().getPaymentNumber()
                    );
                    response.setPaymentShortResponse(shortResponse);
                }
            }
        }
    }

    /**
     * Retrieves a list of CustomerOffsettingResponse objects based on the given receivable ID.
     *
     * @param receivableId the unique identifier of the receivable for which offsetting responses are to be fetched
     * @return a list of CustomerOffsettingResponse objects associated with the specified receivable ID
     */
    private List<CustomerOffsettingResponse> getOffsettingResponseList(Long receivableId) {
        return objectOffsettingService
                .fetchObjectOffsettings(ObjectOffsettingType.RECEIVABLE, receivableId)
                .stream()
                .map(CustomerOffsettingResponse::from)
                .collect(Collectors.toList());
    }

    private String getCustomerDetailedName(Customer customer, CustomerDetails customerDetails) {
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        return String.format(
                "%s (%s%s%s%s)", customer.getIdentifier(), customerDetails.getName(),
                customerDetails.getMiddleName() != null ? " " + customerDetails.getMiddleName() : "",
                customerDetails.getLastName() != null ? " " + customerDetails.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? " " + legalFormName : ""
        );
    }

    private CustomerDetailsShortResponse getCustomerDetailsShortResponse(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found with id :" + customerId));
        CustomerDetails customerDetails = customerDetailsRepository.findById(customer.getLastCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer detail not found with id : " + customer.getLastCustomerDetailId()));
        String customerDetailedName = getCustomerDetailedName(customer, customerDetails);

        return new CustomerDetailsShortResponse(
                customer.getId(),
                customerDetails.getId(),
                customerDetailedName,
                customer.getCustomerType(),
                customerDetails.getBusinessActivity()
        );
    }

    private void setRestFields(CustomerReceivableRequest request, CustomerReceivable customerReceivable) {
        customerReceivable.setOutgoingDocumentFromExternalSystem(request.getOutgoingDocumentForAnExternalSystem());
        customerReceivable.setBasisForIssuing(request.getBasisForIssuing());
        customerReceivable.setIncomeAccountNumber(request.getNumberOfIncomeAccount());
        customerReceivable.setCostCenterControllingOrder(request.getCostCenterControllingOrder());
        customerReceivable.setAdditionalInformation(request.getAdditionalReceivableInformation());
    }

    private void validateCustomerReceivableCreateRequest(
            CustomerReceivableRequest request,
            List<String> errorMessages,
            CustomerReceivable customerReceivable
    ) {
        checkAccountingPeriods(request, errorMessages, customerReceivable);
        checkCurrency(request, errorMessages, customerReceivable);
        checkDirectDebit(request, errorMessages, customerReceivable);
        checkBlockedForOffsetting(request, errorMessages, customerReceivable);

        checkSubObjects(request, errorMessages, customerReceivable);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    private void checkSubObjects(
            CustomerReceivableRequest request,
            List<String> errorMessages,
            CustomerReceivable customerReceivable
    ) {
        if (customerRepository.existsByIdAndStatusIn(request.getCustomerId(), List.of(CustomerStatus.ACTIVE))) {
            customerReceivable.setCustomerId(request.getCustomerId());
        } else {
            errorMessages.add("customerId-[customerId] customer not found;");
        }

        if (request.getBillingGroupId() != null) {
            if (!billingGroupRepository.existsById(request.getBillingGroupId())) {
                errorMessages.add("billingGroupId-[billingGroupId] billing group not found;");
            }
        }
        customerReceivable.setBillingGroupId(request.getBillingGroupId());

        if (request.getAlternativeInvoiceRecipientCustomerId() != null) {
            if (!customerRepository.existsByIdAndStatusIn(
                    request.getAlternativeInvoiceRecipientCustomerId(),
                    List.of(CustomerStatus.ACTIVE)
            )) {
                errorMessages.add("alternativeRecipientOfInvoiceId-[alternativeRecipientOfInvoiceId] alternative recipient not found;");

            }
        }
        customerReceivable.setAlternativeRecipientOfInvoiceId(request.getAlternativeInvoiceRecipientCustomerId());
    }

    private void checkBlockedForOffsetting(
            CustomerReceivableRequest request,
            List<String> errorMessages,
            CustomerReceivable customerReceivable
    ) {
        if (request.isBlockedForOffsetting() && !blockingReasonRepository.existsByIdAndStatusIn(
                request.getReasonId(),
                List.of(NomenclatureItemStatus.ACTIVE)
        )) {
            errorMessages.add("reasonId-[reasonId] reason not found;");
        }
        customerReceivable.setBlockedForPaymentToDate(request.getBlockedToDate());
        customerReceivable.setBlockedForPaymentBlockingReasonId(request.getReasonId());
        customerReceivable.setBlockedForPaymentFromDate(request.getBlockedFromDate());
        customerReceivable.setBlockedForPaymentAdditionalInformation(request.getAdditionalInformation());
        customerReceivable.setBlockedForPayment(request.isBlockedForOffsetting());
    }

    private void checkDirectDebit(
            CustomerReceivableRequest request,
            List<String> errorMessages,
            CustomerReceivable customerReceivable
    ) {
        if (request.getBankId() != null && !bankRepository.existsByIdAndStatusIn(
                request.getBankId(),
                List.of(NomenclatureItemStatus.ACTIVE)
        )) {
            errorMessages.add("bankId-[bankId] Bank with such ID not found!;");
        }
        customerReceivable.setBankId(request.getBankId());
        customerReceivable.setBankAccount(request.getBankAccount());
        customerReceivable.setDirectDebit(request.isDirectDebit());
    }

    private void checkCurrency(CustomerReceivableRequest request, List<String> errorMessages, CustomerReceivable customerReceivable) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(
                request.getCurrencyId(),
                List.of(NomenclatureItemStatus.ACTIVE)
        );
        if (currencyOptional.isEmpty()) {
            errorMessages.add("currencyId-[currencyId] currency with such ID not found!;");
        } else {
            calculateAndSetAmounts(request, customerReceivable, currencyOptional.get());
        }
    }

    private void checkAccountingPeriods(
            CustomerReceivableRequest request,
            List<String> errorMessages,
            CustomerReceivable customerReceivable
    ) {
        Optional<AccountingPeriods> accountingPeriodsOptional = accountingPeriodsRepository.findByIdAndStatus(
                request.getAccountingPeriodId(),
                AccountingPeriodStatus.OPEN
        );
        if (accountingPeriodsOptional.isEmpty()) {
            errorMessages.add("accountingPeriodId-[accountingPeriodId] accounting period not found!;");
        } else if (accountingPeriodsOptional.get()
                .getStartDate()
                .isAfter(LocalDateTime.now())) {
            errorMessages.add("accountingPeriodId-[accountingPeriodId] account period should be past;");
        } else {
            customerReceivable.setAccountPeriodId(accountingPeriodsOptional.get()
                    .getId());
        }
    }

    private void calculateAndSetAmounts(CustomerReceivableRequest request, CustomerReceivable customerReceivable, Currency currency) {
        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();
        BigDecimal amountInOtherCurrency = null;
        if (exchangeRate != null) {
            amountInOtherCurrency = request.getInitialAmount()
                    .multiply(exchangeRate);
            amountInOtherCurrency = amountInOtherCurrency.setScale(2, RoundingMode.HALF_UP);
        }
        customerReceivable.setInitialAmount(request.getInitialAmount());
        customerReceivable.setCurrentAmount(request.getInitialAmount());
        customerReceivable.setCurrencyId(request.getCurrencyId());
        customerReceivable.setInitialAmountInOtherCurrency(amountInOtherCurrency);
        customerReceivable.setCurrentAmountInOtherCurrency(amountInOtherCurrency);
    }


    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(CUSTOMER_RECEIVABLE)
                .contains(permission.getId())) {
            throw new ClientException(
                    "You don't have appropriate permission: %s;".formatted(permission.name()),
                    ErrorCode.OPERATION_NOT_ALLOWED
            );
        }
    }

    private boolean permissionCheck(PermissionEnum permissionEnum) {
        return permissionService.getPermissionsFromContext(CUSTOMER_RECEIVABLE)
                .contains(permissionEnum.getId());
    }

    public CustomerReceivable createReceivableDependingOnCustomerVatBase(
            Invoice invoice,
            BigDecimal customerReceivableCalculatedAmount
    ) {
        if (!invoice.getInvoiceStatus()
                .equals(InvoiceStatus.REAL)) {
            throw new ClientException("Invalid invoice type or status!", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        CustomerReceivable customerReceivable = customerReceivableMapperService.mapReceivableFromInvoiceByVatBase(
                invoice,
                customerReceivableCalculatedAmount
        );
        CustomerReceivable savedReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(savedReceivable);

        return savedReceivable;
    }

    /**
     * Creates a new {@link CustomerReceivable} from the reverse compensation data.
     *
     * <p>This method uses the provided {@link InvoiceCompensation} to map the relevant
     * information (such as compensation receipt ID, currency ID, invoice ID, and compensation
     * amount) into a new {@link CustomerReceivable} entity for reverse compensation scenarios.
     * The created receivable is then saved to the database, and a receivable number is assigned
     * based on the entity's ID.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the data used
     *                            to create the customer receivable.
     * @return A {@link CustomerReceivable} object that has been created and saved to the database.
     * @throws DomainEntityNotFoundException If any of the related entities (such as currency,
     *                                       invoice, or account) cannot be found during the mapping process.
     */
    @Transactional
    public CustomerReceivable createReceivableFromReverseCompensation(InvoiceCompensation invoiceCompensation) {
        CustomerReceivable customerReceivable = customerReceivableMapperService.mapFromInvoiceCompensation(
                invoiceCompensation.getCompensationReceiptId(),
                invoiceCompensation.getCurrencyId(),
                invoiceCompensation.getInvoiceId(),
                invoiceCompensation.getCompAmount()
        );
        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
        return customerReceivable;
    }

    /**
     * Creates a new {@link CustomerReceivable} from the invoiced compensation data.
     *
     * <p>This method uses the provided {@link InvoiceCompensation} to map the relevant
     * information (such as customer ID, currency ID, invoice ID, and compensation amount)
     * into a new {@link CustomerReceivable} entity for invoiced compensation scenarios.
     * The created receivable is then saved to the database, and a receivable number is assigned
     * based on the entity's ID.</p>
     *
     * @param invoiceCompensation The invoice compensation object containing the data used
     *                            to create the customer receivable.
     * @return A {@link CustomerReceivable} object that has been created and saved to the database.
     * @throws DomainEntityNotFoundException If any of the related entities (such as currency,
     *                                       invoice, or account) cannot be found during the mapping process.
     */
    @Transactional
    public CustomerReceivable createReceivableFromInvoicedCompensation(InvoiceCompensation invoiceCompensation) {
        CustomerReceivable customerReceivable = customerReceivableMapperService.mapFromInvoiceCompensation(
                invoiceCompensation.getCustomerId(),
                invoiceCompensation.getCurrencyId(),
                invoiceCompensation.getInvoiceId(),
                invoiceCompensation.getCompAmount()
        );
        customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);
        return customerReceivable;
    }
}
