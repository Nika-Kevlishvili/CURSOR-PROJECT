package bg.energo.phoenix.model.response.terms;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;

public interface ServiceOrderPaymentTermResponse {
    Long getId();
    Integer getOrderTermValue();
    Boolean getExcludeWeekends();
    Boolean getExcludeHolidays();
    DueDateChange getDueDateChange();
    CalendarType getCalendarType();
    Long getCalendarId();
    Boolean getNoInterestOnOverdueDebt();
}
