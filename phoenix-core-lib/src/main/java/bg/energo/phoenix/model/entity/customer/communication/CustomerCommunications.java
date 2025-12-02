package bg.energo.phoenix.model.entity.customer.communication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_communications", schema = "customer")

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerCommunications extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_communications_seq",
            sequenceName = "customer.customer_communications_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_communications_seq"
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "contact_type_name", nullable = false)
    private String contactTypeName;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "populated_place_id")
    private Long populatedPlaceId;

    @Column(name = "street_id")
    private Long streetId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "street_type")
    private StreetType streetType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "residential_area_type")
    private ResidentialAreaType residentialAreaType;

    @Column(name = "residential_area_id")
    private Long residentialAreaId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "zip_code_id")
    private Long zipCodeId;

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

    @Column(name = "street_foreign")
    private String streetForeign;

    @Column(name = "residential_area_foreign")
    private String residentialAreaForeign;

    @Column(name = "street_number")
    private String streetNumber;

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

    @Column(name = "address_additional_info")
    private String addressAdditionalInfo;

    @Column(name = "customer_detail_id")
    private Long customerDetailsId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private Status status;

    @Column(name = "foreign_address")
    private Boolean foreignAddress;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "foreign_street_type")
    private StreetType streetTypeForeign;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "foreign_residential_area_type")
    private ResidentialAreaType residentialAreaTypeForeign;
}
