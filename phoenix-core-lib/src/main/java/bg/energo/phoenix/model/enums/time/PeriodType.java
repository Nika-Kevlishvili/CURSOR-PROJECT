package bg.energo.phoenix.model.enums.time;

public enum PeriodType {
    FIFTEEN_MINUTES("15 minute"),
    ONE_HOUR("1 hour"),
    ONE_DAY("1 day"),
    ONE_MONTH("1 month");

    private final String interval;

    PeriodType(String interval) {
        this.interval = interval;
    }

    public String getInterval() {
        return interval;
    }
}
