package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXValue;
import bg.energo.phoenix.billingRun.service.BillingRunCurrencyService;
import bg.energo.phoenix.billingRun.service.BillingRunPriceComponentEvaluationService;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.billing.invoice.CreateInvoiceCancellationRequest;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderForInvoiceResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentForServiceOrderResponseImpl;
import bg.energo.phoenix.model.response.priceParameter.ServiceOrderProcessPriceParamResponse;
import bg.energo.phoenix.model.response.terms.ServiceOrderPaymentTermResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDetailedDataRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.invoice.cancellation.InvoiceCancellationService;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.billing.runs.services.evaluatePriceComponentCondition.EvaluateServiceOrderConditionService;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.util.order.OrderInvoiceService;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.OgnlException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICE_ORDERS;
import static bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy.AbstractPriceComponentPriceEvaluationStrategy.definePriceComponentFormulaComplexity;

@Slf4j
@RequiredArgsConstructor
@Service
public class ServiceOrderProcessService {
    private final ServiceOrderRepository serviceOrderRepository;
    private final CurrencyRepository currencyRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailedDataRepository invoiceDetailedDataRepository;
    private final EvaluateServiceOrderConditionService evaluateServiceOrderConditionService;
    private final ExpressionStringParser expressionStringParser;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final PermissionService permissionService;
    private final CustomerLiabilityService customerLiabilityService;
    private final OrderInvoiceService orderInvoiceService;
    private final TransactionTemplate transactionTemplate;
    private final InvoiceNumberService invoiceNumberService;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final BillingRunPriceComponentEvaluationService priceComponentEvaluationService;
    private final InvoiceCancellationService invoiceCancellationService;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;


