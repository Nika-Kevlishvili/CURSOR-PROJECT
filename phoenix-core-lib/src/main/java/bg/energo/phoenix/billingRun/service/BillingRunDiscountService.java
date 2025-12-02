package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.*;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.repository.pod.discount.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunDiscountService {

    private final DiscountRepository discountRepository;
    private final BillingRunProcessScaleCalculationService billingRunProcessScaleCalculationService;
    private final BillingRunCurrencyService billingRunCurrencyService;

    public List<BillingRunInvoiceDetailBaseModel> processDiscountScale(Map.Entry<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> row, SlotGrouping key, HashMap<DiscountMap, List<Discount>> discountHashMap, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId) {

        List<BillingRunInvoiceDetailBaseModel> discountFinalList = new ArrayList<>();

        List<DiscountGroup> discountGroups = new ArrayList<>();
        for (BillingRunInvoiceDetailBaseModel detail : row.getValue()) {
             Optional<DiscountGroup> discountGroup = discountGroups.stream().filter(d->
                     d.getPodId().equals(detail.getPodId())
                     && d.getTo().equals(detail.getDateTo().toLocalDate())
                     && d.getFrom().equals(detail.getDateFrom())
                             && d.getCustomerId().equals(detail.getCustomerId())
                     && d.getBgInvoiceSlotId().equals(detail.getBgInvoiceSlotId())).findAny();
             if(discountGroup.isPresent()){
                 discountGroup.get().setVolumes(discountGroup.get().getVolumes().add(detail.getFinalRestrictionVolume() == null ? detail.getCalculatedVolumes() : detail.getFinalRestrictionVolume()));
             }else{
                 discountGroups.add(new DiscountGroup(detail.getPodId(),
                         detail.getDateFrom(),
                         detail.getDateTo().toLocalDate(),
                         detail.getFinalRestrictionVolume() == null ? detail.getCalculatedVolumes() : detail.getFinalRestrictionVolume(),
                         detail.getCustomerId(),
                         detail.getPcGroupId(),
                         detail.getPcId(),
                         detail.getBgInvoiceSlotId(),
                         detail.getContractDetailId(),
                         detail.getCustomerDetailId(),
                         detail.getPriceComponentPriceTypeId(),
                         detail.getProductDetailId()));
             }
        }

        for (DiscountGroup detail : discountGroups) {
            DiscountMap discountMap = new DiscountMap(detail.getPodId(), detail.getCustomerId());
            List<Discount> allDiscounts;
            if (discountHashMap.containsKey(discountMap)) {
                allDiscounts = discountHashMap.get(discountMap);
                if (allDiscounts == null) {
                    continue;
                }
            } else {
                allDiscounts = discountRepository.findActiveDiscountByPointOfDeliveryIdAndCustomerId(key.getPodId(), detail.getCustomerId());
                if (allDiscounts.isEmpty()) {
                    discountHashMap.put(discountMap, null);
                    continue;
                }
                discountHashMap.put(discountMap, allDiscounts);
            }


            LocalDate periodFrom = detail.getFrom();
            LocalDate periodTo = detail.getTo();
            List<Discount> filteredDiscounts = allDiscounts
                    .stream()
                    .filter(discount ->
                            !periodFrom.isAfter(discount.getDateTo()) &&
                                    !periodTo.isBefore(discount.getDateFrom()))
                    .toList();
            if (filteredDiscounts.isEmpty()) {
                continue;
            }

            long periodNumberOfDays = periodFrom.until(periodTo, ChronoUnit.DAYS) + 1;

            BigDecimal consumptionPerDay = detail.getVolumes()
                        .divide(new BigDecimal(periodNumberOfDays), 12, RoundingMode.HALF_UP);


            List<BillingRunInvoiceDetailBaseModel> discountList = new ArrayList<>();

            if(!filteredDiscounts.isEmpty()){
                row.getValue().stream().filter(r->r.getPodId().equals(detail.getPodId())
                && r.getPcId().equals(detail.getPcId())
                && r.getBgInvoiceSlotId().equals(detail.getBgInvoiceSlotId())
                && r.getDateFrom().equals(detail.getFrom())
                && r.getDateTo().toLocalDate().equals(detail.getTo())
                        && r.getType().equals(InvoiceStandardDetailType.SCALE))
                        .forEach(d->{
                            d.setOldMeterReading(null);
                            d.setNewMeterReading(null);
                            d.setCorrection(null);
                            d.setDeducted(null);
                            d.setCalcIsOldMeterReadingSplit(null);
                            d.setCalcIsMeterReadingNewSplit(null);
                            d.setCalcIsCorrectionSplit(null);
                            d.setCalcIsDeductSplit(null);
                            d.setCalcIsDifferenceSplit(null);
                            d.setMultiplier(null);
                            d.setDifference(null);
                            d.setDiscounted(true);
                        });
            }


            for (Discount discount : filteredDiscounts) {
                BigDecimal price = billingRunCurrencyService.convertToCurrency(discount.getCurrencyId(), mainCurrencyId, discount.getAmountInMoneyPerKWH(), currencyHashMap);

                LocalDate calculatedDiscountPeriodFrom = discount.getDateFrom().isBefore(periodFrom) ? periodFrom : discount.getDateFrom();
                LocalDate calculatedDiscountPeriodTo = discount.getDateTo().isAfter(periodTo) ? periodTo : discount.getDateTo();

                long calculatedDays = calculatedDiscountPeriodFrom.until(calculatedDiscountPeriodTo, ChronoUnit.DAYS) + 1;

                BigDecimal totalVolumeConsumptionBetweenDates = consumptionPerDay
                        .multiply(new BigDecimal(calculatedDays))
                        .setScale(12, RoundingMode.HALF_UP);

                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
                model.setType(InvoiceStandardDetailType.DISCOUNT_SCALE);
                model.setDiscountPerKWH(price);
                model.setDateFrom(calculatedDiscountPeriodFrom);
                model.setDateTo(calculatedDiscountPeriodTo.atStartOfDay());
                model.setPcId(detail.getPcId());
                model.setPcGroupId(detail.getPcGroupId());
                model.setPodId(detail.getPodId());
                model.setBgInvoiceSlotId(detail.getBgInvoiceSlotId());
                model.setValue(totalVolumeConsumptionBetweenDates.setScale(8, RoundingMode.HALF_UP));
                model.setValueBefore9DigitRounding(totalVolumeConsumptionBetweenDates.setScale(9, RoundingMode.HALF_UP));
                model.setContractDetailId(detail.getContractDetailId());
                model.setCustomerDetailId(detail.getCustomerDetailId());
                model.setDiscountId(discount.getId());
                model.setPriceComponentPriceTypeId(detail.getPriceComponentPriceTypeId());
                model.setProductDetailId(detail.getProductDetailId());
                discountList.add(model);
            }

            List<BillingRunInvoiceDetailBaseModel> discountForRounding = new ArrayList<>();

            discountForRounding.addAll(discountList);
            discountForRounding.addAll(findGaps(discountList, periodFrom, periodTo, consumptionPerDay));

            billingRunProcessScaleCalculationService.roundAndAdjustValues(
                    detail.getVolumes()
                    , discountForRounding,1);

            discountList
                    .forEach(model -> {
                        model.setDiscountAmount(model.getValue().multiply(model.getDiscountPerKWH()).setScale(12, RoundingMode.HALF_UP));
                        model.setInvoiceTotalConsumption(model.getValue());
                        model.setDateFrom(detail.getFrom());
                        model.setDateTo(detail.getTo().atStartOfDay());
                    });

            discountFinalList.addAll(discountList);
        }
        return discountFinalList;
    }

    public List<BillingRunInvoiceDetailBaseModel> processDiscount(Map.Entry<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> row, SlotGrouping key, HashMap<DiscountMap, List<Discount>> discountHashMap, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId) {

        List<BillingRunInvoiceDetailBaseModel> discountFinalList = new ArrayList<>();
        for (BillingRunInvoiceDetailBaseModel detail : row.getValue()) {
            DiscountMap discountMap = new DiscountMap(detail.getPodId(), detail.getCustomerId());
            List<Discount> allDiscounts;
            if (discountHashMap.containsKey(discountMap)) {
                allDiscounts = discountHashMap.get(discountMap);
                if (allDiscounts == null) {
                    continue;
                }
            } else {
                allDiscounts = discountRepository.findActiveDiscountByPointOfDeliveryIdAndCustomerId(key.getPodId(), detail.getCustomerId());
                if (allDiscounts.isEmpty()) {
                    discountHashMap.put(discountMap, null);
                    continue;
                }
                discountHashMap.put(discountMap, allDiscounts);
            }


            LocalDate periodFrom = detail.getDateFrom();
            LocalDate periodTo = detail.getDateTo().toLocalDate();
            List<Discount> filteredDiscounts = allDiscounts
                    .stream()
                    .filter(discount ->
                            !periodFrom.isAfter(discount.getDateTo()) &&
                                    !periodTo.isBefore(discount.getDateFrom()))
                    .toList();
            if (filteredDiscounts.isEmpty()) {
                continue;
            }
            long periodNumberOfDays = periodFrom.until(periodTo, ChronoUnit.DAYS) + 1;


            BigDecimal consumptionPerDay;
            if (detail.getFinalRestrictionVolume() != null) {
                consumptionPerDay = detail.getFinalRestrictionVolume()
                        .divide(new BigDecimal(periodNumberOfDays), 12, RoundingMode.HALF_UP);
            } else {
                consumptionPerDay = detail.getCalculatedVolumes()
                        .divide(new BigDecimal(periodNumberOfDays), 12, RoundingMode.HALF_UP);
            }

            List<BillingRunInvoiceDetailBaseModel> discountList = new ArrayList<>();
            for (Discount discount : filteredDiscounts) {
                BigDecimal price = billingRunCurrencyService.convertToCurrency(discount.getCurrencyId(), mainCurrencyId, discount.getAmountInMoneyPerKWH(), currencyHashMap);

                LocalDate calculatedDiscountPeriodFrom = discount.getDateFrom().isBefore(periodFrom) ? periodFrom : discount.getDateFrom();
                LocalDate calculatedDiscountPeriodTo = discount.getDateTo().isAfter(periodTo) ? periodTo : discount.getDateTo();

                long calculatedDays = calculatedDiscountPeriodFrom.until(calculatedDiscountPeriodTo, ChronoUnit.DAYS) + 1;

                BigDecimal totalVolumeConsumptionBetweenDates = consumptionPerDay
                        .multiply(new BigDecimal(calculatedDays))
                        .setScale(12, RoundingMode.HALF_UP);

                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
                model.setType(InvoiceStandardDetailType.DISCOUNT);
                model.setDiscountPerKWH(price);
                model.setDateFrom(calculatedDiscountPeriodFrom);
                model.setDateTo(calculatedDiscountPeriodTo.atStartOfDay());
                model.setPcId(detail.getPcId());
                model.setPcGroupId(detail.getPcGroupId());
                model.setPodId(detail.getPodId());
                model.setBgInvoiceSlotId(detail.getBgInvoiceSlotId());
                model.setValue(totalVolumeConsumptionBetweenDates.setScale(8, RoundingMode.HALF_UP));
                model.setValueBefore9DigitRounding(totalVolumeConsumptionBetweenDates.setScale(9, RoundingMode.HALF_UP));
                model.setContractDetailId(detail.getContractDetailId());
                model.setCustomerDetailId(detail.getCustomerDetailId());
                model.setDiscountId(discount.getId());
                model.setPriceComponentPriceTypeId(detail.getPriceComponentPriceTypeId());
                model.setProductDetailId(detail.getProductDetailId());

                discountList.add(model);
                detail.setDiscounted(true);
            }
            List<BillingRunInvoiceDetailBaseModel> discountForRounding = new ArrayList<>();

            discountForRounding.addAll(discountList);
            discountForRounding.addAll(findGaps(discountList, periodFrom, periodTo, consumptionPerDay));

            billingRunProcessScaleCalculationService.roundAndAdjustValues(
                    detail.getFinalRestrictionVolume() == null ? detail.getCalculatedVolumes() : detail.getFinalRestrictionVolume()
                    , discountForRounding,1);

            discountList
                    .forEach(model -> {
                        model.setDiscountAmount(model.getValue().multiply(model.getDiscountPerKWH()).setScale(12, RoundingMode.HALF_UP));
                        model.setInvoiceTotalConsumption(model.getValue());
                    });

            discountFinalList.addAll(discountList);

        }
        return discountFinalList;
    }


    private List<BillingRunInvoiceDetailBaseModel> findGaps(List<BillingRunInvoiceDetailBaseModel> dateRanges, LocalDate limitStartDate, LocalDate limitEndDate, BigDecimal consumptionPerDay) {
        List<BillingRunInvoiceDetailBaseModel> gaps = new ArrayList<>();

        // Sort the date ranges by dateFrom
        dateRanges.sort(Comparator.comparing(Value::getDateFrom));

        // Check for gaps between the date ranges
        LocalDate currentStart = limitStartDate;
        for (BillingRunInvoiceDetailBaseModel range : dateRanges) {
            if (currentStart.isBefore(range.getDateFrom())) {
                LocalDate from = currentStart;
                LocalDate to = range.getDateFrom().minusDays(1);
                long calculatedDays = from.until(to, ChronoUnit.DAYS) + 1;
                BigDecimal totalVolumeConsumptionBetweenDates = consumptionPerDay
                        .multiply(new BigDecimal(calculatedDays))
                        .setScale(12, RoundingMode.HALF_UP);

                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
                model.setDateFrom(from);
                model.setDateTo(to.atStartOfDay());
                model.setValue(totalVolumeConsumptionBetweenDates.setScale(8, RoundingMode.HALF_UP));
                model.setValueBefore9DigitRounding(totalVolumeConsumptionBetweenDates.setScale(9, RoundingMode.HALF_UP));
                model.setDiscountId(range.getDiscountId());
                gaps.add(model);
            }
            if (currentStart.isBefore(range.getDateTo().toLocalDate())) {
                currentStart = range.getDateTo().toLocalDate().plusDays(1);
            }
        }

        // Check for a gap after the last date range
        if (currentStart.isBefore(limitEndDate)) {
            LocalDate from = currentStart;
            LocalDate to = limitEndDate;

            long calculatedDays = from.until(to, ChronoUnit.DAYS) + 1;

            BigDecimal totalVolumeConsumptionBetweenDates = consumptionPerDay
                    .multiply(new BigDecimal(calculatedDays))
                    .setScale(12, RoundingMode.HALF_UP);

            BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
            model.setDateFrom(from);
            model.setDateTo(to.atStartOfDay());
            model.setValue(totalVolumeConsumptionBetweenDates.setScale(8, RoundingMode.HALF_UP));
            model.setValueBefore9DigitRounding(totalVolumeConsumptionBetweenDates.setScale(9, RoundingMode.HALF_UP));

            gaps.add(model);
        }

        return gaps;
    }
}
