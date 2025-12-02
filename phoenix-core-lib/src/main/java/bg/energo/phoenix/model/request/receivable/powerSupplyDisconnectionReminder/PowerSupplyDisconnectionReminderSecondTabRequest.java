package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PowerSupplyDisconnectionReminderSecondTabRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private ReminderForDisconnectionSecondTabSearchFields searchFields;

    @NotNull(message = "powerSupplyDisconnectionReminderId is mandatory;")
    private Long powerSupplyDisconnectionReminderId;
}
