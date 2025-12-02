package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.RelatedCustomer;
import phoenix.core.customer.model.entity.nomenclature.customer.CiConnectionType;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.relatedCustomer.BaseRelatedCustomerRequest;
import phoenix.core.customer.model.request.relatedCustomer.CreateRelatedCustomerRequest;
import phoenix.core.customer.model.request.relatedCustomer.EditRelatedCustomerRequest;
import phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;
import phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerResponse;
import phoenix.core.customer.repository.customer.CustomerDetailsRepository;
import phoenix.core.customer.repository.customer.CustomerRepository;
import phoenix.core.customer.repository.customer.RelatedCustomerRepository;
import phoenix.core.customer.repository.nomenclature.customer.CiConnectionTypeRepository;
import phoenix.core.exception.DomainEntityNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service("coreRelatedCustomerService")
@RequiredArgsConstructor
@Validated
public class RelatedCustomerFacade {
    private final RelatedCustomerRepository relatedCustomerRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CiConnectionTypeRepository ciConnectionTypeRepository;

    // for viewing related customers list on create customer page
    public List<RelatedCustomerBasicInfo> previewByCustomer(Customer customer, List<String> exceptions) {
        if (customer == null) {
            log.error("Customer object is null, cannot retrieve related customers");
            exceptions.add("Customer object is null, cannot retrieve related customers");
            return null;
        }

        if (!customerRepository.existsById(customer.getId())) {
            log.error("Customer not found, ID: " + customer.getId());
            exceptions.add("Customer not found, ID: " + customer.getId());
            return null;
        }

        log.debug("Fetching related customers for customer ID: {}", customer.getId());
        return relatedCustomerRepository.getRelatedCustomersByCustomerId(customer.getId(), Status.ACTIVE);
    }

    // for viewing a related customer in its own modal
    public RelatedCustomerResponse getDetailedRelatedCustomerById(Long id) {
        log.debug("Fetching related customer by ID: {}", id);

        RelatedCustomer relatedCustomerRecord = relatedCustomerRepository
                .findRelatedCustomerByIdAndStatuses(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active related Customer record not found, ID: " + id));

        Customer relatedCustomer = customerRepository
                .findById(relatedCustomerRecord.getRelatedCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Related customer record not found in customer table, ID: "
                        + relatedCustomerRecord.getRelatedCustomerId()
                ));

        CustomerDetails customerDetails = customerDetailsRepository
                .findFirstByCustomerId(relatedCustomer.getId(), Sort.by(Sort.Direction.DESC, "versionId"))
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Detail not found for customer ID " + relatedCustomer.getId()));

        return new RelatedCustomerResponse(relatedCustomerRecord, relatedCustomer, customerDetails);
    }

    // for adding related customers when creating
    @Transactional
    public void addRelatedCustomers(List<CreateRelatedCustomerRequest> relatedCustomerRequests,
                                    Customer customer,
                                    List<String> exceptions) {
        if (relatedCustomerRequests != null && !relatedCustomerRequests.isEmpty()) {
            if (customer == null) {
                log.error("Customer object is null, cannot add related customers");
                exceptions.add("Customer object is null, cannot add related customers");
                return;
            }

            log.debug("Adding related customers list: {} to customer ID: {}", relatedCustomerRequests.toString(), customer.getId());

            if (!customerRepository.existsById(customer.getId())) {
                log.error("Customer not found, ID: " + customer.getId());
                exceptions.add("Customer not found, ID: " + customer.getId());
                return;
            }

            List<RelatedCustomer> tempRelatedCustomersList = new ArrayList<>();

            for (CreateRelatedCustomerRequest request : relatedCustomerRequests) {
                validateRequest(request, customer, tempRelatedCustomersList, exceptions);
                add(tempRelatedCustomersList, request, customer, exceptions);
            }

            if (exceptions.isEmpty()) {
                relatedCustomerRepository.saveAll(tempRelatedCustomersList);
            }
        }
    }

    // for editing related customers
    @Transactional
    public void editRelatedCustomers(List<EditRelatedCustomerRequest> relatedCustomerRequests, Customer customer, List<String> exceptions) {
        if (relatedCustomerRequests!= null && !relatedCustomerRequests.isEmpty()) {
            log.debug("Editing related customers list: {} to customer ID: {}", relatedCustomerRequests.toString(), customer.getId());
            process(relatedCustomerRequests, customer, exceptions);
        }
    }

