package bg.energo.phoenix.model.response.priceParameter;

import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServiceOrderProcessPriceParamResponse {

    private Long id;

    private BigDecimal price;

    private PeriodType periodType;
}
