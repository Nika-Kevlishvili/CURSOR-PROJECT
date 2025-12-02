package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriods;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.MinuteRange;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.SettlementPeriodHours;
import lombok.Data;

import java.util.Comparator;
import java.util.List;

@Data
public class SettlementPeriodsResponse {
    private MinuteRange minuteRange;
    private List<SettlementPeriodHours> hours;

    public SettlementPeriodsResponse(SettlementPeriods settlementPeriods) {
        this.minuteRange = settlementPeriods.getMinuteRange();
        this.hours = settlementPeriods.getHours().stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }
}
