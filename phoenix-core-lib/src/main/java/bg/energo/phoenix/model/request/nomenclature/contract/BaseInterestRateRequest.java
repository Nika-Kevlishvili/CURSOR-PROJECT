package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseInterestRateRequest {

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;

    @DecimalMin(value = "-100.00", message = "percentageRate-percentageRate should be in range -100.00_100.00;")
    @DecimalMax(value = "100.00", message = "percentageRate-percentageRate should be in range -100.00_100.00;")
    @Digits(integer = 3, fraction = 2, message = "percentageRate-Percentage rate accepts max {integer} integral and max {fraction} fractional digits;")
    private BigDecimal percentageRate;

    @NotNull(message = "dateFrom-dateFrom should not be null;")
    private LocalDate dateFrom;

}
