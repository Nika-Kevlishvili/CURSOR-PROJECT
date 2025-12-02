package bg.energo.phoenix.model.request.product.price.priceParameter;

import bg.energo.phoenix.model.customAnotations.product.price.ValidPriceParamDetailInfoDates;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidPriceParamDetailInfoDates
public class CreatePriceParameterRequest {

    @NotNull(message = "name-Name must not be null;")
    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 1024, message = "name-Name length be between {min} and {max} symbols;" )
    private String name;

    @NotNull(message = "periodType-Period type must not be null")
    private PeriodType periodType;

    @NotNull(message = "timeZone-Time zone must not be null")
    private TimeZone timeZone;

    @Valid
    private List<CreatePriceParameterDetailRequest> priceParameterDetails;

}
