package bg.energo.phoenix.service.billing.runs.services.restriction;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.service.billing.runs.models.restriction.CcyRestrictions;
import bg.energo.phoenix.service.billing.runs.models.restriction.PriceComponentOptions;
import bg.energo.phoenix.service.billing.runs.models.restriction.RestrictionModelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRestrictionCcyService {
    private final CurrencyRepository currencyRepository;

    public void calculateCcyRestriction(PriceComponentOptions priceComponentOptions, List<RestrictionModelDto> dtoList, List<String> exceptionMessages) {
        if (BooleanUtils.isNotTrue(priceComponentOptions.getIsCcyRestrictionSelected())) {
            return;
        }
        if (CollectionUtils.isEmpty(priceComponentOptions.getCcyRestrictions())) {
            return;
        }
        List<CcyRestrictions> ccyRestrictions = priceComponentOptions.getCcyRestrictions();
        //find Main currency
        Optional<Currency> currencyOptional = currencyRepository.findMainCurrencyNowAndActive();
        if (currencyOptional.isEmpty()) {
            exceptionMessages.add("Main currency not found;");
            return;
        }
        Currency mainCurrency = currencyOptional.get();
        //validate dto list if all currencies is main
        if (!validateDtoListOnMainCurrency(mainCurrency, dtoList, exceptionMessages)) {
            return;
        }
        //validate and exchange currencies in ccy intervals
        if (!exchangeCurrencies(ccyRestrictions, exceptionMessages, mainCurrency)) {
            return;
        }
        calculateTotalAmounts(dtoList);
        for (int i = 0; i < dtoList.size(); i++) {
            calculateRowForInterval(dtoList, ccyRestrictions, i);
        }
    }

    private void calculateTotalAmounts(List<RestrictionModelDto> dtoList) {
        dtoList.forEach(dto -> dto.setTotalAmount(dto.getPrice().multiply(dto.getTotalVolumes())));
    }

    private void calculateRowForInterval(List<RestrictionModelDto> dtoList, List<CcyRestrictions> ccyRestrictions, int rowIndex) {
        //TODO check main currency, exchange (intervals) if currency from price component is different
        BigDecimal totalVolumesOfPreviousRecords = calculatePreviousTotalAmount(rowIndex, dtoList);
        BigDecimal totalRestriction = BigDecimal.ZERO;
        for (CcyRestrictions interval : ccyRestrictions) {
            BigDecimal maxValueOfInterval = interval.getValueFrom().max(interval.getValueTo());
            BigDecimal minValueOfInterval = interval.getValueFrom().min(interval.getValueTo());
            //first iteration
            BigDecimal first = totalVolumesOfPreviousRecords.add(dtoList.get(rowIndex).getTotalAmount()).min(maxValueOfInterval);
            //second iteration
            BigDecimal second = minValueOfInterval.max(totalVolumesOfPreviousRecords);
            //third iteration
            if (first.compareTo(second) > 0) {
//               totalRestriction + first-second+1
                totalRestriction = totalRestriction.add(first).subtract(second).add(BigDecimal.ONE);
            } else {
                totalRestriction = totalRestriction.add(BigDecimal.ZERO);
            }
        }
        fillRowRestrictionIntoResponse(dtoList, totalRestriction, rowIndex);
    }

    private BigDecimal calculatePreviousTotalAmount(int rowIndex, List<RestrictionModelDto> dtoList) {
        BigDecimal totalVolumesOfPreviousRecords = BigDecimal.ZERO;
        for (int i = 0; i < rowIndex; i++) {
            totalVolumesOfPreviousRecords = totalVolumesOfPreviousRecords.add(dtoList.get(i).getTotalAmount());
        }
        return totalVolumesOfPreviousRecords;
    }

    private void fillRowRestrictionIntoResponse(List<RestrictionModelDto> dtoList, BigDecimal totalRowRestriction, int rowIndex) {
        dtoList.get(rowIndex).setAmountOfCcyRestriction(totalRowRestriction);
        dtoList.get(rowIndex).setVolumesOfCcyRestriction(totalRowRestriction.divide(dtoList.get(rowIndex).getPrice(), 2, RoundingMode.HALF_UP));
    }

    private boolean validateDtoListOnMainCurrency(Currency mainCurrency, List<RestrictionModelDto> dtoList, List<String> exceptionMessages) {
        //validate list on valid main currency
        for (RestrictionModelDto dto : dtoList) {
            if (!dto.getCurrencyId().equals(mainCurrency.getId())) {
                exceptionMessages.add("Invalid currency in dtoList;");
                return false;
            }
        }
        return true;
    }

    private boolean exchangeCurrencies(List<CcyRestrictions> ccyRestrictions, List<String> exceptionMessages, Currency mainCurrency) {
        Long intervalCurrencyId = ccyRestrictions.get(0).getCurrencyId();
        if (intervalCurrencyId.equals(mainCurrency.getId())) {
            return true;
        }
        if (!validateExchangeCurrencies(mainCurrency, exceptionMessages, intervalCurrencyId)) {
            return false;
        }
        Optional<Currency> intervalCurrency = currencyRepository.findByIdAndStatus(intervalCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
        if (intervalCurrency.isEmpty()) {
            exceptionMessages.add("Interval currency with id %s not found;".formatted(intervalCurrencyId));
            return false;
        }
        BigDecimal exchangeRate = intervalCurrency.get().getAltCurrency().getAltCurrencyExchangeRate();
        //exchange intervals
        ccyRestrictions.forEach(interval -> {
            interval.setValueFrom(interval.getValueFrom().multiply(exchangeRate));
            interval.setValueTo(interval.getValueTo().multiply(exchangeRate));
        });

        return true;
    }

    private boolean validateExchangeCurrencies(Currency mainCurrency, List<String> exceptionMessages, Long intervalCurrencyId) {
        Optional<Currency> intervalCurrencyOptional = currencyRepository.findByIdAndStatus(intervalCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
        if (intervalCurrencyOptional.isEmpty()) {
            exceptionMessages.add("Interval currency with id %s not found;".formatted(intervalCurrencyId));
            return false;
        }
        Currency intervalCurrency = intervalCurrencyOptional.get();
        if (intervalCurrency.getAltCurrency() == null) {
            exceptionMessages.add("Alternative currency not found in currency with id %s;".formatted(intervalCurrency.getId()));
            return false;
        }
        if (!intervalCurrency.getAltCurrency().getId().equals(mainCurrency.getId())) {
            exceptionMessages.add("Alternative currency with id %s is not main;".formatted(intervalCurrency.getAltCurrency().getId()));
            return false;
        }
        return true;
    }
}
