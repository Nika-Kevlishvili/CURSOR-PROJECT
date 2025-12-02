package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingRunInvoiceDetailBaseModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BillingRunRestrictionPercentService {

    public void calculate(List<BillingRunInvoiceDetailBaseModel> values, BigDecimal percentRestriction) {
        BigDecimal percentSum = BigDecimal.ZERO;
        for (BillingRunInvoiceDetailBaseModel value : values) {
            percentSum = percentSum.add(value.getCalculatedVolumes());
        }

        percentSum = percentSum.multiply(percentRestriction)
                .setScale(12, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal finalPercentSum = percentSum;
        BigDecimal subtractedPercentRestrictionValue = finalPercentSum;
        for (BillingRunInvoiceDetailBaseModel row : values) {
            row.setTotalPercentRestrictionByPriceComponent(finalPercentSum);
            subtractedPercentRestrictionValue = compareAndSetPercentRestrictedValueToRow(subtractedPercentRestrictionValue, row);
        }
    }

    private BigDecimal compareAndSetPercentRestrictedValueToRow(BigDecimal totalRestrictionValue, BillingRunInvoiceDetailBaseModel row) {
        BigDecimal calculatedVolumes = row.getCalculatedVolumes();
        if (calculatedVolumes.compareTo(totalRestrictionValue) <= 0) {
            row.setVolumesOfPercentageRestriction(calculatedVolumes);
            row.setAmountOfPercentageRestriction(calculatedVolumes.multiply(row.getKwhPrice())
                    .setScale(12, RoundingMode.HALF_UP));
            return totalRestrictionValue.subtract(calculatedVolumes);
        } else {
            row.setVolumesOfPercentageRestriction(totalRestrictionValue);
            row.setAmountOfPercentageRestriction(totalRestrictionValue.multiply(row.getKwhPrice())
                    .setScale(12, RoundingMode.HALF_UP));
            return BigDecimal.ZERO;
        }
    }
}
