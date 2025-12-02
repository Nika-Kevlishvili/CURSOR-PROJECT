package bg.energo.phoenix.service.billing.runs.models.restriction;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsKwhRestrictions;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleKwhRestriction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwhRestriction {
    private Integer valueFrom;
    private Integer valueTo;

    private static KwhRestriction fromSettlementPeriodsKwhRestrictions(SettlementPeriodsKwhRestrictions settlementPeriodsKwhRestrictions) {
        return new KwhRestriction(settlementPeriodsKwhRestrictions.getValueFrom(), settlementPeriodsKwhRestrictions.getValueTo());
    }

    private static KwhRestriction fromVolumesByScaleKwhRestriction(VolumesByScaleKwhRestriction volumesByScaleKwhRestriction) {
        return new KwhRestriction(volumesByScaleKwhRestriction.getValueFrom(), volumesByScaleKwhRestriction.getValueTo());
    }

    public static List<KwhRestriction> fromSettlementPeriodsKwhRestrictions(List<SettlementPeriodsKwhRestrictions> settlementPeriodsKwhRestrictions) {
        return settlementPeriodsKwhRestrictions.stream()
                .map(KwhRestriction::fromSettlementPeriodsKwhRestrictions)
                .collect(Collectors.toList());
    }

    public static List<KwhRestriction> fromVolumesByScaleKwhRestriction(List<VolumesByScaleKwhRestriction> volumesByScaleKwhRestriction) {
        return volumesByScaleKwhRestriction.stream()
                .map(KwhRestriction::fromVolumesByScaleKwhRestriction)
                .collect(Collectors.toList());
    }

}
