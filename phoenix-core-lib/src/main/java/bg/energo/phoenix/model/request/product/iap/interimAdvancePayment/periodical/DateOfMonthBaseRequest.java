package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.AllMonthDaysSelectedValidator;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
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
public class DateOfMonthBaseRequest {

    @NotNull(message = "dateOfMonths.month-Month is required;")
    private Month month;

    @NotEmpty(message = "dateOfMonths.monthNumbers-Month Numbers must not be empty;")
    @AllMonthDaysSelectedValidator
    private Set<MonthNumber> monthNumbers;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateOfMonthBaseRequest that = (DateOfMonthBaseRequest) o;
        return month == that.month;
    }

    @Override
    public int hashCode() {
        return Objects.hash(month);
    }
}
