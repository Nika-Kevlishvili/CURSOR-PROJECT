package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerPreference;
import phoenix.core.customer.model.entity.customer.CustomerSegment;
import phoenix.core.customer.model.entity.nomenclature.address.ZipCode;
import phoenix.core.customer.model.entity.nomenclature.customer.Bank;
import phoenix.core.customer.model.entity.nomenclature.customer.BelongingCapitalOwner;
import phoenix.core.customer.model.entity.nomenclature.customer.CreditRating;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.CustomerVersionsResponse;
import phoenix.core.customer.model.response.customer.communicationData.CommunicationDataBasicInfo;
import phoenix.core.customer.model.response.customer.manager.ManagerBasicInfo;
import phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerViewResponse {
    //CustomerObjectFields
    private Long customerId;
    private Long customerNumber;
    private String identifier;
    private CustomerType customerType;
//    private List<CustomerViewOwner> customerOwners;
    private Long lastCustomerDetailId;
    private String customerSystemUserId;
    private LocalDateTime customerCreateDate;
    private LocalDateTime customerModifyDate;
    private String customerModifySystemUserId;
    private CustomerStatus customerStatus;
    //CustomerDetail Fields
    private Long customerDetailsId;
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
    private CustomerDetailStatus status;
    private List<CustomerSegmentResponse> customerSegments;
    private List<CustomerPreferenceResponse> customerPreferences;
    private String customerDetailsSystemUserId;
    private LocalDateTime customerDetailsCreateDate;
    private LocalDateTime customerDetailsModifyDate;
    private String customerDetailsModifySystemUserId;
    private List<CustomerVersionsResponse> customerVersions;
    Boolean gdprMasking;
    private List<ManagerBasicInfo> managerBasicInfos;
    private List<RelatedCustomerBasicInfo> relatedCustomerBasicInfos;
    private List<CustomerOwnerResponse> customerOwnerBasicInfos;
    List<CommunicationDataBasicInfo> customerCommunications;

    public static List<CustomerSegmentResponse> mapCustomerSegment(List<CustomerSegment> customerSegments){
        if(customerSegments == null || customerSegments.size() == 0){
            return null;
        }
        List<CustomerSegmentResponse> returnList = new ArrayList<>();
        for (int i = 0; i < customerSegments.size(); i++) {
            CustomerSegmentResponse customerSegmentResponse = new CustomerSegmentResponse();
            CustomerSegment customerSegment = customerSegments.get(i);
            customerSegmentResponse.setId(customerSegment.getId());
            //customerSegmentResponse.setCustomerDetail(mapCustomerDetails(customerSegment.getCustomerDetail()));
            customerSegmentResponse.setSegment(new SegmentResponse(customerSegment.getSegment()));
            customerSegmentResponse.setStatus(customerSegment.getStatus());
            returnList.add(customerSegmentResponse);
        }
        return returnList;
    }
    public static List<CustomerPreferenceResponse> mapCustomerPreferences(List<CustomerPreference> customerPreferences) {
        if(customerPreferences == null || customerPreferences.size() == 0){
            return null;
        }
        List<CustomerPreferenceResponse> returnList = new ArrayList<>();
        for (int i = 0; i < customerPreferences.size(); i++) {
            CustomerPreference customerPreference = customerPreferences.get(i);
            CustomerPreferenceResponse customerPreferenceResponse = new CustomerPreferenceResponse();
            customerPreferenceResponse.setId(customerPreference.getId());
            //customerPreference.setCustomerDetail(mapCustomerDetails());
            customerPreferenceResponse.setPreferences(new PreferencesResponse(customerPreference.getPreferences()));
            customerPreferenceResponse.setStatus(customerPreference.getStatus());
            returnList.add(customerPreferenceResponse);
        }
        return returnList;
    }

    public static ZipCodeResponse mapZipCode(ZipCode zipCode){
        if(zipCode == null){
            return null;
        }
        return new ZipCodeResponse(zipCode);
    }
    public static BankResponse mapBank(Bank bank){
        if(bank == null){
            return null;
        }
        return new BankResponse(bank);
    }

    public static CreditRatingResponse mapCreditRating(CreditRating creditRating){
        if(creditRating == null){
            return null;
        }
        CreditRatingResponse creditRatingResponse = new CreditRatingResponse();
        creditRatingResponse.setId(creditRating.getId());
        creditRatingResponse.setName(creditRating.getName());
        creditRatingResponse.setStatus(creditRating.getStatus());
        creditRatingResponse.setDefaultSelection(creditRating.getIsDefault());
        creditRatingResponse.setOrderingId(creditRating.getOrderingId());
        return creditRatingResponse;
    }


    private static CustomerResponse mapCustomer(Customer customer) {
        if(customer == null){
            return null;
        }
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setId(customer.getId());
        customerResponse.setCustomerNumber(customer.getCustomerNumber());
        customerResponse.setIdentifier(customer.getIdentifier());
        customerResponse.setCustomerType(customer.getCustomerType());
       // customerResponse.setCustomerOwners(mapOwners(customer.getCustomerOwners())); //TODO DEBUG HERE
        customerResponse.setLastCustomerDetailId(customer.getLastCustomerDetailId());
        customerResponse.setIsDeleted(customer.getStatus());
        return customerResponse;
    }


    public static BelongingCapitalOwnerResponse mapBelongingCapitalOwner(BelongingCapitalOwner belongingCapitalOwner) {
        if(belongingCapitalOwner == null){
            return null;
        }
        BelongingCapitalOwnerResponse belongingCapitalOwnerResponse = new BelongingCapitalOwnerResponse();
        belongingCapitalOwnerResponse.setId(belongingCapitalOwner.getId());
        belongingCapitalOwnerResponse.setName(belongingCapitalOwner.getName());
        belongingCapitalOwnerResponse.setStatus(belongingCapitalOwner.getStatus());
        belongingCapitalOwnerResponse.setOrderingId(belongingCapitalOwner.getOrderingId());
        belongingCapitalOwnerResponse.setDefaultSelection(belongingCapitalOwner.isDefaultSelection());
        belongingCapitalOwnerResponse.setSystemUserId(belongingCapitalOwner.getSystemUserId());
        return belongingCapitalOwnerResponse;
    }
}
