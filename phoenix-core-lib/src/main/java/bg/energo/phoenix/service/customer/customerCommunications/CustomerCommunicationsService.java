package bg.energo.phoenix.service.customer.customerCommunications;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPerson;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.address.*;
import bg.energo.phoenix.model.enums.customer.ContractPurposeType;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.ForeignAddressData;
import bg.energo.phoenix.model.request.customer.LocalAddressData;
import bg.energo.phoenix.model.request.customer.communicationData.*;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.EditContactPersonRequest;
import bg.energo.phoenix.model.response.customer.communicationData.*;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPersonDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPersonRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.Status.ACTIVE;
import static bg.energo.phoenix.model.enums.customer.Status.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCommunicationsService {
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

    private final CommContactPurposeService contactPurposeService;
    private final CommContactService contactService;
    private final CommContactPersonService contactPersonService;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;

    /**
     * Previews all active {@link CustomerCommunications} for presented {@link CustomerDetails} in a compact format that is shown on customer preview page.
     *
     * @param customerDetailId a version of the customer for which the customer communications are requested.
     * @param exceptions       list of errors which is populated in case of exceptions or validation violations
     * @return {@link List<CommunicationDataBasicInfo> List&lt;CommunicationDataBasicInfo&gt;}
     */
    public List<CommunicationDataBasicInfo> previewByCustomerDetailId(Long customerDetailId, List<String> exceptions) {
        if (!customerDetailsRepository.existsById(customerDetailId)) {
            log.error("Customer detail not found, ID: " + customerDetailId);
            exceptions.add("%s-Customer detail not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, customerDetailId));
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

    /**
     * Retrieves detailed information for the requested {@link CustomerCommunications} by the {@link CustomerCommunications} ID.
     * Contains information for all the sub objects belonging to the parent object - contact purposes, contacts and contact persons.
     *
     * @param id ID of the requested {@link CustomerCommunications}.
     * @return DomainEntityNotFoundException if no active {@link CustomerCommunications} was found by the requested ID.
     */
    public CommunicationDataResponse getById(Long id) {
        CustomerCommunications communicationData = customerCommunicationsRepository
                .findByIdAndStatuses(id, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active communication data not found, ID: " + id + ";"));

        LocalAddressInfo localAddressInfo = null;
        ForeignAddressInfo foreignAddressInfo = null;
        if (communicationData.getForeignAddress()) {
            foreignAddressInfo = customerCommunicationsRepository.getForeignAddressInfo(communicationData.getId());
        } else {
            localAddressInfo = customerCommunicationsRepository.getLocalAddressInfo(communicationData.getId());
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

    /**
     * Lists all active {@link CustomerCommunications} for presented ID of the {@link CustomerDetails}.
     * Due to db querying optimization factors, firstly, all {@link CustomerCommunications} objects are fetched,
     * then db is queried to fetch all {@link CustomerCommContactPurposes} by all customer communication ids -
     * the same is done for {@link CustomerCommContactPerson}s and {@link CustomerCommunicationContacts}.
     * These sub object results are collected grouped in maps and used to build final detailed {@link CustomerCommunications} data objects.
     *
     * @param customerDetailId version ID of the customer for which the {@link CustomerCommunications} are requested.
     * @return {@link List<CustomerCommunicationsDetailedResponse> List&lt;CustomerCommunicationsDetailedResponse&gt;} that contains extended information about each {@link CustomerCommunications}
     */
    public List<CustomerCommunicationsDetailedResponse> getCustomerCommunicationsByCustomerDetailId(Long customerDetailId) {
        List<CustomerCommunicationsDetailedResponse> response = new ArrayList<>();

        List<CustomerCommunications> communicationsList = customerCommunicationsRepository
                .findByCustomerDetailIdAndStatuses(customerDetailId, List.of(ACTIVE));

        return fillCommunicationData(
                customerDetailId,
                response,
                communicationsList,
                Arrays.stream(CustomerCommContactTypes.values()).toList()
        );
    }

    public List<CustomerCommunicationsDetailedResponse> getCustomerCommunicationForExpress(Long customerDetailId) {
        List<CustomerCommunicationsDetailedResponse> response = new ArrayList<>();

        List<CustomerCommunications> communicationsList = customerCommunicationsRepository
                .findCommunicationWithBillingAndContract(customerDetailId, communicationContactPurposeProperties.getBillingCommunicationId(), communicationContactPurposeProperties.getContractCommunicationId());

        return fillCommunicationData(
                customerDetailId,
                response,
                communicationsList,
                List.of(
                        CustomerCommContactTypes.EMAIL,
                        CustomerCommContactTypes.LANDLINE_PHONE,
                        CustomerCommContactTypes.MOBILE_NUMBER
                )
        );
    }


    private List<CustomerCommunicationsDetailedResponse> fillCommunicationData(Long customerDetailId,
                                                                               List<CustomerCommunicationsDetailedResponse> response,
                                                                               List<CustomerCommunications> communicationsList,
                                                                               List<CustomerCommContactTypes> customerCommContactTypes) {
        if (communicationsList.isEmpty()) {
            log.debug("No active communication data attached to customer detail, ID: " + customerDetailId);
            return null;
        }

        List<Long> customerCommIds = communicationsList
                .stream()
                .map(CustomerCommunications::getId)
                .toList();

        Map<Long, List<ContactPurposeDetailedResponse>> contactPurposesMap = getContactPurposesByCustomerCommIds(customerCommIds, List.of(ACTIVE));
        Map<Long, List<ContactPersonDetailedResponse>> contactPersonsMap = getContactPersonsByCustomerCommIds(customerCommIds, List.of(ACTIVE));
        Map<Long, List<ContactDetailedResponse>> contactsMap = getContactsByCustomerCommIds(customerCommIds, List.of(ACTIVE), customerCommContactTypes);

        for (CustomerCommunications cc : communicationsList) {
            LocalAddressInfo localAddressInfo = null;
            ForeignAddressInfo foreignAddressInfo = null;
            if (cc.getForeignAddress()) {
                foreignAddressInfo = customerCommunicationsRepository.getForeignAddressInfo(cc.getId());
            } else {
                localAddressInfo = customerCommunicationsRepository.getLocalAddressInfo(cc.getId());
            }

            CustomerCommunicationsDetailedResponse ccdr = new CustomerCommunicationsDetailedResponse(
                    cc,
                    cc.getForeignAddress(),
                    foreignAddressInfo,
                    localAddressInfo,
                    contactPurposesMap.get(cc.getId()),
                    contactPersonsMap.get(cc.getId()),
                    contactsMap.get(cc.getId())
            );

            response.add(ccdr);
        }

        return response;
    }

    /**
     * Retrieves Contact Purposes by Customer Communications.
     *
     * @param customerCommIds ID of {@link CustomerCommunications}
     * @param statuses        {@link List<Status> List&lt;Status&gt;} that contains the requested statuses
     * @return Map&lt;Long, List&lt;ContactPurposeDetailedResponse&gt;&gt; in which {@link CustomerCommContactPurposes} are grouped by belonging {@link CustomerCommunications} ID.
     */
    private Map<Long, List<ContactPurposeDetailedResponse>> getContactPurposesByCustomerCommIds(List<Long> customerCommIds, List<Status> statuses) {
        List<ContactPurposeDetailedResponse> contactPurposes = contactPurposesRepository
                .findByCustomerCommIds(customerCommIds, statuses);
        Map<Long, List<ContactPurposeDetailedResponse>> contactPurposesMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(contactPurposes)) {
            Long billingCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
            Long contractCommunicationId = communicationContactPurposeProperties.getContractCommunicationId();
            contactPurposes
                    .forEach(cp -> {
                        List<ContactPurposeDetailedResponse> value = contactPurposesMap.getOrDefault(cp.getCustomerCommunicationsId(), new ArrayList<>());
                        value.add(cp);
                        if (billingCommunicationId.equals(cp.getId())) {
                            cp.setContractPurposeType(ContractPurposeType.BILLING);
                        } else if (contractCommunicationId.equals(cp.getId())) {
                            cp.setContractPurposeType(ContractPurposeType.CONTRACT);
                        }
                        contactPurposesMap.put(cp.getCustomerCommunicationsId(), value);
                    });
        }
        return contactPurposesMap;
    }

    /**
     * Retrieves Contact Persons by Customer Communications.
     *
     * @param customerCommIds ID of {@link CustomerCommunications}
     * @param statuses        {@link List<Status> List&lt;Status&gt;} that contains the requested statuses
     * @return Map&lt;Long, List&lt;ContactPersonDetailedResponse&gt;&gt; in which {@link CustomerCommContactPerson}s are grouped by belonging {@link CustomerCommunications} ID.
     */
    private Map<Long, List<ContactPersonDetailedResponse>> getContactPersonsByCustomerCommIds(List<Long> customerCommIds, List<Status> statuses) {
        List<ContactPersonDetailedResponse> contactPersons = contactPersonRepository
                .findByCustomerCommIds(customerCommIds, statuses);
        Map<Long, List<ContactPersonDetailedResponse>> contactPersonsMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(contactPersons)) {
            contactPersons
                    .forEach(cp -> {
                        List<ContactPersonDetailedResponse> value = contactPersonsMap.getOrDefault(cp.getCustomerCommunicationsId(), new ArrayList<>());
                        value.add(cp);
                        contactPersonsMap.put(cp.getCustomerCommunicationsId(), value);
                    });
        }
        return contactPersonsMap;
    }

    /**
     * Retrieves Contacts by Customer Communications.
     *
     * @param customerCommIds ID of {@link CustomerCommunications}
     * @param statuses        {@link List<Status> List&lt;Status&gt;} that contains the requested statuses
     * @return Map&lt;Long, List&lt;ContactDetailedResponse&gt;&gt; in which {@link CustomerCommunicationContacts} are grouped by belonging {@link CustomerCommunications} ID.
     */
    private Map<Long, List<ContactDetailedResponse>> getContactsByCustomerCommIds(List<Long> customerCommIds, List<Status> statuses, List<CustomerCommContactTypes> customerCommContactTypes) {
        List<ContactDetailedResponse> contacts = contactsRepository
                .findByCustomerCommIds(customerCommIds, statuses, customerCommContactTypes);
        Map<Long, List<ContactDetailedResponse>> contactsMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(contacts)) {
            contacts
                    .forEach(cp -> {
                        List<ContactDetailedResponse> value = contactsMap.getOrDefault(cp.getCustomerCommunicationsId(), new ArrayList<>());
                        value.add(cp);
                        contactsMap.put(cp.getCustomerCommunicationsId(), value);
                    });
        }
        return contactsMap;
    }

    /**
     * Retrieves detailed information fot the requested {@link CustomerCommContactPerson} by the {@link CustomerCommContactPerson}'s ID.
     *
     * @param communicationDataId ID of {@link CustomerCommunications}
     * @param contactPersonId     ID of the requested {@link CustomerCommContactPerson}.
     * @return {@link ContactPersonResponse} that contains all the information needed to display the {@link CustomerCommContactPerson} in its own modal.
     * @throws DomainEntityNotFoundException if no active {@link CustomerCommunications} or {@link CustomerCommContactPerson} were found.
     */
    public ContactPersonResponse getContactPersonById(Long communicationDataId, Long contactPersonId) {
        if (!customerCommunicationsRepository.existsById(communicationDataId)) {
            log.error("Active communication data not found, ID: " + communicationDataId);
            throw new DomainEntityNotFoundException("communicationDataId-Active communication data not found, ID: " + communicationDataId + ";");
        }

        return contactPersonRepository
                .getDetailedContactPersonByIdAndStatuses(contactPersonId, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active contact person not found, ID: " + contactPersonId + ";"));
    }

    /**
     * Adds {@link CustomerCommunications} to the provided {@link CustomerDetails} if the request list not empty and exceptions are empty.
     * This method should be used only when creating a new customer or a new version.
     *
     * @param communicationDataRequests {@link List<CreateCustomerCommunicationsRequest> List&lt;CreateCustomerCommunicationsRequest&gt;}
     * @param customerDetails           a version of the customer to which the communications should be added
     * @param exceptions                list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void createCustomerCommunicationsData(List<CreateCustomerCommunicationsRequest> communicationDataRequests,
                                                 CustomerDetails customerDetails,
                                                 List<String> exceptions) {
        if (!CollectionUtils.isEmpty(communicationDataRequests)) {
            if (customerDetails == null || customerDetails.getId() == null) {
                log.error("Customer details object is null, cannot create communication data");
                exceptions.add("%s-Customer details object is null, cannot create communication data;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
                return;
            }

            if (!customerDetailsRepository.existsById(customerDetails.getId())) {
                log.error("Customer details not found, ID: " + customerDetails.getId());
                exceptions.add("%s-Customer details not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, customerDetails.getId()));
                return;
            }

            log.debug("Creating communication data: {} to customer detail ID: {}", communicationDataRequests, customerDetails.getId());
            List<CustomerCommContactPurposes> tempContactPurposesList = new ArrayList<>();
            List<CustomerCommContactPerson> tempContactPersonsList = new ArrayList<>();
            List<CustomerCommunicationContacts> tempContactsList = new ArrayList<>();

            for (int i = 0; i < communicationDataRequests.size(); i++) {
                CreateCustomerCommunicationsRequest request = communicationDataRequests.get(i);
                createCustomerCommunicationsSubObjects(
                        request,
                        customerDetails.getId(),
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions,
                        i
                );
            }

            if (exceptions.isEmpty()) {
                contactPurposesRepository.saveAll(tempContactPurposesList);
                contactPersonRepository.saveAll(tempContactPersonsList);
                contactsRepository.saveAll(tempContactsList);
            }
        }
    }

    /**
     * This method takes care of GDPR compliance before adding a list of customer communications to a new version of a customer details object.
     *
     * @param communicationDataRequests a list of {@link EditCustomerCommunicationsRequest}
     * @param newVersion                the new version of the customer details object
     * @param exceptions                a list of exceptions that may occur during the execution of this method
     */
    @Transactional
    public void addCustomerCommunicationsToNewVersion(List<EditCustomerCommunicationsRequest> communicationDataRequests,
                                                      CustomerDetails newVersion,
                                                      CustomerDetails oldVersion,
                                                      List<String> exceptions) {
        if (customerDetailsChecks(newVersion, exceptions)) return;
        if (CollectionUtils.isNotEmpty(communicationDataRequests)) {
            List<CustomerCommContactPerson> dbContactPersonsList = contactPersonRepository.findContactPersonsListByCustomerCommIds(
                    communicationDataRequests.stream().map(EditCustomerCommunicationsRequest::getId).toList(),
                    List.of(ACTIVE)
            );
            log.debug("Creating communication data: {} to customer detail ID: {}", communicationDataRequests, newVersion.getId());
            List<CustomerCommContactPurposes> tempContactPurposesList = new ArrayList<>();
            List<CustomerCommContactPerson> tempContactPersonsList = new ArrayList<>();
            List<CustomerCommunicationContacts> tempContactsList = new ArrayList<>();

            for (int i = 0; i < communicationDataRequests.size(); i++) {
                EditCustomerCommunicationsRequest request = communicationDataRequests.get(i);
                List<EditContactPersonRequest> contactPersons = request.getContactPersons();
                if (CollectionUtils.isNotEmpty(contactPersons)) {
                    for (int j = 0; j < contactPersons.size(); j++) {
                        EditContactPersonRequest contactPersonRequest = contactPersons.get(j);
                        // Request may contain newly added contact persons that have ID null,
                        // and we don't want to look them up in the DB
                        if (contactPersonRequest.getId() != null) {
                            Optional<CustomerCommContactPerson> person = dbContactPersonsList
                                    .stream()
                                    .filter(p -> p.getId().equals(contactPersonRequest.getId()))
                                    .findFirst();

                            if (person.isEmpty()) {
                                log.error("communicationDataRequests[%s].contactPersons[%s]-Error while fetching contact person details for communications ID: [%s];".formatted(i, j, request.getId()));
                                exceptions.add("communicationDataRequests[%s].contactPersons[%s]-Error while fetching contact person details for communications ID: [%s];".formatted(i, j, request.getId()));
                                return;
                            }

                            if (StringUtils.equals(contactPersonRequest.getName(), EPBFinalFields.GDPR)) {
                                contactPersonRequest.setName(person.get().getName());
                            }

                            if (StringUtils.equals(contactPersonRequest.getMiddleName(), EPBFinalFields.GDPR)) {
                                contactPersonRequest.setMiddleName(person.get().getMiddleName());
                            }

                            if (StringUtils.equals(contactPersonRequest.getSurname(), EPBFinalFields.GDPR)) {
                                contactPersonRequest.setSurname(person.get().getSurname());
                            }

                            if (StringUtils.equals(contactPersonRequest.getBirthDate(), EPBFinalFields.GDPR)) {
                                LocalDate birthDate = person.get().getBirthDate();
                                contactPersonRequest.setBirthDate(birthDate == null ? null : birthDate.toString());
                            }

                        }
                    }
                }
                createCreateCommunicationNewVersion(
                        newVersion,
                        oldVersion,
                        exceptions,
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        i,
                        request
                );
            }

            if (exceptions.isEmpty()) {
                contactPurposesRepository.saveAll(tempContactPurposesList);
                contactPersonRepository.saveAll(tempContactPersonsList);
                contactsRepository.saveAll(tempContactsList);
            }
        }
    }

    private void createCreateCommunicationNewVersion(CustomerDetails newVersion, CustomerDetails oldVersion, List<String> exceptions, List<CustomerCommContactPurposes> tempContactPurposesList, List<CustomerCommContactPerson> tempContactPersonsList, List<CustomerCommunicationContacts> tempContactsList, int i, EditCustomerCommunicationsRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("communicationData[%s].status-Cannot set DELETED status when creating communication data;".formatted(i));
            exceptions.add("communicationData[%s].status-Cannot set DELETED status when creating communication data;".formatted(i));
        }

        CustomerCommunications communicationData = new CustomerCommunications();
        Map<Long, CustomerCommunications> communicationsMap = customerCommunicationsRepository.findByCustomerDetailIdAndStatuses(oldVersion.getId(), List.of(ACTIVE))
                .stream().collect(Collectors.toMap(CustomerCommunications::getId, j -> j));
        CustomerCommunications oldCommunications = communicationsMap.get(request.getId());
        communicationData.setContactTypeName(request.getContactTypeName());
        CustomerCommAddressRequest address = request.getAddress();
        if (address.getForeign()) {
            fillForeignAddressDataWhenCreatingForNewVersion(communicationData, oldCommunications, address.getForeignAddressData(), exceptions, i);
        } else {
            fillLocalAddressDataWhenCreatingForNewVersion(communicationData, oldCommunications, address.getLocalAddressData(), exceptions, i);
        }
        fillCommunicationData(request, communicationData, address);
        communicationData.setCustomerDetailsId(newVersion.getId());

        CustomerCommunications customerCommunications = null;
        if (exceptions.isEmpty()) {
            customerCommunications = customerCommunicationsRepository.save(communicationData);
        }


        createSubObjectsForNewVersion(
                request,
                customerCommunications,
                oldCommunications,
                tempContactPurposesList,
                tempContactPersonsList,
                tempContactsList,
                exceptions,
                i
        );

        if (!exceptions.isEmpty() && customerCommunications != null) {
            customerCommunicationsRepository.delete(customerCommunications);
        }
    }

    private boolean customerDetailsChecks(CustomerDetails newVersion, List<String> exceptions) {
        if (newVersion == null || newVersion.getId() == null) {
            log.error("Customer details object is null, cannot create communication data");
            exceptions.add("%s-Customer details object is null, cannot create communication data;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
            return true;
        }

        if (!customerDetailsRepository.existsById(newVersion.getId())) {
            log.error("Customer details not found, ID: " + newVersion.getId());
            exceptions.add("%s-Customer details not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, newVersion.getId()));
            return true;
        }
        return false;
    }

    private void fillCommunicationData(EditCustomerCommunicationsRequest request, CustomerCommunications communicationData, CustomerCommAddressRequest address) {
        communicationData.setForeignAddress(address.getForeign());
        communicationData.setBlock(address.getBlock());
        communicationData.setStreetNumber(address.getNumber());
        communicationData.setEntrance(address.getEntrance());
        communicationData.setFloor(address.getFloor());
        communicationData.setApartment(address.getApartment());
        communicationData.setMailbox(address.getMailbox());
        communicationData.setAddressAdditionalInfo(address.getAdditionalInformation());
        communicationData.setStatus(request.getStatus());
    }

    /**
     * Validates {@link CreateCustomerCommunicationsRequest} request and pass it to sub object creation method.
     *
     * @param request                 {@link CreateCustomerCommunicationsRequest}
     * @param customerDetailsId       ID of the {@link CustomerDetails}
     * @param tempContactPurposesList {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactPersonsList  {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactsList        {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions              list of errors which is populated in case of exceptions or validation violations
     */
    private void createCustomerCommunicationsSubObjects(CreateCustomerCommunicationsRequest request,
                                                        Long customerDetailsId,
                                                        List<CustomerCommContactPurposes> tempContactPurposesList,
                                                        List<CustomerCommContactPerson> tempContactPersonsList,
                                                        List<CustomerCommunicationContacts> tempContactsList,
                                                        List<String> exceptions,
                                                        int index) {
        if (request.getStatus().equals(DELETED)) {
            log.error("communicationData[%s].status-Cannot set DELETED status when creating communication data;".formatted(index));
            exceptions.add("communicationData[%s].status-Cannot set DELETED status when creating communication data;".formatted(index));
        }

        CustomerCommunications communicationData = createCustomerCommunications(customerDetailsId, request, exceptions, index);

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
                exceptions,
                index
        );

        if (!exceptions.isEmpty() && customerCommunications != null) {
            customerCommunicationsRepository.delete(customerCommunications);
        }
    }

    /**
     * Create Sub Objects of Customer Communications.
     *
     * @param request                 {@link CreateCustomerCommunicationsRequest}
     * @param communicationData       ID of the {@link CustomerCommunications}
     * @param tempContactPurposesList {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactPersonsList  {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactsList        {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions              list of errors which is populated in case of exceptions or validation violations
     */
    private void createSubObjects(CreateCustomerCommunicationsRequest request,
                                  CustomerCommunications communicationData,
                                  List<CustomerCommContactPurposes> tempContactPurposesList,
                                  List<CustomerCommContactPerson> tempContactPersonsList,
                                  List<CustomerCommunicationContacts> tempContactsList,
                                  List<String> exceptions,
                                  int index) {
        contactPurposeService.createContactPurposes(
                communicationData,
                request.getContactPurposes(),
                tempContactPurposesList,
                exceptions,
                index
        );
        contactPersonService.createContactPersons(
                communicationData,
                request.getContactPersons(),
                tempContactPersonsList,
                exceptions,
                index
        );
        contactService.createContacts(
                communicationData,
                request.getCommunicationContacts(),
                tempContactsList,
                exceptions,
                index
        );
    }

    private void createSubObjectsForNewVersion(EditCustomerCommunicationsRequest request,
                                               CustomerCommunications communicationData,
                                               CustomerCommunications oldCommunications,
                                               List<CustomerCommContactPurposes> tempContactPurposesList,
                                               List<CustomerCommContactPerson> tempContactPersonsList,
                                               List<CustomerCommunicationContacts> tempContactsList,
                                               List<String> exceptions,
                                               int index) {
        contactPurposeService.createContactPurposesForNewVersion(
                communicationData,
                oldCommunications,
                request.getContactPurposes(),
                tempContactPurposesList,
                exceptions,
                index
        );
        contactPersonService.createContactPersonForNewVersion(
                communicationData,
                oldCommunications,
                request.getContactPersons(),
                tempContactPersonsList,
                exceptions,
                index
        );
        contactService.createContactsForNewVersion(
                communicationData,
                oldCommunications,
                request.getCommunicationContacts(),
                tempContactsList,
                exceptions,
                index
        );
    }

    /**
     * Combines three operations - editing {@link CustomerCommunications} belonging to the provided {@link CustomerDetails} if ID is not null in the request,
     * adding new {@link CustomerCommunications} if the ID is null or deleting removed {@link CustomerCommunications}.
     *
     * @param communicationDataRequests {@link List<EditCustomerCommunicationsRequest> List&lt;EditCustomerCommunicationsRequest&gt;}
     * @param customerDetails           a version of the customer for which the customer communications should be edited
     * @param exceptions                list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void editCustomerCommunicationsData(List<EditCustomerCommunicationsRequest> communicationDataRequests,
                                               CustomerDetails customerDetails,
                                               List<String> exceptions) {
        if (communicationDataRequests == null) {
            communicationDataRequests = Collections.emptyList();
        }

        if (customerDetails == null || customerDetails.getId() == null) {
            log.error("Customer details object is null, cannot edit communication data");
            exceptions.add("%s-Customer details object is null, cannot edit communication data;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
            return;
        }

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer details not found, ID: " + customerDetails.getId());
            exceptions.add("%s-Customer details not found, cannot edit communication data;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
            return;
        }

        List<Long> customerCommunicationIdsByCustomerDetailId = customerCommunicationsRepository
                .getCustomerCommunicationIdsByCustomerDetailId(customerDetails.getId(), List.of(ACTIVE))
                .stream()
                .map(CustomerCommunications::getId)
                .toList();

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer details not found, ID: " + customerDetails.getId());
            exceptions.add("%s-Customer details not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, customerDetails.getId()));
            return;
        }

        List<CustomerCommunications> tempCustomerCommunications = new ArrayList<>();
        List<CustomerCommContactPurposes> tempContactPurposesList = new ArrayList<>();
        List<CustomerCommContactPerson> tempContactPersonsList = new ArrayList<>();
        List<CustomerCommunicationContacts> tempContactsList = new ArrayList<>();

        for (int i = 0; i < communicationDataRequests.size(); i++) {
            EditCustomerCommunicationsRequest request = communicationDataRequests.get(i);
            if (request.getStatus().equals(DELETED)) {
                log.error("communicationData[%s].status-Cannot set DELETED status when editing communication data;".formatted(i));
                exceptions.add("communicationData[%s].status-Cannot set DELETED status when editing communication data;".formatted(i));
            }

            if (request.getId() == null) {
                createCustomerCommunicationsSubObjects(
                        new CreateCustomerCommunicationsRequest(request),
                        customerDetails.getId(),
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions,
                        i
                );
            } else {
                editCustomerCommunicationsSubObjects(
                        request,
                        tempCustomerCommunications,
                        tempContactPurposesList,
                        tempContactPersonsList,
                        tempContactsList,
                        exceptions,
                        i
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

    /**
     * Edits customer communications, prepare them in a temporary list and pass them to sub objects editing methods to continue validations.
     *
     * @param request                    {@link EditCustomerCommunicationsRequest}
     * @param tempCustomerCommunications {@link List<CustomerCommunications> List&lt;CustomerCommunications&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactPurposesList    {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactPersonsList     {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactsList           {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions                 list of errors which is populated in case of exceptions or validation violations
     */
    private void editCustomerCommunicationsSubObjects(EditCustomerCommunicationsRequest request,
                                                      List<CustomerCommunications> tempCustomerCommunications,
                                                      List<CustomerCommContactPurposes> tempContactPurposesList,
                                                      List<CustomerCommContactPerson> tempContactPersonsList,
                                                      List<CustomerCommunicationContacts> tempContactsList,
                                                      List<String> exceptions,
                                                      int commDataIndex) {
        Optional<CustomerCommunications> commData = customerCommunicationsRepository.findById(request.getId());
        if (commData.isEmpty()) {
            log.error("communicationData[%s].id-Customer communications data not found, ID: [%s];".formatted(commDataIndex, request.getId()));
            exceptions.add("communicationData[%s].id-Customer communications data not found, ID: [%s];".formatted(commDataIndex, request.getId()));
            return;
        }

        CustomerCommunications customerCommunications = editCustomerCommunications(commData.get(), request, exceptions, commDataIndex);

        tempCustomerCommunications.add(customerCommunications);

        editSubObjects(
                request,
                customerCommunications.getId(),
                tempContactPurposesList,
                tempContactPersonsList,
                tempContactsList,
                exceptions,
                commDataIndex
        );
    }

    /**
     * <h2>Edit Sub Objects of Customer Communications</h2>
     *
     * @param request                  {@link EditCustomerCommunicationsRequest}
     * @param customerCommunicationsId ID of the {@link CustomerCommunications}
     * @param tempContactPurposesList  {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactPersonsList   {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param tempContactsList         {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     */
    private void editSubObjects(EditCustomerCommunicationsRequest request,
                                Long customerCommunicationsId,
                                List<CustomerCommContactPurposes> tempContactPurposesList,
                                List<CustomerCommContactPerson> tempContactPersonsList,
                                List<CustomerCommunicationContacts> tempContactsList,
                                List<String> exceptions,
                                int commDataIndex) {
        contactPurposeService.editContactPurposes(
                customerCommunicationsId,
                request.getContactPurposes(),
                tempContactPurposesList,
                exceptions,
                commDataIndex
        );
        contactPersonService.editContactPersons(
                customerCommunicationsId,
                request.getContactPersons(),
                tempContactPersonsList,
                exceptions,
                commDataIndex
        );
        contactService.editContacts(
                customerCommunicationsId,
                request.getCommunicationContacts(),
                tempContactsList,
                exceptions,
                commDataIndex
        );
    }

    /**
     * Loops over persisted {@link CustomerCommunications} and deletes them if not found in the provided request.
     *
     * @param communicationDataRequests  {@link List<EditCustomerCommunicationsRequest> List&lt;EditCustomerCommunicationsRequest&gt;}
     * @param dbCustomerCommunicationIds ID of the persisted {@link CustomerCommunications}
     * @param exceptions                 list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteRemovedCustomerCommunications(List<EditCustomerCommunicationsRequest> communicationDataRequests,
                                                     List<Long> dbCustomerCommunicationIds,
                                                     List<String> exceptions) {
        if (!dbCustomerCommunicationIds.isEmpty()) {
            List<Long> requestCustomerCommunicationIds = communicationDataRequests
                    .stream()
                    .map(EditCustomerCommunicationsRequest::getId)
                    .toList();

            for (Long id : dbCustomerCommunicationIds) {
                if (!requestCustomerCommunicationIds.contains(id)) {
                    deleteCustomerCommunication(id, exceptions);
                }
            }
        }
    }

    /**
     * Deletes {@link CustomerCommunications} if not already deleted. Only {@link CustomerCommunications} is deleted, statuses of the sub objects won't be changed.
     *
     * @param id         ID of the {@link CustomerCommunications}
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteCustomerCommunication(Long id, List<String> exceptions) {
        log.debug("Deleting customer communication with ID: {}", id);

        Optional<CustomerCommunications> customerCommunicationsOptional = customerCommunicationsRepository.findById(id);
        if (customerCommunicationsOptional.isEmpty()) {
            log.error("Customer communications not found, ID: " + id);
            exceptions.add("id-Customer communications not found, ID: " + id + ";");
            return;
        }

        CustomerCommunications customerCommunications = customerCommunicationsOptional.get();
        if (!customerCommunications.getStatus().equals(DELETED)) {
            customerCommunications.setStatus(DELETED);
        }
    }

    /**
     * <h2>Create Customer Communications Object</h2>
     *
     * @param customerDetailId ID of {@link CustomerDetails}
     * @param request          {@link CreateCustomerCommunicationsRequest}
     * @param exceptions       list of errors which is populated in case of exceptions or validation violations
     * @return populated {@link CustomerCommunications} object
     */
    private CustomerCommunications createCustomerCommunications(Long customerDetailId,
                                                                CreateCustomerCommunicationsRequest request,
                                                                List<String> exceptions,
                                                                int index) {
        CustomerCommunications customerCommunications = new CustomerCommunications();
        customerCommunications.setContactTypeName(request.getContactTypeName());
        CustomerCommAddressRequest address = request.getAddress();
        if (address.getForeign()) {
            fillForeignAddressDataWhenCreating(customerCommunications, address.getForeignAddressData(), exceptions, index);
        } else {
            fillLocalAddressDataWhenCreating(customerCommunications, address.getLocalAddressData(), exceptions, index);
        }
        customerCommunications.setForeignAddress(address.getForeign());
        customerCommunications.setBlock(address.getBlock());
        customerCommunications.setStreetNumber(address.getNumber());
        customerCommunications.setEntrance(address.getEntrance());
        customerCommunications.setFloor(address.getFloor());
        customerCommunications.setApartment(address.getApartment());
        customerCommunications.setMailbox(address.getMailbox());
        customerCommunications.setAddressAdditionalInfo(address.getAdditionalInformation());
        customerCommunications.setStatus(request.getStatus());
        customerCommunications.setCustomerDetailsId(customerDetailId);
        return customerCommunications;
    }

    /**
     * <h2>Edit Customer Communications Object</h2>
     *
     * @param dbCustomerCommunications persisted {@link CustomerCommunications}
     * @param request                  {@link EditCustomerCommunicationsRequest}
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     * @return populated {@link CustomerCommunications} object
     */
    private CustomerCommunications editCustomerCommunications(CustomerCommunications dbCustomerCommunications,
                                                              EditCustomerCommunicationsRequest request,
                                                              List<String> exceptions,
                                                              int index) {
        dbCustomerCommunications.setContactTypeName(request.getContactTypeName());
        CustomerCommAddressRequest address = request.getAddress();
        if (address.getForeign()) {
            fillForeignAddressDataWhenEditing(dbCustomerCommunications, address.getForeignAddressData(), exceptions, index);
        } else {
            fillLocalAddressDataWhenEditing(dbCustomerCommunications, address.getLocalAddressData(), exceptions, index);
        }
        fillCommunicationData(request, dbCustomerCommunications, address);
        dbCustomerCommunications.setCustomerDetailsId(dbCustomerCommunications.getCustomerDetailsId());
        return dbCustomerCommunications;
    }

    /**
     * <h2>Fill Foreign Address Data for Creation Operation</h2>
     * This method checks if nomenclatures are active and all mandatory fields are provided.
     *
     * @param customerCommunications ID of {@link CustomerCommunications}
     * @param foreignAddressData     {@link ForeignAddressData}
     * @param exceptions             list of errors which is populated in case of exceptions or validation violations
     */
    private void fillForeignAddressDataWhenCreating(CustomerCommunications customerCommunications,
                                                    CustomerCommForeignAddressData foreignAddressData,
                                                    List<String> exceptions,
                                                    int commDataIndex) {
        if (foreignAddressData.getCountryId() == null) {
            log.error("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
        }

        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(foreignAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE));
        foreignAddressChecks(customerCommunications, foreignAddressData, exceptions, commDataIndex, countryOptional);
    }

    private void fillForeignAddressDataWhenCreatingForNewVersion(CustomerCommunications customerCommunications,
                                                                 CustomerCommunications oldCommuncations,
                                                                 CustomerCommForeignAddressData foreignAddressData,
                                                                 List<String> exceptions,
                                                                 int commDataIndex) {
        if (foreignAddressData.getCountryId() == null) {
            log.error("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
        }

        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(foreignAddressData.getCountryId(), getAddressStatusForNewVersion(oldCommuncations == null ? null : oldCommuncations.getCountryId(), foreignAddressData.getCountryId()));
        foreignAddressChecks(customerCommunications, foreignAddressData, exceptions, commDataIndex, countryOptional);
    }

    private void foreignAddressChecks(CustomerCommunications customerCommunications, CustomerCommForeignAddressData foreignAddressData, List<String> exceptions, int commDataIndex, Optional<Country> countryOptional) {
        if (countryOptional.isEmpty()) {
            log.error("communicationData[%s].foreignAddressData.countryId-Active country not found, ID: [%s];".formatted(commDataIndex, foreignAddressData.getCountryId()));
            exceptions.add("communicationData[%s].foreignAddressData.countryId-Active country not found, ID: [%s];".formatted(commDataIndex, foreignAddressData.getCountryId()));
        }
        customerCommunications.setCountryId(foreignAddressData.getCountryId());

        if (StringUtils.isEmpty(foreignAddressData.getRegion())) {
            log.error("communicationData[%s].foreignAddressData.region-Region is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.region-Region is mandatory in foreign address;".formatted(commDataIndex));
        }
        customerCommunications.setRegionForeign(foreignAddressData.getRegion());

        if (StringUtils.isEmpty(foreignAddressData.getMunicipality())) {
            log.error("communicationData[%s].foreignAddressData.municipality-Municipality is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.municipality-Municipality is mandatory in foreign address;".formatted(commDataIndex));
        }
        customerCommunications.setMunicipalityForeign(foreignAddressData.getMunicipality());

        if (StringUtils.isEmpty(foreignAddressData.getPopulatedPlace())) {
            log.error("communicationData[%s].foreignAddressData.populatedPlace-Populated place is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.populatedPlace-Populated place is mandatory in foreign address;".formatted(commDataIndex));
        }
        customerCommunications.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());

        if (StringUtils.isEmpty(foreignAddressData.getZipCode())) {
            log.error("communicationData[%s].foreignAddressData.zipCode-Zip code is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.zipCode-Zip code is mandatory in foreign address;".formatted(commDataIndex));
            return;
        }
        customerCommunications.setZipCodeForeign(foreignAddressData.getZipCode());

        customerCommunications.setDistrictForeign(foreignAddressData.getDistrict());
        customerCommunications.setStreetTypeForeign(foreignAddressData.getStreetType());
        customerCommunications.setStreetForeign(foreignAddressData.getStreet());
        customerCommunications.setResidentialAreaTypeForeign(foreignAddressData.getResidentialAreaType());
        customerCommunications.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
    }

    private List<NomenclatureItemStatus> getAddressStatusForNewVersion(Long oldAddressId, Long newAddressId) {
        if (Objects.equals(oldAddressId, newAddressId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h2>Fill Foreign Address Data for Editing Operation</h2>
     * This method checks if all mandatory fields are provided and nomenclatures are active/inactive and if nomenclatures are inactive
     * - whether they are different from the persisted {@link CustomerCommunications} nomenclatures.
     *
     * @param foreignAddressData {@link ForeignAddressData}
     * @param exceptions         list of errors which is populated in case of exceptions or validation violations
     */
    private void fillForeignAddressDataWhenEditing(CustomerCommunications dbCustomerCommunications,
                                                   CustomerCommForeignAddressData foreignAddressData,
                                                   List<String> exceptions,
                                                   int commDataIndex) {
        if (foreignAddressData.getCountryId() == null) {
            log.error("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.countryId-Country is mandatory in foreign address;".formatted(commDataIndex));
        }

        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(foreignAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (countryOptional.isEmpty()) {
            log.error("communicationData[%s].foreignAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, foreignAddressData.getCountryId()));
            exceptions.add("communicationData[%s].foreignAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, foreignAddressData.getCountryId()));
            return;
        }

        if (countryOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getCountryId().equals(countryOptional.get().getId())) {
                log.error("communicationData[%s].foreignAddressData.countryId-Country: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                exceptions.add("communicationData[%s].foreignAddressData.countryId-Country: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
            }
        }

        dbCustomerCommunications.setCountryId(foreignAddressData.getCountryId());

        if (StringUtils.isEmpty(foreignAddressData.getRegion())) {
            log.error("communicationData[%s].foreignAddressData.region-Region is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.region-Region is mandatory in foreign address;".formatted(commDataIndex));
        }
        dbCustomerCommunications.setRegionForeign(foreignAddressData.getRegion());

        if (StringUtils.isEmpty(foreignAddressData.getMunicipality())) {
            log.error("communicationData[%s].foreignAddressData.municipality-Municipality is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.municipality-Municipality is mandatory in foreign address;".formatted(commDataIndex));
        }
        dbCustomerCommunications.setMunicipalityForeign(foreignAddressData.getMunicipality());

        if (StringUtils.isEmpty(foreignAddressData.getPopulatedPlace())) {
            log.error("communicationData[%s].foreignAddressData.populatedPlace-Populated place is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.populatedPlace-Populated place is mandatory in foreign address;".formatted(commDataIndex));
        }
        dbCustomerCommunications.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());

        if (StringUtils.isEmpty(foreignAddressData.getZipCode())) {
            log.error("communicationData[%s].foreignAddressData.zipCode-Customer communications: Zip code is mandatory in foreign address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].foreignAddressData.zipCode-Customer communications: Zip code is mandatory in foreign address;".formatted(commDataIndex));
            return;
        }
        dbCustomerCommunications.setZipCodeForeign(foreignAddressData.getZipCode());

        dbCustomerCommunications.setDistrictForeign(foreignAddressData.getDistrict());
        dbCustomerCommunications.setStreetTypeForeign(foreignAddressData.getStreetType());
        dbCustomerCommunications.setStreetForeign(foreignAddressData.getStreet());
        dbCustomerCommunications.setResidentialAreaTypeForeign(foreignAddressData.getResidentialAreaType());
        dbCustomerCommunications.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
    }

    /**
     * <h2>Fill Local Address Data for Creation Operation</h2>
     * This method checks if nomenclatures are active, all mandatory fields are provided and the address chain is valid.
     *
     * @param customerCommunications ID of {@link CustomerCommunications}
     * @param localAddressData       {@link LocalAddressData}
     * @param exceptions             list of errors which is populated in case of exceptions or validation violations
     */
    private void fillLocalAddressDataWhenCreating(CustomerCommunications customerCommunications,
                                                  CustomerCommLocalAddressData localAddressData,
                                                  List<String> exceptions,
                                                  int commDataIndex) {
        if (localAddressData.getCountryId() == null) {
            log.error("communicationData[%s].localAddressData.countryId-Country is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.countryId-Country is mandatory in local address;".formatted(commDataIndex));
            return;
        }

        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (countryOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            exceptions.add("communicationData[%s].localAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            return;
        }

        if (localAddressData.getPopulatedPlaceId() == null) {
            log.error("communicationData[%s].localAddressData.populatedPlaceId-Populated place is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.populatedPlaceId-Populated place is mandatory in local address;".formatted(commDataIndex));
            return;
        }
        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (optionalPopulatedPlace.isEmpty()) {
            log.error("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            return;
        } else {
            if (!optionalPopulatedPlace.get()
                    .getMunicipality().getRegion().getCountry().getId()
                    .equals(localAddressData.getCountryId())) {
                log.error("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                exceptions.add("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                return;
            }
        }

        if (localAddressData.getZipCodeId() == null) {
            log.error("communicationData[%s].localAddressData.zipCodeId-Zip code is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.zipCodeId-Zip code is mandatory in local address;".formatted(commDataIndex));
            return;
        }
        Optional<ZipCode> zipCodeOptional = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE)
                );
        if (zipCodeOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.zipCodeId-Active zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.zipCodeId-Active zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            return;
        }

        if (localAddressData.getStreetId() != null) {
            Optional<Street> streetOptional = streetRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getStreetId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE)
                    );
            if (streetOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.streetId-Active street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.streetId-Active street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setStreetId(localAddressData.getStreetId());
        }

        if (localAddressData.getResidentialAreaId() != null) {
            Optional<ResidentialArea> residentialAreaOptional = residentialAreaRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getResidentialAreaId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE)
                    );
            if (residentialAreaOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.residentialAreaId-Active residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.residentialAreaId-Active residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setResidentialAreaId(localAddressData.getResidentialAreaId());
        }

        if (localAddressData.getDistrictId() != null) {
            Optional<District> districtOptional = districtRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getDistrictId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE)
                    );
            if (districtOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.districtId-Active district ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.districtId-Active district ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setDistrictId(localAddressData.getDistrictId());
        }

        customerCommunications.setCountryId(localAddressData.getCountryId());
        customerCommunications.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());
        customerCommunications.setZipCodeId(localAddressData.getZipCodeId());
        customerCommunications.setStreetType(localAddressData.getStreetType());
        customerCommunications.setResidentialAreaType(localAddressData.getResidentialAreaType());
    }

    private void fillLocalAddressDataWhenCreatingForNewVersion(CustomerCommunications customerCommunications,
                                                               CustomerCommunications oldCommunications,
                                                               CustomerCommLocalAddressData localAddressData,

                                                               List<String> exceptions,
                                                               int commDataIndex) {
        if (localAddressData.getCountryId() == null) {
            log.error("communicationData[%s].localAddressData.countryId-Country is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.countryId-Country is mandatory in local address;".formatted(commDataIndex));
            return;
        }

        boolean oldCommunicationsExists = oldCommunications == null;
        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(localAddressData.getCountryId(), getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getCountryId(), localAddressData.getCountryId()));
        if (countryOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            exceptions.add("communicationData[%s].localAddressData.countryId-Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            return;
        }

        if (localAddressData.getPopulatedPlaceId() == null) {
            log.error("communicationData[%s].localAddressData.populatedPlaceId-Populated place is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.populatedPlaceId-Populated place is mandatory in local address;".formatted(commDataIndex));
            return;
        }
        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getPopulatedPlaceId(), localAddressData.getPopulatedPlaceId()));
        if (optionalPopulatedPlace.isEmpty()) {
            log.error("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            return;
        } else {
            if (!optionalPopulatedPlace.get()
                    .getMunicipality().getRegion().getCountry().getId()
                    .equals(localAddressData.getCountryId())) {
                log.error("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                exceptions.add("communicationData[%s].localAddressData.PopulatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                return;
            }
        }

        if (localAddressData.getZipCodeId() == null) {
            log.error("communicationData[%s].localAddressData.zipCodeId-Zip code is mandatory in local address;".formatted(commDataIndex));
            exceptions.add("communicationData[%s].localAddressData.zipCodeId-Zip code is mandatory in local address;".formatted(commDataIndex));
            return;
        }
        Optional<ZipCode> zipCodeOptional = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        optionalPopulatedPlace.get().getId(),
                        getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getZipCodeId(), localAddressData.getZipCodeId())
                );
        if (zipCodeOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.zipCodeId-Active zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.zipCodeId-Active zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            return;
        }

        if (localAddressData.getStreetId() != null) {
            Optional<Street> streetOptional = streetRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getStreetId(),
                            optionalPopulatedPlace.get().getId(),
                            getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getStreetId(), localAddressData.getStreetId())
                    );
            if (streetOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.streetId-Active street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.streetId-Active street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setStreetId(localAddressData.getStreetId());
        }

        if (localAddressData.getResidentialAreaId() != null) {
            Optional<ResidentialArea> residentialAreaOptional = residentialAreaRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getResidentialAreaId(),
                            optionalPopulatedPlace.get().getId(),
                            getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getResidentialAreaId(), localAddressData.getResidentialAreaId())
                    );
            if (residentialAreaOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.residentialAreaId-Active residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.residentialAreaId-Active residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setResidentialAreaId(localAddressData.getResidentialAreaId());
        }

        if (localAddressData.getDistrictId() != null) {
            Optional<District> districtOptional = districtRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getDistrictId(),
                            optionalPopulatedPlace.get().getId(),
                            getAddressStatusForNewVersion(oldCommunicationsExists ? null : oldCommunications.getDistrictId(), localAddressData.getDistrictId())
                    );
            if (districtOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.districtId-Active district ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.districtId-Active district ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                return;
            }
            customerCommunications.setDistrictId(localAddressData.getDistrictId());
        }

        customerCommunications.setCountryId(localAddressData.getCountryId());
        customerCommunications.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());
        customerCommunications.setZipCodeId(localAddressData.getZipCodeId());
        customerCommunications.setStreetType(localAddressData.getStreetType());
        customerCommunications.setResidentialAreaType(localAddressData.getResidentialAreaType());
    }

    /**
     * <h2>Fill Local Address Data for Editing Operation</h2>
     * This method checks if all mandatory fields are provided, address chain is valid and nomenclatures are active/inactive and if nomenclatures are inactive
     * - whether they are different from the persisted {@link CustomerCommunications} nomenclatures.
     *
     * @param dbCustomerCommunications persisted {@link CustomerCommunications}
     * @param localAddressData         {@link LocalAddressData}
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     */
    private void fillLocalAddressDataWhenEditing(CustomerCommunications dbCustomerCommunications,
                                                 CustomerCommLocalAddressData localAddressData,
                                                 List<String> exceptions,
                                                 int commDataIndex) {
        Optional<Country> countryOptional = countryRepository.findByIdAndStatus(localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (countryOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.countryId-ACTIVE/INACTIVE Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            exceptions.add("communicationData[%s].localAddressData.countryId-ACTIVE/INACTIVE Country not found, ID: [%s];".formatted(commDataIndex, localAddressData.getCountryId()));
            return;
        }

        if (countryOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getCountryId().equals(countryOptional.get().getId())) {
                log.error("communicationData[%s].localAddressData.countryId-Country: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                exceptions.add("communicationData[%s].localAddressData.countryId-Country: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
            }
        }

        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (optionalPopulatedPlace.isEmpty()) {
            log.error("communicationData[%s].localAddressData.populatedPlaceId-ACTIVE/INACTIVE Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.populatedPlaceId-ACTIVE/INACTIVE Populated place not found, ID: [%s];".formatted(commDataIndex, localAddressData.getPopulatedPlaceId()));
            return;
        } else {
            if (!optionalPopulatedPlace.get()
                    .getMunicipality().getRegion().getCountry().getId()
                    .equals(localAddressData.getCountryId())) {
                log.error("communicationData[%s].localAddressData.populatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                exceptions.add("communicationData[%s].localAddressData.populatedPlaceId-Populated place ID: [%s] does not belong to the entered country ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getPopulatedPlaceId(), localAddressData.getCountryId()));
                return;
            }
        }

        if (optionalPopulatedPlace.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getPopulatedPlaceId().equals(optionalPopulatedPlace.get().getId())) {
                log.error("communicationData[%s].localAddressData.populatedPlaceId-Populated place: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                exceptions.add("communicationData[%s].localAddressData.populatedPlaceId-Populated place: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
            }
        }

        Optional<ZipCode> zipCodeOptional = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        optionalPopulatedPlace.get().getId(),
                        List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                );
        if (zipCodeOptional.isEmpty()) {
            log.error("communicationData[%s].localAddressData.zipCodeId-ACTIVE/INACTIVE Zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            exceptions.add("communicationData[%s].localAddressData.zipCodeId-ACTIVE/INACTIVE Zip code ID: [%s] not found in entered populated place ID: [%s];"
                    .formatted(commDataIndex, localAddressData.getZipCodeId(), localAddressData.getPopulatedPlaceId()));
            return;
        }

        if (zipCodeOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbCustomerCommunications.getZipCodeId().equals(zipCodeOptional.get().getId())) {
                log.error("communicationData[%s].localAddressData.zipCodeId-Zip code: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                exceptions.add("communicationData[%s].localAddressData.zipCodeId-Zip code: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
            }
        }

        if (localAddressData.getStreetId() != null) {
            Optional<Street> streetOptional = streetRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getStreetId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                    );
            if (streetOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.streetId-ACTIVE/INACTIVE Street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.streetId-ACTIVE/INACTIVE Street ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getStreetId(), localAddressData.getPopulatedPlaceId()));
                return;
            }

            if (streetOptional.get().getStatus().equals(INACTIVE)) {
                if (!dbCustomerCommunications.getStreetId().equals(streetOptional.get().getId())) {
                    log.error("communicationData[%s].localAddressData.streetId-Street: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                    exceptions.add("communicationData[%s].localAddressData.streetId-Street: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                }
            }
            dbCustomerCommunications.setStreetId(localAddressData.getStreetId());
        } else {
            dbCustomerCommunications.setStreetId(null);
        }

        if (localAddressData.getResidentialAreaId() != null) {
            Optional<ResidentialArea> residentialAreaOptional = residentialAreaRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getResidentialAreaId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                    );
            if (residentialAreaOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.residentialAreaId-ACTIVE/INACTIVE Residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.residentialAreaId-ACTIVE/INACTIVE Residential area ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getResidentialAreaId(), localAddressData.getPopulatedPlaceId()));
                return;
            }

            if (residentialAreaOptional.get().getStatus().equals(INACTIVE)) {
                if (!dbCustomerCommunications.getResidentialAreaId().equals(residentialAreaOptional.get().getId())) {
                    log.error("communicationData[%s].localAddressData.residentialAreaId-Residential area: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                    exceptions.add("communicationData[%s].localAddressData.residentialAreaId-Residential area: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                }
            }
            dbCustomerCommunications.setResidentialAreaId(localAddressData.getResidentialAreaId());
        } else {
            dbCustomerCommunications.setResidentialAreaId(null);
        }

        if (localAddressData.getDistrictId() != null) {
            Optional<District> districtOptional = districtRepository
                    .findByIdAndPopulatedPlaceIdAndStatus(
                            localAddressData.getDistrictId(),
                            optionalPopulatedPlace.get().getId(),
                            List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
                    );
            if (districtOptional.isEmpty()) {
                log.error("communicationData[%s].localAddressData.districtId-ACTIVE/INACTIVE District ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                exceptions.add("communicationData[%s].localAddressData.districtId-ACTIVE/INACTIVE District ID: [%s] not found in entered populated place ID: [%s];"
                        .formatted(commDataIndex, localAddressData.getDistrictId(), localAddressData.getPopulatedPlaceId()));
                return;
            }

            if (districtOptional.get().getStatus().equals(INACTIVE)) {
                if (!dbCustomerCommunications.getDistrictId().equals(districtOptional.get().getId())) {
                    log.error("communicationData[%s].localAddressData.districtId-District: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                    exceptions.add("communicationData[%s].localAddressData.districtId-District: Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex));
                }
            }
            dbCustomerCommunications.setDistrictId(localAddressData.getDistrictId());
        } else {
            dbCustomerCommunications.setDistrictId(null);
        }

        dbCustomerCommunications.setCountryId(localAddressData.getCountryId());
        dbCustomerCommunications.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());
        dbCustomerCommunications.setZipCodeId(localAddressData.getZipCodeId());
        dbCustomerCommunications.setStreetType(localAddressData.getStreetType());
        dbCustomerCommunications.setResidentialAreaType(localAddressData.getResidentialAreaType());
    }

}
