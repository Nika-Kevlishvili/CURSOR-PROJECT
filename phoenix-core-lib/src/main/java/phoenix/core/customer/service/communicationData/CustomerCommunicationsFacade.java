package phoenix.core.customer.service.communicationData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPerson;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPurposes;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunicationContacts;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;
import phoenix.core.customer.model.entity.nomenclature.address.*;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.CustomerAddressRequest;
import phoenix.core.customer.model.request.ForeignAddressData;
import phoenix.core.customer.model.request.LocalAddressData;
import phoenix.core.customer.model.request.communicationData.CreateCommunicationDataRequest;
import phoenix.core.customer.model.request.communicationData.EditCommunicationDataRequest;
import phoenix.core.customer.model.response.customer.communicationData.*;
import phoenix.core.customer.repository.customer.CustomerDetailsRepository;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommContactPersonRepository;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommunicationsRepository;
import phoenix.core.customer.repository.nomenclature.address.*;
import phoenix.core.exception.DomainEntityNotFoundException;

import java.time.LocalDateTime;
import java.util.*;

import static phoenix.core.customer.model.enums.customer.Status.ACTIVE;
import static phoenix.core.customer.model.enums.customer.Status.DELETED;
import static phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service("coreCustomerCommunicationsService")
@RequiredArgsConstructor
public class CustomerCommunicationsFacade {
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerCommunicationContactsRepository contactsRepository;
    private final CustomerCommContactPurposesRepository contactPurposesRepository;
    private final CustomerCommContactPersonRepository contactPersonRepository;

    private final CountryRepository countryRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final DistrictRepository districtRepository;
    private final StreetRepository streetRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final ZipCodeRepository zipCodeRepository;

    private final CommContactPurposeFacade contactPurposeService;
    private final CommContactFacade contactService;
    private final CommContactPersonFacade contactPersonService;

    public List<CommunicationDataBasicInfo> previewByCustomerDetailId(Long customerDetailId, List<String> exceptions) {
        if (!customerDetailsRepository.existsById(customerDetailId)) {
            log.error("Customer detail not found, ID: " + customerDetailId);
            exceptions.add("Customer detail not found, ID: " + customerDetailId);
            return null;
        }

        log.debug("Fetching communication data for customer detail ID: {}", customerDetailId);

        List<CustomerCommunications> communicationsList = customerCommunicationsRepository
                .findByCustomerDetailIdAndStatuses(customerDetailId, List.of(ACTIVE));

        if (communicationsList.isEmpty()) {
            log.debug("No active communication data attached to customer detail, ID: " + customerDetailId);
            return null;
        }

        List<CommunicationDataBasicInfo> temp = new ArrayList<>();

        Map<Long, List<String>> purposesMap = new HashMap<>();
        contactPurposesRepository
                .getContactPurposesByCommunicationDataIdsAndStatuses(
                        communicationsList.stream().map(CustomerCommunications::getId).toList(),
                        List.of(ACTIVE)
                ).forEach(cp -> {
                    List<String> value = purposesMap.getOrDefault(cp.getCustomerCommunicationsDataId(), new ArrayList<>());
                    value.add(cp.getPurposeName());
                    purposesMap.put(cp.getCustomerCommunicationsDataId(), value);
                });

        for (CustomerCommunications cc : communicationsList) {
            temp.add(new CommunicationDataBasicInfo(
                    cc.getId(),
                    cc.getContactTypeName() + " (" + String.join(", ", purposesMap.getOrDefault(cc.getId(), new ArrayList<>())) + ")"
            ));
        }

        return temp;
    }


