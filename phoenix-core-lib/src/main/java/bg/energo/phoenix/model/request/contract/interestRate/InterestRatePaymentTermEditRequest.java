package bg.energo.phoenix.model.request.contract.interestRate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class InterestRatePaymentTermEditRequest extends InterestRatePaymentTermBaseRequest {
    private Long id;
}
