package bg.energo.phoenix.model.request.product.term.terms;

import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.EditInvoicePaymentTermRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditTermsRequest extends BaseTermsRequest {

    @NotNull(message = "id-Id of terms must not be null;")
    private Long id;

    @NotEmpty(message = "invoicePaymentTerms-Payment term request list must not be empty;" )
    private List<@Valid EditInvoicePaymentTermRequest> invoicePaymentTerms;

}
