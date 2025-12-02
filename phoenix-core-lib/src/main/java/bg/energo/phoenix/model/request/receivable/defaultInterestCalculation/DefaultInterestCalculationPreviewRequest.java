package bg.energo.phoenix.model.request.receivable.defaultInterestCalculation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultInterestCalculationPreviewRequest {

    @NotNull(message = "page-Page is mandatory;")
    private Integer page;

    @NotNull(message = "size-Size is mandatory;")
    private Integer size;

    @NotNull(message = "customerId-Customer is mandatory;")
    public Long customerId;

    @NotNull(message = "virtualPaymentDate-Virtual payment date is mandatory;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate virtualPaymentDate;
}
