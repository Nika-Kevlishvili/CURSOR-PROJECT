package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit;

import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DayOfWeekBaseRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EditDayOfWeekRequest extends DayOfWeekBaseRequest {

    private Long id;

}
