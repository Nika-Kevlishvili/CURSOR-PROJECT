package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDetailedData;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderPaymentTerm;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermDueDateChange;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.model.request.billing.invoice.CreateInvoiceCancellationRequest;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderForInvoiceResponse;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderGoodsForInvoiceResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDetailedDataRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderGoodsRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderPaymentTermRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.invoice.cancellation.InvoiceCancellationService;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.util.order.OrderInvoiceService;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static bg.energo.phoenix.permissions.PermissionContextEnum.GOODS_ORDERS;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsOrderProcessService {
    private final GoodsOrderRepository goodsOrderRepository;
    private final GoodsOrderGoodsRepository goodsOrderGoodsRepository;
    private final CurrencyRepository currencyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailedDataRepository invoiceDetailedDataRepository;
    private final CalendarRepository calendarRepository;
    private final HolidaysRepository holidaysRepository;
    private final GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final PermissionService permissionService;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final OrderInvoiceService orderInvoiceService;
    private final TransactionTemplate transactionTemplate;
    private final DocumentsRepository documentsRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final InvoiceCancellationService invoiceCancellationService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;



    /**
     * issues pro forma draft invoice for goods order.
     *
     * @param id ID of the goods order
     */
    @Transactional
    public void issueProformaInvoice(Long id) {
        if (!permissionService.getPermissionsFromContext(GOODS_ORDERS).contains(PermissionEnum.GOODS_ORDER_CREATE_PRO_FORMA_INVOICE.getId())) {
            throw new ClientException("cannot issue pro forma invoice without permission", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        GoodsOrderForInvoiceResponse goodsOrder = goodsOrderRepository.findByOrderIdAndOrderStatusAndNonExistentInvoice(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find appropriate goods order to issue pro forma invoice;"));


        Currency mainCurrency = currencyRepository
                .findMainCurrencyNowAndActive()
                .orElseThrow(() -> new DomainEntityNotFoundException("Respective main currency not found for this time;"));

        generateProFormaInvoice(goodsOrder, mainCurrency);
        goodsOrderRepository.findById(id).get().setOrderInvoiceStatus(OrderInvoiceStatus.DRAFT_PROFORMA_GENERATED);

    }

    /**
     * changes pro forma draft invoice status to real,
     * goods order status to 'awaiting payment',
     * generates customer liability from invoice.
     *
     * @param id ID of the goods order
     */
    @Transactional
    public void startAccounting(Long id) {
        GoodsOrder goodsOrder = goodsOrderRepository.findByIdAndOrderStatus(id, GoodsOrderStatus.CONFIRMED)
                .orElseThrow(() -> new DomainEntityNotFoundException("valid goods order with 'confirmed' status not found to start accounting;"));
        Invoice invoice = invoiceRepository.findGoodsOrderInvoiceByStatusAndDocumentTypeIn(id, InvoiceStatus.DRAFT_GENERATED, List.of(InvoiceDocumentType.PROFORMA_INVOICE))
                .orElseThrow(() -> new DomainEntityNotFoundException("valid invoice with 'draft generated' status not found to start accounting for;"));
        invoice.setInvoiceStatus(InvoiceStatus.REAL);
        goodsOrder.setOrderStatus(GoodsOrderStatus.AWAITING_PAYMENT);
        goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_PROFORMA_GENERATED);
        invoiceRepository.saveAndFlush(invoice);
        customerLiabilityService.createLiabilityFromInvoice(invoice.getId(), LiabilityOrReceivableCreationSource.GOODS_ORDER);
        invoiceNumberService.fillInvoiceNumber(invoice);
        invoiceRepository.findLatestInvoiceDocument(invoice.getId()).ifPresent(document -> orderInvoiceService.sendMailForGoodsOrder(invoice, goodsOrder, document));
    }


    public void startGenerating(Long id) {
        MDC.put("goodsOrderGenerating", String.valueOf(id));
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        GoodsOrder goodsOrder = goodsOrderRepository.findByIdAndOrderStatus(id, GoodsOrderStatus.CONFIRMED)
                .orElseThrow(() -> new DomainEntityNotFoundException("valid goods order with 'confirmed' status not found to start generating;"));
        Pair<Invoice, Document> result = transactionTemplate.execute((x) -> {

            Invoice invoice = invoiceRepository.findGoodsOrderInvoiceByStatusAndDocumentTypeIn(id, InvoiceStatus.DRAFT, List.of(InvoiceDocumentType.PROFORMA_INVOICE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("valid draft proforma invoice not found to start generating for;"));
            invoice.setInvoiceStatus(InvoiceStatus.DRAFT_GENERATED);
            invoiceNumberService.fillInvoiceNumber(invoice);
            goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.PDF_GENERATED);
            Document invoiceDocument = orderInvoiceService.generateDocumentForInvoice(invoice, true);
            invoiceRepository.saveAndFlush(invoice);
            return Pair.of(invoice, invoiceDocument);
        });
        log.debug("generation success; {}", result.getValue().getName());
    }


    /**
     * changes pro forma real invoice document type to 'invoice',
     * updates payment deadline, invoice number and invoice date.
     *
     * @param id ID of the goods order
     */
    public void issueInvoice(Long id) {
        if (!permissionService.getPermissionsFromContext(GOODS_ORDERS).contains(PermissionEnum.GOODS_ORDER_CREATE_INVOICE.getId())) {
            throw new ClientException("cannot issue real invoice without permission", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        GoodsOrder goodsOrder = goodsOrderRepository.findByIdAndOrderStatusAndPaymentTermFilled(id, GoodsOrderStatus.AWAITING_PAYMENT)
                .orElseThrow(() -> new ClientException("valid goods order not found to issue invoice - order status is invalid or payment term is not filled", ErrorCode.CONFLICT));

        Pair<Invoice, Document> result = transactionTemplate.execute((x) -> {
            GoodsOrderPaymentTerm goodsOrderPaymentTerm = goodsOrderPaymentTermRepository
                    .findAllByOrderIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                    .stream().findFirst()
                    .orElseThrow(() -> new DomainEntityNotFoundException("active payment term cannot be found for goods order;"));

            Invoice invoice = invoiceRepository.findGoodsOrderInvoiceByStatusAndDocumentTypeIn(id, InvoiceStatus.REAL, List.of(InvoiceDocumentType.PROFORMA_INVOICE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("valid real proforma invoice not found to issue invoice;"));
            invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setTaxEventDate(LocalDate.now());
            invoice.setPaymentDeadline(calculateDeadline(goodsOrderPaymentTerm, LocalDate.now()));
            invoice.setCurrentStatusChangeDate(LocalDateTime.now());
            invoiceNumberService.fillInvoiceNumber(invoice);
            Document invoiceDocument = orderInvoiceService.generateDocumentForInvoice(invoice, true);
            goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.REAL_INVOICE_GENERATED);
            customerLiabilityRepository.updateAllIncomeAccountNumberByLiabilityIds(invoice.getId(), incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name()));
            return Pair.of(invoice, invoiceDocument);
        });
        orderInvoiceService.sendMailForGoodsOrder(result.getKey(), goodsOrder, result.getValue());
    }



    /**
     * Checks for and deletes any overdue pro forma invoices associated with goods orders.
     */
    @Transactional
    @ExecutionTimeLogger
    public void checkOverdueProformaInvoicesForOrders() {
        List<GoodsOrder> orders = goodsOrderRepository.findOrdersWithOverdueInvoices();
        if (!CollectionUtils.isEmpty(orders)) {
            orders.forEach(order -> order.setStatus(EntityStatus.DELETED));
            List<Invoice> invoicesToDelete = invoiceRepository.findAllByGoodsOrderIdIn(orders.stream().map(GoodsOrder::getId).toList());
            orderInvoiceService.extractInvoicesAndDeleteRelatedEntities(invoicesToDelete);
        }
    }

    /**
     * deletes pro forma draft invoice for goods order
     *
     * @param id ID of the goods order
     */
    @Transactional
    public void deleteProformaInvoice(Long id) {
        List<Object[]> goodsOrderInvoiceByStatusAndDocumentTypeToDelete = invoiceRepository.findGoodsOrderInvoiceByStatusAndDocumentTypeToDelete(id, InvoiceDocumentType.PROFORMA_INVOICE);
        if (goodsOrderInvoiceByStatusAndDocumentTypeToDelete.isEmpty()) {
            throw new DomainEntityNotFoundException("goods order invoice not found to delete;");
        }
        Object[] objects = goodsOrderInvoiceByStatusAndDocumentTypeToDelete.get(0);
        Invoice invoice = (Invoice) objects[0];
        GoodsOrder goodsOrder = (GoodsOrder) objects[1];
        if (invoice.getInvoiceStatus().equals(InvoiceStatus.REAL)) {
            goodsOrder.setOrderStatus(GoodsOrderStatus.CONFIRMED);
        }
        goodsOrder.setOrderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED);
        orderInvoiceService.extractInvoicesAndDeleteRelatedEntities(List.of(invoice));
    }

    /**
     * Cancels an invoice associated with a goods order.
     *
     * @param id         The ID of the goods order.
     * @param templateId The ID of the template to be used for the invoice cancellation.
     */
    public void cancelInvoice(Long id, Long templateId) {
        Invoice invoice = invoiceRepository.findGoodsOrderInvoiceByStatusAndDocumentTypeIn(id, InvoiceStatus.REAL, List.of(InvoiceDocumentType.PROFORMA_INVOICE, InvoiceDocumentType.INVOICE))
                .orElseThrow(() -> new DomainEntityNotFoundException("valid invoice not found to start cancellation process for."));

        invoiceCancellationService.createInvoiceCancellation(CreateInvoiceCancellationRequest.builder()
                .invoices(invoice.getInvoiceNumber().substring(invoice.getInvoiceNumber().indexOf("-") + 1))
                .taxEventDate(LocalDate.now())
                .templateId(templateId)
                .build());
    }

    /**
     * Generates a pro forma invoice for a goods order.
     *
     * @param goodsOrder   The goods order for which the pro forma invoice is being generated.
     * @param mainCurrency The main currency to be used for the invoice.
     */
    private void generateProFormaInvoice(GoodsOrderForInvoiceResponse goodsOrder, Currency mainCurrency) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(LocalDate.now());
        GoodsOrderPaymentTerm goodsOrderPaymentTerm = goodsOrderPaymentTermRepository
                .findAllByOrderIdAndStatusIn(goodsOrder.id(), List.of(EntityStatus.ACTIVE))
                .stream().findFirst()
                .orElseThrow(() -> new DomainEntityNotFoundException("active payment term cannot be found for goods order;"));
        invoice.setPaymentDeadline(calculateDeadline(goodsOrderPaymentTerm, LocalDate.now()));
        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceDocumentType(InvoiceDocumentType.PROFORMA_INVOICE);
        invoice.setInvoiceType(InvoiceType.STANDARD);
        invoice.setBasisForIssuing("Продажба на стоки");
        invoice.setIncomeAccountNumber(goodsOrder.incomeAccountNumber());
        invoice.setCostCenterControllingOrder(goodsOrder.costCenterControllingOrder());
        invoice.setInterestRateId(goodsOrder.applicableInterestRateId());
        invoice.setDirectDebit(goodsOrder.directDebit());
        invoice.setBankId(goodsOrder.bankId());
        invoice.setCustomerDetailId(goodsOrder.customerDetailId());
        invoice.setCustomerId(goodsOrder.customerId());
        invoice.setCustomerCommunicationId(goodsOrder.customerCommunicationId());
        invoice.setGoodsOrderId(goodsOrder.id());
        invoice.setIban(goodsOrder.iban());
        invoice.setCurrencyId(mainCurrency.getId());
        invoice.setCurrencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate());
        invoice.setCurrencyIdInOtherCurrency(mainCurrency.getAltCurrencyId());
        invoice.setAccountPeriodId(accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId().orElse(null));
        invoice.setAlternativeRecipientCustomerDetailId(goodsOrder.customerDetailId());
        invoice.setNoInterestOnOverdueDebts(goodsOrder.noInterestOnOverdueDebts());
        invoice.setTemplateDetailId(contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIdAndDate(goodsOrder.invoiceTemplateId(), LocalDate.now()).map(ContractTemplateDetail::getId).orElse(null));
        invoice = invoiceRepository.save(invoice);
        List<InvoiceDetailedData> invoiceDetailedDataList = generateInvoiceDetailedData(invoice.getId(), goodsOrder, mainCurrency);
        List<InvoiceVatRateValue> uncommittedInvoiceVatRates = orderInvoiceService.calculateVatRateValuesForOrderInvoices(invoiceDetailedDataList, invoice);
        invoiceVatRateValueRepository.saveAll(uncommittedInvoiceVatRates);
        invoiceNumberService.fillInvoiceNumber(invoice);
    }


    /**
     * Generates a list of {@link InvoiceDetailedData} objects for a given goods order and main currency.
     *
     * @param invoiceId    The ID of the invoice for which the detailed data is being generated.
     * @param goodsOrder   The goods order for which the invoice detailed data is being generated.
     * @param mainCurrency The main currency to be used for the invoice.
     * @return A list of {@link InvoiceDetailedData} objects representing the detailed data for the invoice.
     */
    private List<InvoiceDetailedData> generateInvoiceDetailedData(Long invoiceId, GoodsOrderForInvoiceResponse goodsOrder, Currency mainCurrency) {

        List<GoodsOrderGoodsForInvoiceResponse> orderGoods = goodsOrderGoodsRepository.getAllByOrderId(goodsOrder.id());
        List<InvoiceDetailedData> uncommittedDetailedDataList = new ArrayList<>();
        for (GoodsOrderGoodsForInvoiceResponse good : orderGoods) {
            uncommittedDetailedDataList.add(createInvoiceDetailedData(goodsOrder, good, mainCurrency, invoiceId));
        }
        invoiceDetailedDataRepository.saveAll(uncommittedDetailedDataList);
        return uncommittedDetailedDataList;
    }

    /**
     * Creates an {@link InvoiceDetailedData} object for a given goods order, goods item, main currency, and invoice ID.
     *
     * @param goodsOrder   The goods order for which the invoice detailed data is being generated.
     * @param good         The goods item for which the invoice detailed data is being generated.
     * @param mainCurrency The main currency to be used for the invoice.
     * @param invoiceId    The ID of the invoice for which the detailed data is being generated.
     * @return An {@link InvoiceDetailedData} object representing the detailed data for the invoice.
     */
    private InvoiceDetailedData createInvoiceDetailedData(GoodsOrderForInvoiceResponse goodsOrder, GoodsOrderGoodsForInvoiceResponse good, Currency mainCurrency, Long invoiceId) {
        BigDecimal amount = good.getPrice().multiply(BigDecimal.valueOf(good.getQuantity()));
        if (!good.getCurrencyId().equals(mainCurrency.getId())) {
            if (!Objects.equals(good.getAltCurrencyId(), mainCurrency.getId())) {
                throw new ClientException("goods currency cannot be exchanged to main currency", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
            amount = amount.multiply(good.getAltCurrencyExchangeRate()).setScale(10, RoundingMode.HALF_UP);
        }
        InvoiceDetailedData invoiceDetailedData = new InvoiceDetailedData();
        invoiceDetailedData.setInvoiceId(invoiceId);
        invoiceDetailedData.setValue(amount);
        invoiceDetailedData.setMeasureUnitForValueOrders(mainCurrency.getId());
        invoiceDetailedData.setUnitPrice(good.getPrice());
        invoiceDetailedData.setTotalVolumes(BigDecimal.valueOf(good.getQuantity()));
        invoiceDetailedData.setMeasureUnitForTotalVolumesGoodsOrder(good.getGoodsUnitId());
        invoiceDetailedData.setVatRateId(goodsOrder.vatRateId());
        invoiceDetailedData.setVatRatePercent(goodsOrder.vatRatePercent() != null ? goodsOrder.vatRatePercent() : BigDecimal.valueOf(1));
        invoiceDetailedData.setIncomeAccountNumber(good.getNumberOfIncomingAccount() != null ? good.getNumberOfIncomingAccount() : goodsOrder.incomeAccountNumber());
        invoiceDetailedData.setCostCenterControllingOrder(good.getCostCenterOrControllingOrder() != null ? good.getCostCenterOrControllingOrder() : goodsOrder.costCenterControllingOrder());
        invoiceDetailedData.setGoodName(good.getName());
        return invoiceDetailedData;
    }

    /**
     * Calculates the deadline date for a goods order payment term based on the specified parameters.
     *
     * @param term        The payment term object containing the details for calculating the deadline.
     * @param invoiceDate The date of the invoice for which the deadline is being calculated.
     * @return The calculated deadline date.
     */
    private LocalDate calculateDeadline(GoodsOrderPaymentTerm term, LocalDate invoiceDate) {
        Integer days = term.getValue();
        LocalDate endDate = invoiceDate;
        List<GoodsOrderPaymentTermExcludes> excludes = term.getExcludes();
        GoodsOrderPaymentTermDueDateChange dueDateChange = term.getDueDateChange();

        Long calendarId = term.getCalendarId();
        Calendar calendar = calendarRepository
                .findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Presented Payment Term calendar not found;"));
        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarId, List.of(HolidayStatus.ACTIVE));

        switch (term.getType()) {
            case CALENDAR_DAYS -> {
                weekends = excludes.contains(GoodsOrderPaymentTermExcludes.WEEKENDS) ? weekends : new ArrayList<>();
                holidays = excludes.contains(GoodsOrderPaymentTermExcludes.HOLIDAYS) ? holidays : new ArrayList<>();
                endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(days, endDate, weekends, holidays);
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
            case WORKING_DAYS ->
                    endDate = PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(days, endDate, weekends, holidays);
            case CERTAIN_DAYS -> {
                endDate = PaymentTermUtils.calculateEndDateForCertainDays(days, endDate);
                endDate = PaymentTermUtils.shiftDateAccordingToTermDueDate(endDate, Objects.nonNull(dueDateChange) ? dueDateChange.name() : null, weekends, holidays);
            }
        }
        return endDate;
    }


}
