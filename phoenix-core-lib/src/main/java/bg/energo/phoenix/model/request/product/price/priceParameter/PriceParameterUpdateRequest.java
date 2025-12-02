package bg.energo.phoenix.model.request.product.price.priceParameter;

import bg.energo.phoenix.model.customAnotations.product.price.PriceTimeUpdateValidator;
import bg.energo.phoenix.model.enums.time.PeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@PriceTimeUpdateValidator
public class PriceParameterUpdateRequest {

    private Long versionId;
    @NotBlank(message = "name-name can not be null")
    private String name;

    private boolean newVersion;
    @NotNull(message = "periodType-periodType can not be null")
    private PeriodType periodType;
    @Valid
    private List<PriceParameterTimeRequest> priceParameterDetails;
}