    /**
     * Generates and issues a pro forma invoice for the specified service order.
     *
     * @param id The ID of the service order for which to generate the pro forma invoice.
     * @throws ClientException               if the user does not have the necessary permission to create a pro forma invoice.
     * @throws DomainEntityNotFoundException if the specified service order is not found or does not have the appropriate status to generate a pro forma invoice.
     */
    @Transactional
    public void issueProformaInvoice(Long id) {
        if (!permissionService.getPermissionsFromContext(SERVICE_ORDERS).contains(PermissionEnum.SERVICE_ORDER_CREATE_PRO_FORMA_INVOICE.getId())) {
            throw new ClientException("cannot issue pro forma invoice without permission", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse = serviceOrderRepository.findByOrderIdAndOrderStatusAndNonExistentInvoice(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Couldn't find appropriate service order to issue pro forma invoice;"));

        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective main currency not found for this time;"));

        List<Invoice> invoices = retrievePriceComponentsAndGenerateInvoice(id, mainCurrency, serviceOrderForInvoiceResponse);
        ServiceOrder serviceOrder = serviceOrderRepository.findById(id).get();
        serviceOrder.setOrderInvoiceStatus(!invoices.isEmpty() ? OrderInvoiceStatus.DRAFT_PROFORMA_GENERATED : OrderInvoiceStatus.NOT_GENERATED);
        invoiceNumberService.fillInvoiceNumber(invoices);
    }


    /**
     * Deletes the pro forma invoices associated with the specified service order.
     *
     * @param id The ID of the service order for which to delete the pro forma invoices.
     * @throws DomainEntityNotFoundException If the service order is not found or if no invoices are found to delete.
     */
    @Transactional
    public void deleteProformaInvoice(Long id) {
        ServiceOrder serviceOrder = serviceOrderRepository.findByIdAndOrderStatus(id, List.of(ServiceOrderStatus.CONFIRMED, ServiceOrderStatus.AWAITING_PAYMENT))
                .orElseThrow(() -> new DomainEntityNotFoundException("service order not found to delete invoices for"));
        List<Invoice> invoicesToDelete = invoiceRepository.findServiceOrderInvoicesWithStatus(id, serviceOrder.getOrderStatus().name(), true);
        if (invoicesToDelete.isEmpty()) {
            throw new DomainEntityNotFoundException("service order invoices not found to delete;");
        }
        if (serviceOrderRepository.anyInvoicePaid(id)) {
            throw new ClientException("Unable to delete invoices because there are invoices that have already been paid.", ErrorCode.UNSUPPORTED_OPERATION);
        }
        serviceOrder.setOrderStatus(ServiceOrderStatus.CONFIRMED);

        orderInvoiceService.extractInvoicesAndDeleteRelatedEntities(invoicesToDelete);
        serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED);
    }

    /**
     * Starts the accounting process for the specified service order.
     * <p>
     * This method finds the service order with the specified ID and the 'CONFIRMED' status, and then retrieves all invoices associated with that service order. If no valid invoices are found, a {@link DomainEntityNotFoundException} is thrown.
     * <p>
     * For each invoice, the method sets the invoice status to 'REAL' and saves the invoice. It then creates a customer liability for the invoice using the
     * <p>
     * Finally, the method updates the service order status to 'AWAITING_PAYMENT' and the order invoice status to 'REAL_PROFORMA_GENERATED'. It also publishes an invoice event using the
     *
     * @param id The ID of the service order for which to start the accounting process.
     * @throws DomainEntityNotFoundException If the service order is not found or if no valid invoices are found.
     */
    @Transactional
    public void startAccounting(Long id) {
        ServiceOrder serviceOrder = serviceOrderRepository.findByIdAndOrderStatus(id, List.of(ServiceOrderStatus.CONFIRMED))
                .orElseThrow(() -> new DomainEntityNotFoundException("service order not found to start accounting for;"));
        List<Invoice> invoices = invoiceRepository.findServiceOrderInvoicesWithStatus(id, serviceOrder.getOrderStatus().name(), false);
        if (invoices.isEmpty()) {
            throw new DomainEntityNotFoundException("valid invoice not found to start accounting for;");
        }

        invoices.forEach(i -> {
            i.setInvoiceStatus(InvoiceStatus.REAL);
            invoiceNumberService.fillInvoiceNumber(i);
            invoiceRepository.saveAndFlush(i);
            customerLiabilityService.createLiabilityFromInvoice(i.getId(), LiabilityOrReceivableCreationSource.SERVICE_ORDER);
        });
        serviceOrder.setOrderStatus(ServiceOrderStatus.AWAITING_PAYMENT);
        serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_PROFORMA_GENERATED);
        for (Invoice invoice : invoices) {
            invoiceRepository.findLatestInvoiceDocument(invoice.getId()).ifPresent(document -> orderInvoiceService.sendMailForServiceOrder(invoice, serviceOrder, document));
        }

    }

    /**
     * Starts the generation process for draft proforma invoices associated with the specified service order.
     * <p>
     * This method first finds the service order with the specified ID and the 'CONFIRMED' status. If the service order is not found, it throws a {@link DomainEntityNotFoundException}.
     * <p>
     * It then retrieves all draft proforma invoices associated with the service order. If no valid draft invoices are found, it throws a {@link DomainEntityNotFoundException}.
     * <p>
     * For each draft invoice, the method generates the document for the invoice and sets the invoice status to 'DRAFT_GENERATED'.
     * <p>
     * Finally, the method updates the service order's invoice status to 'PDF_GENERATED'.
     *
     * @param id The ID of the service order for which to start the generation process.
     * @throws DomainEntityNotFoundException If the service order or valid draft invoices are not found.
     */

    public void startGenerating(Long id) {
        MDC.put("ServiceOrderId", String.valueOf(id));
        ServiceOrder serviceOrder = serviceOrderRepository.findByIdAndOrderStatus(id, List.of(ServiceOrderStatus.CONFIRMED))
                .orElseThrow(() -> new DomainEntityNotFoundException("valid service order with 'confirmed' status not found to start generating;"));
        List<Pair<Invoice, Document>> result = transactionTemplate.execute((x) -> {

            List<Invoice> invoices = invoiceRepository.findServiceOrderInvoicesForGeneration(id);
            if (invoices.isEmpty()) {
                throw new DomainEntityNotFoundException("valid draft proforma invoice not found to start generating for;");
            }
            List<Pair<Invoice, Document>> res = new ArrayList<>();
            invoices.forEach(i -> {
                i.setInvoiceStatus(InvoiceStatus.DRAFT_GENERATED);
                invoiceNumberService.fillInvoiceNumber(i);
                res.add(Pair.of(i, orderInvoiceService.generateDocumentForInvoice(i, false)));
            });
            serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.PDF_GENERATED);
            return res;
        });
        log.debug("ResultReturnedSO {}", result);
    }

