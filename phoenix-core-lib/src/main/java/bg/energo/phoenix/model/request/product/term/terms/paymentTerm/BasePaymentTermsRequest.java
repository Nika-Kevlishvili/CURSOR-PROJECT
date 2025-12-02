package bg.energo.phoenix.model.request.product.term.terms.paymentTerm;

import bg.energo.phoenix.model.customAnotations.product.terms.ValidInvoicePaymentTermValues;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ValidInvoicePaymentTermValues
public class BasePaymentTermsRequest {

    @NotNull(message = "calendarType-Calendar type must not be null;")
    private CalendarType calendarType;

    // Description of the invoice payment term, displayed on term page,
    // i.e. "5 working days (Exclude Weekends, Holidays, change to Next working day)"
    @NotBlank(message = "name-Name must not be null or blank;")
    private String name;

    private Integer value;

    private Integer valueFrom;

    private Integer valueTo;

    @NotNull(message = "calendarId-Calendar must not be null;")
    private Long calendarId;

    private Boolean excludeWeekends;

    private Boolean excludeHolidays;

    private DueDateChange dueDateChange;

}
