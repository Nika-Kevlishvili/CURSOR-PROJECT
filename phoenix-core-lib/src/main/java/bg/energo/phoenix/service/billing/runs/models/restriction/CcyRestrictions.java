package bg.energo.phoenix.service.billing.runs.models.restriction;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsCcyRestrictions;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleCcyRestriction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CcyRestrictions {
    private BigDecimal valueFrom;
    private BigDecimal valueTo;
    private Long currencyId;

    private static CcyRestrictions fromSettlementPeriodsCcyRestrictions(SettlementPeriodsCcyRestrictions settlementPeriodsCcyRestrictions) {
        return new CcyRestrictions(BigDecimal.valueOf(settlementPeriodsCcyRestrictions.getValueFrom()), BigDecimal.valueOf(settlementPeriodsCcyRestrictions.getValueTo()), settlementPeriodsCcyRestrictions.getCurrencyId());
    }

    private static CcyRestrictions fromVolumesByScaleCcyRestriction(VolumesByScaleCcyRestriction volumesByScaleCcyRestriction) {
        return new CcyRestrictions(BigDecimal.valueOf(volumesByScaleCcyRestriction.getValueFrom()), BigDecimal.valueOf(volumesByScaleCcyRestriction.getValueTo()), volumesByScaleCcyRestriction.getCurrencyId());
    }

    public static List<CcyRestrictions> fromSettlementPeriodsCcyRestrictions(List<SettlementPeriodsCcyRestrictions> settlementPeriodsCcyRestrictions) {
        return settlementPeriodsCcyRestrictions.stream()
                .map(CcyRestrictions::fromSettlementPeriodsCcyRestrictions)
                .collect(Collectors.toList());
    }

    public static List<CcyRestrictions> fromVolumesByScaleCcyRestriction(List<VolumesByScaleCcyRestriction> volumesByScaleCcyRestriction) {
        return volumesByScaleCcyRestriction.stream()
                .map(CcyRestrictions::fromVolumesByScaleCcyRestriction)
                .collect(Collectors.toList());
    }
}
