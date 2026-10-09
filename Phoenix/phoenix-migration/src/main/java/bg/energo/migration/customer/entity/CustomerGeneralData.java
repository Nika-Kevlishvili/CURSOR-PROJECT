package bg.energo.migration.customer.entity;

import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerDetailStatus;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerType;
import bg.energo.migration.integration.phoenix.customer.model.enums.StreetType;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "customers", schema = "customer_migration")
public class CustomerGeneralData {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_type")
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @Column(name = "business_activity")
    private Boolean businessActivity;

    @Column(name = "customer_identifier")
    private String customerIdentifier;

    //todo find in req
    @Column(name = "customer_number")
    private String customerNumber;

    @Column(name = "foreign_entity_person")
    private Boolean foreignEntityPerson;

    @Column(name = "marketing_comm_consent")
    private Boolean marketingCommConsent;

    @Column(name = "prefer_communication_in_english")
    private Boolean preferCommunicationInEnglish;

    @Column(name = "old_customer_numbers")
    private String oldCustomerNumbers;

    @Column(name = "vat_number")
    private String vatNumber;

    //todo ask: what will be type? Status?
    @Column(name = "customer_status")
    private String customerStatus;

    @Column(name = "customer_detail_status")
    @Enumerated(EnumType.STRING)
    private CustomerDetailStatus customerDetailStatus;

    @Column(name = "public_procurement_law")
    private Boolean publicProcurementLaw;

    @Column(name = "customer_name")
    private String name;

    @Column(name = "customer_middle_name")
    private String customerMiddleName;

    @Column(name = "customer_last_name")
    private String customerLastName;

    @Column(name = "legal_form_id")
    private Long legalFormId;

    @Column(name = "legal_form_transl_id")
    private Long legalFormTranslId;

    @Column(name = "gdpr_regulation_consent")
    private Boolean gdprRegulationConsent;

    @Column(name = "economic_branch_ci_id")
    private Long economicBranchCiId;

    @Column(name = "main_activity_subject")
    private String mainsActivitySubject;

    //todo ask: who is this?
    @Column(name = "cust_type_desc")
    private String customerTypeDesc;

    @Column(name = "ownership_form_id")
    private Long ownershipFormId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "municipality_id")
    private Long municipalityId;

    @Column(name = "populated_place_id")
    private Long populatedPlaceId;

    @Column(name = "street_id")
    private Long streetId;

    @Column(name = "street_type")
    @Enumerated(EnumType.STRING)
    private StreetType streetType;

    @Column(name = "zip_code_id")
    private Long zipCodeId;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "block")
    private String block;

    @Column(name = "apartment")
    private String apartment;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bic_code")
    private String bicCode;

    @Column(name = "iban")
    private String iban;

    @Column(name = "bank_id")
    private Long bankId;

    //todo ask: who is this?
    @Column(name = "customer_id")
    private String customerId;

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

    //From witch source is customer info getting
    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "id_from_phoenix")
    private Long idFromPhoenix;

}
