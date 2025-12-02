package bg.energo.phoenix.service.massImport.customerLiability;


import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForBank;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityRequest;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
public class CustomerLiabilityExcelMapper {
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final InterestRateRepository interestRateRepository;
    private final CurrencyRepository currencyRepository;
    private final BankRepository bankRepository;
    private final BlockingReasonRepository blockingReasonRepository;
    private final CustomerRepository customerRepository;
    private final ContractBillingGroupRepository billingGroupRepository;

    public CustomerLiabilityRequest toCustomerLiabilityCreateRequest(CustomerLiabilityRequest request, Row row, List<String> errorMessages) {
        Long accountingPeriodId = getAccountingPeriodId(getStringValue(1, row), errorMessages);
        request.setAccountingPeriodId(accountingPeriodId);

        request.setDueDate(getDateValue(2, row));

        Long applicableInterestRate = getApplicableInterestRateId(getStringValue(3, row), errorMessages);
        request.setApplicableInterestRateId(applicableInterestRate);

        request.setInterestDateFrom(getDateValue(4, row));
        request.setInterestDateTo(getDateValue(5, row));
        request.setInitialAmount(getDecimalValue(6, row));

        Long currencyId = getCurrencyId(getStringValue(7, row), errorMessages);
        request.setCurrencyId(currencyId);

        request.setOutgoingDocumentFromExternalSystem(getStringValue(8, row));
        request.setBasisForIssuing(getStringValue(9, row));
        request.setNumberOfIncomeAccount(getStringValue(10, row));
        request.setCostCenterControllingOrder(getStringValue(11, row));
        request.setDirectDebit(Boolean.TRUE.equals(getBoolean(getStringValue(12, row), errorMessages, "directDebit")));

        Long bankId = getBankId(getStringValue(13, row), errorMessages);
        request.setBankId(bankId);

        request.setBankAccount(getStringValue(14, row));
        request.setAdditionalLiabilityInformation(getStringValue(15, row));

        request.setBlockedForPayment(Boolean.TRUE.equals(getBoolean(getStringValue(16, row), errorMessages, "blockedForPayment")));
        request.setBlockedForPaymentFromDate(getDateValue(17, row));
        request.setBlockedForPaymentToDate(getDateValue(18, row));
        Long paymentReasonId = getReasonId(getStringValue(19, row), errorMessages);
        request.setBlockedForPaymentReasonId(paymentReasonId);
        request.setBlockedForPaymentAdditionalInfo(getStringValue(20, row));

        request.setBlockedForReminderLetters(Boolean.TRUE.equals(getBoolean(getStringValue(21, row), errorMessages, "blockedForReminderLetters")));
        request.setBlockedForReminderLettersFromDate(getDateValue(22, row));
        request.setBlockedForReminderLettersToDate(getDateValue(23, row));
        Long reminderLettersReasonId = getReasonId(getStringValue(24, row), errorMessages);
        request.setBlockedForReminderLettersReasonId(reminderLettersReasonId);
        request.setBlockedForReminderLettersAdditionalInfo(getStringValue(25, row));

        request.setBlockedForCalculationOfLatePayment(Boolean.TRUE.equals(getBoolean(getStringValue(26, row), errorMessages, "blockedForCalculationOfLatePayment")));
        request.setBlockedForCalculationOfLatePaymentFromDate(getDateValue(27, row));
        request.setBlockedForCalculationOfLatePaymentToDate(getDateValue(28, row));
        Long latePaymentReasonId = getReasonId(getStringValue(29, row), errorMessages);
        request.setBlockedForCalculationOfLatePaymentReasonId(latePaymentReasonId);
        request.setBlockedForCalculationOfLatePaymentAdditionalInfo(getStringValue(30, row));

        request.setBlockedForLiabilitiesOffsetting(Boolean.TRUE.equals(getBoolean(getStringValue(31, row), errorMessages, "blockedForLiabilitiesOffsetting")));
        request.setBlockedForLiabilitiesOffsettingFromDate(getDateValue(32, row));
        request.setBlockedForLiabilitiesOffsettingToDate(getDateValue(33, row));
        Long liabilitiesOffsettingReasonId = getReasonId(getStringValue(34, row), errorMessages);
        request.setBlockedForLiabilitiesOffsettingReasonId(liabilitiesOffsettingReasonId);
        request.setBlockedForLiabilitiesOffsettingAdditionalInfo(getStringValue(35, row));

        request.setBlockedForSupplyTermination(Boolean.TRUE.equals(getBoolean(getStringValue(36, row), errorMessages, "blockedForSupplyTermination")));
        request.setBlockedForSupplyTerminationFromDate(getDateValue(37, row));
        request.setBlockedForSupplyTerminationToDate(getDateValue(38, row));
        Long supplyTerminationReasonId = getReasonId(getStringValue(39, row), errorMessages);
        request.setBlockedForSupplyTerminationReasonId(supplyTerminationReasonId);
        request.setBlockedForSupplyTerminationAdditionalInfo(getStringValue(40, row));

        Long customerId = getCustomerId(getStringValue(41, row), errorMessages);
        request.setCustomerId(customerId);

        Long billingGroupId = getBillingGroupId(getStringValue(42, row), errorMessages, customerId);
        request.setBillingGroupId(billingGroupId);

        Long alternativeInvoiceRecipientCustomerId = getCustomerId(getStringValue(43, row), errorMessages);
        request.setAlternativeInvoiceRecipientCustomerId(alternativeInvoiceRecipientCustomerId);

        request.setAmountWithoutInterest(getDecimalValue(44, row));
        request.setOccurrenceDate(getDateValue(45, row));

        return request;
    }

