package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import bg.energo.phoenix.model.enums.billing.processPeriodicity.WeekTemporary;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseDayOfWeekDto {

    private WeekTemporary week;

    private List<Day> days;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseDayOfWeekDto that = (BaseDayOfWeekDto) o;
        return week == that.week;
    }

    @Override
    public int hashCode() {
        return Objects.hash(week);
    }
}
