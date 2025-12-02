package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@AllArgsConstructor
public class PriceAndDateMap {
    private String id;
    private LocalDateTime dateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceAndDateMap that = (PriceAndDateMap) o;
        return Objects.equals(id, that.id) && Objects.equals(dateTime, that.dateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateTime);
    }
}
