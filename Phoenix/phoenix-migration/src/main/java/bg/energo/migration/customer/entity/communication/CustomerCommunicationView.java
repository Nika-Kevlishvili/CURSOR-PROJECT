package bg.energo.migration.customer.entity.communication;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import bg.energo.migration.integration.phoenix.customer.model.enums.StreetType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Data
@Entity
@Immutable
@Table(name = "v_customer_communications", schema = "customer_migration")
public class CustomerCommunicationView {

    @Id
    private UUID id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "populated_place_id")
    private Long populatedPlaceId;

    @Column(name = "municipality_id")
    private Long municipalityId;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "block")
    private String block;

    @Column(name = "apartment")
    private String apartment;

    @Column(name = "street_id")
    private Long streetId;

    @Column(name = "zip_code_id")
    private Long zipCodeId;

    @Column(name = "foreign_address")
    private Boolean foreignAddress;

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


    @Column(name = "street_type")
    @Enumerated(EnumType.STRING)
    private StreetType streetType;

    @Column(name = "contact_type_name")
    private String contactTypeName;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

}
