package bg.energo.phoenix.service.massImport.customerReceivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForBank;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.receivable.customerReceivable.CustomerReceivableRequest;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
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
public class CustomerReceivableExcelMapper {
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final CurrencyRepository currencyRepository;
    private final BankRepository bankRepository;
    private final BlockingReasonRepository blockingReasonRepository;
    private final CustomerRepository customerRepository;
    private final ContractBillingGroupRepository billingGroupRepository;

    public CustomerReceivableRequest toCustomerReceivableCreateRequest(CustomerReceivableRequest customerReceivableRequest, Row row, List<String> errorMessages) {
        Long accountingPeriodId = getAccountingPeriodId(getStringValue(1, row), errorMessages);
        customerReceivableRequest.setAccountingPeriodId(accountingPeriodId);
        customerReceivableRequest.setInitialAmount(getDecimalValue(2, row));

        Long currencyId = getCurrencyId(getStringValue(3, row), errorMessages);
        customerReceivableRequest.setCurrencyId(currencyId);
        customerReceivableRequest.setOutgoingDocumentForAnExternalSystem(getStringValue(4, row));
        customerReceivableRequest.setBasisForIssuing(getStringValue(5, row));
        customerReceivableRequest.setNumberOfIncomeAccount(getStringValue(6, row));
        customerReceivableRequest.setCostCenterControllingOrder(getStringValue(7, row));
        customerReceivableRequest.setDirectDebit(Boolean.TRUE.equals(getBoolean(getStringValue(8, row), errorMessages, "directDebit")));

        Long bankId = getBankId(getStringValue(9, row), errorMessages);
        customerReceivableRequest.setBankId(bankId);
        customerReceivableRequest.setBankAccount(getStringValue(10, row));
        customerReceivableRequest.setAdditionalReceivableInformation(getStringValue(11, row));

        customerReceivableRequest.setBlockedForOffsetting(Boolean.TRUE.equals(getBoolean(getStringValue(12, row), errorMessages, "blockedForOffsetting")));
        customerReceivableRequest.setBlockedFromDate(getDateValue(13, row));
        customerReceivableRequest.setBlockedToDate(getDateValue(14, row));
        customerReceivableRequest.setReasonId(getReasonId(getStringValue(15, row), errorMessages));
        customerReceivableRequest.setAdditionalInformation(getStringValue(16, row));

        Long customerId = getCustomerId(getStringValue(17, row), errorMessages);
        customerReceivableRequest.setCustomerId(customerId);

        Long billingGroupId = getBillingGroupId(getStringValue(18, row), errorMessages);
        customerReceivableRequest.setBillingGroupId(billingGroupId);

        Long alternativeRecipientOfInvoiceId = getCustomerId(getStringValue(19, row), errorMessages);
        customerReceivableRequest.setAlternativeInvoiceRecipientCustomerId(alternativeRecipientOfInvoiceId);
        setOccurrenceDate(getDateValue(20, row), customerReceivableRequest, errorMessages);
        setDueDate(getDateValue(21, row), customerReceivableRequest, errorMessages);
        return customerReceivableRequest;
    }

    public CustomerReceivableRequest toCustomerReceivableUpdateRequest(CustomerReceivable customerReceivable, Row row, List<String> errorMessages) {
        CustomerReceivableRequest request = new CustomerReceivableRequest();
        AccountingPeriods accountingPeriods = accountingPeriodsRepository.findById(customerReceivable.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting Period Not Found!"));
        AccountingPeriodStatus status = accountingPeriods.getStatus();
        Long accountingPeriodId = getAccountingPeriodId(getStringValue(1, row), errorMessages);
        if (accountingPeriodId == null)
            request.setAccountingPeriodId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getAccountPeriodId());
        else
            request.setAccountingPeriodId(accountingPeriodId);

        BigDecimal initialAmount = getDecimalValue(2, row);
        if (initialAmount == null)
            request.setInitialAmount(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getInitialAmount());
        else
            request.setInitialAmount(initialAmount);

        Long currencyId = getCurrencyId(getStringValue(3, row), errorMessages);
        if (currencyId == null)
            request.setCurrencyId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getCurrencyId());
        else
            request.setCurrencyId(currencyId);

        String outgoingDocumentFromExternalSystem = getStringValue(4, row);
        if (StringUtils.isBlank(outgoingDocumentFromExternalSystem))
            request.setOutgoingDocumentForAnExternalSystem(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getOutgoingDocumentFromExternalSystem());
        else
            request.setOutgoingDocumentForAnExternalSystem(outgoingDocumentFromExternalSystem);

        String basisForIssuing = getStringValue(5, row);
        if (StringUtils.isBlank(basisForIssuing))
            request.setBasisForIssuing(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getBasisForIssuing());
        else
            request.setBasisForIssuing(basisForIssuing);

        String numberOfIncomeAccount = getStringValue(6, row);
        if (StringUtils.isBlank(numberOfIncomeAccount))
            request.setNumberOfIncomeAccount(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getIncomeAccountNumber());
        else
            request.setNumberOfIncomeAccount(numberOfIncomeAccount);

        String costCenterControllingOrder = getStringValue(7, row);
        if (StringUtils.isBlank(costCenterControllingOrder))
            request.setCostCenterControllingOrder(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getCostCenterControllingOrder());
        else
            request.setCostCenterControllingOrder(costCenterControllingOrder);