    public CustomerLiabilityRequest toCustomerLiabilityUpdateRequest(CustomerLiability customerLiability, Row row, List<String> errorMessages) {
        CustomerLiabilityRequest request = new CustomerLiabilityRequest();
        AccountingPeriods accountingPeriods = accountingPeriodsRepository.findById(customerLiability.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting Period Not Found!"));
        AccountingPeriodStatus status = accountingPeriods.getStatus();

        Long accountingPeriodId = getAccountingPeriodId(getStringValue(1, row), errorMessages);
        if (accountingPeriodId == null)
            request.setAccountingPeriodId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getAccountPeriodId());
        else
            request.setAccountingPeriodId(accountingPeriodId);

        LocalDate dueDate = getDateValue(2, row);
        if (dueDate == null)
            request.setDueDate(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getDueDate());
        else
            request.setDueDate(dueDate);

        Long applicableInterestRate = getApplicableInterestRateId(getStringValue(3, row), errorMessages);
        if (applicableInterestRate == null)
            request.setApplicableInterestRateId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getApplicableInterestRateId());
        else
            request.setApplicableInterestRateId(applicableInterestRate);

        LocalDate interestDateFrom = getDateValue(4, row);
        if (interestDateFrom == null)
            request.setInterestDateFrom(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getApplicableInterestRateDateFrom());
        else
            request.setInterestDateFrom(interestDateFrom);

        LocalDate interestDateTo = getDateValue(5, row);
        if (interestDateTo == null)
            request.setInterestDateTo(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getApplicableInterestRateDateTo());
        else
            request.setInterestDateTo(interestDateTo);

        BigDecimal initialAmount = getDecimalValue(6, row);
        if (initialAmount == null)
            request.setInitialAmount(status == AccountingPeriodStatus.CLOSED ? null : EPBDecimalUtils.roundToTwoDecimalPlaces(customerLiability.getInitialAmount()));
        else
            request.setInitialAmount(initialAmount);

