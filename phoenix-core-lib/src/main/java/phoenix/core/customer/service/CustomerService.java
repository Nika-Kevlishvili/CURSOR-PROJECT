package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.CustomerFacade;
import phoenix.core.customer.apis.model.CustomerCheckRequest;
import phoenix.core.customer.apis.model.CustomerCheckResponse;
import phoenix.core.customer.apis.service.ApisService;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerListColumns;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.*;
import phoenix.core.customer.model.response.customer.CustomerOwnerResponse;
import phoenix.core.customer.model.response.customer.CustomerResponse;
import phoenix.core.customer.model.response.customer.CustomerViewResponse;
import phoenix.core.customer.model.response.customer.communicationData.CommunicationDataBasicInfo;
import phoenix.core.customer.model.response.customer.manager.ManagerBasicInfo;
import phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;
import phoenix.core.customer.repository.customer.CustomerRepository;
import phoenix.core.customer.service.communicationData.CustomerCommunicationsFacade;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.CustomerCreateException;
import phoenix.core.exception.DomainEntityNotFoundException;
import phoenix.core.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static phoenix.core.customer.model.response.customer.CustomerViewResponse.*;

@Slf4j
@Service("coreCustomerService")
@RequiredArgsConstructor
@Validated
public class CustomerService implements CustomerFacade {

    private final CustomerRepository customerRepository;
    private final ManagerFacade managerFacade;
    private final RelatedCustomerFacade relatedCustomerFacade;
    private final CustomerCommunicationsFacade customerCommunicationsFacade;
    private final ApisService apisApiService;
    private final CustomerOwnerFacade customerOwnerFacade;
    private final CustomerSegmentFacade customerSegmentFacade;
    private final CustomerPreferenceFacade customerPreferenceFacade;
    private final CustomerDetailsFacade customerDetailsFacade;

    @Transactional
    @Override
    public CustomerResponse create(CreateCustomerRequest request, String systemUserId) {
        List<String> exceptionMessages = new ArrayList<>();
        Customer customer = createCustomer(request, exceptionMessages, systemUserId);
        CustomerDetails customerDetails = customerDetailsFacade.createCustomerdetails(request, customer,
                List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages, false);
        customerSegmentFacade.createCustomerSegment(request, customerDetails, List.of(NomenclatureItemStatus.ACTIVE)
                , exceptionMessages);
        customerPreferenceFacade.createCustomerPreference(request, customerDetails,
                List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);

        customerOwnerFacade.saveCustomerOwner(request.getOwner(), customer, exceptionMessages);
        managerFacade.addManagers(request.getManagers(), customerDetails, exceptionMessages);
        relatedCustomerFacade.addRelatedCustomers(request.getRelatedCustomers(), customer, exceptionMessages);
        customerCommunicationsFacade.createCommunicationData(request.getCommunicationData(), customerDetails,
                exceptionMessages);

        throwExceptionIfRequired(exceptionMessages);

        CustomerResponse customerResponse = new CustomerResponse();
        if (customerDetails != null && customer != null) {
            customer.setLastCustomerDetailId(customerDetails.getCustomerId());
            customerRepository.save(customer);
            customerResponse.setId(customer.getId());
        }

        return customerResponse;
    }


    private Customer createCustomer(CreateCustomerRequest request,
                                    List<String> exceptionMessages, String systemUserId) {
        Customer customer = new Customer();
        assignCustomerNumber(request, customer);
        String identifier = request.getCustomerIdentifier();
        checkCustomerExistence(identifier, exceptionMessages);
        checkCustomerExistenceInAPIS(request.getCustomerType(), identifier, exceptionMessages);
        customer.setIdentifier(identifier);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCreateDate(LocalDateTime.now());
        customer.setSystemUserId(systemUserId);
        customer.setCustomerType(request.getCustomerType());
        if (exceptionMessages.isEmpty()) return customerRepository.save(customer);
        return null;
    }


    //TODO: change customer number assignation logic
    private void assignCustomerNumber(CreateCustomerRequest request,
                                      Customer customer) {
        Random random = new Random();
        String generated = String.format("%08d", random.nextInt(100000000));

        if (request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER) {
            customer.setCustomerNumber(Long.parseLong("66" + generated));
        } else {
            customer.setCustomerNumber(Long.parseLong("60" + generated));
        }
    }

