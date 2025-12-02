package bg.energo.phoenix.service.billing.billingRunProcess;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunBillingGroup;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceStandardDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.repository.billing.billingRun.*;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceStandardDetailedDataRepository;
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
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class BillingRunManualInterimAdvancePaymentProcess extends AbstractBillingRunManualInvoice {
    private final InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository;

    public BillingRunManualInterimAdvancePaymentProcess(ServiceContractDetailsRepository serviceContractDetailsRepository,
                                                        InvoicePaymentTermsRepository invoicePaymentTermsRepository,
                                                        CalendarRepository calendarRepository,
                                                        HolidaysRepository holidaysRepository,
                                                        BankRepository bankRepository,
                                                        ProductContractDetailsRepository productContractDetailsRepository,
                                                        GoodsOrderRepository goodsOrderRepository,
                                                        ServiceOrderRepository serviceOrderRepository,
                                                        CustomerDetailsRepository customerDetailsRepository,
                                                        InvoiceRepository invoiceRepository,
                                                        VatRateRepository vatRateRepository,
                                                        CurrencyRepository currencyRepository,
                                                        InvoiceEventPublisher invoiceEventPublisher,
                                                        ServiceDetailsRepository serviceDetailsRepository,
                                                        InvoiceVatRateValueRepository invoiceVatRateValueRepository,
                                                        BillingRunBillingGroupRepository billingRunBillingGroupRepository,
                                                        ContractBillingGroupRepository contractBillingGroupRepository,
                                                        ContractPodRepository contractPodRepository,
                                                        ProductDetailsRepository productDetailsRepository,
                                                        BillingSummaryDataRepository billingSummaryDataRepository,
                                                        BillingDetailedDataRepository billingDetailedDataRepository,
                                                        GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository,
                                                        BillingRunManualInvoicesMapper billingRunManualInvoicesMapper,
                                                        ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                                        BillingRunRepository billingRunRepository,
                                                        NotificationEventPublisher notificationEventPublisher,
                                                        BillingErrorDataRepository billingErrorDataRepository,
                                                        InvoiceNumberService invoiceNumberService,
                                                        InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository) {
        super(
                serviceContractDetailsRepository,
                invoicePaymentTermsRepository,
                calendarRepository,
                holidaysRepository,
                bankRepository,
                productContractDetailsRepository,
                goodsOrderRepository,
                serviceOrderRepository,
                customerDetailsRepository,
                serviceDetailsRepository,
                invoiceVatRateValueRepository,
                contractBillingGroupRepository,
                productDetailsRepository,
                billingRunBillingGroupRepository,
                invoiceRepository,
                vatRateRepository,
                currencyRepository,
                invoiceEventPublisher,
                contractPodRepository,
                billingSummaryDataRepository,
                billingDetailedDataRepository,
                goodsOrderPaymentTermRepository,
                billingRunManualInvoicesMapper,
                contractTemplateDetailsRepository,
                billingRunRepository,
                notificationEventPublisher,
                billingErrorDataRepository,
                invoiceNumberService
        );
        this.invoiceStandardDetailedDataRepository = invoiceStandardDetailedDataRepository;
    }

    /**
     * Process Manual Interim Advance Payment for a Billing Run.
     * <p>
     * This method generates an interim advance payment based on the type of Billing Run.
     *
     * @param billingRun the Billing Run for which the interim advance payment needs to be generated
     * @throws IllegalArgumentsProvidedException if the Billing Run has an invalid object type for generating interim advance payment
     */
    @Transactional
    public void process(BillingRun billingRun) {
        try {
            BillingRunObjectType billingRunObjectType = BillingRunObjectType.defineInvoiceObjectType(billingRun);
            LocalDateTime billingRunStartTime = LocalDateTime.now();

            LocalDate currentDate = LocalDate.now();

            Optional<ContractTemplateDetail> templateDetailOptional = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIdAndDate(billingRun.getTemplateId(), currentDate);
            if (templateDetailOptional.isEmpty()) {
                log.error("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
                throw new IllegalArgumentsProvidedException("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
            }
            Long templateDetailId = templateDetailOptional.get().getId();

            switch (billingRunObjectType) {
                case ONLY_CUSTOMER ->
                        generateInterimAdvancePaymentForCustomer(billingRunStartTime, billingRun, templateDetailId);
                case PRODUCT_CONTRACT ->
                        generateInterimAdvancePaymentForProductContract(billingRunStartTime, billingRun, templateDetailId);
                case SERVICE_CONTRACT ->
                        generateInterimAdvancePaymentForServiceContract(billingRunStartTime, billingRun, templateDetailId);
                case GOODS_ORDER, SERVICE_ORDER ->
                        throw new IllegalArgumentsProvidedException("You cannot generate interim advance payment for order");
            }

            billingRun.setStatus(BillingStatus.DRAFT);
            billingRunRepository.save(billingRun);
        } catch (Exception e) {
            publishNotification(billingRun.getId(), NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
            throw e;
        }
    }

    /**
     * Generates an interim advance payment invoice for a customer based on the billing run details.
     *
     * @param billingRunStartTime The start time of the billing run.
     * @param billingRun          The billing run details.
     */
    private void generateInterimAdvancePaymentForCustomer(LocalDateTime billingRunStartTime,
                                                          BillingRun billingRun, Long templateDetailId) {
        LocalDate invoiceDate = billingRun.getInvoiceDate();
        if (Objects.nonNull(invoiceDate)) {
            if (invoiceDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentsProvidedException("Invoice date must not be before current date;");
            }
        }

        LocalDate invoiceDueDate = billingRun.getInvoiceDueDate();
        if (Objects.isNull(invoiceDueDate)) {
            throw new IllegalArgumentsProvidedException("Invoice due date must be defined;");
        }

        String basisForIssuing = billingRun.getBasisForIssuing();
        if (StringUtils.isBlank(basisForIssuing)) {
            throw new IllegalArgumentsProvidedException("Basis for issuing must be defined;");
        }

        if (Objects.isNull(billingRun.getInterestRateId())) {
            throw new IllegalArgumentsProvidedException("Applicable interest rate must be defined;");
        }

        Currency billingRunCurrency = currencyRepository
                .findById(billingRun.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing Run currency not found"));

        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

        if (!billingRunCurrency.equals(mainCurrency)) {
            if (!billingRunCurrency.getAltCurrency().equals(mainCurrency)) {
                throw new IllegalArgumentsProvidedException("Alternative currency of the billing run is not main currency, cannot calculate amounts");
            }
        }

        Long vatRateId = billingRun.getVatRateId();
        VatRate vatRate;
        if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
            vatRate = vatRateRepository
                    .findGlobalVatRate(billingRunStartTime.toLocalDate(), PageRequest.of(0, 1))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found for current date"));
        } else {
            if (Objects.isNull(vatRateId)) {
                throw new IllegalArgumentsProvidedException("Vat Rate must be defined;");
            } else {
                vatRate = vatRateRepository
                        .findById(vatRateId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(vatRateId)));
            }
        }

        BillingRunAmountsPayload billingRunAmountsPayload = calculateBillingRunAmounts(billingRun,
                mainCurrency,
                billingRunCurrency,
                vatRate);

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(billingRun.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer detail with id: [%s] not found;".formatted(billingRun.getCustomerDetailId())));

        DirectDebitPayload directDebitPayload = new DirectDebitPayload(null, null, null, null);
        if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
            if (Objects.isNull(billingRun.getBankId())) {
                throw new IllegalArgumentsProvidedException("Bank must be defined;");
            }
            Bank bank = bankRepository
                    .findById(billingRun.getBankId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found;".formatted(billingRun.getBankId())));

            directDebitPayload = new DirectDebitPayload(billingRun.getDirectDebit(), billingRun.getBankId(), bank.getBic(), billingRun.getIban());
        } else if (Boolean.FALSE.equals(billingRun.getDirectDebit())) {
            directDebitPayload = new DirectDebitPayload(null, null, null, null);
        } else {
            if (Boolean.TRUE.equals(customerDetails.getDirectDebit())) {
                Optional<Bank> customerBankOptional = Optional.ofNullable(customerDetails.getBank());

                if (customerBankOptional.isPresent()) {
                    Bank bank = customerBankOptional.get();
                    directDebitPayload = new DirectDebitPayload(customerDetails.getDirectDebit(), bank.getId(), bank.getBic(), customerDetails.getIban());
                }
            }
        }

        LocalDate issuingForTheMonth = calculateIssuingForTheMonth(billingRun);

        Invoice invoice = Invoice.builder()
                .billingId(billingRun.getId())
                .invoiceDate(billingRun.getInvoiceDate())
                .invoiceStatus(InvoiceStatus.DRAFT)
                .invoiceDocumentType(InvoiceDocumentType.INVOICE)
                .taxEventDate(billingRun.getTaxEventDate())
                .paymentDeadline(invoiceDueDate)
                .invoiceType(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)
                .basisForIssuing(basisForIssuing)
                .bankId(directDebitPayload.bankId())
                .iban(directDebitPayload.iban())
                .directDebit(directDebitPayload.directDebit())
                .customerId(customerDetails.getCustomerId())
                .customerDetailId(billingRun.getCustomerDetailId())
                .customerCommunicationId(billingRun.getCustomerCommunicationId())
                .billingId(billingRun.getId())
                .currencyId(mainCurrency.getId())
                .currencyIdInOtherCurrency(mainCurrency.getAltCurrencyId())
                .incomeAccountNumber(Optional.ofNullable(billingRun.getNumberOfIncomeAccount()).orElseThrow(() -> new IllegalArgumentsProvidedException("Income account number must be defined;")))
                .costCenterControllingOrder(Optional.ofNullable(billingRun.getCostCenterControllingOrder()).orElseThrow(() -> new IllegalArgumentsProvidedException("Cost center/Controlling order must be defined;")))
                .totalAmountExcludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedTotalAmountExcludingVat()))
                .totalAmountIncludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedTotalAmountIncludingVat()))
                .totalAmountOfVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedAmountOfVat()))
                .totalAmountIncludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedTotalAmountIncludingVatInOtherCurrency()))
                .totalAmountExcludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedTotalAmountExcludingVatInOtherCurrency()))
                .totalAmountOfVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmountsPayload.calculatedAmountOfVatInOtherCurrency()))
                .currencyExchangeRateOnInvoiceCreation(billingRunCurrency.getAltCurrencyExchangeRate())
                .alternativeRecipientCustomerDetailId(billingRun.getCustomerDetailId())
                .accountPeriodId(billingRun.getAccountingPeriodId())
                .interestRateId(billingRun.getInterestRateId())
                .templateDetailId(templateDetailId)
                .contractType(billingRun.getPrefixType() == PrefixType.PRODUCT ? ContractType.PRODUCT_CONTRACT : ContractType.SERVICE_CONTRACT)
                .isDeducted(false)
                .deductionFromType(billingRun.getDeductionFrom())
                .issuingForTheMonth(issuingForTheMonth.withDayOfMonth(1))
                .issuingForPaymentTermDate(invoiceDueDate) // todo
                .build();

        List<Invoice> invoices = new ArrayList<>();
        for (IssuedSeparateInvoice issuedSeparateInvoice : billingRun.getIssuedSeparateInvoices()) {
            Invoice invoiceClone = invoice.clone();
            invoiceClone.setInvoiceSlot(issuedSeparateInvoice.name());
            invoiceClone.setPodId(null);

            invoices.add(invoiceClone);
        }

        persistInvoices(invoices, vatRate, billingRunAmountsPayload);
    }

    /**
     * Generates interim advance payment invoices for a product contract billing run.
     *
     * @param billingRunStartTime The start time of the billing run.
     * @param billingRun          The billing run object.
     * @param templateDetailId
     */
    private void generateInterimAdvancePaymentForProductContract(LocalDateTime billingRunStartTime, BillingRun billingRun, Long templateDetailId) {
        LocalDate billingRunStartDate = billingRunStartTime.toLocalDate();
        BillingRunObjectType billingRunObjectType = BillingRunObjectType.defineInvoiceObjectType(billingRun);

        List<BillingRunBillingGroup> billingRunBillingGroups = billingRunBillingGroupRepository
                .findByBillingRunId(billingRun.getId(), EntityStatus.ACTIVE);

        ProductContractDetails productContractDetails = productContractDetailsRepository
                .findRespectiveProductContractDetailsByProductContractId(billingRunStartDate, billingRun.getProductContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective product contract details not found for service contract with id: [%s] and date: [%s]".formatted(billingRun.getProductContractId(), billingRunStartDate)));

        ProductDetails productDetails = productDetailsRepository
                .findById(productContractDetails.getProductDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Product Details with id: [%s] not found;".formatted(productContractDetails.getProductDetailId())));

        String basisForIssuing = billingRun.getBasisForIssuing();
        if (StringUtils.isBlank(basisForIssuing)) {
            throw new IllegalArgumentsProvidedException("Basis for issuing must be defined;");
        }

        Currency billingRunCurrency = currencyRepository
                .findById(billingRun.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing Run currency not found"));

        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

        LocalDate invoiceDueDate;
        InvoiceDueDateType invoiceDueDateType = Optional.ofNullable(billingRun.getInvoiceDueDateType()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date type is null;"));
        switch (invoiceDueDateType) {
            case ACCORDING_TO_THE_CONTRACT ->
                    invoiceDueDate = calculateProductContractPaymentDeadline(billingRun.getInvoiceDate(), billingRun);
            case DATE ->
                    invoiceDueDate = Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date must be defined;"));
            default -> throw new IllegalArgumentsProvidedException("Cannot define invoice due date type");
        }

        Long vatRateId = billingRun.getVatRateId();
        VatRate vatRate;
        if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
            vatRate = vatRateRepository
                    .findGlobalVatRate(billingRunStartTime.toLocalDate(), PageRequest.of(0, 1))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found for current date"));
        } else {
            if (Objects.isNull(vatRateId)) {
                if (Boolean.TRUE.equals(productDetails.getGlobalVatRate())) {
                    vatRate = vatRateRepository
                            .findGlobalVatRate(billingRunStartTime.toLocalDate(), PageRequest.of(0, 1))
                            .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found for current date"));
                } else {
                    vatRate = productDetails.getVatRate();
                }
            } else {
                vatRate = vatRateRepository
                        .findById(vatRateId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(vatRateId)));
            }
        }

        BillingRunAmountsPayload billingRunAmounts = calculateBillingRunAmounts(billingRun, mainCurrency, billingRunCurrency, vatRate);

        String costCenterControllingOrder = Optional.ofNullable(billingRun.getCostCenterControllingOrder()).orElseGet(productDetails::getCostCenterControllingOrder);
        String incomeAccountNumber = Optional.ofNullable(billingRun.getNumberOfIncomeAccount()).orElseGet(productDetails::getIncomeAccountNumber);

        LocalDate issuingForTheMonth = calculateIssuingForTheMonth(billingRun);

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(billingRun.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer detail with id: [%s] not found;".formatted(billingRun.getCustomerDetailId())));

        List<Invoice> invoices = new ArrayList<>();
        for (BillingRunBillingGroup billingRunBillingGroup : billingRunBillingGroups) {
            ContractBillingGroup contractBillingGroup = contractBillingGroupRepository
                    .findByIdAndStatusIn(billingRunBillingGroup.getBillingGroupId(), List.of(EntityStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract Billing group with id: [%s] does not exists".formatted(billingRunBillingGroup.getBillingGroupId())));

            DirectDebitPayload directDebitPayload = defineDirectDebit(billingRunObjectType, billingRunStartDate, billingRun, Optional.of(contractBillingGroup));

            Invoice invoice = Invoice.builder()
                    .billingId(billingRun.getId())
                    .invoiceDate(billingRun.getInvoiceDate())
                    .invoiceStatus(InvoiceStatus.DRAFT)
                    .invoiceDocumentType(InvoiceDocumentType.INVOICE)
                    .taxEventDate(billingRun.getTaxEventDate())
                    .paymentDeadline(invoiceDueDate)
                    .invoiceType(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)
                    .basisForIssuing(basisForIssuing)
                    .bankId(directDebitPayload.bankId())
                    .iban(directDebitPayload.iban())
                    .directDebit(directDebitPayload.directDebit())
                    .customerId(customerDetails.getCustomerId())
                    .customerDetailId(billingRun.getCustomerDetailId())
                    .customerCommunicationId(ObjectUtils.defaultIfNull(contractBillingGroup.getBillingCustomerCommunicationId(), billingRun.getCustomerCommunicationId()))
                    .billingId(billingRun.getId())
                    .currencyId(mainCurrency.getId())
                    .currencyIdInOtherCurrency(mainCurrency.getAltCurrencyId())
                    .costCenterControllingOrder(costCenterControllingOrder)
                    .incomeAccountNumber(incomeAccountNumber)
                    .productContractId(billingRun.getProductContractId())
                    .productContractDetailId(productContractDetails.getId())
                    .productDetailId(productContractDetails.getProductDetailId())
                    .contractBillingGroupId(contractBillingGroup.getId())
                    .totalAmountExcludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountExcludingVat()))
                    .totalAmountIncludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountIncludingVat()))
                    .totalAmountOfVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedAmountOfVat()))
                    .totalAmountIncludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountIncludingVatInOtherCurrency()))
                    .totalAmountExcludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountExcludingVatInOtherCurrency()))
                    .totalAmountOfVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedAmountOfVatInOtherCurrency())).currencyExchangeRateOnInvoiceCreation(billingRunCurrency.getAltCurrencyExchangeRate())
                    .alternativeRecipientCustomerDetailId(Optional.ofNullable(contractBillingGroup.getAlternativeRecipientCustomerDetailId()).orElse(billingRun.getCustomerDetailId()))
                    .accountPeriodId(billingRun.getAccountingPeriodId())
                    .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), productContractDetails.getApplicableInterestRate()))
                    .templateDetailId(templateDetailId)
                    .contractType(ContractType.PRODUCT_CONTRACT)
                    .isDeducted(false)
                    .deductionFromType(billingRun.getDeductionFrom())
                    .issuingForTheMonth(issuingForTheMonth.withDayOfMonth(1))
                    .issuingForPaymentTermDate(invoiceDueDate) // todo
                    .build();

            Boolean separateInvoiceForEachPod = contractBillingGroup.getSeparateInvoiceForEachPod();
            if (Boolean.TRUE.equals(separateInvoiceForEachPod)) {
                List<Long> productContractPointOfDeliveryIdsForBillingGroup = contractPodRepository
                        .findProductContractPointOfDeliveriesByBillingGroupAndBillingDate(contractBillingGroup.getId(), billingRun.getInvoiceDate());

                for (IssuedSeparateInvoice issuedSeparateInvoice : billingRun.getIssuedSeparateInvoices()) {
                    for (Long podId : productContractPointOfDeliveryIdsForBillingGroup) {
                        Invoice invoiceClone = invoice.clone();
                        invoiceClone.setInvoiceSlot(issuedSeparateInvoice.name());
                        invoiceClone.setPodId(podId);

                        invoices.add(invoiceClone);
                    }
                }

                if (productContractPointOfDeliveryIdsForBillingGroup.isEmpty()) {
                    throw new IllegalArgumentsProvidedException("Billing Group with number: [%s] has not active point of deliveries, cannot generate Advance Payments".formatted(contractBillingGroup.getGroupNumber()));
                }
            } else {
                for (IssuedSeparateInvoice issuedSeparateInvoice : billingRun.getIssuedSeparateInvoices()) {
                    Invoice invoiceClone = invoice.clone();
                    invoiceClone.setInvoiceSlot(issuedSeparateInvoice.name());
                    invoiceClone.setPodId(null);

                    invoices.add(invoiceClone);
                }
            }
        }

        persistInvoices(invoices, vatRate, billingRunAmounts);
    }

    /**
     * Generates interim advance payment for a service contract.
     *
     * @param billingRunStartTime the start time of the billing run
     * @param billingRun          the billing run object
     * @param templateDetailId
     * @throws DomainEntityNotFoundException     if the service contract details, service details, billing run
     *                                           currency, main currency, vat rate, or global vat rate
     *                                           cannot be found
     * @throws IllegalArgumentsProvidedException if the basis for issuing, applicable interest rate,
     *                                           invoice due date type, or invoice due date is not provided
     */
    private void generateInterimAdvancePaymentForServiceContract(LocalDateTime billingRunStartTime, BillingRun
            billingRun, Long templateDetailId) {
        LocalDate billingRunStartDate = billingRunStartTime.toLocalDate();
        BillingRunObjectType billingRunObjectType = BillingRunObjectType.defineInvoiceObjectType(billingRun);

        ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                .findRespectiveServiceContractDetailsByServiceContractId(billingRunStartDate, billingRun.getServiceContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details not found for service contract with id: [%s] and date: [%s]".formatted(billingRun.getServiceContractId(), billingRunStartDate)));

        ServiceDetails serviceDetails = serviceDetailsRepository
                .findById(serviceContractDetails.getServiceDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found;".formatted(serviceContractDetails.getServiceDetailId())));

        String basisForIssuing = billingRun.getBasisForIssuing();
        if (StringUtils.isBlank(basisForIssuing)) {
            throw new IllegalArgumentsProvidedException("Basis for issuing must be defined;");
        }

        Currency billingRunCurrency = currencyRepository
                .findById(billingRun.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing Run currency not found"));

        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

        LocalDate invoiceDueDate;
        InvoiceDueDateType invoiceDueDateType = Optional.ofNullable(billingRun.getInvoiceDueDateType()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date type is null;"));
        switch (invoiceDueDateType) {
            case ACCORDING_TO_THE_CONTRACT ->
                    invoiceDueDate = calculateServiceContractPaymentDeadline(billingRun.getInvoiceDate(), billingRun);
            case DATE ->
                    invoiceDueDate = Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date must be defined;"));
            default -> throw new IllegalArgumentsProvidedException("Cannot define invoice due date type");
        }

        DirectDebitPayload directDebitPayload = defineDirectDebit(billingRunObjectType, billingRunStartDate, billingRun, Optional.empty());

        Long vatRateId = billingRun.getVatRateId();
        VatRate vatRate;
        if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
            vatRate = vatRateRepository
                    .findGlobalVatRate(billingRunStartTime.toLocalDate(), PageRequest.of(0, 1))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found for current date"));
        } else {
            if (Objects.isNull(vatRateId)) {
                if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                    vatRate = vatRateRepository
                            .findGlobalVatRate(billingRunStartTime.toLocalDate(), PageRequest.of(0, 1))
                            .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found for current date"));
                } else {
                    vatRate = serviceDetails.getVatRate();
                }
            } else {
                vatRate = vatRateRepository
                        .findById(vatRateId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(vatRateId)));
            }
        }

        BillingRunAmountsPayload billingRunAmounts = calculateBillingRunAmounts(billingRun, mainCurrency, billingRunCurrency, vatRate);

        String costCenterControllingOrder = Optional.ofNullable(billingRun.getCostCenterControllingOrder()).orElseGet(serviceDetails::getCostCenterControllingOrder);
        String incomeAccountNumber = Optional.ofNullable(billingRun.getNumberOfIncomeAccount()).orElseGet(serviceDetails::getIncomeAccountNumber);

        LocalDate issuingForTheMonth = calculateIssuingForTheMonth(billingRun);

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(billingRun.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer detail with id: [%s] not found;".formatted(billingRun.getCustomerDetailId())));

        Invoice invoice = Invoice.builder()
                .billingId(billingRun.getId())
                .invoiceDate(billingRun.getInvoiceDate())
                .invoiceStatus(InvoiceStatus.DRAFT)
                .invoiceDocumentType(InvoiceDocumentType.INVOICE)
                .taxEventDate(billingRun.getTaxEventDate())
                .paymentDeadline(invoiceDueDate)
                .invoiceType(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)
                .basisForIssuing(basisForIssuing)
                .bankId(directDebitPayload.bankId())
                .iban(directDebitPayload.iban())
                .directDebit(directDebitPayload.directDebit())
                .customerId(customerDetails.getCustomerId())
                .customerDetailId(billingRun.getCustomerDetailId())
                .customerCommunicationId(billingRun.getCustomerCommunicationId())
                .billingId(billingRun.getId())
                .currencyId(mainCurrency.getId())
                .currencyIdInOtherCurrency(mainCurrency.getAltCurrencyId())
                .costCenterControllingOrder(costCenterControllingOrder)
                .incomeAccountNumber(incomeAccountNumber)
                .serviceContractId(billingRun.getServiceContractId())
                .serviceContractDetailId(serviceContractDetails.getId())
                .serviceDetailId(serviceContractDetails.getServiceDetailId())
                .totalAmountExcludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountExcludingVat()))
                .totalAmountIncludingVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountIncludingVat()))
                .totalAmountOfVat(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedAmountOfVat()))
                .totalAmountIncludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountIncludingVatInOtherCurrency()))
                .totalAmountExcludingVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedTotalAmountExcludingVatInOtherCurrency()))
                .totalAmountOfVatInOtherCurrency(EPBDecimalUtils.convertToCurrencyScale(billingRunAmounts.calculatedAmountOfVatInOtherCurrency())).currencyExchangeRateOnInvoiceCreation(billingRunCurrency.getAltCurrencyExchangeRate())
                .alternativeRecipientCustomerDetailId(billingRun.getCustomerDetailId())
                .accountPeriodId(billingRun.getAccountingPeriodId())
                .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceContractDetails.getApplicableInterestRate()))
                .templateDetailId(templateDetailId)
                .contractType(ContractType.SERVICE_CONTRACT)
                .isDeducted(false)
                .deductionFromType(billingRun.getDeductionFrom())
                .issuingForTheMonth(issuingForTheMonth.withDayOfMonth(1))
                .issuingForPaymentTermDate(invoiceDueDate) // todo
                .build();

        List<Invoice> invoices = new ArrayList<>();

        for (IssuedSeparateInvoice issuedSeparateInvoice : billingRun.getIssuedSeparateInvoices()) {
            Invoice invoiceClone = invoice.clone();
            invoiceClone.setInvoiceSlot(issuedSeparateInvoice.name());
            invoiceClone.setPodId(null);

            invoices.add(invoiceClone);
        }

        persistInvoices(invoices, vatRate, billingRunAmounts);
    }

    /**
     * Persists invoices with corresponding VAT rate values.
     *
     * @param invoices          the list of invoices to be persisted
     * @param vatRate           the VAT rate applied to the invoices
     * @param billingRunAmounts the billing run amounts payload containing calculated amounts
     */
    private void persistInvoices(List<Invoice> invoices,
                                 VatRate vatRate,
                                 BillingRunAmountsPayload billingRunAmounts) {
        List<Invoice> persistedInvoices = invoiceRepository.saveAll(invoices);

        for (Invoice inv : persistedInvoices) {
            InvoiceVatRateValue vatRateValue = invoiceVatRateValueRepository.save(
                    InvoiceVatRateValue
                            .builder()
                            .invoiceId(inv.getId())
                            .amountExcludingVat(billingRunAmounts.calculatedTotalAmountExcludingVat())
                            .vatRatePercent(vatRate.getValueInPercent())
                            .valueOfVat(billingRunAmounts.calculatedAmountOfVat())
                            .build()
            );

            invoiceStandardDetailedDataRepository.save(
                    new InvoiceStandardDetailedData(
                            InvoiceStandardDetailType.INTERIM_EXACT_AMOUNT,
                            inv.getId(),
                            inv,
                            inv.getCustomerDetailId(),
                            inv.getCostCenterControllingOrder(),
                            inv.getIncomeAccountNumber(),
                            inv.getCurrencyId(),
                            inv.getCurrencyIdInOtherCurrency(),
                            inv.getTotalAmountExcludingVat(),
                            inv.getTotalAmountIncludingVat(),
                            inv.getTotalAmountOfVat(),
                            vatRate.getId(),
                            vatRateValue.getVatRatePercent(),
                            inv.getTotalAmountIncludingVatInOtherCurrency(),
                            inv.getTotalAmountIncludingVatInOtherCurrency(),
                            inv.getTotalAmountOfVatInOtherCurrency()
                    )
            );
        }

        invoiceNumberService.fillInvoiceNumber(persistedInvoices);
    }

    private LocalDate calculateIssuingForTheMonth(BillingRun billingRun) {
        LocalDate issuingForTheMonth = billingRun.getInvoiceDate();
        switch (billingRun.getIssuingForTheMonthToCurrent()) {
            case MINUS_ONE -> issuingForTheMonth = issuingForTheMonth.minusMonths(1);
            case PLUS_ONE -> issuingForTheMonth = issuingForTheMonth.plusMonths(1);
            case PLUS_TWO -> issuingForTheMonth = issuingForTheMonth.plusMonths(2);
            case PLUS_TWELVE -> issuingForTheMonth = issuingForTheMonth.plusMonths(12);
        }
        return issuingForTheMonth;
    }
}
