package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyDateOfMonths;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
public class ApplicationModelDateOfMonthResponse {
    private Long id;
    private Month month;
    private List<MonthNumber> monthNumbers;

    public ApplicationModelDateOfMonthResponse(PeriodicallyDateOfMonths dateOfMonths) {
        this.id = dateOfMonths.getId();
        this.month = dateOfMonths.getMonth();
        this.monthNumbers = dateOfMonths.getMonthNumber().stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }
}
