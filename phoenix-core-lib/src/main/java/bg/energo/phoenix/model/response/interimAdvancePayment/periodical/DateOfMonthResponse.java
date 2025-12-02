package bg.energo.phoenix.model.response.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDateOfMonth;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DateOfMonthResponse {
    private Long id;
    private Month month;
    private List<MonthNumber> monthNumber;
    private InterimAdvancePaymentSubObjectStatus status;

    public DateOfMonthResponse(InterimAdvancePaymentDateOfMonth interimAdvancePaymentDateOfMonth){
        this.id = interimAdvancePaymentDateOfMonth.getId();
        this.month = interimAdvancePaymentDateOfMonth.getMonth();
        this.monthNumber = interimAdvancePaymentDateOfMonth.getMonthNumbers();
        this.status = interimAdvancePaymentDateOfMonth.getStatus();
    }
}
