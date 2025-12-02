package bg.energo.phoenix.model.request.product.term.terms.paymentTerm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditInvoicePaymentTermRequest extends BasePaymentTermsRequest {

    private Long id;

    public static CreateInvoicePaymentTermRequest toCreateRequest(EditInvoicePaymentTermRequest request) {
        CreateInvoicePaymentTermRequest result = new CreateInvoicePaymentTermRequest();
        result.setName(request.getName());
        result.setCalendarType(request.getCalendarType());
        result.setValue(request.getValue());
        result.setValueFrom(request.getValueFrom());
        result.setValueTo(request.getValueTo());
        result.setCalendarId(request.getCalendarId());
        result.setExcludeWeekends(request.getExcludeWeekends());
        result.setExcludeHolidays(request.getExcludeHolidays());
        result.setDueDateChange(request.getDueDateChange());
        return result;
    }

}
