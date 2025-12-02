package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenaltyPaymentTermsResponse {
    private Long id;
    private String name;
    private CalendarType calendarType;
    private Integer value;
    private Integer valueFrom;
    private Integer valueTo;
    private Long calendarId;
    private String calendarName;
    private Boolean excludeWeekends;
    private Boolean excludeHolidays;
    private DueDateChange dueDateChange;
    private EntityStatus status;

}
