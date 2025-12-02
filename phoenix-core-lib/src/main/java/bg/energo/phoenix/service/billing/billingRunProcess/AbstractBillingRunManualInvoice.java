package bg.energo.phoenix.service.billing.billingRunProcess;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderPaymentTerm;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.repository.billing.billingRun.*;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderPaymentTermRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.service.billing.billingRunProcess.enums.BillingRunObjectType;
import bg.energo.phoenix.service.billing.billingRunProcess.mapper.BillingRunManualInvoicesMapper;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBillingRunManualInvoice {
    protected final ServiceContractDetailsRepository serviceContractDetailsRepository;
    protected final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    protected final CalendarRepository calendarRepository;
    protected final HolidaysRepository holidaysRepository;
    protected final BankRepository bankRepository;
    protected final ProductContractDetailsRepository productContractDetailsRepository;
    protected final GoodsOrderRepository goodsOrderRepository;
    protected final ServiceOrderRepository serviceOrderRepository;
    protected final CustomerDetailsRepository customerDetailsRepository;
    protected final ServiceDetailsRepository serviceDetailsRepository;
    protected final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    protected final ContractBillingGroupRepository contractBillingGroupRepository;
    protected final ProductDetailsRepository productDetailsRepository;
    protected final BillingRunBillingGroupRepository billingRunBillingGroupRepository;
    protected final InvoiceRepository invoiceRepository;
    protected final VatRateRepository vatRateRepository;
    protected final CurrencyRepository currencyRepository;
    protected final InvoiceEventPublisher invoiceEventPublisher;
    protected final ContractPodRepository contractPodRepository;
    protected final BillingSummaryDataRepository billingSummaryDataRepository;
    protected final BillingDetailedDataRepository billingDetailedDataRepository;
    protected final GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository;
    protected final BillingRunManualInvoicesMapper billingRunManualInvoicesMapper;
    protected final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    protected final BillingRunRepository billingRunRepository;
    protected final NotificationEventPublisher notificationEventPublisher;
    protected final BillingErrorDataRepository billingErrorDataRepository;
    protected final InvoiceNumberService invoiceNumberService;

    protected abstract void process(BillingRun billingRun);

    protected void publishNotification(Long billingRunId, NotificationType notificationType, NotificationState notificationState) {
        notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, notificationType, billingRunRepository, notificationState));
    }

    /**
     * Validates the values and currencies in the given list of BillingSummaryData.
     *
     * @param billingSummaryData the list of BillingSummaryData to be validated
     * @throws IllegalArgumentsProvidedException if any value or currency is null in the summary data
     */
    protected void validateValues(List<BillingSummaryData> billingSummaryData) {
        // if any value is null, return true, false otherwise
        boolean isAnyValueNull = billingSummaryData
                .stream()
                .map(BillingSummaryData::getValue)
                .anyMatch(Objects::isNull);

        if (isAnyValueNull) {
            log.error("Values in summery data must not be null");
            throw new IllegalArgumentsProvidedException("Values in summery data must not be null");
        }

        boolean isAnyCurrencyNull = billingSummaryData
                .stream()
                .map(BillingSummaryData::getValueCurrencyId)
                .anyMatch(Objects::isNull);

        if (isAnyCurrencyNull) {
            log.error("Currency in summery data must not be null;");
            throw new IllegalArgumentsProvidedException("Currency in summery data must not be null;");
        }
    }


    /**
     * Calculates the minimum period from date in the given list of billing detailed data.
     *
     * @param billingDetailedData the list of billing detailed data
     * @return the minimum period from date, or null if the list is empty or contains null values
     */
    protected LocalDate calculateMeterReadingPeriodFrom(List<BillingDetailedData> billingDetailedData) {
        if (CollectionUtils.isEmpty(billingDetailedData)) {
            return null;
        }

        return billingDetailedData
                .stream()
                .map(BillingDetailedData::getPeriodFrom)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Calculates the maximum period to date from the given list of billing detailed data.
     *
     * @param billingDetailedData the list of billing detailed data
     * @return the maximum period to date, or null if the list is empty or contains null values
     */
    protected LocalDate calculateMeterReadingPeriodTo(List<BillingDetailedData> billingDetailedData) {
        if (CollectionUtils.isEmpty(billingDetailedData)) {
            return null;
        }

        List<BillingDetailedData> filteredBillingDetailedData = billingDetailedData
                .stream()
                .filter(bdd -> Objects.nonNull(bdd.getPeriodFrom()))
                .toList();

        if (filteredBillingDetailedData.stream().anyMatch(bdd -> Objects.isNull(bdd.getPeriodTo()))) {
            return null;
        }

        return filteredBillingDetailedData
                .stream()
                .map(BillingDetailedData::getPeriodTo)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    protected List<InvoiceVatRateResponse> groupByVatRates(List<InvoiceDetailedDataAmountModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            return new ArrayList<>();
        }

        List<InvoiceVatRateResponse> context = new ArrayList<>();

        Map<BigDecimal, List<InvoiceDetailedDataAmountModel>> groupedByVatRate = models
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(Objects.requireNonNullElse(model.vatRatePercent(), BigDecimal.ZERO), model.pureAmount(), model.isMainCurrency(), model.alternativeCurrencyExchangeRate()))
                .collect(Collectors.groupingBy(InvoiceDetailedDataAmountModel::vatRatePercent));

        for (Map.Entry<BigDecimal, List<InvoiceDetailedDataAmountModel>> entry : groupedByVatRate.entrySet()) {
            BigDecimal vatRatePercent = entry.getKey();
            List<InvoiceDetailedDataAmountModel> amountModels = entry.getValue();
            BigDecimal totalAmount = EPBDecimalUtils
                    .calculateSummary(amountModels
                            .stream()
                            .map(model -> {
                                if (model.isMainCurrency()) {
                                    return model.pureAmount();
                                } else {
                                    return model.pureAmount().multiply(model.alternativeCurrencyExchangeRate());
                                }
                            })
                            .toList()
                    );
            context.add(new InvoiceVatRateResponse(vatRatePercent, totalAmount));
        }

        return context;
    }

    protected void calculateTotalAmountsAndSetToInvoice(Invoice invoice,
                                                        Currency mainCurrency,
                                                        List<InvoiceDetailedDataAmountModel> amountModels,
                                                        List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        BigDecimal totalAmountExcludingVat = EPBDecimalUtils.convertToCurrencyScale(
                EPBDecimalUtils
                        .calculateSummary(amountModels
                                .stream()
                                .map(model -> {
                                    if (model.isMainCurrency()) {
                                        return model.pureAmount();
                                    } else {
                                        return model.pureAmount().multiply(model.alternativeCurrencyExchangeRate());
                                    }
                                })
                                .collect(Collectors.toList()))
        );
        BigDecimal totalAmountOfVat = EPBDecimalUtils.convertToCurrencyScale(EPBDecimalUtils.calculateSummary(invoiceVatRateResponses.stream().map(InvoiceVatRateResponse::valueOfVat).toList()));
        BigDecimal totalAmountIncludingVat = EPBDecimalUtils.convertToCurrencyScale(totalAmountExcludingVat.add(totalAmountOfVat));
        BigDecimal totalAmountIncludingVatInOtherCurrency = EPBDecimalUtils.convertToCurrencyScale(calculateTotalAmountIncludingVatInOtherCurrency(invoice, totalAmountIncludingVat));

        invoice.setTotalAmountExcludingVat(totalAmountExcludingVat);
        invoice.setTotalAmountOfVat(totalAmountOfVat);
        invoice.setTotalAmountIncludingVat(totalAmountIncludingVat);
        invoice.setTotalAmountIncludingVatInOtherCurrency(totalAmountIncludingVatInOtherCurrency);

        Currency alternativeCurrency = Optional.ofNullable(mainCurrency.getAltCurrency())
                .orElseThrow(() -> new DomainEntityNotFoundException("Alternative currency not found for currency with id: [%s]".formatted(mainCurrency.getId())));

        invoice.setCurrencyIdInOtherCurrency(alternativeCurrency.getId());
        invoice.setTotalAmountOfVatInOtherCurrency(totalAmountIncludingVat.multiply(mainCurrency.getAltCurrencyExchangeRate()));
        invoice.setTotalAmountExcludingVatInOtherCurrency(totalAmountExcludingVat.multiply(mainCurrency.getAltCurrencyExchangeRate()));
    }

    protected void mapVatRatesAndSaveToDatabase(Long invoiceId, List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        if (CollectionUtils.isNotEmpty(invoiceVatRateResponses)) {
            List<InvoiceVatRateValue> invoiceVatRateValues = invoiceVatRateResponses
                    .stream()
                    .map(vat -> InvoiceVatRateValue
                            .builder()
                            .invoiceId(invoiceId)
                            .valueOfVat(vat.valueOfVat())
                            .amountExcludingVat(vat.amountExcludingVat())
                            .vatRatePercent(vat.vatRatePercent())
                            .build()).toList();
            invoiceVatRateValueRepository.saveAll(invoiceVatRateValues);
        }
    }

    private BigDecimal calculateTotalAmountIncludingVatInOtherCurrency(Invoice invoice, BigDecimal totalAmountIncludingVatInOtherCurrency) {
        return totalAmountIncludingVatInOtherCurrency.multiply(Objects.requireNonNullElse(invoice.getCurrencyExchangeRateOnInvoiceCreation(), BigDecimal.ONE));
    }

    /**
     * Defines the direct debit payload based on the given parameters.
     *
     * @param billingStartDate     The billing start date.
     * @param billingRun           The billing run.
     * @param billingGroupOptional The optional contract billing group.
     * @return The direct debit payload.
     * @throws DomainEntityNotFoundException     If the bank with the given ID is not found.
     * @throws ClientException                   If the billing run object type is not defined.
     * @throws DomainEntityNotFoundException     If the respective product contract details are not found.
     * @throws DomainEntityNotFoundException     If the respective service contract details are not found.
     * @throws DomainEntityNotFoundException     If the goods order is not found.
     * @throws DomainEntityNotFoundException     If the service order is not found.
     * @throws IllegalArgumentsProvidedException If the direct debit cannot be determined.
     * @throws DomainEntityNotFoundException     If the customer details are not found.
     */
    protected DirectDebitPayload defineDirectDebit(BillingRunObjectType billingRunObjectType,
                                                   LocalDate billingStartDate,
                                                   BillingRun billingRun,
                                                   Optional<ContractBillingGroup> billingGroupOptional) {
        if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
            Long bankId = billingRun.getBankId();
            Bank bank = bankRepository
                    .findById(bankId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
            return new DirectDebitPayload(billingRun.getDirectDebit(), bankId, bank.getBic(), billingRun.getIban());
        } else if (Boolean.FALSE.equals(billingRun.getDirectDebit())) {
            return new DirectDebitPayload(null, null, null, null);
        }

        if (billingGroupOptional.isPresent()) {
            ContractBillingGroup contractBillingGroup = billingGroupOptional.get();

            if (Boolean.TRUE.equals(contractBillingGroup.getDirectDebit())) {
                Long bankId = contractBillingGroup.getBankId();
                Bank bank = bankRepository
                        .findById(bankId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
                return new DirectDebitPayload(contractBillingGroup.getDirectDebit(), bankId, bank.getBic(), contractBillingGroup.getIban());
            }
        }

        switch (billingRunObjectType) {
            case PRODUCT_CONTRACT -> {
                ProductContractDetails productContractDetails = productContractDetailsRepository
                        .findRespectiveProductContractDetailsByProductContractId(billingStartDate, billingRun.getProductContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective product contract details not found for billing start date: [%s]".formatted(billingStartDate)));

                if (Boolean.TRUE.equals(productContractDetails.getDirectDebit())) {
                    Long bankId = productContractDetails.getBankId();
                    Bank bank = bankRepository
                            .findById(bankId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
                    return new DirectDebitPayload(productContractDetails.getDirectDebit(), bankId, bank.getBic(), productContractDetails.getIban());
                }
            }
            case SERVICE_CONTRACT -> {
                ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                        .findRespectiveServiceContractDetailsByServiceContractId(billingStartDate, billingRun.getServiceContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details for contract with id: [%s] not found;".formatted(billingRun.getServiceContractId())));

                if (Boolean.TRUE.equals(serviceContractDetails.getDirectDebit())) {
                    Long bankId = serviceContractDetails.getBankId();
                    Bank bank = bankRepository
                            .findById(bankId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
                    return new DirectDebitPayload(serviceContractDetails.getDirectDebit(), bankId, bank.getBic(), serviceContractDetails.getIban());
                }
            }
            case GOODS_ORDER -> {
                GoodsOrder goodsOrder = goodsOrderRepository
                        .findById(billingRun.getGoodsOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Goods order with id: [%s] not found;".formatted(billingRun.getGoodsOrderId())));

                if (Boolean.TRUE.equals(goodsOrder.getDirectDebit())) {
                    Long bankId = goodsOrder.getBankId();
                    Bank bank = bankRepository
                            .findById(bankId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
                    return new DirectDebitPayload(goodsOrder.getDirectDebit(), bankId, bank.getBic(), goodsOrder.getIban());
                }
            }
            case SERVICE_ORDER -> {
                ServiceOrder serviceOrder = serviceOrderRepository
                        .findById(billingRun.getServiceOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;".formatted(billingRun.getServiceOrderId())));

                if (Boolean.TRUE.equals(serviceOrder.getDirectDebit())) {
                    Long bankId = serviceOrder.getBankId();
                    Bank bank = bankRepository
                            .findById(bankId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
                    return new DirectDebitPayload(serviceOrder.getDirectDebit(), bankId, bank.getBic(), serviceOrder.getIban());
                }
            }
        }

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(billingRun.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details with id: [%s] not found;".formatted(billingRun.getCustomerDetailId())));

        if (Boolean.TRUE.equals(customerDetails.getDirectDebit())) {
            Long bankId = customerDetails.getBank().getId();
            Bank bank = bankRepository
                    .findById(bankId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(bankId)));
            return new DirectDebitPayload(customerDetails.getDirectDebit(), bankId, bank.getBic(), customerDetails.getIban());
        }

        return new DirectDebitPayload(null, null, null, null);
    }

    /**
     * Calculates the payment deadline for a service contract based on the billing start date and billing run.
     *
     * @param billingStartDate The start date of the billing period.
     * @param billingRun       The billing run associated with the service contract.
     * @return The payment deadline for the service contract.
     * @throws DomainEntityNotFoundException if the respective service contract details for the given service contract ID are not found.
     */
    protected LocalDate calculateServiceContractPaymentDeadline(LocalDate billingStartDate, BillingRun billingRun) {
        ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                .findRespectiveServiceContractDetailsByServiceContractId(billingStartDate, billingRun.getServiceContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details for contract with id: [%s] not found;".formatted(billingRun.getServiceContractId())));

        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, serviceContractDetails.getInvoicePaymentTermId(), serviceContractDetails.getInvoicePaymentTermValue());
    }

    /**
     * Calculates the payment deadline for a product contract based on the billing start date and billing run.
     *
     * @param billingStartDate The billing start date for which the payment deadline needs to be calculated.
     * @param billingRun       The billing run associated with the product contract.
     * @return The payment deadline calculated based on the invoice payment terms of the product contract.
     * @throws DomainEntityNotFoundException If the respective product contract details are not found for the given billing start date.
     */
    protected LocalDate calculateProductContractPaymentDeadline(LocalDate billingStartDate, BillingRun billingRun) {
        ProductContractDetails productContractDetails = productContractDetailsRepository
                .findRespectiveProductContractDetailsByProductContractId(billingStartDate, billingRun.getProductContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective product contract details not found for billing start date: [%s]".formatted(billingStartDate)));

        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, productContractDetails.getInvoicePaymentTermId(), productContractDetails.getInvoicePaymentTermValue());
    }

    protected LocalDate calculateProductContractPaymentDeadline(ProductContractDetails productContractDetails, BillingRun billingRun) {
        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, productContractDetails.getInvoicePaymentTermId(), productContractDetails.getInvoicePaymentTermValue());
    }

    protected LocalDate calculateServiceContractPaymentDeadline(ServiceContractDetails serviceContractDetails, BillingRun billingRun) {
        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, serviceContractDetails.getInvoicePaymentTermId(), serviceContractDetails.getInvoicePaymentTermValue());
    }

    protected LocalDate calculateGoodsOrderPaymentDeadline(BillingRun billingRun, GoodsOrder goodsOrder) {
        GoodsOrderPaymentTerm goodsOrderPaymentTerm = goodsOrderPaymentTermRepository
                .findActiveGoodsOrderPaymentTerm(goodsOrder.getId())
                .orElseThrow(() -> new ClientException("Goods order payment term is not defined, cannot calculate payment deadline", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED));

        switch (goodsOrderPaymentTerm.getType()) {
            case WORKING_DAYS -> {
                LocalDate paymentDeadline = billingRun.getInvoiceDate();
                long termIterator = goodsOrderPaymentTerm.getValue();
                return calculatePaymentDeadlineByCalendarType(
                        paymentDeadline,
                        termIterator,
                        goodsOrderPaymentTerm.getCalendarId()
                );
            }
            case CALENDAR_DAYS -> {
                return billingRun.getInvoiceDate().plusDays(goodsOrderPaymentTerm.getValue());
            }
            case CERTAIN_DAYS -> {
                int billingStartDateDayOfMonth = billingRun.getInvoiceDate().getDayOfMonth();
                int invoicePaymentTermCertainDay = Objects.requireNonNullElse(goodsOrderPaymentTerm.getValue(), 0);

                /*
                  if certain day is selected, check if billing start date day of month is more than selected day of month
                  in case if -> billing start date day of month is more than selected day of month, then calculate next certain day of next month
                  in other case -> set certain day of billing start date month
                  */
                return calculatePaymentDeadlineByCertainDays(billingRun, billingStartDateDayOfMonth, invoicePaymentTermCertainDay);
            }
            default -> throw new IllegalArgumentsProvidedException("Cannot determinate payment deadline");
        }
    }

    protected LocalDate calculateServiceOrderPaymentDeadline(BillingRun billingRun, ServiceOrder serviceOrder) {
        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, serviceOrder.getInvoicePaymentTermId(), serviceOrder.getInvoicePaymentTermValue());
    }

    /**
     * Calculates the payment deadline based on the given invoice payment terms.
     *
     * @param billingRun              The billing run for which to calculate the payment deadline.
     * @param invoicePaymentTermId    The ID of the invoice payment term.
     * @param invoicePaymentTermValue The value associated with the invoice payment term.
     * @return The calculated payment deadline as a LocalDate.
     * @throws DomainEntityNotFoundException     If the invoice payment term is not found.
     * @throws ClientException                   If the invoice payment term calendar type is not defined.
     * @throws IllegalArgumentsProvidedException If the payment deadline cannot be determined.
     */
    protected LocalDate calculatePaymentDeadlineByInvoicePaymentTerms(BillingRun billingRun,
                                                                      Long invoicePaymentTermId,
                                                                      Integer invoicePaymentTermValue) {
        InvoicePaymentTerms invoicePaymentTerms = invoicePaymentTermsRepository
                .findById(invoicePaymentTermId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice Payment Term not found with id: [%s]".formatted(invoicePaymentTermId)));

        if (Objects.isNull(invoicePaymentTerms.getCalendarType())) {
            log.error("Invoice payment term calendar type is not defined, cannot calculate payment deadline");
            throw new ClientException("Invoice payment term calendar type is not defined, cannot calculate payment deadline", ErrorCode.APPLICATION_ERROR);
        }

        switch (invoicePaymentTerms.getCalendarType()) {
            case WORKING_DAYS -> {
                LocalDate paymentDeadline = billingRun.getInvoiceDate();
                long termIterator = invoicePaymentTermValue;
                return calculatePaymentDeadlineByCalendarType(paymentDeadline, termIterator, invoicePaymentTerms.getCalendarId());
            }
            case CALENDAR_DAYS -> {
                LocalDate initialPaymentDeadline = billingRun.getInvoiceDate().plusDays(Objects.requireNonNullElse(invoicePaymentTermValue, 0));
                return adjustPaymentDeadlineByPaymentTermParameters(initialPaymentDeadline, invoicePaymentTerms);
            }
            case CERTAIN_DAYS -> {
                int billingStartDateDayOfMonth = billingRun.getInvoiceDate().getDayOfMonth();
                int invoicePaymentTermCertainDay = Objects.requireNonNullElse(invoicePaymentTermValue, 0);

                /*
                  if certain day is selected, check if billing start date day of month is more than selected day of month
                  in case if -> billing start date day of month is more than selected day of month, then calculate next certain day of next month
                  in other case -> set certain day of billing start date month
                  */
                LocalDate initialPaymentDeadline = calculatePaymentDeadlineByCertainDays(billingRun, billingStartDateDayOfMonth, invoicePaymentTermCertainDay);
                return adjustPaymentDeadlineByPaymentTermParameters(initialPaymentDeadline, invoicePaymentTerms);
            }
            default -> throw new IllegalArgumentsProvidedException("Cannot determinate payment deadline");
        }
    }

    private LocalDate adjustPaymentDeadlineByPaymentTermParameters(LocalDate initialPaymentDeadline, InvoicePaymentTerms invoicePaymentTerms) {
        Boolean excludeHolidays = invoicePaymentTerms.getExcludeHolidays();
        Boolean excludeWeekends = invoicePaymentTerms.getExcludeWeekends();

        Calendar calendar = calendarRepository
                .findById(invoicePaymentTerms.getCalendarId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar with id: [%s] not found".formatted(invoicePaymentTerms.getCalendarId())));

        List<DayOfWeek> weekends =
                Arrays.stream(
                                Objects.requireNonNullElse(calendar.getWeekends(), "")
                                        .split(";")
                        )
                        .filter(StringUtils::isNotBlank)
                        .map(DayOfWeek::valueOf)
                        .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarId(calendar.getId());
        List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHoliday).toList().stream().map(LocalDateTime::toLocalDate).toList();

        LocalDate targetDay = initialPaymentDeadline;
        if (Boolean.TRUE.equals(excludeHolidays) || Boolean.TRUE.equals(excludeWeekends)) {
            // define stop flag to avoid thread lock
            int stopFlag = 0;
            while (stopFlag < 1000) {
                if (Boolean.TRUE.equals(excludeHolidays) && Boolean.TRUE.equals(excludeWeekends)) {
                    if (holidayDates.contains(targetDay) || weekends.contains(targetDay.getDayOfWeek())) {
                        switch (invoicePaymentTerms.getDueDateChange()) {
                            case PREVIOUS_WORKING_DAY -> targetDay = targetDay.minusDays(1);
                            case NEXT_WORKING_DAY -> targetDay = targetDay.plusDays(1);
                        }
                        stopFlag++;
                    } else {
                        return targetDay;
                    }
                } else if (Boolean.TRUE.equals(excludeHolidays)) {
                    if (holidayDates.contains(targetDay)) {
                        switch (invoicePaymentTerms.getDueDateChange()) {
                            case PREVIOUS_WORKING_DAY -> targetDay = targetDay.minusDays(1);
                            case NEXT_WORKING_DAY -> targetDay = targetDay.plusDays(1);
                        }
                        stopFlag++;
                    } else {
                        return targetDay;
                    }
                } else {
                    if (weekends.contains(targetDay.getDayOfWeek())) {
                        switch (invoicePaymentTerms.getDueDateChange()) {
                            case PREVIOUS_WORKING_DAY -> targetDay = targetDay.minusDays(1);
                            case NEXT_WORKING_DAY -> targetDay = targetDay.plusDays(1);
                        }
                        stopFlag++;
                    } else {
                        return targetDay;
                    }
                }
            }

            throw new IllegalArgumentException("Cannot calculate payment deadline;");
        }

        return targetDay;
    }

    /**
     * Calculates the payment deadline based on certain days from the billing run.
     *
     * @param billingRun                   the billing run for which the payment deadline is being calculated
     * @param billingStartDateDayOfMonth   the day of the month when the billing run starts
     * @param invoicePaymentTermCertainDay the certain day of the month for the payment term
     * @return the payment deadline as a LocalDate object
     */
    protected LocalDate calculatePaymentDeadlineByCertainDays(BillingRun billingRun, int billingStartDateDayOfMonth, int invoicePaymentTermCertainDay) {
        if (billingStartDateDayOfMonth > invoicePaymentTermCertainDay) {
            LocalDate nextMonthFirstDay = billingRun.getInvoiceDate().with(TemporalAdjusters.firstDayOfNextMonth());
            int lastDayOfMonth = YearMonth.from(nextMonthFirstDay).atEndOfMonth().getDayOfMonth();
            return nextMonthFirstDay.withDayOfMonth(Math.min(lastDayOfMonth, invoicePaymentTermCertainDay));
        } else {
            int lastDayOfMonth = YearMonth.from(billingRun.getInvoiceDate()).atEndOfMonth().getDayOfMonth();
            return billingRun.getInvoiceDate().withDayOfMonth(Math.min(lastDayOfMonth, invoicePaymentTermCertainDay));
        }
    }

    /**
     * Calculates the payment deadline based on the calendar type.
     *
     * @param paymentDeadline The initial payment deadline.
     * @param termIterator    The number of terms to iterate.
     * @param calendarId      The ID of the calendar.
     * @return The calculated payment deadline.
     * @throws DomainEntityNotFoundException If the calendar with the specified ID is not found.
     */
    protected LocalDate calculatePaymentDeadlineByCalendarType(LocalDate paymentDeadline, long termIterator, Long calendarId) {
        Calendar calendar = calendarRepository
                .findById(calendarId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar with id: [%s] not found".formatted(calendarId)));

        List<DayOfWeek> weekends =
                Arrays.stream(
                                Objects.requireNonNullElse(calendar.getWeekends(), "")
                                        .split(";")
                        )
                        .filter(StringUtils::isNotBlank)
                        .map(DayOfWeek::valueOf)
                        .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendar.getId(), List.of(HolidayStatus.ACTIVE));
        List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHoliday).toList().stream().map(LocalDateTime::toLocalDate).toList();

        while (termIterator != 0) {
            LocalDate nextDay = paymentDeadline.plusDays(1);
            if (isWorkingDay(nextDay, weekends, holidayDates)) {
                termIterator--;
            }
            paymentDeadline = nextDay;
        }

        return paymentDeadline;
    }

    /**
     * Checks if the given date is a working day.
     *
     * @param date     the date to check
     * @param weekends the list of weekend days
     * @param holidays the list of holidays
     * @return true if the date is a working day (not a holiday and not a weekend), false otherwise
     */
    private boolean isWorkingDay(LocalDate date, List<DayOfWeek> weekends, List<LocalDate> holidays) {
        return !holidays.contains(date) && !weekends.contains(date.getDayOfWeek());
    }

    protected record DirectDebitPayload(
            Boolean directDebit,
            Long bankId,
            String bic,
            String iban
    ) {
    }

    /**
     * Calculates the billing run amounts based on the given parameters.
     *
     * @param billingRun         The billing run object containing the necessary data for calculation.
     * @param mainCurrency       The main currency object.
     * @param billingRunCurrency The currency in which the billing run is performed.
     * @param vatRate            The VAT rate to be applied.
     * @return A BillingRunAmountsPayload object containing the calculated amounts.
     */
    protected BillingRunAmountsPayload calculateBillingRunAmounts(BillingRun billingRun,
                                                                  Currency mainCurrency,
                                                                  Currency billingRunCurrency,
                                                                  VatRate vatRate) {
        BigDecimal calculatedTotalAmountExcludingVat;
        BigDecimal calculatedTotalAmountIncludingVat;
        BigDecimal calculatedAmountOfVat;
        BigDecimal calculatedTotalAmountExcludingVatInOtherCurrency;
        BigDecimal calculatedTotalAmountIncludingVatInOtherCurrency;
        BigDecimal calculatedAmountOfVatInOtherCurrency;
        if (Objects.equals(billingRun.getCurrencyId(), mainCurrency.getId())) {
            // advance payment is in main currency
            calculatedTotalAmountExcludingVat = billingRun.getAmountExcludingVat();
        } else {
            // advance payment is in other currency, exchange required here
            calculatedTotalAmountExcludingVat = billingRun.getAmountExcludingVat().multiply(billingRunCurrency.getAltCurrencyExchangeRate());
        }
        calculatedAmountOfVat = calculatedTotalAmountExcludingVat.multiply(vatRate.getValueInPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        calculatedTotalAmountIncludingVat = calculatedTotalAmountExcludingVat.add(calculatedAmountOfVat);

        calculatedTotalAmountExcludingVatInOtherCurrency = calculatedTotalAmountExcludingVat.multiply(mainCurrency.getAltCurrencyExchangeRate());
        calculatedTotalAmountIncludingVatInOtherCurrency = calculatedTotalAmountIncludingVat.multiply(mainCurrency.getAltCurrencyExchangeRate());
        calculatedAmountOfVatInOtherCurrency = calculatedAmountOfVat.multiply(mainCurrency.getAltCurrencyExchangeRate());

        return new BillingRunAmountsPayload(
                calculatedTotalAmountExcludingVat,
                calculatedTotalAmountIncludingVat,
                calculatedAmountOfVat,
                calculatedTotalAmountExcludingVatInOtherCurrency,
                calculatedTotalAmountIncludingVatInOtherCurrency,
                calculatedAmountOfVatInOtherCurrency
        );
    }

    protected record BillingRunAmountsPayload(BigDecimal calculatedTotalAmountExcludingVat,
                                              BigDecimal calculatedTotalAmountIncludingVat,
                                              BigDecimal calculatedAmountOfVat,
                                              BigDecimal calculatedTotalAmountExcludingVatInOtherCurrency,
                                              BigDecimal calculatedTotalAmountIncludingVatInOtherCurrency,
                                              BigDecimal calculatedAmountOfVatInOtherCurrency) {
    }
}
