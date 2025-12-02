package bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class BillingRunInvoiceRequest {

    private String prompt;

    private String invoiceNumber;


    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;
}
