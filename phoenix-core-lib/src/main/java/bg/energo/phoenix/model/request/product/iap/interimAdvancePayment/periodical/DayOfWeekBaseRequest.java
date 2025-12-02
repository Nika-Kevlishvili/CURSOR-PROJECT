package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.AllWeekDaysSelectedValidator;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Week;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekBaseRequest {

    @NotNull(message = "week-Week is required;")
    private Week week;

    @NotEmpty(message = "days-Days must not be empty;")
    @AllWeekDaysSelectedValidator
    private Set<Day> days;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayOfWeekBaseRequest that = (DayOfWeekBaseRequest) o;
        return week == that.week;
    }

    @Override
    public int hashCode() {
        return Objects.hash(week);
    }
}
