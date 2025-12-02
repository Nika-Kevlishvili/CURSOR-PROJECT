package bg.energo.phoenix.model.request.product.price.priceParameter;


import bg.energo.phoenix.model.customAnotations.product.price.ValidPriceFractionalWithoutNullCheck;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceParameterTimeRequest {
    @NotNull(message = "periodFrom-periodFrom can not be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime periodFrom;

    @ValidPriceFractionalWithoutNullCheck(value = "${validatedValue}", fieldName = "Price", fraction = 10)
    //@Min(value = 0,message = "price-Price value must be positive")
    private BigDecimal price;

    private boolean shiftedHour;
}
