package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingRunInvoiceDetailBaseModel;
import bg.energo.phoenix.billingRun.model.CcyRestrictions;
import bg.energo.phoenix.billingRun.model.CcyRestrictionsImpl;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunRestrictionCcyService {

    private final CurrencyRepository currencyRepository;
    private final BillingRunCurrencyService billingRunCurrencyService;

    public void calculate(List<CcyRestrictions> ccyRestrictions, List<BillingRunInvoiceDetailBaseModel> dtoList, PriceComponent priceComponent, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId, Long altCurrencyId) {

        List<CcyRestrictionsImpl> ccyRestrictionsList = validateCurrencyAndTransform(ccyRestrictions, currencyHashMap, mainCurrencyId, altCurrencyId);

        BigDecimal totalAmounts = calculateTotalAmounts(dtoList);
        for (int i = 0; i < dtoList.size(); i++) {
            calculateRowForInterval(dtoList, ccyRestrictionsList, i, totalAmounts);
        }
    }

    private List<CcyRestrictionsImpl> validateCurrencyAndTransform(List<CcyRestrictions> ccyRestrictions, HashMap<Long, Currency> currencyHashMap, Long mainCurrencyId, Long altCurrencyId) {
        // Validate currencies and transform restrictions
        List<CcyRestrictionsImpl> ccyRestrictionsList = new ArrayList<>();
        for (CcyRestrictions r : ccyRestrictions) {
            Long currencyId = r.getCurrencyId();
            if (mainCurrencyId.equals(currencyId)) {
                ccyRestrictionsList.add(new CcyRestrictionsImpl(r.getValueFrom(), r.getValueTo(), currencyId));
            } else {
                BigDecimal newValueFrom = billingRunCurrencyService.convertToCurrency(currencyId, mainCurrencyId, r.getValueFrom(), currencyHashMap);
                BigDecimal newValueTo = billingRunCurrencyService.convertToCurrency(currencyId, mainCurrencyId, r.getValueTo(), currencyHashMap);
                ccyRestrictionsList.add(new CcyRestrictionsImpl(newValueFrom, newValueTo, currencyId));
            }
        }
        return ccyRestrictionsList;
    }

    private BigDecimal calculateTotalAmounts(List<BillingRunInvoiceDetailBaseModel> dtoList) {
        BigDecimal totalAmounts = BigDecimal.ZERO;
        for (BillingRunInvoiceDetailBaseModel dto : dtoList) {
            totalAmounts = totalAmounts.add(dto.getCalculatedPrice());
        }
        return totalAmounts;
    }

    private void calculateRowForInterval(List<BillingRunInvoiceDetailBaseModel> dtoList, List<CcyRestrictionsImpl> ccyRestrictions, int rowIndex, BigDecimal totalAmounts) {
        BigDecimal totalRestriction = BigDecimal.ZERO;
        BigDecimal totalVolumesOfPreviousRecords = calculatePreviousTotalAmount(rowIndex, dtoList);
        for (CcyRestrictionsImpl interval : ccyRestrictions) {
            BigDecimal maxValueOfInterval = interval.getValueFrom().max(interval.getValueTo());
            BigDecimal minValueOfInterval = interval.getValueFrom().min(interval.getValueTo());
            BigDecimal first = totalVolumesOfPreviousRecords.add(dtoList.get(rowIndex).getCalculatedPrice()).min(maxValueOfInterval);
            BigDecimal second = minValueOfInterval.max(totalVolumesOfPreviousRecords);
            totalRestriction = totalRestriction.add(first.max(BigDecimal.ZERO).subtract(second.min(maxValueOfInterval)).add(BigDecimal.ONE));
        }
        fillRowRestrictionIntoResponse(dtoList, totalRestriction, rowIndex, totalAmounts);
    }

    private BigDecimal calculatePreviousTotalAmount(int rowIndex, List<BillingRunInvoiceDetailBaseModel> dtoList) {
        BigDecimal totalVolumesOfPreviousRecords = BigDecimal.ZERO;
        for (int i = 0; i < rowIndex; i++) {
            totalVolumesOfPreviousRecords = totalVolumesOfPreviousRecords.add(dtoList.get(i).getCalculatedPrice());
        }
        return totalVolumesOfPreviousRecords;
    }

    private void fillRowRestrictionIntoResponse(List<BillingRunInvoiceDetailBaseModel> dtoList, BigDecimal totalRowRestriction, int rowIndex, BigDecimal totalAmounts) {
        dtoList.get(rowIndex).setAmountOfCcyRestriction(totalRowRestriction);
        dtoList.get(rowIndex).setVolumesOfCcyRestriction(totalRowRestriction.divide(dtoList.get(rowIndex).getKwhPrice(), 8, RoundingMode.HALF_UP));
    }
}

