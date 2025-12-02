package bg.energo.phoenix.service.billing.runs.models;

import lombok.Data;

@Data
public class RepeatablePeriod {

    private Integer day;
    private Integer month;

    public RepeatablePeriod(Integer day, Integer month) {
        this.day = day;
        this.month = month;
    }

    public RepeatablePeriod(String period) {
        this.month=Integer.valueOf(period.substring(0,2));
        this.day=Integer.valueOf(period.substring(3,5));
    }
}
