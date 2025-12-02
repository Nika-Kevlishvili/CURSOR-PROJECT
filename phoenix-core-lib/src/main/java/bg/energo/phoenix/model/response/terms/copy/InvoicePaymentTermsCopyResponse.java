package bg.energo.phoenix.model.response.terms.copy;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoicePaymentTermsCopyResponse {
    private String name;
    private CalendarType calendarType;
    private Integer value;
    private Integer valueFrom;
    private Integer valueTo;
    private CalendarResponse calendar;
    private Boolean excludeWeekends;
    private Boolean excludeHolidays;
    private DueDateChange dueDateChange;
    private Long termId;
    private InterimAdvancePaymentSubObjectStatus status;

    public InvoicePaymentTermsCopyResponse(InvoicePaymentTermsResponse item) {
        this.name = item.getName();
        this.calendarType = item.getCalendarType();
        this.value = item.getValue();
        this.valueFrom = item.getValueFrom();
        this.valueTo = item.getValueTo();
        this.calendar =item.getCalendar();
        this.excludeWeekends = item.getExcludeWeekends();
        this.excludeHolidays = item.getExcludeHolidays();
        this.dueDateChange = item.getDueDateChange();
        this.termId = item.getTermId();
        this.status = getStatus(item.getStatus());
    }

    public InterimAdvancePaymentSubObjectStatus getStatus(PaymentTermStatus status) {
        if (status.equals(PaymentTermStatus.ACTIVE)) {
            return InterimAdvancePaymentSubObjectStatus.ACTIVE;
        } else return InterimAdvancePaymentSubObjectStatus.DELETED;
    }
}
