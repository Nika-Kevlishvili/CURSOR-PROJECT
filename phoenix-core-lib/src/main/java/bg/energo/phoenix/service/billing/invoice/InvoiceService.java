package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.exception.AccessDeniedException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.billing.invoice.InvoiceDataListingRequest;
import bg.energo.phoenix.model.request.billing.invoice.InvoiceListingRequest;
import bg.energo.phoenix.model.request.receivable.payment.InvoiceForPaymentListingRequest;
import bg.energo.phoenix.model.response.billing.invoice.*;
import bg.energo.phoenix.model.response.customer.CustomerVersionShortResponse;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.InvoiceCancellationDocumentRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.compensation.CompensationRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.CompensationReceivableService;
import bg.energo.phoenix.util.UrlEncodingUtil;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final InvoiceMapper invoiceMapper;
    private final BankRepository bankRepository;
    private final InvoiceRepository invoiceRepository;
    private final PermissionService permissionService;
    private final CurrencyRepository currencyRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final BillingRunRepository billingRunRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final InterestRateRepository interestRateRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final InvoiceDetailedDataRepository invoiceDetailedDataRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final ManualInvoiceSummaryDataRepository manualInvoiceSummaryDataRepository;
    private final ManualInvoiceDetailedDataRepository manualInvoiceDetailedDataRepository;
    private final InvoiceFileService invoiceFileService;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository;
    private final ManualDebitOrCreditNoteInvoiceDetailedDataRepository manualDebitOrCreditNoteInvoiceDetailedDataRepository;
    private final InvoiceDocumentFileRepository invoiceDocumentFileRepository;
    private final InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository;
    private final FileService fileService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final InvoiceCancellationDocumentRepository invoiceCancellationDocumentRepository;
    private final DocumentsRepository documentsRepository;
    private final EDMSFileArchivationService fileArchivationService;
    private final CompensationRepository compensationRepository;
    private final CompanyDetailRepository companyDetailRepository;
    private final CompensationReceivableService compensationReceivableService;

    /**
     * Fetches an invoice and related data, and maps them to an InvoiceResponse object.
     *
     * @param id The ID of the invoice to preview.
     * @return The InvoiceResponse object containing the invoice and related data.
     */
    public InvoiceResponse preview(Long id) {
        Invoice invoice = fetchInvoice(id);
        if (List.of(InvoiceStatus.DRAFT, InvoiceStatus.DRAFT_GENERATED)
                .contains(invoice.getInvoiceStatus())) {
            if (!permissionService.permissionContextContainsPermissions(
                    PermissionContextEnum.INVOICE,
                    List.of(PermissionEnum.INVOICE_VIEW_DRAFT)
            )) {
                log.error("User has not permission to view draft invoice;");
                throw new AccessDeniedException("You have not access to view draft invoice;");
            }
        }

        return prepareResponse(invoice);
    }

    private InvoiceResponse prepareResponse(Invoice invoice) {
        Long id = invoice.getId();

        InterestRate interestRate = fetchInterestRate(invoice);
        Bank bank = fetchBank(invoice);
        CustomerVersionShortResponse customer = fetchCustomer(invoice.getCustomerDetailId());
        CustomerVersionShortResponse alternativeCustomer = fetchCustomer(invoice.getAlternativeRecipientCustomerDetailId());
        GoodsOrder goodsOrder = fetchGoodsOrder(invoice);
        ServiceOrder serviceOrder = fetchServiceOrder(invoice);
        ContractBillingGroup contractBillingGroup = fetchContractBillingGroup(invoice);
        ProductContract productContract = fetchProductContract(invoice);
        ProductDetails productDetails = fetchProductDetails(invoice.getProductDetailId());
        ServiceContracts serviceContracts = fetchServiceContracts(invoice);
        ServiceDetails serviceDetails = fetchServiceDetails(invoice.getServiceDetailId());
        BillingRun billingRun = fetchBillingRun(invoice);
        InvoiceCommunicationResponse customerCommunications = fetchCustomerCommunications(invoice);
        Currency currency = fetchCurrency(invoice);
        List<InvoiceVatRateResponse> invoiceVatRateResponses = fetchVatRateResponses(invoice.getId())
                .stream()
                .map(vrr ->
                        new InvoiceVatRateResponse(
                                EPBDecimalUtils.convertToCurrencyScale(ObjectUtils.defaultIfNull(
                                        vrr.vatRatePercent(),
                                        BigDecimal.ZERO
                                )),
                                EPBDecimalUtils.convertToCurrencyScale(ObjectUtils.defaultIfNull(
                                        vrr.amountExcludingVat(),
                                        BigDecimal.ZERO
                                )),
                                EPBDecimalUtils.convertToCurrencyScale(ObjectUtils.defaultIfNull(
                                        vrr.valueOfVat(),
                                        BigDecimal.ZERO
                                ))
                        ))
                .toList();
        List<Document> invoiceDocument = fetchInvoiceDocument(invoice.getId());
        List<InvoiceLiabilitiesReceivableResponse> liabilitiesAndReceivables = fetchLiabilitiesAndReceivables(id);
        List<ShortResponse> debitCredits = fetchDebitCredits(id);
        ContractTemplateShortResponse template = fetchTemplate(invoice.getTemplateDetailId());
        boolean detailedData = invoiceRepository.hasThirdTab(id);
        ProxyFileResponse cancelDocumentResponse = invoice
                .getInvoiceStatus()
                .equals(InvoiceStatus.CANCELLED)
                ?
                fetchCancellationDocument(invoice.getId())
                :
                null;
        List<ShortResponse> compensations = fetchCompensations(invoice);
        ShortResponse issuer = fetchIssuer();

        return invoiceMapper.mapFromEntityToResponse(
                invoice,
                interestRate,
                bank,
                customer,
                alternativeCustomer,
                goodsOrder,
                serviceOrder,
                contractBillingGroup,
                productContract,
                serviceContracts,
                invoiceVatRateResponses,
                billingRun,
                currency,
                customerCommunications,
                productDetails,
                serviceDetails,
                invoiceDocument,
                liabilitiesAndReceivables,
                debitCredits,
                template,
                cancelDocumentResponse,
                compensations,
                issuer,
                detailedData
        );
    }

    @Transactional
    public void regenerateCompensations(Long id) {
        Invoice invoice = invoiceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with presented id: [%s] does not exists".formatted(id)));

        if (!Objects.equals(invoice.getInvoiceStatus(), InvoiceStatus.REAL)) {
            log.error("Cannot regenerate compensations for invoice with presented id: {};", id);
            throw new DomainEntityNotFoundException("You can't regenerate compensations for invoice, it must be with status 'REAL'");
        }

        if (invoice.getCompensationIndex() == null) {
            log.error("Cannot regenerate compensations for invoice with presented id: {};", id);
            throw new DomainEntityNotFoundException("Compensations is not accounted for this invoice; invoice id: [%s]".formatted(id));
        }

        try {
            compensationReceivableService.runInvoiceCompensation(invoice, null, false);
        } catch (Exception e) {
            log.error("Cannot regenerate compensations for invoice with presented id: {};", id, e);
            throw new DomainEntityNotFoundException("Cannot regenerate compensations for invoice, error: [%s]".formatted(e.getMessage()));
        }
    }

    private ShortResponse fetchIssuer() {
        return companyDetailRepository
                .findLatestCompanyDetailVersion()
                .map(cd ->
                        new ShortResponse(
                                cd.getId(),
                                cd.getDisplayName()
                        )
                )
                .orElse(null);
    }

    /**
     * Retrieves invoice cancellation document associated with the specified cancelled invoice.
     *
     * @param id The ID of the invoice cancellation.
     * @return File response, or null if not found.
     */
    private ProxyFileResponse fetchCancellationDocument(Long id) {
        return invoiceCancellationDocumentRepository.findByInvoiceCancellationId(id)
                .orElse(null);
    }

    /**
     * Retrieves a list of liability and receivable responses associated with the specified invoice.
     *
     * @param id The ID of the invoice.
     * @return The list of liability and receivable responses.
     */
    private List<InvoiceLiabilitiesReceivableResponse> fetchLiabilitiesAndReceivables(Long id) {
        List<InvoiceLiabilitiesReceivableResponse> liabilitiesAndReceivables = customerLiabilityRepository.findAllByInvoiceId(id);
        List<InvoiceLiabilitiesReceivableResponse> receivables = customerReceivableRepository.findShortResponseByInvoiceId(id);
        liabilitiesAndReceivables.addAll(receivables);
        return liabilitiesAndReceivables;
    }

    /**
     * Retrieves a list of debit and credit notes associated with the specified invoice.
     *
     * @param id The ID of the invoice.
     * @return The list of debit and credit note responses.
     */
    private List<ShortResponse> fetchDebitCredits(Long id) {
        return invoiceRepository.findInvoiceDebitCreditNotes(id);
    }

    /**
     * Fetches a list of compensations associated with the given invoice.
     *
     * @param invoice the invoice for which compensations need to be fetched
     * @return a list of ShortResponse objects representing the compensations for the provided invoice
     */
    private List<ShortResponse> fetchCompensations(Invoice invoice) {
        return compensationRepository
                .findByInvoiceIdAndCompensationIndexAndStatus(invoice.getId(), invoice.getCompensationIndex());
    }

    /**
     * Fetches the contract template associated with the given detail ID.
     *
     * @param id The ID of the contract template detail.
     * @return The contract template response, or null if not found.
     */
    private ContractTemplateShortResponse fetchTemplate(Long id) {
        return contractTemplateRepository.findInvoiceTemplateResponseByDetailId(id)
                .orElse(null);
    }

    /**
     * Fetches the VAT rate responses associated with the specified invoice.
     *
     * @param invoiceId The ID of the invoice.
     * @return The list of VAT rate responses for the invoice.
     */
    private List<InvoiceVatRateResponse> fetchVatRateResponses(Long invoiceId) {
        return invoiceVatRateValueRepository.findByInvoiceId(invoiceId);
    }

    private List<InvoiceDetailedExportModel> fetchVatRateResponsesForExport(Long billingId, InvoiceStatus invoiceStatus) {
        return invoiceVatRateValueRepository.findByBillingIdForExport(billingId, invoiceStatus);
    }

    /**
     * Fetches the currency associated with the given invoice.
     *
     * @param invoice the invoice object for which the currency needs to be fetched
     * @return the Currency object associated with the invoice, or null if the invoice has no currency
     * @throws DomainEntityNotFoundException if the currency with the given id is not found for the invoice
     */
    private Currency fetchCurrency(Invoice invoice) {
        Long invoiceCurrencyId = invoice.getCurrencyId();
        if (Objects.isNull(invoiceCurrencyId)) {
            return null;
        }

        return currencyRepository
                .findById(invoiceCurrencyId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency with id: [%s] not found for this invoice;".formatted(
                        invoiceCurrencyId)));
    }

    /**
     * Retrieves a list of summary data for invoices based on the given ID and request parameters.
     *
     * @param id      The ID of the invoice.
     * @param request The request parameters for listing the summary data.
     * @return The list of invoice summary data.
     */
    public Page<InvoiceSummaryDataResponse> listSummaryData(Long id, InvoiceDataListingRequest request) {
        Invoice invoice = invoiceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with presented id: [%s] does not exists".formatted(id)));

        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(invoice);

        boolean isOrderType = invoiceObjectType.equals(InvoiceObjectType.SERVICE_ORDER) ||
                invoiceObjectType.equals(InvoiceObjectType.GOODS_ORDER);
        switch (invoice.getInvoiceType()) {
            case MANUAL -> {
                switch (invoice.getInvoiceDocumentType()) {
                    case CREDIT_NOTE, DEBIT_NOTE -> {
                        return manualDebitOrCreditNoteInvoiceSummaryDataRepository
                                .findManualDebitOrCreditNoteInvoiceSummaryDataByInvoiceId(
                                        id,
                                        PageRequest.of(
                                                request.page(),
                                                request.size()
                                        )
                                );
                    }
                    default -> {
                        return manualInvoiceSummaryDataRepository
                                .findManualInvoiceSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                    }
                }
            }
            case REVERSAL -> {
                InvoiceTypesDto type = invoiceRepository.findInvoiceTypeById(invoice.getReversalCreatedFromId());
                switch (type.getType()) {
                    case MANUAL -> {
                        switch (type.getDocumentType()) {
                            case CREDIT_NOTE, DEBIT_NOTE -> {
                                return manualDebitOrCreditNoteInvoiceSummaryDataRepository
                                        .findManualDebitOrCreditNoteInvoiceSummaryDataByInvoiceId(
                                                id,
                                                PageRequest.of(
                                                        request.page(),
                                                        request.size()
                                                )
                                        );
                            }
                            default -> {
                                return manualInvoiceSummaryDataRepository
                                        .findManualInvoiceSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                            }
                        }
                    }
                    case STANDARD -> {
                        if (isOrderType) {
                            return invoiceDetailedDataRepository
                                    .findAllSummaryDataByInvoiceId(
                                            id,
                                            String.valueOf(invoiceObjectType),
                                            PageRequest.of(request.page(), request.size())
                                    )
                                    .map(InvoiceSummaryDataResponse::new);
                        } else {
                            return invoiceStandardDetailedDataRepository
                                    .findAllSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                                    .map(InvoiceSummaryDataResponse::new);

                        }
                    }
                    case INTERIM_AND_ADVANCE_PAYMENT -> {
                        if (billingRunRepository.isBillingRunTypeManualInterim(id)) {
                            return invoiceRepository.findManualInterimSummaryDataByInvoiceId(
                                    id,
                                    PageRequest.of(
                                            request.page(),
                                            request.size()
                                    )
                            );
                        } else {
                            return invoiceStandardDetailedDataRepository
                                    .findAllInterimSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                                    .map(InvoiceSummaryDataResponse::new);
                        }
                    }
                    case RECONNECTION -> {
                        return manualDebitOrCreditNoteInvoiceSummaryDataRepository.findManualDebitOrCreditNoteInvoiceSummaryDataByInvoiceId(
                                id,
                                PageRequest.of(request.page(), request.size())
                        );
                    }
                    default -> {
                        return invoiceDetailedDataRepository
                                .findAllSummaryDataByInvoiceId(
                                        id,
                                        String.valueOf(invoiceObjectType),
                                        PageRequest.of(request.page(), request.size())
                                )
                                .map(InvoiceSummaryDataResponse::new);
                    }
                }
            }
            case CORRECTION, STANDARD -> {
                if (isOrderType) {
                    return invoiceDetailedDataRepository
                            .findAllSummaryDataByInvoiceId(
                                    id,
                                    String.valueOf(invoiceObjectType),
                                    PageRequest.of(request.page(), request.size())
                            )
                            .map(InvoiceSummaryDataResponse::new);
                } else {
                    return invoiceStandardDetailedDataRepository
                            .findAllSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                            .map(InvoiceSummaryDataResponse::new);
                }
            }
            case INTERIM_AND_ADVANCE_PAYMENT -> {
                if (billingRunRepository.isBillingRunTypeManualInterim(id)) {
                    return invoiceRepository.findManualInterimSummaryDataByInvoiceId(
                            id,
                            PageRequest.of(request.page(), request.size())
                    );
                } else {
                    return invoiceStandardDetailedDataRepository
                            .findAllInterimSummaryDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                            .map(InvoiceSummaryDataResponse::new);
                }
            }
            case RECONNECTION -> {
                return manualDebitOrCreditNoteInvoiceSummaryDataRepository.findManualDebitOrCreditNoteInvoiceSummaryDataByInvoiceId(
                        id,
                        PageRequest.of(
                                request.page(),
                                request.size()
                        )
                );
            }
            default -> {
                return invoiceDetailedDataRepository
                        .findAllSummaryDataByInvoiceId(
                                id,
                                String.valueOf(invoiceObjectType),
                                PageRequest.of(request.page(), request.size())
                        )
                        .map(InvoiceSummaryDataResponse::new);
            }
        }
    }

    /**
     * Retrieves a list of detailed data for invoices based on the given ID and request parameters.
     *
     * @param id      The ID of the invoice.
     * @param request The request parameters for listing the detailed data.
     * @return The list of invoice detailed data.
     */
    public Page<InvoiceDetailedDataResponse> listDetailedData(Long id, InvoiceDataListingRequest request) {
        Invoice invoice = invoiceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with presented id: [%s] does not exists".formatted(id)));

        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(invoice);

        boolean isOrderType = invoiceObjectType.equals(InvoiceObjectType.SERVICE_ORDER) ||
                invoiceObjectType.equals(InvoiceObjectType.GOODS_ORDER);

        switch (invoice.getInvoiceType()) {
            case MANUAL -> {
                switch (invoice.getInvoiceDocumentType()) {
                    case CREDIT_NOTE, DEBIT_NOTE -> {
                        return manualDebitOrCreditNoteInvoiceDetailedDataRepository
                                .findManualDebitOrCreditNoteInvoiceDetailedDataByInvoiceId(
                                        id,
                                        PageRequest.of(
                                                request.page(),
                                                request.size()
                                        )
                                );
                    }
                    default -> {
                        return manualInvoiceDetailedDataRepository
                                .findManualInvoiceDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                    }
                }
            }
            case REVERSAL -> {
                InvoiceTypesDto type = invoiceRepository.findInvoiceTypeById(invoice.getReversalCreatedFromId());
                switch (type.getType()) {
                    case MANUAL -> {
                        switch (type.getDocumentType()) {
                            case CREDIT_NOTE, DEBIT_NOTE -> {
                                return manualDebitOrCreditNoteInvoiceDetailedDataRepository
                                        .findManualDebitOrCreditNoteInvoiceDetailedDataByInvoiceId(
                                                id,
                                                PageRequest.of(
                                                        request.page(),
                                                        request.size()
                                                )
                                        );
                            }
                            default -> {
                                return manualInvoiceDetailedDataRepository
                                        .findManualInvoiceDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                            }
                        }
                    }
                    case STANDARD -> {
                        if (isOrderType) {
                            return invoiceDetailedDataRepository
                                    .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                        }
                        return invoiceStandardDetailedDataRepository
                                .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                                .map(InvoiceDetailedDataResponse::new);
                    }
                    default -> {
                        return invoiceDetailedDataRepository
                                .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                    }
                }
            }
            case CORRECTION, STANDARD -> {
                if (isOrderType) {
                    return invoiceDetailedDataRepository
                            .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
                }
                return invoiceStandardDetailedDataRepository
                        .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()))
                        .map(InvoiceDetailedDataResponse::new);
            }
            default -> {
                return invoiceDetailedDataRepository
                        .findAllDetailedDataByInvoiceId(id, PageRequest.of(request.page(), request.size()));
            }
        }
    }

    /**
     * Fetches the customer communications associated with the given invoice.
     *
     * @param invoice The invoice for which to fetch the customer communications.
     * @return The customer communications associated with the invoice, or null if no communications are found.
     * @throws DomainEntityNotFoundException if no customer communications are found for the given invoice.
     */
    private InvoiceCommunicationResponse fetchCustomerCommunications(Invoice invoice) {
        Long customerCommunicationId = invoice.getCustomerCommunicationId();
        Long contractCommunicationId = invoice.getContractCommunicationId();
        if (customerCommunicationId == null && contractCommunicationId == null) {
            return null;
        }
        Long billingCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
        Long id = ObjectUtils.firstNonNull(customerCommunicationId, contractCommunicationId);
        InvoiceCommunicationResponse communicationResponse = customerCommunicationsRepository
                .findByIdForInvoice(id, billingCommunicationId)
                .orElse(null);
        if (Objects.nonNull(communicationResponse)) {
            communicationResponse.setFromBillingGroup(id.equals(customerCommunicationId));
        }
        return communicationResponse;
    }

    /**
     * Fetches the BillingRun associated with an Invoice.
     *
     * @param invoice The Invoice object.
     * @return The BillingRun object associated with the Invoice, or null if the billingId is null.
     * @throws DomainEntityNotFoundException if the BillingRun with the specified billingId is not found.
     */
    private BillingRun fetchBillingRun(Invoice invoice) {
        Long billingId = invoice.getBillingId();
        if (billingId == null) {
            return null;
        }

        return billingRunRepository
                .findById(billingId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing with id: [%s] assigned to Invoice not found;".formatted(
                        billingId)));
    }

    /**
     * Retrieves a paginated list of invoice listings based on the provided request parameters.
     *
     * @param request the request object containing filtering and pagination parameters
     * @return a page object containing the invoice listing responses
     */
    public Page<InvoiceListingResponse> listing(InvoiceListingRequest request) {
        return invoiceRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                        CollectionUtils.isEmpty(request.documentTypes()) ? null : request.documentTypes(),
                        request.accountingPeriodId(),
                        EPBStringUtils.fromPromptToQueryParameter(request.billingRun()),
                        request.dateOfInvoiceFrom(),
                        request.dateOfInvoiceTo(),
                        request.totalAmountFrom(),
                        request.totalAmountTo(),
                        request.meterReadingPeriodFrom(),
                        request.meterReadingPeriodTo(),
                        CollectionUtils.isEmpty(request.invoiceStatuses()) ? null :
                                request.invoiceStatuses()
                                        .stream()
                                        .map(InvoiceListingRequest.InvoiceListingStatus::mapToInvoiceStatus)
                                        .toList(),
                        extractSearchBy(request.searchBy()),
                        PageRequest.of(
                                request.page(),
                                request.size(),
                                extractSorting(request)
                        )
                );
    }

    /**
     * Returns the search-by value from the given InvoiceListingSearchBy object.
     *
     * @param searchBy the InvoiceListingSearchBy object representing the search-by value.
     * @return the search-by value as a string.
     */
    private String extractSearchBy(InvoiceListingRequest.InvoiceListingSearchBy searchBy) {
        return Objects.requireNonNullElse(searchBy, InvoiceListingRequest.InvoiceListingSearchBy.ALL).name();
    }

    /**
     * Extracts the sorting criteria from the given InvoiceListingRequest object and returns a Sort object.
     *
     * @param request the InvoiceListingRequest object containing the sorting criteria
     * @return a Sort object representing the sorting criteria extracted from the request
     */
    private Sort extractSorting(InvoiceListingRequest request) {
        List<Sort.Order> orders = new ArrayList<>();

        Sort.Direction defaultDirection = Objects.requireNonNullElse(request.sortDirection(), Sort.Direction.ASC);

        switch (defaultDirection) {
            case ASC -> orders.add(
                    Sort.Order.asc(
                            Objects.requireNonNullElse(
                                            request.sortBy(),
                                            InvoiceListingRequest.InvoiceListingSortBy.INVOICE_NUMBER
                                    )
                                    .getColumnName()
                    )
            );
            case DESC -> orders.add(
                    Sort.Order.desc(
                            Objects.requireNonNullElse(
                                            request.sortBy(),
                                            InvoiceListingRequest.InvoiceListingSortBy.INVOICE_NUMBER
                                    )
                                    .getColumnName()
                    )
            );
        }

        orders.add(
                Sort.Order.desc(
                        InvoiceListingRequest.InvoiceListingSortBy.ID.getColumnName()
                )
        );

        return Sort.by(orders);
    }

    /**
     * Fetches the service contract associated with the given invoice.
     *
     * @param invoice the invoice object to fetch the service contract for
     * @return the service contract object associated with the invoice, or null if no service contract is found
     * @throws DomainEntityNotFoundException if the service contract with the specified id is not found
     */
    private ServiceContracts fetchServiceContracts(Invoice invoice) {
        Long serviceContractId = invoice.getServiceContractId();
        if (serviceContractId == null) {
            return null;
        }

        return serviceContractsRepository
                .findByIdAndStatusIn(serviceContractId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Service Contract with id: [%s] assigned to Invoice not found;".formatted(serviceContractId))
                );
    }

    /**
     * Fetches the product contract assigned to the given invoice.
     *
     * @param invoice the invoice object representing the invoice
     * @return the product contract assigned to the invoice, or null if no product contract is assigned
     * @throws DomainEntityNotFoundException if the product contract with the specified id is not found
     */
    private ProductContract fetchProductContract(Invoice invoice) {
        Long productContractId = invoice.getProductContractId();
        if (productContractId == null) {
            return null;
        }

        return productContractRepository
                .findByIdAndStatusIn(productContractId, List.of(ProductContractStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Product Contract with id: [%s] assigned to Invoice not found;".formatted(productContractId))
                );
    }

    /**
     * Retrieves the ContractBillingGroup associated with the given Invoice.
     *
     * @param invoice the Invoice object to retrieve the ContractBillingGroup from
     * @return the retrieved ContractBillingGroup or null if the invoice does not have a ContractBillingGroup assigned
     * @throws DomainEntityNotFoundException if the ContractBillingGroup with the specified id is not found
     */
    private ContractBillingGroup fetchContractBillingGroup(Invoice invoice) {
        Long contractBillingGroupId = invoice.getContractBillingGroupId();
        if (contractBillingGroupId == null) {
            return null;
        }

        return contractBillingGroupRepository
                .findByIdAndStatusIn(contractBillingGroupId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Contract Billing Group with id: [%s] assigned to Invoice not found;".formatted(contractBillingGroupId))
                );
    }

    /**
     * Fetches the Service Order assigned to the given Invoice.
     *
     * @param invoice the invoice for which the Service Order needs to be fetched
     * @return the Service Order assigned to the invoice, or null if no Service Order is assigned
     * @throws DomainEntityNotFoundException if the Service Order with the given id is not found
     */
    private ServiceOrder fetchServiceOrder(Invoice invoice) {
        Long serviceOrderId = invoice.getServiceOrderId();
        if (serviceOrderId == null) {
            return null;
        }

        return serviceOrderRepository
                .findByIdAndStatusIn(serviceOrderId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Service Order with id: [%s] assigned to Invoice not found;".formatted(serviceOrderId))
                );
    }

    /**
     * Fetches the GoodsOrder associated with the given Invoice.
     *
     * @param invoice the Invoice object to fetch the GoodsOrder for
     * @return the GoodsOrder associated with the Invoice, or null if no GoodsOrder is found
     * @throws DomainEntityNotFoundException if the GoodsOrder with the specified ID is not found or is not in ACTIVE status
     */
    private GoodsOrder fetchGoodsOrder(Invoice invoice) {
        Long goodsOrderId = invoice.getGoodsOrderId();
        if (goodsOrderId == null) {
            return null;
        }

        return goodsOrderRepository
                .findByIdAndStatusIn(goodsOrderId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Goods Order with id: [%s] assigned to Invoice not found;".formatted(
                        goodsOrderId)));
    }

    /**
     * Fetches a customer from the repository using the provided customerDetailId.
     *
     * @param customerDetailId The id of the customer detail.
     * @return The fetched customer detail as a ShortResponse object.
     * Returns null if the customerDetailId is null.
     * @throws DomainEntityNotFoundException If the customer detail with the provided id is not found.
     */
    private CustomerVersionShortResponse fetchCustomer(Long customerDetailId) {
        if (customerDetailId == null) {
            return null;
        }

        return customerDetailsRepository
                .findByCustomerDetailsId(customerDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Detail with id: [%s] assigned to Invoice not found;".formatted(
                        customerDetailId)));
    }

    /**
     * Fetches the Bank associated with the given Invoice.
     *
     * @param invoice the Invoice object to retrieve the Bank for
     * @return the Bank object associated with the Invoice, or null if the invoice does not have a bankId
     * @throws DomainEntityNotFoundException if the Bank with the specified id is not found or is not active or inactive
     */
    private Bank fetchBank(Invoice invoice) {
        Long bankId = invoice.getBankId();
        if (bankId == null) {
            return null;
        }

        return bankRepository
                .findByIdAndStatus(bankId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Bank with id: [%s] assigned to Invoice not found;".formatted(
                        bankId)));
    }

    /**
     * Fetches the interest rate for the given invoice.
     *
     * @param invoice the invoice for which the interest rate needs to be fetched
     * @return the interest rate assigned to the invoice, or null if no interest rate is found
     * @throws DomainEntityNotFoundException if the interest rate with the specified id is not found
     */
    private InterestRate fetchInterestRate(Invoice invoice) {
        Long interestRateId = invoice.getInterestRateId();
        if (interestRateId == null) {
            return null;
        }

        return interestRateRepository
                .findByIdAndStatusIn(interestRateId, List.of(InterestRateStatus.ACTIVE, InterestRateStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Interest rate with id: [%s] assigned to Invoice not found;".formatted(
                        interestRateId)));
    }

    /**
     * Fetches an invoice by its ID.
     *
     * @param id the ID of the invoice to fetch
     * @return the fetched invoice
     * @throws DomainEntityNotFoundException if the invoice with the specified ID is not found
     */
    private Invoice fetchInvoice(Long id) {
        return invoiceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with presented id: [%s] not found;".formatted(id)));
    }

    /**
     * Fetches the product details based on the given product detail ID.
     *
     * @param productDetailId The ID of the product detail to fetch.
     * @return The product details associated with the given ID.
     * @throws DomainEntityNotFoundException If the product detail with the given ID is not found.
     */
    private ProductDetails fetchProductDetails(Long productDetailId) {
        if (productDetailId == null) {
            return null;
        }

        return productDetailsRepository
                .findById(productDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Product Detail with id: [%s] not found;".formatted(
                        productDetailId)));
    }

    /**
     * Fetch service details by id.
     *
     * @param serviceDetailId the service detail id
     * @return the service details
     */
    private ServiceDetails fetchServiceDetails(Long serviceDetailId) {
        if (serviceDetailId == null) {
            return null;
        }

        return serviceDetailsRepository
                .findById(serviceDetailId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service Detail with id: [%s] not found;"));
    }

    private List<Document> fetchInvoiceDocument(Long invoiceId) {
        return invoiceDocumentFileRepository.findAllByInvoiceId(invoiceId);
    }

    /**
     * Finds invoices by the given criteria of invoice ID, customer detail ID, and contract billing group ID.
     *
     * @param invoiceForPaymentListingRequest The request object containing the criteria for finding invoices.                                        This object should include the following properties:                                        - customerId: The ID of the customer.                                        - contractBillingGroupId: The ID of the contract billing group.                                        - page: The page number for pagination.                                        - size: The number of invoices to return per page.
     * @return A page of invoice short responses that match the given criteria.
     */
    public Page<InvoiceShortResponse> findByIdAndCustomerDetailIdAndContractBillingGroupId(InvoiceForPaymentListingRequest invoiceForPaymentListingRequest) {
        return invoiceRepository.findByIdAndCustomerDetailIdAndContractBillingGroupId(
                invoiceForPaymentListingRequest.getCustomerId(),
                invoiceForPaymentListingRequest.getContractBillingGroupId(),
                invoiceForPaymentListingRequest.getPrompt(),
                PageRequest.of(
                        invoiceForPaymentListingRequest.getPage(),
                        invoiceForPaymentListingRequest.getSize()
                )
        );
    }

    /**
     * Export files http entity.
     *
     * @param billingRunId the billing run id
     * @return the http entity
     */
    public HttpEntity<ByteArrayResource> exportFiles(Long billingRunId) {
        boolean exists = billingRunRepository.existsById(billingRunId);
        if (!exists) {
            throw new DomainEntityNotFoundException("Billing run with id: %s not found!;".formatted(billingRunId));
        }
        Pair<String, ByteArrayResource> pair = invoiceFileService.fetchInvoice(billingRunId);
        ByteArrayResource byteArrayResource = pair.getSecond();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(pair.getFirst()))
        );
        return new HttpEntity<>(byteArrayResource, header);
    }

    /**
     * Method generates invoice export files for billing run,
     * should be called after billing run invoice calculation and when Draft invoices change status to Real.
     *
     * @param billingRunId  For which file should be generated.
     * @param invoiceStatus Invoice status based on billing status.
     */
    public void generateExcel(Long billingRunId, InvoiceStatus invoiceStatus) {
        Map<Long, InvoiceResponseExport> invoiceExportData = invoiceRepository
                .findAllByBillingId(billingRunId, invoiceStatus)
                .stream()
                .collect(Collectors.toMap(InvoiceResponseExport::getInvoiceId, j -> j));
        List<InvoiceDetailedExportModel> invoiceDetailedExportModels = fetchVatRateResponsesForExport(billingRunId, invoiceStatus);
        Map<Long, List<InvoiceLiabilitiesAndReceivablesExportModel>> groupedLiabilitiesAndReceivables = fetchLiabilitiesAndReceivablesForExport(
                billingRunId);

        Map<Long, List<InvoiceDetailedExportModel>> groupedVatRates = invoiceDetailedExportModels
                .stream()
                .collect(Collectors.groupingBy(InvoiceDetailedExportModel::getInvoiceId));

        int maxSize = groupedVatRates.keySet()
                .stream()
                .map(x -> groupedVatRates
                        .get(x)
                        .size()
                )
                .max(Integer::compare)
                .orElse(1);

        invoiceExportData.forEach((key, value) -> {
            List<InvoiceDetailedExportModel> vatRate = groupedVatRates.get(key);
            if (vatRate == null) {
                vatRate = new ArrayList<>();
            }
            for (int i = 0; i < maxSize - vatRate.size(); i++) {
                vatRate.add(new InvoiceDetailedExportModel());
            }
            value.setVatRateResponses(vatRate);

            if (groupedLiabilitiesAndReceivables.containsKey(key)) {
                List<InvoiceLiabilitiesAndReceivablesExportModel> model = groupedLiabilitiesAndReceivables.get(key);
                if (CollectionUtils.isNotEmpty(model)) {
                    String receivablesAndLiabilities = model.stream()
                            .map(InvoiceLiabilitiesAndReceivablesExportModel::getNumber)
                            .collect(Collectors.joining(";"));
                    value.setLiabilityCustomerReceivable(receivablesAndLiabilities);
                }
            }
        });

        invoiceFileService.saveInvoiceCsv(billingRunId, invoiceExportData.values(), maxSize);
    }

    /**
     * Fetch liabilities and receivables for export map.
     *
     * @param billingRunId the billing run id
     * @return the map
     */
    private Map<Long, List<InvoiceLiabilitiesAndReceivablesExportModel>> fetchLiabilitiesAndReceivablesForExport(Long billingRunId) {
        return invoiceRepository
                .findCustomerLiabilitiesAndReceivablesForInvoice(billingRunId)
                .stream()
                .collect(Collectors.groupingBy(InvoiceLiabilitiesAndReceivablesExportModel::getInvoiceId));
    }

    /**
     * Download invoice template http entity.
     *
     * @param invoiceDocumentId the invoice template id
     * @return the http entity
     */
    public HttpEntity<ByteArrayResource> downloadInvoiceDocument(Long invoiceDocumentId) {
        Document invoiceDocument = documentsRepository
                .findById(invoiceDocumentId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice Document with id: [%s] not found;".formatted(
                        invoiceDocumentId)));

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(invoiceDocument.getName()))
        );

        if (Boolean.TRUE.equals(invoiceDocument.getIsArchived())) {
            if (Objects.nonNull(invoiceDocument.getSignedFileUrl())) {
                return new HttpEntity<>(fileService.downloadFile(invoiceDocument.getSignedFileUrl()), header);
            } else {
                return new HttpEntity<>(
                        fileArchivationService.downloadArchivedFile(
                                invoiceDocument.getDocumentId(),
                                invoiceDocument.getFileId()
                        ), header
                );
            }
        } else {
            return new HttpEntity<>(fileService.downloadFile(invoiceDocument.getSignedFileUrl()), header);
        }
    }

    /**
     * Download invoice cancellation document.
     *
     * @param id the cancellation document id
     * @return the http entity
     */
    public HttpEntity<ByteArrayResource> downloadCancellationDocument(Long id) {
        Document cancellationDocument = documentsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice Cancellation Document with id: [%s] not found;".formatted(
                        id)));
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(cancellationDocument.getName()))
        );

        if (Boolean.TRUE.equals(cancellationDocument.getIsArchived())) {
            if (Objects.nonNull(cancellationDocument.getSignedFileUrl())) {
                return new HttpEntity<>(fileService.downloadFile(cancellationDocument.getSignedFileUrl()), header);
            } else {
                return new HttpEntity<>(
                        fileArchivationService.downloadArchivedFile(
                                cancellationDocument.getDocumentId(),
                                cancellationDocument.getFileId()
                        ), header
                );
            }
        } else {
            return new HttpEntity<>(fileService.downloadFile(cancellationDocument.getSignedFileUrl()), header);
        }
    }
}
