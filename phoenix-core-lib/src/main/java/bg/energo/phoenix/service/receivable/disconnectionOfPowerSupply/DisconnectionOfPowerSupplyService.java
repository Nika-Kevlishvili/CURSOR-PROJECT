package bg.energo.phoenix.service.receivable.disconnectionOfPowerSupply;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionOfPowerSupply;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionOfPowerSupplyPod;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.*;
import bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.listing.DisconnectionPowerSupplyListingListColumns;
import bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.listing.DisconnectionPowerSupplyListingRequest;
import bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply.listing.DisconnectionPowerSupplyListingSearchByEnums;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceReconnectionDto;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.*;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryDataRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyPodRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.document.TaxInvoiceDocumentGenerationService;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalProcessService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
import static bg.energo.phoenix.util.epb.EPBDecimalUtils.convertToCurrencyScale;

@Slf4j
@Service
public class DisconnectionOfPowerSupplyService {

    private static final String PREFIX = "Disconnection";
    private static final String CUSTOMER_LIABILITY_PREFIX = "Liability-";
    private final DisconnectionPowerSupplyRepository disconnectionPowerSupplyRepository;
    private final DisconnectionPowerSupplyPodRepository disconnectionPowerSupplyPodRepository;
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final TemplateRepository templateRepository;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final TaskService taskService;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final ProductContractDetailsRepository productContractRepository;
    private final VatRateRepository vatRateRepository;
    private final CurrencyRepository currencyRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final InvoiceEventPublisher invoiceEventPublisher;
    private final InvoiceReversalProcessService invoiceReversalProcessService;
    private final TaxInvoiceDocumentGenerationService taxInvoiceDocumentGenerationService;
    private final PlatformTransactionManager platformTransactionManager;
    private TransactionTemplate transactionTemplate;
    private final InvoiceNumberService invoiceNumberService;

    public DisconnectionOfPowerSupplyService(
            DisconnectionPowerSupplyRepository disconnectionPowerSupplyRepository,
            DisconnectionPowerSupplyPodRepository disconnectionPowerSupplyPodRepository,
            DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository,
            TemplateRepository templateRepository, PermissionService permissionService,
            FileService fileService, TaskService taskService, PointOfDeliveryRepository pointOfDeliveryRepository,
            InvoiceRepository invoiceRepository, CustomerDetailsRepository customerDetailsRepository,
            ContractBillingGroupRepository contractBillingGroupRepository,
            ProductContractDetailsRepository productContractRepository,
            VatRateRepository vatRateRepository,
            CurrencyRepository currencyRepository,
            InvoiceVatRateValueRepository invoiceVatRateValueRepository,
            ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            InvoiceEventPublisher invoiceEventPublisher, InvoiceReversalProcessService invoiceReversalProcessService,
            TaxInvoiceDocumentGenerationService taxInvoiceDocumentGenerationService,
            PlatformTransactionManager platformTransactionManager,
            IncomeAccountNameRepository incomeAccountNameRepository,
            InvoiceNumberService invoiceNumberService
    ) {
        this.disconnectionPowerSupplyRepository = disconnectionPowerSupplyRepository;
        this.disconnectionPowerSupplyPodRepository = disconnectionPowerSupplyPodRepository;
        this.disconnectionPowerSupplyRequestRepository = disconnectionPowerSupplyRequestRepository;
        this.templateRepository = templateRepository;
        this.permissionService = permissionService;
        this.fileService = fileService;
        this.taskService = taskService;
        this.pointOfDeliveryRepository = pointOfDeliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.contractBillingGroupRepository = contractBillingGroupRepository;
        this.productContractRepository = productContractRepository;
        this.vatRateRepository = vatRateRepository;
        this.currencyRepository = currencyRepository;
        this.invoiceVatRateValueRepository = invoiceVatRateValueRepository;
        this.manualDebitOrCreditNoteInvoiceSummaryDataRepository = manualDebitOrCreditNoteInvoiceSummaryDataRepository;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.contractTemplateDetailsRepository = contractTemplateDetailsRepository;
        this.invoiceEventPublisher = invoiceEventPublisher;
        this.invoiceReversalProcessService = invoiceReversalProcessService;
        this.taxInvoiceDocumentGenerationService = taxInvoiceDocumentGenerationService;
        this.platformTransactionManager = platformTransactionManager;
        this.incomeAccountNameRepository = incomeAccountNameRepository;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.invoiceNumberService = invoiceNumberService;
    }

    /**
     * Service method to create a new Disconnection of Power Supply record.
     * This method handles both saving as draft and saving with execution, based on the save type provided in the request.
     *
     * @param disconnectionOfPowerSupplyRequest the request object containing all necessary information for creating a Disconnection of Power Supply.
     * @return the unique identifier (sequence value) of the newly created Disconnection of Power Supply.
     * @throws ClientException if the user does not have the required permissions to perform the action or some error is happened.
     */
    @Transactional
    public Long create(DisconnectionOfPowerSupplyRequest disconnectionOfPowerSupplyRequest) {
        if (disconnectionOfPowerSupplyRequest.saveType() == PowerSupplyDisconnectionStatus.DRAFT && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_SAVE_DRAFT)) {
            throw new ClientException("You don't have permission to save as draft;", ErrorCode.ACCESS_DENIED);
        }

