package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.pod.pod.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@PromptSymbolReplacer
public class PodSearchRequest {
    @NotNull(message = "page-Page must not be null;")
    @Min(value = 0, message = "page-Page should be positive;")
    private Integer page;
    @NotNull(message = "size-Size must not be null;")
    @Range(min = 1, max = 100, message = "size-size should be between 1 and 100!;")
    private Integer size;
    @NotNull(message = "prompt-prompt must not be null;")
    private String prompt="";
    private PodSearchFields searchFields;
    @NotNull(message = "numberType-numberType you can not provide null value explicitly!;")
    private List<PODType> podTypes = new ArrayList<>();
    @NotNull(message = "valueTypeIds-valueTypeIds you can not provide null value explicitly!;")
    private List<Long> gridOperatorIds = new ArrayList<>();
    @NotNull(message = "priceTypeIds-priceTypeIds you can not provide null value explicitly!;")
    private List<PODConsumptionPurposes> consumptionPurposes = new ArrayList<>();
    @NotNull(message = "priceTypeIds-priceTypeIds you can not provide null value explicitly!;")
    private List<PODVoltageLevels> voltageLevels = new ArrayList<>();
    @NotNull(message = "priceTypeIds-priceTypeIds you can not provide null value explicitly!;")
    private List<PODMeasurementType> measurementTypes = new ArrayList<>();
    private BigDecimal providedPowerTo;
    private BigDecimal providedPowerFrom;
    private List<PODDisconnectionPowerSupply> disconnection;
    @NotNull(message = "sortBy-sortBy you can not provide null value explicitly!;")
    private PodSortColumns sortBy = PodSortColumns.ID;
    @NotNull(message = "sortDirection-sortDirection you can not provide null value explicitly!;")
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private boolean excludeOldVersions;
}
