package bg.energo.phoenix.model.request.billing.billingRun;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceCorrectionParameters {
    private boolean priceChange;
    private boolean volumeChange;
    private Long fileId;

    @Size(min = 1, max = 2048, message = "List Of Invoices size should be between {min} and {max} symbols;")
    private String listOfInvoices;

    @JsonIgnore
    @AssertTrue(message =
            "invoiceCorrectionParameters.priceChange-Invoice correction file or list of invoices must be defined when only PRICE CHANGE is selected;" +
                    "invoiceCorrectionParameters.fileId-Invoice correction file or list of invoices must be defined when only PRICE CHANGE is selected;" +
                    "invoiceCorrectionParameters.listOfInvoices-Invoice correction file or list of invoices must be defined when only PRICE CHANGE is selected;"
    )
    public boolean isInvoiceCorrectionParametersValid() {
        if (priceChange && !volumeChange) {
            return fileId != null || listOfInvoices != null;
        }

        return true;
    }
}
