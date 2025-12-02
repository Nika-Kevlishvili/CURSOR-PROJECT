package bg.energo.phoenix.model.response.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterimAdvancePaymentTermsResponse {
    private Long id;
    private String name;
    private CalendarType calendarType;
    private Integer value;
    private Integer valueFrom;
    private Integer valueTo;
    private CalendarResponse calendar;
    private Boolean excludeWeekends;
    private Boolean excludeHolidays;
    private DueDateChange dueDateChange;
    private InterimAdvancePaymentSubObjectStatus status;

    public InterimAdvancePaymentTermsResponse(InterimAdvancePaymentTerms interimAdvancePaymentTerms) {
        if (interimAdvancePaymentTerms == null) return;
        this.id = interimAdvancePaymentTerms.getId();
        this.name = interimAdvancePaymentTerms.getName();
        this.calendarType = interimAdvancePaymentTerms.getCalendarType();
        this.value = interimAdvancePaymentTerms.getValue();
        this.valueFrom = interimAdvancePaymentTerms.getValueFrom();
        this.valueTo = interimAdvancePaymentTerms.getValueTo();
        this.calendar = new CalendarResponse(interimAdvancePaymentTerms.getCalendar());
        this.excludeWeekends = interimAdvancePaymentTerms.getExcludeWeekends();
        this.excludeHolidays = interimAdvancePaymentTerms.getExcludeHolidays();
        this.dueDateChange = interimAdvancePaymentTerms.getDueDateChange();
        this.status = interimAdvancePaymentTerms.getStatus();
    }

}