    private void checkCustomerExistence(String identifier,
                                        List<String> exceptionMessages) {
        Optional<Customer> optionalCustomer = customerRepository
                .findFirstByIdentifierAndStatusIn(identifier, List.of(CustomerStatus.ACTIVE));
        if (optionalCustomer.isPresent()) {
            exceptionMessages.add("Customer with identifier already exists; ");
        }
    }

    private void checkCustomerExistenceInAPIS(CustomerType customerType,
                                              String identifier,
                                              List<String> exceptionMessages) {
        if (customerType == CustomerType.LEGAL_ENTITY) {
            CustomerCheckResponse customerCheckResponse = checkCustomer(
                    new CustomerCheckRequest(List.of(identifier))
            );
            if (customerCheckResponse == null
                || customerCheckResponse.getCustomers() == null
                || customerCheckResponse.getCustomers().isEmpty()) {
                exceptionMessages.add("Customer does not exists in apis; ");
            }
        }
    }

    private void throwExceptionIfRequired(List<String> exceptionMessages) {
        if (!exceptionMessages.isEmpty()) {
            throw new CustomerCreateException(exceptionMessages);
        }
    }

    @Override
    public CustomerCheckResponse checkCustomer(CustomerCheckRequest customerCheckRequest) {
        return apisApiService.checkApisCustomersInfo(customerCheckRequest);
    }

