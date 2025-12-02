package bg.energo.phoenix.model.request.billing.compensations;

import bg.energo.phoenix.model.customAnotations.nomenclature.FirstDayOfMonth;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompensationRequest {

    @Size(min = 1, max = 255, message = "number-[number] length must be between {min} and {max};")
    @NotEmpty(message = "number-[number] is mandatory;")
    private String number;

    @NotNull(message = "date-[date] is mandatory;")
    private LocalDate date;

    @NotNull(message = "volumes-[volumes] is mandatory;")
    private BigDecimal volumes;

    @NotNull(message = "price-[price] is mandatory;")
    private BigDecimal price;

    @NotEmpty(message = "reason-[reason] is mandatory;")
    private String reason;

    @FirstDayOfMonth(message = "documentPeriod-[documentPeriod] must always be the first day of the month;")
    @NotNull(message = "documentPeriod-[documentPeriod] is mandatory;")
    private LocalDate documentPeriod;

    @NotNull(message = "documentAmount-[documentAmount] is mandatory;")
    @DecimalMin(value = "0.0", inclusive = false, message = "documentAmount-[documentAmount] must be greater than 0;")
    private BigDecimal documentAmount;

    @NotNull(message = "documentCurrencyId-[documentCurrencyId] is mandatory;")
    private Long documentCurrencyId;

    @NotNull(message = "customerId-[customerId] is mandatory;")
    private Long customerId;

    @NotNull(message = "podId-[podId] is mandatory;")
    private Long podId;

    @NotNull(message = "recipientId-[recipientId] is mandatory;")
    private Long recipientId;

}
