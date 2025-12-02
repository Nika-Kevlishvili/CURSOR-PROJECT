package bg.energo.phoenix.service.receivable.customerReceivables;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.LiabilityVatBaseModel;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceStandardDetailedDataVatBaseRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.entity.EntityStatus.ACTIVE;

@Service
@RequiredArgsConstructor
public class CustomerReceivableMapperService {
    private final BillingRunRepository billingRunRepository;
    private final CustomerRepository customerRepository;
    private final CurrencyRepository currencyRepository;
    private final InvoiceStandardDetailedDataVatBaseRepository invoiceStandardDetailedDataVatBaseRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerReceivable mapReceivableFromInvoice(Invoice invoice, LiabilityOrReceivableCreationSource source) {
        CustomerReceivable.CustomerReceivableBuilder customerReceivableBuilder = CustomerReceivable.builder();
        return setUpInvoiceMapping(invoice, invoice.getTotalAmountIncludingVat(), customerReceivableBuilder, source);
    }

    public CustomerReceivable mapReceivableFromInvoiceByVatBase(Invoice invoice, BigDecimal customerReceivableCalculatedAmount) {
        CustomerReceivable.CustomerReceivableBuilder customerReceivableBuilder = CustomerReceivable.builder();
        return setUpInvoiceMapping(invoice, customerReceivableCalculatedAmount, customerReceivableBuilder, null);
    }

    private CustomerReceivable setUpInvoiceMapping(Invoice invoice, BigDecimal amount, CustomerReceivable.CustomerReceivableBuilder builder, LiabilityOrReceivableCreationSource source) {
        BillingRun billingRun = null;
        if (invoice.getBillingId() != null) {
            billingRun = billingRunRepository.findById(invoice.getBillingId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Billing run not found by ID %s;".formatted(invoice.getBillingId())));
        }

        Currency currency = currencyRepository.findByIdAndStatus(invoice.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("currencyId-[currencyId] currency with such ID not found!;"));
        fillReceivableParameters(builder, invoice, billingRun, source);
        calculateAndFillAmounts(builder, amount, currency);
        return builder.build();
    }

    private void fillReceivableParameters(CustomerReceivable.CustomerReceivableBuilder builder, Invoice invoice, BillingRun billingRun, LiabilityOrReceivableCreationSource source) {
        Customer customer = customerRepository.findByCustomerDetailIdAndStatusIn(invoice.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found with customerDetailId: %s;".formatted(invoice.getCustomerDetailId())));
        Long accountPeriodId = Optional.ofNullable(billingRun)
                .map(BillingRun::getAccountingPeriodId)
                .orElseGet(() -> accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period for current month not found")));

        builder.receivableNumber("TEMP");
        builder.accountPeriodId(accountPeriodId);
        builder.outgoingDocumentFromExternalSystem(invoice.getInvoiceNumber());
        builder.basisForIssuing(invoice.getBasisForIssuing());
        builder.incomeAccountNumber(invoice.getIncomeAccountNumber());
        builder.costCenterControllingOrder(invoice.getCostCenterControllingOrder());
        builder.directDebit(invoice.getDirectDebit());

        builder.incomeAccountNumber(getIncomeAccountNumberBasedOnSource(invoice, source));
        builder.occurrenceDate(invoice.getInvoiceDate());
        builder.dueDate(invoice.getPaymentDeadline() == null ? invoice.getInvoiceDate() : invoice.getPaymentDeadline());

        if (Boolean.TRUE.equals(invoice.getDirectDebit())) {
            builder.bankId(invoice.getBankId());
            builder.bankAccount(invoice.getIban());
        }

        builder.customerId(customer.getId());
        builder.billingGroupId(invoice.getContractBillingGroupId());

        builder.invoiceId(invoice.getId());
        builder.outgoingDocumentType(OutgoingDocumentType.CREDIT_NOTE);
        builder.creationType(CreationType.AUTOMATIC);
        builder.status(ACTIVE);
        builder.blockedForPayment(false);
    }

    private String getIncomeAccountNumberBasedOnSource(Invoice invoice, LiabilityOrReceivableCreationSource source) {
        String incomeAccountNumber = incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_RECEIVABLES.name());
        if (source != null && source.equals(LiabilityOrReceivableCreationSource.BILLING_RUN) && incomeAccountNumber == null) {
            throw new ClientException("Unable to find default receivable account number for invoice with number [%s];".formatted(invoice.getInvoiceNumber()), ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }
        return incomeAccountNumber;
    }

    private void calculateAndFillAmounts(CustomerReceivable.CustomerReceivableBuilder builder, BigDecimal amount, Currency currency) {
        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();
        BigDecimal amountInOtherCurrency = Objects.isNull(exchangeRate) ? null : amount.multiply(exchangeRate);
        builder.initialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(amount.abs()));
        builder.currentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(amount.abs()));
        builder.currencyId(currency.getId());
        builder.initialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(amountInOtherCurrency.abs()));
        builder.currentAmountInOtherCurrency(BigDecimal.ZERO);
    }

