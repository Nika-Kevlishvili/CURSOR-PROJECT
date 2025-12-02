package bg.energo.phoenix.service.billing.runs.services.restriction;

import bg.energo.phoenix.service.billing.runs.models.restriction.KwhRestriction;
import bg.energo.phoenix.service.billing.runs.models.restriction.PriceComponentOptions;
import bg.energo.phoenix.service.billing.runs.models.restriction.RestrictionModelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRestrictionKwhService {

    public void calculateKwhRestriction(PriceComponentOptions priceComponentOptions, List<RestrictionModelDto> dtoList) {
        if (BooleanUtils.isNotTrue(priceComponentOptions.getIsKwtRestrictionSelected())) {
            return;
        }
        if (CollectionUtils.isEmpty(priceComponentOptions.getKwhRestrictions())) {
            return;
        }
        List<KwhRestriction> kwhRestrictions = priceComponentOptions.getKwhRestrictions();

        for (int i = 0; i < dtoList.size(); i++) {
            calculateRowForInterval(dtoList, kwhRestrictions, i);
        }
    }

    private void calculateRowForInterval(List<RestrictionModelDto> dtoList, List<KwhRestriction> kwhRestrictions, int rowIndex) {
        BigDecimal totalVolumesOfPreviousRecords = calculatePreviousTotalVolumes(rowIndex, dtoList);
        BigDecimal totalRestriction = BigDecimal.ZERO;
        for (KwhRestriction interval : kwhRestrictions) {
            BigDecimal maxValueOfInterval = BigDecimal.valueOf(interval.getValueFrom()).max(BigDecimal.valueOf(interval.getValueTo()));
            BigDecimal minValueOfInterval = BigDecimal.valueOf(interval.getValueFrom()).min(BigDecimal.valueOf(interval.getValueTo()));
            //first iteration
            BigDecimal first = totalVolumesOfPreviousRecords.add(dtoList.get(rowIndex).getTotalVolumes()).min(maxValueOfInterval);
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

    private BigDecimal calculatePreviousTotalVolumes(int rowIndex, List<RestrictionModelDto> dtoList) {
        BigDecimal totalVolumesOfPreviousRecords = BigDecimal.ZERO;
        for (int i = 0; i < rowIndex; i++) {
            totalVolumesOfPreviousRecords = totalVolumesOfPreviousRecords.add(dtoList.get(i).getTotalVolumes());
        }
        return totalVolumesOfPreviousRecords;
    }

    private void fillRowRestrictionIntoResponse(List<RestrictionModelDto> dtoList, BigDecimal totalRowRestriction, int rowIndex) {
        dtoList.get(rowIndex).setVolumesOfKwhRestriction(totalRowRestriction);
        dtoList.get(rowIndex).setAmountOfKwhRestriction(totalRowRestriction.multiply(dtoList.get(rowIndex).getPrice()));
    }

}
