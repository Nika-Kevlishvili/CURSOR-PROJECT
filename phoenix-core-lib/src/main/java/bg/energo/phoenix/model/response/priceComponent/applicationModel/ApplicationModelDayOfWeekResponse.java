package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Week;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
@Data
@NoArgsConstructor
public class ApplicationModelDayOfWeekResponse {
    private Long id;
    private Week week;
    private List<Day> day;

    public ApplicationModelDayOfWeekResponse(PeriodicallyDayWeekPeriodYear periodYear) {
        this.id = periodYear.getId();
        this.week = periodYear.getWeek();
        this.day = periodYear.getDay().stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();;
    }
}
