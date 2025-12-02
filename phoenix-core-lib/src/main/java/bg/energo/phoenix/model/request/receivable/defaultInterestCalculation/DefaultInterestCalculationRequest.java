package bg.energo.phoenix.model.request.receivable.defaultInterestCalculation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultInterestCalculationRequest {

    List<Long> liabilityIds;

    @NotNull(message = "virtualPaymentDate-Virtual payment date is mandatory;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate virtualPaymentDate;
}
