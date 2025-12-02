package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.BasePaymentTermsRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInterimAdvancePaymentTermRequest extends BasePaymentTermsRequest {

    @Builder
    public CreateInterimAdvancePaymentTermRequest(CalendarType calendarType, String name, Integer value, Integer valueFrom,
                                                  Integer valueTo, Long calendarId, Boolean excludeWeekends,
                                                  Boolean excludeHolidays, DueDateChange dueDateChange) {
        super(calendarType, name, value, valueFrom, valueTo, calendarId, excludeWeekends, excludeHolidays,
                dueDateChange);
    }

}
