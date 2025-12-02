package bg.energo.phoenix.model.request.nomenclature.product.terms;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.WeekDays;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalendarRequest {
    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 512, message = "name-Name does not match the allowed length, range: [1-512];")
    private String name;

    @Size(max = 6, message = "User is not able to select all week as weekend")
    private List<WeekDays> weekends;

    @Valid
    private List<HolidaysRequest> holidays;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default Selection must not be null;")
    private Boolean defaultSelection;
}
