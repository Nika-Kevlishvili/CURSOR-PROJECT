package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingRunInvoiceDetailBaseModel;
import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXMap;
import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXValue;
import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimeOneTime;
import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimePeriodical;
import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimeWithElectricity;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunOverTimeOneTimeRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunOverTimePeriodicalRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunOverTimeWithElectricityRepository;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunProcessingOverTimeService {
    private final BillingRunContractsRepository billingRunContractsRepository;
    private final BillingRunPriceComponentEvaluationService priceComponentEvaluationService;
    private final BillingRunOverTimeOneTimeRepository billingRunOverTimeOneTimeRepository;
    private final BillingRunOverTimePeriodicalRepository billingRunOverTimePeriodicalRepository;
    private final BillingRunCurrencyService billingRunCurrencyService;
    private final PriceComponentRepository priceComponentRepository;
    private final BillingRunOverTimeWithElectricityRepository billingRunOverTimeWithElectricityRepository;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;

    public List<BillingRunInvoiceDetailBaseModel> evaluateOverTimeOneTime(LocalDate invoiceDate,BillingRunContracts runContract, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap, HashMap<Long, PriceComponent> priceComponentHashMap) {
        List<BillingRunInvoiceDetailBaseModel> invoiceDetails = new ArrayList<>();
        String regex = "\\$(\\d+)\\$";

        List<BillingRunOverTimeOneTime> details = billingRunOverTimeOneTimeRepository.findAllByRunContractId(runContract.getId());
        for (BillingRunOverTimeOneTime detail : details) {
            try {
                if (failedSlots.contains(detail.getBgInvoiceSlotId())) {
                    continue;
                }
                Map<String, Object> variablesContext = new HashMap<>();
                List<PriceComponentFormulaXValue> formulaXValues = priceComponentEvaluationService.getFormulaXValues(detail.getContractDetailId(), detail.getPriceComponentId(), xValueMap, runContract);

                for (PriceComponentFormulaXValue x : formulaXValues) {
                    variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(detail.getPriceFormula());
                List<String> matches = new ArrayList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(1));
                }
                if (!matches.isEmpty()) {
                    for (String part : matches) {
                        extractPriceParameterPrice(invoiceDate, variablesContext, part);
                    }
                }

                BigDecimal price = priceComponentEvaluationService.evaluateExpression(detail.getPriceFormula(), variablesContext);

                PriceComponent priceComponent = priceComponentHashMap.getOrDefault(detail.getPriceComponentId(), null);
                if (priceComponent == null) {
                    priceComponent = priceComponentRepository.findById(detail.getPriceComponentId()).get();
                    priceComponentHashMap.put(detail.getPriceComponentId(), priceComponent);
                }
                if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
                    price = price.negate();
                }
                detail.setRunTotalPrice(
                        billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap)
                );

                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel(InvoiceStandardDetailType.OVER_TIME_ONE_TIME,
                        detail.getBgInvoiceSlotId(),
                        detail.getContractDetailId(),
                        detail.getRunTotalPrice(),
                        detail.getPriceComponentId());
                model.setPcGroupId(detail.getPcGroupId());
                model.setPodId(detail.getPodId());
                model.setUnrecognizedPod(detail.getUnrecognizedPod());
                model.setPriceComponentPriceTypeId(detail.getPriceComponentPriceTypeId());

                model.setCustomerDetailId(detail.getCustomerDetailId());
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    model.setProductDetailId(detail.getProductDetailId());
                } else {
                    model.setServiceDetailId(detail.getProductDetailId());
                }
                invoiceDetails.add(model);
            } catch (Exception e) {
                log.error("Exception in billing run over time periodical evaluation  BgInvoiceSlotId: [%s], PcId: [%s] PC_Id: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId()), e);
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    billingRunContractsRepository.updateBgInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time periodical evaluation error, BgInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                } else {
                    billingRunContractsRepository.updateSvInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time periodical evaluation error, SvInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                }
                failedSlots.add(detail.getBgInvoiceSlotId());
            }
        }
        return invoiceDetails;
    }

    private void extractPriceParameterPrice(LocalDate invoiceDate, Map<String, Object> variablesContext, String part) {
        LocalDateTime startDayOfMonth = invoiceDate.withDayOfMonth(1).atStartOfDay();
        BigDecimal price;
        PriceParameterDetailInfo ppd = priceParameterDetailInfoRepository.findByParameterIdAndTypeAndExactDate(Long.parseLong(part), startDayOfMonth, PeriodType.ONE_MONTH);
        if (ppd != null && ppd.getPrice() != null) {
            price = ppd.getPrice();
        } else {
            PriceParameterDetailInfo ppd2 = priceParameterDetailInfoRepository.findByParameterIdAndTypeAndExactDate(Long.parseLong(part), invoiceDate.atStartOfDay(), PeriodType.ONE_DAY);
            if (ppd2 != null && ppd2.getPrice() != null) {
                price = ppd2.getPrice();
            } else {
                // Mark split as error and handle accordingly
                throw new RuntimeException("Applicable price parameter not found for part: " + part + " and date: " + startDayOfMonth);
            }
        }
        variablesContext.put("$" + part + "$", price);
    }

    public List<BillingRunInvoiceDetailBaseModel> evaluateOverTimePeriodical(LocalDate invoiceDate, BillingRunContracts runContract, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap, HashMap<Long, PriceComponent> priceComponentHashMap) {
        List<BillingRunInvoiceDetailBaseModel> invoiceDetails = new ArrayList<>();
        List<BillingRunOverTimePeriodical> details = billingRunOverTimePeriodicalRepository.findAllByRunContractId(runContract.getId());
        String regex = "\\$(\\d+)\\$";

        for (BillingRunOverTimePeriodical detail : details) {
            try {
                if (failedSlots.contains(detail.getBgInvoiceSlotId())) {
                    continue;
                }
                Map<String, Object> variablesContext = new HashMap<>();
                List<PriceComponentFormulaXValue> formulaXValues = priceComponentEvaluationService.getFormulaXValues(detail.getContractDetailId(), detail.getPriceComponentId(), xValueMap, runContract);

                for (PriceComponentFormulaXValue x : formulaXValues) {
                    variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(detail.getPriceFormula());
                List<String> matches = new ArrayList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(1));
                }
                if (!matches.isEmpty()) {
                    for (String part : matches) {
                        extractPriceParameterPrice(invoiceDate, variablesContext, part);
                    }
                }

                BigDecimal price = priceComponentEvaluationService.evaluateExpression(detail.getPriceFormula(), variablesContext);
                PriceComponent priceComponent = priceComponentHashMap.getOrDefault(detail.getPriceComponentId(), null);
                if (priceComponent == null) {
                    priceComponent = priceComponentRepository.findById(detail.getPriceComponentId()).get();
                    priceComponentHashMap.put(detail.getPriceComponentId(), priceComponent);
                }
                if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
                    price = price.negate();
                }
                BigDecimal finalPrice = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);

                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel(InvoiceStandardDetailType.OVER_TIME_PERIODICAL,
                        detail.getBgInvoiceSlotId(),
                        detail.getContractDetailId(),
                        finalPrice,
                        detail.getPriceComponentId());
                model.setPriceComponentPriceTypeId(detail.getPriceComponentPriceTypeId());
                model.setPcGroupId(detail.getPcGroupId());
                model.setPodId(detail.getPodId());
                model.setUnrecognizedPod(detail.getUnrecognizedPod());
                model.setCustomerDetailId(detail.getCustomerDetailId());
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    model.setProductDetailId(detail.getProductDetailId());
                } else {
                    model.setServiceDetailId(detail.getProductDetailId());
                }
                invoiceDetails.add(model);
            } catch (Exception e) {
                log.error("Exception in billing run over time periodical evaluation  BgInvoiceSlotId: [%s], PcId: [%s] PC_Id: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId()), e);
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    billingRunContractsRepository.updateBgInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time periodical evaluation error, BgInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                } else {
                    billingRunContractsRepository.updateSvInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time periodical evaluation error, SvInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                }
                failedSlots.add(detail.getBgInvoiceSlotId());
            }
        }
        return invoiceDetails;
    }

    public List<BillingRunInvoiceDetailBaseModel> evaluateOverTimeWithElectricity(LocalDate invoiceDate,BillingRunContracts runContract, HashSet<Long> failedSlots, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValueMap, Long mainCurrencyId, HashMap<Long, Currency> currencyHashMap, HashMap<Long, PriceComponent> priceComponentHashMap) {
        List<BillingRunInvoiceDetailBaseModel> invoiceDetails = new ArrayList<>();
        List<BillingRunOverTimeWithElectricity> details = billingRunOverTimeWithElectricityRepository.findAllByRunContractId(runContract.getId());
        String regex = "\\$(\\d+)\\$";
        for (BillingRunOverTimeWithElectricity detail : details) {
            try {
                if (failedSlots.contains(detail.getBgInvoiceSlotId())) {
                    continue;
                }
                if (Objects.equals(detail.getHasInvalidPriceFormula(), Boolean.TRUE)) {
                    throw new IllegalArgumentException("price formula contains price profile or price parameter");
                }
                Map<String, Object> variablesContext = new HashMap<>();
                List<PriceComponentFormulaXValue> formulaXValues = priceComponentEvaluationService.getFormulaXValues(detail.getContractDetailId(), detail.getPriceComponentId(), xValueMap, runContract);

                for (PriceComponentFormulaXValue x : formulaXValues) {
                    variablesContext.put("$" + x.getKey() + "$", new BigDecimal(x.getValue()));
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(detail.getPriceFormula());
                List<String> matches = new ArrayList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(1));
                }
                if (!matches.isEmpty()) {
                    for (String part : matches) {
                        extractPriceParameterPrice(invoiceDate, variablesContext, part);
                    }
                }

                BigDecimal price = priceComponentEvaluationService.evaluateExpression(detail.getPriceFormula(), variablesContext);

                PriceComponent priceComponent = priceComponentHashMap.getOrDefault(detail.getPriceComponentId(), null);
                if (priceComponent == null) {
                    priceComponent = priceComponentRepository.findById(detail.getPriceComponentId()).get();
                    priceComponentHashMap.put(detail.getPriceComponentId(), priceComponent);
                }
                if(priceComponent.getNumberType().equals(NumberType.NEGATIVE)){
                    price = price.negate();
                }
                BigDecimal finalPrice = billingRunCurrencyService.convertToCurrency(priceComponent.getCurrencyId(), mainCurrencyId, price, currencyHashMap);


                BillingRunInvoiceDetailBaseModel model = new BillingRunInvoiceDetailBaseModel(InvoiceStandardDetailType.WITH_ELECTRICITY,
                        detail.getBgInvoiceSlotId(),
                        detail.getContractDetailId(),
                        finalPrice,
                        detail.getPriceComponentId());
                model.setPcGroupId(detail.getPcGroupId());
                model.setPriceComponentPriceTypeId(detail.getPriceComponentPriceTypeId());

                model.setPodId(detail.getPodId());
                model.setCustomerDetailId(detail.getCustomerDetailId());
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    model.setProductDetailId(detail.getProductDetailId());
                } else {
                    model.setServiceDetailId(detail.getProductDetailId());
                }
                invoiceDetails.add(model);
            } catch (Exception e) {
                log.error("Exception in billing run over time with electricity evaluation  BgInvoiceSlotId: [%s], PcId: [%s] PC_Id: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId()), e);
                if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                    billingRunContractsRepository.updateBgInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time with electricity evaluation error, BgInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                } else {
                    billingRunContractsRepository.updateSvInvoiceSlotStatus(detail.getBgInvoiceSlotId(), "ERROR",
                            " over time with electricity evaluation error, SvInvoiceSlotId: [%s] over_time_periodical_id: [%s] PC_Id: [%s] ExceptionMessage: [%s]".formatted(detail.getBgInvoiceSlotId(), detail.getId(), detail.getPriceComponentId(), e.getMessage()));
                }
                failedSlots.add(detail.getBgInvoiceSlotId());
            }
        }
        return invoiceDetails;
    }
}
