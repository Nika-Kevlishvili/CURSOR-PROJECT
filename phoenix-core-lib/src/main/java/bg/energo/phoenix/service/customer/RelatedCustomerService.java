package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.RelatedCustomer;
import bg.energo.phoenix.model.entity.nomenclature.customer.CiConnectionType;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.relatedCustomer.BaseRelatedCustomerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.CreateRelatedCustomerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.EditRelatedCustomerRequest;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.RelatedCustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.CiConnectionTypeRepository;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;

import static bg.energo.phoenix.model.enums.customer.CustomerType.LEGAL_ENTITY;
import static bg.energo.phoenix.model.enums.customer.CustomerType.PRIVATE_CUSTOMER;


@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedCustomerService {
    private final RelatedCustomerRepository relatedCustomerRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CiConnectionTypeRepository ciConnectionTypeRepository;

    /**
     * Previews all active {@link RelatedCustomer}s for presented {@link Customer} in a compact format that is shown on customer preview page.
     *
     * @param customer customer for which the related customers are requested.
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     * @return {@link List<RelatedCustomerBasicInfo> List&lt;RelatedCustomerBasicInfo&gt;}
     */
    public List<RelatedCustomerBasicInfo> previewByCustomer(Customer customer, List<String> exceptions) {
        if (customer == null) {
            log.error("Customer object is null, cannot retrieve related customers");
            exceptions.add("Customer object is null, cannot retrieve related customers;");
            return null;
        }

        if (!customerRepository.existsById(customer.getId())) {
            log.error("Customer not found, ID: " + customer.getId());
            exceptions.add("relatedCustomers.id-Customer not found, ID: " + customer.getId() + ";");
            return null;
        }

        log.debug("Fetching related customers for customer ID: {}", customer.getId());
        return relatedCustomerRepository.getRelatedCustomersByCustomerId(customer.getId(), Status.ACTIVE);
    }

    /**
     * Retrieves detailed information for the requested {@link RelatedCustomer} by the {@link Customer}'s ID.
     *
     * @param id ID of the requested {@link RelatedCustomer}.
     * @return {@link RelatedCustomerResponse} that contains all the information needed to display the {@link RelatedCustomer} in its own modal.
     * @throws DomainEntityNotFoundException if no active {@link RelatedCustomer} or his/her details were found.
     */
    public RelatedCustomerResponse getDetailedRelatedCustomerById(Long id) {
        log.debug("Fetching related customer by ID: {}", id);

        RelatedCustomer relatedCustomerRecord = relatedCustomerRepository
                .findRelatedCustomerByIdAndStatuses(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("relatedCustomers.id-Active related Customer record not found, ID: " + id + ";"));

        Customer relatedCustomer = customerRepository
                .findById(relatedCustomerRecord.getRelatedCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "relatedCustomers.id-Related customer record not found in customer table, ID: "
                        + relatedCustomerRecord.getRelatedCustomerId() + ";"
                ));

        CustomerDetails customerDetails = customerDetailsRepository
                .findFirstByCustomerId(relatedCustomer.getId(), Sort.by(Sort.Direction.DESC, "versionId"))
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Detail not found for customer ID " + relatedCustomer.getId() + ";"));

        return new RelatedCustomerResponse(relatedCustomerRecord, relatedCustomer, customerDetails);
    }

    /**
     * Lists all active {@link RelatedCustomer}s for presented ID of the {@link Customer},
     * that include customers that are related to the presented customer and customers that the presented customer is related to.
     *
     * @param customerId ID of the customer for which the {@link RelatedCustomer}s are requested.
     * @return {@link List<RelatedCustomerResponse> List&lt;RelatedCustomerResponse&gt;} that contains extended information about each {@link RelatedCustomer}
     */
    public List<RelatedCustomerResponse> getRelatedCustomersByCustomerId(Long customerId) {
        log.debug("Fetching related customers by customer ID: {}", customerId);
        List<RelatedCustomerResponse> relatedCustomersOfUser = relatedCustomerRepository.getRelatedCustomersByCustomerId(customerId, List.of(Status.ACTIVE));
        List<RelatedCustomerResponse> customersUserIsRelatedTo = relatedCustomerRepository.getCustomersUserIsRelatedTo(customerId, List.of(Status.ACTIVE));
        List<RelatedCustomerResponse> relatedCustomers = new ArrayList<>(Stream.concat(relatedCustomersOfUser.stream(), customersUserIsRelatedTo.stream()).toList());
        relatedCustomers.sort(Comparator.comparing(RelatedCustomerResponse::getId));
        return relatedCustomers;
    }

    /**
     * Adds {@link RelatedCustomer}s to the provided {@link Customer} if the request list not empty and exceptions are empty.
     * This method should be used only when creating a new customer or a new version.
     *
     * @param relatedCustomerRequests {@link List<CreateRelatedCustomerRequest> List&lt;CreateRelatedCustomerRequest&gt;}
     * @param customer customer to which the related customers should be added
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void addRelatedCustomers(List<CreateRelatedCustomerRequest> relatedCustomerRequests,
                                    Customer customer,
                                    List<String> exceptions) {
        if (!CollectionUtils.isEmpty(relatedCustomerRequests)) {
            if (customer == null) {
                log.error("Customer object is null, cannot add related customers");
                exceptions.add("%s-Customer object is null, cannot add related customers;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
                return;
            }

            log.debug("Adding related customers list: {} to customer ID: {}", relatedCustomerRequests, customer.getId());

            // related customers cannot be added to a customer that is not private or private with business activity
            if (!customerRepository.existsByIdAndCustomerTypeInAndStatusIn(
                    customer.getId(),
                    List.of(PRIVATE_CUSTOMER),
//                    List.of(PRIVATE_CUSTOMER, PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY),
                    List.of(CustomerStatus.ACTIVE))) {
                log.error("relatedCustomers-Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: [%s];".formatted(customer.getId()));
                exceptions.add("relatedCustomers-Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: [%s];".formatted(customer.getId()));
                return;
            }

            List<RelatedCustomer> tempRelatedCustomersList = new ArrayList<>();

            for (int i = 0; i < relatedCustomerRequests.size(); i++) {
                CreateRelatedCustomerRequest request = relatedCustomerRequests.get(i);

                // customer cannot be his own related customer
                if (customer.getId().equals(request.getRelatedCustomerId())) {
                    log.error("relatedCustomers[%s].relatedCustomerId-Customer ID: %s cannot be his/her own related customer;".formatted(i, customer.getId()));
                    exceptions.add("relatedCustomers[%s].relatedCustomerId-Customer ID: %s cannot be his/her own related customer;".formatted(i, customer.getId()));
                }

                validateRequest(request, customer, tempRelatedCustomersList, exceptions, i, null);
                add(tempRelatedCustomersList, request, customer, exceptions, i);
            }

            if (exceptions.isEmpty()) {
                relatedCustomerRepository.saveAll(tempRelatedCustomersList);
            }
        }
    }

    /**
     * Combines three operations - editing {@link RelatedCustomer}s belonging to the provided {@link Customer} if ID is not null in the request,
     * adding new {@link RelatedCustomer}s if the ID is null or deleting removed {@link RelatedCustomer}s.
     *
     * @param relatedCustomerRequests {@link List<EditRelatedCustomerRequest> List&lt;EditRelatedCustomerRequest&gt;}
     * @param customer customer for which the related customers should be edited
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void editRelatedCustomers(List<EditRelatedCustomerRequest> relatedCustomerRequests, Customer customer, List<String> exceptions) {
        log.debug("Editing related customers list to customer ID: {}", customer.getId());
        process(relatedCustomerRequests, customer, exceptions);
    }

    /**
     * Processes each manager, deciding validation and saving strategy
     * depending on the operation - adding, editing or deleting removed items.
     * If an active related customer with the provided ID is already assigned to the customer,
     * new related customer with the same ID will not be added and error will be produced.
     *
     * @param relatedCustomerRequests {@link List<EditRelatedCustomerRequest> List&lt;EditRelatedCustomerRequest&gt;}
     * @param customer customer for which the related customers should be processed
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void process(List<EditRelatedCustomerRequest> relatedCustomerRequests, Customer customer, List<String> exceptions) {
        if (customer.getCustomerType().equals(LEGAL_ENTITY)) {
            return;
        }

        if (relatedCustomerRequests == null) {
            relatedCustomerRequests = Collections.emptyList();
        }

        // related customers cannot be added to a customer that is not private or private with business activity
        if (!customerRepository.existsByIdAndCustomerTypeInAndStatusIn(
                customer.getId(),
                List.of(PRIVATE_CUSTOMER),
//                List.of(PRIVATE_CUSTOMER, PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY),
                List.of(CustomerStatus.ACTIVE))) {
            log.error("Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: {}", customer.getId());
            exceptions.add("relatedCustomers-Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: [%s];".formatted(customer.getId()));
            return;
        }

        // get all active records for the customer, that are customer's related customers or customers that the customer is related to
        List<Long> persistedCustomerRelations = relatedCustomerRepository.findCustomerRelationsRecordIds(customer.getId(), Status.ACTIVE);

        // removed related customers should be deleted first, so in case user deletes (id (1) a -> b and adds the same customer (id (null) a -> b)
        // in the same request, operation will complete successfully
        deleteRemovedRelatedCustomers(relatedCustomerRequests, persistedCustomerRelations, exceptions);

        List<RelatedCustomer> tempRelatedCustomersList = new ArrayList<>();

        for (int i = 0; i < relatedCustomerRequests.size(); i++) {
            EditRelatedCustomerRequest request = relatedCustomerRequests.get(i);
            validateRequest(request, customer, tempRelatedCustomersList, exceptions, i, request.getId());
            if (request.getId() == null) {
                // customer cannot be his own related customer
                if (customer.getId().equals(request.getRelatedCustomerId())) {
                    log.error("relatedCustomers[%s].relatedCustomerId-Customer ID: %s cannot be his/her own related customer;".formatted(i, customer.getId()));
                    exceptions.add("relatedCustomers[%s].relatedCustomerId-Customer ID: %s cannot be his/her own related customer;".formatted(i, customer.getId()));
                }

                add(tempRelatedCustomersList, new CreateRelatedCustomerRequest(request), customer, exceptions, i);
            } else {
                edit(tempRelatedCustomersList, request, exceptions, i);
            }
        }

        if (exceptions.isEmpty()) {
            relatedCustomerRepository.saveAll(tempRelatedCustomersList);
        }
    }

    /**
     * Validations for adding a related customer include checking if the provided nomenclatures exist and are active.
     *
     * @param tempRelatedCustomersList {@link List<RelatedCustomer> List&lt;RelatedCustomer&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param request {@link CreateRelatedCustomerRequest}
     * @param customer customer to which the related customers should be added
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void add(List<RelatedCustomer> tempRelatedCustomersList,
                     CreateRelatedCustomerRequest request,
                     Customer customer,
                     List<String> exceptions,
                     int index) {
        Optional<CiConnectionType> ciConnectionTypeOptional = ciConnectionTypeRepository.findByIdAndStatusIn(request.getCiConnectionTypeId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (ciConnectionTypeOptional.isEmpty()) {
            log.error("relatedCustomers[%s].ciConnectionTypeId-ACTIVE Ci connection not found, ID: %s;".formatted(index, request.getCiConnectionTypeId()));
            exceptions.add("relatedCustomers[%s].ciConnectionTypeId-ACTIVE Ci connection not found, ID: %s;".formatted(index, request.getCiConnectionTypeId()));
            return;
        }

        saveNewRelatedCustomer(
                tempRelatedCustomersList,
                request,
                customer.getId(),
                ciConnectionTypeOptional.get()
        );
    }

    /**
     * Validations for editing a related customer include checking if a persisted {@link RelatedCustomer} exists with the provided ID,
     * if the persisted {@link RelatedCustomer} belongs to the provided {@link Customer},
     * if the nomenclatures exist and are active/inactive and if nomenclatures are inactive - whether they are different
     * from the persisted {@link RelatedCustomer}s nomenclatures.
     *
     * @param tempRelatedCustomersList {@link List<RelatedCustomer> List&lt;RelatedCustomer&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param request {@link EditRelatedCustomerRequest}
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void edit(List<RelatedCustomer> tempRelatedCustomersList,
                      EditRelatedCustomerRequest request,
                      List<String> exceptions,
                      int index) {
        Optional<RelatedCustomer> relatedCustomerRecordOptional = relatedCustomerRepository.findById(request.getId());
        if (relatedCustomerRecordOptional.isEmpty()) {
            log.error("relatedCustomers[%s].id-Related Customer record not found by ID: %s;".formatted(index, request.getId()));
            exceptions.add("relatedCustomers[%s].id-Related Customer record not found by ID: %s;".formatted(index, request.getId()));
            return;
        }
        RelatedCustomer relatedCustomer = relatedCustomerRecordOptional.get();

        Optional<CiConnectionType> ciConnectionTypeOptional = ciConnectionTypeRepository
                .findByIdAndStatusIn(request.getCiConnectionTypeId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (ciConnectionTypeOptional.isEmpty()) {
            log.error("relatedCustomers[%s].ciConnectionTypeId-ACTIVE/INACTIVE Ci connection not found by ID: %s;".formatted(index, request.getCiConnectionTypeId()));
            exceptions.add("relatedCustomers[%s].ciConnectionTypeId-ACTIVE/INACTIVE Ci connection not found by ID: %s;".formatted(index, request.getCiConnectionTypeId()));
            return;
        }

        if (ciConnectionTypeOptional.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
            if (!relatedCustomer.getCiConnectionType().getId().equals(request.getCiConnectionTypeId())) {
                log.error("relatedCustomers[%s].ciConnectionTypeId-Cannot save object with different INACTIVE nomenclature;".formatted(index));
                exceptions.add("relatedCustomers[%s].ciConnectionTypeId-Cannot save object with different INACTIVE nomenclature;".formatted(index));
            }
        }

        saveEditedRelatedCustomer(
                tempRelatedCustomersList,
                request,
                ciConnectionTypeOptional.get(),
                exceptions,
                index
        );
    }

    /**
     * Checks validations for {@link BaseRelatedCustomerRequest}.
     *
     * @param request {@link BaseRelatedCustomerRequest}
     * @param customer {@link Customer}
     * @param tempRelatedCustomersList {@link List<RelatedCustomer> List&lt;RelatedCustomer&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void validateRequest(BaseRelatedCustomerRequest request,
                                 Customer customer,
                                 List<RelatedCustomer> tempRelatedCustomersList,
                                 List<String> exceptions,
                                 int index,
                                 Long requestId) {
        if (request.getStatus().equals(Status.DELETED)) {
            log.error("relatedCustomers[%s].status-Cannot set DELETED status when processing related customer;".formatted(index));
            exceptions.add("relatedCustomers[%s].status-Cannot set DELETED status when processing related customer;".formatted(index));
        }

        for (RelatedCustomer trc : tempRelatedCustomersList) {
            boolean duplicatedValue = (trc.getCustomerId().equals(customer.getId()) && trc.getRelatedCustomerId().equals(request.getRelatedCustomerId()))
                    || (trc.getRelatedCustomerId().equals(customer.getId()) && trc.getCustomerId().equals(request.getRelatedCustomerId()));
            if (duplicatedValue) {
                log.error("relatedCustomers[%s].relatedCustomerId-Duplicate customer relations: ID %s and ID %s;".formatted(index, customer.getId(), request.getRelatedCustomerId()));
                exceptions.add("relatedCustomers[%s].relatedCustomerId-Duplicate customer relations: ID %s and ID %s;".formatted(index, customer.getId(), request.getRelatedCustomerId()));
            }
        }

        // if the customer is already related to the provided customer, do not add it again
        if (relatedCustomerRepository.existsRelation(customer.getId(), request.getRelatedCustomerId(), List.of(Status.ACTIVE)) && requestId == null) {
            log.error("relatedCustomers[%s].relativeCustomerId-Active relation already exists between customers with IDs: %s and %s;"
                              .formatted(index, customer.getId(), request.getRelatedCustomerId()));
            exceptions.add("relatedCustomers[%s].relativeCustomerId-Active relation already exists between customers with IDs: %s and %s;"
                                   .formatted(index, customer.getId(), request.getRelatedCustomerId()));
        }

        // customer that is not private or private with business activity can not be added as a related customer
        if (!customerRepository.existsByIdAndCustomerTypeInAndStatusIn(
                request.getRelatedCustomerId(),
                List.of(PRIVATE_CUSTOMER),
//                List.of(PRIVATE_CUSTOMER, PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY),
                List.of(CustomerStatus.ACTIVE))) {
            log.error("relatedCustomers[%s].relatedCustomerId-Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: %s;".formatted(index, request.getRelatedCustomerId()));
            exceptions.add("relatedCustomers[%s].relatedCustomerId-Active PRIVATE/PRIVATE_WITH_BUSINESS customer not found by ID: %s;".formatted(index, request.getRelatedCustomerId()));
        }
    }

    /**
     * Populates {@link RelatedCustomer} object to be persisted afterwards.
     *
     * @param tempList temporary list in which all processed requests are accumulated and then saved together
     * @param request {@link CreateRelatedCustomerRequest}
     * @param customerId ID of the {@link Customer}
     * @param connectionType {@link CiConnectionType}
     */
    private void saveNewRelatedCustomer(List<RelatedCustomer> tempList,
                                        CreateRelatedCustomerRequest request,
                                        Long customerId,
                                        CiConnectionType connectionType) {
        RelatedCustomer relatedCustomer = new RelatedCustomer();
        relatedCustomer.setCustomerId(customerId);
        relatedCustomer.setRelatedCustomerId(request.getRelatedCustomerId());
        relatedCustomer.setCiConnectionType(connectionType);
        relatedCustomer.setStatus(request.getStatus());
        tempList.add(relatedCustomer);
    }

    /**
     * Populates {@link RelatedCustomer} object to be persisted afterwards.
     *
     * @param tempRelatedCustomersList temporary list in which all processed requests are accumulated and then saved together
     * @param request {@link EditRelatedCustomerRequest}
     * @param connectionType {@link CiConnectionType}
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void saveEditedRelatedCustomer(List<RelatedCustomer> tempRelatedCustomersList,
                                           EditRelatedCustomerRequest request,
                                           CiConnectionType connectionType,
                                           List<String> exceptions,
                                           int index) {
        Optional<RelatedCustomer> relatedCustomerOptional = relatedCustomerRepository.findById(request.getId());
        if (relatedCustomerOptional.isEmpty()) {
            log.error("relatedCustomers[%s].id-Related Customer record not found, ID: [%s];".formatted(index, request.getId()));
            exceptions.add("relatedCustomers[%s].id-Related Customer record not found, ID: [%s];".formatted(index, request.getId()));
            return;
        }

        RelatedCustomer dbRelatedCustomer = relatedCustomerOptional.get();
        dbRelatedCustomer.setCiConnectionType(connectionType);
        dbRelatedCustomer.setStatus(request.getStatus());
        tempRelatedCustomersList.add(dbRelatedCustomer);
    }

    /**
     * Loops over persisted {@link RelatedCustomer}s and delete them if not found in the provided request.
     *
     * @param requests {@link List<EditRelatedCustomerRequest> List&lt;EditRelatedCustomerRequest&gt;}
     * @param persistedRelatedCustomers {@link List<Long> List&lt;Long&gt;} IDs of the persisted related customers
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteRemovedRelatedCustomers(List<EditRelatedCustomerRequest> requests,
                                               List<Long> persistedRelatedCustomers,
                                               List<String> exceptions) {
        if (!persistedRelatedCustomers.isEmpty()) {
            List<Long> requestRelatedCustomerIds = requests
                    .stream()
                    .map(EditRelatedCustomerRequest::getId)
                    .toList();

            for (Long relatedCustomerId : persistedRelatedCustomers) {
                if (!requestRelatedCustomerIds.contains(relatedCustomerId)) {
                    deleteRelatedCustomer(relatedCustomerId, exceptions);
                }
            }
        }
    }

    /**
     * Deletes a related customer if not already deleted
     *
     * @param relatedCustomerId ID of the {@link RelatedCustomer} that should be deleted
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteRelatedCustomer(Long relatedCustomerId, List<String> exceptions) {
        log.debug("Deleting related customer with ID: {}", relatedCustomerId);
        Optional<RelatedCustomer> relatedCustomerOptional = relatedCustomerRepository.findById(relatedCustomerId);
        if (relatedCustomerOptional.isEmpty()) {
            log.error("Related Customer record not found, ID: " + relatedCustomerId);
            exceptions.add("relatedCustomers-Related Customer record not found, ID: " + relatedCustomerId + ";");
            return;
        }

        RelatedCustomer relatedCustomer = relatedCustomerOptional.get();
        if (!relatedCustomer.getStatus().equals(Status.DELETED)) {
            relatedCustomer.setStatus(Status.DELETED);
            relatedCustomerRepository.save(relatedCustomer);
        }
    }
}