    private void process(List<EditRelatedCustomerRequest> relatedCustomerRequests, Customer customer, List<String> exceptions) {
        if (!customerRepository.existsById(customer.getId())) {
            log.error("Customer not found, ID: " + customer.getId());
            exceptions.add("Customer not found, ID: " + customer.getId());
            return;
        }

        List<Long> persistedRelatedCustomers = relatedCustomerRepository
                .getRelatedCustomerRecordIdsByCustomerId(customer.getId(), Status.ACTIVE);

        List<Long> relatedCustomerIds = relatedCustomerRepository
                .findRelatedCustomerIdsByCustomerIdAndStatus(customer.getId(), Status.ACTIVE);

        List<RelatedCustomer> tempRelatedCustomersList = new ArrayList<>();

        for (EditRelatedCustomerRequest request : relatedCustomerRequests) {
            validateRequest(request, customer, tempRelatedCustomersList, exceptions);

            if (request.getId() == null) {
                if (relatedCustomerIds.contains(request.getRelatedCustomerId())) {
                    log.error("Related customer ID: {} has already been added to the customer ID: {}", request.getRelatedCustomerId(), customer.getId());
                    exceptions.add("Related customer ID: " + request.getRelatedCustomerId() + " has already been added to the customer ID: " + customer.getId());
                }
                add(tempRelatedCustomersList, new CreateRelatedCustomerRequest(request), customer, exceptions);
            } else {
                edit(tempRelatedCustomersList, request, customer, exceptions);
            }
        }

        if (exceptions.isEmpty()) {
            relatedCustomerRepository.saveAll(tempRelatedCustomersList);
            deleteRemovedRelatedCustomers(relatedCustomerRequests, persistedRelatedCustomers, exceptions);
        }
    }

    private void add(List<RelatedCustomer> tempRelatedCustomersList,
                     CreateRelatedCustomerRequest request,
                     Customer customer,
                     List<String> exceptions) {
        Optional<CiConnectionType> ciConnectionTypeOptional = ciConnectionTypeRepository.findById(request.getCiConnectionTypeId());
        if (ciConnectionTypeOptional.isEmpty()) {
            log.error("Ci connection not found, ID: " + request.getCiConnectionTypeId());
            exceptions.add("Ci connection not found, ID: " + request.getCiConnectionTypeId());
            return;
        }

        if (!ciConnectionTypeOptional.get().getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
            log.error("Nomenclature item status should be ACTIVE while adding new objects");
            exceptions.add("Nomenclature item status should be ACTIVE while adding new objects");
        }

        saveNewRelatedCustomer(
                tempRelatedCustomersList,
                request,
                customer.getId(),
                ciConnectionTypeOptional.get()
        );
    }

