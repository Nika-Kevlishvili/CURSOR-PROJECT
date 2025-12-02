package bg.energo.phoenix.service.billing.billingRunProcess;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunInvoices;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceRelatedInvoice;
import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
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
import bg.energo.phoenix.model.enums.billing.billings.DocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.billing.billingRun.*;
import bg.energo.phoenix.repository.billing.invoice.*;
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
import bg.energo.phoenix.service.billing.billingRunProcess.mapper.BillingRunManualInvoicesMapper;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class BillingRunManualDebitOrCreditNoteProcess extends AbstractBillingRunManualInvoice {
    private final BillingRunInvoicesRepository billingRunInvoicesRepository;
    private final ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository;
    private final ManualDebitOrCreditNoteInvoiceDetailedDataRepository manualDebitOrCreditNoteInvoiceDetailedDataRepository;
    private final InvoiceRelatedInvoiceRepository invoiceRelatedInvoiceRepository;

    public BillingRunManualDebitOrCreditNoteProcess(ServiceContractDetailsRepository serviceContractDetailsRepository,
                                                    InvoicePaymentTermsRepository invoicePaymentTermsRepository,
                                                    CalendarRepository calendarRepository,
                                                    HolidaysRepository holidaysRepository,
                                                    BankRepository bankRepository,
                                                    ProductContractDetailsRepository productContractDetailsRepository,
                                                    GoodsOrderRepository goodsOrderRepository,
                                                    ServiceOrderRepository serviceOrderRepository,
                                                    CustomerDetailsRepository customerDetailsRepository,
                                                    ServiceDetailsRepository serviceDetailsRepository,
                                                    InvoiceVatRateValueRepository invoiceVatRateValueRepository,
                                                    ContractBillingGroupRepository contractBillingGroupRepository,
                                                    ProductDetailsRepository productDetailsRepository,
                                                    BillingRunBillingGroupRepository billingRunBillingGroupRepository,
                                                    InvoiceRepository invoiceRepository,
                                                    VatRateRepository vatRateRepository,
                                                    CurrencyRepository currencyRepository,
                                                    InvoiceEventPublisher invoiceEventPublisher,
                                                    ContractPodRepository contractPodRepository,
                                                    BillingRunInvoicesRepository billingRunInvoicesRepository,
                                                    BillingSummaryDataRepository billingSummaryDataRepository,
                                                    BillingDetailedDataRepository billingDetailedDataRepository,
                                                    BillingRunManualInvoicesMapper billingRunManualInvoicesMapper,
                                                    InvoiceRelatedInvoiceRepository invoiceRelatedInvoiceRepository,
                                                    GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository,
                                                    ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository,
                                                    ManualDebitOrCreditNoteInvoiceDetailedDataRepository manualDebitOrCreditNoteInvoiceDetailedDataRepository,
                                                    ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                                    BillingRunRepository billingRunRepository,
                                                    NotificationEventPublisher notificationEventPublisher,
                                                    BillingErrorDataRepository billingErrorDataRepository,
                                                    InvoiceNumberService invoiceNumberService) {
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
        this.billingRunInvoicesRepository = billingRunInvoicesRepository;
        this.invoiceRelatedInvoiceRepository = invoiceRelatedInvoiceRepository;
        this.manualDebitOrCreditNoteInvoiceSummaryDataRepository = manualDebitOrCreditNoteInvoiceSummaryDataRepository;
        this.manualDebitOrCreditNoteInvoiceDetailedDataRepository = manualDebitOrCreditNoteInvoiceDetailedDataRepository;
    }

    @Override
    public void process(BillingRun billingRun) {
        try {
            LocalDate billingStartDate = LocalDate.now();

            LocalDate currentDate = LocalDate.now();

            Optional<ContractTemplateDetail> templateDetailOptional = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIdAndDate(billingRun.getTemplateId(), currentDate);
            if (templateDetailOptional.isEmpty()) {
                log.error("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
                throw new IllegalArgumentsProvidedException("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
            }
            Long templateDetailId = templateDetailOptional.get().getId();

            List<Pair<BillingRunInvoices, Invoice>> billingRunInvoices = billingRunInvoicesRepository.findBillingRunInvoicesAndMapWithInvoices(billingRun.getId())
                    .stream()
                    .map((entity) -> Pair.of((BillingRunInvoices) entity[0], (Invoice) entity[1]))
                    .toList();
            if (CollectionUtils.isEmpty(billingRunInvoices)) {
                throw new IllegalArgumentsProvidedException("Invoices is empty for presented billing run, cannot generate debit/credit note");
            }

            boolean isDirectInvoice = billingRunInvoices.size() == 1;
            if (isDirectInvoice) {
                generateDirectInvoiceDebitOrCreditNote(billingStartDate, billingRun, billingRunInvoices, templateDetailId);
            } else {
                generateNonDirectInvoiceDebitOrCreditNote(billingStartDate, billingRun, billingRunInvoices, templateDetailId);
            }

            billingRun.setStatus(BillingStatus.DRAFT);
            billingRunRepository.save(billingRun);
        } catch (Exception e) {
            publishNotification(billingRun.getId(), NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
            throw e;
        }
    }

    private void generateDirectInvoiceDebitOrCreditNote(LocalDate billingRunStartDate,
                                                        BillingRun billingRun,
                                                        List<Pair<BillingRunInvoices, Invoice>> billingRunInvoices, Long templateDetailId) {
        Pair<BillingRunInvoices, Invoice> billingRunInvoicesInvoicePair = billingRunInvoices.get(0);
        Invoice billingRunInvoice = billingRunInvoicesInvoicePair.getValue();
        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(billingRunInvoice);

        DocumentType documentType = Optional.ofNullable(billingRun.getDocumentType())
                .orElseThrow(() -> new IllegalArgumentsProvidedException("Document type is not defined, cannot generate debit/credit note"));

        // finding all summery data by billing run
        List<BillingSummaryData> billingSummaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
        List<BillingDetailedData> billingDetailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId());
        validateValues(billingSummaryData);

        Set<Long> persistedInvoicesContext = new HashSet<>();
        switch (invoiceObjectType) {
            case PRODUCT_CONTRACT -> {
                ProductContractDetails productContractDetails = productContractDetailsRepository
                        .findRespectiveProductContractDetailsByProductContractId(billingRunStartDate, billingRunInvoice.getProductContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective Product Contract not found;"));

                ProductDetails productDetails = productDetailsRepository
                        .findById(productContractDetails.getProductDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Product Details with id: [%s] not found;".formatted(productContractDetails.getProductDetailId())));

                CustomerDetails customerDetails = customerDetailsRepository
                        .findById(productContractDetails.getCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(productContractDetails.getCustomerDetailId())));

                Currency mainCurrency = currencyRepository
                        .findMainCurrencyNowAndActive()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : ObjectUtils.defaultIfNull(billingRun.getInvoiceDueDate(), calculateProductContractPaymentDeadline(productContractDetails, billingRun));

                LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                ContractBillingGroup contractBillingGroup = contractBillingGroupRepository
                        .findById(billingRunInvoice.getContractBillingGroupId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Contract Billing Group with id: [%s] not found".formatted(billingRunInvoice.getContractBillingGroupId())));

                DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun, productContractDetails, customerDetails, Optional.of(contractBillingGroup));

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), productDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), productDetails.getCostCenterControllingOrder());

                Optional<VatRate> globalVatRateOptional = vatRateRepository
                        .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                VatRate defaultVatRate;
                if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else if (Objects.nonNull(billingRun.getVatRateId())) {
                    defaultVatRate = vatRateRepository
                            .findById(billingRun.getVatRateId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                } else {
                    if (Boolean.TRUE.equals(productDetails.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else {
                        defaultVatRate = Optional.ofNullable(productDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                    }
                }

                Invoice invoice = Invoice
                        .builder()
                        .billingId(billingRun.getId())
                        .customerId(customerDetails.getCustomerId())
                        .invoiceStatus(InvoiceStatus.DRAFT)
                        .invoiceDate(billingRun.getInvoiceDate())
                        .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                        .taxEventDate(billingRun.getTaxEventDate())
                        .paymentDeadline(paymentDeadline)
                        .invoiceType(InvoiceType.MANUAL)
                        .meterReadingPeriodFrom(meterReadingPeriodFrom)
                        .meterReadingPeriodTo(meterReadingPeriodTo)
                        .productContractId(productContractDetails.getContractId())
                        .productContractDetailId(productContractDetails.getId())
                        .productDetailId(productDetails.getId())
                        .contractBillingGroupId(contractBillingGroup.getId())
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(ObjectUtils.defaultIfNull(contractBillingGroup.getBillingCustomerCommunicationId(), productContractDetails.getCustomerCommunicationIdForBilling()))
                        .accountPeriodId(billingRun.getAccountingPeriodId())
                        .alternativeRecipientCustomerDetailId(ObjectUtils.defaultIfNull(contractBillingGroup.getAlternativeRecipientCustomerDetailId(), customerDetails.getId()))
                        .incomeAccountNumber(incomeAccountNumber)
                        .basisForIssuing(billingRun.getBasisForIssuing())
                        .costCenterControllingOrder(costCenterControllingOrder)
                        .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), productContractDetails.getApplicableInterestRate()))
                        .directDebit(directDebitPayload.directDebit())
                        .bankId(directDebitPayload.bankId())
                        .iban(directDebitPayload.iban())
                        .currencyId(mainCurrency.getId())
                        .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                        .templateDetailId(templateDetailId)
                        .parentInvoiceId(billingRunInvoice.getId())
                        .build();

                generateInvoiceAndDetailedData(
                        invoice,
                        billingRunInvoice,
                        mainCurrency,
                        defaultVatRate,
                        globalVatRateOptional,
                        billingSummaryData,
                        billingDetailedData,
                        persistedInvoicesContext
                );
            }
            case SERVICE_CONTRACT -> {
                ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                        .findRespectiveServiceContractDetailsByServiceContractId(billingRunStartDate, billingRunInvoice.getServiceContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective Service Contract not found;"));

                ServiceDetails serviceDetails = serviceDetailsRepository
                        .findById(serviceContractDetails.getServiceDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found;".formatted(serviceContractDetails.getServiceDetailId())));

                CustomerDetails customerDetails = customerDetailsRepository
                        .findById(serviceContractDetails.getCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(serviceContractDetails.getCustomerDetailId())));

                Currency mainCurrency = currencyRepository
                        .findMainCurrencyNowAndActive()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : ObjectUtils.defaultIfNull(billingRun.getInvoiceDueDate(), calculateServiceContractPaymentDeadline(serviceContractDetails, billingRun));

                LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun, serviceContractDetails, customerDetails, Optional.empty());

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), serviceDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), serviceDetails.getCostCenterControllingOrder());

                Optional<VatRate> globalVatRateOptional = vatRateRepository
                        .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                VatRate defaultVatRate;
                if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else if (Objects.nonNull(billingRun.getVatRateId())) {
                    defaultVatRate = vatRateRepository
                            .findById(billingRun.getVatRateId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                } else {
                    if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else {
                        defaultVatRate = Optional.ofNullable(serviceDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                    }
                }

                Invoice invoice = Invoice
                        .builder()
                        .customerId(customerDetails.getCustomerId())
                        .billingId(billingRun.getId())
                        .invoiceStatus(InvoiceStatus.DRAFT)
                        .invoiceDate(billingRun.getInvoiceDate())
                        .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                        .taxEventDate(billingRun.getTaxEventDate())
                        .paymentDeadline(paymentDeadline)
                        .invoiceType(InvoiceType.MANUAL)
                        .meterReadingPeriodFrom(meterReadingPeriodFrom)
                        .meterReadingPeriodTo(meterReadingPeriodTo)
                        .serviceContractId(serviceContractDetails.getContractId())
                        .serviceContractDetailId(serviceContractDetails.getId())
                        .serviceDetailId(serviceDetails.getId())
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), serviceContractDetails.getCustomerCommunicationIdForBilling()))
                        .accountPeriodId(billingRun.getAccountingPeriodId())
                        .alternativeRecipientCustomerDetailId(customerDetails.getId())
                        .incomeAccountNumber(incomeAccountNumber)
                        .basisForIssuing(billingRun.getBasisForIssuing())
                        .costCenterControllingOrder(costCenterControllingOrder)
                        .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceContractDetails.getApplicableInterestRate()))
                        .directDebit(directDebitPayload.directDebit())
                        .bankId(directDebitPayload.bankId())
                        .iban(directDebitPayload.iban())
                        .currencyId(mainCurrency.getId())
                        .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                        .parentInvoiceId(billingRunInvoice.getId())
                        .build();

                generateInvoiceAndDetailedData(
                        invoice,
                        billingRunInvoice,
                        mainCurrency,
                        defaultVatRate,
                        globalVatRateOptional,
                        billingSummaryData,
                        billingDetailedData,
                        persistedInvoicesContext
                );
            }
            case GOODS_ORDER -> {
                GoodsOrder goodsOrder = goodsOrderRepository
                        .findById(billingRunInvoice.getGoodsOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with id: [%s] not found;"));

                CustomerDetails customerDetails = customerDetailsRepository
                        .findById(goodsOrder.getCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(goodsOrder.getCustomerDetailId())));

                Currency mainCurrency = currencyRepository
                        .findMainCurrencyNowAndActive()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : ObjectUtils.defaultIfNull(billingRun.getInvoiceDueDate(), calculateGoodsOrderPaymentDeadline(billingRun, goodsOrder));

                LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun, goodsOrder, customerDetails, Optional.empty());

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), goodsOrder.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), goodsOrder.getCostCenterControllingOrder());

                Optional<VatRate> globalVatRateOptional = vatRateRepository
                        .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                VatRate defaultVatRate;
                if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else if (Objects.nonNull(billingRun.getVatRateId())) {
                    defaultVatRate = vatRateRepository
                            .findById(billingRun.getVatRateId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                } else {
                    if (Boolean.TRUE.equals(goodsOrder.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else {
                        defaultVatRate = vatRateRepository.findById(goodsOrder.getVatRateId()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                    }
                }

                Invoice invoice = Invoice
                        .builder()
                        .customerId(customerDetails.getCustomerId())
                        .billingId(billingRun.getId())
                        .invoiceStatus(InvoiceStatus.DRAFT)
                        .invoiceDate(billingRun.getInvoiceDate())
                        .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                        .taxEventDate(billingRun.getTaxEventDate())
                        .paymentDeadline(paymentDeadline)
                        .invoiceType(InvoiceType.MANUAL)
                        .meterReadingPeriodFrom(meterReadingPeriodFrom)
                        .meterReadingPeriodTo(meterReadingPeriodTo)
                        .goodsOrderId(goodsOrder.getId())
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), billingRunInvoice.getCustomerCommunicationId()))
                        .accountPeriodId(billingRun.getAccountingPeriodId())
                        .alternativeRecipientCustomerDetailId(customerDetails.getId())
                        .incomeAccountNumber(incomeAccountNumber)
                        .basisForIssuing(billingRun.getBasisForIssuing())
                        .costCenterControllingOrder(costCenterControllingOrder)
                        .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), goodsOrder.getApplicableInterestRateId()))
                        .directDebit(directDebitPayload.directDebit())
                        .bankId(directDebitPayload.bankId())
                        .iban(directDebitPayload.iban())
                        .currencyId(mainCurrency.getId())
                        .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                        .parentInvoiceId(billingRunInvoice.getId())
                        .build();

                generateInvoiceAndDetailedData(
                        invoice,
                        billingRunInvoice,
                        mainCurrency,
                        defaultVatRate,
                        globalVatRateOptional,
                        billingSummaryData,
                        billingDetailedData,
                        persistedInvoicesContext
                );
            }
            case SERVICE_ORDER -> {
                ServiceOrder serviceOrder = serviceOrderRepository
                        .findById(billingRunInvoice.getServiceOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;"));

                ServiceDetails serviceDetails = serviceDetailsRepository
                        .findById(serviceOrder.getServiceDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found"));

                CustomerDetails customerDetails = customerDetailsRepository
                        .findById(serviceOrder.getCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(serviceOrder.getCustomerDetailId())));

                Currency mainCurrency = currencyRepository
                        .findMainCurrencyNowAndActive()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice due date must be defined if Service Order is selected;"));

                LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun, serviceOrder, customerDetails, Optional.empty());

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), serviceDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), serviceDetails.getCostCenterControllingOrder());

                Optional<VatRate> globalVatRateOptional = vatRateRepository
                        .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                VatRate defaultVatRate;
                if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else if (Objects.nonNull(billingRun.getVatRateId())) {
                    defaultVatRate = vatRateRepository
                            .findById(billingRun.getVatRateId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                } else {
                    if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else {
                        defaultVatRate = Optional.ofNullable(serviceDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                    }
                }

                Invoice invoice = Invoice
                        .builder()
                        .billingId(billingRun.getId())
                        .customerId(customerDetails.getCustomerId())
                        .invoiceStatus(InvoiceStatus.DRAFT)
                        .invoiceDate(billingRun.getInvoiceDate())
                        .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                        .taxEventDate(billingRun.getTaxEventDate())
                        .paymentDeadline(paymentDeadline)
                        .invoiceType(InvoiceType.MANUAL)
                        .meterReadingPeriodFrom(meterReadingPeriodFrom)
                        .meterReadingPeriodTo(meterReadingPeriodTo)
                        .serviceOrderId(serviceOrder.getId())
                        .serviceDetailId(serviceDetails.getId())
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), billingRunInvoice.getCustomerCommunicationId()))
                        .accountPeriodId(billingRun.getAccountingPeriodId())
                        .alternativeRecipientCustomerDetailId(customerDetails.getId())
                        .incomeAccountNumber(incomeAccountNumber)
                        .basisForIssuing(billingRun.getBasisForIssuing())
                        .costCenterControllingOrder(costCenterControllingOrder)
                        .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceOrder.getApplicableInterestRateId()))
                        .directDebit(directDebitPayload.directDebit())
                        .bankId(directDebitPayload.bankId())
                        .iban(directDebitPayload.iban())
                        .currencyId(mainCurrency.getId())
                        .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                        .parentInvoiceId(billingRunInvoice.getId())
                        .build();

                generateInvoiceAndDetailedData(
                        invoice,
                        billingRunInvoice,
                        mainCurrency,
                        defaultVatRate,
                        globalVatRateOptional,
                        billingSummaryData,
                        billingDetailedData,
                        persistedInvoicesContext
                );
            }
            case ONLY_CUSTOMER ->{

                CustomerDetails customerDetails = customerDetailsRepository
                        .findById(billingRunInvoice.getCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(billingRun.getCustomerDetailId())));

                Currency mainCurrency = currencyRepository
                        .findMainCurrencyNowAndActive()
                        .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice due date must be defined if Service Order is selected;"));

                LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                //---
                DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun,customerDetails);

                //------------
                String incomeAccountNumber = billingRun.getNumberOfIncomeAccount();
                String costCenterControllingOrder = billingRun.getCostCenterControllingOrder();

                Optional<VatRate> globalVatRateOptional = vatRateRepository
                        .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                VatRate defaultVatRate;
                if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else  {
                    defaultVatRate = vatRateRepository
                            .findById(billingRun.getVatRateId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                }
                //------------

                Invoice invoice = Invoice
                        .builder()
                        .billingId(billingRun.getId())
                        .invoiceStatus(InvoiceStatus.DRAFT)
                        .customerId(customerDetails.getCustomerId())
                        .invoiceDate(billingRun.getInvoiceDate())
                        .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                        .taxEventDate(billingRun.getTaxEventDate())
                        .paymentDeadline(paymentDeadline)
                        .invoiceType(InvoiceType.MANUAL)
                        .meterReadingPeriodFrom(meterReadingPeriodFrom)
                        .meterReadingPeriodTo(meterReadingPeriodTo)
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), billingRunInvoice.getCustomerCommunicationId()))
                        .accountPeriodId(billingRun.getAccountingPeriodId())
                        .alternativeRecipientCustomerDetailId(customerDetails.getId())
                        .incomeAccountNumber(incomeAccountNumber)
                        .basisForIssuing(billingRun.getBasisForIssuing())
                        .costCenterControllingOrder(costCenterControllingOrder)
                        .interestRateId(billingRun.getInterestRateId())
                        .directDebit(directDebitPayload.directDebit())
                        .bankId(directDebitPayload.bankId())
                        .iban(directDebitPayload.iban())
                        .currencyId(mainCurrency.getId())
                        .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                        .parentInvoiceId(billingRunInvoice.getId())
                        .build();

                generateInvoiceAndDetailedData(
                        invoice,
                        billingRunInvoice,
                        mainCurrency,
                        defaultVatRate,
                        globalVatRateOptional,
                        billingSummaryData,
                        billingDetailedData,
                        persistedInvoicesContext
                );
            }
        }
        invoiceNumberService.fillInvoiceNumber(persistedInvoicesContext);
    }

    private void generateNonDirectInvoiceDebitOrCreditNote(LocalDate billingRunStartDate, BillingRun billingRun, List<Pair<BillingRunInvoices, Invoice>> billingRunInvoices, Long templateDetailId) {
        boolean containProductContractInvoice = billingRunInvoices
                .stream()
                .anyMatch(bri -> Objects.nonNull(bri.getValue().getProductContractId()));

        // finding all summery data by billing run
        List<BillingSummaryData> billingSummaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
        List<BillingDetailedData> billingDetailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId());
        validateValues(billingSummaryData);

        DocumentType documentType = Optional.ofNullable(billingRun.getDocumentType())
                .orElseThrow(() -> new IllegalArgumentsProvidedException("Document type is not defined, cannot generate debit/credit note"));

        Set<Long> persistedInvoicesContext = new HashSet<>();
        if (containProductContractInvoice) {
            // Take Due date and Sending an invoice values from contract
            Invoice anyInvoice = billingRunInvoices.get(0).getValue();

            ProductContractDetails productContractDetails = initializeProductContractDetails(billingRunStartDate, anyInvoice);
            ProductDetails productDetails = initializeProductDetails(productContractDetails.getProductDetailId());
            CustomerDetails customerDetails = initializeCustomerDetails(productContractDetails.getCustomerDetailId());

            Currency mainCurrency = currencyRepository
                    .findMainCurrencyNowAndActive()
                    .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

            LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice due date must be defined if Goods Order is selected;"));

            LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
            LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

            String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), productDetails.getIncomeAccountNumber());
            String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), productDetails.getCostCenterControllingOrder());

            Optional<VatRate> globalVatRateOptional = vatRateRepository
                    .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
            VatRate defaultVatRate;
            if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
            } else if (Objects.nonNull(billingRun.getVatRateId())) {
                defaultVatRate = vatRateRepository
                        .findById(billingRun.getVatRateId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
            } else {
                if (Boolean.TRUE.equals(productDetails.getGlobalVatRate())) {
                    defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                } else {
                    defaultVatRate = Optional.ofNullable(productDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                }
            }

            ContractBillingGroup contractBillingGroup = contractBillingGroupRepository
                    .findById(anyInvoice.getContractBillingGroupId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract Billing Group with id: [%s] not found".formatted(anyInvoice.getContractBillingGroupId())));

            DirectDebitPayload directDebitPayload = determinateDirectDebitByInvoice(billingRun, productContractDetails, customerDetails, Optional.of(contractBillingGroup));

            Invoice invoice = Invoice
                    .builder()
                    .billingId(billingRun.getId())
                    .invoiceStatus(InvoiceStatus.DRAFT)
                    .invoiceDate(billingRun.getInvoiceDate())
                    .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                    .taxEventDate(billingRun.getTaxEventDate())
                    .paymentDeadline(paymentDeadline)
                    .invoiceType(InvoiceType.MANUAL)
                    .meterReadingPeriodFrom(meterReadingPeriodFrom)
                    .meterReadingPeriodTo(meterReadingPeriodTo)
                    .productContractId(productContractDetails.getContractId())
                    .productContractDetailId(productContractDetails.getId())
                    .productDetailId(productDetails.getId())
                    .contractBillingGroupId(contractBillingGroup.getId())
                    .customerDetailId(customerDetails.getId())
                    .customerCommunicationId(ObjectUtils.defaultIfNull(contractBillingGroup.getBillingCustomerCommunicationId(), productContractDetails.getCustomerCommunicationIdForBilling()))
                    .accountPeriodId(billingRun.getAccountingPeriodId())
                    .alternativeRecipientCustomerDetailId(ObjectUtils.defaultIfNull(contractBillingGroup.getAlternativeRecipientCustomerDetailId(), customerDetails.getId()))
                    .incomeAccountNumber(incomeAccountNumber)
                    .basisForIssuing(billingRun.getBasisForIssuing())
                    .costCenterControllingOrder(costCenterControllingOrder)
                    .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), productContractDetails.getApplicableInterestRate()))
                    .directDebit(directDebitPayload.directDebit())
                    .bankId(directDebitPayload.bankId())
                    .iban(directDebitPayload.iban())
                    .currencyId(mainCurrency.getId())
                    .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                    .templateDetailId(templateDetailId)
                    .parentInvoiceId(anyInvoice.getId())
                    .build();

            generateInvoiceAndDetailedData(
                    invoice,
                    anyInvoice,
                    mainCurrency,
                    defaultVatRate,
                    globalVatRateOptional,
                    billingSummaryData,
                    billingDetailedData,
                    persistedInvoicesContext
            );
        } else {
            // Take Due date and Sending an invoice values from billing run object
            Invoice anyInvoice = billingRunInvoices.get(0).getValue();

            InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(anyInvoice);

            switch (invoiceObjectType) {
                case SERVICE_CONTRACT -> {
                    ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                            .findRespectiveServiceContractDetailsByServiceContractId(billingRunStartDate, anyInvoice.getServiceContractId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Respective Service Contract not found;"));

                    ServiceDetails serviceDetails = serviceDetailsRepository
                            .findById(serviceContractDetails.getServiceDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found;".formatted(serviceContractDetails.getServiceDetailId())));

                    CustomerDetails customerDetails = customerDetailsRepository
                            .findById(serviceContractDetails.getCustomerDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(serviceContractDetails.getCustomerDetailId())));

                    Currency mainCurrency = currencyRepository
                            .findMainCurrencyNowAndActive()
                            .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                    LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date must be defined;"));

                    LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                    LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                    DirectDebitPayload directDebitPayload = new DirectDebitPayload(null, null, null, null);
                    if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
                        Bank bank = bankRepository.findById(billingRun.getBankId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] does not exists;".formatted(billingRun.getBankId())));

                        directDebitPayload = new DirectDebitPayload(billingRun.getDirectDebit(), billingRun.getBankId(), bank.getBic(), billingRun.getIban());
                    }

                    Optional<VatRate> globalVatRateOptional = vatRateRepository
                            .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                    VatRate defaultVatRate;
                    if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else if (Objects.nonNull(billingRun.getVatRateId())) {
                        defaultVatRate = vatRateRepository
                                .findById(billingRun.getVatRateId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                    } else {
                        if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                            defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                        } else {
                            defaultVatRate = Optional.ofNullable(serviceDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                        }
                    }

                    Invoice invoice = Invoice
                            .builder()
                            .billingId(billingRun.getId())
                            .invoiceStatus(InvoiceStatus.DRAFT)
                            .invoiceDate(billingRun.getInvoiceDate())
                            .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                            .taxEventDate(billingRun.getTaxEventDate())
                            .paymentDeadline(paymentDeadline)
                            .invoiceType(InvoiceType.MANUAL)
                            .meterReadingPeriodFrom(meterReadingPeriodFrom)
                            .meterReadingPeriodTo(meterReadingPeriodTo)
                            .serviceContractId(serviceContractDetails.getContractId())
                            .serviceContractDetailId(serviceContractDetails.getId())
                            .serviceDetailId(serviceDetails.getId())
                            .customerDetailId(customerDetails.getId())
                            .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), serviceContractDetails.getCustomerCommunicationIdForBilling()))
                            .accountPeriodId(billingRun.getAccountingPeriodId())
                            .alternativeRecipientCustomerDetailId(customerDetails.getId())
                            .incomeAccountNumber(billingRun.getNumberOfIncomeAccount())
                            .basisForIssuing(billingRun.getBasisForIssuing())
                            .costCenterControllingOrder(billingRun.getCostCenterControllingOrder())
                            .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceContractDetails.getApplicableInterestRate()))
                            .directDebit(directDebitPayload.directDebit())
                            .bankId(directDebitPayload.bankId())
                            .iban(directDebitPayload.iban())
                            .currencyId(mainCurrency.getId())
                            .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                            .parentInvoiceId(anyInvoice.getId())
                            .build();

                    generateInvoiceAndDetailedData(
                            invoice,
                            anyInvoice,
                            mainCurrency,
                            defaultVatRate,
                            globalVatRateOptional,
                            billingSummaryData,
                            billingDetailedData,
                            persistedInvoicesContext
                    );
                }
                case GOODS_ORDER -> {
                    GoodsOrder goodsOrder = goodsOrderRepository
                            .findById(anyInvoice.getGoodsOrderId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with id: [%s] not found;"));

                    CustomerDetails customerDetails = customerDetailsRepository
                            .findById(goodsOrder.getCustomerDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(goodsOrder.getCustomerDetailId())));

                    Currency mainCurrency = currencyRepository
                            .findMainCurrencyNowAndActive()
                            .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                    LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date must be defined;"));

                    LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                    LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                    DirectDebitPayload directDebitPayload = new DirectDebitPayload(null, null, null, null);
                    if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
                        Bank bank = bankRepository.findById(billingRun.getBankId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] does not exists;".formatted(billingRun.getBankId())));

                        directDebitPayload = new DirectDebitPayload(billingRun.getDirectDebit(), billingRun.getBankId(), bank.getBic(), billingRun.getIban());
                    }

                    Optional<VatRate> globalVatRateOptional = vatRateRepository
                            .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                    VatRate defaultVatRate;
                    if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else if (Objects.nonNull(billingRun.getVatRateId())) {
                        defaultVatRate = vatRateRepository
                                .findById(billingRun.getVatRateId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                    } else {
                        if (Boolean.TRUE.equals(goodsOrder.getGlobalVatRate())) {
                            defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                        } else {
                            defaultVatRate = vatRateRepository.findById(goodsOrder.getVatRateId())
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(goodsOrder.getVatRateId())));
                        }
                    }

                    Invoice invoice = Invoice
                            .builder()
                            .billingId(billingRun.getId())
                            .invoiceStatus(InvoiceStatus.DRAFT)
                            .invoiceDate(billingRun.getInvoiceDate())
                            .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                            .taxEventDate(billingRun.getTaxEventDate())
                            .paymentDeadline(paymentDeadline)
                            .invoiceType(InvoiceType.MANUAL)
                            .meterReadingPeriodFrom(meterReadingPeriodFrom)
                            .meterReadingPeriodTo(meterReadingPeriodTo)
                            .goodsOrderId(goodsOrder.getId())
                            .customerDetailId(customerDetails.getId())
                            .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), anyInvoice.getCustomerCommunicationId()))
                            .accountPeriodId(billingRun.getAccountingPeriodId())
                            .alternativeRecipientCustomerDetailId(customerDetails.getId())
                            .incomeAccountNumber(billingRun.getNumberOfIncomeAccount())
                            .basisForIssuing(billingRun.getBasisForIssuing())
                            .costCenterControllingOrder(billingRun.getCostCenterControllingOrder())
                            .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), goodsOrder.getApplicableInterestRateId()))
                            .directDebit(directDebitPayload.directDebit())
                            .bankId(directDebitPayload.bankId())
                            .iban(directDebitPayload.iban())
                            .currencyId(mainCurrency.getId())
                            .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                            .parentInvoiceId(anyInvoice.getId())
                            .build();

                    generateInvoiceAndDetailedData(
                            invoice,
                            anyInvoice,
                            mainCurrency,
                            defaultVatRate,
                            globalVatRateOptional,
                            billingSummaryData,
                            billingDetailedData,
                            persistedInvoicesContext
                    );
                }
                case SERVICE_ORDER -> {
                    ServiceOrder serviceOrder = serviceOrderRepository
                            .findById(anyInvoice.getServiceOrderId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;"));

                    ServiceDetails serviceDetails = serviceDetailsRepository
                            .findById(serviceOrder.getServiceDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found"));

                    CustomerDetails customerDetails = customerDetailsRepository
                            .findById(serviceOrder.getCustomerDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(serviceOrder.getCustomerDetailId())));

                    Currency mainCurrency = currencyRepository
                            .findMainCurrencyNowAndActive()
                            .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));

                    LocalDate paymentDeadline = Objects.equals(billingRun.getDocumentType(), DocumentType.CREDIT_NOTE) ? null : Optional.ofNullable(billingRun.getInvoiceDueDate()).orElseThrow(() -> new IllegalArgumentsProvidedException("Invoice Due Date must be defined;"));

                    LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
                    LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);

                    DirectDebitPayload directDebitPayload = new DirectDebitPayload(null, null, null, null);
                    if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
                        Bank bank = bankRepository.findById(billingRun.getBankId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] does not exists;".formatted(billingRun.getBankId())));

                        directDebitPayload = new DirectDebitPayload(billingRun.getDirectDebit(), billingRun.getBankId(), bank.getBic(), billingRun.getIban());
                    }

                    Optional<VatRate> globalVatRateOptional = vatRateRepository
                            .findGlobalVatRate(billingRunStartDate, PageRequest.of(0, 1));
                    VatRate defaultVatRate;
                    if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
                        defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                    } else if (Objects.nonNull(billingRun.getVatRateId())) {
                        defaultVatRate = vatRateRepository
                                .findById(billingRun.getVatRateId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
                    } else {
                        if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                            defaultVatRate = globalVatRateOptional.orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for billing run start date;"));
                        } else {
                            defaultVatRate = Optional.ofNullable(serviceDetails.getVatRate()).orElseThrow(() -> new DomainEntityNotFoundException("Cannot define default vat rate"));
                        }
                    }

                    Invoice invoice = Invoice
                            .builder()
                            .billingId(billingRun.getId())
                            .invoiceStatus(InvoiceStatus.DRAFT)
                            .invoiceDate(billingRun.getInvoiceDate())
                            .invoiceDocumentType(documentType.mapToInvoiceDocumentType())
                            .taxEventDate(billingRun.getTaxEventDate())
                            .paymentDeadline(paymentDeadline)
                            .invoiceType(InvoiceType.MANUAL)
                            .meterReadingPeriodFrom(meterReadingPeriodFrom)
                            .meterReadingPeriodTo(meterReadingPeriodTo)
                            .serviceOrderId(serviceOrder.getId())
                            .serviceDetailId(serviceDetails.getId())
                            .customerDetailId(customerDetails.getId())
                            .customerCommunicationId(ObjectUtils.defaultIfNull(billingRun.getCustomerCommunicationId(), anyInvoice.getCustomerCommunicationId()))
                            .accountPeriodId(billingRun.getAccountingPeriodId())
                            .alternativeRecipientCustomerDetailId(customerDetails.getId())
                            .incomeAccountNumber(billingRun.getNumberOfIncomeAccount())
                            .basisForIssuing(billingRun.getBasisForIssuing())
                            .costCenterControllingOrder(billingRun.getCostCenterControllingOrder())
                            .interestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceOrder.getApplicableInterestRateId()))
                            .directDebit(directDebitPayload.directDebit())
                            .bankId(directDebitPayload.bankId())
                            .iban(directDebitPayload.iban())
                            .currencyId(mainCurrency.getId())
                            .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                            .parentInvoiceId(anyInvoice.getId())
                            .build();

                    generateInvoiceAndDetailedData(
                            invoice,
                            anyInvoice,
                            mainCurrency,
                            defaultVatRate,
                            globalVatRateOptional,
                            billingSummaryData,
                            billingDetailedData,
                            persistedInvoicesContext
                    );
                }
                default ->
                        throw new IllegalArgumentsProvidedException("Exception handled while trying to generate manual debit/credit note");
            }
        }
        invoiceNumberService.fillInvoiceNumber(persistedInvoicesContext);
    }

    private ProductContractDetails initializeProductContractDetails(LocalDate billingRunStartDate, Invoice invoice) {
        return productContractDetailsRepository
                .findRespectiveProductContractDetailsByProductContractId(billingRunStartDate, invoice.getProductContractId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective Product Contract not found;"));
    }

    private ProductDetails initializeProductDetails(Long productDetailId) {
        return productDetailsRepository
                .findById(productDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Product Details with id: [%s] not found;".formatted(productDetailId)));
    }

    private CustomerDetails initializeCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository
                .findById(customerDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details with id: [%s] not found;".formatted(customerDetailId)));
    }

    private void generateInvoiceAndDetailedData(Invoice invoice,
                                                Invoice billingRunInvoice,
                                                Currency mainCurrency,
                                                VatRate defaultVatRate,
                                                Optional<VatRate> globalVatRateOptional,
                                                List<BillingSummaryData> billingSummaryData,
                                                List<BillingDetailedData> billingDetailedData,
                                                Set<Long> persistedInvoicesContext) {
        Invoice persistedInvoice = invoiceRepository.saveAndFlush(invoice);

        invoiceRelatedInvoiceRepository.save(new InvoiceRelatedInvoice(null, invoice.getId(), billingRunInvoice.getId()));

        List<ManualDebitOrCreditNoteInvoiceSummaryData> manualDebitOrCreditNoteInvoiceSummaryData = manualDebitOrCreditNoteInvoiceSummaryDataRepository
                .saveAll(billingSummaryData
                        .stream()
                        .map(sd -> billingRunManualInvoicesMapper.mapToManualDebitOrCreditNoteInvoiceSummaryData(
                                sd,
                                globalVatRateOptional,
                                defaultVatRate,
                                persistedInvoice
                        ))
                        .toList());

        manualDebitOrCreditNoteInvoiceDetailedDataRepository
                .saveAll(billingDetailedData
                        .stream()
                        .map(dd -> billingRunManualInvoicesMapper.mapToManualDebitOrCreditNoteInvoiceDetailedData(
                                dd,
                                globalVatRateOptional,
                                persistedInvoice
                        ))
                        .toList()
                );

        List<InvoiceDetailedDataAmountModel> amountModels = manualDebitOrCreditNoteInvoiceSummaryData
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(
                        model.getVatRatePercent(),
                        model.getValue(),
                        Objects.equals(model.getValueCurrencyId(), mainCurrency.getId()),
                        model.getValueCurrencyExchangeRate()
                ))
                .toList();

        List<InvoiceVatRateResponse> invoiceVatRates = groupByVatRates(amountModels);

        calculateTotalAmountsAndSetToInvoice(invoice, mainCurrency, amountModels, invoiceVatRates);
        mapVatRatesAndSaveToDatabase(invoice.getId(), invoiceVatRates);

        persistedInvoicesContext.add(persistedInvoice.getId());
    }

    private DirectDebitPayload determinateDirectDebitByInvoice(BillingRun billingRun,
                                                               ProductContractDetails productContractDetails,
                                                               CustomerDetails customerDetails,
                                                               Optional<ContractBillingGroup> contractBillingGroupOptional) {
        return extractDirectDebit(billingRun, customerDetails, contractBillingGroupOptional, productContractDetails.getDirectDebit(), productContractDetails.getBankId(), productContractDetails.getIban());
    }

    private DirectDebitPayload determinateDirectDebitByInvoice(BillingRun billingRun,
                                                               CustomerDetails customerDetails) {
        return extractDirectDebit(billingRun, customerDetails, Optional.<ContractBillingGroup>empty(), null,null, null);
    }

    private DirectDebitPayload determinateDirectDebitByInvoice(BillingRun billingRun,
                                                               ServiceContractDetails serviceContractDetails,
                                                               CustomerDetails customerDetails,
                                                               Optional<ContractBillingGroup> contractBillingGroupOptional) {
        return extractDirectDebit(billingRun, customerDetails, contractBillingGroupOptional, serviceContractDetails.getDirectDebit(), serviceContractDetails.getBankId(), serviceContractDetails.getIban());
    }

    private DirectDebitPayload determinateDirectDebitByInvoice(BillingRun billingRun,
                                                               GoodsOrder goodsOrder,
                                                               CustomerDetails customerDetails,
                                                               Optional<ContractBillingGroup> contractBillingGroupOptional) {
        return extractDirectDebit(billingRun, customerDetails, contractBillingGroupOptional, goodsOrder.getDirectDebit(), goodsOrder.getBankId(), goodsOrder.getIban());
    }

    private DirectDebitPayload determinateDirectDebitByInvoice(BillingRun billingRun,
                                                               ServiceOrder serviceOrder,
                                                               CustomerDetails customerDetails,
                                                               Optional<ContractBillingGroup> contractBillingGroupOptional) {
        return extractDirectDebit(billingRun, customerDetails, contractBillingGroupOptional, serviceOrder.getDirectDebit(), serviceOrder.getBankId(), serviceOrder.getIban());
    }

    private DirectDebitPayload extractDirectDebit(BillingRun billingRun,
                                                  CustomerDetails customerDetails,
                                                  Optional<ContractBillingGroup> contractBillingGroupOptional,
                                                  Boolean directDebit,
                                                  Long bankId,
                                                  String iban) {
        if (Boolean.TRUE.equals(billingRun.getDirectDebit())) {
            Bank bank = bankRepository
                    .findById(billingRun.getBankId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] not found".formatted(billingRun.getBankId())));
            return new DirectDebitPayload(billingRun.getDirectDebit(), bank.getId(), bank.getBic(), billingRun.getIban());
        } else if (Boolean.FALSE.equals(billingRun.getDirectDebit())) {
            return new DirectDebitPayload(null, null, null, null);
        }

        if (contractBillingGroupOptional.isPresent()) {
            ContractBillingGroup contractBillingGroup = contractBillingGroupOptional.get();
            if (Boolean.TRUE.equals(contractBillingGroup.getDirectDebit())) {
                Bank bank = bankRepository
                        .findById(contractBillingGroup.getBankId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] does not exists;".formatted(contractBillingGroup.getBankId())));
                return new DirectDebitPayload(contractBillingGroup.getDirectDebit(), contractBillingGroup.getBankId(), bank.getBic(), contractBillingGroup.getIban());
            }
        }

        if (Boolean.TRUE.equals(directDebit)) {
            Bank bank = bankRepository
                    .findById(bankId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] does not exists;".formatted(bankId)));
            return new DirectDebitPayload(true, bankId, bank.getBic(), iban);
        }

        if (Boolean.TRUE.equals(customerDetails.getDirectDebit())) {
            Bank bank = Optional.of(customerDetails.getBank())
                    .orElseThrow(() -> new IllegalArgumentsProvidedException("Customer has not assigned bank;"));
            return new DirectDebitPayload(customerDetails.getDirectDebit(), bank.getId(), bank.getBic(), customerDetails.getIban());
        }

        return new DirectDebitPayload(null, null, null, null);
    }
}
