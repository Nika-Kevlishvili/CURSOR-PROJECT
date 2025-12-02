package bg.energo.phoenix.model.entity.pod.pod;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PODVoltageLevels;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "pod_details", schema = "pod")
public class PointOfDeliveryDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "pod_details_id_seq",
            sequenceName = "pod.pod_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "pod_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "balancing_group_coordinator_id")
    private Long balancingGroupCoordinatorId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PODType type;

    @Column(name = "estimated_monthly_avg_consumption")
    private Integer estimatedMonthlyAvgConsumption;

    @Column(name = "consumption_purpose")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PODConsumptionPurposes consumptionPurpose;

    @Column(name = "user_type_id")
    private Long userTypeId;

    @Column(name = "voltage_level")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PODVoltageLevels voltageLevel;

    @Column(name = "customer_identifier_by_grid_operator")
    private String customerIdentifierByGridOperator;

    @Column(name = "customer_number_by_grid_operator")
    private String customerNumberByGridOperator;

    @Column(name = "measurement_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PODMeasurementType measurementType;

    @Column(name = "provided_power")
    private BigDecimal providedPower;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "zip_code_id")
    private Long zipCodeId;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "address_additional_info")
    private String addressAdditionalInfo;

    @Column(name = "block")
    private String block;

    @Column(name = "entrance")
    private String entrance;

    @Column(name = "floor")
    private String floor;

    @Column(name = "apartment")
    private String apartment;

    @Column(name = "mailbox")
    private String mailbox;

    @Column(name = "street_id")
    private Long streetId;

    @Column(name = "residential_area_id")
    private Long residentialAreaId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "region_foreign")
    private String regionForeign;

    @Column(name = "municipality_foreign")
    private String municipalityForeign;

    @Column(name = "populated_place_foreign")
    private String populatedPlaceForeign;

    @Column(name = "zip_code_foreign")
    private String zipCodeForeign;

    @Column(name = "district_foreign")
    private String districtForeign;

    @Column(name = "foreign_address")
    private Boolean foreignAddress;

    @Column(name = "populated_place_id")
    private Long populatedPlaceId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "street_foreign")
    private String streetForeign;

    @Column(name = "residential_area_foreign")
    private String residentialAreaForeign;

    @Column(name = "foreign_street_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StreetType foreignStreetType;

    @Column(name = "foreign_residential_area_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ResidentialAreaType foreignResidentialAreaType;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "version_id")
    private Integer versionId;


    @Column(name = "additional_identifier")
    private String additionalIdentifier;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_measurement_types_id")
    private Long podMeasurementTypeId;
}
