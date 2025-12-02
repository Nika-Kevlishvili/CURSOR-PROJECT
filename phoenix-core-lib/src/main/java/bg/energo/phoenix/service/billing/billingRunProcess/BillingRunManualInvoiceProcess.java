package bg.energo.phoenix.service.billing.billingRunProcess;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.*;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.ManualInvoiceDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.ManualInvoiceSummaryData;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderPaymentTerm;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.billing.billingRun.*;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.billing.invoice.ManualInvoiceDetailedDataRepository;
import bg.energo.phoenix.repository.billing.invoice.ManualInvoiceSummaryDataRepository;
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
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Class to handle the manual invoice process for a billing run.
 */
@Slf4j
@Service
public class BillingRunManualInvoiceProcess extends AbstractBillingRunManualInvoice {
    private final GoodsOrderRepository goodsOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final BillingRunInvoicesRepository billingRunInvoicesRepository;
    private final BillingRunBillingGroupRepository billingRunBillingGroupRepository;
    private final ManualInvoiceSummaryDataRepository manualInvoiceSummaryDataRepository;
    private final ManualInvoiceDetailedDataRepository manualInvoiceDetailedDataRepository;

    public BillingRunManualInvoiceProcess(ServiceContractDetailsRepository serviceContractDetailsRepository,
                                          InvoicePaymentTermsRepository invoicePaymentTermsRepository,
                                          CalendarRepository calendarRepository,
                                          HolidaysRepository holidaysRepository,
                                          ContractBillingGroupRepository contractBillingGroupRepository,
                                          CustomerDetailsRepository customerDetailsRepository,
                                          BankRepository bankRepository,
                                          ServiceDetailsRepository serviceDetailsRepository,
                                          ProductDetailsRepository productDetailsRepository,
                                          InvoiceRepository invoiceRepository,
                                          VatRateRepository vatRateRepository,
                                          CurrencyRepository currencyRepository,
                                          GoodsOrderRepository goodsOrderRepository,
                                          ServiceOrderRepository serviceOrderRepository,
                                          BillingSummaryDataRepository billingSummaryDataRepository,
                                          BillingRunInvoicesRepository billingRunInvoicesRepository,
                                          BillingDetailedDataRepository billingDetailedDataRepository,
                                          GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository,
                                          BillingRunBillingGroupRepository billingRunBillingGroupRepository,
                                          ProductContractDetailsRepository productContractDetailsRepository,
                                          ManualInvoiceSummaryDataRepository manualInvoiceSummaryDataRepository,
                                          ManualInvoiceDetailedDataRepository manualInvoiceDetailedDataRepository,
                                          InvoiceVatRateValueRepository invoiceVatRateValueRepository,
                                          InvoiceEventPublisher invoiceEventPublisher,
                                          ContractPodRepository contractPodsRepository,
                                          BillingRunManualInvoicesMapper billingRunManualInvoicesMapper,
                                          ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                          NotificationEventPublisher notificationEventPublisher,
                                          BillingRunRepository billingRunRepository,
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
                contractPodsRepository,
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
        this.goodsOrderRepository = goodsOrderRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.billingRunInvoicesRepository = billingRunInvoicesRepository;
        this.billingRunBillingGroupRepository = billingRunBillingGroupRepository;
        this.manualInvoiceSummaryDataRepository = manualInvoiceSummaryDataRepository;
        this.manualInvoiceDetailedDataRepository = manualInvoiceDetailedDataRepository;
    }

    /**
     * Processes a manual invoice for a billing run.
     *
     * @param billingRun the BillingRun for which to generate the invoice
     */
    @Transactional
    public void process(BillingRun billingRun) {
        try {
            execute(billingRun);

            billingRun.setStatus(BillingStatus.DRAFT);
            billingRunRepository.save(billingRun);
        } catch (Exception e) {
            log.debug("Exception handled while processing manual invoice for billing run with id: [%s];".formatted(billingRun.getId()), e);
            publishNotification(billingRun.getId(), NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);
            throw e;
        }
    }

