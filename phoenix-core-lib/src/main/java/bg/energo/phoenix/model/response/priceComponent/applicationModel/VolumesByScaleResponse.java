package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.response.nomenclature.priceComponent.ScalesResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VolumesByScaleResponse {
    private Long modelId;

    private List<ScalesResponse> scalesResponse;
    private List<IssuingPeriodsResponse> periodsOfYear;
    private List<KwhRestrictionResponse> kwhRestriction;
    private List<CcyRestrictionResponse> ccyRestriction;

    private boolean hasVolumeRestriction;
    private boolean hasValueRestriction;
    private boolean yearRound;

    private BigDecimal volumeRestrictionPercent;

}
