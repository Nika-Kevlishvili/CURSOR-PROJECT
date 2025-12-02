package bg.energo.phoenix.model.request.product.term.terms;

import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.CreateInvoicePaymentTermRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTermsRequest extends BaseTermsRequest {

    @NotEmpty(message = "invoicePaymentTerms-Payment term request list must not be empty;" )
    List<@Valid CreateInvoicePaymentTermRequest> invoicePaymentTerms;

}