    /**
     * Issues an invoice for the specified service order.
     * <p>
     * This method first checks if the user has the necessary permission to create an invoice. If not, it throws a {@link ClientException}.
     * <p>
     * It then finds the service order with the specified ID and the 'AWAITING_PAYMENT' status. If the service order is not found, it throws a {@link DomainEntityNotFoundException}.
     * <p>
     * Next, it retrieves all invoices associated with the service order that have the 'AWAITING_PAYMENT' status. If no valid invoices are found, it throws a {@link DomainEntityNotFoundException}.
     * <p>
     * The method then calculates the payment deadline for the invoices based on the service order's payment terms.
     * <p>
     * Finally, it updates the invoices with the necessary information (invoice document type, invoice date, create date, payment deadline), updates the service order's invoice status to 'REAL_INVOICE_GENERATED', and publishes an invoice event.
     *
     * @param id The ID of the service order for which to issue the invoice.
     * @throws ClientException               If the user does not have the necessary permission to create an invoice.
     * @throws DomainEntityNotFoundException If the service order or valid invoices are not found.
     */
    public void issueInvoice(Long id) {
        if (!permissionService.getPermissionsFromContext(SERVICE_ORDERS).contains(PermissionEnum.SERVICE_ORDER_CREATE_INVOICE.getId())) {
            throw new ClientException("cannot issue real invoice without permission", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        ServiceOrder serviceOrder = serviceOrderRepository.findByIdAndOrderStatus(id, List.of(ServiceOrderStatus.AWAITING_PAYMENT))
                .orElseThrow(() -> new DomainEntityNotFoundException("valid service order not found to issue invoice"));


        List<Pair<Invoice, Document>> result = transactionTemplate.execute((x) -> {
            List<Invoice> invoices = invoiceRepository.findServiceOrderInvoicesWithStatus(id, ServiceOrderStatus.AWAITING_PAYMENT.name(), false);
            if (invoices.isEmpty()) {
                throw new DomainEntityNotFoundException("valid invoice not found to issue invoice;");
            }
            ServiceOrderPaymentTermResponse invoicePaymentTerms = invoicePaymentTermsRepository.findInvoiceTermForServiceOrder(id)
                    .orElseThrow(() -> new ClientException("cannot calculate payment deadline due to missing payment term", ErrorCode.CONFLICT));

            LocalDate paymentDeadline = calculatePaymentDeadline(serviceOrder.getInvoicePaymentTermValue(), invoicePaymentTerms, LocalDate.now());
            List<Pair<Invoice, Document>> res = new ArrayList<>();
            invoices.forEach(invoice -> {
                invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
                invoice.setInvoiceDate(LocalDate.now());
                invoice.setTaxEventDate(LocalDate.now());
                invoice.setCreateDate(LocalDateTime.now());
                invoice.setPaymentDeadline(paymentDeadline);
                Invoice saved = invoiceRepository.saveAndFlush(invoice);
                invoice.setCurrentStatusChangeDate(LocalDateTime.now());
                invoiceNumberService.fillInvoiceNumber(invoice);
                res.add(Pair.of(invoice, orderInvoiceService.generateDocumentForInvoice(saved, false)));
                customerLiabilityRepository.updateAllIncomeAccountNumberByLiabilityIds(invoice.getId(), incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name()));
            });
            serviceOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_INVOICE_GENERATED);
            return res;
        });

        for (Pair<Invoice, Document> invoiceDocumentPair : result) {
            orderInvoiceService.sendMailForServiceOrder(invoiceDocumentPair.getFirst(), serviceOrder, invoiceDocumentPair.getSecond());
        }
    }

    /**
     * Cancels all invoices associated with the specified service order.
     * <p>
     * This method first checks if the service order has a valid invoice status (either REAL_INVOICE_GENERATED or REAL_PROFORMA_GENERATED). If no valid service order is found, it throws a {@link DomainEntityNotFoundException}.
     * <p>
     * Next, it retrieves the invoice numbers for the invoices that need to be canceled, and then creates an invoice cancellation request using the {@link invoiceCancellationService}.
     *
     * @param id         The ID of the service order for which to cancel the invoices.
     * @param templateId The ID of the invoice cancellation template to use.
     * @throws DomainEntityNotFoundException If no valid service order is found.
     */
    public void cancelInvoices(Long id, Long templateId) {
        String invoiceNumbers = invoiceRepository.getInvoiceNumbersToCancelByServiceOrderId(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("valid invoices not found to start cancellation process for"));

        invoiceCancellationService.createInvoiceCancellation(CreateInvoiceCancellationRequest.builder()
                .invoices(invoiceNumbers)
                .taxEventDate(LocalDate.now())
                .templateId(templateId)
                .build());
    }

    /**
     * Checks for and handles any overdue proforma invoices associated with service orders.
     * <p>
     * This method retrieves all service orders that have overdue proforma invoices. For each of these service orders, it sets the order status to 'DELETED' and then deletes all related invoices.
     */
    @ExecutionTimeLogger
    @Transactional
    public void checkOverdueProformaInvoicesForOrders() {
        List<ServiceOrder> orders = serviceOrderRepository.findOrdersWithOverdueInvoices();
        if (!org.springframework.util.CollectionUtils.isEmpty(orders)) {
            for (ServiceOrder order : orders) {
                order.setStatus(EntityStatus.DELETED);
            }
            List<Invoice> invoices = invoiceRepository.findAllByServiceOrderIdIn(orders.stream().map(ServiceOrder::getId).toList());
            orderInvoiceService.extractInvoicesAndDeleteRelatedEntities(invoices);
        }
    }


