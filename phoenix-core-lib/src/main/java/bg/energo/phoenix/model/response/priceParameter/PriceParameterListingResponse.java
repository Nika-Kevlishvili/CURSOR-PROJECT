package bg.energo.phoenix.model.response.priceParameter;

import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceParameterListingResponse {
    private Long id;
    private String name;
    private PriceParameterStatus status;
    private PeriodType periodType;
    private LocalDateTime dateOfCreation;
}
