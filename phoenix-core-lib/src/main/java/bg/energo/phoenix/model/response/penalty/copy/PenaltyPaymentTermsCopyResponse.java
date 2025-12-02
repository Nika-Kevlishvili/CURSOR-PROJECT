package bg.energo.phoenix.model.response.penalty.copy;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenaltyPaymentTermsCopyResponse {
    private String name;
    private CalendarType calendarType;
    private Integer value;
    private Integer valueFrom;
    private Integer valueTo;
    private CalendarResponse calendar;
    private Boolean excludeWeekends;
    private Boolean excludeHolidays;
    private DueDateChange dueDateChange;
    private EntityStatus status;
}
