package bg.energo.phoenix.model.request.product.price.aplicationModel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class ValueRanges {
    private Long id;
    @Range(min = 1, max = 99_999_999, message = "valueFrom-valueFrom should be between 1 and 99 999 999;")
    private Integer valueFrom;
    @Range(min = 1, max = 99_999_999, message = "valueTo-valueTo should be between 1 and 99 999 999;")
    private Integer valueTo;
    @Min(value = 1, message = "currencyId-currencyId can not be less than 1;")
    @NotNull(message = "currencyId-currencyId can not be null;")
    private Long currency;
}
