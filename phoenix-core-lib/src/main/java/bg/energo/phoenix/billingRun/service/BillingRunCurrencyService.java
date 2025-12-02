package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class BillingRunCurrencyService {

    private final CurrencyRepository currencyRepository;


    public BigDecimal convertToCurrency(Long fromCurrencyId, Long toCurrencyId, BigDecimal value,
                                        HashMap<Long, Currency> currencyHashMap) {
        Currency currencyFrom = currencyHashMap.get(fromCurrencyId);
        Currency currencyTo = currencyHashMap.get(toCurrencyId);

        if (currencyFrom == null) {
            Optional<Currency> currencyFromOptional = currencyRepository.findByIdAndStatus(fromCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyFromOptional.isEmpty())
                throw new RuntimeException("Cannot find active currency " + fromCurrencyId);
            currencyHashMap.put(fromCurrencyId, currencyFromOptional.get());
            currencyFrom = currencyFromOptional.get();
        }

        if (currencyTo == null) {
            Optional<Currency> currencyToOptional = currencyRepository.findByIdAndStatus(toCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyToOptional.isEmpty())
                throw new RuntimeException("Cannot find active currency " + toCurrencyId);
            currencyHashMap.put(toCurrencyId, currencyToOptional.get());
            currencyTo = currencyToOptional.get();
        }
        if (!currencyTo.getId().equals(currencyFrom.getId())) {

            Currency altCurrency = currencyHashMap.get(currencyFrom.getAltCurrencyId());
            if (altCurrency == null) {
                Optional<Currency> altCurrencyOptional = currencyRepository.findByIdAndStatus(toCurrencyId, List.of(NomenclatureItemStatus.ACTIVE));
                if (altCurrencyOptional.isEmpty())
                    throw new RuntimeException("Alternative currency not found in currency with id " + currencyFrom.getAltCurrencyId());
                currencyHashMap.put(currencyFrom.getAltCurrencyId(), altCurrencyOptional.get());
                altCurrency = altCurrencyOptional.get();
            }

            if (!altCurrency.getId().equals(currencyTo.getId())) {
                throw new RuntimeException("Alternative currency with id " + altCurrency.getId() + " is not currency for check: " + currencyTo.getId());
            }
            return value.multiply(currencyFrom.getAltCurrencyExchangeRate())
                    .setScale(12, RoundingMode.HALF_UP);
        }

        return value;
    }
}
