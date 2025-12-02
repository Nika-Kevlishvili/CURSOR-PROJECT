package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.*;
import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.model.entity.BillingRunSettlementPeriod;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunSettlementPeriodRepository;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunProcessSettlementService {

    private final BillingRunSettlementPeriodRepository billingRunSettlementPeriodRepository;
    private final BillingRunContractsRepository billingRunContractsRepository;
    private final BillingRunPriceComponentEvaluationService priceComponentEvaluationService;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final PriceComponentRepository priceComponentRepository;

    public Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> evaluateSettlement(BillingRunContracts runContract, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap, HashMap<Long, PriceComponent> priceComponentHashMap) {
        List<BillingRunSettlementPeriod> details = billingRunSettlementPeriodRepository.findAllByRunContractId(runContract.getId());
        Map<SlotGrouping, List<BillingRunInvoiceDetailBaseModel>> slotGrouping = new HashMap<>();

        for (BillingRunSettlementPeriod detail : details) {
            try {
                if (failedSlots.contains(detail.getBgInvoiceSlotId())) {
                    continue;
                }
                PriceComponent priceComponent = priceComponentHashMap.getOrDefault(detail.getPriceComponentId(), null);
                if (priceComponent == null) {
                    priceComponent = priceComponentRepository.findById(detail.getPriceComponentId()).get();
                    priceComponentHashMap.put(detail.getPriceComponentId(), priceComponent);
                }
                if (failedSlots.contains(detail.getBgInvoiceSlotId())) {
                    continue;
                }
                List<PriceComponentFormulaXValue> formulaXValues = priceComponentEvaluationService.getFormulaXValues(detail.getContractDetailId(), detail.getPriceComponentId(), xValueMap, runContract);
                List<ProfileValue> profileValues = EPBJsonUtils.readList(detail.getDataJson(), ProfileValue.class);

                BigDecimal totalConsumption = BigDecimal.ZERO;
                BigDecimal totalConsumptionBeforePercent = BigDecimal.ZERO;
                BigDecimal totalPrice = BigDecimal.ZERO;

                // Loop through each profile value
                for (ProfileValue profileValue : profileValues) {
                    totalConsumptionBeforePercent = totalConsumptionBeforePercent.add(profileValue.getV());

                    Map<String, Object> variablesContext = new HashMap<>();
                    for (PriceComponentFormulaXValue x : formulaXValues) {
                        variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
                    }

                    if (profileValue.getP() != null) {
                        // Loop through each price in formula
                        for (ProfilePrice price : profileValue.getP()) {
                            if (price.getB())
                                variablesContext.put("$PRICE_PROFILE$", price.getV());
                            else
                                variablesContext.put("$" + price.getP() + "$", price.getV());
                        }
                    }

                    BigDecimal price = priceComponentEvaluationService.evaluateExpression(detail.getPriceFormula(), variablesContext);
                    if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
                        price = price.negate();
                    }
                    price = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

                    BigDecimal consumptionAfterPercent;
                    if(!detail.getPercent().equals(BigDecimal.valueOf(100L))){
                        consumptionAfterPercent = profileValue.getV().multiply(
                                detail.getPercent().divide(BigDecimal.valueOf(100),12, RoundingMode.HALF_UP))
                                .setScale(8, RoundingMode.HALF_UP);
                    }else{
                        consumptionAfterPercent = profileValue.getV();
                    }
                    totalConsumption = totalConsumption.add(consumptionAfterPercent);
                    BigDecimal calculatedPrice = consumptionAfterPercent.multiply(price)
                            .setScale(12, RoundingMode.HALF_UP);

                    totalPrice = totalPrice.add(calculatedPrice);
                }


                if (detail.getScaleValueForCombined() != null && detail.getProfileTotalForCombined() != null) {
                    if (detail.getScaleValueForCombined().compareTo(detail.getProfileTotalForCombined()) != 0) {
                        BigDecimal totalConsumptionCombine = totalConsumptionBeforePercent
                                .divide(detail.getProfileTotalForCombined(),12, RoundingMode.HALF_UP)
                                .multiply(detail.getScaleValueForCombined())
                                .setScale(12, RoundingMode.HALF_UP)
                                .divide(BigDecimal.valueOf(100),12, RoundingMode.HALF_UP)
                                .multiply(detail.getPercent())
                                .setScale(8, RoundingMode.HALF_UP);
                        BigDecimal avgPrice = totalConsumption.compareTo(BigDecimal.ZERO)==0? BigDecimal.ZERO : totalPrice.divide(totalConsumption,12, RoundingMode.HALF_UP);
                        totalPrice = totalConsumptionCombine.multiply(avgPrice).setScale(12, RoundingMode.HALF_UP);
                        totalConsumption = totalConsumptionCombine;
                    }
                }

                detail.setRunTotalPrice(totalPrice);
                detail.setRunTotalConsumption(totalConsumption);

            } catch (Exception e) {
                log.error("Exception in billing run settlement evaluation  BgInvoiceSlotId: [%s], PcId: [%s] PodId: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPodId()), e);
                billingRunContractsRepository.updateBgInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                        "settlement evaluation error, BgInvoiceSlotId: [%s] BillingRunSettlementPeriodId: [%s] Pod: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPodId(), e.getMessage()));
                failedSlots.add(detail.getBgInvoiceSlotId());
            }
        }

        // Group by settlement contract version
        Map<SettlementContractVersionGrouping, List<BillingRunSettlementPeriod>> groupByVersion = details.stream()
                .collect(Collectors.groupingBy(SettlementContractVersionGrouping::new));

        for (Map.Entry<SettlementContractVersionGrouping, List<BillingRunSettlementPeriod>> group : groupByVersion.entrySet()) {
            BigDecimal totalConsumption = BigDecimal.ZERO;
            BigDecimal totalPrice = BigDecimal.ZERO;
            BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel();
            model.setBillingDataProfileIds(new Long[0]);
            for (BillingRunSettlementPeriod row : group.getValue()) {
                totalConsumption = totalConsumption.add(row.getRunTotalConsumption());
                totalPrice = totalPrice.add(row.getRunTotalPrice());
                model.setBillingDataProfileIds(Stream.concat(Arrays.stream(model.getBillingDataProfileIds()),
                                Arrays.stream(row.getBillingDataProfileIds())).distinct()
                        .toArray(Long[]::new));

                model.setBillingDataScaleIds( Stream.concat(
                                Arrays.stream(model.getBillingDataScaleIds() != null ? model.getBillingDataScaleIds() : new Long[0]),
                                Arrays.stream(row.getBillingDataScaleIds() != null ? row.getBillingDataScaleIds() : new Long[0])
                        )
                        .distinct() // Ensure all IDs are unique
                        .toArray(Long[]::new));
            }

            BillingRunSettlementPeriod period = group.getValue().get(0);
            PriceComponent priceComponent = priceComponentHashMap.getOrDefault(period.getPriceComponentId(), null);
            if (priceComponent == null) {
                priceComponent = priceComponentRepository.findById(period.getPriceComponentId()).get();
                priceComponentHashMap.put(period.getPriceComponentId(), priceComponent);
            }

            model.setType(InvoiceStandardDetailType.SETTLEMENT);
            model.setCalculatedPrice(totalPrice);
            model.setCalculatedVolumes(totalConsumption);
            model.setDateFrom(period.getDateFrom().toLocalDate());
            model.setDateTo(period.getDateTo());
            model.setPcId(period.getPriceComponentId());
            model.setPcGroupId(period.getPcGroupId());
            model.setPodId(period.getPodId());
            model.setPodDetailId(period.getPodDetailId());
            model.setBgInvoiceSlotId(period.getBgInvoiceSlotId());
            model.setCustomerId(period.getCustomerId());
            model.setCustomerDetailId(period.getCustomerDetailId());
            model.setContractDetailId(period.getContractDetailId());
            model.setKwhPrice(totalConsumption.compareTo(BigDecimal.ZERO)==0? BigDecimal.ZERO: totalPrice.divide(totalConsumption,12, RoundingMode.HALF_UP));

            model.setProductDetailId(period.getProductDetailId());
            model.setPriceComponentPriceTypeId(period.getPriceComponentPriceTypeId());

            SlotGrouping slot = new SlotGrouping(model.getBgInvoiceSlotId(), model.getPodId(), model.getPcId());
            slotGrouping.computeIfAbsent(slot, k -> new ArrayList<>()).add(model);
        }

        return slotGrouping;
    }
}
