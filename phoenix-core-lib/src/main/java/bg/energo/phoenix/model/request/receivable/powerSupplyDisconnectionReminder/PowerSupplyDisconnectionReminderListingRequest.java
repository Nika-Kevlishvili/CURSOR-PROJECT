package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.DateTimeRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.powerSupplyDisconnectionReminder.ValidReminderForDisconnectionListingRequest;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ValidReminderForDisconnectionListingRequest
public class PowerSupplyDisconnectionReminderListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private ReminderForDisconnectionSearchFields searchFields;

    private List<PowerSupplyDisconnectionReminderStatus> statuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "creationDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate creationDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "creationDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate creationDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @DateTimeRangeValidator(fieldPath = "sendingDateFrom", fromDateTime = "1990-01-01 00:00", toDateTime = "2090-12-31 23:59")
    private LocalDateTime sendingDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @DateTimeRangeValidator(fieldPath = "sendingDateTo", fromDateTime = "1990-01-01 00:00", toDateTime = "2090-12-31 23:59")
    private LocalDateTime sendingDateTo;

    @Digits(integer = 6, fraction = 0, message = "numberOfCustomersFrom must be a whole number;")
    @Min(value = 1, message = "numberOfCustomersFrom must be in the 1-999999 range;")
    @Max(value = 999999, message = "numberOfCustomersFrom must be in the 1-999999 range;")
    private Integer numberOfCustomersFrom;

    @Digits(integer = 6, fraction = 0, message = "numberOfCustomersTo must be a whole number;")
    @Min(value = 1, message = "numberOfCustomersTo must be in the 1-999999 range;")
    @Max(value = 999999, message = "numberOfCustomersTo must be in the 1-999999 range;")
    private Integer numberOfCustomersTo;

    private ReminderForDisconnectionListColumns columns;

    private Sort.Direction direction;
}
