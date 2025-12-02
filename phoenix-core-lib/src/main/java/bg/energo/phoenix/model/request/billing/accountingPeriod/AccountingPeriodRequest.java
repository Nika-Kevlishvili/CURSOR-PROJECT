package bg.energo.phoenix.model.request.billing.accountingPeriod;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPeriodRequest {

    @NotBlank(message = "name-Name must not blank;")
    @Size(min = 16, max = 16, message = "name-The length of name must be exactly 16;")
    @Pattern(regexp = "^[0-9A-Z]+$", message = "name-Allowed symbols in name are: A-Z 0-9;")
    private String name;

    @NotNull(message = "starDate-StartDate must not be null;")
    private LocalDateTime startDate;

    @NotNull(message = "starDate-EndDate must not be null;")
    private LocalDateTime endDate;

    @NotNull(message = "status-Status must not be null;")
    private AccountingPeriodStatus status;

    @NotNull(message = "modifyDate-Modify Date must not be null;")
    private LocalDateTime modifyDate;
}
