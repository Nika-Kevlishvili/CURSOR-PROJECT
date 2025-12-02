package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.BillingRunInvoiceDetailBaseModel;
import bg.energo.phoenix.billingRun.model.KwhRestrictions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunRestrictionKwhService {

    public void calculate(List<BillingRunInvoiceDetailBaseModel> dtoList, List<KwhRestrictions> kwhRestrictions) {
        for (int i = 0; i < dtoList.size(); i++) {
            calculateRowForInterval(dtoList, kwhRestrictions, i);
        }
    }

    private void calculateRowForInterval(List<BillingRunInvoiceDetailBaseModel> dtoList, List<KwhRestrictions> kwhRestrictions, int rowIndex) {
        BigDecimal totalVolumesOfPreviousRecords = calculatePreviousTotalVolumes(rowIndex, dtoList);
        BigDecimal totalRestriction = BigDecimal.ZERO;
        for (KwhRestrictions interval : kwhRestrictions) {
            BigDecimal maxValueOfInterval = BigDecimal.valueOf(interval.getValueFrom()).max(BigDecimal.valueOf(interval.getValueTo()));
            BigDecimal minValueOfInterval = BigDecimal.valueOf(interval.getValueFrom()).min(BigDecimal.valueOf(interval.getValueTo()));
            BigDecimal first = totalVolumesOfPreviousRecords.add(dtoList.get(rowIndex).getCalculatedVolumes()).min(maxValueOfInterval);
            BigDecimal second = minValueOfInterval.max(totalVolumesOfPreviousRecords);
            totalRestriction = totalRestriction.add(first.max(BigDecimal.ZERO).subtract(second.min(maxValueOfInterval)).add(BigDecimal.ONE));
        }
        fillRowRestrictionIntoResponse(dtoList, totalRestriction, rowIndex);
    }

    private BigDecimal calculatePreviousTotalVolumes(int rowIndex, List<BillingRunInvoiceDetailBaseModel> dtoList) {
        BigDecimal totalVolumesOfPreviousRecords = BigDecimal.ZERO;
        for (int i = 0; i < rowIndex; i++) {
            totalVolumesOfPreviousRecords = totalVolumesOfPreviousRecords.add(dtoList.get(i).getCalculatedVolumes());
        }
        return totalVolumesOfPreviousRecords;
    }

    private void fillRowRestrictionIntoResponse(List<BillingRunInvoiceDetailBaseModel> dtoList, BigDecimal totalRowRestriction, int rowIndex) {
        dtoList.get(rowIndex).setVolumesOfKwhRestriction(totalRowRestriction);
        dtoList.get(rowIndex).setAmountOfKwhRestriction(totalRowRestriction.multiply(dtoList.get(rowIndex).getKwhPrice())
                .setScale(12, RoundingMode.HALF_UP));
    }
}
