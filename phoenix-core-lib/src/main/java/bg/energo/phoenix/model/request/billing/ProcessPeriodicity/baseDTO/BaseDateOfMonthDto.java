package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BaseDateOfMonthDto {

    private Month month;

    private List<MonthNumber> monthNumbers;

}
