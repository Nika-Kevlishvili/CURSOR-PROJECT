package bg.energo.phoenix.model.request.nomenclature.product.terms;

import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class HolidaysRequest {
    private Long id;
    @NotNull(message = "holidays.holidayDate-Must not be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate holidayDate;

    @NotNull(message = "holidays.holidayStatus-Must not be null")
    private HolidayStatus holidayStatus;
}
