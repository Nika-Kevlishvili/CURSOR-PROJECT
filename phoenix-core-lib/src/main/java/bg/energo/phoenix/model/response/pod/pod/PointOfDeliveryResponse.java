package bg.energo.phoenix.model.response.pod.pod;

import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.enums.pod.pod.*;
import bg.energo.phoenix.model.response.nomenclature.address.*;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PointOfDeliveryResponse {
    private Long id;
    private Integer versionId;

    private String identifier;
    private String name;
    private String additionalIdentifier;
    private Integer estimatedMonthlyAvgConsumption;
    private String customerIdentifierByGridOperator;
    private String customerNumberByGridOperator;
    private BigDecimal providedPower;
    private BigDecimal multiplier;
    private PODType type;
    private PODConsumptionPurposes consumptionPurpose;
    private PODVoltageLevels voltageLevel;
    private PODMeasurementType measurementType;
    private boolean impossibleToDisconnect;
    private boolean blockedDisconnection;
    private boolean blockedBilling;

    private BlockedBillingResponse blockedBillingResponse;
    private BlockedDisconnectionResponse blockedDisconnectionResponse;

    private Long gridOperatorId;
    private String gridOperatorName;
    private Long balancingGroupCoordinatorId;
    private String balancingGroupCoordinatorName;
    private Long userTypeId;
    private String userTypeName;
    private Long customerId;
    private String customerName;
    private String customerIdentifier;


    private CountryResponse country;
    private RegionResponse region;
    private MunicipalityResponse municipality;
    private PopulatedPlaceResponse populatedPlace;
    private ZipCodeResponse zipCode;
    private DistrictResponse district;
    private ResidentialAreaResponse residentialArea;
    private StreetsResponse streets;
    private List<PointOfDeliveryVersionResponse> versions;

    private List<ShortResponse> pointOfDeliveryAdditionalParamsShortResponse;

    private String streetNumber;
    private String addressAdditionalInfo;
    private String block;
    private String entrance;
    private String floor;
    private String apartment;
    private String mailbox;
    private String regionForeign;
    private String municipalityForeign;
    private String populatedPlaceForeign;
    private String zipCodeForeign;
    private String districtForeign;
    private ResidentialAreaType residentialAreaTypeForeign;
    private String residentialAreaForeign;
    private StreetType streetTypeForeign;
    private String streetForeign;

    private PodStatus status;
    private boolean isLocked;

    private PodViewMeasurementType podViewMeasurementType;
    private boolean settlementPeriod;
    private boolean slp;
}