    /**
     * Retrieves the valid price components for a service order, checks if the price parameters are filled, and generates invoices based on the price components.
     *
     * @param id                             The ID of the service order.
     * @param mainCurrency                   The main currency for the invoices.
     * @param serviceOrderForInvoiceResponse The service order response object.
     * @return A list of generated invoices.
     * @throws ClientException               If there are no valid price components or if the price parameters are not filled.
     * @throws DomainEntityNotFoundException If the service order or valid invoices are not found.
     */
    private List<Invoice> retrievePriceComponentsAndGenerateInvoice(Long id, Currency mainCurrency, ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse) {
        List<PriceComponentForServiceOrderResponseImpl> validPriceComponents = priceComponentRepository
                .getValidPriceComponentsByServiceOrderId(id)
                .stream().map(PriceComponentForServiceOrderResponseImpl::new)
                .toList();
        if (CollectionUtils.isEmpty(validPriceComponents)) {
            throw new ClientException("Could not generate proforma invoice due to non-valid price components", ErrorCode.CONFLICT);
        }
        //retrieve price parameters from price components and check if data is fully filled
        Map<Long, List<Long>> priceParameters = validPriceComponents.stream()
                .filter(pc -> definePriceComponentFormulaComplexity(pc.getPriceFormula())
                        .equals(PriceComponentFormulaComplexity.WITH_PRICE_PARAMETER))
                .collect(Collectors.toMap(PriceComponentForServiceOrderResponseImpl::getId, pc -> extractPriceParametersFromFormula(pc.getPriceFormula())));

        Set<Long> allPriceParamsUsed = priceParameters.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

        List<ServiceOrderProcessPriceParamResponse> priceParamDetails = priceParameterDetailInfoRepository.findByParameterIdsAndTypeAndExactDate(allPriceParamsUsed);

        if (priceParamDetails.size() != allPriceParamsUsed.size() ||
            priceParamDetails.stream()
                    .anyMatch(p -> List.of(PeriodType.ONE_DAY, PeriodType.ONE_MONTH).contains(p.getPeriodType())
                                   && Objects.isNull(p.getPrice()))
        ) {
            throw new ClientException("Price wasn't calculated because of missing data", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Map<Long, List<ServiceOrderProcessPriceParamResponse>> priceCompPriceParameters = new HashMap<>();
        for (ServiceOrderProcessPriceParamResponse pp : priceParamDetails) {
            for (Map.Entry<Long, List<Long>> entry : priceParameters.entrySet()) {
                Long priceComponentId = entry.getKey();
                List<Long> ppIds = entry.getValue();
                if (ppIds.contains(pp.getId())) {
                    priceCompPriceParameters.computeIfAbsent(priceComponentId, k -> new ArrayList<>())
                            .add(pp);
                }
            }
        }


        applyQuantityAndPodsAccordingToConditionsAndApplicationLevel(serviceOrderForInvoiceResponse, validPriceComponents);


        validPriceComponents = validPriceComponents.stream().filter(pc -> pc.getPodQuantityToMultiplyPrice() > 0).toList();

        Map<IssuedSeparateInvoice, List<PriceComponentForServiceOrderResponseImpl>> priceComponentsMap = validPriceComponents.stream()
                .collect(Collectors.groupingBy(PriceComponentForServiceOrderResponseImpl::getIssuedSeparateInvoice));
        List<Invoice> invoicesList = new ArrayList<>();
        generateInvoices(serviceOrderForInvoiceResponse, mainCurrency, priceComponentsMap, priceCompPriceParameters, invoicesList);
        return invoicesList;
    }

    private void applyQuantityAndPodsAccordingToConditionsAndApplicationLevel(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, List<PriceComponentForServiceOrderResponseImpl> validPriceComponents) {
        validPriceComponents.forEach(
                pc ->
                {
                    List<Long> podIds = getRecognizedPods(serviceOrderForInvoiceResponse.getRecognizedPodIds());

                    List<String> unrecognizedPodIdentifiers = getUnrecognizedPods(serviceOrderForInvoiceResponse.getUnrecognizedPods());

                    if (noConditionsUsed(serviceOrderForInvoiceResponse, pc, podIds, unrecognizedPodIdentifiers))
                        return;

                    String condition = evaluateServiceOrderConditionService.replaceConditionWithDbValues(pc.getConditions());

                    Integer finalQuantity = 0;

                    if (Boolean.TRUE.equals(pc.getNoPodCondition())) {
                        finalQuantity = calculateOnNoPodCondition(serviceOrderForInvoiceResponse, pc, condition,
                                finalQuantity, podIds, unrecognizedPodIdentifiers);
                    } else {
                        finalQuantity = calculateOnPodsConditions(serviceOrderForInvoiceResponse, pc, podIds, condition, finalQuantity);
                    }

                    pc.setPodQuantityToMultiplyPrice(finalQuantity);
                }
        );
    }

    private Integer calculateOnPodsConditions(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, PriceComponentForServiceOrderResponseImpl pc, List<Long> podIds, String condition, Integer finalQuantity) {
        if (CollectionUtils.isNotEmpty(podIds)) {
            for (Long podId : podIds) {
                Integer evaluated = evaluateServiceOrderConditionService.evaluateConditions(serviceOrderForInvoiceResponse.getId(), podId, condition);
                finalQuantity += evaluated;
                if (evaluated != 0 && Objects.equals(pc.getApplicationLevel(), ApplicationLevel.POD)) {
                    pc.getPodIds().add(podId);
                }
            }
            if (finalQuantity > 0 && !Objects.equals(pc.getApplicationLevel(), ApplicationLevel.POD)) {
                finalQuantity = 1;
            }
        }
        return finalQuantity;
    }

    private Integer calculateOnNoPodCondition(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, PriceComponentForServiceOrderResponseImpl pc, String condition, Integer finalQuantity, List<Long> podIds, List<String> unrecognizedPodIdentifiers) {
        Integer evaluated = evaluateServiceOrderConditionService.evaluateConditions(serviceOrderForInvoiceResponse.getId(), null, condition);
        if (evaluated != 0) {
            finalQuantity = serviceOrderForInvoiceResponse.getPodQuantity().intValue();
            if (Objects.equals(pc.getApplicationLevel(), ApplicationLevel.POD)) {
                pc.getPodIds().addAll(podIds);
                pc.getUnrecognizedPods().addAll(unrecognizedPodIdentifiers);
            }
        }
        return finalQuantity;
    }

    private List<String> getUnrecognizedPods(String unrecognizedPods) {
        return Objects.nonNull(unrecognizedPods) ? Arrays.stream(unrecognizedPods.split(";"))
                .toList() : new ArrayList<>();
    }

    private List<Long> getRecognizedPods(String recognizedPodIds) {
        return Objects.nonNull(recognizedPodIds) ? Arrays.stream(recognizedPodIds.split(";"))
                .map(Long::parseLong)
                .toList() : new ArrayList<>();
    }

    private boolean noConditionsUsed(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, PriceComponentForServiceOrderResponseImpl pc, List<Long> podIds, List<String> unrecognizedPodIdentifiers) {
        if (StringUtils.isEmpty(pc.getConditions())) {
            if (Objects.equals(pc.getApplicationLevel(), ApplicationLevel.POD)) {
                pc.getPodIds().addAll(podIds);
                pc.getUnrecognizedPods().addAll(unrecognizedPodIdentifiers);
            }
            pc.setPodQuantityToMultiplyPrice(Objects.nonNull(serviceOrderForInvoiceResponse.getPodQuantity())
                                             && serviceOrderForInvoiceResponse.getPodQuantity() != 0L ?
                    serviceOrderForInvoiceResponse.getPodQuantity().intValue() : 1);
            return true;
        }
        return false;
    }

    /**
     * Saves the calculated invoice VAT rate values and invoice detailed data to the respective repositories.
     * This method is called after creating the invoices for a service order.
     */
    private void generateInvoices(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, Currency mainCurrency, Map<IssuedSeparateInvoice, List<PriceComponentForServiceOrderResponseImpl>> collectedByIssuedSeparateInvoice, Map<Long, List<ServiceOrderProcessPriceParamResponse>> priceParameters, List<Invoice> invoicesList) {
        List<InvoiceDetailedData> uncommittedInvoiceDetailedData = new ArrayList<>();
        List<InvoiceVatRateValue> invoiceVatRateValues = new ArrayList<>();

        ServiceOrderPaymentTermResponse invoicePaymentTerms = invoicePaymentTermsRepository.findInvoiceTermForServiceOrder(serviceOrderForInvoiceResponse.getId())
                .orElseThrow(() -> new ClientException("cannot calculate  payment deadline due to missing payment term", ErrorCode.CONFLICT));
        LocalDate paymentDeadline = calculatePaymentDeadline(serviceOrderForInvoiceResponse.getPaymentTermValue(), invoicePaymentTerms, LocalDate.now());


        for (Map.Entry<IssuedSeparateInvoice, List<PriceComponentForServiceOrderResponseImpl>> entry : collectedByIssuedSeparateInvoice.entrySet()) {
            try {
                createInvoices(serviceOrderForInvoiceResponse, mainCurrency, entry.getValue(), uncommittedInvoiceDetailedData, invoiceVatRateValues, priceParameters, invoicesList, invoicePaymentTerms.getNoInterestOnOverdueDebt(), paymentDeadline);
            } catch (Exception e) {
                throw new ClientException("exception happened during invoice generation - %s".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
            }
        }
        invoiceVatRateValueRepository.saveAll(invoiceVatRateValues);
        invoiceDetailedDataRepository.saveAll(uncommittedInvoiceDetailedData);

    }

    /**
     * Generates an invoice and its detailed data for a service order.
     * This method is called within the `generateInvoices` method to create individual invoices for a service order.
     *
     * @param serviceOrderForInvoiceResponse The service order response containing information needed to create the invoice.
     * @param mainCurrency                   The main currency to be used for the invoice.
     * @param pcList                         The list of price components for the service order.
     * @param invoiceVatRateValues           The list to store the calculated VAT rate values for the invoice.
     * @param priceParameters                The map of price parameters for the price components.
     * @param invoicesList                   The list to store the generated invoices.
     * @param noInterestOnOverdueDebt        Indicates whether there should be no interest on overdue debt.
     * @param paymentDeadline                The payment deadline for the invoice.
     * @throws OgnlException If an error occurs during price calculation.
     */
    private void createInvoices(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, Currency mainCurrency,
                                List<PriceComponentForServiceOrderResponseImpl> pcList,
                                List<InvoiceDetailedData> uncommittedInvoiceDetailedData,
                                List<InvoiceVatRateValue> invoiceVatRateValues,
                                Map<Long, List<ServiceOrderProcessPriceParamResponse>> priceParameters,
                                List<Invoice> invoicesList, Boolean noInterestOnOverdueDebt, LocalDate paymentDeadline) throws OgnlException {

        Invoice invoice = newInvoiceObject(mainCurrency, serviceOrderForInvoiceResponse, paymentDeadline, noInterestOnOverdueDebt);
        List<InvoiceDetailedData> detailedDataForCurrentInvoice = new ArrayList<>();

        for (PriceComponentForServiceOrderResponseImpl pc : pcList) {
            if (Objects.equals(pc.getApplicationLevel(), ApplicationLevel.POD)) {
                addDetailedDataWithPods(serviceOrderForInvoiceResponse, mainCurrency, priceParameters, pc, detailedDataForCurrentInvoice, invoice);
            } else {
                detailedDataForCurrentInvoice.add(
                        newInvoiceDetailedDataObject(invoice.getId(), serviceOrderForInvoiceResponse, pc,
                                mainCurrency, priceParameters.get(pc.getId()), null, null)
                );
            }

        }

        invoiceVatRateValues.addAll(orderInvoiceService.calculateVatRateValuesForOrderInvoices(detailedDataForCurrentInvoice, invoice));
        invoicesList.add(invoice);
        uncommittedInvoiceDetailedData.addAll(detailedDataForCurrentInvoice);

    }

    private void addDetailedDataWithPods(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, Currency mainCurrency, Map<Long, List<ServiceOrderProcessPriceParamResponse>> priceParameters, PriceComponentForServiceOrderResponseImpl pc, List<InvoiceDetailedData> detailedDataForCurrentInvoice, Invoice invoice) throws OgnlException {
        boolean hasPods = CollectionUtils.isNotEmpty(pc.getPodIds());
        boolean hasUnrecognizedPods = CollectionUtils.isNotEmpty(pc.getUnrecognizedPods());

        if (hasPods) {
            for (Long podId : pc.getPodIds()) {
                detailedDataForCurrentInvoice.add(
                        newInvoiceDetailedDataObject(invoice.getId(), serviceOrderForInvoiceResponse, pc,
                                mainCurrency, priceParameters.get(pc.getId()), podId, null)
                );
            }
        }
        if (hasUnrecognizedPods) {
            for (String pod : pc.getUnrecognizedPods()) {
                detailedDataForCurrentInvoice.add(
                        newInvoiceDetailedDataObject(invoice.getId(), serviceOrderForInvoiceResponse, pc,
                                mainCurrency, priceParameters.get(pc.getId()), null, pod)
                );
            }
        }
        if (!hasPods && !hasUnrecognizedPods) {
            detailedDataForCurrentInvoice.add(
                    newInvoiceDetailedDataObject(invoice.getId(), serviceOrderForInvoiceResponse, pc,
                            mainCurrency, priceParameters.get(pc.getId()), null, null)
            );
        }
    }


    /**
     * Creates a new invoice detailed data object for a service order.
     *
     * @param id                             The ID of the invoice.
     * @param serviceOrderForInvoiceResponse The service order response containing information needed to create the invoice detailed data.
     * @param pc                             The price component for the service order.
     * @param mainCurrency                   The main currency to be used for the invoice detailed data.
     * @param priceParamIds                  The list of price parameters for the price component.
     * @return The newly created invoice detailed data object.
     * @throws OgnlException If an error occurs during price calculation.
     */
    private InvoiceDetailedData newInvoiceDetailedDataObject(Long id,
                                                             ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse,
                                                             PriceComponentForServiceOrderResponseImpl pc,
                                                             Currency mainCurrency,
                                                             List<ServiceOrderProcessPriceParamResponse> priceParamIds,
                                                             Long podId,
                                                             String podIdentifier) throws OgnlException {

        BigDecimal calculatedPrice = calculatePrice(pc.getId(), pc.getPriceFormula(), priceParamIds, serviceOrderForInvoiceResponse.getId());
        BigDecimal finalQuantity = getFinalQuantity(serviceOrderForInvoiceResponse, pc);

        HashMap<Long, Currency> currencyHashMap = new HashMap<>();
        currencyHashMap.put(mainCurrency.getId(), mainCurrency);
        currencyHashMap.put(mainCurrency.getAltCurrencyId(), mainCurrency.getAltCurrency());

        calculatedPrice = billingRunCurrencyService.convertToCurrency(pc.getCurrencyId(), mainCurrency.getId(), calculatedPrice, currencyHashMap)
                .multiply(Objects.equals(pc.getNumberType(), NumberType.NEGATIVE) ?
                        BigDecimal.valueOf(-1) : BigDecimal.ONE);

        BigDecimal calculatedPriceWithQuantity = calculatedPrice.multiply(
                finalQuantity).setScale(12, RoundingMode.HALF_UP);

        return InvoiceDetailedData.builder()
                .invoiceId(id)
                .priceComponentId(pc.getId())
                .totalVolumes(finalQuantity)
                .measureUnitForTotalVolumesServiceOrder(pc.getServiceUnitId())
                .unitPrice(calculatedPrice)
                .value(calculatedPriceWithQuantity)
                .measureUnitForUnitPrice(pc.getValueTypeId())
                .measureUnitForValueOrders(mainCurrency.getId())
                .currencyExchangeRate(mainCurrency.getAltCurrencyExchangeRate())
                .vatRateId(pc.getVatRateId())
                .vatRatePercent(BigDecimal.valueOf(pc.getVatRatePercent() != null ? pc.getVatRatePercent() : 1))
                .costCenterControllingOrder(pc.getCostCenterControllingOrder() != null ?
                        pc.getCostCenterControllingOrder() : serviceOrderForInvoiceResponse.getCostCenterControllingOrder())
                .incomeAccountNumber(pc.getIncomeAccountNumber() != null ?
                        pc.getIncomeAccountNumber() : serviceOrderForInvoiceResponse.getIncomeAccountNumber())
                .pcGroupDetailId(pc.getPcGroupDetailId())
                .podId(podId)
                .unrecognizedPod(podIdentifier)
                .build();
    }

    /**
     * Calculates the final quantity for a price component based on the service order response.
     *
     * @param serviceOrderForInvoiceResponse The service order response containing information needed to calculate the final quantity.
     * @param pc                             The price component for the service order.
     * @return The calculated final quantity.
     */
    private BigDecimal getFinalQuantity(ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse, PriceComponentForServiceOrderResponseImpl pc) {
        BigDecimal finalQuantity = BigDecimal.ONE;
        if (pc.getApplicationModelType().equals(ApplicationModelType.PRICE_AM_PER_PIECE)) {
            Integer quantity = serviceOrderForInvoiceResponse.getQuantity();
            if (Objects.isNull(quantity)) {
                throw new ClientException("cannot calculate price due to missing quantity in service order;", ErrorCode.CONFLICT);
            }
            String[] ranges = pc.getPerPieceRanges().split(";");
            int amount = 0;
            for (String s : ranges) {
                String[] range = s.split("-");
                int valueFrom = Integer.parseInt(range[0]);
                int valueTo = Integer.parseInt(range[1]);

                if (quantity > valueTo) {
                    amount += valueTo - valueFrom + 1;
                } else if (quantity < valueFrom) {
                    amount = quantity;
                    break;
                } else {
                    amount += quantity - valueFrom + 1;
                    break;
                }

            }
            finalQuantity = BigDecimal.valueOf(amount);
        }
        return finalQuantity;
    }

    /**
     * Creates a new invoice object with the provided details.
     *
     * @param mainCurrency                   The main currency to be used for the invoice.
     * @param serviceOrderForInvoiceResponse The service order response containing information needed to create the invoice.
     * @param paymentDeadline                The payment deadline for the invoice.
     * @param noInterestOnOverdueDebt        Indicates whether no interest should be charged on overdue debt.
     * @return The newly created invoice object.
     */
    private Invoice newInvoiceObject(Currency mainCurrency, ServiceOrderForInvoiceResponse serviceOrderForInvoiceResponse,
                                     LocalDate paymentDeadline, Boolean noInterestOnOverdueDebt) {
        Invoice invoice = Invoice.builder()
                .invoiceStatus(InvoiceStatus.DRAFT)
                .invoiceDocumentType(InvoiceDocumentType.PROFORMA_INVOICE)
                .invoiceType(InvoiceType.STANDARD)
                .invoiceDate(LocalDate.now())
                .incomeAccountNumber(serviceOrderForInvoiceResponse.getIncomeAccountNumber())
                .costCenterControllingOrder(serviceOrderForInvoiceResponse.getCostCenterControllingOrder())
                .interestRateId(serviceOrderForInvoiceResponse.getApplicableInterestRateId())
                .directDebit(serviceOrderForInvoiceResponse.getDirectDebit())
                .bankId(serviceOrderForInvoiceResponse.getBankId())
                .iban(serviceOrderForInvoiceResponse.getIban())
                .accountPeriodId(accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId().orElse(null))
                .currencyId(mainCurrency.getId())
                .currencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate())
                .currencyIdInOtherCurrency(mainCurrency.getAltCurrencyId())
                .basisForIssuing("Продажба на стоки")
                .paymentDeadline(paymentDeadline)
                .noInterestOnOverdueDebts(noInterestOnOverdueDebt)
                .customerDetailId(serviceOrderForInvoiceResponse.getCustomerDetailId())
                .customerId(serviceOrderForInvoiceResponse.getCustomerId())
                .customerCommunicationId(serviceOrderForInvoiceResponse.getCustomerCommunicationId())
                .serviceOrderId(serviceOrderForInvoiceResponse.getId())
                .serviceDetailId(serviceOrderForInvoiceResponse.getServiceDetailId())
                .alternativeRecipientCustomerDetailId(serviceOrderForInvoiceResponse.getCustomerDetailId())
                .templateDetailId(serviceOrderForInvoiceResponse.getTemplateDetailId())
                .build();
        invoice.setCreateDate(LocalDateTime.now());
        return invoiceRepository.saveAndFlush(invoice);
    }

    /**
     * Calculates the payment deadline for an invoice based on the provided payment terms.
     *
     * @param termValue           The number of days for the payment term.
     * @param invoicePaymentTerms The payment term details, including calendar type, weekends, holidays, and due date change.
     * @param invoiceDate         The date the invoice was created.
     * @return The calculated payment deadline date.
     */
    private LocalDate calculatePaymentDeadline(Integer termValue, ServiceOrderPaymentTermResponse invoicePaymentTerms, LocalDate invoiceDate) {
        LocalDate endDate = invoiceDate;
        Boolean excludeWeekends = invoicePaymentTerms.getExcludeWeekends();
        Boolean excludeHolidays = invoicePaymentTerms.getExcludeHolidays();
        DueDateChange dueDateChange = invoicePaymentTerms.getDueDateChange();
        CalendarType calendarType = invoicePaymentTerms.getCalendarType();
        termValue = Objects.requireNonNullElse(termValue, invoicePaymentTerms.getOrderTermValue());
        Calendar calendar = calendarRepository
                .findByIdAndStatusIsIn(invoicePaymentTerms.getCalendarId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Presented Payment Term calendar not found;"));
        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(invoicePaymentTerms.getCalendarId(), List.of(HolidayStatus.ACTIVE));

        switch (calendarType) {
            case CALENDAR_DAYS -> {
                weekends = Boolean.TRUE.equals(excludeWeekends) ? weekends : new ArrayList<>();
                holidays = Boolean.TRUE.equals(excludeHolidays) ? holidays : new ArrayList<>();
                endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(termValue, endDate, weekends, holidays);
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
            case WORKING_DAYS ->
                    endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(termValue, endDate, weekends, holidays);
            case CERTAIN_DAYS -> {
                endDate = PaymentTermUtils.calculateEndDateForCertainDays(termValue, endDate);
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
        }
        return endDate;
    }


    /**
     * Calculates the price for a given price component based on the provided formula and price parameters.
     *
     * @param priceComponentId the ID of the price component
     * @param formula          the formula used to calculate the price
     * @param priceParamIds    the list of price parameters to be used in the formula
     * @param id
     * @return the calculated price for the price component
     * @throws OgnlException if there is an error evaluating the formula
     */
    private BigDecimal calculatePrice(Long priceComponentId, String formula, List<ServiceOrderProcessPriceParamResponse> priceParamIds, Long id) throws OgnlException {
        if (formula.contains("$PRICE_PROFILE$")) {
            throw new ClientException("cannot generate invoice - price profile used in price formula;", ErrorCode.CONFLICT);
        }
        //calculate price component formula evaluation
        List<PriceComponentFormulaXValue> xValues = priceComponentFormulaVariableRepository.findAllByPriceComponentIdAndServiceOrderId(priceComponentId, id);

        Map<String, Object> variablesContext = new HashMap<>();

        for (PriceComponentFormulaXValue x : xValues) {
            variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
        }


        if (!CollectionUtils.isEmpty(priceParamIds)) {
            for (ServiceOrderProcessPriceParamResponse pp : priceParamIds) {
                if (!List.of(PeriodType.ONE_DAY, PeriodType.ONE_MONTH).contains(pp.getPeriodType())) {
                    throw new ClientException("cannot generate invoice due to non-valid price parameter periodicity;", ErrorCode.CONFLICT);
                }
                variablesContext.put("$" + pp.getId() + "$", pp.getPrice());

            }
        }
        return priceComponentEvaluationService.evaluateExpression(formula, variablesContext);
    }

    /**
     * Extracts a list of unique price parameter IDs from the provided formula.
     *
     * @param formula the formula string to extract the price parameter IDs from
     * @return a list of unique price parameter IDs
     */
    private List<Long> extractPriceParametersFromFormula(String formula) {
        Set<String> priceComponentMathVariableNames = Arrays.stream(PriceComponentMathVariableName.values())
                .map(PriceComponentMathVariableName::name)
                .collect(Collectors.toSet());

        List<String> formulaVariables = expressionStringParser.getVariables(formula);

        return formulaVariables.stream()
                .filter(fv -> !priceComponentMathVariableNames.contains(fv))
                .distinct()
                .map(Long::valueOf)
                .toList();
    }

}