        Boolean directDebit = getBoolean(getStringValue(8, row), errorMessages, "directDebit");
        if (directDebit == null)
            request.setDirectDebit(customerReceivable.getDirectDebit());
        else
            request.setDirectDebit(Boolean.TRUE.equals(directDebit));

        Long bankId = getBankId(getStringValue(9, row), errorMessages);
        if (bankId == null)
            request.setBankId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getBankId());
        else
            request.setBankId(bankId);

        String bankAccount = getStringValue(10, row);
        if (StringUtils.isBlank(bankAccount))
            request.setBankAccount(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getBankAccount());
        else
            request.setBankAccount(bankAccount);

        String additionalLiabilityInformation = getStringValue(11, row);
        if (StringUtils.isBlank(additionalLiabilityInformation))
            request.setAdditionalInformation(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getAdditionalInformation());
        else
            request.setAdditionalReceivableInformation(additionalLiabilityInformation);

        Boolean blockedForLiabilitiesOffsetting = getBoolean(getStringValue(12, row), errorMessages, "blockedForLiabilitiesOffsetting");
        if (blockedForLiabilitiesOffsetting == null)
            request.setBlockedForOffsetting(customerReceivable.getBlockedForPayment());
        else
            request.setBlockedForOffsetting(Boolean.TRUE.equals(blockedForLiabilitiesOffsetting));

        LocalDate blockedForPaymentFromDate = getDateValue(13, row);
        if (blockedForPaymentFromDate == null)
            request.setBlockedFromDate(status == AccountingPeriodStatus.OPEN ? null : customerReceivable.getBlockedForPaymentFromDate());
        else
            request.setBlockedFromDate(blockedForPaymentFromDate);

        LocalDate blockedForPaymentToDate = getDateValue(14, row);
        if (blockedForPaymentToDate == null)
            request.setBlockedToDate(status == AccountingPeriodStatus.OPEN ? null : customerReceivable.getBlockedForPaymentToDate());
        else
            request.setBlockedToDate(blockedForPaymentToDate);

        Long reasonId = getReasonId(getStringValue(15, row), errorMessages);
        if (reasonId == null)
            request.setReasonId(status == AccountingPeriodStatus.OPEN ? null : customerReceivable.getBlockedForPaymentBlockingReasonId());
        else
            request.setReasonId(reasonId);

        String blockedForPaymentAdditionalInfo = getStringValue(16, row);
        if (StringUtils.isBlank(blockedForPaymentAdditionalInfo))
            request.setAdditionalInformation(status == AccountingPeriodStatus.OPEN ? null : customerReceivable.getBlockedForPaymentAdditionalInformation());
        else
            request.setAdditionalInformation(blockedForPaymentAdditionalInfo);

        Long customerId = getCustomerId(getStringValue(17, row), errorMessages);
        if (customerId == null)
            request.setCustomerId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getCustomerId());
        else
            request.setCustomerId(customerId);

        Long billingGroupId = getBillingGroupId(getStringValue(18, row), errorMessages);
        if (billingGroupId == null)
            request.setBillingGroupId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getBillingGroupId());
        else
            request.setBillingGroupId(billingGroupId);

        Long alternativeInvoiceRecipientCustomerId = getCustomerId(getStringValue(19, row), errorMessages);
        if (alternativeInvoiceRecipientCustomerId == null)
            request.setAlternativeInvoiceRecipientCustomerId(status == AccountingPeriodStatus.CLOSED ? null : customerReceivable.getAlternativeRecipientOfInvoiceId());
        else
            request.setAlternativeInvoiceRecipientCustomerId(alternativeInvoiceRecipientCustomerId);

        LocalDate occurrenceDate = getDateValue(20, row);
        if (occurrenceDate == null)
            request.setOccurrenceDate(customerReceivable.getOccurrenceDate());
        else
            request.setOccurrenceDate(occurrenceDate);

        LocalDate dueDate = getDateValue(21, row);
        if (dueDate == null)
            request.setDueDate(customerReceivable.getDueDate());
        else
            request.setDueDate(dueDate);

        return request;
    }

    private Long getBillingGroupId(String billingGroupNumber, List<String> errorMessages) {
        Long result = null;
        if (billingGroupNumber != null) {
            Optional<CacheObject> billingGroupOptional = billingGroupRepository
                    .findCacheObjectByName(billingGroupNumber, EntityStatus.ACTIVE);
            if (billingGroupOptional.isPresent()) {
                result = billingGroupOptional.get().getId();
            } else {
                errorMessages.add("billingGroupId-Not found billing group with billing group number: %s;".formatted(billingGroupNumber));
            }
        }
        return result;
    }

    private void setOccurrenceDate(LocalDate occurrenceDate, CustomerReceivableRequest customerReceivableRequest, List<String> errorMessages) {
        if (occurrenceDate != null) {
            customerReceivableRequest.setOccurrenceDate(occurrenceDate);
        } else {
            errorMessages.add("occurrenceDate-occurrenceDate can not be null");
        }
    }

    private void setDueDate(LocalDate dueDate, CustomerReceivableRequest customerReceivableRequest, List<String> errorMessages) {
        if (dueDate != null) {
            customerReceivableRequest.setDueDate(dueDate);
        } else {
            errorMessages.add("dueDate-dueDate can not be null");
        }
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
        return EPBExcelUtils.getStringValue(columnNumber, row);
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
}
