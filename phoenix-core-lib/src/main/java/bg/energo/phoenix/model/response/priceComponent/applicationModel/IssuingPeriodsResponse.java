package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyIssuingPeriods;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleIssuingPeriods;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IssuingPeriodsResponse {

    private Long id;
    private String startDate;
    private String endDate;

    public IssuingPeriodsResponse(VolumesByScaleIssuingPeriods periods) {
        this.id = periods.getId();
        this.startDate = periods.getPeriodFrom();
        this.endDate = periods.getPeriodTo();
    }

    public IssuingPeriodsResponse(PeriodicallyIssuingPeriods periods) {
        this.id = periods.getId();
        this.startDate = periods.getPeriodFrom();
        this.endDate = periods.getPeriodTo();
    }
}
