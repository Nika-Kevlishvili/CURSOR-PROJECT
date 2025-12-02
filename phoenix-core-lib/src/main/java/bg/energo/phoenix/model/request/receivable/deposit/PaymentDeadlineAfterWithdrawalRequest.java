package bg.energo.phoenix.model.request.receivable.deposit;

import bg.energo.phoenix.model.customAnotations.receivable.deposit.ValidPaymentDeadlineAfterWithdrawalRequest;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@ValidPaymentDeadlineAfterWithdrawalRequest
public class PaymentDeadlineAfterWithdrawalRequest {

    @NotNull(message = "paymentDeadlineAfterWithdrawalType-[paymentDeadlineAfterWithdrawalType] must not be null;")
    private CalendarType calendarType;

    @NotNull(message = "value-[value] must not be null;")
    private Integer value;

    @NotNull(message = "calendarId-[calendarId] must not be null;")
    private Long calendarId;

    private Boolean excludeWeekends;

    private Boolean excludeHolidays;

    private DueDateChange dueDateChange;

    @NotBlank(message = "name-[name] must not be null;")
    private String name;

}
