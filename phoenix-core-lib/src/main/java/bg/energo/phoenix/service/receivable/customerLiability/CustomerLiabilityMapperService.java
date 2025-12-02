package bg.energo.phoenix.service.receivable.customerLiability;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.InvoiceCompensation;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.entity.receivable.customerLiability.*;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositPaymentDeadlineAfterWithdrawal;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositPaymentDeadlineExclude;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityRequest;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.OffsettingObject;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityListingResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityPodResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingInstallment;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceStandardDetailedDataVatBaseRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.*;
import bg.energo.phoenix.repository.receivable.deposit.DepositPaymentDeadlineAfterWithdrawalRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.receivable.ObjectOffsettingService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER_LIABILITY;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLiabilityMapperService {
    public static final String POD_DATA_ROW_DELIMITER = "~";
    public static final String POD_DATA_COLUMN_DELIMITER = ",";
    private final InterestRateRepository interestRateRepository;
    private final CustomerRepository customerRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final BankRepository bankRepository;
    private final ContractBillingGroupRepository billingGroupRepository;
    private final PermissionService permissionService;
    private final BlockingReasonRepository blockingReasonRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final CustomerLiabilityPaidByReceivableRepository customerLiabilityPaidByReceivableRepository;
    private final CustomerLiabilityPaidByReschedulingRepository customerLiabilityPaidByReschedulingRepository;
    private final CustomerLiabilityPaidByPaymentRepository customerLiabilityPaidByPaymentRepository;
    private final CustomerLiabilityPaidByDepositRepository customerLiabilityPaidByDepositRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final DepositPaymentDeadlineAfterWithdrawalRepository depositPaymentDeadlineAfterWithdrawalRepository;
    private final InvoiceStandardDetailedDataVatBaseRepository invoiceStandardDetailedDataVatBaseRepository;
    private final DepositRepository depositRepository;
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final ObjectOffsettingService objectOffsettingService;

    /**
     * Maps a CustomerLiability entity to a CustomerLiabilityResponse DTO.
     *
     * @param customerLiability the CustomerLiability entity to map
     * @return the CustomerLiabilityResponse DTO
     */
    public CustomerLiabilityResponse mapToCustomerLiabilityResponse(CustomerLiability customerLiability) {
        CustomerLiabilityResponse response = new CustomerLiabilityResponse();
        response.setId(customerLiability.getId());
        response.setNumber(customerLiability.getLiabilityNumber());
        response.setAccountPeriodId(customerLiability.getAccountPeriodId());
        response.setDueDate(customerLiability.getDueDate());
        response.setFullOffsetDate(customerLiability.getFullOffsetDate());
        response.setInterestDateFrom(customerLiability.getApplicableInterestRateDateFrom());
        response.setInterestDateTo(customerLiability.getApplicableInterestRateDateTo());
        response.setInitialAmount(customerLiability.getInitialAmount());
        response.setInitialAmountInOtherCurrency(customerLiability.getInitialAmountInOtherCurrency());
        response.setCurrentAmount(customerLiability.getCurrentAmount());
        response.setCurrentAmountInOtherCurrency(customerLiability.getCurrentAmountInOtherCurrency());
        response.setOutgoingDocumentFromExternalSystem(customerLiability.getOutgoingDocumentFromExternalSystem());
        response.setBasisForIssuing(customerLiability.getBasisForIssuing());
        response.setNumberOfIncomeAccount(customerLiability.getIncomeAccountNumber());
        response.setCostCenterControllingOrder(customerLiability.getCostCenterControllingOrder());
        response.setDirectDebit(customerLiability.getDirectDebit());
        response.setIban(customerLiability.getIban());
        response.setBlockedForPayment(customerLiability.getBlockedForPayment());
        response.setBlockedForPaymentFromDate(customerLiability.getBlockedForPaymentFromDate());
        response.setBlockedForPaymentToDate(customerLiability.getBlockedForPaymentToDate());
        response.setBlockedForPaymentReason(getBlockedReasonResponse(customerLiability.getBlockedForPaymentBlockingReasonId()));
        response.setBlockedForPaymentAdditionalInfo(customerLiability.getBlockedForPaymentAdditionalInfo());
        response.setBlockedForReminderLetters(customerLiability.getBlockedForReminderLetters());
        response.setBlockedForReminderLettersFromDate(customerLiability.getBlockedForReminderLettersFromDate());
        response.setBlockedForReminderLettersToDate(customerLiability.getBlockedForReminderLettersToDate());
        response.setBlockedForReminderLettersReason(getBlockedReasonResponse(customerLiability.getBlockedForReminderLettersBlockingReasonId()));
        response.setBlockedForReminderLettersAdditionalInfo(customerLiability.getBlockedForReminderLettersAdditionalInfo());
        response.setBlockedForCalculationOfLatePayment(customerLiability.getBlockedForCalculationOfLatePayment());
        response.setBlockedForCalculationOfLatePaymentFromDate(customerLiability.getBlockedForCalculationOfLatePaymentFromDate());
        response.setBlockedForCalculationOfLatePaymentToDate(customerLiability.getBlockedForCalculationOfLatePaymentToDate());
        response.setBlockedForCalculationOfLatePaymentReason(getBlockedReasonResponse(customerLiability.getBlockedForCalculationOfLatePaymentBlockingReasonId()));
        response.setBlockedForCalculationOfLatePaymentAdditionalInfo(customerLiability.getBlockedForCalculationOfLatePaymentAdditionalInfo());
        response.setBlockedForLiabilitiesOffsetting(customerLiability.getBlockedForLiabilitiesOffsetting());
        response.setBlockedForLiabilitiesOffsettingFromDate(customerLiability.getBlockedForLiabilitiesOffsettingFromDate());
        response.setBlockedForLiabilitiesOffsettingToDate(customerLiability.getBlockedForLiabilitiesOffsettingToDate());
        response.setBlockedForLiabilitiesOffsettingReason(getBlockedReasonResponse(customerLiability.getBlockedForLiabilitiesOffsettingBlockingReasonId()));
        response.setBlockedForLiabilitiesOffsettingAdditionalInfo(customerLiability.getBlockedForLiabilitiesOffsettingAdditionalInfo());
        response.setBlockedForSupplyTermination(customerLiability.getBlockedForSupplyTermination());
        response.setBlockedForSupplyTerminationFromDate(customerLiability.getBlockedForSupplyTerminationFromDate());
        response.setBlockedForSupplyTerminationToDate(customerLiability.getBlockedForSupplyTerminationToDate());
        response.setBlockedForSupplyTerminationReason(getBlockedReasonResponse(customerLiability.getBlockedForSupplyTerminationBlockingReasonId()));
        response.setBlockedForSupplyTerminationAdditionalInfo(customerLiability.getBlockedForSupplyTerminationAdditionalInfo());
        response.setInvoiceResponse(customerLiability.getInvoiceId() != null ? getInvoiceResponse(customerLiability.getInvoiceId()) : null);
        response.setActionShortResponse(customerLiability.getActionId() != null ? getActionResponse(customerLiability.getActionId()) : null);
        response.setDepositShortResponse(customerLiability.getDepositId() != null ? getDepositResponse(customerLiability.getDepositId()) : null);
        response.setAdditionalInfo(customerLiability.getAdditionalInfo());
        response.setOutgoingDocumentType(customerLiability.getOutgoingDocumentType());
        response.setStatus(customerLiability.getStatus());
        response.setCreationType(customerLiability.getCreationType());
        response.setApplicableInterestRate(customerLiability.getApplicableInterestRateId() != null ? getInterestRate(customerLiability.getApplicableInterestRateId()) : null);
        response.setCurrencyResponse(getCurrencyResponse(customerLiability.getCurrencyId()));
        response.setCustomerResponse(getCustomerResponse(customerLiability.getCustomerId()));
        response.setCustomerLiabilityOffsettingReponseList(getCustomerLiabilityOffsettingResponseList(customerLiability.getId()));
        response.setCreationDate(LocalDate.from(customerLiability.getCreateDate()));
        response.setAmountWithoutInterest(customerLiability.getAmountWithoutInterest() == null ? BigDecimal.ZERO : customerLiability.getAmountWithoutInterest());
        response.setAmountWithoutInterestInOtherCurrency(customerLiability.getAmountWithoutInterestInOtherCurrency() == null ? BigDecimal.ZERO : customerLiability.getAmountWithoutInterestInOtherCurrency());
        response.setOccurrenceDate(customerLiability.getOccurrenceDate());
        response.setReschedulingShortResponse(
                customerLiability.getOutgoingDocumentType() != null ? customerLiability.getOutgoingDocumentType()
                        .equals(CustomerLiabilitiesOutgoingDocType.RESCHEDULING)
                        ? getReschedulingResponse(customerLiability.getOutgoingDocumentFromExternalSystem())
                        : null : null
        );

        if (CustomerLiabilitiesOutgoingDocType.LATE_PAYMENT_FINE.equals(customerLiability.getOutgoingDocumentType())) {
            String outgoingDocumentFromExternalSystem = customerLiability.getOutgoingDocumentFromExternalSystem();
            if (!Objects.isNull(outgoingDocumentFromExternalSystem) && !outgoingDocumentFromExternalSystem.isEmpty()) {
                Optional<LatePaymentFine> latePaymentNumber = latePaymentFineRepository.findByLatePaymentNumber(
                        outgoingDocumentFromExternalSystem);
                if (latePaymentNumber.isPresent()) {
                    ShortResponse shortResponse = new ShortResponse(
                            latePaymentNumber.get()
                                    .getId(),
                            "Fine-".concat(latePaymentNumber.get()
                                    .getLatePaymentNumber())
                    );
                    response.setLatePaymentFineShortResponse(shortResponse);
                }
            }
        }

        AccountingPeriods accountingPeriod = getAccountingPeriod(customerLiability.getAccountPeriodId());
        response.setAccountPeriodId(accountingPeriod.getId());
        response.setAccountingPeriodName(accountingPeriod.getName());
        response.setAccountingPeriodStatus(accountingPeriod.getStatus());

        if (customerLiability.getBankId() != null) {
            response.setBankResponse(getBank(customerLiability.getBankId()));
        }

        if (customerLiability.getContractBillingGroupId() != null) {
            response.setBillingGroupResponse(getBillingGroupResponse(customerLiability.getContractBillingGroupId()));
        }

        CustomerDetailsShortResponse alternativeRecipientCustomerDetailsShortResponse = null;

        if (customerLiability.getContractBillingGroupId() != null) {
            ContractBillingGroup contractBillingGroup = billingGroupRepository.findById(customerLiability.getContractBillingGroupId())
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "Billing group not found with id : %s;".formatted(
                                    customerLiability.getContractBillingGroupId())));

            if (contractBillingGroup.getAlternativeRecipientCustomerDetailId() != null) {
                CustomerDetails customerDetails = customerDetailsRepository.findById(contractBillingGroup.getAlternativeRecipientCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException(
                                "Customer detail not found with id : %s".formatted(
                                        contractBillingGroup.getAlternativeRecipientCustomerDetailId())));

                Customer customer = customerRepository.findById(customerDetails.getCustomerId())
                        .orElseThrow(() -> new DomainEntityNotFoundException(
                                "Customer not found with id : %s;".formatted(customerDetails.getCustomerId())));
                alternativeRecipientCustomerDetailsShortResponse = new CustomerDetailsShortResponse(
                        customer.getId(),
                        customerDetails.getId(),
                        customerDetails.getName(),
                        customer.getCustomerType(),
                        customerDetails.getBusinessActivity()
                );
            }
        }

        if (customerLiability.getAltInvoiceRecipientCustomerId() != null) {
            alternativeRecipientCustomerDetailsShortResponse = getCustomerResponse(customerLiability.getAltInvoiceRecipientCustomerId());
        }

        response.setAlternativeInvoiceRecipientCustomerResponse(alternativeRecipientCustomerDetailsShortResponse);

        if (customerLiability.getAltInvoiceRecipientCustomerId() != null) {
            response.setAlternativeInvoiceRecipientCustomerResponse(getCustomerResponse(customerLiability.getAltInvoiceRecipientCustomerId()));
        }

        return response;
    }

    /**
     * Maps the parameters from a CustomerLiabilityRequest to a new CustomerLiability instance.
     *
     * @param request the CustomerLiabilityRequest containing the parameters to map
     * @return a new CustomerLiability instance with the mapped parameters
     */
    public CustomerLiability mapParametersForCreate(CustomerLiabilityRequest request) {
        CustomerLiability customerLiability = new CustomerLiability();
        //set temporary number for saving
        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setDueDate(request.getDueDate());
        customerLiability.setAccountPeriodId(request.getAccountingPeriodId());
        customerLiability.setApplicableInterestRateId(request.getApplicableInterestRateId());
        customerLiability.setApplicableInterestRateDateFrom(request.getInterestDateFrom());
        customerLiability.setApplicableInterestRateDateTo(request.getInterestDateTo());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        customerLiability.setAmountWithoutInterest(request.getAmountWithoutInterest() == null ? BigDecimal.ZERO : request.getAmountWithoutInterest());
        customerLiability.setAmountWithoutInterestInOtherCurrency(BigDecimal.ZERO);
        customerLiability.setCurrencyId(request.getCurrencyId());
        customerLiability.setOutgoingDocumentFromExternalSystem(trim(request.getOutgoingDocumentFromExternalSystem()));
        customerLiability.setBasisForIssuing(trim(request.getBasisForIssuing()));
        customerLiability.setIncomeAccountNumber(trim(request.getNumberOfIncomeAccount()));
        customerLiability.setCostCenterControllingOrder(trim(request.getCostCenterControllingOrder()));
        customerLiability.setDirectDebit(request.isDirectDebit());
        customerLiability.setBankId(request.getBankId());
        customerLiability.setIban(request.getBankAccount());
        customerLiability.setAdditionalInfo(trim(request.getAdditionalLiabilityInformation()));
        customerLiability.setCustomerId(request.getCustomerId());
        customerLiability.setCreationType(CreationType.MANUAL);
        customerLiability.setOccurrenceDate(request.getOccurrenceDate());
        customerLiability.setStatus(EntityStatus.ACTIVE);
        return customerLiability;
    }

    /**
     * Maps the parameters from an Invoice to a new CustomerLiability instance.
     *
     * @param invoice                           the Invoice containing the parameters to map
     * @param customerLiabilityCalculatedAmount the calculated amount of the customer liability
     * @param errorMessages                     a list to store any error messages encountered during the mapping
     * @return a new CustomerLiability instance with the mapped parameters
     */
    public CustomerLiability mapLiabilityFromInvoice(
            Invoice invoice,
            BigDecimal customerLiabilityCalculatedAmount,
            List<String> errorMessages
    ) {
        CustomerLiability customerLiability = new CustomerLiability();

        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setAccountPeriodId(invoice.getAccountPeriodId());
        customerLiability.setDueDate(invoice.getPaymentDeadline());
        customerLiability.setApplicableInterestRateId(invoice.getInterestRateId());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(customerLiabilityCalculatedAmount));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(customerLiabilityCalculatedAmount));
        customerLiability.setCurrencyId(invoice.getCurrencyId());
        customerLiability.setOutgoingDocumentFromExternalSystem(invoice.getInvoiceNumber());
        customerLiability.setBasisForIssuing(invoice.getBasisForIssuing());
        customerLiability.setIncomeAccountNumber(invoice.getIncomeAccountNumber());
        customerLiability.setCostCenterControllingOrder(invoice.getCostCenterControllingOrder());
        customerLiability.setDirectDebit(invoice.getDirectDebit());
        customerLiability.setAccountPeriodId(invoice.getAccountPeriodId());

        if (Boolean.TRUE.equals(invoice.getDirectDebit())) {
            customerLiability.setBankId(invoice.getBankId());
            customerLiability.setIban(invoice.getIban());
        }

        Customer customer = customerRepository.findByCustomerDetailIdAndStatusIn(
                        invoice.getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                )
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer not found with customerDetailId: %s;".formatted(invoice.getCustomerDetailId())));

        customerLiability.setCustomerId(customer.getId());
        customerLiability.setContractBillingGroupId(invoice.getContractBillingGroupId());

        if (invoice.getProductContractId() != null) {
            Optional<BillingGroupListingResponse> billingGroup = billingGroupRepository.findAllByContractId(
                            invoice.getProductContractId(),
                            List.of(EntityStatus.ACTIVE)
                    )
                    .stream()
                    .findFirst();
            billingGroup.ifPresent(billingGroupListingResponse -> customerLiability.setContractBillingGroupId(
                    billingGroupListingResponse.getId()));
        }

        customerLiability.setContractBillingGroupId(invoice.getContractBillingGroupId());
        customerLiability.setInvoiceId(invoice.getId());
        customerLiability.setOutgoingDocumentFromExternalSystem(invoice.getInvoiceNumber());
        customerLiability.setOutgoingDocumentType(Objects.equals(invoice.getInvoiceDocumentType(), InvoiceDocumentType.DEBIT_NOTE) ? CustomerLiabilitiesOutgoingDocType.DEBIT_NOTE : CustomerLiabilitiesOutgoingDocType.INVOICE);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        checkCurrencyAndSetAmounts(
                invoice.getCurrencyId(),
                customerLiabilityCalculatedAmount,
                customerLiabilityCalculatedAmount,
                customerLiability,
                errorMessages
        );

        return customerLiability;
    }

    /**
     * Generates additional customer liabilities for the VAT base of an invoice.
     * <p>
     * This method retrieves the VAT base models associated with the given invoice, groups them by customer, and creates a list of
     * {@link CustomerLiability} instances representing the additional liabilities for each customer's VAT base.
     *
     * @param invoice the invoice for which to generate the additional liabilities
     * @return a list of {@link CustomerLiability} instances representing the additional liabilities for the invoice's VAT base
     */
    public List<CustomerLiability> generateAdditionalLiabilitiesForVatBase(Invoice invoice) {
        log.debug("Generating additional liabilities for vat bases");
        List<LiabilityVatBaseModel> vatBaseModels = invoiceStandardDetailedDataVatBaseRepository.findByInvoiceId(invoice.getId());
        if (CollectionUtils.isNotEmpty(vatBaseModels)) {
            Map<Long, List<LiabilityVatBaseModel>> vatBaseGroupedByCustomer = vatBaseModels
                    .stream()
                    .collect(Collectors.groupingBy(LiabilityVatBaseModel::getCustomerId));

            List<CustomerLiability> vatBaseLiabilities = new ArrayList<>();

            for (Map.Entry<Long, List<LiabilityVatBaseModel>> vatBaseEntry : vatBaseGroupedByCustomer.entrySet()) {
                Long customerId = vatBaseEntry.getKey();
                List<LiabilityVatBaseModel> vatBases = vatBaseEntry.getValue();
                LiabilityVatBaseModel anyVatBase = vatBases.get(0);

                BigDecimal totalAmountWithoutVatMainCurrencySummary = EPBDecimalUtils.roundToTwoDecimalPlaces(EPBDecimalUtils.calculateSummary(
                        vatBases.stream()
                                .map(LiabilityVatBaseModel::getTotalAmountWithoutVatMainCurrency)
                                .toList()));
                BigDecimal totalAmountWithoutVatAltCurrencySummary = EPBDecimalUtils.roundToTwoDecimalPlaces(EPBDecimalUtils.calculateSummary(
                        vatBases.stream()
                                .map(LiabilityVatBaseModel::getTotalAmountWithoutVatAltCurrency)
                                .toList()));

                vatBaseLiabilities.add(
                        CustomerLiability
                                .builder()
                                .liabilityNumber("TEMPORARY_NUMBER")
                                .accountPeriodId(invoice.getAccountPeriodId())
                                .dueDate(invoice.getPaymentDeadline())
                                .currencyId(anyVatBase.getMainCurrencyId())
                                .initialAmount(totalAmountWithoutVatMainCurrencySummary)
                                .currentAmount(totalAmountWithoutVatMainCurrencySummary)
                                .outgoingDocumentFromExternalSystem(invoice.getInvoiceNumber())
                                .basisForIssuing(invoice.getBasisForIssuing())
                                .incomeAccountNumber(invoice.getIncomeAccountNumber())
                                .costCenterControllingOrder(invoice.getCostCenterControllingOrder())
                                .directDebit(invoice.getDirectDebit())
                                .accountPeriodId(invoice.getAccountPeriodId())
                                .bankId(invoice.getBankId())
                                .iban(invoice.getIban())
                                .customerId(customerId)
                                .contractBillingGroupId(invoice.getContractBillingGroupId())
                                .invoiceId(invoice.getId())
                                .outgoingDocumentType(CustomerLiabilitiesOutgoingDocType.INVOICE)
                                .creationType(CreationType.AUTOMATIC)
                                .status(EntityStatus.ACTIVE)
                                .initialAmountInOtherCurrency(totalAmountWithoutVatAltCurrencySummary)
                                .currentAmountInOtherCurrency(totalAmountWithoutVatAltCurrencySummary)
                                .build()
                );
            }

            log.debug("Total additional liabilities for vat bases generated: [%s]".formatted(vatBaseLiabilities.size()));

            return customerLiabilityRepository.saveAll(vatBaseLiabilities);
        }
        log.debug("Vat bases for invoice: [%s] not found".formatted(invoice.getInvoiceNumber()));
        return new ArrayList<>();
    }

    /**
     * Maps a customer liability from an Action object.
     *
     * @param action        the Action object to map from
     * @param errorMessages a list to store any error messages encountered during the mapping
     * @return a CustomerLiability object populated with the data from the Action
     */
    public CustomerLiability mapLiabilityFromAction(Action action, List<String> errorMessages) {
        CustomerLiability customerLiability = new CustomerLiability();
        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period not found for current month;"));

        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
        if (incomeAccountNumber == null) {
            throw new ClientException("Unable to find default liability income account number;", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        customerLiability.setIncomeAccountNumber(incomeAccountNumber);
        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerLiability.setDueDate(action.getExecutionDate());
        customerLiability.setOutgoingDocumentType(CustomerLiabilitiesOutgoingDocType.ACTION);
        //When We have Penalty OR Without Automatic Penalty with its Claim Amount, Generated Liability Should use Claim amount Fields
        if ((action.getPenaltyId() != null || action.getWithoutPenalty()) && action.getPenaltyClaimAmount() != null) {
            customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(action.getPenaltyClaimAmount()));
            customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(action.getPenaltyClaimAmount()));
            customerLiability.setCurrencyId(action.getPenaltyClaimCurrencyId());
            checkCurrencyAndSetAmounts(
                    action.getPenaltyClaimCurrencyId(),
                    action.getPenaltyClaimAmount(),
                    action.getPenaltyClaimAmount(),
                    customerLiability,
                    errorMessages
            );
        } else {
            customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(action.getCalculatedPenaltyAmount()));
            customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(action.getCalculatedPenaltyAmount()));
            customerLiability.setCurrencyId(action.getCalculatedPenaltyCurrencyId());
            checkCurrencyAndSetAmounts(
                    action.getCalculatedPenaltyCurrencyId(),
                    action.getCalculatedPenaltyAmount(),
                    action.getCalculatedPenaltyAmount(),
                    customerLiability,
                    errorMessages
            );
        }

        customerLiability.setOutgoingDocumentFromExternalSystem(String.valueOf(action.getId()));
        customerLiability.setCustomerId(action.getCustomerId());

        customerLiability.setActionId(action.getId());
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setOccurrenceDate(LocalDate.now());
        if (getIfBlockedForCalculationOfLatePayment(action.getProductContractId())) {
            customerLiability.setBlockedForCalculationOfLatePayment(getIfBlockedForCalculationOfLatePayment(action.getProductContractId()));
            customerLiability.setBlockedForCalculationOfLatePaymentFromDate(LocalDate.now());
            customerLiability.setBlockedForCalculationOfLatePaymentBlockingReasonId(blockingReasonRepository.findByNameAndHardCodedTrue(
                    "От системата"));
        }
        return customerLiability;
    }

    /**
     * Maps a customer liability from a LatePaymentFine object.
     *
     * @param latePaymentFine the LatePaymentFine object to map from
     * @param errorMessages   a list to store any error messages encountered during the mapping
     * @return a CustomerLiability object populated with the data from the LatePaymentFine
     */
    public CustomerLiability mapLiabilityFromLatePaymentFine(LatePaymentFine latePaymentFine, List<String> errorMessages) {
        CustomerLiability customerLiability = new CustomerLiability();

        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository
                .findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period not found for current month;"));

        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerLiability.setDueDate(latePaymentFine.getDueDate());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFine.getAmount()));
        customerLiability.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFine.getAmountInOtherCcy()));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFine.getAmount()));
        customerLiability.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(latePaymentFine.getAmountInOtherCcy()));
        customerLiability.setCurrencyId(latePaymentFine.getCurrencyId());
        customerLiability.setOutgoingDocumentFromExternalSystem(latePaymentFine.getLatePaymentNumber());
        customerLiability.setIncomeAccountNumber(latePaymentFine.getIncomeAccountNumber());
        customerLiability.setCostCenterControllingOrder(latePaymentFine.getConstCentreControllingOrder());
        customerLiability.setCustomerId(latePaymentFine.getCustomerId());
        customerLiability.setOutgoingDocumentType(CustomerLiabilitiesOutgoingDocType.LATE_PAYMENT_FINE);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setLatePaymentFineId(latePaymentFine.getId());
        customerLiability.setAmountWithoutInterest(latePaymentFine.getAmount());
        if (latePaymentFine.getContractBillingGroupId() != null) {
            customerLiability.setContractBillingGroupId(latePaymentFine.getContractBillingGroupId());
        }
        customerLiability.setOccurrenceDate(latePaymentFine.getCreateDate()
                .toLocalDate());
        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LPF_LIABILITIES.name());
        if (incomeAccountNumber == null) {
            throw new DomainEntityNotFoundException("Default income account not found for receivables!;");
        }
        customerLiability.setIncomeAccountNumber(incomeAccountNumber);

        return customerLiability;
    }

    /**
     * Maps a customer liability from a Deposit object.
     *
     * @param deposit             the Deposit object to map from
     * @param isDepositWithdrawal if deposit covered some liability in manual liability offsetting
     * @param initialAmount       the initial amount to use for the customer liability
     * @return a CustomerLiability object populated with the data from the Deposit
     * @throws ClientException               if the current amount of the deposit is greater than or equal to the initial amount
     * @throws DomainEntityNotFoundException if the accounting period for the current month or the currency for the deposit is not found
     */
    public CustomerLiability mapLiabilityFromDeposit(
            Deposit deposit,
            Boolean isDepositWithdrawal,
            BigDecimal initialAmount,
            Long mloId
    ) throws ClientException, DomainEntityNotFoundException {
        CustomerLiability customerLiability = new CustomerLiability();
        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period not found for current month;"));
        DepositPaymentDeadlineAfterWithdrawal withdrawal = depositPaymentDeadlineAfterWithdrawalRepository
                .findByDepositIdAndStatusIn(deposit.getId(), List.of(EntityStatus.ACTIVE, EntityStatus.DELETED))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("PaymentDeadlineAfterWithdrawal not found with Deposit id: %s;".formatted(
                                deposit.getId())));

        Calendar calendar = calendarRepository.findById(withdrawal.getCalendarId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Calendar not found with id: %s;".formatted(
                                (withdrawal.getCalendarId()))));

        LocalDate dueDate = deposit.getPaymentDeadline();
        if (isDepositWithdrawal) {
            dueDate = getDueDate(deposit, withdrawal, calendar);
        }

        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerLiability.setDueDate(dueDate);
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmount));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmount));
        customerLiability.setCurrencyId(deposit.getCurrencyId());

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
            BigDecimal initialAmountInOtherCurrency = initialAmount.multiply(currency.getAltCurrencyExchangeRate());

            customerLiability.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmountInOtherCurrency));
            customerLiability.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmountInOtherCurrency));
        }

        String number = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_DEPOSIT.name());
        if (number == null) {
            throw new DomainEntityNotFoundException("Default income account not found for receivables!;");
        }
        customerLiability.setIncomeAccountNumber(number);


        customerLiability.setDepositId(deposit.getId());
        customerLiability.setOutgoingDocumentFromExternalSystem(deposit.getDepositNumber());
        customerLiability.setCustomerId(deposit.getCustomerId());
        customerLiability.setOutgoingDocumentType(CustomerLiabilitiesOutgoingDocType.DEPOSIT);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setOccurrenceDate(LocalDate.now());
        customerLiability.setManualLiabilityOffsettingId(mloId);
        return customerLiability;
    }

    /**
     * Calculates the due date for a deposit based on payment deadlines, holidays, and withdrawal settings.
     *
     * @param deposit    the deposit object containing payment deadline information
     * @param withdrawal the withdrawal object containing calendar type and other deadline adjustments
     * @param calendar   the calendar object used to retrieve holiday data and other configurations
     * @return the adjusted due date as a LocalDate object
     */
    private LocalDate getDueDate(Deposit deposit, DepositPaymentDeadlineAfterWithdrawal withdrawal, Calendar calendar) {
        LocalDate dueDate = deposit.getPaymentDeadline();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(
                calendar.getId(),
                List.of(HolidayStatus.ACTIVE)
        );

        dueDate = adjustDueDate(withdrawal.getCalendarType(), dueDate, holidays, calendar, withdrawal);

        return dueDate;
    }

    /**
     * Adjusts the due date based on the specified calendar type, holidays, and other payment deadline rules.
     *
     * @param calendarType Defines the type of calendar to be considered for adjusting the due date
     *                     (e.g., calendar days, working days, certain days).
     * @param dueDate      The initial due date to be adjusted.
     * @param holidays     A list of holiday dates to be considered for exclusion when adjusting the due date.
     * @param calendar     Provides information about weekends and other calendar-specific rules.
     * @param withdrawal   The withdrawal object that specifies details including the days to adjust,
     *                     exclusions (e.g., weekends, holidays), and the rule for changing the due date.
     * @return The adjusted due date after applying all specified rules and exclusions.
     */
    public LocalDate adjustDueDate(
            CalendarType calendarType, LocalDate dueDate,
            List<Holiday> holidays, Calendar calendar,
            DepositPaymentDeadlineAfterWithdrawal withdrawal
    ) {

        List<DueDateChange> dueDateChangeList = withdrawal.getDueDateChange();
        DueDateChange dueDateChange = CollectionUtils.isEmpty(dueDateChangeList) ? null : dueDateChangeList.get(0);

        List<DepositPaymentDeadlineExclude> deadlineExcludes = CollectionUtils
                .isEmpty(withdrawal.getDepositPaymentDeadlineExcludes()) ? new ArrayList<>() : withdrawal.getDepositPaymentDeadlineExcludes();

        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();

        Integer value = withdrawal.getValue();

        switch (calendarType) {
            case CALENDAR_DAYS -> {
                weekends = deadlineExcludes.contains(DepositPaymentDeadlineExclude.WEEKENDS) ? weekends : new ArrayList<>();
                holidays = deadlineExcludes.contains(DepositPaymentDeadlineExclude.HOLIDAYS) ? holidays : new ArrayList<>();
                dueDate = calculateDeadlineForCalendarDays(value, dueDate);
                dueDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(
                        dueDate,
                        Objects.nonNull(dueDateChange) ? dueDateChange.name() : null,
                        weekends,
                        holidays
                );
            }
            case WORKING_DAYS ->
                    dueDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(value, dueDate, weekends, holidays);
            case CERTAIN_DAYS -> {
                weekends = deadlineExcludes.contains(DepositPaymentDeadlineExclude.WEEKENDS) ? weekends : new ArrayList<>();
                holidays = deadlineExcludes.contains(DepositPaymentDeadlineExclude.HOLIDAYS) ? holidays : new ArrayList<>();

                LocalDate certainDay = LocalDate.of(dueDate.getYear(), dueDate.getMonthValue(), value);
                dueDate = certainDay.isBefore(dueDate) ? certainDay.plusMonths(1) : certainDay;
                dueDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(
                        dueDate,
                        Objects.nonNull(dueDateChange) ? dueDateChange.name() : null,
                        weekends,
                        holidays
                );
            }
        }
        return dueDate;
    }

    /**
     * Calculates the deadline date by adding the specified number of calendar days to the given end date.
     *
     * @param days    the number of calendar days to add to the end date.
     *                A positive value extends the deadline into the future, while a negative value sets it in the past.
     * @param endDate the starting date from which the days will be added.
     *                This date should not be null.
     * @return the calculated deadline date as a {@link LocalDate}.
     */
    private LocalDate calculateDeadlineForCalendarDays(Integer days, LocalDate endDate) {
        return endDate.plusDays(days);
    }

    /**
     * Maps a customer liability from a rescheduling installment.
     *
     * @param installment                  the rescheduling installment to map from
     * @param currencyId                   the ID of the currency for the customer liability
     * @param customerId                   the ID of the customer for the customer liability
     * @param reschedulingNumber           the number of the rescheduling for the customer liability
     * @param interestRateIdForInstallment the ID of the interest rate for the installment
     * @param directDebit                  whether the customer liability is set for direct debit
     * @param errorMessages                a list to store any error messages encountered during the mapping
     * @return a CustomerLiability object populated with the data from the rescheduling installment
     */
    public CustomerLiability mapLiabilityFromReschedulingInstallment(
            ReschedulingInstallment installment,
            Long currencyId,
            Long customerId,
            Long reschedulingId,
            String reschedulingNumber,
            Long interestRateIdForInstallment,
            Boolean directDebit,
            List<String> errorMessages
    ) {
        CustomerLiability customerLiability = new CustomerLiability();

        Long currentMonthsAccountingPeriodId = accountingPeriodsRepository
                .findCurrentMonthsAccountingPeriodId()
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period not found for current month;"));

        customerLiability.setLiabilityNumber("TEMPORARY_NUMBER");
        customerLiability.setAccountPeriodId(currentMonthsAccountingPeriodId);
        customerLiability.setDueDate(installment.getDueDate());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(installment.getInstallmentAmount()));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(installment.getInstallmentAmount()));
        customerLiability.setCurrencyId(currencyId);

        Currency currency = currencyRepository
                .findCurrencyByIdAndStatuses(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id: %s;".formatted(currencyId)));

        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();

        if (exchangeRate != null) {
            BigDecimal amountInOtherCurrency = installment.getInstallmentAmount()
                    .multiply(currency.getAltCurrencyExchangeRate());
            customerLiability.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(amountInOtherCurrency));
            customerLiability.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(amountInOtherCurrency));
        }

        customerLiability.setOutgoingDocumentFromExternalSystem(reschedulingNumber);
        customerLiability.setReschedulingId(reschedulingId);
        customerLiability.setApplicableInterestRateId(interestRateIdForInstallment);
        customerLiability.setDirectDebit(directDebit);
        customerLiability.setCustomerId(customerId);
        customerLiability.setOutgoingDocumentType(CustomerLiabilitiesOutgoingDocType.RESCHEDULING);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setOccurrenceDate(LocalDate.now());
        customerLiability.setAmountWithoutInterest(installment.getInterestAmount() == null ? BigDecimal.ZERO :
                installment.getInterestAmount()
                        .setScale(2, RoundingMode.HALF_UP)
        );
        customerLiability.setAmountWithoutInterestInOtherCurrency(BigDecimal.ZERO);
        String incomeAccountNameId = incomeAccountNameRepository.findByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
        if (incomeAccountNameId != null) {
            IncomeAccountName incomeAccountName = incomeAccountNameRepository
                    .findByName(incomeAccountNameId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Income account name not found with id: %s;".formatted(
                            incomeAccountNameId)));

            customerLiability.setIncomeAccountNumber(incomeAccountName.getNumber());
        }

        return customerLiability;
    }

    /**
     * Maps the parameters from the provided CustomerLiabilityRequest to the given CustomerLiability object.
     *
     * @param customerLiability the CustomerLiability object to update
     * @param request           the CustomerLiabilityRequest containing the new parameter values
     */
    public void mapParametersForUpdate(CustomerLiability customerLiability, CustomerLiabilityRequest request) {
        customerLiability.setDueDate(request.getDueDate());
        customerLiability.setAccountPeriodId(request.getAccountingPeriodId());
        customerLiability.setApplicableInterestRateId(request.getApplicableInterestRateId());
        customerLiability.setApplicableInterestRateDateFrom(request.getInterestDateFrom());
        customerLiability.setApplicableInterestRateDateTo(request.getInterestDateTo());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        customerLiability.setAmountWithoutInterest(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getAmountWithoutInterest() == null ? BigDecimal.ZERO : request.getAmountWithoutInterest()));
        customerLiability.setCurrencyId(request.getCurrencyId());
        customerLiability.setOutgoingDocumentFromExternalSystem(trim(request.getOutgoingDocumentFromExternalSystem()));
        customerLiability.setBasisForIssuing(trim(request.getBasisForIssuing()));
        customerLiability.setIncomeAccountNumber(trim(request.getNumberOfIncomeAccount()));
        customerLiability.setCostCenterControllingOrder(trim(request.getCostCenterControllingOrder()));
        customerLiability.setDirectDebit(request.isDirectDebit());
        customerLiability.setBankId(request.getBankId());
        customerLiability.setIban(request.getBankAccount());
        customerLiability.setAdditionalInfo(trim(request.getAdditionalLiabilityInformation()));
        customerLiability.setCustomerId(request.getCustomerId());
    }

    /**
     * Maps a CustomerLiabilityListingMiddleResponse to a CustomerLiabilityListingResponse.
     * This method is used to transform the middle-layer response object into a response object
     * that can be returned to the client.
     *
     * @param customerLiabilityListingMiddleResponse the middle-layer response object to be mapped
     * @return a CustomerLiabilityListingResponse containing the mapped data
     */
    public CustomerLiabilityListingResponse mapCustomerLiabilityListing(CustomerLiabilityListingMiddleResponse customerLiabilityListingMiddleResponse) {
        CustomerLiabilityListingResponse customerLiabilityListingResponse = new CustomerLiabilityListingResponse();
        customerLiabilityListingResponse.setCustomerId(customerLiabilityListingMiddleResponse.getCustomerId());
        customerLiabilityListingResponse.setCustomer(customerLiabilityListingMiddleResponse.getCustomer());
        customerLiabilityListingResponse.setLiabilityNumber(customerLiabilityListingMiddleResponse.getLiabilityNumber());
        customerLiabilityListingResponse.setId(customerLiabilityListingMiddleResponse.getId());
        customerLiabilityListingResponse.setStatus(customerLiabilityListingMiddleResponse.getStatus());
        customerLiabilityListingResponse.setCurrencyId(customerLiabilityListingMiddleResponse.getCurrencyId());
        customerLiabilityListingResponse.setBillingGroup(customerLiabilityListingMiddleResponse.getBillingGroup());
        customerLiabilityListingResponse.setAlternativeRecipientOfAnInvoice(customerLiabilityListingMiddleResponse.getAlternativeRecipientOfAnInvoice());
        customerLiabilityListingResponse.setCurrentAmount(customerLiabilityListingMiddleResponse.getCurrentAmount());
        customerLiabilityListingResponse.setInitialAmount(customerLiabilityListingMiddleResponse.getInitialAmount());
        customerLiabilityListingResponse.setCreationType(customerLiabilityListingMiddleResponse.getCreationType());
        customerLiabilityListingResponse.setAccountingPeriodStatus(customerLiabilityListingMiddleResponse.getAccountingPeriodStatus());
        customerLiabilityListingResponse.setCurrencyName(customerLiabilityListingMiddleResponse.getCurrencyName());
        customerLiabilityListingResponse.setOccurrenceDate(customerLiabilityListingMiddleResponse.getOccurrenceDate());
        customerLiabilityListingResponse.setDueDate(customerLiabilityListingMiddleResponse.getDueDate());
        customerLiabilityListingResponse.setPods(splitPodData(customerLiabilityListingMiddleResponse.getPods()));

        return customerLiabilityListingResponse;
    }

    /**
     * Splits the input pod data string into a list of CustomerLiabilityPodResponse objects
     * based on a predefined delimiter.
     *
     * @param podData the input string containing pod data separated by a specific delimiter
     * @return a list of CustomerLiabilityPodResponse objects created from the split pod data
     */
    private List<CustomerLiabilityPodResponse> splitPodData(String podData) {
        if (StringUtils.isBlank(podData)) {
            return new ArrayList<>();
        }
        return Arrays.stream(podData.split(POD_DATA_ROW_DELIMITER))
                .map(CustomerLiabilityPodResponse::new)
                .toList();
    }

    /**
     * Trims the given string value if it is not null, otherwise returns null.
     *
     * @param value the string value to be trimmed
     * @return the trimmed string value, or null if the input value was null
     */
    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    /**
     * Retrieves a short response containing the details of an interest rate with the given ID.
     *
     * @param interestRateId the ID of the interest rate to retrieve details for
     * @return a InterestRateShortResponse containing the interest rate's details
     * @throws DomainEntityNotFoundException if the interest rate with the given ID is not found
     */
    private InterestRateShortResponse getInterestRate(Long interestRateId) {
        return new InterestRateShortResponse(
                interestRateRepository.findById(interestRateId)
                        .orElseThrow(() ->
                                new DomainEntityNotFoundException(
                                        "Interest rate with given id: %s not found".formatted(interestRateId))
                        )
        );
    }

    /**
     * Retrieves a short response containing the details of a currency with the given ID.
     *
     * @param currencyId the ID of the currency to retrieve details for
     * @return a CurrencyShortResponse containing the currency's details
     * @throws DomainEntityNotFoundException if the currency with the given ID is not found
     */
    private CurrencyShortResponse getCurrencyResponse(Long currencyId) {
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Currency not found with given id: %s".formatted(currencyId)));
        return new CurrencyShortResponse(currency);
    }

    /**
     * Retrieves a short response containing the details of a bank with the given ID.
     *
     * @param bankId the ID of the bank to retrieve details for
     * @return a BankResponse containing the bank's details
     * @throws DomainEntityNotFoundException if the bank with the given ID is not found
     */
    private BankResponse getBank(Long bankId) {
        return new BankResponse(
                bankRepository.findById(bankId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Bank with given id: %s not found;".formatted(bankId)))
        );
    }

    /**
     * Retrieves a short response containing the details of a blocking reason with the given ID.
     *
     * @param reasonId the ID of the blocking reason to retrieve details for
     * @return a ShortResponse containing the blocking reason's ID and name, or null if the blocking reason is not found
     * @throws DomainEntityNotFoundException if the blocking reason with the given ID is not found
     */
    private ShortResponse getBlockedReasonResponse(Long reasonId) {
        if (reasonId != null) {
            BlockingReason blockingReason = blockingReasonRepository.findById(reasonId)
                    .orElseThrow(() -> new DomainEntityNotFoundException(
                            "Blocking reason not found with given id: %s".formatted(
                                    reasonId)));
            return new ShortResponse(blockingReason.getId(), blockingReason.getName());
        }

        return null;
    }

    /**
     * Retrieves a short response containing the customer details for the given customer ID.
     *
     * @param customerId the ID of the customer to retrieve details for
     * @return a CustomerDetailsShortResponse containing the customer's details
     * @throws DomainEntityNotFoundException if the customer or customer details with the given ID are not found
     */
    private CustomerDetailsShortResponse getCustomerResponse(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Customer not found with given id: %s".formatted(customerId)));

        CustomerDetails customerDetails = getCustomerDetails(customer.getLastCustomerDetailId());

        return new CustomerDetailsShortResponse(
                customer.getId(),
                customerDetails.getId(),
                getCustomerName(customer, customerDetails),
                customer.getCustomerType(),
                customerDetails.getBusinessActivity()
        );
    }

    /**
     * Retrieves the customer details for the given customer detail ID.
     *
     * @param customerDetailId the ID of the customer details to retrieve
     * @return the customer details for the given ID
     * @throws DomainEntityNotFoundException if the customer details with the given ID are not found
     */
    private CustomerDetails getCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository.findById(customerDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("CustomerDetails not found with id:%s".formatted(
                        customerDetailId)));
    }

    /**
     * Retrieves the customer's full name, including the legal form name if available.
     *
     * @param customer        the customer object
     * @param customerDetails the customer details object
     * @return the customer's full name in the format "Identifier (Name Middle_Name Last_Name [Legal_Form_Name])"
     */
    private String getCustomerName(Customer customer, CustomerDetails customerDetails) {
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        return String.format(
                "%s (%s%s%s%s)", customer.getIdentifier(), customerDetails.getName(),
                customerDetails.getMiddleName() != null ? " " + customerDetails.getMiddleName() : "",
                customerDetails.getLastName() != null ? " " + customerDetails.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? " " + legalFormName : ""
        );
    }

    /**
     * Retrieves a short response containing the billing group details for the given billing group ID.
     *
     * @param billingGroupId the ID of the billing group to retrieve details for
     * @return a BillingGroupListingResponse containing the billing group's details
     * @throws DomainEntityNotFoundException if the billing group with the given ID is not found
     */
    private BillingGroupListingResponse getBillingGroupResponse(Long billingGroupId) {
        ContractBillingGroup billingGroup = billingGroupRepository.findById(billingGroupId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Billing group not found with id: %s".formatted(
                                billingGroupId)));

        return new BillingGroupListingResponse(billingGroupId, billingGroup.getGroupNumber());
    }

    /**
     * Retrieves a short response containing the invoice details for the given invoice ID.
     *
     * @param invoiceId the ID of the invoice to retrieve details for
     * @return an InvoiceShortResponse containing the invoice's details
     * @throws DomainEntityNotFoundException if the invoice with the given ID is not found
     */
    private InvoiceShortResponse getInvoiceResponse(Long invoiceId) {
        Invoice invoice = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with id: %s".formatted(invoiceId)));

        String invoiceNumber;
        if (InvoiceDocumentType.DEBIT_NOTE.equals(invoice.getInvoiceDocumentType())) {
            invoiceNumber = "Debit Note-".concat(invoice.getInvoiceNumber());
        } else {
            invoiceNumber = "Invoice-".concat(invoice.getInvoiceNumber());
        }

        return new InvoiceShortResponse(invoiceId, invoiceNumber);
    }

    /**
     * Retrieves a short response containing the deposit details for the given deposit ID.
     *
     * @param depositId the ID of the deposit to retrieve details for
     * @return a ShortResponse containing the deposit's details
     * @throws DomainEntityNotFoundException if the deposit with the given ID is not found
     */
    private ShortResponse getDepositResponse(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Deposit not found with id: %s".formatted(
                        depositId)));

        return new ShortResponse(depositId, deposit.getDepositNumber());
    }

    /**
     * Retrieves a short response containing the action details for the given action ID.
     *
     * @param actionId the ID of the action to retrieve details for
     * @return a ShortResponse containing the action's details
     */
    private ShortResponse getActionResponse(Long actionId) {
        return new ShortResponse(actionId, "Action-".concat(String.valueOf(actionId)));
    }

    /**
     * Retrieves a short response containing the rescheduling details for the given rescheduling name.
     *
     * @param reschedulingName the name of the rescheduling to retrieve details for
     * @return a ShortResponse containing the rescheduling's details
     * @throws DomainEntityNotFoundException if the rescheduling with the given name is not found
     */
    private ShortResponse getReschedulingResponse(String reschedulingName) {
        Long reschedulingId = Long.parseLong(reschedulingName.split("-")[1]);
        Rescheduling rescheduling = reschedulingRepository.findById(reschedulingId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Rescheduling not found with id: %s".formatted(reschedulingId)));

        return new ShortResponse(reschedulingId, rescheduling.getReschedulingNumber());
    }

    /**
     * Checks the currency ID and sets the initial and current amounts in the other currency on the customer liability object.
     * If the currency is not found, an error message is added to the error messages list.
     *
     * @param currencyId        the ID of the currency to use for the conversion
     * @param initialAmount     the initial amount to convert
     * @param currentAmount     the current amount to convert
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if the currency is not found
     */
    public void checkCurrencyAndSetAmounts(
            Long currencyId,
            BigDecimal initialAmount,
            BigDecimal currentAmount,
            CustomerLiability customerLiability,
            List<String> errorMessages
    ) {
        Optional<Currency> currency = currencyRepository.findByIdAndStatus(
                currencyId,
                List.of(
                        NomenclatureItemStatus.ACTIVE,
                        NomenclatureItemStatus.INACTIVE
                )
        );
        if (currency.isEmpty()) {
            errorMessages.add("currencyId-[currencyId] currency not found;");
        } else {
            BigDecimal exchangeRate = currency.get()
                    .getAltCurrencyExchangeRate();

            if (exchangeRate != null) {
                BigDecimal initialAmountInOtherCurrency = initialAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal currentAmountInOtherCurrency = currentAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

                customerLiability.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmountInOtherCurrency));
                customerLiability.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(currentAmountInOtherCurrency));
            }
        }
    }

    /**
     * Checks if the customer liability is blocked for payment and sets the corresponding fields on the customer liability object.
     * If the customer liability is blocked for payment, the method checks if the user has the necessary permission and validates the blocking reason.
     * If the customer liability is not blocked for payment, the method resets the corresponding fields on the customer liability object.
     *
     * @param request           the customer liability request containing the blocking information
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if there are any issues
     * @param permissions       the set of permissions the user has
     */
    public void checkAndSetBlockedForPaymentParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, PermissionEnum permissionToCheck
    ) {
        if (request.isBlockedForPayment()) {
            checkPermission(permissionToCheck, permissions);
            checkBlockingReason(request.getBlockedForPaymentReasonId(), "blockedForPaymentReasonId", errorMessages);
            customerLiability.setBlockedForPayment(true);
            customerLiability.setBlockedForPaymentFromDate(request.getBlockedForPaymentFromDate());
            customerLiability.setBlockedForPaymentToDate(request.getBlockedForPaymentToDate());
            customerLiability.setBlockedForPaymentBlockingReasonId(request.getBlockedForPaymentReasonId());
            customerLiability.setBlockedForPaymentAdditionalInfo(trim(request.getBlockedForPaymentAdditionalInfo()));
        } else {
            customerLiability.setBlockedForPayment(false);
            customerLiability.setBlockedForPaymentFromDate(null);
            customerLiability.setBlockedForPaymentToDate(null);
            customerLiability.setBlockedForPaymentBlockingReasonId(null);
            customerLiability.setBlockedForPaymentAdditionalInfo(null);
        }
    }

    /**
     * Checks if the customer liability is blocked for reminder letters and sets the corresponding fields on the customer liability object.
     * If the customer liability is blocked for reminder letters, the method checks if the user has the necessary permission and validates the blocking reason.
     * If the customer liability is not blocked for reminder letters, the method resets the corresponding fields on the customer liability object.
     *
     * @param request           the customer liability request containing the blocking information
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if there are any issues
     * @param permissions       the set of permissions the user has
     */
    public void checkAndSetBlockedForReminderLettersParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, PermissionEnum permissionToCheck
    ) {
        if (request.isBlockedForReminderLetters()) {
            checkPermission(permissionToCheck, permissions);
            checkBlockingReason(request.getBlockedForReminderLettersReasonId(), "blockedForReminderLettersReasonId", errorMessages);
            customerLiability.setBlockedForReminderLetters(true);
            customerLiability.setBlockedForReminderLettersFromDate(request.getBlockedForReminderLettersFromDate());
            customerLiability.setBlockedForReminderLettersToDate(request.getBlockedForReminderLettersToDate());
            customerLiability.setBlockedForReminderLettersBlockingReasonId(request.getBlockedForReminderLettersReasonId());
            customerLiability.setBlockedForReminderLettersAdditionalInfo(trim(request.getBlockedForReminderLettersAdditionalInfo()));
        } else {
            customerLiability.setBlockedForReminderLetters(false);
            customerLiability.setBlockedForReminderLettersFromDate(null);
            customerLiability.setBlockedForReminderLettersToDate(null);
            customerLiability.setBlockedForReminderLettersBlockingReasonId(null);
            customerLiability.setBlockedForReminderLettersAdditionalInfo(null);
        }
    }

    /**
     * Checks if the customer liability is blocked for calculation of late payment and sets the corresponding fields on the customer liability object.
     * If the customer liability is blocked for calculation of late payment, the method checks if the user has the necessary permission and validates the blocking reason.
     * If the customer liability is not blocked for calculation of late payment, the method resets the corresponding fields on the customer liability object.
     *
     * @param request           the customer liability request containing the blocking information
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if there are any issues
     * @param permissions       the set of permissions the user has
     */
    public void checkAndSetBlockedForCalculationOfLatePaymentParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, PermissionEnum permissionToCheck
    ) {
        if (request.isBlockedForCalculationOfLatePayment()) {
            checkPermission(permissionToCheck, permissions);
            checkBlockingReason(
                    request.getBlockedForCalculationOfLatePaymentReasonId(),
                    "blockedForCalculationOfLatePaymentReasonId",
                    errorMessages
            );
            customerLiability.setBlockedForCalculationOfLatePayment(true);
            customerLiability.setBlockedForCalculationOfLatePaymentFromDate(request.getBlockedForCalculationOfLatePaymentFromDate());
            customerLiability.setBlockedForCalculationOfLatePaymentToDate(request.getBlockedForCalculationOfLatePaymentToDate());
            customerLiability.setBlockedForCalculationOfLatePaymentBlockingReasonId(request.getBlockedForCalculationOfLatePaymentReasonId());
            customerLiability.setBlockedForCalculationOfLatePaymentAdditionalInfo(trim(request.getBlockedForCalculationOfLatePaymentAdditionalInfo()));
        } else {
            customerLiability.setBlockedForCalculationOfLatePayment(false);
            customerLiability.setBlockedForCalculationOfLatePaymentFromDate(null);
            customerLiability.setBlockedForCalculationOfLatePaymentToDate(null);
            customerLiability.setBlockedForCalculationOfLatePaymentBlockingReasonId(null);
            customerLiability.setBlockedForCalculationOfLatePaymentAdditionalInfo(null);
        }
    }

    /**
     * Checks if the customer liability is blocked for liabilities offsetting and sets the corresponding fields on the customer liability object.
     * If the customer liability is blocked for liabilities offsetting, the method checks if the user has the necessary permission and validates the blocking reason.
     * If the customer liability is not blocked for liabilities offsetting, the method resets the corresponding fields on the customer liability object.
     *
     * @param request           the customer liability request containing the blocking information
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if there are any issues
     * @param permissions       the set of permissions the user has
     */
    public void checkAndSetBlockedForLiabilitiesOffsettingParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, PermissionEnum permissionToCheck
    ) {
        if (request.isBlockedForLiabilitiesOffsetting()) {
            checkPermission(permissionToCheck, permissions);
            checkBlockingReason(
                    request.getBlockedForLiabilitiesOffsettingReasonId(),
                    "blockedForLiabilitiesOffsettingReasonId",
                    errorMessages
            );
            customerLiability.setBlockedForLiabilitiesOffsetting(true);
            customerLiability.setBlockedForLiabilitiesOffsettingFromDate(request.getBlockedForLiabilitiesOffsettingFromDate());
            customerLiability.setBlockedForLiabilitiesOffsettingToDate(request.getBlockedForLiabilitiesOffsettingToDate());
            customerLiability.setBlockedForLiabilitiesOffsettingBlockingReasonId(request.getBlockedForLiabilitiesOffsettingReasonId());
            customerLiability.setBlockedForLiabilitiesOffsettingAdditionalInfo(trim(request.getBlockedForLiabilitiesOffsettingAdditionalInfo()));
        } else {
            customerLiability.setBlockedForLiabilitiesOffsetting(false);
            customerLiability.setBlockedForLiabilitiesOffsettingFromDate(null);
            customerLiability.setBlockedForLiabilitiesOffsettingToDate(null);
            customerLiability.setBlockedForLiabilitiesOffsettingBlockingReasonId(null);
            customerLiability.setBlockedForLiabilitiesOffsettingAdditionalInfo(null);
        }
    }

    /**
     * Checks if the customer liability is blocked for supply termination and sets the corresponding fields on the customer liability object.
     * If the customer liability is blocked for supply termination, the method checks if the user has the necessary permission and validates the blocking reason.
     * If the customer liability is not blocked for supply termination, the method resets the corresponding fields on the customer liability object.
     *
     * @param request           the customer liability request containing the blocking information
     * @param customerLiability the customer liability object to update
     * @param errorMessages     the list of error messages to add to if there are any issues
     * @param permissions       the set of permissions the user has
     */
    public void checkAndSetBlockedForSupplyTerminationParameters(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions, PermissionEnum permissionToCheck
    ) {
        if (request.isBlockedForSupplyTermination()) {
            checkPermission(permissionToCheck, permissions);
            checkBlockingReason(
                    request.getBlockedForSupplyTerminationReasonId(),
                    "blockedForSupplyTerminationReasonId",
                    errorMessages
            );
            customerLiability.setBlockedForSupplyTermination(true);
            customerLiability.setBlockedForSupplyTerminationFromDate(request.getBlockedForSupplyTerminationFromDate());
            customerLiability.setBlockedForSupplyTerminationToDate(request.getBlockedForSupplyTerminationToDate());
            customerLiability.setBlockedForSupplyTerminationBlockingReasonId(request.getBlockedForSupplyTerminationReasonId());
            customerLiability.setBlockedForSupplyTerminationAdditionalInfo(trim(request.getBlockedForSupplyTerminationAdditionalInfo()));
        } else {
            customerLiability.setBlockedForSupplyTermination(false);
            customerLiability.setBlockedForSupplyTerminationFromDate(null);
            customerLiability.setBlockedForSupplyTerminationToDate(null);
            customerLiability.setBlockedForSupplyTerminationBlockingReasonId(null);
            customerLiability.setBlockedForSupplyTerminationAdditionalInfo(null);
        }
    }

    /**
     * Checks if the provided blocking reason ID is valid and exists in the system.
     * If the blocking reason is not found, an error message is added to the provided error list.
     *
     * @param reasonId      the blocking reason ID to check
     * @param field         the name of the field associated with the blocking reason
     * @param errorMessages the list of error messages to add to if the blocking reason is not found
     */
    private void checkBlockingReason(Long reasonId, String field, List<String> errorMessages) {
        if (reasonId != null && !blockingReasonRepository.existsByIdAndStatusIn(
                reasonId,
                List.of(
                        NomenclatureItemStatus.ACTIVE,
                        NomenclatureItemStatus.INACTIVE
                )
        )) {
            errorMessages.add(field + "-[" + field + "] blocking reason not found;");
        }
    }

    /**
     * Checks if the provided billing group ID is valid and sets it on the customer liability.
     * If the billing group is not found, an error message is added to the provided error list.
     *
     * @param request           the customer liability request containing the billing group ID
     * @param customerLiability the customer liability to update with the billing group
     * @param errorMessages     the list of error messages to add to if the billing group is not found
     */
    public void checkAndSetBillingGroup(
            CustomerLiabilityRequest request,
            CustomerLiability customerLiability,
            List<String> errorMessages
    ) {
        if (request.getBillingGroupId() != null) {
            if (billingGroupRepository.existsById(request.getBillingGroupId())) {
                customerLiability.setContractBillingGroupId(request.getBillingGroupId());
            } else {
                errorMessages.add("billingGroupId-[billingGroupId] billing group not found;");
            }
        } else {
            customerLiability.setContractBillingGroupId(null);
        }
    }

    /**
     * Checks if the provided alternative invoice recipient customer ID is valid and sets it on the customer liability.
     * If the alternative recipient customer is not found, an error message is added to the provided error list.
     *
     * @param request           the customer liability request containing the alternative invoice recipient customer ID
     * @param customerLiability the customer liability to update with the alternative invoice recipient customer
     * @param errorMessages     the list of error messages to add to if the alternative recipient customer is not found
     */
    public void checkAndSetAlternativeInvoiceRecipientCustomer(
            CustomerLiabilityRequest request,
            CustomerLiability customerLiability,
            List<String> errorMessages
    ) {
        if (request.getAlternativeInvoiceRecipientCustomerId() != null) {
            if (customerRepository.existsByIdAndStatusIn(
                    request.getAlternativeInvoiceRecipientCustomerId(),
                    List.of(CustomerStatus.ACTIVE)
            )) {
                customerLiability.setAltInvoiceRecipientCustomerId(request.getAlternativeInvoiceRecipientCustomerId());
            } else {
                errorMessages.add("alternativeRecipientOfInvoiceId-[alternativeRecipientOfInvoiceId] alternative recipient not found;");
            }
        }
    }

    /**
     * Checks if the current user has the specified permission. If the user does not have the permission, a {@link ClientException} is thrown with an appropriate error message.
     *
     * @param permission  the permission to check
     * @param permissions the set of permissions the user has
     * @throws ClientException if the user does not have the specified permission
     */
    private void checkPermission(PermissionEnum permission, Set<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (!permissionService.getPermissionsFromContext(CUSTOMER_LIABILITY)
                    .contains(permission.getId())) {
                throw new ClientException(
                        "You don't have appropriate permission: %s;".formatted(permission.name()),
                        ErrorCode.OPERATION_NOT_ALLOWED
                );
            }
        } else if (!permissions.contains(permission.getId())) {
            throw new ClientException(
                    "You don't have the appropriate permission: %s".formatted(permission.name()),
                    ErrorCode.OPERATION_NOT_ALLOWED
            );
        }
    }

    /**
     * Retrieves an {@link AccountingPeriods} entity by the given ID.
     *
     * @param accountPeriodId the ID of the accounting period to retrieve
     * @return the {@link AccountingPeriods} entity with the given ID
     * @throws DomainEntityNotFoundException if the accounting period with the given ID is not found
     */
    private AccountingPeriods getAccountingPeriod(Long accountPeriodId) {
        return accountingPeriodsRepository.findById(accountPeriodId)
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Accounting period not found with given id: %s".formatted(accountPeriodId)));
    }

