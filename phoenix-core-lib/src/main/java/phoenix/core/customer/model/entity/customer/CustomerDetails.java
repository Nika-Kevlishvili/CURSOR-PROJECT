package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.address.ZipCode;
import phoenix.core.customer.model.entity.nomenclature.customer.Bank;
import phoenix.core.customer.model.entity.nomenclature.customer.CreditRating;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "customer_details", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "old_customer_numbers", length = 512)
    private String oldCustomerNumbers;

    @Column(name = "vat_number", length = 15)
    private String vatNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_transl", nullable = false)
    private String nameTransl;

    @Column(name = "legal_form_id")
    private Long legalFormId;

    @Column(name = "legal_form_transl_id")
    private Long legalFormTranslId;

    @Column(name = "ownership_form_id")
    private Long ownershipFormId;

    @Column(name = "economic_branch_ci_id")
    private Long economicBranchCiId;

    @Column(name = "economic_branch_ncea_id")
    private Long economicBranchNceaId;

    @Column(name = "main_activity_subject", length = 2048)
    private String mainActivitySubject;

    @Column(name = "public_procurement_law")
    private Boolean publicProcurementLaw = false;

    @Column(name = "customer_declared_consumption", length = 11)
    private String customerDeclaredConsumption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_rating_id", nullable = false)
    private CreditRating creditRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(name = "iban", length = 22)
    private String iban;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zip_code_id", nullable = false)
    private ZipCode zipCode;

    @Column(name = "street_number", length = 32)
    private String streetNumber;

    @Column(name = "address_additional_info", length = 512)
    private String addressAdditionalInfo;

    @Column(name = "block", length = 128)
    private String block;

    @Column(name = "entrance", length = 32)
    private String entrance;

    @Column(name = "floor", length = 16)
    private String floor;

    @Column(name = "apartment", length = 32)
    private String apartment;

    @Column(name = "mailbox", length = 32)
    private String mailbox;

    @Column(name = "street_id")
    private Long streetId;

    @Column(name = "residential_area_id")
    private Long residentialAreaId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "region_foreign", length = 512)
    private String regionForeign;

    @Column(name = "municipality_foreign", length = 512)
    private String municipalityForeign;

    @Column(name = "populated_place_foreign", length = 512)
    private String populatedPlaceForeign;

    @Column(name = "zip_code_foreign", length = 32)
    private String zipCodeForeign;

    @Column(name = "district_foreign", length = 512)
    private String districtForeign;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "marketing_comm_consent")
    private Boolean marketingCommConsent;

    @Column(name = "foreign_entity_person")
    private Boolean foreignEntityPerson;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "foreign_address")
    private Boolean foreignAddress;

    @Column(name = "populated_place_id")
    private Long populatedPlaceId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "middle_name_transl")
    private String middleNameTransl;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "last_name_transl")
    private String lastNameTransl;

    @Column(name = "business_activity_name")
    private String businessActivityName;

    @Column(name = "business_activity_name_transl")
    private String businessActivityNameTransl;

    @Column(name = "gdpr_regulation_consent")
    private Boolean gdprRegulationConsent;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status")
    private CustomerDetailStatus status;

    @Column(name = "street_foreign")
    private String streetForeign;

    @Column(name = "residential_area_foreign")
    private String ResidentialAreaForeign;

    @OneToMany(mappedBy = "customerDetail")
    private List<CustomerSegment> customerSegments;

    @OneToMany(mappedBy = "customerDetail")
    private List<CustomerPreference> customerPreferences;

}
