package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit;

import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.PeriodOfYearBaseRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EditPeriodOfYearRequest extends PeriodOfYearBaseRequest {

    private Long id;

}