        Long currencyId = getCurrencyId(getStringValue(7, row), errorMessages);
        if (currencyId == null)
            request.setCurrencyId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getCurrencyId());
        else
            request.setCurrencyId(currencyId);

        String outgoingDocumentFromExternalSystem = getStringValue(8, row);
        if (StringUtils.isBlank(outgoingDocumentFromExternalSystem))
            request.setOutgoingDocumentFromExternalSystem(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getOutgoingDocumentFromExternalSystem());
        else
            request.setOutgoingDocumentFromExternalSystem(outgoingDocumentFromExternalSystem);

        String basisForIssuing = getStringValue(9, row);
        if (StringUtils.isBlank(basisForIssuing))
            request.setBasisForIssuing(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getBasisForIssuing());
        else
            request.setBasisForIssuing(basisForIssuing);

        String numberOfIncomeAccount = getStringValue(10, row);
        if (StringUtils.isBlank(numberOfIncomeAccount))
            request.setNumberOfIncomeAccount(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getIncomeAccountNumber());
        else
            request.setNumberOfIncomeAccount(numberOfIncomeAccount);

        String costCenterControllingOrder = getStringValue(11, row);
        if (StringUtils.isBlank(costCenterControllingOrder))
            request.setCostCenterControllingOrder(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getCostCenterControllingOrder());
        else
            request.setCostCenterControllingOrder(costCenterControllingOrder);

        Boolean directDebit = getBoolean(getStringValue(12, row), errorMessages, "directDebit");
        if (directDebit == null)
            request.setDirectDebit(customerLiability.getDirectDebit());
        else
            request.setDirectDebit(Boolean.TRUE.equals(directDebit));

        Long bankId = getBankId(getStringValue(13, row), errorMessages);
        if (bankId == null)
            request.setBankId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getBankId());
        else
            request.setBankId(bankId);

        String bankAccount = getStringValue(14, row);
        if (StringUtils.isBlank(bankAccount))
            request.setBankAccount(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getIban());
        else
            request.setBankAccount(bankAccount);

        String additionalLiabilityInformation = getStringValue(15, row);
        if (StringUtils.isBlank(additionalLiabilityInformation))
            request.setAdditionalLiabilityInformation(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getAdditionalInfo());
        else
            request.setAdditionalLiabilityInformation(additionalLiabilityInformation);

        Boolean blockedForPayment = getBoolean(getStringValue(16, row), errorMessages, "blockedForPayment");
        if (blockedForPayment == null)
            request.setBlockedForPayment(customerLiability.getBlockedForPayment());
        else
            request.setBlockedForPayment(Boolean.TRUE.equals(blockedForPayment));

        LocalDate blockedForPaymentFromDate = getDateValue(17, row);
        if (blockedForPaymentFromDate == null)
            if (Boolean.FALSE.equals(blockedForPayment)) {
                request.setBlockedForPaymentFromDate(null);
            } else {
                request.setBlockedForPaymentFromDate(customerLiability.getBlockedForPaymentFromDate());
            }
        else
            request.setBlockedForPaymentFromDate(blockedForPaymentFromDate);

        LocalDate blockedForPaymentToDate = getDateValue(18, row);
        if (blockedForPaymentToDate == null)
            if (Boolean.FALSE.equals(blockedForPayment)) {
                request.setBlockedForPaymentToDate(null);
            } else {
                request.setBlockedForPaymentToDate(customerLiability.getBlockedForPaymentToDate());
            }
        else
            request.setBlockedForPaymentToDate(blockedForPaymentToDate);

        Long paymentReasonId = getReasonId(getStringValue(19, row), errorMessages);
        if (paymentReasonId == null)
            if (Boolean.FALSE.equals(blockedForPayment)) {
                request.setBlockedForPaymentReasonId(null);
            } else {
                request.setBlockedForPaymentReasonId(customerLiability.getBlockedForPaymentBlockingReasonId());
            }
        else
            request.setBlockedForPaymentReasonId(paymentReasonId);

        String blockedForPaymentAdditionalInfo = getStringValue(20, row);
        if (StringUtils.isBlank(blockedForPaymentAdditionalInfo))
            if (Boolean.FALSE.equals(blockedForPayment)) {
                request.setBlockedForPaymentAdditionalInfo(null);
            } else {
                request.setBlockedForPaymentAdditionalInfo(customerLiability.getBlockedForPaymentAdditionalInfo());
            }
        else
            request.setBlockedForPaymentAdditionalInfo(blockedForPaymentAdditionalInfo);

        Boolean blockedForReminderLetters = getBoolean(getStringValue(21, row), errorMessages, "blockedForReminderLetters");
        if (blockedForReminderLetters == null)
            request.setBlockedForReminderLetters(customerLiability.getBlockedForReminderLetters());
        else
            request.setBlockedForReminderLetters(Boolean.TRUE.equals(blockedForReminderLetters));

        LocalDate blockedForReminderLettersFromDate = getDateValue(22, row);
        if (blockedForReminderLettersFromDate == null)
            if (Boolean.FALSE.equals(blockedForReminderLetters)) {
                request.setBlockedForReminderLettersFromDate(null);
            } else {
                request.setBlockedForReminderLettersFromDate(customerLiability.getBlockedForReminderLettersFromDate());
            }
        else
            request.setBlockedForReminderLettersFromDate(blockedForReminderLettersFromDate);

        LocalDate blockedForReminderLettersToDate = getDateValue(23, row);
        if (blockedForReminderLettersToDate == null)
            if (Boolean.FALSE.equals(blockedForReminderLetters)) {
                request.setBlockedForReminderLettersToDate(null);
            } else {
                request.setBlockedForReminderLettersToDate(customerLiability.getBlockedForReminderLettersToDate());
            }
        else
            request.setBlockedForReminderLettersToDate(blockedForReminderLettersToDate);

        Long reminderLettersReasonId = getReasonId(getStringValue(24, row), errorMessages);
        if (reminderLettersReasonId == null)
            if (Boolean.FALSE.equals(blockedForReminderLetters)) {
                request.setBlockedForReminderLettersReasonId(null);
            } else {
                request.setBlockedForReminderLettersReasonId(customerLiability.getBlockedForReminderLettersBlockingReasonId());
            }
        else
            request.setBlockedForReminderLettersReasonId(reminderLettersReasonId);

        String blockedForReminderLettersAdditionalInfo = getStringValue(25, row);
        if (StringUtils.isBlank(blockedForReminderLettersAdditionalInfo))
            if (Boolean.FALSE.equals(blockedForReminderLetters)) {
                request.setBlockedForReminderLettersAdditionalInfo(null);
            } else {
                request.setBlockedForReminderLettersAdditionalInfo(customerLiability.getBlockedForReminderLettersAdditionalInfo());
            }
        else
            request.setBlockedForReminderLettersAdditionalInfo(blockedForReminderLettersAdditionalInfo);

        Boolean blockedForCalculationOfLatePayment = getBoolean(getStringValue(26, row), errorMessages, "blockedForCalculationOfLatePayment");
        if (blockedForCalculationOfLatePayment == null)
            request.setBlockedForCalculationOfLatePayment(customerLiability.getBlockedForCalculationOfLatePayment());
        else
            request.setBlockedForCalculationOfLatePayment(Boolean.TRUE.equals(blockedForCalculationOfLatePayment));

        LocalDate blockedForCalculationOfLatePaymentFromDate = getDateValue(27, row);
        if (blockedForCalculationOfLatePaymentFromDate == null)
            if (Boolean.FALSE.equals(blockedForCalculationOfLatePayment)) {
                request.setBlockedForCalculationOfLatePaymentFromDate(null);
            } else {
                request.setBlockedForCalculationOfLatePaymentFromDate(customerLiability.getBlockedForCalculationOfLatePaymentFromDate());
            }
        else
            request.setBlockedForCalculationOfLatePaymentFromDate(blockedForCalculationOfLatePaymentFromDate);

        LocalDate blockedForCalculationOfLatePaymentToDate = getDateValue(28, row);
        if (blockedForCalculationOfLatePaymentToDate == null)
            if (Boolean.FALSE.equals(blockedForCalculationOfLatePayment)) {
                request.setBlockedForCalculationOfLatePaymentToDate(null);
            } else {
                request.setBlockedForCalculationOfLatePaymentToDate(customerLiability.getBlockedForCalculationOfLatePaymentToDate());
            }
        else
            request.setBlockedForCalculationOfLatePaymentToDate(blockedForCalculationOfLatePaymentToDate);

        Long latePaymentReasonId = getReasonId(getStringValue(29, row), errorMessages);
        if (latePaymentReasonId == null)
            if (Boolean.FALSE.equals(blockedForCalculationOfLatePayment)) {
                request.setBlockedForCalculationOfLatePaymentReasonId(null);
            } else {
                request.setBlockedForCalculationOfLatePaymentReasonId(customerLiability.getBlockedForCalculationOfLatePaymentBlockingReasonId());
            }
        else
            request.setBlockedForCalculationOfLatePaymentReasonId(latePaymentReasonId);

        String blockedForCalculationOfLatePaymentAdditionalInfo = getStringValue(30, row);
        if (StringUtils.isBlank(blockedForCalculationOfLatePaymentAdditionalInfo))
            if (Boolean.FALSE.equals(blockedForCalculationOfLatePayment)) {
                request.setBlockedForCalculationOfLatePaymentAdditionalInfo(null);
            } else {
                request.setBlockedForCalculationOfLatePaymentAdditionalInfo(customerLiability.getBlockedForCalculationOfLatePaymentAdditionalInfo());
            }
        else
            request.setBlockedForCalculationOfLatePaymentAdditionalInfo(blockedForCalculationOfLatePaymentAdditionalInfo);

        Boolean blockedForLiabilitiesOffsetting = getBoolean(getStringValue(31, row), errorMessages, "blockedForLiabilitiesOffsetting");
        if (blockedForLiabilitiesOffsetting == null)
            request.setBlockedForLiabilitiesOffsetting(customerLiability.getBlockedForLiabilitiesOffsetting());
        else
            request.setBlockedForLiabilitiesOffsetting(Boolean.TRUE.equals(blockedForLiabilitiesOffsetting));

        LocalDate blockedForLiabilitiesOffsettingFromDate = getDateValue(32, row);
        if (blockedForLiabilitiesOffsettingFromDate == null)
            if (Boolean.FALSE.equals(blockedForLiabilitiesOffsetting)) {
                request.setBlockedForLiabilitiesOffsettingFromDate(null);
            } else {
                request.setBlockedForLiabilitiesOffsettingFromDate(customerLiability.getBlockedForLiabilitiesOffsettingFromDate());
            }
        else
            request.setBlockedForLiabilitiesOffsettingFromDate(blockedForLiabilitiesOffsettingFromDate);

        LocalDate blockedForLiabilitiesOffsettingToDate = getDateValue(33, row);
        if (blockedForLiabilitiesOffsettingToDate == null)
            if (Boolean.FALSE.equals(blockedForLiabilitiesOffsetting)) {
                request.setBlockedForLiabilitiesOffsettingToDate(null);
            } else {
                request.setBlockedForLiabilitiesOffsettingToDate(customerLiability.getBlockedForLiabilitiesOffsettingToDate());
            }
        else
            request.setBlockedForLiabilitiesOffsettingToDate(blockedForLiabilitiesOffsettingToDate);

        Long liabilitiesOffsettingReasonId = getReasonId(getStringValue(34, row), errorMessages);
        if (liabilitiesOffsettingReasonId == null)
            if (Boolean.FALSE.equals(blockedForLiabilitiesOffsetting)) {
                request.setBlockedForLiabilitiesOffsettingReasonId(null);
            } else {
                request.setBlockedForLiabilitiesOffsettingReasonId(customerLiability.getBlockedForLiabilitiesOffsettingBlockingReasonId());
            }
        else
            request.setBlockedForLiabilitiesOffsettingReasonId(liabilitiesOffsettingReasonId);

        String blockedForLiabilitiesOffsettingAdditionalInfo = getStringValue(35, row);
        if (StringUtils.isBlank(blockedForLiabilitiesOffsettingAdditionalInfo))
            if (Boolean.FALSE.equals(blockedForLiabilitiesOffsetting)) {
                request.setBlockedForLiabilitiesOffsettingAdditionalInfo(null);
            } else {
                request.setBlockedForLiabilitiesOffsettingAdditionalInfo(customerLiability.getBlockedForLiabilitiesOffsettingAdditionalInfo());
            }
        else
            request.setBlockedForLiabilitiesOffsettingAdditionalInfo(blockedForLiabilitiesOffsettingAdditionalInfo);

        Boolean blockedForSupplyTermination = getBoolean(getStringValue(36, row), errorMessages, "blockedForSupplyTermination");
        if (blockedForSupplyTermination == null)
            request.setBlockedForSupplyTermination(customerLiability.getBlockedForSupplyTermination());
        else
            request.setBlockedForSupplyTermination(Boolean.TRUE.equals(blockedForSupplyTermination));

        LocalDate blockedForSupplyTerminationFromDate = getDateValue(37, row);
        if (blockedForSupplyTerminationFromDate == null)
            if (Boolean.FALSE.equals(blockedForSupplyTermination)) {
                request.setBlockedForSupplyTerminationFromDate(null);
            } else {
                request.setBlockedForSupplyTerminationFromDate(customerLiability.getBlockedForSupplyTerminationFromDate());
            }
        else
            request.setBlockedForSupplyTerminationFromDate(blockedForSupplyTerminationFromDate);

        LocalDate blockedForSupplyTerminationToDate = getDateValue(38, row);
        if (blockedForSupplyTerminationToDate == null)
            if (Boolean.FALSE.equals(blockedForSupplyTermination)) {
                request.setBlockedForSupplyTerminationToDate(null);
            } else {
                request.setBlockedForSupplyTerminationToDate(customerLiability.getBlockedForSupplyTerminationToDate());
            }
        else
            request.setBlockedForSupplyTerminationToDate(blockedForSupplyTerminationToDate);

        Long supplyTerminationReasonId = getReasonId(getStringValue(39, row), errorMessages);
        if (supplyTerminationReasonId == null)
            if (Boolean.FALSE.equals(blockedForSupplyTermination)) {
                request.setBlockedForSupplyTerminationReasonId(null);
            } else {
                request.setBlockedForSupplyTerminationReasonId(customerLiability.getBlockedForSupplyTerminationBlockingReasonId());
            }
        else
            request.setBlockedForSupplyTerminationReasonId(supplyTerminationReasonId);

        String blockedForSupplyTerminationAdditionalInfo = getStringValue(40, row);
        if (StringUtils.isBlank(blockedForSupplyTerminationAdditionalInfo))
            if (Boolean.FALSE.equals(blockedForSupplyTermination)) {
                request.setBlockedForSupplyTerminationAdditionalInfo(null);
            } else {
                request.setBlockedForSupplyTerminationAdditionalInfo(customerLiability.getBlockedForSupplyTerminationAdditionalInfo());
            }
        else
            request.setBlockedForSupplyTerminationAdditionalInfo(blockedForSupplyTerminationAdditionalInfo);

        Long customerId = getCustomerId(getStringValue(41, row), errorMessages);
        if (customerId == null)
            request.setCustomerId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getCustomerId());
        else
            request.setCustomerId(customerId);

        Long billingGroupId = getBillingGroupId(getStringValue(42, row), errorMessages, customerId);
        if (billingGroupId == null)
            request.setBillingGroupId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getContractBillingGroupId());
        else
            request.setBillingGroupId(billingGroupId);

        Long alternativeInvoiceRecipientCustomerId = getCustomerId(getStringValue(43, row), errorMessages);
        if (alternativeInvoiceRecipientCustomerId == null)
            request.setAlternativeInvoiceRecipientCustomerId(status == AccountingPeriodStatus.CLOSED ? null : customerLiability.getAltInvoiceRecipientCustomerId());
        else
            request.setAlternativeInvoiceRecipientCustomerId(alternativeInvoiceRecipientCustomerId);

        BigDecimal amountWithoutInterest = getDecimalValue(44, row);
        if (amountWithoutInterest == null)
            request.setAmountWithoutInterest(status == AccountingPeriodStatus.CLOSED ? BigDecimal.ZERO : (customerLiability.getAmountWithoutInterest() == null ? BigDecimal.ZERO : customerLiability.getAmountWithoutInterest()));
        else
            request.setAmountWithoutInterest(amountWithoutInterest);

        LocalDate occurrenceDate = getDateValue(45, row);
        if (occurrenceDate == null)
            request.setOccurrenceDate(customerLiability.getOccurrenceDate());
        else
            request.setOccurrenceDate(occurrenceDate);

        return request;
    }

    private Long getBillingGroupId(String billingGroupNumber, List<String> errorMessages, Long customerId) {
        Long result = null;
        if (billingGroupNumber != null && customerId != null) {
            Optional<CacheObject> billingGroupOptional = billingGroupRepository
                    .findByCustomerIdAndGroupNumber(customerId, billingGroupNumber);
            if (billingGroupOptional.isPresent()) {
                result = billingGroupOptional.get().getId();
            } else {
                errorMessages.add("billingGroupId-Not found billing group with billing group number: %s;".formatted(billingGroupNumber));
            }
        }
        return result;
    }

    private Long getCustomerId(String customerIdentifier, List<String> errorMessages) {
        Long result = null;
        if (customerIdentifier != null) {
            Optional<CacheObject> customerOptional = customerRepository
                    .findCacheObjectByName(customerIdentifier, CustomerStatus.ACTIVE);
            if (customerOptional.isPresent()) {
                result = customerOptional.get().getId();
            } else {
                errorMessages.add("customerId-Not found customer with identifier: %s;".formatted(customerIdentifier));
            }
        }
        return result;
    }

    private Long getReasonId(String reasonName, List<String> errorMessages) {
        Long result = null;
        if (reasonName != null) {
            Optional<CacheObject> reasonOptional = blockingReasonRepository
                    .findCacheObjectByName(reasonName, NomenclatureItemStatus.ACTIVE);
            if (reasonOptional.isPresent()) {
                result = reasonOptional.get().getId();
            } else {
                errorMessages.add("reasonId-Not found blocking reason with name: %s;".formatted(reasonName));
            }
        }
        return result;
    }

    private Long getBankId(String bankName, List<String> errorMessages) {
        Long result = null;
        if (bankName != null) {
            Optional<CacheObjectForBank> bankOptional = bankRepository
                    .getByNameAndStatus(bankName, NomenclatureItemStatus.ACTIVE);
            if (bankOptional.isPresent()) {
                result = bankOptional.get().getId();
            } else {
                errorMessages.add("bankId-Not found bank with name: %s;".formatted(bankName));
            }
        }
        return result;
    }

    private Long getCurrencyId(String currencyName, List<String> errorMessages) {
        Long result = null;
        if (currencyName != null) {
            Optional<CacheObject> currencyOptional = currencyRepository
                    .getCacheObjectByNameAndStatusIn(currencyName, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
            if (currencyOptional.isPresent()) {
                result = currencyOptional.get().getId();
            } else {
                errorMessages.add("currencyId-Not found currency with name: %s;".formatted(currencyName));
            }
        }
        return result;
    }

    private Long getApplicableInterestRateId(String applicableInterestRateName, List<String> errorMessages) {
        Long result = null;
        if (applicableInterestRateName != null) {
            Optional<CacheObject> interestRate = interestRateRepository
                    .findCacheObjectByName(applicableInterestRateName, InterestRateStatus.ACTIVE);
            if (interestRate.isPresent()) {
                result = interestRate.get().getId();
            } else {
                errorMessages.add("applicableInterestRateId-Not found applicableInterestRate with name: %s;".formatted(applicableInterestRateName));
            }
        }
        return result;
    }

    private Long getAccountingPeriodId(String accountingPeriodName, List<String> errorMessages) {
        Long result = null;
        if (accountingPeriodName != null) {
            Optional<CacheObject> accountingPeriod = accountingPeriodsRepository
                    .findCacheObjectByName(accountingPeriodName, AccountingPeriodStatus.OPEN);
            if (accountingPeriod.isPresent()) {
                result = accountingPeriod.get().getId();
            } else {
                errorMessages.add("accountingPeriodId-Not found accountingPeriod with name: %s;".formatted(accountingPeriodName));
            }
        }
        return result;
    }

    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return null;
    }

    private LocalDate getDateValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            if (row.getCell(columnNumber).getDateCellValue() != null) {
                return from(
                        LocalDate.ofInstant(
                                row.getCell(columnNumber).getDateCellValue().toInstant(), ZoneId.systemDefault()));
            }
        }
        return null;
    }

    private BigDecimal getDecimalValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            BigDecimal result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = BigDecimal.valueOf(Long.parseLong(row.getCell(columnNumber).getStringCellValue()));
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = BigDecimal.valueOf(row.getCell(columnNumber).getNumericCellValue());
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }
}
