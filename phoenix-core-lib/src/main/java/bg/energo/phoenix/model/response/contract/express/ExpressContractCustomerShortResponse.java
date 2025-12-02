package bg.energo.phoenix.model.response.contract.express;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.contract.express.*;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerAddress;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import bg.energo.phoenix.model.response.customer.manager.ManagerResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ExpressContractCustomerShortResponse {

    private Long id;
    private String identifier;
    private Long customerDetailId;
    private String vatNumber;
    private String name;
    private String nameTransl;
    private CustomerType customerType;
    private Boolean businessActivity;
    private CustomerStatus customerStatus;
    private CustomerDetailStatus detailStatus;
    private String middleName;
    private String middleNameTransl;
    private String lastName;
    private String lastNameTransl;
    private String businessActivityName;
    private String businessActivityNameTransl;
    private boolean preferCommunicationInEnglish;
    private boolean consentToMarketingCommunication;

    private boolean publicProcurementLaw;

    private LegalFormResponse legalForm;
    private LegalFormTranResponse legalFormTranslId;
    private OwnershipFormResponse ownershipFormId;
    private EconomicBranchCIResponse economicBranchCiId;
    private String mainActivitySubject;

    private List<SegmentResponse> customerSegments;
    private List<ManagerResponse> managers;
    private List<CustomerCommunicationsDetailedResponse> communications;
    private CustomerAddress addresses;

    public boolean equalsRequest(ExpressContractCustomerRequest request) {
        if (!request.getIdentifier().equals(this.getIdentifier())) return false;
        if (!request.getCustomerType().equals(this.getCustomerType())) return false;
        if (request.isPreferCommunicationInEnglish() != this.preferCommunicationInEnglish) return false;
        if (request.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            ExpressContractPrivateCustomer privateCustomerDetails = request.getPrivateCustomerDetails();
            if (!Objects.equals(privateCustomerDetails.getFirstName(),this.name)) return false;
            if (!Objects.equals(privateCustomerDetails.getLastName(),this.lastName)) return false;
            if (!Objects.equals(privateCustomerDetails.getMiddleName(),this.middleName)) return false;
            if (!Objects.equals(privateCustomerDetails.getFirstNameTranslated(),this.nameTransl)) return false;
            if (!Objects.equals(privateCustomerDetails.getMiddleNameTranslated(),this.middleNameTransl)) return false;
            if (!Objects.equals(privateCustomerDetails.getLastNameTranslated(),this.lastNameTransl)) return false;

        }
        if (this.businessActivity == null) {
            this.businessActivity = false;
        }
        if (this.customerType.equals(CustomerType.LEGAL_ENTITY))
            request.setBusinessActivity(false);
        if (!Objects.equals(request.getBusinessActivity(), this.businessActivity)) return false;
        if (request.getBusinessActivity().equals(Boolean.TRUE) || request.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            if (request.getVatNumber() == null){
                if (this.vatNumber != null) return false;
            }else{
                if (!request.getVatNumber().equals(this.vatNumber)) return false;
            }
            if (!Objects.equals(request.getOwnershipFormId(),this.ownershipFormId.getId())) return false;
            if (!Objects.equals(request.getEconomicBranchCiId(),this.economicBranchCiId.getId())) return false;
            if (!Objects.equals(request.getMainActivitySubject(),this.mainActivitySubject)) return false;
            ExpressContractBusinessCustomer businessCustomerDetails = request.getBusinessCustomerDetails();
            if (this.legalForm == null && businessCustomerDetails.getLegalFormId() != null)
                return false;
            if (this.legalForm != null && !Objects.equals(businessCustomerDetails.getLegalFormId(), this.legalForm.getId()))
                return false;
            if (this.legalFormTranslId == null && businessCustomerDetails.getLegalFormTransId() != null)
                return false;
            if (this.legalFormTranslId != null && !Objects.equals(businessCustomerDetails.getLegalFormTransId(), this.legalFormTranslId.getId()))
                return false;
        }
        if (request.getBusinessActivity().equals(Boolean.TRUE) || request.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            ExpressContractBusinessCustomer businessCustomerDetails = request.getBusinessCustomerDetails();
            if (!Objects.equals(businessCustomerDetails.getName(),this.businessActivityName)) return false;
            if  (!Objects.equals(businessCustomerDetails.getNameTranslated(),this.businessActivityNameTransl)) return false;
        }
        List<Long> segments = this.customerSegments.stream().map(SegmentResponse::getId).toList();
        Set<Long> requestSegments = request.getCustomerSegments();
        if (!segments.equals(new ArrayList<>(requestSegments))) return false;
        for (Long segment : segments) {
            if (!requestSegments.contains(segment)) {
                return false;
            }
        }
        List<ExpressContractManagerRequest> managerRequestList = request.getManagerRequests();
        if (managers == null && managerRequestList != null) return false;
        if (managers != null && managerRequestList == null) return false;
        if (managers != null) {
            if (this.managers.size() != managerRequestList.size() || managerRequestList.stream().filter(x -> x.getId() != null).toList().size() != this.managers.size())
                return false;
            Map<Long, ExpressContractManagerRequest> managerRequests = managerRequestList.stream().collect(Collectors.toMap(ExpressContractManagerRequest::getId, j -> j));
            for (ManagerResponse manager : managers) {
                ExpressContractManagerRequest managerRequest = managerRequests.get(manager.getId());
                if (managerRequest == null) return false;
                if (!managerRequest.equalsResponse(manager)) return false;
            }
        }


        List<ExpressContractCommunicationsRequest> communicationsRequests = request.getCommunications();
        if (communications == null && communicationsRequests != null) return false;
        if (communications != null && communicationsRequests == null) return false;
        if (communications != null) {
            if (this.communications.size() != communicationsRequests.stream().map(ExpressContractCommunicationsRequest::getId).filter(Objects::nonNull).collect(Collectors.toSet()).size())
                return false;
            Map<Long, CustomerCommunicationsDetailedResponse> communicationsMap = communications.stream().collect(Collectors.toMap(x -> x.getId(), j -> j));
            for (ExpressContractCommunicationsRequest communicationsRequest : communicationsRequests) {
                CustomerCommunicationsDetailedResponse response = communicationsMap.get(communicationsRequest.getId());
                if (response == null) return false;
                if (!communicationsRequest.equalsResponse(response)) return false;
            }

        }

        return this.addresses.equalsRequest(request.getAddress());
    }
}
