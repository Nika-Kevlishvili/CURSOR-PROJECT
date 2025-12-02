package bg.energo.phoenix.model.response.nomenclature.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayResponse {
    private Long id;
    private Long calendarId;
    private LocalDate holidayDate;
    private HolidayStatus status;

    public HolidayResponse(Holiday holiday) {
        this.id = holiday.getId();
        this.calendarId = holiday.getCalendarId();
        this.holidayDate = holiday.getHoliday().toLocalDate();
        this.status = holiday.getHolidayStatus();
    }
}
