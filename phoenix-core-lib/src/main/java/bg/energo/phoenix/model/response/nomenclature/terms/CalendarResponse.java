package bg.energo.phoenix.model.response.nomenclature.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.WeekDays;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CalendarResponse {
    private Long id;
    private String name;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;
    private Long orderingId;
    private List<WeekDays> weekends = new ArrayList<>();
    private List<HolidayResponse> holidays = new ArrayList<>();

    public CalendarResponse(Calendar calendar, List<Holiday> holidays) {
        this.id = calendar.getId();
        this.name = calendar.getName();
        if (calendar.getWeekends() != null) {
            String[] splitWeekDays = calendar.getWeekends().split(";");
            for (String weekday : splitWeekDays) {
                weekends.add(WeekDays.valueOf(weekday));
            }
        }
        this.defaultSelection = calendar.isDefaultSelection();
        this.status = calendar.getStatus();
        this.orderingId = calendar.getOrderingId();
        if (holidays != null) {
            this.holidays = holidays.stream().map(HolidayResponse::new).toList();
        }
    }

    public CalendarResponse(Calendar calendar) {
        this.id = calendar.getId();
        this.name = calendar.getName();
        if (calendar.getWeekends() != null) {
            String[] splitWeekDays = calendar.getWeekends().split(";");
            for (String weekday : splitWeekDays) {
                weekends.add(WeekDays.valueOf(weekday));
            }
        }
        this.defaultSelection = calendar.isDefaultSelection();
        this.status = calendar.getStatus();
        this.orderingId = calendar.getOrderingId();
    }
}
