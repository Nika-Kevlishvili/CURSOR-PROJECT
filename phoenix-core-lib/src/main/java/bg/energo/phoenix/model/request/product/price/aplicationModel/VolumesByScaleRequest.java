package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.ApplicationModelDateRangesValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.ValueRangesValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.VolumeRangesValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class VolumesByScaleRequest {

    @NotNull(message = "applicationModelRequest.volumesByScaleRequest.scaleIds-scaleIds can not be null;")
    @Size(min = 1, message = "applicationModelRequest.volumesByScaleRequest.scaleIds-scaleIds can not be empty;")
    private Set<Long> scaleIds;

    @ApplicationModelDateRangesValidator
    private List<@Valid APPeriodOfYearRequest> periodsOfYear;

    private boolean hasVolumeRestriction;

    private boolean hasValueRestriction;

    private boolean yearRound;

    @VolumeRangesValidator(message ="applicationModelRequest.volumesByScaleRequest.kwhRestriction")
    private List<VolumeRanges> kwhRestriction;

    @ValueRangesValidator(message = "applicationModelRequest.settlementPeriodsRequest.ccyRestriction")
    private List<ValueRanges> ccyRestriction;

    @DecimalMin(value = "0.01",message = "applicationModelRequest.volumesByScaleRequest.kwhRestrictionPercent-Allowed range is between 0.01 and 100.00;")
    @DecimalMax(value = "100.00",message = "applicationModelRequest.volumesByScaleRequest.kwhRestrictionPercent-Allowed range is between 0.01 and 100.00;")
    @Digits(integer = 3, fraction = 2, message = "applicationModelRequest.volumesByScaleRequest.kwhRestrictionPercent-[kwhRestrictionPercent] should not exceed {integer} integral and {fraction} fractional parts;")
   // @NotNull(message = "applicationModelRequest.volumesByScaleRequest.kwhRestrictionPercent-kwhRestrictionPercent can not be set to null;")
    private BigDecimal kwhRestrictionPercent;

}