    public List<CustomerReceivable> generateAdditionalReceivablesForVatBase(Invoice invoice) {

        List<LiabilityVatBaseModel> vatBaseModels = invoiceStandardDetailedDataVatBaseRepository.findByInvoiceId(invoice.getReversalCreatedFromId());
        if (CollectionUtils.isNotEmpty(vatBaseModels)) {
            Map<Long, List<LiabilityVatBaseModel>> vatBaseGroupedByCustomer = vatBaseModels
                    .stream()
                    .collect(Collectors.groupingBy(LiabilityVatBaseModel::getCustomerId));

            List<CustomerReceivable> receivables = new ArrayList<>();

            vatBaseGroupedByCustomer.forEach((customerId, vatBases) -> {
                LiabilityVatBaseModel anyVatBase = vatBases.get(0);
                BigDecimal totalAmountWithoutVatMainCurrencySummary = EPBDecimalUtils.calculateSummary(vatBases.stream().map(LiabilityVatBaseModel::getTotalAmountWithoutVatMainCurrency).toList());
                BigDecimal totalAmountWithoutVatAltCurrencySummary = EPBDecimalUtils.calculateSummary(vatBases.stream().map(LiabilityVatBaseModel::getTotalAmountWithoutVatAltCurrency).toList());

                receivables.add(
                        CustomerReceivable.builder()
                                .receivableNumber("TEMPORARY_NUMBER")
                                .accountPeriodId(invoice.getAccountPeriodId())
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
                                .bankAccount(invoice.getIban())
                                .customerId(customerId)
                                .billingGroupId(invoice.getContractBillingGroupId())
                                .invoiceId(invoice.getId())
                                .outgoingDocumentType(OutgoingDocumentType.CREDIT_NOTE)
                                .creationType(CreationType.AUTOMATIC)
                                .status(EntityStatus.ACTIVE)
                                .initialAmountInOtherCurrency(totalAmountWithoutVatAltCurrencySummary)
                                .currentAmountInOtherCurrency(BigDecimal.ZERO)
                                .build()
                );

            });
            return receivables;
        }
        return new ArrayList<>();
    }

    /**
     * Maps the given invoice compensation details to a new {@link CustomerReceivable} entity.
     *
     * <p>This method takes the customer ID, currency ID, invoice ID, and compensation amount
     * as input, and creates a new {@link CustomerReceivable} object with the necessary fields
     * populated based on the provided data. It also handles retrieval of related entities
     * such as currency, invoice, and accounting period. If any of the required entities are
     * not found, appropriate exceptions will be thrown.</p>
     *
     * @param customerId The ID of the customer for whom the receivable is being created.
     * @param currencyId The ID of the currency associated with the receivable.
     * @param invoiceId The ID of the invoice related to the compensation.
     * @param compensationAmount The amount of the compensation.
     *
     * @return A {@link CustomerReceivable} object populated with the provided and related data.
     *
     * @throws DomainEntityNotFoundException If any of the following entities cannot be found:
     *         - Default income account number for receivables
     *         - Currency with the given currency ID
     *         - Invoice with the given invoice ID
     *         - Accounting period for the current month
     * @throws IllegalArgumentException      If any of the input values are invalid or null.
     *
     */
    public CustomerReceivable mapFromInvoiceCompensation(
            Long customerId,
            Long currencyId,
            Long invoiceId,
            BigDecimal compensationAmount
    ) {
        CustomerReceivable customerReceivable = new CustomerReceivable();
        customerReceivable.setReceivableNumber("TEMP");
        customerReceivable.setCustomerId(customerId);
        customerReceivable.setIncomeAccountNumber(
                incomeAccountNameRepository
                        .findNumberByDefaultAssignmentTypeOptional(DefaultAssignmentType.DEFAULT_FOR_RECEIVABLES.name())
                        .orElseThrow(() -> new DomainEntityNotFoundException("No default income account number found for receivables"))
        );

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

        customerReceivable.setAccountPeriodId(
                Optional.ofNullable(invoice.getBillingId())
                        .flatMap(billingRunRepository::findById)
                        .map(BillingRun::getAccountingPeriodId)
                        .orElseGet(() -> accountingPeriodsRepository
                                .findCurrentMonthsAccountingPeriodId()
                                .orElseThrow(
                                        () -> new DomainEntityNotFoundException("Accounting period for current month not found")
                                )
                        )
        );

        customerReceivable.setOutgoingDocumentFromExternalSystem(invoice.getInvoiceNumber());
        customerReceivable.setInvoiceId(invoice.getId());

        customerReceivable.setBasisForIssuing(invoice.getBasisForIssuing());
        customerReceivable.setCostCenterControllingOrder(invoice.getCostCenterControllingOrder());
        customerReceivable.setDirectDebit(invoice.getDirectDebit());
        customerReceivable.setOccurrenceDate(invoice.getInvoiceDate());
        customerReceivable.setDueDate(
                Objects.requireNonNullElse(
                        invoice.getPaymentDeadline(),
                        invoice.getInvoiceDate()
                )
        );

        Optional.ofNullable(invoice.getDirectDebit())
                .filter(Boolean.TRUE::equals)
                .ifPresent(directDebit -> {
                            customerReceivable.setBankId(invoice.getBankId());
                            customerReceivable.setBankAccount(invoice.getIban());
                        }
                );

        customerReceivable.setBillingGroupId(invoice.getContractBillingGroupId());
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setBlockedForPayment(false);

        BigDecimal exchangeRate = currency.getAltCurrencyExchangeRate();
        BigDecimal amountInOtherCurrency = Objects.isNull(exchangeRate) ? null : compensationAmount.multiply(exchangeRate);
        customerReceivable.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(compensationAmount.abs()));
        customerReceivable.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(compensationAmount.abs()));
        customerReceivable.setInitialAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(amountInOtherCurrency.abs()));
        customerReceivable.setCurrentAmountInOtherCurrency(EPBDecimalUtils.roundToTwoDecimalPlaces(amountInOtherCurrency.abs()));
        customerReceivable.setCurrencyId(currency.getId());

        return customerReceivable;
    }
}
