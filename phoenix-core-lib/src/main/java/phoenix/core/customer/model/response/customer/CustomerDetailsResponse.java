package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDetailsResponse {
    private Long id;
    private String oldCustomerNumbers;
    private String vatNumber;
    private String name;
    private String nameTransl;
    private Long legalFormId;
    private Long legalFormTranslId;
    private Long ownershipFormId;
    private Long economicBranchCiId;
    private Long economicBranchNceaId;
    private String mainActivitySubject;
    private String customerDeclaredConsumption;
    private CreditRatingResponse creditRating;
    private BankResponse bank;
    private String iban;
    private ZipCodeResponse zipCode;
    private String streetNumber;
    private String addressAdditionalInfo;
    private String block;
    private String entrance;
    private String floor;
    private String apartment;
    private String mailbox;
    private Long streetId;
    private Long residentialAreaId;
    private Long districtId;
    private String regionForeign;
    private String municipalityForeign;
    private String populatedPlaceForeign;
    private String zipCodeForeign;
    private String districtForeign;
    private Long customerDetailsCustomerId;
    private Long versionId;
    private Boolean publicProcurementLaw;
    private Boolean marketingCommConsent;
    private Boolean foreignEntityPerson;
    private Boolean directDebit;
    private Boolean foreignAddress;
    private Long populatedPlaceId;
    private Long countryId;
    private String middleName;
    private String middleNameTransl;
    private String lastName;
    private String lastNameTransl;
    private String businessActivityName;
    private String businessActivityNameTransl;
    private Boolean businessActivity;
    private Boolean gdprRegulationConsent;
    private CustomerDetailStatus customerDetailStatus;
    private List<CustomerSegmentResponse> customerSegments;
    private List<CustomerPreferenceResponse> customerPreferences;
}