    @Override
    public CustomerResponse delete(Long id, String systemUserId) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found"));
        if (!customer.getStatus().equals(CustomerStatus.DELETED)) {
            customer.setStatus(CustomerStatus.DELETED);
            customer.setModifyDate(LocalDateTime.now());
            customer.setModifySystemUserId("user"); //TODO sysUser add
            customerRepository.save(customer);
        } else {
            throw new ClientException("Customer is already deleted", ErrorCode.APPLICATION_ERROR);
        }
        return new CustomerResponse(customer.getId());
    }

    @Transactional
    @Override
    public CustomerResponse update(Long id, EditCustomerRequest request, String systemUserId) {
        log.debug("Editing unwanted customer: {}", request.toString());

        List<String> exceptionMessages = new ArrayList<>();
        CustomerResponse customerResponse = new CustomerResponse();
        Optional<Customer> customerOptional = customerRepository.findById(id);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            if (customer.getStatus() != null && customer.getStatus().equals(CustomerStatus.DELETED)) {
                throw new ClientException("Can't edit deleted customer", ErrorCode.APPLICATION_ERROR);
            }
            CustomerDetails savedDetails = null;
            if (!request.getUpdateExistingVersion()) {
                savedDetails = customerDetailsFacade.editCustomerDetails(customer, id, request, exceptionMessages);
            } else {
                return create(new CreateCustomerRequest(request), systemUserId);
            }
            if (savedDetails != null) {
                customerSegmentFacade.editCustomerSegment(request, savedDetails,
                        List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
                customerPreferenceFacade.editCustomerPreference(request, savedDetails,
                        List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
                managerFacade.editManagers(request.getManagers(), savedDetails, exceptionMessages);
                relatedCustomerFacade.editRelatedCustomers(request.getRelatedCustomers(), customer, exceptionMessages);
                customerCommunicationsFacade.editCommunicationData(request.getCommunicationData(), savedDetails,
                        exceptionMessages);
                customerOwnerFacade.editCustomerOwners(request.getOwner(), customer, exceptionMessages);
                customerResponse.setId(savedDetails.getId());
            } else {
                exceptionMessages.add("Customer Details is null; ");
            }
        } else {
            exceptionMessages.add("Customer with this Id not found; ");
        }
        throwExceptionIfRequired(exceptionMessages);
        return customerResponse;
    }

    @Override
    public Page<CustomerListingResponse> list(GetCustomersListRequest request) {
        Sort.Order order = new Sort.Order(request.getColumnDirection(), checkSortField(request));
        //Page<CustomerListingResponse> returnList =
        String searchField = null;
        if (request.getSearchFields() != null) {
            searchField = request.getSearchFields().toString();
        }
        return customerRepository.getCustomersList(request.getPrompt(), searchField, PageRequest.of(request.getPage()
                , request.getSize(), Sort.by(order)));
    }

    private String checkSortField(GetCustomersListRequest getCustomersListRequest) {
        if (getCustomersListRequest.getCustomerListColumns() == null) {
            return CustomerListColumns.ID.getValue();
        } else return getCustomersListRequest.getCustomerListColumns().getValue();
    }

    @Override
    public CustomerViewResponse view(Long id, Long version) {
        List<String> exceptionMessages = new ArrayList<>();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ClientException("Customer with this id not found", ErrorCode.APPLICATION_ERROR));
        CustomerDetails customerDetails;
        if (version != null) {
            customerDetails = customerDetailsFacade.findByCustomerIdAndVersionId(id, version)
                    .orElseThrow(() -> new ClientException("CustomerDetails with this version id not found",
                            ErrorCode.APPLICATION_ERROR));
        } else {
            customerDetails =
                    customerDetailsFacade.findFirstByCustomerId(id, Sort.by(Sort.Direction.DESC, "createDate"))
                            .orElseThrow(() -> new ClientException("Customer Details not found",
                                    ErrorCode.APPLICATION_ERROR));
        }
        List<ManagerBasicInfo> managers = managerFacade.previewByCustomerDetailId(customerDetails, exceptionMessages);
        List<RelatedCustomerBasicInfo> relatedCustomers = relatedCustomerFacade.previewByCustomer(customer,
                exceptionMessages);
        List<CustomerOwnerResponse> owners = customerOwnerFacade.getOwnersForCustomer(customer.getId(),
                exceptionMessages);
        List<CommunicationDataBasicInfo> customerCommunications =
                customerCommunicationsFacade.previewByCustomerDetailId(customerDetails.getId(), exceptionMessages);
        return getViewObject(customer, customerDetails, managers, relatedCustomers, owners, customerCommunications);
    }

    private CustomerViewResponse getViewObject(Customer customer,
                                               CustomerDetails customerDetails,
                                               List<ManagerBasicInfo> managers,
                                               List<RelatedCustomerBasicInfo> relatedCustomers,
                                               List<CustomerOwnerResponse> ownerResponses,
                                               List<CommunicationDataBasicInfo> customerCommunications) {
        //CustomerDataMap
        CustomerViewResponse customerViewResponse = new CustomerViewResponse();
        customerViewResponse.setCustomerId(customer.getId());
        customerViewResponse.setCustomerNumber(customer.getCustomerNumber());
        customerViewResponse.setIdentifier(customer.getIdentifier());
        customerViewResponse.setCustomerType(customer.getCustomerType());
//        customerViewResponse.setCustomerOwners(mapCustomerOwners(customer.getCustomerOwners()));
        customerViewResponse.setLastCustomerDetailId(customer.getLastCustomerDetailId());
        customerViewResponse.setCustomerStatus(customer.getStatus());
        //CustomerDetailsDataMaps
        customerViewResponse.setCustomerDetailsId(customerDetails.getId());
        customerViewResponse.setOldCustomerNumbers(customerDetails.getOldCustomerNumbers());
        customerViewResponse.setVatNumber(customerDetails.getVatNumber());
        customerViewResponse.setName(customerDetails.getName());
        customerViewResponse.setNameTransl(customerDetails.getNameTransl());
        customerViewResponse.setMiddleName(customerDetails.getMiddleName());
        customerViewResponse.setMiddleNameTransl(customerDetails.getMiddleNameTransl());
        customerViewResponse.setLastName(customerDetails.getLastName());
        customerViewResponse.setLastNameTransl(customerDetails.getLastNameTransl());
        customerViewResponse.setLegalFormId(customerDetails.getLegalFormId());
        customerViewResponse.setLegalFormTranslId(customerDetails.getLegalFormTranslId());
        customerViewResponse.setOwnershipFormId(customerDetails.getOwnershipFormId());
        customerViewResponse.setEconomicBranchCiId(customerDetails.getEconomicBranchCiId());
        customerViewResponse.setEconomicBranchNceaId(customerDetails.getEconomicBranchNceaId());
        customerViewResponse.setMainActivitySubject(customerDetails.getMainActivitySubject());
        customerViewResponse.setCustomerDeclaredConsumption(customerDetails.getCustomerDeclaredConsumption());
        customerViewResponse.setCreditRating(mapCreditRating(customerDetails.getCreditRating()));
        customerViewResponse.setBank(mapBank(customerDetails.getBank()));
        customerViewResponse.setIban(customerDetails.getIban());
        customerViewResponse.setZipCode(mapZipCode(customerDetails.getZipCode()));
        customerViewResponse.setStreetNumber(customerDetails.getStreetNumber());
        customerViewResponse.setAddressAdditionalInfo(customerDetails.getAddressAdditionalInfo());
        customerViewResponse.setBlock(customerDetails.getBlock());
        customerViewResponse.setEntrance(customerDetails.getEntrance());
        customerViewResponse.setFloor(customerDetails.getFloor());
        customerViewResponse.setApartment(customerDetails.getApartment());
        customerViewResponse.setMailbox(customerDetails.getMailbox());
        customerViewResponse.setStreetId(customerDetails.getStreetId());
        customerViewResponse.setResidentialAreaId(customerDetails.getResidentialAreaId());
        customerViewResponse.setDistrictId(customerDetails.getDistrictId());
        customerViewResponse.setRegionForeign(customerDetails.getRegionForeign());
        customerViewResponse.setMunicipalityForeign(customerDetails.getMunicipalityForeign());
        customerViewResponse.setPopulatedPlaceForeign(customerDetails.getPopulatedPlaceForeign());
        customerViewResponse.setZipCodeForeign(customerDetails.getZipCodeForeign());
        customerViewResponse.setDistrictForeign(customerDetails.getDistrictForeign());
        customerViewResponse.setCustomerDetailsCustomerId(customerDetails.getCustomerId());
        customerViewResponse.setVersionId(customerDetails.getVersionId());
        customerViewResponse.setPublicProcurementLaw(customerDetails.getPublicProcurementLaw());
        customerViewResponse.setMarketingCommConsent(customerDetails.getMarketingCommConsent());
        customerViewResponse.setForeignEntityPerson(customerDetails.getForeignEntityPerson());
        customerViewResponse.setDirectDebit(customerDetails.getDirectDebit());
        customerViewResponse.setForeignAddress(customerDetails.getForeignAddress());
        customerViewResponse.setPopulatedPlaceId(customerDetails.getPopulatedPlaceId());
        customerViewResponse.setCountryId(customerDetails.getCountryId());
        customerViewResponse.setBusinessActivityName(customerDetails.getBusinessActivityName());
        customerViewResponse.setBusinessActivityNameTransl(customerDetails.getBusinessActivityNameTransl());
        customerViewResponse.setGdprRegulationConsent(customerDetails.getGdprRegulationConsent());
        customerViewResponse.setStatus(customerDetails.getStatus());
        customerViewResponse.setCustomerSegments(mapCustomerSegment(customerDetails.getCustomerSegments()));
        customerViewResponse.setCustomerPreferences(mapCustomerPreferences(customerDetails.getCustomerPreferences()));
        customerViewResponse.setCustomerVersions(getCustomerVersions(customerDetails));
        customerViewResponse.setManagerBasicInfos(managers);
        customerViewResponse.setRelatedCustomerBasicInfos(relatedCustomers);
        customerViewResponse.setCustomerOwnerBasicInfos(ownerResponses);
        customerViewResponse.setCustomerCommunications(customerCommunications);
        return customerViewResponse;
    }

    private List<CustomerVersionsResponse> getCustomerVersions(CustomerDetails customerDetails) {
        List<CustomerDetailStatus> customerDetailStatuses = new ArrayList<>();
        customerDetailStatuses.add(CustomerDetailStatus.POTENTIAL);
        customerDetailStatuses.add(CustomerDetailStatus.NEW);
        customerDetailStatuses.add(CustomerDetailStatus.ACTIVE);
        customerDetailStatuses.add(CustomerDetailStatus.LOST);
        customerDetailStatuses.add(CustomerDetailStatus.ENDED);
        return customerDetailsFacade.getVersions(customerDetails.getCustomerId(), customerDetailStatuses);
    }

}
