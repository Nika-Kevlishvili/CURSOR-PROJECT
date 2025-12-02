package bg.energo.phoenix.service.billing.runs.services.restriction;

import bg.energo.phoenix.service.billing.runs.models.restriction.PriceComponentOptions;
import bg.energo.phoenix.service.billing.runs.models.restriction.RestrictionModelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRestrictionByPercentService {
    public void calculatePercentRestriction(PriceComponentOptions priceComponentOptions, List<RestrictionModelDto> dtoList) {
        if (BooleanUtils.isNotTrue(priceComponentOptions.getIsKwtRestrictionSelected())) {
            return;
        }
        if (priceComponentOptions.getRestrictionPercent() != null) {
            BigDecimal totalRestrictionByPercent = calculatePercentOfSum(priceComponentOptions.getRestrictionPercent(), dtoList);
            //only for testing save total percent value TODO remove in final version
            dtoList.forEach(dto -> dto.setTotalPercentRestrictionByPriceComponent(totalRestrictionByPercent));
            restrictTotalValuesByPercent(dtoList, totalRestrictionByPercent);
        }
    }

    private BigDecimal calculatePercentOfSum(BigDecimal percent, List<RestrictionModelDto> dtoList) {
        BigDecimal sum = dtoList.stream()
                .map(RestrictionModelDto::getTotalVolumes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void restrictTotalValuesByPercent(List<RestrictionModelDto> dtoList, BigDecimal totalRestrictionByPercent) {
        BigDecimal subtractedPercentRestrictionValue = totalRestrictionByPercent;
        for (RestrictionModelDto row : dtoList) {
            subtractedPercentRestrictionValue = compareAndSetPercentRestrictedValueToRow(subtractedPercentRestrictionValue, row);
        }
    }

    private BigDecimal compareAndSetPercentRestrictedValueToRow(BigDecimal totalRestrictionValue, RestrictionModelDto row) {
        switch (row.getTotalVolumes().compareTo(totalRestrictionValue)) {
            case -1, 0 -> {
                row.setVolumesOfPercentageRestriction(row.getTotalVolumes());
                row.setAmountOfPercentageRestriction(row.getTotalVolumes().multiply(row.getPrice()));
                return totalRestrictionValue.subtract(row.getTotalVolumes());
            }
            case 1 -> {
                row.setVolumesOfPercentageRestriction(totalRestrictionValue);
                row.setAmountOfPercentageRestriction(totalRestrictionValue.multiply(row.getPrice()));
                return totalRestrictionValue.subtract(totalRestrictionValue);
            }
        }
        return BigDecimal.ZERO;
    }
}
