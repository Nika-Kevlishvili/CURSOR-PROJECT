package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import lombok.Data;

import java.util.List;

@Data
public class OverTimePeriodicallyResponse {
    private Periodicity periodType;
    private String formula;
    private List<ApplicationModelDateOfMonthResponse> dateOfMonths;
    private ApplicationModelDayWeekPeriodOfYearResponse dayWeekPeriodOfYear;
}
