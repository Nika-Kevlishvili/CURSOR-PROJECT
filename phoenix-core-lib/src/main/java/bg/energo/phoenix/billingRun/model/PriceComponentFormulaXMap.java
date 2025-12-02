package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class PriceComponentFormulaXMap {
    private Long contractDetailId;
    private Long priceComponentId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceComponentFormulaXMap that = (PriceComponentFormulaXMap) o;
        return Objects.equals(contractDetailId, that.contractDetailId) && Objects.equals(priceComponentId, that.priceComponentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractDetailId, priceComponentId);
    }
}
