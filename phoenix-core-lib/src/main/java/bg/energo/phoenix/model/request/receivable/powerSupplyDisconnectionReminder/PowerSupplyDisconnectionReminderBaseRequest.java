package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.DateTimeRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.powerSupplyDisconnectionReminder.ReminderForDisconnectionCreateRequestValidator;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ReminderForDisconnectionCreateRequestValidator
public class PowerSupplyDisconnectionReminderBaseRequest {

    @Future(message = "Date and time to send to the customer must be in the future;")
    @NotNull(message = "customerSendToDateAndTime-[customerSendToDateAndTime] date and time is mandatory!;")
    @DateTimeRangeValidator(fieldPath = "customerSendToDateAndTime", fromDateTime = "1990-01-01 00:00", toDateTime = "2090-12-31 23:59")
    private LocalDateTime customerSendToDateAndTime;

    @DecimalMin(value = "0", message = "Liability amount from must be greater than or equal to 0;")
    @Digits(integer = 13, fraction = 2, message = "Liability amount from must have up to 13 digits and 2 decimal places;")
    private BigDecimal liabilityAmountFrom;

    @DecimalMin(value = "1", message = "Liability amount to must be greater than or equal to 0;")
    @Digits(integer = 13, fraction = 2, message = "Liability amount to must have up to 13 digits and 2 decimal places;")
    private BigDecimal liabilityAmountTo;

    private Long currencyId;

    @NotNull(message = "Liabilities max due date is mandatory;")
    @DateRangeValidator(fieldPath = "liabilitiesMaxDueDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate liabilitiesMaxDueDate;

    private String excludeCustomers;

    @NotEmpty(message = "communicationChannels-[communicationChannels] should not be null or empty;")
    private List<CommunicationChannel> communicationChannels;

    private Long emailTemplateId;

    private Long smsTemplateId;

    private Long documentTemplateId;

    @NotNull(message = "Disconnection date is mandatory;")
    @DateRangeValidator(fieldPath = "disconnectionDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate disconnectionDate;

}
