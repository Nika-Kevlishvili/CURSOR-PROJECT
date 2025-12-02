package bg.energo.phoenix.model.entity.pod.pod;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PODVoltageLevels;
import bg.energo.phoenix.model.response.nomenclature.address.*;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class PodContractResponse {
    private String identifier;
    private String name;
    private Long gridOperatorId;
    private String gridOperatorName;
    private PODType type;
    private PODVoltageLevels voltageLevel;
    private PODMeasurementType measurementType;
    private BigDecimal providedPower;
    private BigDecimal multiplier;
    private PODConsumptionPurposes consumptionPurpose;
    private Integer estimatedMonthlyAvgConsumption;


    private CountryResponse country;
    private RegionResponse region;
    private MunicipalityResponse municipality;
    private PopulatedPlaceResponse populatedPlace;
    private ZipCodeResponse zipCode;
    private DistrictResponse district;
    private ResidentialAreaResponse residentialArea;
    private StreetsResponse streets;

    private String streetNumber;
    private String addressAdditionalInfo;
    private String block;
    private String entrance;
    private String floor;
    private String apartment;
    private String mailbox;

    private Boolean foreign;
    private String regionForeign;
    private String municipalityForeign;
    private String populatedPlaceForeign;
    private String zipCodeForeign;
    private String districtForeign;
    private ResidentialAreaType residentialAreaTypeForeign;
    private String residentialAreaForeign;
    private StreetType streetTypeForeign;
    private String streetForeign;

    private PodViewMeasurementType podViewMeasurementType;
    private boolean settlementPeriod;
    private boolean slp;

    private Set<Long> podAdditionalParameters;
    private List<ShortResponse> podAdditionalParametersShortResponse;
}
