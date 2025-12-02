package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.*;
import bg.energo.phoenix.billingRun.model.entity.BillingRunBdbsSplitsIoData;
import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.repository.BillingRunBdbsSplitsIoDataRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.OgnlException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunProcessScaleCalculationService {


    private final BillingRunContractsRepository billingRunContractsRepository;
    private final BillingRunBdbsSplitsIoDataRepository billingRunBdbsSplitsIoDataRepository;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;
    private final BillingDataByProfileRepository billingDataByProfileRepository;
    private final BillingRunPriceComponentEvaluationService priceComponentEvaluationService;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final PriceComponentRepository priceComponentRepository;

    public Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> splitAndEvaluateScales(BillingRunContracts runContract, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap, HashMap<Long, PriceComponent> priceComponentHashMap) {

        List<BillingRunBdbsSplitsIoData> initialSplits = billingRunBdbsSplitsIoDataRepository.findAllByRunContractId(runContract.getId());

        List<BillingRunBdbsSplitsIoData> splitsIos = new ArrayList<>();

        for (BillingRunBdbsSplitsIoData data : initialSplits) {
            if (failedSlots.contains(data.getBgInvoiceSlotId())) {
                continue;
            }
            data.setDateFrom(data.getCalcPeriodFrom());
            data.setValue(data.getCalcVolume());
            splitsIos.add(data);
        }
        List<BillingRunBdbsSplitsIoData> finalSplit = processScaleFinalSplits(splitsIos, failedSlots, xValueMap, runContract,priceComponentHashMap, mainCurrencyId, currencyHashMap);

        Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> slotGrouping = new HashMap<>();

        for (BillingRunBdbsSplitsIoData row : finalSplit) {
            BillingRunInvoiceDetailBaseModel model = createBillingRunVolumeBaseModel(row, failedSlots, priceComponentHashMap, mainCurrencyId, currencyHashMap);
            if (model == null)
                continue;
            SlotGrouping slot = new SlotGrouping(model.getBgInvoiceSlotId(), model.getPodId(), model.getPcId());
            slotGrouping.computeIfAbsent(slot, k -> new ArrayList<>()).add(model);
        }

        return slotGrouping;
    }


    public void roundAndAdjustValues(BigDecimal totalValue, List<? extends Value> values, Integer index) {

        if (index == 1) {//VOLUME
            BigDecimal totalRoundedValues = values.stream()
                    .map(Value::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal diff = totalValue.subtract(totalRoundedValues);

            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                return; // Nothing to adjust
            }

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                values.sort(Comparator
                        .comparing(Value::getValueNinthDigitAfterDot)
                        .thenComparing(Value::getDateFrom));
            } else {
                values.sort(Comparator
                        .comparing(Value::getValueNinthDigitAfterDot, Comparator.reverseOrder())
                        .thenComparing(Value::getDateFrom));
            }


            BigDecimal adjustment = BigDecimal.valueOf(0.00000001);

            while (diff.compareTo(BigDecimal.ZERO) != 0) {

                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().add(adjustment);
                        value.setValue(newValue);
                        diff = diff.subtract(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                } else {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().subtract(adjustment);
                        value.setValue(newValue);
                        diff = diff.add(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                }
            }
        } else if (index == 2) {//Deduction
            BigDecimal totalRoundedValues = values.stream()
                    .map(Value::getValueDeduction)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal diff = totalValue.subtract(totalRoundedValues);

            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                return; // Nothing to adjust
            }

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                values.sort(Comparator
                        .comparing(Value::getDeductionNinthDigitAfterDot)
                        .thenComparing(Value::getDateFrom));
            } else {
                values.sort(Comparator
                        .comparing(Value::getDeductionNinthDigitAfterDot, Comparator.reverseOrder())
                        .thenComparing(Value::getDateFrom));
            }


            BigDecimal adjustment = BigDecimal.valueOf(0.00000001);

            while (diff.compareTo(BigDecimal.ZERO) != 0) {

                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().add(adjustment);
                        value.setValueDeduction(newValue);
                        diff = diff.subtract(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                } else {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().subtract(adjustment);
                        value.setValueDeduction(newValue);
                        diff = diff.add(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                }
            }
        } else if (index == 3) {//Correction
            BigDecimal totalRoundedValues = values.stream()
                    .map(Value::getValueCorrection)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal diff = totalValue.subtract(totalRoundedValues);

            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                return; // Nothing to adjust
            }

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                values.sort(Comparator
                        .comparing(Value::getCorrectionNinthDigitAfterDot)
                        .thenComparing(Value::getDateFrom));
            } else {
                values.sort(Comparator
                        .comparing(Value::getCorrectionNinthDigitAfterDot, Comparator.reverseOrder())
                        .thenComparing(Value::getDateFrom));
            }

            BigDecimal adjustment = BigDecimal.valueOf(0.00000001);

            while (diff.compareTo(BigDecimal.ZERO) != 0) {

                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().add(adjustment);
                        value.setValueCorrection(newValue);
                        diff = diff.subtract(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                } else {
                    for (Value value : values) {
                        BigDecimal newValue = value.getValue().subtract(adjustment);
                        value.setValueCorrection(newValue);
                        diff = diff.add(adjustment);
                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private BillingRunInvoiceDetailBaseModel createBillingRunVolumeBaseModel(BillingRunBdbsSplitsIoData row, HashSet<Long> failedSlots, HashMap<Long, PriceComponent> priceComponentHashMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap) {
        if (failedSlots.contains(row.getBgInvoiceSlotId())) {
            return null;
        }
        PriceComponent priceComponent = priceComponentHashMap.getOrDefault(row.getPcId(), null);
        if (priceComponent == null) {
            priceComponent = priceComponentRepository.findById(row.getPcId()).get();
            priceComponentHashMap.put(row.getPcId(), priceComponent);
        }

        BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
        model.setType(InvoiceStandardDetailType.SCALE);
        model.setCalculatedPrice(row.getRunTotalPrice());
        model.setCalculatedVolumes(row.getRunTotalValue());
        model.setDateFrom(row.getDateFrom());
        model.setDateTo(row.getCalcPeriodTo().atStartOfDay());
        model.setPcId(row.getPcId());
        model.setPodId(row.getPodId());
        model.setPodDetailId(row.getPodDetailId());
        model.setKwhPrice(row.getRunKwhPrice());
        model.setBgInvoiceSlotId(row.getBgInvoiceSlotId());
        model.setContractDetailId(row.getContractDetailId());
        model.setCustomerDetailId(row.getCustomerDetailId());
        model.setCustomerId(row.getCustomerId());
        model.setProductDetailId(row.getProductDetailId());
        model.setPcGroupId(row.getPcGroupId());
        model.setTariff(row.getIsTariff());
        model.setMeterId(row.getMeterId());
        model.setNewMeterReading(row.getCalcNewMeterReadings());
        model.setOldMeterReading(row.getCalcOldMeterReading());
        model.setCorrection(row.getCalcCorrection());
        model.setMultiplier(row.getMultiplier());
        model.setDeducted(row.getCalcDeduction());
        model.setDifference(row.getCalcDifference());
        model.setCalcIsCorrectionSplit(row.getCalcIsCorrectionSplit());
        model.setCalcIsDeductSplit(row.getCalcIsDeductSplit());
        model.setCalcIsOldMeterReadingSplit(row.getCalcIsOldMeterReadingSplit());
        model.setCalcIsMeterReadingNewSplit(row.getCalcIsMeterReadingNewSplit());
        model.setCalcIsDifferenceSplit(row.getCalcIsDifferenceSplit());
        model.setHasCorrection(row.getHasCorrection());
        model.setBillingDataScaleIds(row.getBillingDataScaleIds());
        model.setScaleId(row.getScaleId());
        model.setPriceComponentPriceTypeId(row.getPriceComponentPriceTypeId());
        return model;
    }



    public List<BillingRunBdbsSplitsIoData> processScaleFinalSplits(List<BillingRunBdbsSplitsIoData> splitsIos, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, BillingRunContracts runContract, HashMap<Long, PriceComponent> priceComponentHashMap,Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap) {
        List<BillingRunBdbsSplitsIoData> finalSplit = new ArrayList<>();
        Map<PriceAndDateMap, BigDecimal> priceParamMap = new HashMap<>();
        Map<PriceAndDateMap, BigDecimal> profileParamMap = new HashMap<>();
        String regex = "\\$(\\d+)\\$";

        for (BillingRunBdbsSplitsIoData io : splitsIos) {
            try {
                if (failedSlots.contains(io.getBgInvoiceSlotId())) {
                    continue;
                }
                PriceComponent priceComponent = priceComponentHashMap.getOrDefault(io.getPcId(), null);
                if (priceComponent == null) {
                    priceComponent = priceComponentRepository.findById(io.getPcId()).get();
                    priceComponentHashMap.put(io.getPcId(), priceComponent);
                }
                List<PriceComponentFormulaXValue> formulaXValues = priceComponentEvaluationService.getFormulaXValues(io.getContractDetailId(), io.getPcId(), xValueMap, runContract);

                Map<String, Object> variablesContext = new HashMap<>();
                for (PriceComponentFormulaXValue x : formulaXValues) {
                    variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(io.getPriceFormula());
                List<String> matches = new ArrayList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(1));
                }
//01.01.2024 02.02.2024
                if (io.getPriceFormula().contains("$PRICE_PROFILE$") || !matches.isEmpty()) {
                    if (checkSameMonth(io.getDateFrom(), io.getCalcPeriodTo())) {
                        // Split not needed
                        processNoSplit(finalSplit, io, matches, priceParamMap, profileParamMap, variablesContext, runContract,priceComponent,mainCurrencyId,currencyHashMap);
                    } else {
                        // Split may be needed
                        processSplit(finalSplit, io, matches, priceParamMap, profileParamMap, variablesContext, runContract,priceComponent,mainCurrencyId,currencyHashMap);
                    }
                } else {
                    processDefault(finalSplit, io, variablesContext,priceComponent,mainCurrencyId,currencyHashMap);
                }
            } catch (Exception e) {
                log.error("Exception in billing run scale evaluation  bdbsSplitIoId: [%s], PcId: [%s] PodId: [%s]".formatted(io.getBgInvoiceSlotId(), io.getBdsSplitIoId(), io.getPodId()), e);
                billingRunContractsRepository.updateBgInvoiceSlotStatus(io.getBgInvoiceSlotId(), "ERROR",
                        "scale evaluation error, BgInvoiceSlotId: [%s] bdbsSplitIoId: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(io.getBgInvoiceSlotId(), io.getBdsSplitIoId(), io.getPodId(), e.getMessage()));
                failedSlots.add(io.getBgInvoiceSlotId());
            }
        }
        return finalSplit;
    }

    private void processNoSplit(List<BillingRunBdbsSplitsIoData> finalSplit, BillingRunBdbsSplitsIoData io, List<String> matches, Map<PriceAndDateMap, BigDecimal> priceParamMap, Map<PriceAndDateMap, BigDecimal> profileParamMap, Map<String, Object> variablesContext, BillingRunContracts runContract,PriceComponent priceComponent,Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap) throws OgnlException {
        LocalDateTime startDayOfMonth = io.getDateFrom().withDayOfMonth(1).atStartOfDay();
        if (!matches.isEmpty()) {
            for (String part : matches) {
                scaleExtractPriceParameterPrice(priceParamMap, variablesContext, startDayOfMonth, part);
            }
        }
        if (io.getPriceFormula().contains("$PRICE_PROFILE$")) {
            scaleExtractProfilePrice(profileParamMap, io, variablesContext, startDayOfMonth, runContract);
        }

        BigDecimal price = priceComponentEvaluationService.evaluateExpression(io.getPriceFormula(), variablesContext);
        if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
            price = price.negate();
        }
        price = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

        io.setRunKwhPrice(price);
        io.setRunTotalValue(io.getValue());
        io.setRunTotalPrice(price.multiply(io.getValue())
                .setScale(12, RoundingMode.HALF_UP)
        );
        finalSplit.add(io);
    }

    private void processSplit(List<BillingRunBdbsSplitsIoData> finalSplit, BillingRunBdbsSplitsIoData io, List<String> matches, Map<PriceAndDateMap, BigDecimal> priceParamMap, Map<PriceAndDateMap, BigDecimal> profileParamMap, Map<String, Object> variablesContext, BillingRunContracts runContract,PriceComponent priceComponent,Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap) throws OgnlException, CloneNotSupportedException {
        List<BillingRunBdbsSplitsIoData> middleSplit = new ArrayList<>();
        List<LocalDateTime> startOfMonths = splitIntoMonths(io.getDateFrom().atStartOfDay(), io.getCalcPeriodTo().atStartOfDay());
        BigDecimal prevPrice = null;
        LocalDateTime splitStartDate = null;

        for (int i = 0; i < startOfMonths.size(); i++) {
            LocalDateTime startDayOfMonth = startOfMonths.get(i);
            variablesContext = new HashMap<>();
            List<PriceComponentFormulaXValue> formulaXValues = billingRunContractsRepository.getPriceComponentXValuesForProductContracts(io.getContractDetailId(), io.getPcId());
            for (PriceComponentFormulaXValue x : formulaXValues) {
                variablesContext.put("$" + x.getKey() + "$", x.getValue());
            }

            if (!matches.isEmpty()) {
                for (String part : matches) {
                    scaleExtractPriceParameterPrice(priceParamMap, variablesContext, startDayOfMonth, part);
                }
            }
            if (io.getPriceFormula().contains("$PRICE_PROFILE$")) {
                scaleExtractProfilePrice(profileParamMap, io, variablesContext, startDayOfMonth, runContract);
            }

            BigDecimal price = priceComponentEvaluationService.evaluateExpression(io.getPriceFormula(), variablesContext);
            if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
                price = price.negate();
            }
            price = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

            if (prevPrice == null) {
                prevPrice = price;
                splitStartDate = io.getDateFrom().atStartOfDay();
            } else {
                LocalDateTime splitEndDate = startDayOfMonth.minusDays(1);

                if (prevPrice.compareTo(price) != 0) {
                    BillingRunBdbsSplitsIoData bean = io.clone();
                    bean.setCalcPeriodFrom(splitStartDate.toLocalDate());
                    bean.setDateFrom(splitStartDate.toLocalDate());
                    bean.setCalcPeriodTo(splitEndDate.toLocalDate());
                    bean.setRunKwhPrice(prevPrice);
                    bean.setRunTotalValue(bean.getValue());

                    middleSplit.add(bean);
                    splitStartDate = startDayOfMonth;
                }

                if (i == startOfMonths.size() - 1) {
                    BillingRunBdbsSplitsIoData bean = io.clone();
                    bean.setCalcPeriodFrom(splitStartDate.toLocalDate());
                    bean.setDateFrom(splitStartDate.toLocalDate());
                    bean.setCalcPeriodTo(io.getCalcPeriodTo().atStartOfDay().toLocalDate());
                    bean.setRunKwhPrice(price);
                    bean.setRunTotalValue(bean.getValue());
                    bean.setRunTotalPrice(bean.getRunKwhPrice().multiply(bean.getValue())
                            .setScale(12, RoundingMode.HALF_UP));
                    middleSplit.add(bean);
                }
            }
        }

        if(middleSplit.size()>1) {
            Collections.sort(middleSplit, Comparator.comparing(BillingRunBdbsSplitsIoData::getCalcPeriodFrom));

            Boolean calcIsOldMeterReadingSplit = io.getCalcIsOldMeterReadingSplit();
            Boolean calcIsMeterReadingNewSplit = io.getCalcIsMeterReadingNewSplit();
            Boolean calcIsCorrectionSplit = io.getCalcIsCorrectionSplit();
            Boolean calcIsDeductSplit = io.getCalcIsDeductSplit();
            Boolean calcIsDifferenceSplit = io.getCalcIsDifferenceSplit();
            BigDecimal calcOldMeterReading = io.getCalcOldMeterReading();
            BigDecimal calcNewMeterReadings = io.getCalcNewMeterReadings();
            BigDecimal calcDifference;
            BigDecimal calcCorrection = io.getCalcCorrection();
            BigDecimal calcDeduct = io.getCalcDeduction();
            BigDecimal multiplier = io.getMultiplier();

            long totalDays = ChronoUnit.DAYS.between(io.getCalcPeriodFrom(), io.getCalcPeriodTo()) + 1;

            for (int i = 0; i < middleSplit.size(); i++) {
                BillingRunBdbsSplitsIoData row = middleSplit.get(i);
                long daysBetween = ChronoUnit.DAYS.between(row.getCalcPeriodFrom(), row.getCalcPeriodTo()) + 1;

                if (!io.getIsTariff()) {

                    BigDecimal value = io.getCalcVolume().divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(daysBetween))
                            .setScale(12, RoundingMode.HALF_UP);

                    row.setValueBefore9DigitRounding(value.setScale(9, RoundingMode.HALF_UP));
                    row.setValue(value.setScale(8, RoundingMode.HALF_UP));
                    row.setCalcVolume(row.getValue());
                    row.setRunTotalValue(row.getValue());

                    if (io.getHasCorrection() == null || !io.getHasCorrection()) {
                        if (io.getCalcDeduction() != null) {
                            BigDecimal deduction = io.getCalcDeduction().divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(daysBetween))
                                    .setScale(12, RoundingMode.HALF_UP);

                            row.setValueDeductionBefore9DigitRounding(deduction.setScale(9, RoundingMode.HALF_UP));
                            row.setValueDeduction(deduction.setScale(8, RoundingMode.HALF_UP));
                            calcIsDeductSplit = true;
                        }

                        if (io.getCalcCorrection() != null) {
                            BigDecimal correction = io.getCalcCorrection().divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(daysBetween))
                                    .setScale(12, RoundingMode.HALF_UP);
                            row.setValueCorrectionBefore9DigitRounding(correction.setScale(9, RoundingMode.HALF_UP));
                            row.setValueCorrection(correction.setScale(8, RoundingMode.HALF_UP));
                            calcIsCorrectionSplit = true;
                        }
                    }

                } else {
                    BigDecimal value = io.getValue()
                            .divide(BigDecimal.valueOf(totalDays), 12, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(daysBetween))
                            .setScale(12, RoundingMode.HALF_UP);
                    row.setValueBefore9DigitRounding(value.setScale(9, RoundingMode.HALF_UP));
                    row.setValue(value.setScale(8, RoundingMode.HALF_UP));
                    row.setCalcVolume(row.getValue());
                    row.setRunTotalValue(row.getValue());

                }

                row.setCalcOldMeterReading(calcOldMeterReading);
                row.setCalcNewMeterReadings(calcNewMeterReadings);
                row.setCalcCorrection(calcCorrection);
                row.setCalcDeduction(calcDeduct);
                row.setCalcIsOldMeterReadingSplit(calcIsOldMeterReadingSplit);
                row.setCalcIsMeterReadingNewSplit(calcIsMeterReadingNewSplit);
                row.setCalcIsCorrectionSplit(calcIsCorrectionSplit);
                row.setCalcIsDeductSplit(calcIsDeductSplit);
                row.setCalcIsDifferenceSplit(calcIsDifferenceSplit);
            }

            roundAndAdjustValues(io.getCalcVolume(), middleSplit, 1);

            if (io.getCalcDeduction() != null) {
                roundAndAdjustValues(io.getCalcDeduction(), middleSplit, 2);
            }
            if (io.getCalcCorrection() != null) {
                roundAndAdjustValues(io.getCalcCorrection(), middleSplit, 3);
            }

            for (int i = 0; i < middleSplit.size(); i++) {
                BillingRunBdbsSplitsIoData f = middleSplit.get(i);
                f.setRunTotalValue(f.getValue());
                f.setRunTotalPrice(f.getRunKwhPrice().multiply(f.getValue())
                        .setScale(12, RoundingMode.HALF_UP));
                f.setCalcDeduction(f.getValueDeduction());
                f.setCalcCorrection(f.getValueCorrection());

                if (!io.getIsTariff()) {
                    f.setCalcCorrection(f.getValueCorrection());
                    f.setCalcDeduction(f.getValueDeduction());
                    f.setCalcDifference((f.getValue()
                            .subtract(f.getValueCorrection() == null ? BigDecimal.ZERO : f.getValueCorrection())
                            .add(f.getValueDeduction() == null ? BigDecimal.ZERO : f.getValueDeduction()))
                            .divide(io.getMultiplier(), 8, RoundingMode.HALF_UP));
                    f.setCalcIsDifferenceSplit(true);
                    if (i == 0) {
                        f.setCalcOldMeterReading(io.getCalcOldMeterReading());
                    } else {
                        f.setCalcOldMeterReading(middleSplit.get(i - 1).getCalcNewMeterReadings());
                        f.setCalcIsOldMeterReadingSplit(true);
                    }
                    f.setCalcNewMeterReadings(f.getCalcOldMeterReading().add(f.getCalcDifference()).setScale(8, RoundingMode.HALF_UP));
                    f.setCalcIsMeterReadingNewSplit(true);
                }
            }
        }

        finalSplit.addAll(middleSplit);
    }

    private void scaleExtractProfilePrice(Map<PriceAndDateMap, BigDecimal> profileParamMap, BillingRunBdbsSplitsIoData io, Map<String, Object> variablesContext, LocalDateTime startDayOfMonth, BillingRunContracts runContract) {
        PriceAndDateMap pMap = new PriceAndDateMap(io.getPodId().toString(), startDayOfMonth);
        BigDecimal price = profileParamMap.get(pMap);
        if (price == null) {
            // Check if the price is already cached
            BillingDataByProfilePricePrice bp = billingDataByProfileRepository.findPriceProfileByPodIdForExactDateWithType(runContract.getRunId(), io.getPodId(), startDayOfMonth, "ONE_MONTH");
            if (bp != null && bp.getValue() != null) {
                price = bp.getValue();
                profileParamMap.put(pMap, price); // Cache the price
            } else {
                // Mark split as error and handle accordingly
                throw new RuntimeException("Price profile not found for PodId: " + io.getPodId() + " and date: " + startDayOfMonth);
            }
        }
        variablesContext.put("$PRICE_PROFILE$", price);
    }


    private void scaleExtractPriceParameterPrice(Map<PriceAndDateMap, BigDecimal> priceParamMap, Map<String, Object> variablesContext, LocalDateTime startDayOfMonth, String part) {
        PriceAndDateMap pMap = new PriceAndDateMap(part, startDayOfMonth);
        BigDecimal price = priceParamMap.get(pMap);
        if (price == null) {
            // Check if the price is already cached
            PriceParameterDetailInfo ppd = priceParameterDetailInfoRepository.findByParameterIdAndTypeAndExactDate(Long.parseLong(part), startDayOfMonth, PeriodType.ONE_MONTH);
            if (ppd != null && ppd.getPrice() != null) {
                price = ppd.getPrice();
                priceParamMap.put(pMap, price); // Cache the price
            } else {
                // Mark split as error and handle accordingly
                throw new RuntimeException("Price parameter not found for part: " + part + " and date: " + startDayOfMonth);
            }
        }
        variablesContext.put("$" + part + "$", price);
    }


    public List<LocalDateTime> splitIntoMonths(LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> startOfMonths = new ArrayList<>();

        // Move to the start of the month for the start date
        LocalDateTime current = start.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);

        while (!current.isAfter(end)) {
            startOfMonths.add(current);
            current = current.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        }

        return startOfMonths;
    }

    private void processDefault(List<BillingRunBdbsSplitsIoData> finalSplit, BillingRunBdbsSplitsIoData io, Map<String, Object> variablesContext,PriceComponent priceComponent,Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap) throws OgnlException {
        BigDecimal price = priceComponentEvaluationService.evaluateExpression(io.getPriceFormula(), variablesContext);
        if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
            price = price.negate();
        }
        price = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

        io.setRunKwhPrice(price);
        io.setRunTotalValue(io.getValue());
        io.setRunTotalPrice(price.multiply(io.getValue())
                .setScale(12, RoundingMode.HALF_UP)
        );
        finalSplit.add(io);
    }
    public static boolean checkSameMonth(LocalDate date1, LocalDate date2) {
        return date1.getYear() == date2.getYear() && date1.getMonth() == date2.getMonth();
    }
}