    private void execute(BillingRun billingRun) {
        log.debug("Executing manual invoice process for billing run with id: [%s];".formatted(billingRun.getId()));
        BillingRunObjectType billingRunObjectType = BillingRunObjectType.defineInvoiceObjectType(billingRun);
        log.debug("Billing run object type: {}", billingRunObjectType);
        LocalDate billingStartDate = LocalDate.now();

        log.debug("Processing manual invoice process for billing run with id: [%s];".formatted(billingRun.getId()));
        LocalDate invoiceDate = billingRun.getInvoiceDate();
        if (Objects.isNull(invoiceDate)) {
            if (Objects.equals(BillingRunObjectType.ONLY_CUSTOMER, billingRunObjectType)) {
                log.debug("Invoice date is null; skipping processing manual invoice process for billing run with id: [%s];".formatted(billingRun.getId()));
                throw new IllegalArgumentsProvidedException("Invoice date must be defined for customer invoice");
            }
        }

        LocalDate currentDate = LocalDate.now();
        log.debug("Current date: {}", currentDate);

        Optional<ContractTemplateDetail> templateDetailOptional = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIdAndDate(billingRun.getTemplateId(), currentDate);
        if (templateDetailOptional.isEmpty()) {
            log.error("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
            throw new IllegalArgumentsProvidedException("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), currentDate));
        }
        Long templateDetailId = templateDetailOptional.get().getId();
        log.debug("Template detail id: {}", templateDetailId);

        Optional<BillingRunBillingGroup> billingRunBillingGroupOptional = billingRunBillingGroupRepository
                .findByBillingRunIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);

        // finding all summery data by billing run
        List<BillingSummaryData> billingSummaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
        log.debug("Billing summary data: {}", billingSummaryData);
        List<BillingDetailedData> billingDetailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId());
        log.debug("Billing detailed data: {}", billingDetailedData);
        validateValues(billingSummaryData);

        // calculating meter readings, by point of deliveries min activation date in billing group and max deactivation date
        LocalDate meterReadingPeriodFrom = calculateMeterReadingPeriodFrom(billingDetailedData);
        log.debug("Meter reading period from: {}", meterReadingPeriodFrom);
        LocalDate meterReadingPeriodTo = calculateMeterReadingPeriodTo(billingDetailedData);
        log.debug("Meter reading period to: {}", meterReadingPeriodTo);

        // calculating payment deadline
        LocalDate paymentDeadLine = null;
        InvoiceDueDateType invoiceDueDateType = billingRun.getInvoiceDueDateType();
        log.debug("Invoice due date type: {}", invoiceDueDateType);
        switch (invoiceDueDateType) {
            case ACCORDING_TO_THE_CONTRACT ->
                    paymentDeadLine = calculatePaymentDeadLine(billingRunObjectType, billingStartDate, billingRun);
            case DATE -> paymentDeadLine = billingRun.getInvoiceDueDate();
        }
        log.debug("Payment deadline: {}", paymentDeadLine);

        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Main currency not found for this time"));
        log.debug("Main currency: {}", mainCurrency);

        ContractBillingGroup contractBillingGroup = null;
        ProductContractDetails productContractDetails = null;
        if (Objects.nonNull(billingRun.getProductContractId())) {
            productContractDetails = productContractDetailsRepository
                    .findRespectiveProductContractDetailsByProductContractId(billingStartDate, billingRun.getProductContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective product contract details not found for billing start date: [%s]".formatted(billingStartDate)));

            if (billingRunBillingGroupOptional.isPresent()) {
                BillingRunBillingGroup billingRunBillingGroup = billingRunBillingGroupOptional.get();
                contractBillingGroup = contractBillingGroupRepository.findByIdAndStatusIn(billingRunBillingGroup.getBillingGroupId(), List.of(EntityStatus.ACTIVE)).orElse(null);
            }
        }

        ServiceContractDetails serviceContractDetails = null;
        if (Objects.nonNull(billingRun.getServiceContractId())) {
            serviceContractDetails = serviceContractDetailsRepository
                    .findRespectiveServiceContractDetailsByServiceContractId(billingStartDate, billingRun.getServiceContractId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details for contract with id: [%s] not found;".formatted(billingRun.getServiceContractId())));
        }

        Optional<VatRate> globalVatRateOptional = vatRateRepository.findGlobalVatRate(billingStartDate, PageRequest.of(0, 1));
        VatRate defaultBillingRunVatRate = findDefaultBillingRunVatRate(billingRun, globalVatRateOptional, billingRunObjectType, productContractDetails, serviceContractDetails);

        DirectDebitPayload directDebitPayload = defineDirectDebit(billingRunObjectType, billingStartDate, billingRun, Optional.ofNullable(contractBillingGroup));

        Long customerCommunicationId = getCustomerCommunicationId(billingRun, contractBillingGroup);
        CustomerDetails customerDetails = customerDetailsRepository.findById(billingRun.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details for Billing run with id: [%s] not found;".formatted(billingRun.getId())));

        // creating invoice
        Invoice invoice = Invoice.builder()
                .invoiceDate(invoiceDate)
                .invoiceStatus(InvoiceStatus.DRAFT)
                .invoiceDocumentType(InvoiceDocumentType.INVOICE)
                .taxEventDate(billingRun.getTaxEventDate())
                .paymentDeadline(paymentDeadLine)
                .invoiceType(InvoiceType.MANUAL)
                .meterReadingPeriodFrom(meterReadingPeriodFrom)
                .meterReadingPeriodTo(meterReadingPeriodTo)
                .basisForIssuing(billingRun.getBasisForIssuing())
                .bankId(directDebitPayload.bankId())
                .iban(directDebitPayload.iban())
                .directDebit(directDebitPayload.directDebit())
                .customerId(customerDetails.getCustomerId())
                .customerDetailId(billingRun.getCustomerDetailId())
                .goodsOrderId(billingRun.getGoodsOrderId())
                .serviceOrderId(billingRun.getServiceOrderId())
                .contractBillingGroupId(billingRunBillingGroupOptional.map(BillingRunBillingGroup::getBillingGroupId).orElse(null))
                .productContractId(billingRun.getProductContractId())
                .productContractDetailId(productContractDetails == null ? null : productContractDetails.getId())
                .productDetailId(productContractDetails == null ? null : productContractDetails.getProductDetailId())
                .serviceContractId(billingRun.getServiceContractId())
                .serviceContractDetailId(serviceContractDetails == null ? null : serviceContractDetails.getId())
                .serviceDetailId(setServiceDetailId(billingRunObjectType, billingRun, serviceContractDetails))
                .billingId(billingRun.getId())
                .customerCommunicationId(customerCommunicationId)
                .currencyId(mainCurrency.getId())
                .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                .alternativeRecipientCustomerDetailId(contractBillingGroup == null ? billingRun.getCustomerDetailId() : Objects.requireNonNullElse(contractBillingGroup.getAlternativeRecipientCustomerDetailId(), billingRun.getCustomerDetailId()))
                .accountPeriodId(billingRun.getAccountingPeriodId())
                .templateDetailId(templateDetailId)
                .build();
        ManualInvoiceFieldsPayload manualInvoiceFieldsPayload = setInvoiceManualFields(billingRunObjectType, invoice, billingStartDate, billingRun);

        log.debug("Saving invoice: {}", invoice);
        Invoice persistedInvoice = invoiceRepository.saveAndFlush(invoice);

        // assigning invoice to billing run
        billingRunInvoicesRepository.save(new BillingRunInvoices(null, billingRun.getId(), persistedInvoice.getId(), EntityStatus.ACTIVE));
        ManualInvoiceDataModelPayload manualInvoiceDataModelPayload = mapBillingDataToInvoiceDataAndSaveToDatabase(
                persistedInvoice,
                billingSummaryData,
                billingDetailedData,
                globalVatRateOptional,
                defaultBillingRunVatRate
        );

        List<InvoiceDetailedDataAmountModel> amountModels =
                manualInvoiceDataModelPayload
                        .summaryData
                        .stream()
                        .map(model -> new InvoiceDetailedDataAmountModel(
                                model.getVatRatePercent(),
                                model.getValue(),
                                Objects.equals(model.getValueCurrencyId(), mainCurrency.getId()),
                                model.getValueCurrencyExchangeRate()
                        )).toList();

        List<InvoiceVatRateResponse> invoiceVatRateResponses = groupByVatRates(amountModels);

        calculateTotalAmountsAndSetToInvoice(persistedInvoice, mainCurrency, amountModels, invoiceVatRateResponses);
        mapVatRatesAndSaveToDatabase(persistedInvoice.getId(), invoiceVatRateResponses);

        if (persistedInvoice.getTotalAmountIncludingVat().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClientException("Invoice total value is negative", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        log.debug("Persisted invoice: {}", persistedInvoice);
        invoiceRepository.save(persistedInvoice);

        log.debug("Publishing invoice event for invoice with id: [%s];".formatted(persistedInvoice.getId()));
        invoiceNumberService.fillInvoiceNumber(invoice);
    }

    /**
     * Finds the default billing run VAT rate based on the provided parameters.
     *
     * @param billingRun             the billing run entity
     * @param globalVatRateOptional  the optional global VAT rate
     * @param billingRunObjectType   the type of billing run object
     * @param productContractDetails the details of product contract
     * @param serviceContractDetails the details of service contract
     * @return the default VAT rate for the billing run
     */
    private VatRate findDefaultBillingRunVatRate(BillingRun billingRun, Optional<VatRate> globalVatRateOptional, BillingRunObjectType billingRunObjectType, ProductContractDetails productContractDetails, ServiceContractDetails serviceContractDetails) {
        if (Boolean.TRUE.equals(billingRun.getGlobalVatRate())) {
            return globalVatRateOptional
                    .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for current date;"));
        } else if (Objects.nonNull(billingRun.getVatRateId())) {
            return vatRateRepository
                    .findById(billingRun.getVatRateId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(billingRun.getVatRateId())));
        } else {
            switch (billingRunObjectType) {
                case ONLY_CUSTOMER -> {
                    throw new IllegalArgumentsProvidedException("Cannot determinate default Vat Rate");
                }
                case PRODUCT_CONTRACT -> {
                    Long productDetailId = productContractDetails.getProductDetailId();
                    ProductDetails productDetails = productDetailsRepository
                            .findById(productDetailId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Product Details with id: [%s] not found;".formatted(productDetailId)));

                    if (Boolean.TRUE.equals(productDetails.getGlobalVatRate())) {
                        return globalVatRateOptional
                                .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for current date;"));
                    } else {
                        return productDetails.getVatRate();
                    }
                }
                case SERVICE_CONTRACT -> {
                    Long serviceDetailId = serviceContractDetails.getServiceDetailId();
                    ServiceDetails serviceDetails = serviceDetailsRepository
                            .findById(serviceDetailId)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found;".formatted(serviceDetailId)));

                    if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                        return globalVatRateOptional
                                .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for current date;"));
                    } else {
                        return serviceDetails.getVatRate();
                    }
                }
                case GOODS_ORDER -> {
                    GoodsOrder goodsOrder = goodsOrderRepository
                            .findById(billingRun.getGoodsOrderId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with id: [%s] not found;".formatted(billingRun.getGoodsOrderId())));

                    if (Boolean.TRUE.equals(goodsOrder.getGlobalVatRate())) {
                        return globalVatRateOptional
                                .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for current date;"));
                    } else {
                        return vatRateRepository
                                .findById(goodsOrder.getVatRateId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Vat Rate with id: [%s] not found;".formatted(goodsOrder.getVatRateId())));
                    }
                }
                case SERVICE_ORDER -> {
                    ServiceOrder serviceOrder = serviceOrderRepository
                            .findById(billingRun.getServiceOrderId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;".formatted(billingRun.getServiceOrderId())));

                    ServiceDetails serviceDetails = serviceDetailsRepository
                            .findById(serviceOrder.getServiceDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Service Details with id: [%s] not found;".formatted(serviceOrder.getServiceDetailId())));

                    if (Boolean.TRUE.equals(serviceDetails.getGlobalVatRate())) {
                        return globalVatRateOptional
                                .orElseThrow(() -> new DomainEntityNotFoundException("Global Vat Rate not found for current date;"));
                    } else {
                        return serviceDetails.getVatRate();
                    }
                }
            }
        }

        throw new IllegalArgumentsProvidedException("Cannot define Billing Run default Vat Rate;");
    }

    /**
     * Sets the service detail ID based on the type of billing run object and associated information.
     *
     * @param billingRunObjectType   The type of billing run object
     * @param billingRun             The billing run object
     * @param serviceContractDetails The service contract details associated with the billing run
     * @return The service detail ID for the provided billing run object, or null if not found
     */
    private Long setServiceDetailId(BillingRunObjectType billingRunObjectType,
                                    BillingRun billingRun,
                                    ServiceContractDetails serviceContractDetails) {
        switch (billingRunObjectType) {
            case SERVICE_CONTRACT -> {
                return serviceContractDetails == null ? null : serviceContractDetails.getServiceDetailId();
            }
            case SERVICE_ORDER -> {
                ServiceOrder serviceOrder = serviceOrderRepository
                        .findById(billingRun.getServiceOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;".formatted(billingRun.getServiceOrderId())));

                return serviceOrder.getServiceDetailId();
            }
        }
        return null;
    }

    /**
     * This method retrieves the customer communication ID based on the given billing run and contract billing group.
     *
     * @param billingRun           The billing run object.
     * @param contractBillingGroup The contract billing group object.
     * @return The customer communication ID. If the contract billing group is not null and
     * it has an alternative recipient customer detail ID, then the billing customer communication ID
     * is returned if it is not null; otherwise, the customer communication ID from the billing run
     * is returned. If the contract billing group is null, the customer communication ID from the billing run
     * is returned.
     */
    private Long getCustomerCommunicationId(BillingRun billingRun, ContractBillingGroup contractBillingGroup) {
        if (Optional.ofNullable(contractBillingGroup).isPresent()) {
            if (Objects.nonNull(contractBillingGroup.getAlternativeRecipientCustomerDetailId())) {
                return contractBillingGroup.getBillingCustomerCommunicationId() == null ? billingRun.getCustomerCommunicationId() : contractBillingGroup.getBillingCustomerCommunicationId();
            } else {
                return billingRun.getCustomerCommunicationId();
            }
        } else {
            return billingRun.getCustomerCommunicationId();
        }
    }

    /**
     * Set manual fields for the invoice based on the billing run object type.
     *
     * @param billingRunObjectType specifies the type of billing run object
     * @param invoice              the invoice to set manual fields for
     * @param billingStartDate     the billing start date
     * @param billingRun           the billing run object
     * @return ManualInvoiceFieldsPayload containing the cost center controlling order and income account number
     */
    private ManualInvoiceFieldsPayload setInvoiceManualFields(BillingRunObjectType billingRunObjectType, Invoice invoice, LocalDate billingStartDate, BillingRun billingRun) {
        log.debug("Setting manual invoice fields for invoice with id: [%s];".formatted(invoice.getId()));
        switch (billingRunObjectType) {
            case ONLY_CUSTOMER -> {
                invoice.setInterestRateId(billingRun.getInterestRateId());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), "");
                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), "");

                invoice.setCostCenterControllingOrder(costCenterControllingOrder);
                invoice.setIncomeAccountNumber(incomeAccountNumber);

                return new ManualInvoiceFieldsPayload(costCenterControllingOrder, incomeAccountNumber);
            }
            case PRODUCT_CONTRACT -> {
                ProductContractDetails productContractDetails = productContractDetailsRepository
                        .findRespectiveProductContractDetailsByProductContractId(billingStartDate, billingRun.getProductContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective product contract details not found for billing start date: [%s]".formatted(billingStartDate)));

                ProductDetails productDetails = productDetailsRepository
                        .findById(productContractDetails.getProductDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Product Details not found with id: [%s]".formatted(productContractDetails.getProductDetailId())));

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), productDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), productDetails.getCostCenterControllingOrder());

                invoice.setIncomeAccountNumber(incomeAccountNumber);
                invoice.setCostCenterControllingOrder(costCenterControllingOrder);
                invoice.setInterestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), productContractDetails.getApplicableInterestRate()));

                return new ManualInvoiceFieldsPayload(costCenterControllingOrder, incomeAccountNumber);
            }
            case SERVICE_CONTRACT -> {
                ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                        .findRespectiveServiceContractDetailsByServiceContractId(billingStartDate, billingRun.getServiceContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details for contract with id: [%s] not found;".formatted(billingRun.getServiceContractId())));

                ServiceDetails serviceDetails = serviceDetailsRepository
                        .findById(serviceContractDetails.getServiceDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Details not found with id: [%s]".formatted(serviceContractDetails.getServiceDetailId())));

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), serviceDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), serviceDetails.getCostCenterControllingOrder());

                invoice.setIncomeAccountNumber(incomeAccountNumber);
                invoice.setCostCenterControllingOrder(costCenterControllingOrder);
                invoice.setInterestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceContractDetails.getApplicableInterestRate()));

                return new ManualInvoiceFieldsPayload(costCenterControllingOrder, incomeAccountNumber);
            }
            case GOODS_ORDER -> {
                GoodsOrder goodsOrder = goodsOrderRepository
                        .findById(billingRun.getGoodsOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Goods order with id: [%s] not found;".formatted(billingRun.getGoodsOrderId())));

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), goodsOrder.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), goodsOrder.getCostCenterControllingOrder());

                invoice.setIncomeAccountNumber(incomeAccountNumber);
                invoice.setCostCenterControllingOrder(costCenterControllingOrder);
                invoice.setInterestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), goodsOrder.getApplicableInterestRateId()));

                return new ManualInvoiceFieldsPayload(costCenterControllingOrder, incomeAccountNumber);
            }
            case SERVICE_ORDER -> {
                ServiceOrder serviceOrder = serviceOrderRepository
                        .findById(billingRun.getServiceOrderId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;".formatted(billingRun.getServiceOrderId())));

                ServiceDetails serviceDetails = serviceDetailsRepository
                        .findById(serviceOrder.getServiceDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective service contract details for contract with id: [%s] not found;".formatted(billingRun.getServiceContractId())));

                String incomeAccountNumber = ObjectUtils.defaultIfNull(billingRun.getNumberOfIncomeAccount(), serviceDetails.getIncomeAccountNumber());
                String costCenterControllingOrder = ObjectUtils.defaultIfNull(billingRun.getCostCenterControllingOrder(), serviceDetails.getCostCenterControllingOrder());

                invoice.setIncomeAccountNumber(incomeAccountNumber);
                invoice.setCostCenterControllingOrder(costCenterControllingOrder);
                invoice.setInterestRateId(ObjectUtils.defaultIfNull(billingRun.getInterestRateId(), serviceOrder.getApplicableInterestRateId()));

                return new ManualInvoiceFieldsPayload(costCenterControllingOrder, incomeAccountNumber);
            }
            default -> throw new IllegalArgumentsProvidedException("Cannot determinate manual invoice fields");
        }
    }

    /**
     * Maps the billing data to invoice data and saves it to the database.
     *
     * @param invoice      The invoice object to which the billing data will be mapped.
     * @param summaryData  The list of billing summary data.
     * @param detailedData The list of billing detailed data.
     */
    private ManualInvoiceDataModelPayload mapBillingDataToInvoiceDataAndSaveToDatabase(Invoice invoice,
                                                                                       List<BillingSummaryData> summaryData,
                                                                                       List<BillingDetailedData> detailedData,
                                                                                       Optional<VatRate> globalVatRateOptional,
                                                                                       VatRate defaultBillingRunVatRate) {
        List<ManualInvoiceSummaryData> summeryDataMappedToInvoiceDetailedData = summaryData
                .stream()
                .map((sd) -> billingRunManualInvoicesMapper.mapToManualInvoiceSummaryData(
                        sd,
                        globalVatRateOptional,
                        defaultBillingRunVatRate,
                        invoice
                ))
                .toList();

        List<ManualInvoiceDetailedData> detailedDataMappedToInvoiceSummaryData = detailedData
                .stream()
                .map((dd) -> billingRunManualInvoicesMapper.mapToManualInvoiceDetailedData(
                                dd,
                                globalVatRateOptional,
                                invoice
                        )
                ).toList();

        List<ManualInvoiceSummaryData> manualInvoiceSummaryData = manualInvoiceSummaryDataRepository.saveAll(summeryDataMappedToInvoiceDetailedData);
        List<ManualInvoiceDetailedData> manualInvoiceDetailedData = manualInvoiceDetailedDataRepository.saveAll(detailedDataMappedToInvoiceSummaryData);

        return new ManualInvoiceDataModelPayload(manualInvoiceSummaryData, manualInvoiceDetailedData);
    }

    /**
     * Calculates the payment deadline for a billing start date and a billing run.
     *
     * @param billingStartDate The billing start date.
     * @param billingRun       The billing run.
     * @return The payment deadline.
     */
    private LocalDate calculatePaymentDeadLine(BillingRunObjectType billingRunObjectType, LocalDate billingStartDate, BillingRun billingRun) {
        switch (billingRunObjectType) {
            case ONLY_CUSTOMER -> {
                LocalDate invoiceDueDate = billingRun.getInvoiceDueDate();
                if (Objects.isNull(invoiceDueDate)) {
                    throw new IllegalArgumentsProvidedException("Cannot determinate payment deadline, invoice due date must be defined while only customer is selected for manual invoice");
                } else {
                    return billingRun.getInvoiceDate();
                }
            }
            case PRODUCT_CONTRACT -> {
                return calculateProductContractPaymentDeadline(billingStartDate, billingRun);
            }
            case SERVICE_CONTRACT -> {
                return calculateServiceContractPaymentDeadline(billingStartDate, billingRun);
            }
            case GOODS_ORDER -> {
                return calculateGoodsOrderPaymentDeadline(billingRun);
            }
            case SERVICE_ORDER -> {
                return calculateServiceOrderPaymentDeadline(billingRun);
            }
            default -> throw new IllegalArgumentsProvidedException("Cannot determinate payment deadline");
        }
    }

    /**
     * Calculates the payment deadline for a service order based on a billing run.
     *
     * @param billingRun the billing run associated with the service order
     * @return the payment deadline as a LocalDate object
     * @throws DomainEntityNotFoundException if the service order with the given ID is not found
     */
    private LocalDate calculateServiceOrderPaymentDeadline(BillingRun billingRun) {
        ServiceOrder serviceOrder = serviceOrderRepository
                .findById(billingRun.getServiceOrderId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Service Order with id: [%s] not found;".formatted(billingRun.getServiceOrderId())));

        return calculatePaymentDeadlineByInvoicePaymentTerms(billingRun, Optional.ofNullable(serviceOrder.getInvoicePaymentTermId()).orElseThrow(() -> new IllegalArgumentsProvidedException("Service Order payment term id is not defined")), serviceOrder.getInvoicePaymentTermValue());
    }

    /**
     * Calculates the payment deadline for a goods order based on the billing run information.
     *
     * @param billingRun The billing run associated with the goods order.
     * @return The payment deadline for the goods order.
     * @throws DomainEntityNotFoundException If the goods order with the given ID is not found.
     * @throws ClientException               If the goods order payment term is not defined or if there are illegal arguments provided.
     */
    private LocalDate calculateGoodsOrderPaymentDeadline(BillingRun billingRun) {
        GoodsOrder goodsOrder = goodsOrderRepository
                .findById(billingRun.getGoodsOrderId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods order with id: [%s] not found;".formatted(billingRun.getGoodsOrderId())));

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

    record ManualInvoiceDataModelPayload(
            List<ManualInvoiceSummaryData> summaryData,
            List<ManualInvoiceDetailedData> detailedData
    ) {

    }

    record ManualInvoiceFieldsPayload(
            String costCenterControllingOrder,
            String incomeAccountNumber
    ) {
    }
}
