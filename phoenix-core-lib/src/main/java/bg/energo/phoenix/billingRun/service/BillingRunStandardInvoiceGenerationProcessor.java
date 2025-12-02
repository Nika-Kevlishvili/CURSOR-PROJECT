package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.*;
import bg.energo.phoenix.billingRun.model.entity.*;
import bg.energo.phoenix.billingRun.repository.*;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.compensation.Compensations;
import bg.energo.phoenix.model.entity.billing.invoice.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.product.EntityType;
import bg.energo.phoenix.model.response.billing.billingRun.BillingRunStandardProcessModel;
import bg.energo.phoenix.model.response.product.CostCenterAndIncomeAccountResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.compensation.CompensationRepository;
import bg.energo.phoenix.repository.billing.invoice.*;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.billing.billingRun.actions.startGeneration.BillingRunStartGenerationService;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStandardInvoiceGenerationProcessor {
    private final CurrencyRepository currencyRepository;
    private final VatRateRepository vatRateRepository;
    private final BillingRunProcessingPerPieceService billingRunProcessingPerPieceService;
    private final BillingRunProcessingOverTimeService billingRunProcessingOverTimeService;
    private final BillingRunProcessSettlementService billingRunProcessSettlementService;
    private final BillingRunProcessScaleCalculationService billingRunProcessScaleCalculationService;
    private final PriceComponentRepository priceComponentRepository;
    private final BillingRunContractsRepository billingRunContractsRepository;
    private final BillingRunDiscountService billingRunDiscountService;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceTotalActualConsumptionRepository invoiceTotalActualConsumptionRepository;
    private final BillingRunCompensationMappingRepository billingRunCompensationMappingRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;
    private final BillingPaymentTermDayCalculationService billingPaymentTermDayCalculationService;
    private final InvoiceStandardDetailedDataRepository invoiceStandardDetailedDataRepository;
    private final InvoiceStandardDetailedDataVatBaseRepository invoiceStandardDetailedDataVatBaseRepository;
    private final BillingRunBgInvoiceSlotsRepository billingRunBgInvoiceSlotsRepository;
    private final BillingRunSvInvoiceSlotsRepository billingRunSvInvoiceSlotsRepository;
    private final BillingRunRestrictionPercentService billingRunRestrictionPercentService;
    private final BillingRunRestrictionKwhService billingRunRestrictionKwhService;
    private final BillingRunRestrictionCcyService billingRunRestrictionCcyService;
    private final BillingRunInterimDataRepository billingRunInterimDataRepository;
    private final BillingRunInterimProcessingService billingRunInterimProcessingService;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final BillingRunRepository billingRunRepository;
    private final TransactionTemplate transactionTemplate;
    private final InvoiceNumberService invoiceNumberService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final BillingRunStartGenerationService billingRunStartGenerationService;
    private final InvoiceService invoiceService;
    private final CompensationRepository compensationRepository;
    private final BillingRunCorrectionService billingRunCorrectionService;
    private final BillingRunCorrectionInvoiceBaseRepository billingRunCorrectionInvoiceBaseRepository;
    private final DeductionInterimInvoiceRepository deductionInterimInvoiceRepository;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processContract(LocalDateTime billingRunDate,
                                BillingRunContracts runContract,
                                HashSet<Long> failedSlots,
                                BillingRun billingRunModel) {
        try {
            HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap = new HashMap<>();
            HashMap<Long, PriceComponent> priceComponentHashMap = new HashMap<>();

            Optional<Currency> mainCurrencyOptional = currencyRepository.findMainCurrencyNowAndActive();
            if (mainCurrencyOptional.isEmpty()) {
                throw new RuntimeException("cannot find main currency in system");
            }
            Currency mainCurrency = mainCurrencyOptional.get();
            VatRate globalVatRate = vatRateRepository.findGlobalVatRate(billingRunDate.toLocalDate(), PageRequest.of(0, 1)).get();

            Long mainCurrencyId = mainCurrency.getId();
            Long altCurrencyId = mainCurrency.getAltCurrency().getId();
            HashMap<Long, VatRate> vatRateMap = new HashMap<>();
            HashMap<Long, Currency> currencyHashMap = new HashMap<>();

            List<BillingRunInvoiceDetailBaseModel> processedInvoiceDetailList = new ArrayList<>();
            List<BillingRunInvoiceDetailBaseModel> perPieceDetails = null;
            List<BillingRunInvoiceDetailBaseModel> overTimePeriodicalDetails = null;
            List<BillingRunInvoiceDetailBaseModel> overTimeOneTimeDetails = null;
            Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> groupedSettlements = null;
            Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> groupedScales = null;
            List<BillingRunInvoiceDetailBaseModel> overTimeElectricityDetails = null;

            List<ApplicationModelType> applicationModelTypes = billingRunModel.getApplicationModelType();

            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION) || applicationModelTypes.contains(ApplicationModelType.PER_PIECE)) {
                perPieceDetails = billingRunProcessingPerPieceService.evaluatePerPiece(billingRunModel.getInvoiceDate(), runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);
            }

            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION) || applicationModelTypes.contains(ApplicationModelType.OVER_TIME_PERIODICAL)) {
                overTimePeriodicalDetails = billingRunProcessingOverTimeService.evaluateOverTimePeriodical(billingRunModel.getInvoiceDate(), runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);
            }

            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION) || applicationModelTypes.contains(ApplicationModelType.OVER_TIME_ONE_TIME)) {
                overTimeOneTimeDetails = billingRunProcessingOverTimeService.evaluateOverTimeOneTime(billingRunModel.getInvoiceDate(), runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);
            }
            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION) || applicationModelTypes.contains(ApplicationModelType.WITH_ELECTRICITY_INVOICE)) {
                overTimeElectricityDetails = billingRunProcessingOverTimeService.evaluateOverTimeWithElectricity(billingRunModel.getInvoiceDate(), runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);
            }


            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION) || applicationModelTypes.contains(ApplicationModelType.FOR_VOLUMES)) {

                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    groupedSettlements = billingRunProcessSettlementService.evaluateSettlement(runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);
                    groupedScales = billingRunProcessScaleCalculationService.splitAndEvaluateScales(runContract, failedSlots, xValueMap, mainCurrencyId, currencyHashMap, priceComponentHashMap);


                    HashMap<DiscountMap, List<Discount>> discountHashMap = new HashMap<>();

                    for (Map.Entry<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> row : groupedSettlements.entrySet()) {
                        SlotGrouping key = row.getKey();

                        PriceComponent priceComponent = priceComponentHashMap.getOrDefault(key.getPcId(), null);
                        if (priceComponent == null) {
                            priceComponent = priceComponentRepository.findById(key.getPcId()).get();
                            priceComponentHashMap.put(key.getPcId(), priceComponent);
                        }

                        try {
                            if (failedSlots.contains(row.getKey().getBgInvoiceSlotId())) {
                                continue;
                            }

                            //Calculate restriction if exist
                            List<CcyRestrictions> ccyRestrictions = billingRunContractsRepository.getSettlementRestrictionByCcy(row.getKey().getPcId());
                            List<KwhRestrictions> kwhRestrictions = billingRunContractsRepository.getSettlementRestrictionByKwh(row.getKey().getPcId());
                            BigDecimal percentRestriction = billingRunContractsRepository.getSettlementRestrictionPercent(row.getKey().getPcId());
                            if (ccyRestrictions.size() > 0 || kwhRestrictions.size() > 0 || percentRestriction != null) {
                                calculateRestrictions(row.getValue(), ccyRestrictions, kwhRestrictions, percentRestriction, priceComponent, currencyHashMap, mainCurrencyId, altCurrencyId);
                            }
                        } catch (Exception e) {
                            log.error("Exception in billing run settlement restriction calculation BgInvoiceSlotId: [%s], PcId: [%s] PodId: [%s]".formatted(key.getBgInvoiceSlotId(), key.getPcId(), key.getPodId()), e);
                            billingRunContractsRepository.updateBgInvoiceSlotStatus(key.getBgInvoiceSlotId(), "ERROR",
                                    "settlement restriction calculation error, PC_ID: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(key.getPcId(), key.getPodId(), e.getMessage()));
                            failedSlots.add(key.getBgInvoiceSlotId());
                        }

                        //remove all ignored restricted data
                        row.getValue().removeIf(v -> v.getFinalRestrictionVolume() != null && v.getFinalRestrictionVolume().compareTo(BigDecimal.ZERO) == 0);

                        if (!priceComponent.getDiscount()) {
                            continue;
                        }

                        try {
                            if (failedSlots.contains(row.getKey().getBgInvoiceSlotId())) {
                                continue;
                            }
                            List<BillingRunInvoiceDetailBaseModel> discountFinalList = billingRunDiscountService.processDiscount(row, key, discountHashMap, currencyHashMap, mainCurrencyId);
                            if (discountFinalList.isEmpty()) continue;

                            row.getValue().addAll(discountFinalList);

                        } catch (Exception e) {
                            log.error("Exception in billing run settlement discount calculation BgInvoiceSlotId: [%s], PcId: [%s] PodId: [%s]".formatted(key.getBgInvoiceSlotId(), key.getPcId(), key.getPodId()), e);
                            billingRunContractsRepository.updateBgInvoiceSlotStatus(key.getBgInvoiceSlotId(), "ERROR",
                                    "settlement discount calculation error, PC_ID: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(key.getPcId(), key.getPodId(), e.getMessage()));
                            failedSlots.add(key.getBgInvoiceSlotId());
                        }
                    }


                    for (Map.Entry<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> row : groupedScales.entrySet()) {
                        SlotGrouping key = row.getKey();
                        PriceComponent priceComponent = priceComponentHashMap.getOrDefault(key.getPcId(), null);
                        if (priceComponent == null) {
                            priceComponent = priceComponentRepository.findById(key.getPcId()).get();
                            priceComponentHashMap.put(key.getPcId(), priceComponent);
                        }
                        try {
                            if (failedSlots.contains(row.getKey().getBgInvoiceSlotId())) {
                                continue;
                            }

                            //Calculate restriction if exist
                            List<CcyRestrictions> ccyRestrictions = billingRunContractsRepository.getScaleRestrictionByCcy(row.getKey().getPcId());
                            List<KwhRestrictions> kwhRestrictions = billingRunContractsRepository.getScaleRestrictionByKwh(row.getKey().getPcId());
                            BigDecimal percentRestriction = billingRunContractsRepository.getScaleRestrictionPercent(row.getKey().getPcId());
                            if (!ccyRestrictions.isEmpty() || !kwhRestrictions.isEmpty() || percentRestriction != null) {
                                calculateRestrictions(row.getValue(), ccyRestrictions, kwhRestrictions, percentRestriction, priceComponent, currencyHashMap, mainCurrencyId, altCurrencyId);
                            }
                        } catch (Exception e) {
                            log.error("Exception in billing run scale restriction calculation BgInvoiceSlotId: [%s], PcId: [%s] PodId: [%s]".formatted(key.getBgInvoiceSlotId(), key.getPcId(), key.getPodId()), e);
                            billingRunContractsRepository.updateBgInvoiceSlotStatus(key.getBgInvoiceSlotId(), "ERROR",
                                    "scale restriction calculation error, PC_ID: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(key.getPcId(), key.getPodId(), e.getMessage()));
                            failedSlots.add(key.getBgInvoiceSlotId());
                        }

                        //remove all ignored restricted data
                        row.getValue().removeIf(v -> v.getFinalRestrictionVolume() != null && v.getFinalRestrictionVolume().compareTo(BigDecimal.ZERO) == 0);

                        if (!priceComponent.getDiscount()) {
                            continue;
                        }

                        try {
                            if (failedSlots.contains(row.getKey().getBgInvoiceSlotId())) {
                                continue;
                            }

                            List<BillingRunInvoiceDetailBaseModel> discountFinalList = billingRunDiscountService.processDiscountScale(row, key, discountHashMap, currencyHashMap, mainCurrencyId);
                            if (discountFinalList.isEmpty()) continue;

                            row.getValue().addAll(discountFinalList);

                        } catch (Exception e) {
                            log.error("Exception in billing run scale discount calculation BgInvoiceSlotId: [%s], PcId: [%s] PodId: [%s]".formatted(key.getBgInvoiceSlotId(), key.getPcId(), key.getPodId()), e);
                            billingRunContractsRepository.updateBgInvoiceSlotStatus(key.getBgInvoiceSlotId(), "ERROR",
                                    "scale discount calculation error, PC_ID: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(key.getPcId(), key.getPodId(), e.getMessage()));
                            failedSlots.add(key.getBgInvoiceSlotId());
                        }

                    }
                }
            }

            if (perPieceDetails != null) {
                processedInvoiceDetailList.addAll(perPieceDetails.stream().filter(p -> !failedSlots.contains(p.getBgInvoiceSlotId())).toList());
            }
            if (overTimeOneTimeDetails != null) {
                processedInvoiceDetailList.addAll(overTimeOneTimeDetails.stream().filter(p -> !failedSlots.contains(p.getBgInvoiceSlotId())).toList());
            }
            if (overTimePeriodicalDetails != null) {
                processedInvoiceDetailList.addAll(overTimePeriodicalDetails.stream().filter(p -> !failedSlots.contains(p.getBgInvoiceSlotId())).toList());
            }
            if (groupedSettlements != null) {
                groupedSettlements.forEach((k, v) -> {
                    if (!failedSlots.contains(k.getBgInvoiceSlotId())) {
                        processedInvoiceDetailList.addAll(v);
                    }
                });
                groupedSettlements = null;
            }
            if (groupedScales != null) {
                groupedScales.forEach((k, v) -> {
                    if (!failedSlots.contains(k.getBgInvoiceSlotId())) {
                        processedInvoiceDetailList.addAll(v);
                    }
                });
            }

            if (overTimeElectricityDetails != null) {
                processedInvoiceDetailList.addAll(overTimeElectricityDetails.stream().filter(p -> !failedSlots.contains(p.getBgInvoiceSlotId())).toList());
            }


            Map<Long, List<BillingRunInvoiceDetailBaseModel>> groupByInvoiceSlot = processedInvoiceDetailList.stream()
                    .collect(Collectors.groupingBy(BillingRunInvoiceDetailBaseModel::getBgInvoiceSlotId));


            //Check Equal Monthly
            for (Map.Entry<Long, List<BillingRunInvoiceDetailBaseModel>> row : groupByInvoiceSlot.entrySet()) {
                Long slotId = row.getKey();
                try {
                    generateInvoice(
                            billingRunDate,
                            runContract,
                            failedSlots,
                            priceComponentHashMap,
                            mainCurrency,
                            globalVatRate,
                            mainCurrencyId,
                            altCurrencyId,
                            vatRateMap,
                            currencyHashMap,
                            row,
                            billingRunModel
                    );
                } catch (Exception e) {
                    log.error("calculation error, BgInvoiceSlotId: [%s]".formatted(slotId), e);

                    if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                        billingRunContractsRepository.updateBgInvoiceSlotStatus(slotId, "ERROR",
                                "calculation error, BgInvoiceSlotId: [%s] ExceptionMessage: [%s]".formatted(slotId, e.getMessage()));
                    } else {
                        billingRunContractsRepository.updateSvInvoiceSlotStatus(slotId, "ERROR",
                                "calculation, SvInvoiceSlotId: [%s]  ExceptionMessage: [%s]".formatted(slotId, e.getMessage()));
                    }
                    failedSlots.add(slotId);
                }
            }
        } catch (Exception e) {
            log.error("exception in main billing run", e);
            runContract.setErrorMessage(e.getMessage());
            runContract.setProcessingStatus("FAILED");
            billingRunContractsRepository.save(runContract);
        }

    }

    public void startInterimProcessing(BillingRun run, String schedulerId) {
        // call procedure for data preparation
        log.debug("Started interim processing {}", run.getId());
        BillingRunStandardProcessModel billingRunModel = billingRunRepository.findByIdForStandardProcess(run.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run with initial status not found by given id"));
        log.debug("Found billing run processing model!;");
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<BillingRunInterimData> billingRunInterimData = billingRunInterimDataRepository.findAllByRunIdAndIsValidForGenerationAndStatus(run.getId(), true, "CREATED");
        log.debug("Found billing run interim data!;");
        try {
            Optional<Currency> mainCurrencyOptional = currencyRepository.findMainCurrencyNowAndActive();
            if (mainCurrencyOptional.isEmpty()) {
                log.debug("Could not find main currency in system!;");
                throw new RuntimeException("cannot find main currency in system");
            }
            Currency mainCurrency = mainCurrencyOptional.get();
            log.debug("Starting finding vat rates!;");
            VatRate globalVatRate = vatRateRepository.findGlobalVatRate(LocalDate.now(), PageRequest.of(0, 1)).get();//TODO CHANGE LOCALDATE TO BILLING RUN DATE
            log.debug("Found vat rate {}!;", globalVatRate.getName());
            Long mainCurrencyId = mainCurrency.getId();
            Long altCurrencyId = mainCurrency.getAltCurrency().getId();
            HashMap<Long, VatRate> vatRateMap = new HashMap<>();
            HashMap<Long, Currency> currencyHashMap = new HashMap<>();
            LocalDate invoiceIssueDate = LocalDate.now();
            List<Callable<Void>> callables = new ArrayList<>();
            for (BillingRunInterimData interimData : billingRunInterimData) {
                Callable<Void> callable = () -> {
                    try {
                        MDC.put("schedulerId", schedulerId);
                        MDC.put("billingId", String.valueOf(interimData.getRunId()));
                        log.debug("Inside interim callable");
                        billingRunInterimProcessingService.process(interimData, mainCurrency, globalVatRate, vatRateMap, currencyHashMap, mainCurrencyId, altCurrencyId, invoiceIssueDate, billingRunModel);
                        interimData.setStatus("SUCCESS");
                        log.debug("Successfully processed interim data!;");
                    } catch (Exception e) {
                        log.error("Exception in interim processing, row id: {}", interimData.getId(), e);
                        interimData.setStatus("FAILED");
                        interimData.setErrorMessage(e.getMessage());
                    } finally {
                        transactionTemplate.executeWithoutResult(i -> billingRunInterimDataRepository.save(interimData));
                        log.debug("Finally interim block;");
                    }
                    return null;
                };
                callables.add(callable);
            }
            log.debug("Invoking interim callables!;");
            executor.invokeAll(callables);

            billingRunRepository.finalizeDataPreparation(run.getId());

            run.setInterimInvoiceGenerationStatus(BillingRunDataPreparationStatus.FINISHED);
            run.setStatus(BillingStatus.DRAFT);
            billingRunRepository.save(run);

            invoiceService.generateExcel(run.getId(), InvoiceStatus.DRAFT);
        } catch (InterruptedException e) {
            log.error("Error while invoking interim callables", e);
            throw new RuntimeException(e);
        } finally {
            log.debug("Finalizing process");
            finalizeProcess(executor, run);
        }
    }

    private void finalizeProcess(ExecutorService executor,
                                 BillingRun run) {
        try {
            log.debug("Shutting Interim  executor down!;");
            // Shutdown the executor
            executor.shutdown();

            boolean allTasksCompleted = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!allTasksCompleted) {
                log.debug("Some tasks did not complete successfully.");
            }

            if (ListUtils.emptyIfNull(run.getRunStages()).contains(RunStage.GENERATE_AND_SIGN)) {
                billingRunStartGenerationService.execute(run.getId(), false, false);
            }
        } catch (InterruptedException e) {
            log.debug("Thread interrupted while waiting for completion.");
        }
    }

    public List<BillingRunInvoiceDetailBaseModel> splitVolumeDates(List<BillingRunInvoiceDetailBaseModel> billingRunInvoiceDetailBaseModels) {
        if (billingRunInvoiceDetailBaseModels == null || billingRunInvoiceDetailBaseModels.isEmpty()) {
            return null;
        }
        List<ScaleSplitDate> allDates = new ArrayList<>();
        Integer index = 0;
        for (BillingRunInvoiceDetailBaseModel range : billingRunInvoiceDetailBaseModels) {

            if (allDates.stream().noneMatch(
                    item -> item.getDate().equals(range.getDateFrom())
                            && item.getIsStart())
            ) {
                allDates.add(new ScaleSplitDate(range.getDateFrom(), true));
            }

            if (allDates.stream().noneMatch(
                    item -> item.getDate().equals(range.getDateTo().toLocalDate())
                            && !item.getIsStart())
            ) {
                allDates.add(new ScaleSplitDate(range.getDateTo().toLocalDate(), false));
            }
            range.setSplitUniqueIndex(index);
            index++;
        }

        allDates.sort(
                Comparator.comparing(ScaleSplitDate::getDate)
                        .thenComparing(ScaleSplitDate::getIsStart)
        );

        List<VolumeDateRange> newRanges = new ArrayList<>();
        for (int i = 0; i < allDates.size() - 1; i++) {
            ScaleSplitDate splitCurrent = allDates.get(i);
            ScaleSplitDate splitNext = i == allDates.size() - 1 ? allDates.get(i) : allDates.get(i + 1);
            LocalDate fromDate;
            LocalDate toDate;

            if (splitCurrent.getIsStart()) {
                fromDate = splitCurrent.getDate();
            } else {
                fromDate = splitCurrent.getDate().plusDays(1);
            }

            if (splitNext.getIsStart()) {
                toDate = splitNext.getDate().minusDays(1);
            } else {
                toDate = splitNext.getDate();
            }
            if (fromDate.isBefore(toDate)) {
                newRanges.add(new VolumeDateRange(fromDate, toDate));
            }
        }

        //newRanges.add(new VolumeDateRange(allDates.get(allDates.size() - 1), allDates.get(allDates.size() - 1)));
        List<BillingRunInvoiceDetailBaseModel> splitRanges = new ArrayList<>();
        for (VolumeDateRange newRange : newRanges) {
            for (BillingRunInvoiceDetailBaseModel originalRange : billingRunInvoiceDetailBaseModels) {
                if (originalRange.getType().equals(InvoiceStandardDetailType.DISCOUNT_SCALE)) {
                    originalRange.setType(InvoiceStandardDetailType.DISCOUNT);
                }

                if (!(newRange.getTo().isBefore(originalRange.getDateFrom()) || newRange.getFrom().isAfter(originalRange.getDateTo().toLocalDate()))) {
                    LocalDate adjustedFrom = newRange.getFrom().isBefore(originalRange.getDateFrom()) ? originalRange.getDateFrom() : newRange.getFrom();
                    LocalDate adjustedTo = newRange.getTo().isAfter(originalRange.getDateTo().toLocalDate()) ? originalRange.getDateTo().toLocalDate() : newRange.getTo();
                    BillingRunInvoiceDetailBaseModel cloned = originalRange.clone();
                    long originalDateRangeDiff = ChronoUnit.DAYS.between(originalRange.getDateFrom(), originalRange.getDateTo()) + 1;
                    long newDateRangeDiff = ChronoUnit.DAYS.between(adjustedFrom, adjustedTo) + 1;
                    if (originalDateRangeDiff == newDateRangeDiff) {
                        splitRanges.add(cloned);
                        continue;
                    }

                    if (originalRange.getType().equals(InvoiceStandardDetailType.DISCOUNT)) {
                        cloned.setValue(originalRange.getInvoiceTotalConsumption());
                    } else if (originalRange.getFinalRestrictionAmount() != null) {
                        cloned.setValue(originalRange.getFinalRestrictionVolume());
                    } else {
                        cloned.setValue(originalRange.getCalculatedVolumes());
                    }

                    cloned.setBeforeSplitDayCount(originalDateRangeDiff);
                    cloned.setDateFrom(adjustedFrom);
                    cloned.setDateTo(adjustedTo.atStartOfDay());
                    splitRanges.add(cloned);
                }
            }
        }
        Map<Integer, List<BillingRunInvoiceDetailBaseModel>> groupedByIndex = splitRanges.stream()
                .collect(Collectors.groupingBy(BillingRunInvoiceDetailBaseModel::getSplitUniqueIndex));
        for (Map.Entry<Integer, List<BillingRunInvoiceDetailBaseModel>> row : groupedByIndex.entrySet()) {
            if (row.getValue().size() > 1) {

                Collections.sort(row.getValue(), Comparator.comparing(BillingRunInvoiceDetailBaseModel::getDateFrom));

                if (row.getValue().get(0).getType().equals(InvoiceStandardDetailType.DISCOUNT)) {
                    long totalDays = row.getValue().get(0).getBeforeSplitDayCount();
                    BigDecimal originalVolume = row.getValue().get(0).getValue();

                    for (int i = 0; i < row.getValue().size(); i++) {
                        BillingRunInvoiceDetailBaseModel split = row.getValue().get(i);
                        long daysBetween = ChronoUnit.DAYS.between(split.getDateFrom(), split.getDateTo()) + 1;

                        BigDecimal value = originalVolume
                                .divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(daysBetween))
                                .setScale(12, RoundingMode.HALF_UP);
                        split.setValueBefore9DigitRounding(value.setScale(9, RoundingMode.HALF_UP));
                        split.setValue(value.setScale(8, RoundingMode.HALF_UP));
                    }

                    billingRunProcessScaleCalculationService.roundAndAdjustValues(
                            originalVolume
                            , row.getValue(), 1);

                    for (int i = 0; i < row.getValue().size(); i++) {
                        BillingRunInvoiceDetailBaseModel split = row.getValue().get(i);
                        split.setDiscountAmount(split.getValue().multiply(split.getDiscountPerKWH()));
                        split.setInvoiceTotalConsumption(split.getValue());
                    }

                    continue;
                }


                Boolean calcIsOldMeterReadingSplit = row.getValue().get(0).getCalcIsOldMeterReadingSplit();
                Boolean calcIsMeterReadingNewSplit = row.getValue().get(0).getCalcIsMeterReadingNewSplit();
                Boolean calcIsCorrectionSplit = row.getValue().get(0).getCalcIsCorrectionSplit();
                Boolean calcIsDeductSplit = row.getValue().get(0).getCalcIsDeductSplit();
                Boolean calcIsDifferenceSplit = row.getValue().get(0).getCalcIsDifferenceSplit();
                BigDecimal calcOldMeterReading = row.getValue().get(0).getOldMeterReading();
                BigDecimal calcNewMeterReadings = row.getValue().get(0).getNewMeterReading();
                BigDecimal calcDifference = null;
                BigDecimal calcCorrection = row.getValue().get(0).getCorrection();
                BigDecimal calcDeduct = row.getValue().get(0).getDeducted();
                BigDecimal multiplier = row.getValue().get(0).getMultiplier();

                long totalDays = row.getValue().get(0).getBeforeSplitDayCount();
                BigDecimal originalVolume = row.getValue().get(0).getValue();
                BigDecimal restrictedVolumes = row.getValue().get(0).getFinalRestrictionVolume();
                BigDecimal originalDeduction = row.getValue().get(0).getDeducted();
                BigDecimal originalCorrection = row.getValue().get(0).getCorrection();
                Boolean discounted = row.getValue().get(0).getDiscounted() != null && row.getValue().get(0).getDiscounted();
                Boolean hasCorrection = row.getValue().get(0).getHasCorrection();
                Boolean isTariff = row.getValue().get(0).isTariff();


                for (int i = 0; i < row.getValue().size(); i++) {
                    BillingRunInvoiceDetailBaseModel split = row.getValue().get(i);
                    long daysBetween = ChronoUnit.DAYS.between(split.getDateFrom(), split.getDateTo()) + 1;

                    if (!split.isTariff() && split.getFinalRestrictionVolume() == null && !discounted) {

                        BigDecimal value = originalVolume.divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(daysBetween))
                                .setScale(12, RoundingMode.HALF_UP);

                        split.setValueBefore9DigitRounding(value.setScale(9, RoundingMode.HALF_UP));
                        split.setValue(value.setScale(8, RoundingMode.HALF_UP));

                        if (hasCorrection == null || !hasCorrection) {
                            if (originalDeduction != null) {
                                BigDecimal deduction = originalDeduction.divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(daysBetween))
                                        .setScale(12, RoundingMode.HALF_UP);

                                split.setValueDeductionBefore9DigitRounding(deduction.setScale(9, RoundingMode.HALF_UP));
                                split.setValueDeduction(deduction.setScale(8, RoundingMode.HALF_UP));
                                calcIsDeductSplit = true;
                            }

                            if (originalCorrection != null) {
                                BigDecimal correction = originalCorrection.divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(daysBetween))
                                        .setScale(12, RoundingMode.HALF_UP);

                                split.setValueCorrectionBefore9DigitRounding(correction.setScale(9, RoundingMode.HALF_UP));
                                split.setValueCorrection(correction.setScale(8, RoundingMode.HALF_UP));
                                calcIsCorrectionSplit = true;
                            }
                        }
                    } else {
                        BigDecimal value = originalVolume
                                .divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(daysBetween))
                                .setScale(12, RoundingMode.HALF_UP);

                        split.setValueBefore9DigitRounding(value.setScale(9, RoundingMode.HALF_UP));
                        split.setValue(value.setScale(8, RoundingMode.HALF_UP));

                        if (split.getFinalRestrictionVolume() == null) {
                            split.setOldMeterReading(null);
                            split.setNewMeterReading(null);
                            split.setCorrection(null);
                            split.setDeducted(null);
                            split.setCalcIsOldMeterReadingSplit(null);
                            split.setCalcIsMeterReadingNewSplit(null);
                            split.setCalcIsCorrectionSplit(null);
                            split.setCalcIsDeductSplit(null);
                            split.setCalcIsDifferenceSplit(null);
                            split.setMultiplier(null);
                            split.setDifference(null);
                        }
                    }

                    split.setOldMeterReading(calcOldMeterReading);
                    split.setNewMeterReading(calcNewMeterReadings);
                    split.setCorrection(calcCorrection);
                    split.setDeducted(calcDeduct);
                    split.setCalcIsOldMeterReadingSplit(calcIsOldMeterReadingSplit);
                    split.setCalcIsMeterReadingNewSplit(calcIsMeterReadingNewSplit);
                    split.setCalcIsCorrectionSplit(calcIsCorrectionSplit);
                    split.setCalcIsDeductSplit(calcIsDeductSplit);
                    split.setCalcIsDifferenceSplit(calcIsDifferenceSplit);
                    split.setDifference(calcDifference);
                }


                billingRunProcessScaleCalculationService.roundAndAdjustValues(
                        originalVolume
                        , row.getValue(), 1);

                if (originalDeduction != null && restrictedVolumes == null && !discounted) {
                    billingRunProcessScaleCalculationService.roundAndAdjustValues(
                            originalDeduction
                            , row.getValue(), 2);
                }
                if (originalCorrection != null && restrictedVolumes == null && !discounted) {
                    billingRunProcessScaleCalculationService.roundAndAdjustValues(
                            originalCorrection
                            , row.getValue(), 3);
                }


                for (int i = 0; i < row.getValue().size(); i++) {
                    BillingRunInvoiceDetailBaseModel f = row.getValue().get(i);
                    if (restrictedVolumes == null && !discounted) {
                        f.setCalculatedVolumes(f.getValue());
                        f.setCalculatedPrice(f.getValue().multiply(f.getKwhPrice())
                                .setScale(12, RoundingMode.HALF_UP));
                        f.setDeducted(f.getValueDeduction());
                        f.setCorrection(f.getValueCorrection());

                        if (!isTariff) {
                            f.setCorrection(f.getValueCorrection());
                            f.setDeducted(f.getValueDeduction());
                            f.setDifference((f.getValue()
                                    .subtract(f.getValueCorrection() == null ? BigDecimal.ZERO : f.getValueCorrection())
                                    .add(f.getValueDeduction() == null ? BigDecimal.ZERO : f.getValueDeduction()))
                                    .divide(multiplier, 8, RoundingMode.HALF_UP));
                            f.setCalcIsDifferenceSplit(true);
                            if (i == 0) {
                                f.setOldMeterReading(calcOldMeterReading);
                            } else {
                                f.setOldMeterReading(row.getValue().get(i - 1).getNewMeterReading());
                                f.setCalcIsOldMeterReadingSplit(true);
                            }
                            f.setNewMeterReading(f.getOldMeterReading().add(f.getDifference()).setScale(8, RoundingMode.HALF_UP));
                            f.setCalcIsMeterReadingNewSplit(true);
                        }
                    } else {
                        f.setFinalRestrictionVolume(f.getValue());
                        f.setFinalRestrictionAmount(f.getValue().multiply(f.getKwhPrice()).setScale(12, RoundingMode.HALF_UP));
                    }
                }
            }
        }
        return splitRanges;
    }

    public void generateInvoice(LocalDateTime billingRunDate, BillingRunContracts runContract, HashSet<Long> failedSlots,
                                HashMap<Long, PriceComponent> priceComponentHashMap, Currency mainCurrency,
                                VatRate globalVatRate, Long mainCurrencyId, Long altCurrencyId,
                                HashMap<Long, VatRate> vatRateMap, HashMap<Long, Currency> currencyHashMap,
                                Map.Entry<Long, List<BillingRunInvoiceDetailBaseModel>> row,
                                BillingRun billingRunModel) {
        Long slotId = row.getKey();


        List<BillingRunInvoiceDetailBaseModel> volumesData = row.getValue()
                .stream().filter(r -> r.getType().equals(InvoiceStandardDetailType.SCALE)
                        || r.getType().equals(InvoiceStandardDetailType.DISCOUNT_SCALE)
                ).toList();
        row.getValue().removeAll(volumesData);

        Map<Long, List<BillingRunInvoiceDetailBaseModel>> podGroup = volumesData.stream()
                .collect(Collectors.groupingBy(BillingRunInvoiceDetailBaseModel::getPodId));

        for (Map.Entry<Long, List<BillingRunInvoiceDetailBaseModel>> pod : podGroup.entrySet()) {
            List<BillingRunInvoiceDetailBaseModel> splitVolumes = splitVolumeDates(pod.getValue());
            row.getValue().addAll(splitVolumes);
        }


        Invoice invoice = new Invoice();
        invoice.setContractType(runContract.getContractType());
        invoice.setProcessorRowId(slotId);
        invoice.setInvoiceDate(billingRunModel.getInvoiceDate());
        invoice.setTaxEventDate(billingRunModel.getTaxEventDate());
        invoice.setBasisForIssuing("Електрическа енергия за периода");
        invoice.setAccountPeriodId(billingRunModel.getAccountingPeriodId());
        invoice.setProductContractId(runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT) ? runContract.getContractId() : null);
        invoice.setServiceContractId(runContract.getContractType().equals(ContractType.SERVICE_CONTRACT) ? runContract.getContractId() : null);
        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceType(InvoiceType.STANDARD);
        invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
        //List<Invoices> installments = new ArrayList<>();

        BigDecimal totalMainCurrencyWithoutVat = BigDecimal.ZERO;
        BigDecimal totalMainCurrencyWithVat = BigDecimal.ZERO;

        BigDecimal totalAltCurrencyWithoutVat = BigDecimal.ZERO;
        BigDecimal totalAltCurrencyWithVat = BigDecimal.ZERO;

        //Installment installment = null;

        Long contractBillingGroupId = null;
        Long podId = null;
        String invoiceSlot = null;
        Long latestContractDetailId = null;
        Long customerId = null;
        boolean isDeducted = false;

        Invoice baseCorrectionInvoice = null;
        Long correctionRunId = null;
        if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            invoice.setInvoiceType(InvoiceType.CORRECTION);
        }


        if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
            BillingRunBgInvoiceSlots slot = billingRunBgInvoiceSlotsRepository.findByBgInvoiceSlotIdAndRunIdAndContractId(slotId, runContract.getRunId(), runContract.getContractId());
            if (slot.getPodId() != null && (slot.getPodId() != 0)) {
                podId = slot.getPodId();
            }
            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
                correctionRunId = slot.getCorrectionRunId();
                Optional<BillingRunCorrectionInvoiceBase> base = billingRunCorrectionInvoiceBaseRepository.findById(slot.getCorrectionRunId());
                baseCorrectionInvoice = invoiceRepository.findById(base.get()
                        .getInvoiceId()).get();
            }

            customerId = slot.getCustomerId();
            latestContractDetailId = slot.getLatestContractDetailId();
            contractBillingGroupId = slot.getContractBillingGroupId();
            invoiceSlot = slot.getInvoiceSlot();
            invoice.setCustomerId(slot.getCustomerId());
            invoice.setProductId(slot.getProductId());
            invoice.setProductContractDetailId(slot.getLatestContractDetailId());
            invoice.setContractBillingGroupId(contractBillingGroupId);
            invoice.setPodId(podId);
            invoice.setCustomerDetailId(slot.getLatestCustomerDetailId());
            invoice.setProductDetailId(slot.getLatestProductDetailId());
            invoice.setNoInterestOnOverdueDebts(slot.getNoInterestOnOverdueDebt());
            invoice.setAlternativeRecipientCustomerDetailId(slot.getReceiptOfAnInvoiceNumber());
            invoice.setCustomerCommunicationId(slot.getCustomerCommunicationId());
            invoice.setContractCommunicationId(slot.getContractCommunicationId());
            invoice.setCostCenterControllingOrder(slot.getCostCenterControllingOrder());
            invoice.setDirectDebit(slot.getDirectDebit());
            invoice.setIncomeAccountNumber(slot.getNumberOfIncomeAccount());
            invoice.setIban(slot.getBankAccount());
            invoice.setBankId(slot.getBankId());
            invoice.setInterestRateId(slot.getInterestRateId());
            Long templateDetailId = contractTemplateRepository
                    .findTemplateDetailForBilling(EntityType.PRODUCT.name(), slot.getLatestProductDetailId(), runContract.getRunId())
                    .orElse(null);

            invoice.setTemplateDetailId(templateDetailId);
            if (billingRunModel.getInvoiceDueDateType().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                BillingInvoicePaymentTerm term = invoicePaymentTermsRepository.findByProductContractDetailId(slot.getLatestContractDetailId());
                if (term != null) {
                    LocalDate paymentTermEndDate = billingPaymentTermDayCalculationService.calculateDeadline(term, billingRunModel.getInvoiceDate(), term.getValue());
                    invoice.setPaymentDeadline(paymentTermEndDate);
                }
            } else {
                invoice.setPaymentDeadline(billingRunModel.getInvoiceDueDate());
            }
        } else {
            BillingRunSvInvoiceSlots slot = billingRunSvInvoiceSlotsRepository.findBySvInvoiceSlotIdAndRunIdAndContractId(slotId, runContract.getRunId(), runContract.getContractId());
            if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
                correctionRunId = slot.getCorrectionRunId();
                Optional<BillingRunCorrectionInvoiceBase> base = billingRunCorrectionInvoiceBaseRepository.findById(slot.getCorrectionRunId());
                baseCorrectionInvoice = invoiceRepository.findById(base.get()
                        .getInvoiceId()).get();
            }

            invoiceSlot = slot.getInvoiceSlot();
            customerId = slot.getCustomerId();
            invoice.setCustomerId(slot.getCustomerId());
            invoice.setServiceId(slot.getProductId());
            invoice.setServiceContractDetailId(slot.getLatestContractDetailId());
            latestContractDetailId = slot.getLatestContractDetailId();
            invoice.setCustomerDetailId(slot.getLatestCustomerDetailId());
            invoice.setServiceDetailId(slot.getLatestProductDetailId());
            invoice.setNoInterestOnOverdueDebts(slot.getNoInterestOnOverdueDebt());
            invoice.setAlternativeRecipientCustomerDetailId(slot.getReceiptOfAnInvoiceNumber());
            invoice.setCustomerCommunicationId(slot.getCustomerCommunicationId());
            invoice.setContractCommunicationId(slot.getContractCommunicationId());
            invoice.setCostCenterControllingOrder(slot.getCostCenterControllingOrder());
            invoice.setDirectDebit(slot.getDirectDebit());
            invoice.setIncomeAccountNumber(slot.getNumberOfIncomeAccount());
            invoice.setInterestRateId(slot.getInterestRateId());
            invoice.setBankId(slot.getBankId());
            invoice.setIban(slot.getBankAccount());
            Long templateDetailId = contractTemplateRepository
                    .findTemplateDetailForBilling(EntityType.SERVICE.name(), slot.getLatestProductDetailId(), runContract.getRunId())
                    .orElse(null);

            invoice.setTemplateDetailId(templateDetailId);
            if (billingRunModel.getInvoiceDueDateType().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                BillingInvoicePaymentTerm term = invoicePaymentTermsRepository.findByServiceContractDetailId(slot.getLatestContractDetailId());
                if (term != null) {
                    LocalDate paymentTermEndDate = billingPaymentTermDayCalculationService.calculateDeadline(term, billingRunModel.getInvoiceDate(), term.getValue());
                    invoice.setPaymentDeadline(paymentTermEndDate);
                }
            } else {
                invoice.setPaymentDeadline(billingRunModel.getInvoiceDueDate());
            }
        }


        List<InvoiceStandardDetailedData> invoiceDetails = new ArrayList<>();
        List<InvoiceStandardDetailedDataVatBase> standardVatBaseDetails = new ArrayList<>();

        Map<Long, List<Long>> pcProductDetails = row.getValue().stream()
                .filter(d -> Objects.nonNull(d.getProductDetailId()))
                .collect(Collectors.groupingBy(
                        BillingRunInvoiceDetailBaseModel::getPcId,
                        Collectors.mapping(BillingRunInvoiceDetailBaseModel::getProductDetailId, Collectors.toList())
                ));

        Map<Long, List<Long>> pcServiceDetails = row.getValue().stream()
                .filter(d -> Objects.nonNull(d.getServiceDetailId()))
                .collect(Collectors.groupingBy(
                        BillingRunInvoiceDetailBaseModel::getPcId,
                        Collectors.mapping(BillingRunInvoiceDetailBaseModel::getServiceDetailId, Collectors.toList())
                ));

        Map<Pair<Long, Long>, CostCenterAndIncomeAccountResponse> pdCostAndIncomeMap = new HashMap<>();
        Map<Pair<Long, Long>, CostCenterAndIncomeAccountResponse> sdCostAndIncomeMap = new HashMap<>();

        addDetailCostCenterAndIncomeAcc(pcProductDetails.entrySet(), pdCostAndIncomeMap, false);
        addDetailCostCenterAndIncomeAcc(pcServiceDetails.entrySet(), sdCostAndIncomeMap, true);

        for (BillingRunInvoiceDetailBaseModel model : row.getValue()) {

            PriceComponent priceComponent = priceComponentHashMap.getOrDefault(model.getPcId(), null);
            if (priceComponent == null) {
                priceComponent = priceComponentRepository.findById(model.getPcId()).get();
                priceComponentHashMap.put(model.getPcId(), priceComponent);
            }
            VatRate vatRate;
            if (priceComponent.getGlobalVatRate()) {
                vatRate = globalVatRate;
            } else {
                vatRate = vatRateMap.get(priceComponent.getVatRateId());
                if (vatRate == null) {
                    vatRate = vatRateRepository.findById(priceComponent.getVatRateId()).get();
                    vatRateMap.put(vatRate.getId(), vatRate);
                }
            }
            BigDecimal price;
            if (model.getType().equals(InvoiceStandardDetailType.PER_PIECE) || model.getType().equals(InvoiceStandardDetailType.OVER_TIME_ONE_TIME)
                    || model.getType().equals(InvoiceStandardDetailType.OVER_TIME_PERIODICAL) || model.getType().equals(InvoiceStandardDetailType.WITH_ELECTRICITY)) {
                price = model.getFinalPrice();
            } else if (model.getType().equals(InvoiceStandardDetailType.DISCOUNT)) {
                price = model.getDiscountAmount().negate();
            } else {
                if (model.getFinalRestrictionAmount() != null) {
                    price = model.getFinalRestrictionAmount();
                    model.setInvoiceTotalConsumption(model.getFinalRestrictionVolume());
                } else {
                    price = model.getCalculatedPrice();
                    model.setInvoiceTotalConsumption(model.getCalculatedVolumes());
                }
            }

            //MAIN CURRENCY
            model.setInvoiceMainCurrencyId(mainCurrencyId);
            model.setInvoiceMainCurrencyAmountWithoutVat(price.setScale(12, RoundingMode.HALF_UP));
            model.setInvoiceMainCurrencyVatAmount(
                    model.getInvoiceMainCurrencyAmountWithoutVat()
                            .multiply(vatRate.getValueInPercent())
                            .setScale(12, RoundingMode.HALF_UP)
                            .divide(BigDecimal.valueOf(100L), 12, RoundingMode.HALF_UP));

            model.setInvoiceMainCurrencyAmountWithVat(model.getInvoiceMainCurrencyVatAmount()
                    .add(model.getInvoiceMainCurrencyAmountWithoutVat())
                    .setScale(12, RoundingMode.HALF_UP));

            model.setInvoiceVatId(vatRate.getId());
            model.setInvoiceVatPercent(vatRate.getValueInPercent());

            //ORIGINAL CURRENCY
            model.setInvoiceOriginalCurrencyId(priceComponent.getCurrency().getId());
            model.setInvoiceOriginalCurrencyVatAmount(billingRunCurrencyService.convertToCurrency(mainCurrencyId, priceComponent.getCurrency().getId(), model.getInvoiceMainCurrencyVatAmount(), currencyHashMap));
            model.setInvoiceOriginalCurrencyAmountWithoutVat(billingRunCurrencyService.convertToCurrency(mainCurrencyId, priceComponent.getCurrency().getId(), model.getInvoiceMainCurrencyAmountWithoutVat(), currencyHashMap));
            model.setInvoiceOriginalCurrencyAmountWithVat(billingRunCurrencyService.convertToCurrency(mainCurrencyId, priceComponent.getCurrency().getId(), model.getInvoiceMainCurrencyAmountWithVat(), currencyHashMap));

            //ALTERNATIVE CURRENCY
            model.setInvoiceAltCurrencyId(altCurrencyId);
            model.setInvoiceAltCurrencyVatAmount(billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, model.getInvoiceMainCurrencyVatAmount(), currencyHashMap));
            model.setInvoiceAltCurrencyAmountWithoutVat(billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, model.getInvoiceMainCurrencyAmountWithoutVat(), currencyHashMap));
            model.setInvoiceAltCurrencyAmountWithVat(billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, model.getInvoiceMainCurrencyAmountWithVat(), currencyHashMap));

            if (priceComponent.getDoNotIncludeInTheVatBase() == null || !priceComponent.getDoNotIncludeInTheVatBase()) {
                totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.add(model.getInvoiceMainCurrencyAmountWithoutVat());
                totalMainCurrencyWithVat = totalMainCurrencyWithVat.add(model.getInvoiceMainCurrencyAmountWithVat());

                totalAltCurrencyWithoutVat = totalAltCurrencyWithoutVat.add(model.getInvoiceAltCurrencyAmountWithoutVat());
                totalAltCurrencyWithVat = totalAltCurrencyWithVat.add(model.getInvoiceAltCurrencyAmountWithVat());
                if (model.getType() == InvoiceStandardDetailType.SCALE || model.getType() == InvoiceStandardDetailType.SETTLEMENT) {
                    model.setCalculatedVolumes(model.getCalculatedVolumes());
                    model.setKwhPrice(model.getKwhPrice());
                    model.setMeasureUnitForUnitPrice(priceComponent.getValueType().getId().intValue());
                }
                if (model.getType() == InvoiceStandardDetailType.PER_PIECE) {
                    model.setMeasureUnitForUnitPrice(priceComponent.getValueType().getId().intValue());
                }

                String costCenterControllingOrder = priceComponent.getCostCenterControllingOrder();
                String incomeAccountNumber = priceComponent.getIncomeAccountNumber();

                if (Objects.isNull(costCenterControllingOrder)) {
                    if (Objects.nonNull(model.getServiceDetailId())) {
                        CostCenterAndIncomeAccountResponse costCenterAndIncomeAccountResponse = sdCostAndIncomeMap.get(Pair.of(priceComponent.getId(), model.getServiceDetailId()));
                        if (costCenterAndIncomeAccountResponse != null) {
                            costCenterControllingOrder = costCenterAndIncomeAccountResponse.getCostCenter();
                        }
                    } else if (Objects.nonNull(model.getProductDetailId())) {
                        CostCenterAndIncomeAccountResponse costCenterAndIncomeAccountResponse = pdCostAndIncomeMap.get(Pair.of(priceComponent.getId(), model.getProductDetailId()));
                        if (costCenterAndIncomeAccountResponse != null) {
                            costCenterControllingOrder = costCenterAndIncomeAccountResponse.getCostCenter();
                        }
                    }
                }

                if (Objects.isNull(incomeAccountNumber)) {
                    if (Objects.nonNull(model.getServiceDetailId())) {
                        CostCenterAndIncomeAccountResponse costCenterAndIncomeAccountResponse = sdCostAndIncomeMap.get(Pair.of(model.getPcId(), model.getServiceDetailId()));
                        if (costCenterAndIncomeAccountResponse != null) {
                            incomeAccountNumber = costCenterAndIncomeAccountResponse.getIncomeAccount();
                        }
                    } else if (Objects.nonNull(model.getProductDetailId())) {
                        CostCenterAndIncomeAccountResponse costCenterAndIncomeAccountResponse = pdCostAndIncomeMap.get(Pair.of(model.getPcId(), model.getProductDetailId()));
                        if (costCenterAndIncomeAccountResponse != null) {
                            incomeAccountNumber = costCenterAndIncomeAccountResponse.getIncomeAccount();
                        }
                    }
                }

                model.setCostCenterControllingOrder(costCenterControllingOrder);
                model.setIncomeAccountNumber(incomeAccountNumber);


                invoiceDetails.add(new InvoiceStandardDetailedData(model, invoice, runContract.getContractType()));
            } else {
                standardVatBaseDetails.add(new InvoiceStandardDetailedDataVatBase(model, invoice, runContract.getContractType(), priceComponent.getAlternativeRecipientCustomerDetailId()));
            }
        }


        //meter readings
        Optional<LocalDate> meterReadingPeriodTo = row.getValue().stream()
                .filter(f -> f.getDateTo() != null)
                .map(f -> f.getDateTo().toLocalDate())
                .max(LocalDate::compareTo);

        Optional<LocalDate> meterReadingPeriodFrom = row.getValue().stream()
                .filter(f -> f.getDateFrom() != null)
                .map(Value::getDateFrom)
                .min(LocalDate::compareTo);

        invoice.setMeterReadingPeriodFrom(meterReadingPeriodFrom.orElse(null));
        invoice.setMeterReadingPeriodTo(meterReadingPeriodTo.orElse(null));

        List<Invoice> samePeriodInterim = new ArrayList<>();
        List<Invoice> longPaymentTerInterim = new ArrayList<>();

        if (!billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            LocalDate interimSamePeriodCheckDate = billingRunDate.toLocalDate();
            BillingInvoicePaymentTerm term;
            if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                Optional<LocalDate> maxDate = invoiceDetails.stream()
                        .filter(f -> f.getDateTo() != null)
                        .map(InvoiceStandardDetailedData::getDateTo)
                        .max(LocalDate::compareTo);
                if (maxDate.isPresent()) {
                    interimSamePeriodCheckDate = maxDate.get();
                }
                term = invoicePaymentTermsRepository.findByProductContractDetailId(latestContractDetailId);
            } else {
                term = invoicePaymentTermsRepository.findByServiceContractDetailId(latestContractDetailId);
            }
            Invoice invoice1 = invoiceRepository.save(invoice);
            samePeriodInterim = invoiceRepository.getAllNotDeductedInterimSamePeriod(runContract.getContractId(), contractBillingGroupId, podId, runContract.getContractType().name(), invoiceSlot, interimSamePeriodCheckDate.withDayOfMonth(1), customerId,billingRunModel.getId());
            for (Invoice i : samePeriodInterim) {
                isDeducted = true;
                deductionInterimInvoiceRepository.save(new DeductionInterimInvoice(billingRunModel.getId(), invoice1.getId(), i.getId()));
                List<InvoiceStandardDetailedData> oldStandard = invoiceStandardDetailedDataRepository.findAllByInvoiceId(i.getId());
                for (InvoiceStandardDetailedData osi : oldStandard) {
                    InvoiceStandardDetailedData si = new InvoiceStandardDetailedData();
                    si.setInterimDeductionInvoiceId(i.getId());
                    si.setInvoice(invoice);
                    si.setCostCenterControllingOrder(osi.getCostCenterControllingOrder());
                    si.setIncomeAccountNumber(osi.getIncomeAccountNumber());
                    si.setInterimId(osi.getInterimId());
                    si.setVatRatePercent(osi.getVatRatePercent());
                    si.setDetailType(InvoiceStandardDetailType.INTERIM_DEDUCTION);


                    si.setOriginalCurrencyId(osi.getMainCurrencyId());
                    si.setOriginalCurrencyTotalAmountVat(osi.getMainCurrencyTotalAmountVat().negate());
                    si.setOriginalCurrencyTotalAmountWithVat(osi.getMainCurrencyTotalAmountWithVat().negate());
                    si.setOriginalCurrencyTotalAmountWithoutVat(osi.getMainCurrencyTotalAmountWithoutVat().negate());

                    si.setMainCurrencyId(mainCurrencyId);
                    si.setMainCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountVat(), currencyHashMap).negate());
                    si.setMainCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountWithVat(), currencyHashMap).negate());
                    si.setMainCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountWithoutVat(), currencyHashMap).negate());

                    si.setAltCurrencyId(altCurrencyId);
                    si.setAltCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountVat(), currencyHashMap).negate());
                    si.setAltCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountWithVat(), currencyHashMap).negate());
                    si.setAltCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountWithoutVat(), currencyHashMap).negate());

                    totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.subtract(si.getMainCurrencyTotalAmountWithoutVat());
                    totalMainCurrencyWithVat = totalMainCurrencyWithVat.subtract(si.getMainCurrencyTotalAmountWithVat());

                    totalAltCurrencyWithoutVat = totalAltCurrencyWithoutVat.subtract(si.getAltCurrencyTotalAmountWithoutVat());
                    totalAltCurrencyWithVat = totalAltCurrencyWithVat.subtract(si.getAltCurrencyTotalAmountWithVat());
                    invoiceDetails.add(si);
                }
            }
            if (term != null) {
                longPaymentTerInterim = invoiceRepository.getAllNotDeductedInterimLongPaymentTerm(runContract.getContractId(), contractBillingGroupId, podId, runContract.getContractType().name(), invoiceSlot, invoice.getPaymentDeadline(), customerId,billingRunModel.getId());

                for (Invoice i : longPaymentTerInterim) {
                    isDeducted = true;
                    deductionInterimInvoiceRepository.save(new DeductionInterimInvoice(billingRunModel.getId(), invoice1.getId(), i.getId()));
                    List<InvoiceStandardDetailedData> oldStandard = invoiceStandardDetailedDataRepository.findAllByInvoiceId(i.getId());
                    for (InvoiceStandardDetailedData osi : oldStandard) {
                        InvoiceStandardDetailedData si = new InvoiceStandardDetailedData();
                        si.setInterimDeductionInvoiceId(i.getId());
                        si.setInvoice(invoice);
                        si.setInterimId(osi.getInterimId());
                        si.setCostCenterControllingOrder(osi.getCostCenterControllingOrder());
                        si.setIncomeAccountNumber(osi.getIncomeAccountNumber());
                        si.setVatRatePercent(osi.getVatRatePercent());
                        si.setDetailType(InvoiceStandardDetailType.INTERIM_DEDUCTION);

                        si.setOriginalCurrencyId(osi.getMainCurrencyId());
                        si.setOriginalCurrencyTotalAmountVat(osi.getMainCurrencyTotalAmountVat().negate());
                        si.setOriginalCurrencyTotalAmountWithVat(osi.getMainCurrencyTotalAmountWithVat().negate());
                        si.setOriginalCurrencyTotalAmountWithoutVat(osi.getMainCurrencyTotalAmountWithoutVat().negate());

                        si.setMainCurrencyId(mainCurrencyId);
                        si.setMainCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountVat(), currencyHashMap).negate());
                        si.setMainCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountWithVat(), currencyHashMap).negate());
                        si.setMainCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), mainCurrencyId, osi.getMainCurrencyTotalAmountWithoutVat(), currencyHashMap).negate());

                        si.setAltCurrencyId(altCurrencyId);
                        si.setAltCurrencyTotalAmountVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountVat(), currencyHashMap).negate());
                        si.setAltCurrencyTotalAmountWithVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountWithVat(), currencyHashMap).negate());
                        si.setAltCurrencyTotalAmountWithoutVat(billingRunCurrencyService.convertToCurrency(si.getOriginalCurrencyId(), altCurrencyId, osi.getMainCurrencyTotalAmountWithoutVat(), currencyHashMap).negate());

                        totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.subtract(si.getMainCurrencyTotalAmountWithoutVat());
                        totalMainCurrencyWithVat = totalMainCurrencyWithVat.subtract(si.getMainCurrencyTotalAmountWithVat());

                        totalAltCurrencyWithoutVat = totalAltCurrencyWithoutVat.subtract(si.getAltCurrencyTotalAmountWithoutVat());
                        totalAltCurrencyWithVat = totalAltCurrencyWithVat.subtract(si.getAltCurrencyTotalAmountWithVat());
                        invoiceDetails.add(si);
                    }
                }
            }
        }

        AtomicReference<Boolean> priceWasChanged = new AtomicReference<>(invoiceDetails.stream().anyMatch(i -> i.getPriceComponentPriceTypeId() != null && i.getPriceComponentPriceTypeId() == 103
                && i.getMainCurrencyTotalAmountWithoutVat().compareTo(BigDecimal.ZERO) < 0));

        BigDecimal totalAmountWithoutDeduction = invoiceDetails.stream().filter(i -> !i.getDetailType().equals(InvoiceStandardDetailType.INTERIM_DEDUCTION)
                ).map(i -> i.getMainCurrencyTotalAmountWithoutVat() != null ? i.getMainCurrencyTotalAmountWithoutVat() : BigDecimal.ZERO)
                .reduce(BigDecimal::add).get();

        if (totalAmountWithoutDeduction.compareTo(BigDecimal.ZERO) < 0) {
            invoiceDetails.stream().filter(i -> i.getPriceComponentPriceTypeId() != null && i.getPriceComponentPriceTypeId() == 103 && i.getMainCurrencyTotalAmountWithoutVat().compareTo(BigDecimal.ZERO) < 0)
                    .forEach(d -> {
                        d.setMainCurrencyTotalAmountWithoutVat(BigDecimal.ZERO);
                        d.setMainCurrencyTotalAmountWithVat(BigDecimal.ZERO);
                        d.setMainCurrencyTotalAmountVat(BigDecimal.ZERO);
                        d.setOriginalCurrencyTotalAmountVat(BigDecimal.ZERO);
                        d.setOriginalCurrencyTotalAmountWithVat(BigDecimal.ZERO);
                        d.setOriginalCurrencyTotalAmountWithoutVat(BigDecimal.ZERO);
                        d.setAltCurrencyTotalAmountWithoutVat(BigDecimal.ZERO);
                        d.setAltCurrencyTotalAmountWithVat(BigDecimal.ZERO);
                        d.setAltCurrencyTotalAmountVat(BigDecimal.ZERO);
                        d.setUnitPrice(BigDecimal.ZERO);
                        priceWasChanged.set(true);
                    });
            totalMainCurrencyWithoutVat = BigDecimal.ZERO;
            totalMainCurrencyWithVat = BigDecimal.ZERO;
            totalAltCurrencyWithoutVat = BigDecimal.ZERO;
            totalAltCurrencyWithVat = BigDecimal.ZERO;

            for (InvoiceStandardDetailedData i : invoiceDetails) {
                totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.subtract(i.getMainCurrencyTotalAmountWithoutVat());
                totalMainCurrencyWithVat = totalMainCurrencyWithVat.subtract(i.getMainCurrencyTotalAmountWithVat());

                totalAltCurrencyWithoutVat = totalAltCurrencyWithoutVat.subtract(i.getAltCurrencyTotalAmountWithoutVat());
                totalAltCurrencyWithVat = totalAltCurrencyWithVat.subtract(i.getAltCurrencyTotalAmountWithVat());
            }
        }


        invoice.setPriceWasChanged(priceWasChanged.get());

        List<InvoiceDetailedDataAmountModel> amountModels = invoiceDetails
                .stream()
                .map(model -> new InvoiceDetailedDataAmountModel(
                        model.getVatRatePercent(),
                        model.getMainCurrencyTotalAmountWithoutVat(),
                        Boolean.TRUE,
                        invoice.getCurrencyExchangeRateOnInvoiceCreation()
                )).toList();
        List<InvoiceVatRateValue> uncommittedInvoiceVatRates = new ArrayList<>();
        List<InvoiceVatRateResponse> invoiceVatRateResponses = groupByVatRates(amountModels);

        if (!invoiceVatRateResponses.isEmpty()) {
            uncommittedInvoiceVatRates.addAll(mapVatRates(invoice.getId(), invoiceVatRateResponses));
        }
        totalMainCurrencyWithVat = EPBDecimalUtils
                .calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getValueOfVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP)
                .add(EPBDecimalUtils.calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP));

        totalMainCurrencyWithoutVat = EPBDecimalUtils
                .calculateSummary(uncommittedInvoiceVatRates
                        .stream()
                        .map(InvoiceVatRateValue::getAmountExcludingVat)
                        .toList()
                ).setScale(2, RoundingMode.HALF_UP);

        totalAltCurrencyWithVat =
                billingRunCurrencyService.convertToCurrency(mainCurrencyId, altCurrencyId, EPBDecimalUtils
                                .calculateSummary(uncommittedInvoiceVatRates
                                        .stream()
                                        .map(InvoiceVatRateValue::getValueOfVat)
                                        .toList()
                                ).setScale(2, RoundingMode.HALF_UP)
                                .add(EPBDecimalUtils
                                        .calculateSummary(uncommittedInvoiceVatRates
                                                .stream()
                                                .map(InvoiceVatRateValue::getAmountExcludingVat)
                                                .toList()
                                        ).setScale(2, RoundingMode.HALF_UP)), currencyHashMap)
                        .setScale(2, RoundingMode.HALF_UP);

        if (isDeducted || billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            if (totalMainCurrencyWithoutVat.compareTo(BigDecimal.ZERO) < 0) {
                totalMainCurrencyWithVat = totalMainCurrencyWithVat.abs();
                totalMainCurrencyWithoutVat = totalMainCurrencyWithoutVat.abs();
                totalAltCurrencyWithVat = totalAltCurrencyWithVat.abs();
                uncommittedInvoiceVatRates.forEach(v -> {
                    v.setValueOfVat(v.getValueOfVat().abs());
                    v.setAmountExcludingVat(v.getAmountExcludingVat().abs());
                });
                invoice.setInvoiceDocumentType(InvoiceDocumentType.CREDIT_NOTE);
            } else {
                invoice.setInvoiceDocumentType(InvoiceDocumentType.DEBIT_NOTE);
            }
        }

        invoice.setTotalAmountOfVat(totalMainCurrencyWithVat
                .subtract(totalMainCurrencyWithoutVat));

        invoice.setTotalAmountExcludingVat(totalMainCurrencyWithoutVat);
        invoice.setTotalAmountIncludingVat(totalMainCurrencyWithVat);
        invoice.setCurrencyId(mainCurrencyId);
        invoice.setCurrencyExchangeRateOnInvoiceCreation(mainCurrency.getAltCurrencyExchangeRate());
        invoice.setCurrencyIdInOtherCurrency(altCurrencyId);

      /*  invoice.setTotalAmountVatAltCurrency(totalAltCurrencyWithVat TODO DOUBLE CHECK IF THIS IS NEEDED
                .subtract(totalAltCurrencyWithoutVat));
        invoice.setTotalAmountWithoutVatAltCurrency(totalAltCurrencyWithoutVat);
        invoice.setTotalAmountWithVatAltCurrency(totalAltCurrencyWithVat);
        invoice.setAltCurrencyId(altCurrencyId);*/
        invoice.setTotalAmountIncludingVatInOtherCurrency(totalAltCurrencyWithVat);


        invoice.setInvoiceSlot(invoiceSlot);
        invoice.setBillingId(runContract.getRunId());

        List<BillingRunCompensationMapping> compensationMappings = new ArrayList<>();

        if (!billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            compensationMappings = billingRunCompensationMappingRepository.findAllByBgInvoiceSlotIdAndRunId(row.getKey(), runContract.getRunId());
            if (!compensationMappings.isEmpty()) {
                invoice.setCompensationIndex(0);
            }
        }

        BigDecimal totalActualConsumption = invoiceDetails.stream().filter(i ->
                        (i.getDetailType().equals(InvoiceStandardDetailType.SCALE) || i.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT))
                                && i.getPriceComponentPriceTypeId() == 103
                ).map(i -> i.getTotalVolumes() != null ? i.getTotalVolumes() : BigDecimal.ZERO) // Map to the totalVolumes field
                .reduce(BigDecimal::add) // Accumulate the sum, returns Optional<Double>
                .orElse(null);

        BigDecimal totalActualConsumptionAmount = invoiceDetails.stream().filter(i ->
                        (i.getDetailType().equals(InvoiceStandardDetailType.SCALE) || i.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT))
                                && i.getPriceComponentPriceTypeId() == 103
                ).map(i -> i.getMainCurrencyTotalAmountWithoutVat() != null ? i.getMainCurrencyTotalAmountWithoutVat() : BigDecimal.ZERO) // Map to the totalVolumes field
                .reduce(BigDecimal::add) // Accumulate the sum, returns Optional<Double>
                .orElse(null);

        invoice.setTotalActualConsumptionAmount(totalActualConsumptionAmount);
        invoice.setTotalActualConsumption(totalActualConsumption);

        //CHECK if with electricity exists
        boolean hasWithElectricity = invoiceDetails.stream().anyMatch(i -> i.getDetailType().equals(InvoiceStandardDetailType.WITH_ELECTRICITY));
        if (!hasWithElectricity) {
            hasWithElectricity = standardVatBaseDetails.stream().anyMatch(i -> i.getDetailType().equals(InvoiceStandardDetailType.WITH_ELECTRICITY));
        }

        boolean hasWithOneTimeOnTime = invoiceDetails.stream().anyMatch(i -> i.getDetailType().equals(InvoiceStandardDetailType.OVER_TIME_ONE_TIME));
        if (!hasWithOneTimeOnTime) {
            hasWithOneTimeOnTime = standardVatBaseDetails.stream().anyMatch(i -> i.getDetailType().equals(InvoiceStandardDetailType.OVER_TIME_ONE_TIME));
        }

        invoice.setHasOneTimeOnTimeInvoiceDetail(hasWithOneTimeOnTime);
        invoice.setHasWithElectricityInvoiceDetail(hasWithElectricity);
        invoiceRepository.saveAndFlush(invoice);
        invoiceDetails.forEach(i -> i.setInvoiceId(invoice.getId()));
        standardVatBaseDetails.forEach(v -> v.setInvoiceId(invoice.getId()));

        uncommittedInvoiceVatRates.forEach(u -> u.setInvoiceId(invoice.getId()));

        invoiceVatRateValueRepository.saveAll(uncommittedInvoiceVatRates);

        if (!billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            samePeriodInterim.forEach(i -> i.setStandardInvoiceId(invoice.getId()));
            invoiceRepository.saveAll(samePeriodInterim);

            longPaymentTerInterim.forEach(i -> i.setStandardInvoiceId(invoice.getId()));
            invoiceRepository.saveAll(longPaymentTerInterim);
        }

        invoiceStandardDetailedDataRepository.saveAll(invoiceDetails);
        invoiceStandardDetailedDataVatBaseRepository.saveAll(standardVatBaseDetails);


        if (totalActualConsumption != null) {
            invoiceDetails.stream()
                    .filter(i ->
                            (i.getDetailType().equals(InvoiceStandardDetailType.SCALE) ||
                                    i.getDetailType().equals(InvoiceStandardDetailType.SETTLEMENT)) &&
                                    i.getPriceComponentPriceTypeId() == 103
                    )
                    .collect(Collectors.groupingBy(
                            InvoiceStandardDetailedData::getPodId, // Group by podId
                            Collectors.mapping(
                                    i -> i.getTotalVolumes() != null ? i.getTotalVolumes() : BigDecimal.ZERO, // Map totalVolumes, ensure null safety
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add) // Sum totalVolumes per group
                            )
                    )).forEach((pId, tv) -> {
                        InvoiceTotalActualConsumption c = new InvoiceTotalActualConsumption();
                        c.setInvoiceId(invoice.getId());
                        c.setPodId(pId);
                        c.setTotalActualConsumption(tv);
                        invoiceTotalActualConsumptionRepository.save(c);
                    });
        }

        if (!billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            compensationMappings.forEach(c -> {
                Compensations compensation = compensationRepository.findById(c.getCompensationId()).orElse(null);
                if (compensation != null) {
                    compensation.setInvoiceId(invoice.getId());
                    compensation.setInvoiceUsageDate(LocalDate.now());
                    compensation.setCompensationIndex(0);
                    compensationRepository.save(compensation);
                }
            });
        }
        invoiceNumberService.fillInvoiceNumber(invoice);

        if (billingRunModel.getType().equals(BillingType.INVOICE_CORRECTION)) {
            billingRunCorrectionService.reverseInvoices(
                    billingRunModel,
                    baseCorrectionInvoice,
                    invoice,
                    invoiceDetails,
                    standardVatBaseDetails,
                    runContract.getContractType(),
                    currencyHashMap,
                    mainCurrencyId,
                    altCurrencyId,
                    mainCurrency,
                    correctionRunId
            );
        }
    }


    private List<InvoiceVatRateValue> mapVatRates(Long invoiceId, List<InvoiceVatRateResponse> invoiceVatRateResponses) {
        return invoiceVatRateResponses
                .stream()
                .map(vat -> InvoiceVatRateValue
                        .builder()
                        .invoiceId(invoiceId)
                        .valueOfVat(vat.valueOfVat())
                        .amountExcludingVat(vat.amountExcludingVat())
                        .vatRatePercent(vat.vatRatePercent())
                        .build()).toList();
    }

    public List<InvoiceVatRateResponse> groupByVatRates(List<InvoiceDetailedDataAmountModel> models) {
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
                            .map(InvoiceDetailedDataAmountModel::pureAmount)
                            .toList()
                    ).setScale(2, RoundingMode.HALF_UP);
            context.add(new InvoiceVatRateResponse(vatRatePercent, totalAmount));
        }

        return context;
    }

    public void calculateRestrictions(List<BillingRunInvoiceDetailBaseModel> values, List<CcyRestrictions> ccyRestrictions, List<KwhRestrictions> kwhRestrictions, BigDecimal percentRestriction, PriceComponent priceComponent, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId, Long altCurrencyId) {
        values.sort(Comparator.comparing(BillingRunInvoiceDetailBaseModel::getDateFrom));

        if (percentRestriction != null) {
            billingRunRestrictionPercentService.calculate(values, percentRestriction);
        }

        if (!kwhRestrictions.isEmpty()) {
            billingRunRestrictionKwhService.calculate(values, kwhRestrictions);
        }

        if (!ccyRestrictions.isEmpty()) {
            billingRunRestrictionCcyService.calculate(ccyRestrictions, values, priceComponent, currencyHashMap, mainCurrencyId, altCurrencyId);
        }

        BigDecimal volumesOfPercentageRestriction = values.stream()
                .map(BillingRunInvoiceDetailBaseModel::getVolumesOfPercentageRestriction)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a.add(b))
                .orElse(null);

        BigDecimal volumesOfKwhRestriction = values.stream()
                .map(BillingRunInvoiceDetailBaseModel::getVolumesOfKwhRestriction)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a.add(b))
                .orElse(null);

        BigDecimal volumesOfCcyRestriction = values.stream()
                .map(BillingRunInvoiceDetailBaseModel::getVolumesOfCcyRestriction)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a.add(b))
                .orElse(null);

        Integer index = findSmallest(volumesOfPercentageRestriction, volumesOfKwhRestriction, volumesOfCcyRestriction);

        for (BillingRunInvoiceDetailBaseModel dto : values) {
            if (index == 1) {
                if (dto.getVolumesOfPercentageRestriction() == null
                        || dto.getVolumesOfPercentageRestriction().compareTo(BigDecimal.ZERO) <= 0
                ) {
                    dto.setFinalRestrictionVolume(BigDecimal.ZERO);
                    continue;
                }
                dto.setFinalRestrictionVolume(dto.getVolumesOfPercentageRestriction());
                dto.setFinalRestrictionAmount(dto.getFinalRestrictionVolume()
                        .multiply(dto.getKwhPrice())
                        .setScale(12, RoundingMode.HALF_UP));
                dto.setKwhPrice(dto.getFinalRestrictionAmount().divide(dto.getFinalRestrictionVolume(), 12, RoundingMode.HALF_UP));
            } else if (index == 2) {
                if (dto.getVolumesOfKwhRestriction() == null
                        || dto.getVolumesOfKwhRestriction().compareTo(BigDecimal.ZERO) <= 0
                ) {
                    dto.setFinalRestrictionVolume(BigDecimal.ZERO);
                    continue;
                }
                dto.setFinalRestrictionVolume(dto.getVolumesOfKwhRestriction());
                dto.setFinalRestrictionAmount(dto.getFinalRestrictionVolume()
                        .multiply(dto.getKwhPrice())
                        .setScale(12, RoundingMode.HALF_UP));
                dto.setKwhPrice(dto.getFinalRestrictionAmount().divide(dto.getFinalRestrictionVolume(), 12, RoundingMode.HALF_UP));
            } else if (index == 3) {
                if (dto.getVolumesOfCcyRestriction() == null
                        || dto.getVolumesOfCcyRestriction().compareTo(BigDecimal.ZERO) <= 0
                ) {
                    dto.setFinalRestrictionVolume(BigDecimal.ZERO);
                    continue;
                }
                dto.setFinalRestrictionVolume(dto.getVolumesOfCcyRestriction());
                dto.setFinalRestrictionAmount(dto.getFinalRestrictionVolume()
                        .multiply(dto.getKwhPrice())
                        .setScale(12, RoundingMode.HALF_UP));
                dto.setKwhPrice(dto.getFinalRestrictionAmount().divide(dto.getFinalRestrictionVolume(), 12, RoundingMode.HALF_UP));
            }
            dto.setRestricted(true);
            dto.setDeducted(null);
            dto.setCorrection(null);
            dto.setNewMeterReading(null);
            dto.setOldMeterReading(null);
            dto.setNewMeterReading(null);
            dto.setDifference(null);
            dto.setCalcIsDifferenceSplit(false);
            dto.setCalcIsCorrectionSplit(false);
            dto.setCalcIsMeterReadingNewSplit(false);
            dto.setCalcIsDeductSplit(false);
            dto.setCalcIsOldMeterReadingSplit(false);
            dto.setMultiplier(null);
        }
    }

    private Integer findSmallest(BigDecimal a, BigDecimal b, BigDecimal c) {
        Integer number = null;
        BigDecimal smallest = null;

        if (a != null) {
            smallest = a;
            number = 1;
        }

        if (b != null) {
            if (smallest == null || b.compareTo(smallest) < 0) {
                smallest = b;
                number = 2;
            }
        }

        if (c != null) {
            if (smallest == null || c.compareTo(smallest) < 0) {
                smallest = c;
                number = 3;
            }
        }

        return number;
    }

    private void addDetailCostCenterAndIncomeAcc(Set<Map.Entry<Long, List<Long>>> entries, Map<Pair<Long, Long>, CostCenterAndIncomeAccountResponse> costAndIncomeMap, boolean service) {
        for (Map.Entry<Long, List<Long>> entry : entries) {
            List<CostCenterAndIncomeAccountResponse> costCenterAndIncomeAccountByDetailId = service ?
                    serviceDetailsRepository.getCostCenterAndIncomeAccountByDetailId(entry.getValue())
                    : productDetailsRepository.getCostCenterAndIncomeAccountByDetailId(entry.getValue());

            costCenterAndIncomeAccountByDetailId.forEach(resp -> costAndIncomeMap.put(Pair.of(entry.getKey(), resp.getDetailId()), resp));

        }
    }
}