//    /**
//     * Retrieves a list of {@link CustomerOffsettingResponse} objects representing the customer liability offsetting information for the given customer liability ID.
//     * The method fetches the customer liability offsetting data from the corresponding repositories and maps it to the response objects.
//     *
//     * @param liabilityId the ID of the customer liability for which to retrieve the offsetting information
//     * @return a list of {@link CustomerOffsettingResponse} objects representing the customer liability offsetting information
//     */
//    public List<CustomerOffsettingResponse> getCustomerLiabilityOffsettingResponseList(Long liabilityId) {
//
//        List<CustomerLiabilityPaidByDeposit> paidByDepositList = customerLiabilityPaidByDepositRepository.findByCustomerLiabilityIdAndStatus(
//                liabilityId,
//                EntityStatus.ACTIVE
//        );
//        List<CustomerOffsettingResponse> responseList = new ArrayList<>(mapToCustomerLiabilityOffsettingResponse(paidByDepositList));
//
//        List<CustomerLiabilityPaidByPayment> paidByPaymentList = customerLiabilityPaidByPaymentRepository.findByCustomerLiabilityIdAndStatus(
//                liabilityId,
//                EntityStatus.ACTIVE
//        );
//        responseList.addAll(mapToCustomerLiabilityOffsettingResponse(paidByPaymentList));
//
//        List<CustomerLiabilityPaidByReceivable> paidByReceivableList = customerLiabilityPaidByReceivableRepository.findByCustomerLiabilityIdAndStatus(
//                liabilityId,
//                EntityStatus.ACTIVE
//        );
//        responseList.addAll(mapToCustomerLiabilityOffsettingResponse(paidByReceivableList));
//
//        List<CustomerLiabilityPaidByRescheduling> paidByReschedulingList = customerLiabilityPaidByReschedulingRepository.findByCustomerLiabilityIdAndStatus(
//                liabilityId,
//                EntityStatus.ACTIVE
//        );
//        responseList.addAll(mapToCustomerLiabilityOffsettingResponse(paidByReschedulingList));
//
//        return responseList;
//    }

    /**
     * Retrieves a list of customer liability offsetting responses for a given liability ID.
     *
     * @param liabilityId the unique identifier of the liability for which offsetting responses are to be retrieved
     * @return a list of CustomerOffsettingResponse objects representing the offsetting details for the specified liability ID
     */
    public List<CustomerOffsettingResponse> getCustomerLiabilityOffsettingResponseList(Long liabilityId) {
        return objectOffsettingService
                .fetchObjectOffsettings(ObjectOffsettingType.LIABILITY, liabilityId)
                .stream()
                .map(CustomerOffsettingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Maps a list of objects representing customer liability payments or offsets to a list of {@link CustomerOffsettingResponse} objects.
     *
     * @param sourceList the list of objects to be mapped, which can be instances of {@link CustomerLiabilityPaidByDeposit},
     *                   {@link CustomerLiabilityPaidByPayment}, or {@link CustomerLiabilityPaidByReceivable}
     * @return a list of {@link CustomerOffsettingResponse} objects with the mapped data
     */
    private List<CustomerOffsettingResponse> mapToCustomerLiabilityOffsettingResponse(List<?> sourceList) {
        return sourceList.stream()
                .map(this::mapToCustomerLiabilityOffsettingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps an object representing a customer liability payment or offset to a {@link CustomerOffsettingResponse} object.
     *
     * @param source the object to be mapped, which can be an instance of {@link CustomerLiabilityPaidByDeposit},
     *               {@link CustomerLiabilityPaidByPayment}, or {@link CustomerLiabilityPaidByReceivable}
     * @return a {@link CustomerOffsettingResponse} object with the mapped data
     */
    private CustomerOffsettingResponse mapToCustomerLiabilityOffsettingResponse(Object source) {
        CustomerOffsettingResponse response = new CustomerOffsettingResponse();

        if (source instanceof CustomerLiabilityPaidByDeposit paidByDeposit) {
            response.setId(paidByDeposit.getCustomerDepositId());
            response.setAmount(paidByDeposit.getAmount());
            response.setCurrencyResponse(getCurrencyResponse(paidByDeposit.getCurrencyId()));
            response.setDate(paidByDeposit.getCreateDate()
                    .toLocalDate());
            response.setOffsettingObject(OffsettingObject.DEPOSIT);
        } else if (source instanceof CustomerLiabilityPaidByPayment paidByPayment) {
            response.setId(paidByPayment.getCustomerPaymentId());
            response.setAmount(paidByPayment.getAmount());
            response.setCurrencyResponse(getCurrencyResponse(paidByPayment.getCurrencyId()));
            response.setDate(paidByPayment.getCreateDate()
                    .toLocalDate());
            response.setOffsettingObject(OffsettingObject.PAYMENT);
        } else if (source instanceof CustomerLiabilityPaidByReceivable paidByReceivable) {
            response.setId(paidByReceivable.getCustomerReceivableId());
            response.setAmount(paidByReceivable.getAmount());
            response.setCurrencyResponse(getCurrencyResponse(paidByReceivable.getCurrencyId()));
            response.setDate(paidByReceivable.getCreateDate()
                    .toLocalDate());
            response.setOffsettingObject(OffsettingObject.RECEIVABLE);
        } else if (source instanceof CustomerLiabilityPaidByRescheduling paidByRescheduling) {
            response.setId(paidByRescheduling.getCustomerReschedulingId());
            response.setAmount(paidByRescheduling.getAmount());
            response.setCurrencyResponse(getCurrencyResponse(paidByRescheduling.getCurrencyId()));
            response.setDate(paidByRescheduling.getCreateDate()
                    .toLocalDate());
            response.setOffsettingObject(OffsettingObject.RESCHEDULING);
        }

        return response;
    }

    /**
     * Checks and sets the blocked fields for a customer liability based on the provided request and the existing customer liability.
     * The method checks if the blocked fields have changed between the request and the existing customer liability, and if the user has the necessary permissions,
     * it calls the corresponding methods to check and set the blocked fields.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @param errorMessages     a list to store any error messages
     * @param permissions       a set of permissions the user has
     */
    public void checkAndSetBlockedFields(
            CustomerLiabilityRequest request, CustomerLiability customerLiability,
            List<String> errorMessages, Set<String> permissions
    ) {
        if (hasBlockedForPaymentChanged(request, customerLiability)) {
            if (hasPermission(PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT, permissions)) {
                checkAndSetBlockedForPaymentParameters(request, customerLiability, errorMessages, permissions,
                        PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_PAYMENT);
            }
        }

        if (hasBlockedForReminderLettersChanged(request, customerLiability)) {
            if (hasPermission(PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS, permissions)) {
                checkAndSetBlockedForReminderLettersParameters(request, customerLiability, errorMessages, permissions,
                        PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_REMINDER_LETTERS);
            }
        }

        if (hasBlockedForCalculationOfLatePaymentChanged(request, customerLiability)) {
            if (hasPermission(PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS, permissions)) {
                checkAndSetBlockedForCalculationOfLatePaymentParameters(request, customerLiability, errorMessages, permissions,
                        PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_CALCULATION_OF_LATE_PAYMENT_FINES_INTERESTS);
            }
        }

        if (hasBlockedForLiabilitiesOffsettingChanged(request, customerLiability)) {
            if (hasPermission(PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING, permissions)) {
                checkAndSetBlockedForLiabilitiesOffsettingParameters(request, customerLiability, errorMessages, permissions,
                        PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_LIABILITIES_OFFSETTING);
            }
        }

        if (hasBlockedForSupplyTerminationChanged(request, customerLiability)) {
            if (hasPermission(PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION, permissions)) {
                checkAndSetBlockedForSupplyTerminationParameters(request, customerLiability, errorMessages, permissions,
                        PermissionEnum.CUSTOMER_LIABILITY_EDIT_BLOCKED_FOR_SUPPLY_TERMINATION);
            }
        }
    }

    /**
     * Checks if the blocked for payment parameters have changed between the request and the existing customer liability.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @return true if any of the blocked for payment parameters have changed, false otherwise
     */
    public boolean hasBlockedForPaymentChanged(CustomerLiabilityRequest request, CustomerLiability customerLiability) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(customerLiability.getBlockedForPayment());
        if (currentBlockedStatus && !request.isBlockedForPayment()) {
            return true;
        }

        boolean fieldsPresent = request.getBlockedForPaymentReasonId() != null ||
                request.getBlockedForPaymentFromDate() != null ||
                request.getBlockedForPaymentToDate() != null ||
                request.getBlockedForPaymentAdditionalInfo() != null;

        if (!fieldsPresent) {
            return false;
        }

        if (request.isBlockedForPayment() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForPayment()) {
            if (request.getBlockedForPaymentReasonId() != null &&
                    !Objects.equals(request.getBlockedForPaymentReasonId(),
                            customerLiability.getBlockedForPaymentBlockingReasonId())) {
                return true;
            }

            if (request.getBlockedForPaymentFromDate() != null &&
                    !Objects.equals(request.getBlockedForPaymentFromDate(),
                            customerLiability.getBlockedForPaymentFromDate())) {
                return true;
            }

            if (request.getBlockedForPaymentToDate() != null &&
                    !Objects.equals(request.getBlockedForPaymentToDate(),
                            customerLiability.getBlockedForPaymentToDate())) {
                return true;
            }

            return request.getBlockedForPaymentAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForPaymentAdditionalInfo(),
                            customerLiability.getBlockedForPaymentAdditionalInfo());
        }

        return false;
    }

    /**
     * Checks if the blocked for reminder letters parameters have changed between the request and the existing customer liability.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @return true if any of the blocked for reminder letters parameters have changed, false otherwise
     */
    public boolean hasBlockedForReminderLettersChanged(CustomerLiabilityRequest request, CustomerLiability customerLiability) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(customerLiability.getBlockedForReminderLetters());
        if (currentBlockedStatus && !request.isBlockedForReminderLetters()) {
            return true;
        }

        boolean fieldsPresent = request.getBlockedForReminderLettersReasonId() != null ||
                request.getBlockedForReminderLettersFromDate() != null ||
                request.getBlockedForReminderLettersToDate() != null ||
                request.getBlockedForReminderLettersAdditionalInfo() != null;

        if (!fieldsPresent) {
            return false;
        }

        if (request.isBlockedForReminderLetters() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForReminderLetters()) {
            if (request.getBlockedForReminderLettersReasonId() != null &&
                    !Objects.equals(request.getBlockedForReminderLettersReasonId(),
                            customerLiability.getBlockedForReminderLettersBlockingReasonId())) {
                return true;
            }

            if (request.getBlockedForReminderLettersFromDate() != null &&
                    !Objects.equals(request.getBlockedForReminderLettersFromDate(),
                            customerLiability.getBlockedForReminderLettersFromDate())) {
                return true;
            }

            if (request.getBlockedForReminderLettersToDate() != null &&
                    !Objects.equals(request.getBlockedForReminderLettersToDate(),
                            customerLiability.getBlockedForReminderLettersToDate())) {
                return true;
            }

            return request.getBlockedForReminderLettersAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForReminderLettersAdditionalInfo(),
                            customerLiability.getBlockedForReminderLettersAdditionalInfo());
        }

        return false;
    }

    /**
     * Checks if the blocked for calculation of late payment parameters have changed between the request and the existing customer liability.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @return true if any of the blocked for calculation of late payment parameters have changed, false otherwise
     */
    public boolean hasBlockedForCalculationOfLatePaymentChanged(
            CustomerLiabilityRequest request,
            CustomerLiability customerLiability
    ) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(customerLiability.getBlockedForCalculationOfLatePayment());
        if (currentBlockedStatus && !request.isBlockedForCalculationOfLatePayment()) {
            return true;
        }

        boolean fieldsPresent = request.getBlockedForCalculationOfLatePaymentReasonId() != null ||
                request.getBlockedForCalculationOfLatePaymentFromDate() != null ||
                request.getBlockedForCalculationOfLatePaymentToDate() != null ||
                request.getBlockedForCalculationOfLatePaymentAdditionalInfo() != null;

        if (!fieldsPresent) {
            return false;
        }

        if (request.isBlockedForCalculationOfLatePayment() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForCalculationOfLatePayment()) {
            if (request.getBlockedForCalculationOfLatePaymentReasonId() != null &&
                    !Objects.equals(request.getBlockedForCalculationOfLatePaymentReasonId(),
                            customerLiability.getBlockedForCalculationOfLatePaymentBlockingReasonId())) {
                return true;
            }

            if (request.getBlockedForCalculationOfLatePaymentFromDate() != null &&
                    !Objects.equals(request.getBlockedForCalculationOfLatePaymentFromDate(),
                            customerLiability.getBlockedForCalculationOfLatePaymentFromDate())) {
                return true;
            }

            if (request.getBlockedForCalculationOfLatePaymentToDate() != null &&
                    !Objects.equals(request.getBlockedForCalculationOfLatePaymentToDate(),
                            customerLiability.getBlockedForCalculationOfLatePaymentToDate())) {
                return true;
            }

            return request.getBlockedForCalculationOfLatePaymentAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForCalculationOfLatePaymentAdditionalInfo(),
                            customerLiability.getBlockedForCalculationOfLatePaymentAdditionalInfo());
        }

        return false;
    }

    /**
     * Checks if the blocked for liabilities offsetting parameters have changed between the request and the existing customer liability.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @return true if any of the blocked for liabilities offsetting parameters have changed, false otherwise
     */
    public boolean hasBlockedForLiabilitiesOffsettingChanged(CustomerLiabilityRequest request, CustomerLiability customerLiability) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(customerLiability.getBlockedForLiabilitiesOffsetting());
        if (currentBlockedStatus && !request.isBlockedForLiabilitiesOffsetting()) {
            return true;
        }

        boolean fieldsPresent = request.getBlockedForLiabilitiesOffsettingReasonId() != null ||
                request.getBlockedForLiabilitiesOffsettingFromDate() != null ||
                request.getBlockedForLiabilitiesOffsettingToDate() != null ||
                request.getBlockedForLiabilitiesOffsettingAdditionalInfo() != null;

        if (!fieldsPresent) {
            return false;
        }

        if (request.isBlockedForLiabilitiesOffsetting() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForLiabilitiesOffsetting()) {
            if (request.getBlockedForLiabilitiesOffsettingReasonId() != null &&
                    !Objects.equals(request.getBlockedForLiabilitiesOffsettingReasonId(),
                            customerLiability.getBlockedForLiabilitiesOffsettingBlockingReasonId())) {
                return true;
            }

            if (request.getBlockedForLiabilitiesOffsettingFromDate() != null &&
                    !Objects.equals(request.getBlockedForLiabilitiesOffsettingFromDate(),
                            customerLiability.getBlockedForLiabilitiesOffsettingFromDate())) {
                return true;
            }

            if (request.getBlockedForLiabilitiesOffsettingToDate() != null &&
                    !Objects.equals(request.getBlockedForLiabilitiesOffsettingToDate(),
                            customerLiability.getBlockedForLiabilitiesOffsettingToDate())) {
                return true;
            }

            if (request.getBlockedForLiabilitiesOffsettingAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForLiabilitiesOffsettingAdditionalInfo(),
                            customerLiability.getBlockedForLiabilitiesOffsettingAdditionalInfo())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the blocked for supply termination parameters have changed between the request and the existing customer liability.
     *
     * @param request           the customer liability request
     * @param customerLiability the existing customer liability
     * @return true if any of the blocked for supply termination parameters have changed, false otherwise
     */
    public boolean hasBlockedForSupplyTerminationChanged(CustomerLiabilityRequest request, CustomerLiability customerLiability) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(customerLiability.getBlockedForSupplyTermination());
        if (currentBlockedStatus && !request.isBlockedForSupplyTermination()) {
            return true;
        }

        boolean fieldsPresent = request.getBlockedForSupplyTerminationReasonId() != null ||
                request.getBlockedForSupplyTerminationFromDate() != null ||
                request.getBlockedForSupplyTerminationToDate() != null ||
                request.getBlockedForSupplyTerminationAdditionalInfo() != null;

        if (!fieldsPresent) {
            return false;
        }

        if (request.isBlockedForSupplyTermination() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForSupplyTermination()) {
            if (request.getBlockedForSupplyTerminationReasonId() != null &&
                    !Objects.equals(request.getBlockedForSupplyTerminationReasonId(),
                            customerLiability.getBlockedForSupplyTerminationBlockingReasonId())) {
                return true;
            }

            if (request.getBlockedForSupplyTerminationFromDate() != null &&
                    !Objects.equals(request.getBlockedForSupplyTerminationFromDate(),
                            customerLiability.getBlockedForSupplyTerminationFromDate())) {
                return true;
            }

            if (request.getBlockedForSupplyTerminationToDate() != null &&
                    !Objects.equals(request.getBlockedForSupplyTerminationToDate(),
                            customerLiability.getBlockedForSupplyTerminationToDate())) {
                return true;
            }

            return request.getBlockedForSupplyTerminationAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForSupplyTerminationAdditionalInfo(),
                            customerLiability.getBlockedForSupplyTerminationAdditionalInfo());
        }

        return false;
    }

    /**
     * Checks if the current user has the specified permission.
     *
     * @param permission  the permission to check
     * @param permissions the set of permissions the user has
     * @return true if the user has the specified permission, false otherwise
     * @throws ClientException if the user does not have the specified permission
     */
    private boolean hasPermission(PermissionEnum permission, Set<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (!permissionService.getPermissionsFromContext(CUSTOMER_LIABILITY)
                    .contains(permission.getId())) {
                throw new ClientException("You don't have permission to update blocked fields;", ErrorCode.ACCESS_DENIED);
            }
        } else if (!permissions.contains(permission.getId())) {
            throw new ClientException("You don't have permission to update blocked fields;", ErrorCode.ACCESS_DENIED);
        }

        return true;
    }


    /**
     * Checks the currency and sets the amount without interest in the other currency.
     *
     * @param currencyId            The ID of the currency.
     * @param amountWithoutInterest The amount without interest.
     * @param customerLiability     The customer liability.
     * @param errorMessages         The list of error messages.
     */
    public void checkCurrencyAndSetAmountWithoutInterestInOtherCurrency(
            Long currencyId,
            BigDecimal amountWithoutInterest,
            CustomerLiability customerLiability,
            List<String> errorMessages
    ) {
        if (amountWithoutInterest == null) {
            return;
        }

        Optional<Currency> currency = currencyRepository.findByIdAndStatus(
                currencyId,
                List.of(
                        NomenclatureItemStatus.ACTIVE,
                        NomenclatureItemStatus.INACTIVE
                )
        );
        if (currency.isEmpty()) {
            errorMessages.add(String.format("currencyId-[%d] currency not found;", currencyId));
            return;
        }

        BigDecimal exchangeRate = currency.get()
                .getAltCurrencyExchangeRate();
        if (exchangeRate != null) {
            BigDecimal amountWithoutInterestInOtherCurrency = amountWithoutInterest.multiply(exchangeRate)
                    .setScale(2, RoundingMode.HALF_UP);
            customerLiability.setAmountWithoutInterestInOtherCurrency(amountWithoutInterestInOtherCurrency);
        }
    }

    private boolean getIfBlockedForCalculationOfLatePayment(Long productContractId) {
        if (productContractId != null) {
            return customerLiabilityRepository.getIfHasNoInterestOnOverdueDebts(productContractId);
        }
        return false;
    }

    /**
     * Maps the provided invoice compensation details to a new {@link CustomerLiability} object.
     * <p>
     * This method retrieves the necessary {@link Currency} and {@link Invoice} entities from the
     * repositories using the provided IDs. It then constructs a new {@link CustomerLiability}
     * based on the information from the {@link InvoiceCompensation}, {@link Invoice}, and {@link Currency}
     * objects, setting various fields like liability number, customer details, account period,
     * amounts, and other relevant financial data.
     * <p>
     * The method also handles the conversion of amounts to other currencies if an exchange rate
     * is available for the specified currency.
     *
     * @param customerId         The ID of the customer associated with the liability.
     *                           This can be either the customer ID from the invoice compensation or the compensation receipt ID.
     * @param currencyId         The ID of the currency to be used for the liability.
     * @param invoiceId          The ID of the invoice related to the compensation, used to retrieve the invoice details.
     * @param compensationAmount The amount of compensation, which is used as both the initial and current amounts in the liability.
     * @return A newly created {@link CustomerLiability} object, populated with the mapped data.
     * @throws DomainEntityNotFoundException If any of the following entities cannot be found:
     *                                       *         - Default income account number for receivables
     *                                       *         - Currency with the given currency ID
     *                                       *         - Invoice with the given invoice ID
     *                                       *         - Accounting period for the current month
     * @throws IllegalArgumentException      If any of the input values are invalid or null.
     */
    public CustomerLiability mapFromInvoiceCompensation(
            Long customerId,
            Long currencyId,
            Long invoiceId,
            BigDecimal compensationAmount
    ) {
        Currency currency = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("currencyId-[%d] currency with such ID not found!".formatted(currencyId))
                );

        Invoice invoice = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("invoiceId-[%d] invoice with such ID not found!".formatted(invoiceId))
                );

        CustomerLiability customerLiability = new CustomerLiability();

        customerLiability.setLiabilityNumber("TEMP");
        customerLiability.setCustomerId(customerId);
        customerLiability.setAccountPeriodId(invoice.getAccountPeriodId());
        customerLiability.setDueDate(invoice.getPaymentDeadline());
        customerLiability.setApplicableInterestRateId(invoice.getInterestRateId());
        customerLiability.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(compensationAmount));
        customerLiability.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(compensationAmount));
        customerLiability.setCurrencyId(currencyId);
        customerLiability.setOutgoingDocumentFromExternalSystem(invoice.getInvoiceNumber());
        customerLiability.setBasisForIssuing(invoice.getBasisForIssuing());
        customerLiability.setIncomeAccountNumber(
                incomeAccountNameRepository
                        .findNumberByDefaultAssignmentTypeOptional(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name())
                        .orElseThrow(() -> new DomainEntityNotFoundException("No default income account number found for receivables"))
        );
        customerLiability.setCostCenterControllingOrder(invoice.getCostCenterControllingOrder());
        customerLiability.setDirectDebit(invoice.getDirectDebit());
        customerLiability.setAccountPeriodId(invoice.getAccountPeriodId());

        Optional.ofNullable(invoice.getDirectDebit())
                .filter(Boolean.TRUE::equals)
                .ifPresent(directDebit -> {
                            customerLiability.setBankId(invoice.getBankId());
                            customerLiability.setIban(invoice.getIban());
                        }
                );

        customerLiability.setContractBillingGroupId(invoice.getContractBillingGroupId());
        customerLiability.setInvoiceId(invoice.getId());
        customerLiability.setOutgoingDocumentType(CustomerLiabilitiesOutgoingDocType.INVOICE);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setOccurrenceDate(invoice.getInvoiceDate());
        customerLiability.setBlockedForPayment(false);

        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();
        if (exchangeRate != null) {
            BigDecimal initialAmountInOtherCurrency = compensationAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentAmountInOtherCurrency = compensationAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            customerLiability.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(initialAmountInOtherCurrency));
            customerLiability.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(currentAmountInOtherCurrency));
        }

        return customerLiability;
    }
}
