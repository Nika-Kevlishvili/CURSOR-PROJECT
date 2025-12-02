package bg.energo.phoenix.billingRun.model;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;

public interface BillingInvoicePaymentTerm {
    Long getId();
    Integer getValue();
    Boolean getExcludeWeekends();
    Boolean getExcludeHolidays();
    DueDateChange getDueDateChange();
    CalendarType getCalendarType();
    Long getCalendarId();
}
