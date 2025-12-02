package bg.energo.phoenix.model.enums.time;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

public enum TimeZone {
    CET,
    EET;

    public boolean canBeShiftedHour(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            throw new RuntimeException("Provided LocalDateTime is null");
        }

        int hour = localDateTime.getHour();
        int dayOfMonth = localDateTime.getDayOfMonth();
        Month month = localDateTime.getMonth();
        int lastSundayOfMonth = localDateTime.with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY)).getDayOfMonth();

        if (this.equals(CET)) {
            return month.equals(Month.MARCH) && dayOfMonth == lastSundayOfMonth && hour == 3;
        } else if (this.equals(EET)) {
            return month.equals(Month.OCTOBER) && dayOfMonth == lastSundayOfMonth && hour == 4;
        }

        return false;
    }
}
