package bg.energo.phoenix.service.billing.runs.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DateRange {
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public boolean overlaps(DateRange other) {
        return !this.dateTo.isBefore(other.dateFrom) && !this.dateFrom.isAfter(other.dateTo);
    }

    public DateRange intersection(DateRange other) {
        LocalDate latestStart = this.dateFrom.isAfter(other.dateFrom) ? this.dateFrom : other.dateFrom;
        LocalDate earliestEnd = this.dateTo.isBefore(other.dateTo) ? this.dateTo : other.dateTo;

        if (!latestStart.isAfter(earliestEnd)) {
            return new DateRange(latestStart, earliestEnd);
        } else {
            return null; // No intersection
        }
    }
    public DateRange merge(DateRange other) {
        LocalDate mergedFrom = dateFrom.isBefore(other.dateFrom) ? dateFrom : other.dateFrom;
        LocalDate mergedTo = dateTo.isAfter(other.dateTo) ? dateTo : other.dateTo;
        return new DateRange(mergedFrom, mergedTo);
    }
}
