package bg.energo.phoenix.model.request.billing.invoice;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.billing.InvoiceCancellationValidator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@InvoiceCancellationValidator
@Builder
public class CreateInvoiceCancellationRequest {

    @Size(min = 1, max = 2048, message = "invoices size should be between {min} and {max} symbols;")
    private String invoices;

    private Long fileId;

    @DateRangeValidator(fieldPath = "taxEventDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate taxEventDate;

    @NotNull(message = "templateId-[templateId] can not be null;")
    private Long templateId;


}







