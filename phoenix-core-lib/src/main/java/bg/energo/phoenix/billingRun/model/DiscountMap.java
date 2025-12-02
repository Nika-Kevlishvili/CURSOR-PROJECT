package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class DiscountMap {
    private Long podId;
    private Long customerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountMap that = (DiscountMap) o;
        return Objects.equals(podId, that.podId) && Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(podId, customerId);
    }

}
