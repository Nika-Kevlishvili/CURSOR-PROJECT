package bg.energo.phoenix.model.request.product.term.terms.paymentTerm;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PenaltyPaymentTermRequest extends BasePaymentTermsRequest {

    @Builder
    public PenaltyPaymentTermRequest(CalendarType calendarType, String name, Integer value, Integer valueFrom,
                                     Integer valueTo, Long calendarId, Boolean excludeWeekends,
                                     Boolean excludeHolidays, DueDateChange dueDateChange) {
        super(calendarType, name, value, valueFrom, valueTo, calendarId, excludeWeekends, excludeHolidays,
                dueDateChange);
    }
}