        if (disconnectionOfPowerSupplyRequest.saveType() == PowerSupplyDisconnectionStatus.EXECUTED && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_SAVE_AND_EXECUTE)) {
            throw new ClientException("You don't have permission to save and execute;", ErrorCode.ACCESS_DENIED);
        }

        disconnectionPowerSupplyRequestRepository.findByIdAndDisconnectionRequestsStatusAndStatus(
                        disconnectionOfPowerSupplyRequest.requestForDisconnectionId(),
                        DisconnectionRequestsStatus.EXECUTED, EntityStatus.ACTIVE)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Disconnection of power supply request with id [%s] and parameters not found;".formatted(
                                disconnectionOfPowerSupplyRequest.requestForDisconnectionId()
                        ))
                );

        Long sequenceValue = disconnectionPowerSupplyRepository.getNextSequenceValue();

        DisconnectionOfPowerSupply disconnectionOfPowerSupply = DisconnectionOfPowerSupply.builder().id(sequenceValue).powerSupplyDisconnectionRequestId(
                        disconnectionOfPowerSupplyRequest.requestForDisconnectionId())
                .disconnectionNumber("%s-%s".formatted(PREFIX, sequenceValue))
                .disconnectionStatus(disconnectionOfPowerSupplyRequest.saveType()).status(EntityStatus.ACTIVE).build();

        disconnectionPowerSupplyRepository.saveAndFlush(disconnectionOfPowerSupply);

        validateAndSetPods(disconnectionOfPowerSupplyRequest, sequenceValue);

        if (disconnectionOfPowerSupplyRequest.saveType().equals(PowerSupplyDisconnectionStatus.EXECUTED)) {
            List<TaxCalculationExpressReconnectionResponse> taxCalculationResponses = disconnectionPowerSupplyRepository.fetchLiabilities(disconnectionOfPowerSupply.getId());
            taxCalculationResponses.forEach(this::createInvoiceAndLiability);
            taxCalculation(disconnectionOfPowerSupply.getId());
        }

        return sequenceValue;
    }

    private void createInvoiceAndLiability(TaxCalculationExpressReconnectionResponse response) {
        ContractBillingGroup contractBillingGroup = contractBillingGroupRepository
                .findById(response.getBillingGroupId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing group not found!"));

        Invoice liabilityInvoice = invoiceRepository
                .findById(response.getInvoiceId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found!"));

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(liabilityInvoice.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found!"));

        ProductContractDetails productContract = productContractRepository
                .findById(liabilityInvoice.getProductContractDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract not found!"));

        VatRate vatRate = vatRateRepository
                .findGlobalVatRate(LocalDate.now(), PageRequest.of(0, 1))
                .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found!"));

        Currency currency = currencyRepository
                .findById(response.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("currency not found"));

        CustomerDetails alternative = customerDetailsRepository
                .findById(liabilityInvoice.getAlternativeRecipientCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Alternative recipient not found!"));

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .findRespectiveTemplateDetailsByTemplateIdAndDate(response.getDocumentTemplateId(), LocalDate.now())
                .orElseThrow(() -> new DomainEntityNotFoundException("template detail not found for given template with id %s".formatted(response.getDocumentTemplateId())));

        BigDecimal valueOfVat = response.getTaxForExpressReconnection().multiply(vatRate.getValueInPercent()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmountIncludingVat = response.getTaxForExpressReconnection().add(valueOfVat).setScale(2, RoundingMode.HALF_UP);

        Pair<Invoice, ManualDebitOrCreditNoteInvoiceSummaryData> invoiceSummaryDataPair = transactionTemplate.execute(i ->
                generateInvoice(
                        response,
                        liabilityInvoice,
                        customerDetails,
                        currency,
                        productContract,
                        contractBillingGroup,
                        templateDetail,
                        totalAmountIncludingVat,
                        valueOfVat,
                        vatRate,
                        alternative
                )
        );

        taxInvoiceDocumentGenerationService.generateDocumentForInvoice(
                invoiceSummaryDataPair.getLeft(),
                invoiceSummaryDataPair.getRight(),
                templateDetail
        );
    }

    private Pair<Invoice, ManualDebitOrCreditNoteInvoiceSummaryData> generateInvoice(TaxCalculationExpressReconnectionResponse response, Invoice liabilityInvoice, CustomerDetails customerDetails, Currency currency, ProductContractDetails productContract, ContractBillingGroup contractBillingGroup, ContractTemplateDetail templateDetail, BigDecimal totalAmountIncludingVat, BigDecimal valueOfVat, VatRate vatRate, CustomerDetails alternative) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(response.getCustomerId());
        invoice.setAccountPeriodId(liabilityInvoice.getAccountPeriodId());
        invoice.setContractBillingGroupId(response.getBillingGroupId());
        invoice.setIncomeAccountNumber(response.getNumberOfIncomeAccount());
        invoice.setBasisForIssuing(response.getBasisForIssuing());
        invoice.setCostCenterControllingOrder(response.getCostCenterControllingOrder());
        invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
        invoice.setInvoiceStatus(InvoiceStatus.REAL);
        invoice.setCustomerDetailId(customerDetails.getId());
        invoice.setCurrencyId(currency.getId());
        invoice.setCurrencyExchangeRateOnInvoiceCreation(currency.getAltCurrencyExchangeRate());
        invoice.setCurrencyIdInOtherCurrency(currency.getAltCurrencyId());
        invoice.setProductContractId(productContract.getContractId());
        invoice.setProductContractDetailId(productContract.getId());
        invoice.setTaxEventDate(LocalDate.now());
        if (contractBillingGroup.getBillingCustomerCommunicationId() != null) {
            invoice.setCustomerCommunicationId(contractBillingGroup.getBillingCustomerCommunicationId());
        } else {
            invoice.setContractCommunicationId(productContract.getCustomerCommunicationIdForBilling());
        }
        if (contractBillingGroup.getAlternativeRecipientCustomerDetailId() == null) {
            invoice.setAlternativeRecipientCustomerDetailId(customerDetails.getId());
        } else {
            invoice.setAlternativeRecipientCustomerDetailId(liabilityInvoice.getAlternativeRecipientCustomerDetailId());
        }
        invoice.setTemplateDetailId(templateDetail.getId());
        invoice.setInvoiceType(InvoiceType.RECONNECTION);
        invoice.setTotalAmountExcludingVat(response.getTaxForExpressReconnection());
        invoice.setTotalAmountIncludingVat(totalAmountIncludingVat);
        invoice.setTotalAmountOfVatInOtherCurrency(convertToCurrencyScale(valueOfVat.multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setTotalAmountIncludingVatInOtherCurrency(convertToCurrencyScale(totalAmountIncludingVat.multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setTotalAmountExcludingVatInOtherCurrency(convertToCurrencyScale(response.getTaxForExpressReconnection().multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDirectDebit((contractBillingGroup.getDirectDebit() != null && contractBillingGroup.getDirectDebit()) || (productContract.getDirectDebit() != null && productContract.getDirectDebit()) || customerDetails.getDirectDebit() != null && customerDetails.getDirectDebit());
        Long bankId = null;
        String iban = null;
        if (contractBillingGroup.getDirectDebit()) {
            bankId = contractBillingGroup.getBankId();
            iban = contractBillingGroup.getIban();
        } else if (productContract.getDirectDebit()) {
            bankId = productContract.getBankId();
            iban = productContract.getIban();
        } else if (customerDetails.getDirectDebit()) {
            bankId = customerDetails.getBank().getId();
            iban = customerDetails.getIban();
        }
        invoice.setBankId(bankId);
        invoice.setIban(iban);
        invoice.setTotalAmountOfVat(valueOfVat);
        invoice.setPaymentDeadline(invoice.getTaxEventDate());
        invoice.setProductDetailId(productContract.getProductDetailId());
        invoiceRepository.saveAndFlush(invoice);
        invoice = invoiceRepository.saveAndFlush(invoice);
        invoiceNumberService.fillInvoiceNumber(invoice);

        InvoiceVatRateValue invoiceVatRateValue = new InvoiceVatRateValue(null, vatRate.getValueInPercent(), response.getTaxForExpressReconnection(),
                valueOfVat,
                invoice.getId());
        invoiceVatRateValueRepository.save(invoiceVatRateValue);
        ManualDebitOrCreditNoteInvoiceSummaryData manualDebitOrCreditNoteInvoiceSummaryData = getManualDebitOrCreditNoteInvoiceSummaryData(response, invoice, currency, vatRate.getName());
        manualDebitOrCreditNoteInvoiceSummaryDataRepository.save(manualDebitOrCreditNoteInvoiceSummaryData);
        createLiability(
                invoice.getAccountPeriodId(), invoice.getPaymentDeadline(), totalAmountIncludingVat, invoice.getTotalAmountIncludingVatInOtherCurrency(),
                invoice.getCurrencyId(), invoice.getInvoiceNumber(), response.getNumberOfIncomeAccount(), response.getCostCenterControllingOrder(), response.getBasisForIssuing(),
                invoice.getDirectDebit(), invoice.getCustomerId(), invoice.getContractBillingGroupId(),
                contractBillingGroup.getAlternativeRecipientCustomerDetailId() == null ? customerDetails.getCustomerId() : alternative.getCustomerId(), invoice.getId()
        );
        return Pair.of(invoice, manualDebitOrCreditNoteInvoiceSummaryData);
    }

    private void createLiability(
            Long accountingPeriodId, LocalDate dueDate, BigDecimal initialAmount, BigDecimal initialAmountInOtherCcy, Long currencyId, String invoiceNumber,
            String numberOfIncomeAccount, String costCenterControllingOrder, String basisForIssuing, boolean directDebit, Long customerId,
            Long billingGroupId, Long alternativeRecipient, Long invoiceId
    ) {
        CustomerLiability customerLiability = new CustomerLiability();
        customerLiability.setAccountPeriodId(accountingPeriodId);
        customerLiability.setDueDate(dueDate);
        customerLiability.setInvoiceId(invoiceId);
        customerLiability.setInitialAmount(initialAmount);
        customerLiability.setInitialAmountInOtherCurrency(initialAmountInOtherCcy);
        customerLiability.setCurrentAmount(initialAmount);
        customerLiability.setCurrentAmountInOtherCurrency(initialAmountInOtherCcy);
        customerLiability.setCurrencyId(currencyId);
        customerLiability.setOutgoingDocumentFromExternalSystem(invoiceNumber);
        customerLiability.setIncomeAccountNumber(numberOfIncomeAccount);
        customerLiability.setCostCenterControllingOrder(costCenterControllingOrder);
        customerLiability.setBasisForIssuing(basisForIssuing);
        customerLiability.setContractBillingGroupId(billingGroupId);
        customerLiability.setAltInvoiceRecipientCustomerId(alternativeRecipient);
        customerLiability.setDirectDebit(directDebit);
        customerLiability.setCustomerId(customerId);
        customerLiability.setLiabilityNumber("temp");
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setCreationType(CreationType.AUTOMATIC);
        String incomeAccountName = incomeAccountNameRepository.findByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
        if (incomeAccountName != null) {
            IncomeAccountName incomeAccount = incomeAccountNameRepository.findByName(incomeAccountName)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Income account name not found with name: %s;".formatted(incomeAccountName)));
            customerLiability.setIncomeAccountNumber(incomeAccount.getNumber());
        }
        customerLiability.setOccurrenceDate(LocalDate.now());

        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
    }

    private ManualDebitOrCreditNoteInvoiceSummaryData getManualDebitOrCreditNoteInvoiceSummaryData(TaxCalculationExpressReconnectionResponse response, Invoice invoice, Currency currency, String name) {
        ManualDebitOrCreditNoteInvoiceSummaryData manualDebitOrCreditNoteInvoiceSummaryData = new ManualDebitOrCreditNoteInvoiceSummaryData();
        manualDebitOrCreditNoteInvoiceSummaryData.setInvoiceId(invoice.getId());
        manualDebitOrCreditNoteInvoiceSummaryData.setValue(response.getTaxForExpressReconnection());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyId(response.getCurrencyId());
        manualDebitOrCreditNoteInvoiceSummaryData.setPriceComponentOrPriceComponentGroups(response.getPriceComponent());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyName(currency.getName());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyExchangeRate(currency.getAltCurrencyExchangeRate());
        manualDebitOrCreditNoteInvoiceSummaryData.setVatRateName(name);
        return manualDebitOrCreditNoteInvoiceSummaryData;
    }

    private void validateAndSetPods(DisconnectionOfPowerSupplyRequest disconnectionOfPowerSupplyRequest, Long sequenceValue) {
        List<String> errorMessages = new ArrayList<>();
        List<DisconnectionOfPowerSupplyPod> disconnectionOfPowerSupplyPods = new ArrayList<>();
        Set<DisconnectionOfPowerSupplyDisconnectedRequest> disconnectionOfPowerSupplyDisconnectedRequests = new HashSet<>(disconnectionOfPowerSupplyRequest.disconnectedRequest());
        if (!disconnectionOfPowerSupplyDisconnectedRequests.isEmpty()) {
            List<DisconnectionPowerSupplyPodsMiddleResponse> podIdentifiersForDraftTable = disconnectionPowerSupplyRepository.getTable(disconnectionOfPowerSupplyRequest.requestForDisconnectionId(), sequenceValue);

            if (podIdentifiersForDraftTable.isEmpty()) {
                throw new DomainEntityNotFoundException("Table has no pods for this request for disconnection id [%s];".formatted(disconnectionOfPowerSupplyRequest.requestForDisconnectionId()));
            }

            List<DisconnectionPowerSupplyTableList> collect = podIdentifiersForDraftTable.stream().map(DisconnectionPowerSupplyTableList::new).toList();
            List<PointOfDelivery> pods = new ArrayList<>();

            for (DisconnectionOfPowerSupplyDisconnectedRequest request : disconnectionOfPowerSupplyDisconnectedRequests) {
                for (DisconnectionPowerSupplyTableList disconnection : collect) {
                    if (request.podId().equals(disconnection.podId())) {
                        PodAndGotMappedResponse podAndGridOperatorTaxById = disconnectionPowerSupplyRepository.findPodAndGridOperatorTaxById(request.podId(), request.gridOperatorTaxesId());
                        if (podAndGridOperatorTaxById.podId() != null && podAndGridOperatorTaxById.podIdentifier() != null) {
                            if (podAndGridOperatorTaxById.gotId() != null && podAndGridOperatorTaxById.disconnectionType() != null) {
                                DisconnectionOfPowerSupplyPod disconnectionOfPowerSupplyPod = DisconnectionOfPowerSupplyPod.builder()
                                        .withIsChecked(true)
                                        .withCustomerId(request.customerId())
                                        .withPodId(request.podId())
                                        .withPowerSupplyDisconnectionId(sequenceValue)
                                        .withGridOperatorTaxId(request.gridOperatorTaxesId())
                                        .withDisconnectionDate(request.dateOfDisconnection())
                                        .withExpressReconnection(request.expressReconnection())
                                        .build();
                                disconnectionPowerSupplyPodRepository.findExistingDisconnectionPod(disconnection.podId(), disconnection.customerId(), sequenceValue)
                                        .ifPresent(ofPowerSupplyPod -> disconnectionOfPowerSupplyPod.setId(ofPowerSupplyPod.getId()));
                                disconnectionOfPowerSupplyPods.add(disconnectionOfPowerSupplyPod);

                                if (disconnectionOfPowerSupplyRequest.saveType().equals(PowerSupplyDisconnectionStatus.EXECUTED)) {
                                    Optional<PointOfDelivery> pod = pointOfDeliveryRepository.findById(disconnection.podId());
                                    if (pod.isPresent()) {
                                        pod.get().setDisconnectionPowerSupply(true);
                                        pods.add(pod.get());
                                    }
                                }
                            } else {
                                errorMessages.add("Taxes for grid operator not found for given point of delivery id [%s];".formatted(disconnection.podId()));
                            }
                        } else {
                            errorMessages.add("Point of delivery not found with given id [%s];".formatted(disconnection.podId()));
                        }
                    }
                }
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            pointOfDeliveryRepository.saveAll(pods);
            disconnectionPowerSupplyPodRepository.saveAll(disconnectionOfPowerSupplyPods);
        }
    }

    private void validateAndSetExecutedPods(DisconnectionOfPowerSupplyRequest disconnectionOfPowerSupplyRequest, Long sequenceValue) {
        List<String> errorMessages = new ArrayList<>();
        List<DisconnectionOfPowerSupplyPod> disconnectionOfPowerSupplyPods = new ArrayList<>();
        Set<DisconnectionOfPowerSupplyDisconnectedRequest> disconnectionOfPowerSupplyDisconnectedRequests = new HashSet<>(disconnectionOfPowerSupplyRequest.disconnectedRequest());
        if (!disconnectionOfPowerSupplyDisconnectedRequests.isEmpty()) {
            List<DisconnectionPowerSupplyPodsMiddleResponse> podIdentifiersForExecutedTable = disconnectionPowerSupplyRepository.getExecutedTable(disconnectionOfPowerSupplyRequest.requestForDisconnectionId(), sequenceValue);

            if (podIdentifiersForExecutedTable.isEmpty()) {
                throw new DomainEntityNotFoundException("Table has no pods for this request for disconnection id [%s];".formatted(disconnectionOfPowerSupplyRequest.requestForDisconnectionId()));
            }

            List<DisconnectionPowerSupplyTableList> collect = podIdentifiersForExecutedTable.stream().map(DisconnectionPowerSupplyTableList::new).toList();
            List<PointOfDelivery> pods = new ArrayList<>();

            for (DisconnectionOfPowerSupplyDisconnectedRequest request : disconnectionOfPowerSupplyDisconnectedRequests) {
                for (DisconnectionPowerSupplyTableList disconnection : collect) {
                    if (request.podId().equals(disconnection.podId())) {
                        PodAndGotMappedResponse podAndGridOperatorTaxById = disconnectionPowerSupplyRepository.findPodAndGridOperatorTaxById(request.podId(), request.gridOperatorTaxesId());
                        if (podAndGridOperatorTaxById.podId() != null && podAndGridOperatorTaxById.podIdentifier() != null) {
                            if (podAndGridOperatorTaxById.gotId() != null && podAndGridOperatorTaxById.disconnectionType() != null) {
                                DisconnectionOfPowerSupplyPod disconnectionOfPowerSupplyPod = DisconnectionOfPowerSupplyPod.builder()
                                        .withIsChecked(true)
                                        .withCustomerId(request.customerId())
                                        .withPodId(request.podId())
                                        .withPowerSupplyDisconnectionId(sequenceValue)
                                        .withGridOperatorTaxId(request.gridOperatorTaxesId())
                                        .withDisconnectionDate(request.dateOfDisconnection())
                                        .withExpressReconnection(request.expressReconnection())
                                        .build();
                                disconnectionPowerSupplyPodRepository.findExistingDisconnectionPod(disconnection.podId(), disconnection.customerId(), sequenceValue)
                                        .ifPresent(ofPowerSupplyPod -> disconnectionOfPowerSupplyPod.setId(ofPowerSupplyPod.getId()));
                                disconnectionOfPowerSupplyPods.add(disconnectionOfPowerSupplyPod);

                                if (disconnectionOfPowerSupplyRequest.saveType().equals(PowerSupplyDisconnectionStatus.EXECUTED)) {
                                    Optional<PointOfDelivery> pod = pointOfDeliveryRepository.findById(disconnection.podId());
                                    if (pod.isPresent()) {
                                        pod.get().setDisconnectionPowerSupply(true);
                                        pods.add(pod.get());
                                    }
                                }
                            } else {
                                errorMessages.add("Taxes for grid operator not found for given point of delivery id [%s];".formatted(disconnection.podId()));
                            }
                        } else {
                            errorMessages.add("Point of delivery not found with given id [%s];".formatted(disconnection.podId()));
                        }
                    }
                }
            }

            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            pointOfDeliveryRepository.saveAll(pods);
            disconnectionPowerSupplyPodRepository.saveAll(disconnectionOfPowerSupplyPods);
        }
    }

    @Transactional
    public Long update(Long id, DisconnectionOfPowerSupplyRequest disconnectionOfPowerSupplyRequest) {
        DisconnectionOfPowerSupply disconnectionOfPowerSupply = disconnectionPowerSupplyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Disconnection of the power supply not found with given id [%s];".formatted(id)));

        if (!disconnectionOfPowerSupply.getDisconnectionStatus().equals(PowerSupplyDisconnectionStatus.EXECUTED)) {
            if (Objects.equals(disconnectionOfPowerSupply.getPowerSupplyDisconnectionRequestId(), disconnectionOfPowerSupplyRequest.requestForDisconnectionId())) {
                if (disconnectionOfPowerSupplyRequest.saveType() == PowerSupplyDisconnectionStatus.DRAFT && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_SAVE_DRAFT)) {
                    throw new ClientException("You don't have permission to save as draft;", ErrorCode.ACCESS_DENIED);
                }

                if (disconnectionOfPowerSupplyRequest.saveType() == PowerSupplyDisconnectionStatus.EXECUTED && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_SAVE_AND_EXECUTE)) {
                    throw new ClientException("You don't have permission to save and execute;", ErrorCode.ACCESS_DENIED);
                }

                if (!Objects.equals(disconnectionOfPowerSupplyRequest.saveType(), disconnectionOfPowerSupply.getDisconnectionStatus())) {
                    disconnectionOfPowerSupply.setDisconnectionStatus(disconnectionOfPowerSupplyRequest.saveType());
                }
            } else {
                disconnectionPowerSupplyRequestRepository
                        .findByIdAndDisconnectionRequestsStatusAndStatus(disconnectionOfPowerSupplyRequest.requestForDisconnectionId(), DisconnectionRequestsStatus.EXECUTED, EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Disconnection of power supply request with id [%s] and parameters not found;".formatted(disconnectionOfPowerSupplyRequest.requestForDisconnectionId())));

                disconnectionOfPowerSupply.setPowerSupplyDisconnectionRequestId(disconnectionOfPowerSupplyRequest.requestForDisconnectionId());
                disconnectionOfPowerSupply.setDisconnectionStatus(disconnectionOfPowerSupplyRequest.saveType());
            }
        }

        disconnectionPowerSupplyPodRepository.deleteAllByPowerSupplyDisconnectionId(id);
        disconnectionPowerSupplyRepository.save(disconnectionOfPowerSupply);
        if (disconnectionOfPowerSupplyRequest.saveType() == PowerSupplyDisconnectionStatus.EXECUTED) {
            validateAndSetExecutedPods(disconnectionOfPowerSupplyRequest, id);
        } else {
            validateAndSetPods(disconnectionOfPowerSupplyRequest, id);
        }

        if (disconnectionOfPowerSupplyRequest.saveType().equals(PowerSupplyDisconnectionStatus.EXECUTED)) {
            List<TaxCalculationExpressReconnectionResponse> taxCalculationResponses = disconnectionPowerSupplyRepository.fetchLiabilities(disconnectionOfPowerSupply.getId());
            taxCalculationResponses.forEach(this::createInvoiceAndLiability);
            taxCalculation(disconnectionOfPowerSupply.getId());
        }

        return id;
    }

    /**
     * Service method to view a Disconnection of Power Supply record by its ID.
     * This method checks the status and permissions required to view the disconnection record.
     *
     * @param id the unique identifier of the Disconnection of Power Supply to be viewed.
     * @return a {@link DisconnectionOfPowerSupplyResponse} containing the details of the Disconnection of Power Supply.
     * @throws DomainEntityNotFoundException if the Disconnection of Power Supply with the given ID is not found.
     * @throws ClientException               if the user does not have the required permissions to view the disconnection record based on its status.
     */
    public DisconnectionOfPowerSupplyResponse view(Long id) {
        DisconnectionOfPowerSupply disconnectionOfPowerSupply = disconnectionPowerSupplyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Disconnection of power supply with id [%s] not found;".formatted(id)));

        if (disconnectionOfPowerSupply.getStatus().equals(EntityStatus.ACTIVE)) {
            if (disconnectionOfPowerSupply.getDisconnectionStatus().equals(PowerSupplyDisconnectionStatus.DRAFT) && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT)) {
                throw new ClientException("You have no permission to view draft disconnection of power supply;", ErrorCode.ACCESS_DENIED);
            }
            if (disconnectionOfPowerSupply.getDisconnectionStatus().equals(PowerSupplyDisconnectionStatus.EXECUTED) && !hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED)) {
                throw new ClientException("You have no permission to view executed disconnection of power supply;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETED)) {
                throw new ClientException("You have no permission to view deleted disconnection of power supply;", ErrorCode.ACCESS_DENIED);
            }
        }

        return new DisconnectionOfPowerSupplyResponse(
                id,
                disconnectionOfPowerSupply.getDisconnectionStatus(),
                disconnectionOfPowerSupply.getPowerSupplyDisconnectionRequestId(),
                getTasks(id),
                disconnectionOfPowerSupply.getStatus()
        );
    }

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByDisconnectionOfPowerSupplyId(id);
    }

    /**
     * Filters and retrieves a paginated list of disconnection power supply listings based on the provided request parameters.
     *
     * @param request the request object containing various filtering and sorting criteria
     * @return a paginated list of {@link DisconnectionPowerSupplyListingResponse} that matches the filtering criteria
     * @throws IllegalArgumentException if the 'createDateFrom' is after 'createDateTo' or if 'numberOfPodsFrom' is greater than 'numberOfPodsTo'
     */
    public Page<DisconnectionPowerSupplyListingResponse> filter(DisconnectionPowerSupplyListingRequest request) {
        Sort order = EPBListingUtils.extractSortBy(request.getDirection(), request.getSortBy(), DisconnectionPowerSupplyListingListColumns.NUMBER, DisconnectionPowerSupplyListingListColumns::getValue);
        if (Objects.nonNull(request.getCreateDateFrom()) && Objects.nonNull(request.getCreateDateTo()) && request.getCreateDateFrom().isAfter(request.getCreateDateTo())) {
            throw new IllegalArgumentException("createDateFrom-createDateFrom can not be after createDateTo;");
        }
        if (Objects.nonNull(request.getNumberOfPodsFrom()) && Objects.nonNull(request.getNumberOfPodsTo()) && request.getNumberOfPodsFrom() > (request.getNumberOfPodsTo())) {
            throw new IllegalArgumentException("numberOfPodsFrom-numberOfPodsFrom can not be more than numberOfPodsTo;");
        }

        List<String> entityStatuses = getDisconnectionEntityStatuses();
        if (!CollectionUtils.isEmpty(request.getStatuses())) {
            entityStatuses.retainAll(request.getStatuses().stream().map(Enum::name).collect(Collectors.toList()));
        }
        return disconnectionPowerSupplyRepository.filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListingUtils.extractSearchBy(request.getSearchBy(), DisconnectionPowerSupplyListingSearchByEnums.ALL),
                        request.getCreateDateFrom(),
                        request.getCreateDateTo(),
                        getDisconnectionModifiedStatuses(request.getDisconnectionStatuses()),
                        request.getNumberOfPodsFrom(),
                        request.getNumberOfPodsTo(),
                        entityStatuses,
                        CollectionUtils.isEmpty(request.getGridOperatorIds()) ? new ArrayList<>() : request.getGridOperatorIds(),
                        PageRequest.of(request.getPage(), request.getSize(), order))
                .map(DisconnectionPowerSupplyListingResponse::new);
    }

    /**
     * Deletes a Disconnection of Power Supply by its id.
     *
     * @param id the id of the Disconnection of Power Supply to be deleted
     * @return the id of the deleted Disconnection of Power Supply
     * @throws ClientException              if the Disconnection of Power Supply with the given id is not found,
     *                                      or if it is already deleted
     * @throws OperationNotAllowedException if the Disconnection of Power Supply has an EXECUTED status
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting Disconnection of Power Supply with id: {}", id);

        DisconnectionOfPowerSupply disconnectionOfPowerSupply = disconnectionPowerSupplyRepository.findById(id).orElseThrow(() -> new ClientException("Can't find Disconnection of Power Supply with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (disconnectionOfPowerSupply.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Disconnection of Power Supply is already deleted;");
            throw new ClientException("Disconnection of Power Supply is already deleted;", APPLICATION_ERROR);
        }

        if (Objects.equals(disconnectionOfPowerSupply.getDisconnectionStatus(), PowerSupplyDisconnectionStatus.EXECUTED)) {
            log.error("can not delete Disconnection of Power Supply with EXECUTED status;");
            throw new OperationNotAllowedException("can not delete Disconnection of Power Supply with EXECUTED status;");
        }

        disconnectionOfPowerSupply.setStatus(EntityStatus.DELETED);
        disconnectionPowerSupplyRepository.save(disconnectionOfPowerSupply);
        return disconnectionOfPowerSupply.getId();
    }

    /**
     * Retrieves a paginated list of Disconnection of Power Supply records in draft status based on the provided request criteria.
     *
     * @param disconnectionOfPowerSupplyGetDraftTableRequest the request object containing criteria for filtering and pagination.
     * @return a {@link Page} of {@link DisconnectionPowerSupplyTableList} representing the paginated list of draft Disconnection of Power Supply records.
     */
    public Page<DisconnectionPowerSupplyTableList> getDraftTable(DisconnectionOfPowerSupplyGetDraftTableRequest disconnectionOfPowerSupplyGetDraftTableRequest) {
        return disconnectionPowerSupplyRepository.getDraftTable(disconnectionOfPowerSupplyGetDraftTableRequest.requestForDisconnectionId(), disconnectionOfPowerSupplyGetDraftTableRequest.disconnectionId(), EPBStringUtils.fromPromptToQueryParameter(disconnectionOfPowerSupplyGetDraftTableRequest.prompt()), EPBListingUtils.extractSearchBy(disconnectionOfPowerSupplyGetDraftTableRequest.searchField(), DisconnectionOfPowerSupplySearchFields.ALL), PageRequest.of(disconnectionOfPowerSupplyGetDraftTableRequest.page(), disconnectionOfPowerSupplyGetDraftTableRequest.size())).map(DisconnectionPowerSupplyTableList::new);
    }

    /**
     * Retrieves a paginated list of Disconnection of Power Supply records in executed status based on the provided request criteria.
     *
     * @param disconnectionOfPowerSupplyGetExecutedTableRequest the request object containing criteria for filtering and pagination.
     * @return a {@link Page} of {@link DisconnectionPowerSupplyTableList} representing the paginated list of executed Disconnection of Power Supply records.
     */
    public Page<DisconnectionPowerSupplyTableList> getExecutedTable(DisconnectionOfPowerSupplyGetExecutedTableRequest disconnectionOfPowerSupplyGetExecutedTableRequest) {
        return disconnectionPowerSupplyRepository.getExecutedTable(disconnectionOfPowerSupplyGetExecutedTableRequest.disconnectionId(), EPBStringUtils.fromPromptToQueryParameter(disconnectionOfPowerSupplyGetExecutedTableRequest.prompt()), EPBListingUtils.extractSearchBy(disconnectionOfPowerSupplyGetExecutedTableRequest.searchField(), DisconnectionOfPowerSupplySearchFields.ALL), PageRequest.of(disconnectionOfPowerSupplyGetExecutedTableRequest.page(), disconnectionOfPowerSupplyGetExecutedTableRequest.size())).map(DisconnectionPowerSupplyTableList::new);
    }

    /**
     * Processes the uploaded Excel file containing Disconnection of Power Supply data.
     * Validates the file format and content against a predefined template, parses the data,
     * and returns a list of parsed DisconnectionPowerSupplyParsedFile objects.
     *
     * @param file the multipart file containing the Excel data to be uploaded and processed.
     * @return a list of {@link DisconnectionPowerSupplyParsedFile} objects representing the parsed data from the uploaded file.
     * @throws DomainEntityNotFoundException if the template for Disconnection of Power Supply is not found in the database.
     * @throws ClientException               if there are errors during file validation or parsing, or if permissions are insufficient.
     */
    public List<DisconnectionPowerSupplyParsedFile> upload(MultipartFile file) {
        EPBExcelUtils.validateFileFormat(file);

        Template template = templateRepository.findById(EPBFinalFields.DISCONNECTION_OF_POWER_SUPPLY).orElseThrow(() -> new DomainEntityNotFoundException("Template for Disconnection Of Power Supply not found;"));

        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(template.getFileUrl()).getByteArray(), 1);

        List<DisconnectionPowerSupplyParsedFile> result = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();
            if (iterator.hasNext()) iterator.next();

            while (iterator.hasNext()) {
                Row row = iterator.next();

                String podIdentifier = EPBExcelUtils.getStringValue(0, row);
                String typeOfDisconnection = EPBExcelUtils.getStringValue(1, row);
                LocalDate disconnectionDate = EPBExcelUtils.getLocalDateValue(2, row);

                PodAndGotMappedResponse podAndGridOperatorTax = disconnectionPowerSupplyRepository.findPodAndGridOperatorTax(podIdentifier, typeOfDisconnection);
                if (podAndGridOperatorTax != null && podAndGridOperatorTax.podId() != null && podAndGridOperatorTax.podIdentifier() != null) {
                    if (podAndGridOperatorTax.gotId() != null && podAndGridOperatorTax.disconnectionType() != null) {
                        DisconnectionPowerSupplyParsedFile disconnectionPowerSupplyParsedFile = new DisconnectionPowerSupplyParsedFile(podAndGridOperatorTax.podId(), podAndGridOperatorTax.podIdentifier(), podAndGridOperatorTax.gotId(), podAndGridOperatorTax.disconnectionType(), disconnectionDate);

                        result.add(disconnectionPowerSupplyParsedFile);
                    } else {
                        errorMessages.add("Type of disconnection [%s] not found for given point of delivery identifier [%s];".formatted(typeOfDisconnection, podIdentifier));
                    }
                } else {
                    errorMessages.add("Point of delivery not found with given identifier [%s];".formatted(podIdentifier));
                }
            }
        } catch (IllegalArgumentsProvidedException e) {
            log.error("Illegal arguments provided in file", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return result;
    }

    /**
     * Retrieves the template file for Disconnection of Power Supply.
     *
     * @return a {@link DisconnectionOfPowerSupplyTemplateContent} object containing the template name and downloadable file content.
     * @throws DomainEntityNotFoundException if the template for Disconnection of Power Supply is not found in the database.
     */
    public DisconnectionOfPowerSupplyTemplateContent downloadTemplate() {
        Template template = templateRepository.findById(EPBFinalFields.DISCONNECTION_OF_POWER_SUPPLY).orElseThrow(() -> new DomainEntityNotFoundException("Template for Disconnection Of Power Supply not found;"));

        return new DisconnectionOfPowerSupplyTemplateContent(template.getTemplateName(), fileService.downloadFile(template.getFileUrl()));
    }

    /**
     * Checks if the current user has the specified permission for Disconnection of Power Supply operations.
     *
     * @param permission the {@link PermissionEnum} value representing the permission to check.
     * @return {@code true} if the user has the specified permission; {@code false} otherwise.
     */
    private boolean hasPermission(PermissionEnum permission) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.DISCONNECTION_OF_POWER_SUPPLY, List.of(permission));
    }

    /**
     * Retrieves a list of entity statuses for disconnections of power supply.
     * The list always includes the 'ACTIVE' status. If the user has permission to delete disconnections,
     * the 'DELETED' status is also included.
     *
     * @return a list of entity statuses
     */
    private List<String> getDisconnectionEntityStatuses() {
        List<String> entityStatuses = new ArrayList<>();
        entityStatuses.add(EntityStatus.ACTIVE.name());
        if (hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_DELETED)) {
            entityStatuses.add(EntityStatus.DELETED.name());
        }
        return entityStatuses;
    }

    /**
     * Retrieves a list of modified disconnection statuses based on the provided disconnection statuses and user permissions.
     * If the provided list is not empty, it adds statuses the user has permission to view.
     * If the provided list is empty, it checks the user's permissions and adds the statuses accordingly.
     *
     * @param disconnectionStatuses the list of disconnection statuses to be modified
     * @return a list of modified disconnection statuses
     */
    private List<String> getDisconnectionModifiedStatuses(List<PowerSupplyDisconnectionStatus> disconnectionStatuses) {
        List<String> modifiedStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(disconnectionStatuses)) {
            disconnectionStatuses.forEach(status -> {
                if (status.equals(PowerSupplyDisconnectionStatus.DRAFT) && hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT)) {
                    modifiedStatuses.add(PowerSupplyDisconnectionStatus.DRAFT.name());
                } else if (status.equals(PowerSupplyDisconnectionStatus.EXECUTED) && hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED)) {
                    modifiedStatuses.add(PowerSupplyDisconnectionStatus.EXECUTED.name());
                }
            });
        } else {
            if (hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_DRAFT)) {
                modifiedStatuses.add(PowerSupplyDisconnectionStatus.DRAFT.name());
            }
            if (hasPermission(DISCONNECTION_OF_POWER_SUPPLY_VIEW_EXECUTED)) {
                modifiedStatuses.add(PowerSupplyDisconnectionStatus.EXECUTED.name());
            }
        }
        return modifiedStatuses;
    }

    public List<DisconnectionRequestList> filterDisconnectionRequests(String prompt) {
        return disconnectionPowerSupplyRepository.findDisconnectionRequests(EPBStringUtils.fromPromptToQueryParameter(prompt));
    }

    public List<TypeOfDisconnectionList> filterDisconnectionType(Long podId) {
        return disconnectionPowerSupplyRepository.findDisconnectionType(podId);
    }

    private void taxCalculation(Long id) {
        List<TaxCalculationExpressReconnectionResponse> taxCalculationResponses = disconnectionPowerSupplyRepository.fetchLiabilitiesForTaxProcess(id);
        for (TaxCalculationExpressReconnectionResponse taxCalculationResponse : taxCalculationResponses) {
            if (taxCalculationResponse.getSavedInvoiceId() == null) {
                createInvoiceAndLiability(taxCalculationResponse);
            } else {
                VatRate vatRate = vatRateRepository.findGlobalVatRate(LocalDate.now(), PageRequest.of(0, 1))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found!"));
                Invoice invoice = invoiceRepository.findById(taxCalculationResponse.getSavedInvoiceId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with ID: %s".formatted(taxCalculationResponse.getSavedInvoiceId())));
                BigDecimal valueOfVat = taxCalculationResponse.getTaxForExpressReconnection().multiply(vatRate.getValueInPercent()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                BigDecimal totalAmountIncludingVat = taxCalculationResponse.getTaxForExpressReconnection().add(valueOfVat).setScale(2, RoundingMode.HALF_UP);

                if (!Objects.equals(invoice.getTotalAmountIncludingVat(), totalAmountIncludingVat)) {
                    InvoiceReconnectionDto invoiceReconnectionDto = new InvoiceReconnectionDto(taxCalculationResponse.getTaxForExpressReconnection(), valueOfVat, totalAmountIncludingVat, vatRate.getValueInPercent());
                    invoiceReversalProcessService.processReconnection(invoice.getId(), invoiceReconnectionDto);
                }
            }
        }
    }
}