    public CommunicationDataResponse getDetailedCommDataById(Long id) {
        CustomerCommunications communicationData = customerCommunicationsRepository
                .findByIdAndStatuses(id, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active communication data not found, ID: " + id));

        LocalAddressInfo localAddressInfo = null;
        ForeignAddressInfo foreignAddressInfo = null;
        if (communicationData.getForeignAddress()) {
            foreignAddressInfo = customerCommunicationsRepository.getForeignAddressInfo(communicationData.getId());
            if (foreignAddressInfo == null) {
                throw new DomainEntityNotFoundException("Foreign address info not found, communication data ID: " + id);
            }
        } else {
            localAddressInfo = customerCommunicationsRepository.getLocalAddressInfo(communicationData.getId());
            if (localAddressInfo == null) {
                throw new DomainEntityNotFoundException("Local address info not found, communication data ID: " + id);
            }
        }

        List<ContactPurposeBasicInfo> contactPurposes = contactPurposeService
                .getCommContactPurposeBasicInfoByCommDataIdAndStatuses(communicationData.getId(), List.of(ACTIVE));

        List<ContactBasicInfo> contacts = contactService
                .getCommContactBasicInfoByCommDataIdAndStatuses(communicationData.getId(), List.of(ACTIVE));

        List<ContactPersonBasicInfo> contactPersons = contactPersonService
                .getCommContactPersonBasicInfoByCommDataIdAndStatuses(communicationData.getId(), List.of(ACTIVE));

        return new CommunicationDataResponse(
                communicationData.getForeignAddress(),
                foreignAddressInfo,
                localAddressInfo,
                communicationData,
                contactPurposes,
                contacts,
                contactPersons
        );
    }

    public ContactPersonResponse getDetailedCommDataContactPersonById(Long communicationDataId, Long contactPersonId) {
        if (!customerCommunicationsRepository.existsById(communicationDataId)) {
            log.error("Active communication data not found, ID: " + communicationDataId);
            throw new DomainEntityNotFoundException("Active communication data not found, ID: " + communicationDataId);
        }

        return contactPersonRepository
                .getDetailedContactPersonByCustomerCommIdAndStatuses(contactPersonId, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active contact person not found, ID: " + contactPersonId));
    }

    @Transactional
    public void createCommunicationData(List<CreateCommunicationDataRequest> communicationDataRequests,
                                        CustomerDetails customerDetails,
                                        List<String> exceptions) {
        if (!CollectionUtils.isEmpty(communicationDataRequests)) {
            if (customerDetails == null || customerDetails.getId() == null) {
                log.error("Customer details object is null, cannot create communication data");
                exceptions.add("Customer details object is null, cannot create communication data");
                return;
            }

            log.debug("Creating communication data: {} to customer detail ID: {}", communicationDataRequests, customerDetails.getId());

            if (!customerDetailsRepository.existsById(customerDetails.getId())) {
                log.error("Customer details not found, ID: " + customerDetails.getId());
                exceptions.add("Customer details not found, ID: " + customerDetails.getId());
                return;
            }

            List<CustomerCommContactPurposes> tempContactPurposesList = new ArrayList<>();
            List<CustomerCommContactPerson> tempContactPersonsList = new ArrayList<>();
            List<CustomerCommunicationContacts> tempContactsList = new ArrayList<>();

            for (CreateCommunicationDataRequest request : communicationDataRequests) {
                createCustomerCommunications(
                        request,
                        customerDetails.getId(),
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions
                );
            }

            if (exceptions.isEmpty()) {
                contactPurposesRepository.saveAll(tempContactPurposesList);
                contactPersonRepository.saveAll(tempContactPersonsList);
                contactsRepository.saveAll(tempContactsList);
            }
        }
    }

    private void createCustomerCommunications(CreateCommunicationDataRequest request,
                                              Long customerDetailsId,
                                              List<CustomerCommContactPurposes> tempContactPurposesList,
                                              List<CustomerCommContactPerson> tempContactPersonsList,
                                              List<CustomerCommunicationContacts> tempContactsList,
                                              List<String> exceptions) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status when creating communication data");
            exceptions.add("Cannot set DELETED status when creating communication data");
        }

        CustomerCommunications communicationData = createCustomerCommunications(customerDetailsId, request, exceptions);

        CustomerCommunications customerCommunications = null;
        if (exceptions.isEmpty()) {
            customerCommunications = customerCommunicationsRepository.save(communicationData);
        }

        createSubObjects(
                request,
                customerCommunications,
                tempContactPurposesList,
                tempContactPersonsList,
                tempContactsList,
                exceptions
        );

        if (!exceptions.isEmpty() && customerCommunications != null) {
            customerCommunicationsRepository.delete(customerCommunications);
        }
    }

