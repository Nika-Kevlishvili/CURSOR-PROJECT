package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.customer.*;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPerson;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.model.request.customer.*;
import bg.energo.phoenix.model.request.customer.communicationData.CustomerCommAddressRequest;
import bg.energo.phoenix.model.request.customer.communicationData.CustomerCommForeignAddressData;
import bg.energo.phoenix.model.request.customer.communicationData.CustomerCommLocalAddressData;
import bg.energo.phoenix.model.request.customer.communicationData.EditCustomerCommunicationsRequest;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.EditCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.EditContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.EditContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.EditCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.EditRelatedCustomerRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.*;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPersonRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.service.contract.expressContract.ExpressContractMapper;
import bg.energo.phoenix.service.customer.CustomerSegmentService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class DatabaseMapper {

    private final CustomerDetailsRepository customerDetailsRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final ManagerRepository managerRepository;

    private final RelatedCustomerRepository relatedCustomerRepository;

    private final CustomerOwnerRepository customerOwnerRepository;

    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final CustomerCommContactPersonRepository commContactPersonRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;

    private final CustomerAccountManagerRepository customerAccountManagerRepository;
    private final CommunicationContactPurposeProperties productContractProperties;
    private final CustomerSegmentService customerSegmentService;

    @Transactional
    public EditCustomerRequest convertToEditCustomerRequest(Customer customer, Row row,
                                                            List<String> errorMessages) {
        CustomerDetails customerDetails;

        if (row.getCell(1) != null && row.getCell(1).getCellType() != CellType.BLANK) {
            if (getNumericValue(1, row) != 0) {
                long version = (long) row.getCell(1).getNumericCellValue();
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), version);
                if (customerDetailsOptional.isPresent()) {
                    customerDetails = customerDetailsOptional.get();
                    return fillEditCustomerRequestWithDBInfo(customer, customerDetails);
                } else {
                    errorMessages.add("Not found customer details with customer ID - " + customer.getId() + " and version - " + version);
                }
            } else {
                Long lastCustomerDetailId = customer.getLastCustomerDetailId();
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findById(lastCustomerDetailId);
                if (customerDetailsOptional.isPresent()) {
                    customerDetails = customerDetailsOptional.get();
                    return fillEditCustomerRequestWithDBInfo(customer, customerDetails);
                } else {
                    errorMessages.add("Not found customer details with last customerDetailId of customer. ID - " + lastCustomerDetailId);
                }
            }
        }
        EditCustomerRequest editCustomerRequest = new EditCustomerRequest();
        editCustomerRequest.setUpdateExistingVersion(false);
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findById(customer.getLastCustomerDetailId());
        customerDetailsOptional.ifPresent(details -> editCustomerRequest.setCustomerDetailsVersion(details.getVersionId()));
        return editCustomerRequest;
    }

    public EditCustomerRequest fillEditCustomerRequestWithDBInfo(Customer customer, CustomerDetails customerDetails) {
        EditCustomerRequest request = new EditCustomerRequest();
        request.setCustomerDetailsVersion(customerDetails.getVersionId());
        request.setUpdateExistingVersion(false);
        request.setCustomerType(customer.getCustomerType());
        request.setBusinessActivity(customerDetails.getBusinessActivity());
        request.setCustomerIdentifier(customer.getIdentifier());
        request.setForeign(customerDetails.getForeignEntityPerson());
        request.setPreferCommunicationInEnglish(customerDetails.getPreferCommunicationInEnglish() != null ? customerDetails.getPreferCommunicationInEnglish() : false);
        request.setMarketingConsent(customerDetails.getMarketingCommConsent());
        request.setOldCustomerNumber(customerDetails.getOldCustomerNumbers());
        request.setVatNumber(customerDetails.getVatNumber());
        request.setCustomerDetailStatus(customerDetails.getStatus());
        request.setOwnershipFormId(customerDetails.getOwnershipFormId());
        request.setEconomicBranchId(customerDetails.getEconomicBranchCiId());
        request.setEconomicBranchNCEAId(customerDetails.getEconomicBranchNceaId());
        request.setMainSubjectOfActivity(customerDetails.getMainActivitySubject());

        fillCustomerDetails(request, customerDetails);
        fillSegments(request, customerDetails);
        fillSubObjects(request, customerDetails);
        request.setAddress(createCustomerAddressRequest(customerDetails));
        request.setBankingDetails(createCustomerBankingDetails(customerDetails));

        request.setAccountManagers(createAccountManagers(customerDetails.getId()));

        return request;
    }

    private List<EditCustomerAccountManagerRequest> createAccountManagers(Long customerDetailsId) {
        List<CustomerAccountManager> customerAccountManagers
                = customerAccountManagerRepository.getByCustomerDetailsIdAndStatus(customerDetailsId, Status.ACTIVE);

        return customerAccountManagers
                .stream()
                .map(customerAccountManager -> {
                    EditCustomerAccountManagerRequest request = new EditCustomerAccountManagerRequest();
                    request.setId(customerAccountManager.getId());
                    request.setAccountManagerId(customerAccountManager.getManagerId());
                    request.setAccountManagerTypeId(customerAccountManager.getAccountManagerType().getId());
                    return request;
                }).toList();
    }


    private void fillSubObjects(EditCustomerRequest request, CustomerDetails customerDetails) {
        request.setManagers(createEditManagerRequests(customerDetails));
        request.setRelatedCustomers(createEditRelatedCustomerRequests(customerDetails));
        request.setOwner(createCustomerOwnerEditRequests(customerDetails));
        request.setCommunicationData(createCommunicationDataRequests(customerDetails));
    }

    private void fillSegments(EditCustomerRequest request, CustomerDetails customerDetails) {
        List<CustomerSegment> customerSegments = customerDetails.getCustomerSegments();
        List<Long> segmentIds = customerSegments.stream().map(segment -> segment.getSegment().getId()).toList();
        request.setSegmentIds(segmentIds);
    }

    private void fillCustomerDetails(EditCustomerRequest request, CustomerDetails customerDetails) {
        if (request.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            BusinessCustomerDetails businessCustomerDetails = createBusinessCustomerDetails(customerDetails);
            request.setBusinessCustomerDetails(businessCustomerDetails);
        } else {
            if (Boolean.TRUE.equals(request.getBusinessActivity())) {
                BusinessCustomerDetails businessCustomerDetails = createBusinessCustomerDetails(customerDetails);
                PrivateCustomerDetails privateCustomerDetails = createPrivateCustomerDetails(customerDetails);
                request.setBusinessCustomerDetails(businessCustomerDetails);
                request.setPrivateCustomerDetails(privateCustomerDetails);
            } else {
                PrivateCustomerDetails privateCustomerDetails = createPrivateCustomerDetails(customerDetails);
                request.setPrivateCustomerDetails(privateCustomerDetails);
            }
        }
    }

    private List<EditCustomerCommunicationsRequest> createCommunicationDataRequests(CustomerDetails customerDetails) {
        List<EditCustomerCommunicationsRequest> editCommunicationDataRequests = new ArrayList<>();
        List<CustomerCommunications> customerCommunications = customerCommunicationsRepository.findByCustomerDetailIdAndStatuses(customerDetails.getId(), List.of(Status.ACTIVE));
        for (CustomerCommunications communications : customerCommunications) {
            editCommunicationDataRequests.add(addCommunicationData(communications));
        }
        return editCommunicationDataRequests;
    }

    private EditCustomerCommunicationsRequest addCommunicationData(CustomerCommunications communications) {
        EditCustomerCommunicationsRequest communicationDataRequest = new EditCustomerCommunicationsRequest();
        communicationDataRequest.setId(communications.getId());
        communicationDataRequest.setContactTypeName(communications.getContactTypeName());
        communicationDataRequest.setStatus(communications.getStatus());
        communicationDataRequest.setAddress(createCustomerAddressRequestForCommunications(communications));
        communicationDataRequest.setContactPurposes(createEditContactPurposeRequests(communications));
        communicationDataRequest.setContactPersons(createEditContactPersonRequests(communications));
        communicationDataRequest.setCommunicationContacts(createEditCommunicationContactRequests(communications));
        communicationDataRequest.setStatus(Status.ACTIVE);

        return communicationDataRequest;
    }

    private List<EditContactPurposeRequest> createEditContactPurposeRequests(CustomerCommunications communications) {
        List<EditContactPurposeRequest> contactRequests = new ArrayList<>();
        List<CustomerCommContactPurposes> contactPurposes = commContactPurposesRepository.findByCustomerCommId(communications.getId(), List.of(Status.ACTIVE));
        for (CustomerCommContactPurposes purposes : contactPurposes) {
            contactRequests.add(createEditCommContactRequests(purposes));
        }
        return contactRequests;
    }

    private EditContactPurposeRequest createEditCommContactRequests(CustomerCommContactPurposes purpose) {
        EditContactPurposeRequest contactPurposeRequest = new EditContactPurposeRequest();
        contactPurposeRequest.setId(purpose.getId());
        contactPurposeRequest.setContactPurposeId(purpose.getContactPurposeId());
        contactPurposeRequest.setStatus(purpose.getStatus());
        return contactPurposeRequest;
    }

    private List<EditContactPersonRequest> createEditContactPersonRequests(CustomerCommunications communications) {
        List<EditContactPersonRequest> contactPersonRequests = new ArrayList<>();
        List<CustomerCommContactPerson> contactPersons = commContactPersonRepository.findByCustomerCommIdAndStatuses(communications.getId(), List.of(Status.ACTIVE));
        for (CustomerCommContactPerson contactPerson : contactPersons) {
            contactPersonRequests.add(createEditContactPersonRequest(contactPerson));
        }
        return contactPersonRequests;
    }

    private EditContactPersonRequest createEditContactPersonRequest(CustomerCommContactPerson contactPerson) {
        EditContactPersonRequest editContactPersonRequest = new EditContactPersonRequest();
        editContactPersonRequest.setId(contactPerson.getId());
        editContactPersonRequest.setTitleId(contactPerson.getTitleId());
        editContactPersonRequest.setName(contactPerson.getName());
        editContactPersonRequest.setMiddleName(contactPerson.getMiddleName());
        editContactPersonRequest.setSurname(contactPerson.getSurname());
        editContactPersonRequest.setJobPosition(contactPerson.getJobPosition());
        editContactPersonRequest.setPositionHeldFrom(contactPerson.getPositionHeldFrom());
        editContactPersonRequest.setPositionHeldTo(contactPerson.getPositionHeldTo());
        editContactPersonRequest.setStatus(contactPerson.getStatus());
        return editContactPersonRequest;
    }

    private List<EditCommunicationContactRequest> createEditCommunicationContactRequests(CustomerCommunications communications) {
        List<EditCommunicationContactRequest> editCommunicationContactRequests = new ArrayList<>();
        List<CustomerCommunicationContacts> customerCommunicationContacts = customerCommunicationContactsRepository.findByCustomerCommIdAndStatuses(communications.getId(), List.of(Status.ACTIVE));
        for (CustomerCommunicationContacts communicationContacts : customerCommunicationContacts) {
            editCommunicationContactRequests.add(createEditCommunicationContactRequest(communicationContacts));
        }
        return editCommunicationContactRequests;
    }

    private EditCommunicationContactRequest createEditCommunicationContactRequest(CustomerCommunicationContacts communicationContacts) {
        EditCommunicationContactRequest editCommunicationContactRequest = new EditCommunicationContactRequest();
        editCommunicationContactRequest.setId(communicationContacts.getId());
        editCommunicationContactRequest.setSendSms(communicationContacts.isSendSms());
        editCommunicationContactRequest.setPlatformId(communicationContacts.getPlatformId());
        editCommunicationContactRequest.setStatus(communicationContacts.getStatus());
        editCommunicationContactRequest.setContactType(communicationContacts.getContactType());
        editCommunicationContactRequest.setContactValue(communicationContacts.getContactValue());
        return editCommunicationContactRequest;
    }

    private CustomerCommAddressRequest createCustomerAddressRequestForCommunications(CustomerCommunications communications) {
        CustomerCommAddressRequest customerAddressRequest = new CustomerCommAddressRequest();
        customerAddressRequest.setForeign(communications.getForeignAddress());
        if (communications.getForeignAddress()) {
            CustomerCommForeignAddressData foreignAddressData = createForeignAddressDataForCommunications(communications);
            customerAddressRequest.setForeignAddressData(foreignAddressData);
        } else {
            CustomerCommLocalAddressData localAddressData = createLocalAddressDataForCommunications(communications);
            customerAddressRequest.setLocalAddressData(localAddressData);
        }
        customerAddressRequest.setNumber(communications.getStreetNumber());
        customerAddressRequest.setBlock(communications.getBlock());
        customerAddressRequest.setEntrance(communications.getEntrance());
        customerAddressRequest.setFloor(communications.getFloor());
        customerAddressRequest.setApartment(communications.getApartment());
        customerAddressRequest.setMailbox(communications.getMailbox());
        return customerAddressRequest;
    }

    private CustomerCommLocalAddressData createLocalAddressDataForCommunications(CustomerCommunications communications) {
        CustomerCommLocalAddressData localAddressData = new CustomerCommLocalAddressData();
        localAddressData.setCountryId(communications.getCountryId());
        Optional<PopulatedPlace> populatedPlace = populatedPlaceRepository.findByIdAndStatus(communications.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (populatedPlace.isPresent()) {
            localAddressData.setRegionId(populatedPlace.get().getMunicipality().getRegion().getId());
            localAddressData.setMunicipalityId(populatedPlace.get().getMunicipality().getId());
        }
        localAddressData.setPopulatedPlaceId(communications.getPopulatedPlaceId());
        localAddressData.setZipCodeId(communications.getZipCodeId());
        localAddressData.setDistrictId(communications.getDistrictId());
        localAddressData.setResidentialAreaId(communications.getResidentialAreaId());
        localAddressData.setStreetId(communications.getStreetId());
        localAddressData.setStreetType(communications.getStreetType());
        localAddressData.setResidentialAreaType(communications.getResidentialAreaType());
        return localAddressData;
    }

    private CustomerCommForeignAddressData createForeignAddressDataForCommunications(CustomerCommunications communications) {
        CustomerCommForeignAddressData foreignAddressData = new CustomerCommForeignAddressData();
        foreignAddressData.setCountryId(communications.getCountryId());
        foreignAddressData.setRegion(communications.getRegionForeign());
        foreignAddressData.setMunicipality(communications.getMunicipalityForeign());
        foreignAddressData.setPopulatedPlace(communications.getPopulatedPlaceForeign());
        foreignAddressData.setZipCode(communications.getZipCodeForeign());
        foreignAddressData.setDistrict(communications.getDistrictForeign());
        foreignAddressData.setResidentialAreaType(communications.getResidentialAreaTypeForeign());
        foreignAddressData.setResidentialArea(communications.getResidentialAreaForeign());
        foreignAddressData.setStreetType(communications.getStreetTypeForeign());
        foreignAddressData.setStreet(communications.getStreetForeign());
        return foreignAddressData;
    }


    private List<CustomerOwnerEditRequest> createCustomerOwnerEditRequests(CustomerDetails customerDetails) {
        List<CustomerOwnerEditRequest> customerOwnerEditRequests = new ArrayList<>();
        List<CustomerOwner> customerOwners = customerOwnerRepository.findByCustomerIdAndStatuses(customerDetails.getCustomerId(), List.of(Status.ACTIVE));
        for (CustomerOwner customerOwner : customerOwners) {
            customerOwnerEditRequests.add(createCustomerOwnerEditRequest(customerOwner));
        }
        return customerOwnerEditRequests;
    }

    private CustomerOwnerEditRequest createCustomerOwnerEditRequest(CustomerOwner customerOwner) {
        CustomerOwnerEditRequest customerOwnerEditRequest = new CustomerOwnerEditRequest();
        customerOwnerEditRequest.setId(customerOwner.getId());
        if (customerOwner.getOwnerCustomer() != null) {
            customerOwnerEditRequest.setPersonalNumber(customerOwner.getOwnerCustomer().getIdentifier());
        }
        if (customerOwner.getBelongingCapitalOwner() != null) {
            customerOwnerEditRequest.setBelongingOwnerCapitalId(customerOwner.getBelongingCapitalOwner().getId());
        }
        customerOwnerEditRequest.setAdditionalInformation(customerOwner.getAdditionalInfo());
        return customerOwnerEditRequest;
    }

    private List<EditRelatedCustomerRequest> createEditRelatedCustomerRequests(CustomerDetails customerDetails) {
        List<EditRelatedCustomerRequest> editRelatedCustomerRequests = new ArrayList<>();
        List<RelatedCustomer> relatedCustomers = relatedCustomerRepository.getRelatedCustomersByCustomerIdAndStatus(customerDetails.getCustomerId(), Status.ACTIVE);
        for (RelatedCustomer customer : relatedCustomers) {
            editRelatedCustomerRequests.add(createEditRelatedCustomerRequest(customer));
        }
        return editRelatedCustomerRequests;
    }

    private EditRelatedCustomerRequest createEditRelatedCustomerRequest(RelatedCustomer customer) {
        EditRelatedCustomerRequest editRelatedCustomerRequest = new EditRelatedCustomerRequest();
        editRelatedCustomerRequest.setId(customer.getId());
        editRelatedCustomerRequest.setRelatedCustomerId(customer.getRelatedCustomerId());
        if (customer.getCiConnectionType() != null) {
            editRelatedCustomerRequest.setCiConnectionTypeId(customer.getCiConnectionType().getId());
        }
        editRelatedCustomerRequest.setStatus(customer.getStatus());
        return editRelatedCustomerRequest;
    }

    private List<EditManagerRequest> createEditManagerRequests(CustomerDetails customerDetails) {
        List<EditManagerRequest> editManagerRequests = new ArrayList<>();
        List<Manager> managers = managerRepository.getManagersByCustomerDetailIdAndStatus(customerDetails.getId(), Status.ACTIVE);
        for (Manager manager : managers) {
            editManagerRequests.add(createEditManagerRequest(manager));
        }
        return editManagerRequests;
    }

    private EditManagerRequest createEditManagerRequest(Manager manager) {
        EditManagerRequest editManagerRequest = new EditManagerRequest();
        editManagerRequest.setId(manager.getId());
        if (manager.getTitle() != null) editManagerRequest.setTitleId(manager.getTitle().getId());
        editManagerRequest.setName(manager.getName());
        editManagerRequest.setMiddleName(manager.getMiddleName());
        editManagerRequest.setSurname(manager.getSurname());
        editManagerRequest.setJobPosition(manager.getJobPosition());
        editManagerRequest.setPositionHeldFrom(manager.getPositionHeldFrom());
        editManagerRequest.setPositionHeldTo(manager.getPositionHeldTo());
        LocalDate birthDate = manager.getBirthDate();
        editManagerRequest.setBirthDate(birthDate == null ? null : birthDate.toString());
        if (manager.getRepresentationMethod() != null) {
            editManagerRequest.setRepresentationMethodId(manager.getRepresentationMethod().getId());
        }
        editManagerRequest.setAdditionalInformation(manager.getAdditionalInfo());
        editManagerRequest.setStatus(manager.getStatus());
        return editManagerRequest;
    }

    private CustomerBankingDetails createCustomerBankingDetails(CustomerDetails customerDetails) {
        CustomerBankingDetails bankingDetails = new CustomerBankingDetails();
        bankingDetails.setDirectDebit(customerDetails.getDirectDebit());
        if (customerDetails.getBank() != null) {
            bankingDetails.setBankId(customerDetails.getBank().getId());
            bankingDetails.setBic(customerDetails.getBank().getBic());
        }
        bankingDetails.setIban(customerDetails.getIban());
        bankingDetails.setDeclaredConsumption(customerDetails.getCustomerDeclaredConsumption());

        List<CustomerPreference> customerPreferences = customerDetails.getCustomerPreferences();
        List<Long> preferenceIds = customerPreferences.stream().map(preference -> preference.getPreferences().getId()).toList();

        bankingDetails.setPreferenceIds(preferenceIds);
        if (customerDetails.getCreditRating() != null) {
            bankingDetails.setCreditRatingId(customerDetails.getCreditRating().getId());
        }
        return bankingDetails;
    }

    private CustomerAddressRequest createCustomerAddressRequest(CustomerDetails customerDetails) {
        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setForeign(customerDetails.getForeignAddress());
        request.setNumber(customerDetails.getStreetNumber());
        request.setAdditionalInformation(customerDetails.getAddressAdditionalInfo());
        request.setBlock(customerDetails.getBlock());
        request.setEntrance(customerDetails.getEntrance());
        request.setFloor(customerDetails.getFloor());
        request.setApartment(customerDetails.getApartment());
        request.setMailbox(customerDetails.getMailbox());

        if (customerDetails.getForeignAddress()) {
            ForeignAddressData foreignAddressData = createForeignAddressData(customerDetails);
            request.setForeignAddressData(foreignAddressData);
        } else {
            LocalAddressData localAddressData = createLocalAddressData(customerDetails);
            request.setLocalAddressData(localAddressData);
        }

        return request;
    }

    private LocalAddressData createLocalAddressData(CustomerDetails customerDetails) {
        LocalAddressData localAddressData = new LocalAddressData();
        localAddressData.setCountryId(customerDetails.getCountryId());
        Optional<PopulatedPlace> populatedPlace = populatedPlaceRepository.findByIdAndStatus(customerDetails.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (populatedPlace.isPresent()) {
            localAddressData.setRegionId(populatedPlace.get().getMunicipality().getRegion().getId());
            localAddressData.setMunicipalityId(populatedPlace.get().getMunicipality().getId());
        }
        localAddressData.setPopulatedPlaceId(customerDetails.getPopulatedPlaceId());
        if (customerDetails.getZipCode() != null) localAddressData.setZipCodeId(customerDetails.getZipCode().getId());
        localAddressData.setDistrictId(customerDetails.getDistrictId());
        localAddressData.setResidentialAreaId(customerDetails.getResidentialAreaId());
        localAddressData.setStreetId(customerDetails.getStreetId());
        localAddressData.setStreetType(customerDetails.getStreetType());
        localAddressData.setResidentialAreaType(customerDetails.getResidentialAreaType());
        return localAddressData;
    }

    private ForeignAddressData createForeignAddressData(CustomerDetails customerDetails) {
        ForeignAddressData foreignAddressData = new ForeignAddressData();
        foreignAddressData.setCountryId(customerDetails.getCountryId());
        foreignAddressData.setRegion(customerDetails.getRegionForeign());
        foreignAddressData.setMunicipality(customerDetails.getMunicipalityForeign());
        foreignAddressData.setPopulatedPlace(customerDetails.getPopulatedPlaceForeign());
        foreignAddressData.setZipCode(customerDetails.getZipCodeForeign());
        foreignAddressData.setDistrict(customerDetails.getDistrictForeign());
        foreignAddressData.setResidentialAreaType(customerDetails.getResidentialAreaTypeForeign());
        foreignAddressData.setResidentialArea(customerDetails.getResidentialAreaForeign());
        foreignAddressData.setStreetType(customerDetails.getStreetTypeForeign());
        foreignAddressData.setStreet(customerDetails.getStreetForeign());
        return foreignAddressData;
    }

    private PrivateCustomerDetails createPrivateCustomerDetails(CustomerDetails customerDetails) {
        PrivateCustomerDetails privateCustomerDetails = new PrivateCustomerDetails();
        privateCustomerDetails.setGdprRegulationConsent(customerDetails.getGdprRegulationConsent());
        privateCustomerDetails.setFirstName(customerDetails.getName());
        privateCustomerDetails.setFirstNameTranslated(customerDetails.getNameTransl());
        privateCustomerDetails.setMiddleName(customerDetails.getMiddleName());
        privateCustomerDetails.setMiddleNameTranslated(customerDetails.getMiddleNameTransl());
        privateCustomerDetails.setLastName(customerDetails.getLastName());
        privateCustomerDetails.setLastNameTranslated(customerDetails.getLastNameTransl());

        return privateCustomerDetails;
    }

    private BusinessCustomerDetails createBusinessCustomerDetails(CustomerDetails customerDetails) {
        BusinessCustomerDetails businessCustomerDetails = new BusinessCustomerDetails();
        businessCustomerDetails.setProcurementLaw(customerDetails.getPublicProcurementLaw());
        businessCustomerDetails.setName(customerDetails.getName());
        businessCustomerDetails.setNameTranslated(customerDetails.getNameTransl());
        businessCustomerDetails.setLegalFormId(customerDetails.getLegalFormId());
        businessCustomerDetails.setLegalFormTransId(customerDetails.getLegalFormTranslId());
        businessCustomerDetails.setName(customerDetails.getBusinessActivityName());
        businessCustomerDetails.setNameTranslated(customerDetails.getBusinessActivityNameTransl());

        return businessCustomerDetails;
    }


    private Long getNumericValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            return (long) row.getCell(columnNumber).getNumericCellValue();
        }
        return null;
    }

    public CreateCustomerRequest fillCustomerCreateRequestForExpress(ExpressContractCustomerRequest request) {
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCustomerIdentifier(request.getIdentifier());
        createRequest.setCustomerType(request.getCustomerType());
        createRequest.setCustomerDetailStatus(CustomerDetailStatus.NEW);
        createRequest.setVatNumber(request.getVatNumber());
        createRequest.setForeign(request.isForeign());
        createRequest.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
        createRequest.setMarketingConsent(request.isConsentToMarketingCommunication());
        createRequest.setBusinessActivity(request.getBusinessActivity());
        if (request.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            createRequest.setPrivateCustomerDetails(request.getPrivateCustomerDetails().toPrivateCustomerDetails());
        }
        if (request.getCustomerType().equals(CustomerType.LEGAL_ENTITY) || (request.getBusinessActivity() != null && request.getBusinessActivity())) {
            createRequest.setBusinessActivity(true);
            createRequest.setBusinessCustomerDetails(request.getBusinessCustomerDetails().toBusinessCustomer());
            createRequest.setOwnershipFormId(request.getOwnershipFormId());
            createRequest.setEconomicBranchId(request.getEconomicBranchCiId());
            createRequest.setMainSubjectOfActivity(request.getMainActivitySubject());
            if(request.getManagerRequests()!=null && !request.getManagerRequests().isEmpty()){
            createRequest.setManagers(request.getManagerRequests().stream().map(ExpressContractMapper::createManagerRequest).toList());
            }
        }

        if (!customerSegmentService.hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT)) {
            Optional<Segment> defaultSegment = customerSegmentService.getDefaultSegment();
            if (defaultSegment.isEmpty()) {
                throw new IllegalArgumentsProvidedException("segments-No default segment found and you don't have permission to edit segments;");
            }

            if (!(request.getCustomerSegments().size() == 1 && request.getCustomerSegments().contains(defaultSegment.get().getId()))) {
                throw new IllegalArgumentsProvidedException("segments-You don't have permission to edit segments. Only default segment is allowed;");
            }

            createRequest.setSegmentIds(List.of(defaultSegment.get().getId()));
        } else {
            createRequest.setSegmentIds(new ArrayList<>(request.getCustomerSegments()));
        }

        createRequest.setCommunicationData(request.getCommunications()
                .stream()
                .map(x -> ExpressContractMapper
                        .createCommunications(x, productContractProperties.getBillingCommunicationId(), productContractProperties.getContractCommunicationId()))
                .toList());
        createRequest.setAddress(request.getAddress());
        return createRequest;
    }


}
