package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.BasePaymentTermsRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditInterimAdvancePaymentTermRequest extends BasePaymentTermsRequest {

    private Long id;

}