    private void createSubObjects(CreateCommunicationDataRequest request,
                                  CustomerCommunications communicationData,
                                  List<CustomerCommContactPurposes> tempContactPurposesList,
                                  List<CustomerCommContactPerson> tempContactPersonsList,
                                  List<CustomerCommunicationContacts> tempContactsList,
                                  List<String> exceptions) {
        contactPurposeService.createContactPurposes(
                communicationData,
                request.getContactPurposes(),
                tempContactPurposesList,
                exceptions
        );
        contactPersonService.createContactPersons(
                communicationData,
                request.getContactPersons(),
                tempContactPersonsList,
                exceptions
        );
        contactService.createContacts(
                communicationData,
                request.getCommunicationContacts(),
                tempContactsList,
                exceptions
        );
    }

    @Transactional
    public void editCommunicationData(List<EditCommunicationDataRequest> communicationDataRequests,
                                      CustomerDetails customerDetails,
                                      List<String> exceptions) {
        if (communicationDataRequests == null) {
            communicationDataRequests = Collections.emptyList();
        }

        if (customerDetails == null || customerDetails.getId() == null) {
            log.error("Customer details object is null, cannot edit communication data");
            exceptions.add("Customer details object is null, cannot edit communication data");
            return;
        }

        log.debug("Editing communication data: {} to customer detail ID: {}", communicationDataRequests.toString(), customerDetails.getId());

        List<Long> customerCommunicationIdsByCustomerDetailId = customerCommunicationsRepository
                .getCustomerCommunicationIdsByCustomerDetailId(customerDetails.getId(), List.of(ACTIVE))
                .stream()
                .map(CustomerCommunications::getId)
                .toList();

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer details not found, ID: " + customerDetails.getId());
            exceptions.add("Customer details not found, ID: " + customerDetails.getId());
            return;
        }

        List<CustomerCommunications> tempCustomerCommunications = new ArrayList<>();
        List<CustomerCommContactPurposes> tempContactPurposesList = new ArrayList<>();
        List<CustomerCommContactPerson> tempContactPersonsList = new ArrayList<>();
        List<CustomerCommunicationContacts> tempContactsList = new ArrayList<>();

        for (EditCommunicationDataRequest request : communicationDataRequests) {
            if (request.getStatus().equals(DELETED)) {
                log.error("Cannot set DELETED status when editing communication data");
                exceptions.add("Cannot set DELETED status when editing communication data");
            }

            if (request.getId() == null) {
                createCustomerCommunications(
                        new CreateCommunicationDataRequest(request),
                        customerDetails.getId(),
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions
                );
            } else {
                editCustomerCommunications(
                        request,
                        tempCustomerCommunications,
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions
                );
            }
        }

        if (exceptions.isEmpty()) {
            customerCommunicationsRepository.saveAll(tempCustomerCommunications);
            contactPurposesRepository.saveAll(tempContactPurposesList);
            contactPersonRepository.saveAll(tempContactPersonsList);
            contactsRepository.saveAll(tempContactsList);
        }

