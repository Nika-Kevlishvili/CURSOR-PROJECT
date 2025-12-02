package phoenix.core.customer.model.entity.customer.communication;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Table(name = "customer_communications", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
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
    @Type(type = "pgsql_enum")
    @Column(name = "status")
    private Status status;

    @Column(name = "foreign_address")
    private Boolean foreignAddress;
}
