package bg.energo.phoenix.model.request.billing.billingRun.create;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.InvoiceReversalParametersValidator;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@InvoiceReversalParametersValidator
public class InvoiceReversalParameters {

    private Long fileId;

    @Size(min = 1, max = 2048, message = "List Of Invoices size should be between {min} and {max} symbols;")
    private String listOfInvoices;

}