        if (exceptions.isEmpty()) {
            deleteRemovedCustomerCommunications(
                    communicationDataRequests,
                    customerCommunicationIdsByCustomerDetailId,
                    exceptions
            );
        }
    }

    private void editCustomerCommunications(EditCommunicationDataRequest request,
                                            List<CustomerCommunications> tempCustomerCommunications,
                                            List<CustomerCommContactPurposes> tempContactPurposesList,
                                            List<CustomerCommContactPerson> tempContactPersonsList,
                                            List<CustomerCommunicationContacts> tempContactsList,
                                            List<String> exceptions) {
        Optional<CustomerCommunications> commData = customerCommunicationsRepository.findById(request.getId());
        if (commData.isEmpty()) {
            log.error("Customer communications data not found, ID: " + request.getId());
            exceptions.add("Customer communications data not found, ID: " + request.getId());
            return;
        }

        CustomerCommunications customerCommunications = editCustomerCommunications(commData.get(), request, exceptions);

        tempCustomerCommunications.add(customerCommunications);

        editSubObjects(
                request,
                customerCommunications.getId(),
                tempContactPurposesList,
                tempContactPersonsList,
                tempContactsList,
                exceptions
        );
    }

    private void editSubObjects(EditCommunicationDataRequest request,
                                Long customerCommunicationsId,
                                List<CustomerCommContactPurposes> tempContactPurposesList,
                                List<CustomerCommContactPerson> tempContactPersonsList,
                                List<CustomerCommunicationContacts> tempContactsList,
                                List<String> exceptions) {
        contactPurposeService.editContactPurposes(
                customerCommunicationsId,
                request.getContactPurposes(),
                tempContactPurposesList,
                exceptions
        );
        contactPersonService.editContactPersons(
                customerCommunicationsId,
                request.getContactPersons(),
                tempContactPersonsList,
                exceptions
        );
        contactService.editContacts(
                customerCommunicationsId,
                request.getCommunicationContacts(),
                tempContactsList,
                exceptions
        );
    }

    private void deleteRemovedCustomerCommunications(List<EditCommunicationDataRequest> communicationDataRequests,
                                                     List<Long> dbCustomerCommunicationIds,
                                                     List<String> exceptions) {
        if (!dbCustomerCommunicationIds.isEmpty()) {
            List<Long> requestCustomerCommunicationIds = communicationDataRequests
                    .stream()
                    .map(EditCommunicationDataRequest::getId)
                    .toList();

            for (Long id : dbCustomerCommunicationIds) {
                if (!requestCustomerCommunicationIds.contains(id)) {
                    deleteCustomerCommunication(id, exceptions);
                }
            }
        }
    }

    private void deleteCustomerCommunication(Long id, List<String> exceptions) {
        log.debug("Deleting customer communication with ID: {}", id);

        Optional<CustomerCommunications> customerCommunicationsOptional = customerCommunicationsRepository.findById(id);
        if (customerCommunicationsOptional.isEmpty()) {
            log.error("Customer communications not found, ID: " + id);
            exceptions.add("Customer communications not found, ID: " + id);
            return;
        }

        CustomerCommunications customerCommunications = customerCommunicationsOptional.get();
        if (!customerCommunications.getStatus().equals(DELETED)) {
            customerCommunications.setStatus(DELETED);
            // TODO: 18.01.23 set actual user id later
            customerCommunications.setModifySystemUserId("test");
            customerCommunications.setModifyDate(LocalDateTime.now());
        }
    }

    private CustomerCommunications createCustomerCommunications(Long customerDetailId,
                                                                CreateCommunicationDataRequest request,
                                                                List<String> exceptions) {
        CustomerCommunications customerCommunications = new CustomerCommunications();
        customerCommunications.setContactTypeName(request.getContactTypeName());
        // TODO: 17.01.23 address validations
        CustomerAddressRequest address = request.getAddress();
        if (address.getForeign()) {
            fillForeignAddressData(customerCommunications, address.getForeignAddressData());
        } else {
            fillLocalAddressDataWhenCreating(customerCommunications, address.getLocalAddressData(), exceptions);
        }
        customerCommunications.setForeignAddress(address.getForeign());
        customerCommunications.setBlock(address.getBlock());
        customerCommunications.setEntrance(address.getEntrance());
        customerCommunications.setFloor(address.getFloor());
        customerCommunications.setApartment(address.getApartment());
        customerCommunications.setMailbox(address.getMailbox());
        customerCommunications.setAddressAdditionalInfo(address.getAdditionalInformation());
        customerCommunications.setStatus(request.getStatus());
        // TODO: 17.01.23 set actual system user id later
        customerCommunications.setSystemUserId("test");
        customerCommunications.setCreateDate(LocalDateTime.now());
        customerCommunications.setCustomerDetailsId(customerDetailId);
        return customerCommunications;
    }

    private CustomerCommunications editCustomerCommunications(CustomerCommunications dbCustomerCommunications,
                                                              EditCommunicationDataRequest request,
                                                              List<String> exceptions) {
        CustomerCommunications customerCommunications = new CustomerCommunications();
        customerCommunications.setId(dbCustomerCommunications.getId());
        customerCommunications.setCreateDate(dbCustomerCommunications.getCreateDate());
        customerCommunications.setSystemUserId(dbCustomerCommunications.getSystemUserId());
        customerCommunications.setModifyDate(LocalDateTime.now());
        // TODO: 17.01.23 set actual system user id later
        customerCommunications.setModifySystemUserId("test");
        customerCommunications.setContactTypeName(request.getContactTypeName());
        // TODO: 17.01.23 address validations
        CustomerAddressRequest address = request.getAddress();
        if (address.getForeign()) {
            fillForeignAddressData(customerCommunications, address.getForeignAddressData());
        } else {
            fillLocalAddressDataWhenEditing(dbCustomerCommunications, customerCommunications, address.getLocalAddressData(), exceptions);
        }
        customerCommunications.setForeignAddress(address.getForeign());
        customerCommunications.setBlock(address.getBlock());
        customerCommunications.setEntrance(address.getEntrance());
        customerCommunications.setFloor(address.getFloor());
        customerCommunications.setApartment(address.getApartment());
        customerCommunications.setMailbox(address.getMailbox());
        customerCommunications.setAddressAdditionalInfo(address.getAdditionalInformation());
        customerCommunications.setStatus(request.getStatus());
        customerCommunications.setCustomerDetailsId(dbCustomerCommunications.getCustomerDetailsId());
        return customerCommunications;
    }

    private void fillForeignAddressData(CustomerCommunications customerCommunications,
                                        ForeignAddressData foreignAddressData) {
        customerCommunications.setCountryId(foreignAddressData.getCountryId());
        customerCommunications.setRegionForeign(foreignAddressData.getRegion());
        customerCommunications.setMunicipalityForeign(foreignAddressData.getMunicipality());
        customerCommunications.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());
        customerCommunications.setDistrictForeign(foreignAddressData.getDistrict());
        customerCommunications.setStreetForeign(foreignAddressData.getStreet());
        customerCommunications.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
        customerCommunications.setZipCodeForeign(foreignAddressData.getZipCode());
    }

    private void fillLocalAddressDataWhenCreating(CustomerCommunications customerCommunications,
                                                  LocalAddressData localAddressData,
                                                  List<String> exceptions) {
        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (countryOptional.isEmpty()) {
            log.error("Country not found, ID: " + localAddressData.getCountryId());
            exceptions.add("Country not found, ID: " + localAddressData.getCountryId());
            return;
        }

        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (optionalPopulatedPlace.isEmpty()) {
            log.error("Populated place not found, ID: " + localAddressData.getPopulatedPlaceId());
            exceptions.add("Populated place not found, ID: " + localAddressData.getPopulatedPlaceId());
            return;
        } else {
            if (!optionalPopulatedPlace.get()
                    .getMunicipality().getRegion().getCountry().getId()
                    .equals(localAddressData.getCountryId())) {
                log.error("Populated place ID: " + localAddressData.getPopulatedPlaceId() + " does not belong to the entered country");
                exceptions.add("Populated place ID: " + localAddressData.getPopulatedPlaceId() + " does not belong to the entered country");
                return;
            }
        }

        Optional<Street> streetOptional = streetRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getStreetId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (streetOptional.isEmpty()) {
            log.error("Active street ID: " + localAddressData.getStreetId() + " not found in entered populated place");
            exceptions.add("Active street ID: " + localAddressData.getStreetId() + " not found in entered populated place");
            return;
        }

        Optional<ResidentialArea> residentialAreaOptional = residentialAreaRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getResidentialAreaId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (residentialAreaOptional.isEmpty()) {
            log.error("Active residential area ID: " + localAddressData.getResidentialAreaId() + " not found in entered populated place");
            exceptions.add("Active residential area ID: " + localAddressData.getResidentialAreaId() + " not found in entered populated place");
            return;
        }

        Optional<District> districtOptional = districtRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getDistrictId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (districtOptional.isEmpty()) {
            log.error("Active district ID: " + localAddressData.getDistrictId() + " not found in entered populated place");
            exceptions.add("Active district ID: " + localAddressData.getDistrictId() + " not found in entered populated place");
            return;
        }

        Optional<ZipCode> zipCodeOptional = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (zipCodeOptional.isEmpty()) {
            log.error("Active zip code ID: " + localAddressData.getZipCodeId() + " not found in entered populated place");
            exceptions.add("Active zip code ID: " + localAddressData.getZipCodeId() + " not found in entered populated place");
            return;
        }

        customerCommunications.setCountryId(localAddressData.getCountryId());
        customerCommunications.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());
        customerCommunications.setDistrictId(localAddressData.getDistrictId());
        customerCommunications.setStreetId(localAddressData.getStreetId());
        customerCommunications.setResidentialAreaId(localAddressData.getResidentialAreaId());
        customerCommunications.setZipCodeId(localAddressData.getZipCodeId());
    }

    private void fillLocalAddressDataWhenEditing(CustomerCommunications dbCustomerCommunications,
                                                 CustomerCommunications customerCommunications,
                                                 LocalAddressData localAddressData,
                                                 List<String> exceptions) {
        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (countryOptional.isEmpty()) {
            log.error("Country not found, ID: " + localAddressData.getCountryId());
            exceptions.add("Country not found, ID: " + localAddressData.getCountryId());
            return;
        }

        if (countryOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getCountryId().equals(countryOptional.get().getId())) {
                log.error("Country: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Country: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (optionalPopulatedPlace.isEmpty()) {
            log.error("Populated place not found, ID: " + localAddressData.getPopulatedPlaceId());
            exceptions.add("Populated place not found, ID: " + localAddressData.getPopulatedPlaceId());
            return;
        } else {
            if (!optionalPopulatedPlace.get()
                    .getMunicipality().getRegion().getCountry().getId()
                    .equals(localAddressData.getCountryId())) {
                log.error("Populated place ID: " + localAddressData.getPopulatedPlaceId() + " does not belong to the entered country");
                exceptions.add("Populated place ID: " + localAddressData.getPopulatedPlaceId() + " does not belong to the entered country");
                return;
            }
        }

        if (optionalPopulatedPlace.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getPopulatedPlaceId().equals(optionalPopulatedPlace.get().getId())) {
                log.error("Populated place: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Populated place: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        Optional<Street> streetOptional = streetRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getStreetId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                );
        if (streetOptional.isEmpty()) {
            log.error("Active street ID: " + localAddressData.getStreetId() + " not found in entered populated place");
            exceptions.add("Active street ID: " + localAddressData.getStreetId() + " not found in entered populated place");
            return;
        }

        if (streetOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getStreetId().equals(streetOptional.get().getId())) {
                log.error("Street: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Street: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        Optional<ResidentialArea> residentialAreaOptional = residentialAreaRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getResidentialAreaId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                );
        if (residentialAreaOptional.isEmpty()) {
            log.error("Active residential area ID: " + localAddressData.getResidentialAreaId() + " not found in entered populated place");
            exceptions.add("Active residential area ID: " + localAddressData.getResidentialAreaId() + " not found in entered populated place");
            return;
        }

        if (residentialAreaOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getResidentialAreaId().equals(residentialAreaOptional.get().getId())) {
                log.error("Residential area: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Residential area: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        Optional<District> districtOptional = districtRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getDistrictId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                );
        if (districtOptional.isEmpty()) {
            log.error("Active district ID: " + localAddressData.getDistrictId() + " not found in entered populated place");
            exceptions.add("Active district ID: " + localAddressData.getDistrictId() + " not found in entered populated place");
            return;
        }

        if (districtOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getDistrictId().equals(districtOptional.get().getId())) {
                log.error("District: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("District: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        Optional<ZipCode> zipCodeOptional = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (zipCodeOptional.isEmpty()) {
            log.error("Active zip code ID: " + localAddressData.getZipCodeId() + " not found in entered populated place");
            exceptions.add("Active zip code ID: " + localAddressData.getZipCodeId() + " not found in entered populated place");
            return;
        }

        if (zipCodeOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getZipCodeId().equals(zipCodeOptional.get().getId())) {
                log.error("Zip code: Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Zip code: Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        customerCommunications.setCountryId(localAddressData.getCountryId());
        customerCommunications.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());
        customerCommunications.setDistrictId(localAddressData.getDistrictId());
        customerCommunications.setStreetId(localAddressData.getStreetId());
        customerCommunications.setResidentialAreaId(localAddressData.getResidentialAreaId());
        customerCommunications.setZipCodeId(localAddressData.getZipCodeId());
    }

}
