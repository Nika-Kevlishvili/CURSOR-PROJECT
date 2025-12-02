package bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment;

import java.time.DayOfWeek;

public enum Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ALL_DAYS;

    public boolean dayEquals(DayOfWeek day){
        return this.ordinal()==day.ordinal() || this.equals(ALL_DAYS);
    }
}