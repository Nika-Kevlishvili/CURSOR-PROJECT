package bg.energo.phoenix.model.response.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoicePaymentTermsResponse {
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
    private Long termId;
    private PaymentTermStatus status;

    public InvoicePaymentTermsResponse(InvoicePaymentTerms invoicePaymentTerms, Calendar calendar) {
        this.id = invoicePaymentTerms.getId();
        this.name = invoicePaymentTerms.getName();
        this.calendarType = invoicePaymentTerms.getCalendarType();
        this.value = invoicePaymentTerms.getValue();
        this.valueFrom = invoicePaymentTerms.getValueFrom();
        this.valueTo = invoicePaymentTerms.getValueTo();
        this.calendar =new CalendarResponse(calendar);
        this.excludeWeekends = invoicePaymentTerms.getExcludeWeekends();
        this.excludeHolidays = invoicePaymentTerms.getExcludeHolidays();
        this.dueDateChange = invoicePaymentTerms.getDueDateChange();
        this.termId = invoicePaymentTerms.getTermId();
        this.status = invoicePaymentTerms.getStatus();
    }
}