    private void edit(List<RelatedCustomer> tempRelatedCustomersList,
                      EditRelatedCustomerRequest request,
                      Customer customer,
                      List<String> exceptions) {
        Optional<RelatedCustomer> relatedCustomerRecordOptional = relatedCustomerRepository.findById(request.getId());
        if (relatedCustomerRecordOptional.isEmpty()) {
            log.error("Request ID " + request.getId() + ": Related Customer record not found, ID: " + request.getId());
            exceptions.add("Request ID " + request.getId() + ": Related Customer record not found, ID: " + request.getId());
            return;
        }

        RelatedCustomer relatedCustomer = relatedCustomerRecordOptional.get();
        if (!relatedCustomer.getCustomerId().equals(customer.getId())) {
            log.error("Request ID " + request.getId() + ": Cannot change current customer ID: " + relatedCustomer.getCustomerId()
                    + "with the requested different customer ID: " + customer.getId());
            exceptions.add("Request ID " + request.getId() + ": Cannot change current customer ID: " + relatedCustomer.getCustomerId()
                    + "with the requested different customer ID: " + customer.getId());
        }

        Optional<CiConnectionType> ciConnectionTypeOptional = ciConnectionTypeRepository.findById(request.getCiConnectionTypeId());
        if (ciConnectionTypeOptional.isEmpty()) {
            log.error("Request ID " + request.getId() + ": Ci connection not found, ID: " + request.getCiConnectionTypeId());
            exceptions.add("Request ID " + request.getId() + ": Ci connection not found, ID: " + request.getCiConnectionTypeId());
            return;
        }

        if (ciConnectionTypeOptional.get().getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Request ID " + request.getId() + ": Nomenclature item status should not be DELETED while editing objects");
            exceptions.add("Request ID " + request.getId() + ": Nomenclature item status should not be DELETED while editing objects");
        }

        if (ciConnectionTypeOptional.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
            if (!relatedCustomer.getCiConnectionType().getId().equals(request.getCiConnectionTypeId())) {
                log.error("Request ID " + request.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Request ID " + request.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        saveEditedRelatedCustomer(
                tempRelatedCustomersList,
                request,
                customer.getId(),
                ciConnectionTypeOptional.get(),
                exceptions
        );
    }

    private void validateRequest(BaseRelatedCustomerRequest request,
                                 Customer customer,
                                 List<RelatedCustomer> tempRelatedCustomersList,
                                 List<String> exceptions) {
        if (request.getStatus().equals(Status.DELETED)) {
            log.error("Cannot set DELETED status when processing related customer");
            exceptions.add("Cannot set DELETED status when processing related customer");
        }

        if (customer.getId().equals(request.getRelatedCustomerId())) {
            log.error("Customer ID: {} cannot be his/her own related customer", customer.getId());
            exceptions.add("Customer ID: " + customer.getId() + " cannot be his/her own customer");
        }

        if (tempRelatedCustomersList.stream()
                .map(RelatedCustomer::getRelatedCustomerId).toList()
                .contains(request.getRelatedCustomerId())) {
            log.error("Duplicate related customer, ID: {} while adding objects to customer, ID {}",
                    request.getRelatedCustomerId(), customer.getId());
            exceptions.add("Duplicate related customer, ID: " + request.getRelatedCustomerId()
                    + " while adding objects to customer, ID " + customer.getId());
        }

        Optional<Customer> relatedCustomerOptional = customerRepository.findById(request.getRelatedCustomerId());
        if (relatedCustomerOptional.isEmpty()) {
            log.error("Related customer not found in db, ID: " + request.getRelatedCustomerId());
            exceptions.add("Related customer not found in db, ID: " + request.getRelatedCustomerId());
            return;
        }

        Customer relatedCustomer = relatedCustomerOptional.get();
        if (!relatedCustomer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            log.error("Only private customers can be added as related customers");
            exceptions.add("Only private customers can be added as related customers");
        }
    }

    private void saveNewRelatedCustomer(List<RelatedCustomer> tempList,
                                        CreateRelatedCustomerRequest request,
                                        Long customerId,
                                        CiConnectionType connectionType) {
        RelatedCustomer relatedCustomer = new RelatedCustomer();
        relatedCustomer.setCustomerId(customerId);
        relatedCustomer.setRelatedCustomerId(request.getRelatedCustomerId());
        relatedCustomer.setCiConnectionType(connectionType);
        relatedCustomer.setStatus(request.getStatus());
        relatedCustomer.setCreateDate(LocalDateTime.now());
        // TODO: 10.01.23 set actual system user id later
        relatedCustomer.setSystemUserId("test");
        tempList.add(relatedCustomer);
    }

    private void saveEditedRelatedCustomer(List<RelatedCustomer> tempRelatedCustomersList,
                                           EditRelatedCustomerRequest request,
                                           Long customerId,
                                           CiConnectionType connectionType,
                                           List<String> exceptions) {
        Optional<RelatedCustomer> relatedCustomerOptional = relatedCustomerRepository.findById(request.getId());
        if (relatedCustomerOptional.isEmpty()) {
            log.error("Related Customer record not found, ID: " + request.getId());
            exceptions.add("Related Customer record not found, ID: " + request.getId());
            return;
        }

        RelatedCustomer dbRelatedCustomer = relatedCustomerOptional.get();
        RelatedCustomer relatedCustomer = new RelatedCustomer();
        relatedCustomer.setId(dbRelatedCustomer.getId());
        relatedCustomer.setCustomerId(customerId);
        relatedCustomer.setRelatedCustomerId(request.getRelatedCustomerId());
        relatedCustomer.setCiConnectionType(connectionType);
        relatedCustomer.setStatus(request.getStatus());
        relatedCustomer.setSystemUserId(dbRelatedCustomer.getSystemUserId());
        relatedCustomer.setCreateDate(dbRelatedCustomer.getCreateDate());
        // TODO: 10.01.23 set systemUserId later
        relatedCustomer.setModifyDate(LocalDateTime.now());
        relatedCustomer.setModifySystemUserId("test");
        tempRelatedCustomersList.add(relatedCustomer);
    }

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

    private void deleteRelatedCustomer(Long relatedCustomerId, List<String> exceptions) {
        log.debug("Deleting related customer with ID: {}", relatedCustomerId);
        Optional<RelatedCustomer> relatedCustomerOptional = relatedCustomerRepository.findById(relatedCustomerId);
        if (relatedCustomerOptional.isEmpty()) {
            log.error("Related Customer record not found, ID: " + relatedCustomerId);
            exceptions.add("Related Customer record not found, ID: " + relatedCustomerId);
            return;
        }

        RelatedCustomer relatedCustomer = relatedCustomerOptional.get();
        if (!relatedCustomer.getStatus().equals(Status.DELETED)) {
            relatedCustomer.setStatus(Status.DELETED);
            // TODO: 10.01.23 set actual modify system user id
            relatedCustomer.setModifySystemUserId("test");
            relatedCustomer.setModifyDate(LocalDateTime.now());
            relatedCustomerRepository.save(relatedCustomer);
        }
    }
}
