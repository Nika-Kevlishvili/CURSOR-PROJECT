package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ManualLiabilityOffsettingCustomerRequest {

    @DateRangeValidator(fieldPath = "date", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "date-date can not be null;")
    private LocalDate date;

    @NotNull(message = "customerId-customerId can not be null;")
    private Long customerId;

    private Long customerDetailId;
}
