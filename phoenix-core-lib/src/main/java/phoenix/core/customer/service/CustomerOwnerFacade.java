package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.CustomerOwner;
import phoenix.core.customer.model.entity.nomenclature.customer.BelongingCapitalOwner;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.CustomerOwnerEditRequest;
import phoenix.core.customer.model.request.CustomerOwnerRequest;
import phoenix.core.customer.model.response.customer.CustomerOwnerDetailResponse;
import phoenix.core.customer.model.response.customer.CustomerOwnerResponse;
import phoenix.core.customer.repository.customer.CustomerDetailsRepository;
import phoenix.core.customer.repository.customer.CustomerOwnerRepository;
import phoenix.core.customer.repository.customer.CustomerRepository;
import phoenix.core.customer.repository.nomenclature.customer.BelongingCapitalOwnerRepository;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("coreCustomerOwnerService")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CustomerOwnerFacade {

    private final CustomerOwnerRepository ownerRepository;
    private final BelongingCapitalOwnerRepository capitalOwnerRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository detailsRepository;

    public List<CustomerOwnerResponse> getOwnersForCustomer(Long customerId, List<String> exceptionMessages) {
        List<CustomerOwner> owners = ownerRepository.findByCustomerIdAndStatuses(customerId, List.of(Status.ACTIVE));
        Optional<CustomerDetails> ownersOptional = detailsRepository.findFirstByCustomerId(
                customerId,
                Sort.by(Sort.Direction.DESC, "createDate")
        );
        if (ownersOptional.isEmpty()) {
            exceptionMessages.add("Customer details not found");
            return null;
        }
        CustomerDetails customerDetails = ownersOptional.get();
        return owners.stream().map(o -> new CustomerOwnerResponse(o, customerDetails)).collect(Collectors.toList());
    }

    public CustomerOwnerDetailResponse get(Long id) {
        CustomerOwner owner = ownerRepository.findByIdAndStatuses(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new ClientException("Customer owner not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Customer ownerCustomer = owner.getOwnerCustomer();
        CustomerDetails customerDetails = detailsRepository.findFirstByCustomerId(
                ownerCustomer.getId(),
                Sort.by(Sort.Direction.DESC, "createDate")
        ).orElseThrow(() -> new ClientException("Customer owner not found. Contact admin", ErrorCode.APPLICATION_ERROR));
        return new CustomerOwnerDetailResponse(owner, customerDetails);


    }


    @Transactional
    public void saveCustomerOwner(List<CustomerOwnerRequest> requests, Customer currentCustomer, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }
        if (currentCustomer == null) {
            exceptionMessages.add("Can not create customer owner when given customer does not exist");
            return;
        }
        log.debug("Request to save customer owner for customer: {}", currentCustomer.getId());


        requests.forEach(request -> {
            CustomerOwner customerOwner = createCustomerOwner(currentCustomer, exceptionMessages, request);
            Optional<Customer> ownerCustomer = customerRepository
                    .findFirstByIdentifierAndStatusIn(request.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
            if (ownerCustomer.isEmpty()) {
                exceptionMessages.add("Customer with identifier " + request.getPersonalNumber() + " not found");
            }
            ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
            if (exceptionMessages.isEmpty()) {
                ownerRepository.save(customerOwner);
            }

        });

    }


    @Transactional
    public void editCustomerOwners(List<CustomerOwnerEditRequest> requests, Customer currentCustomer, List<String> exceptionMessages) {

        if (requests==null) {
            requests=new ArrayList<>();
        }
        if (currentCustomer != null) {
            log.debug("Request to edit customer owner for customer: {}", currentCustomer.getId());
        } else {
            exceptionMessages.add("Can not edit customer owner when given customer does not exist");
            return;
        }

        List<CustomerOwner> customerOwners = ownerRepository.findByCustomerIdAndStatuses(currentCustomer.getId(), List.of(Status.ACTIVE));
        addCustomerOwners(requests, currentCustomer, customerOwners, exceptionMessages);
        Map<Long, CustomerOwnerEditRequest> ownerEditMap = requests.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(CustomerOwnerEditRequest::getId, co -> co));
        editExistingCustomerOwner(ownerEditMap, customerOwners, exceptionMessages);
        if (exceptionMessages.isEmpty()) {
            ownerRepository.saveAll(customerOwners);
        }
    }


    //edits existing customer owner
    private void editExistingCustomerOwner(Map<Long, CustomerOwnerEditRequest> requests, List<CustomerOwner> owners, List<String> exceptionMessages) {
        owners.forEach(customerOwner -> {
            CustomerOwnerEditRequest editRequest = requests.get(customerOwner.getId());
            if (editRequest == null && customerOwner.getId() != null) {
                customerOwner.setStatus(Status.DELETED);
            } else if (editRequest != null) {
                belongingCapitalEdit(exceptionMessages, customerOwner, editRequest.getAdditionalInformation(), editRequest.getBelongingOwnerCapitalId());
                Optional<Customer> ownerCustomer = customerRepository
                        .findFirstByIdentifierAndStatusIn(editRequest.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
                if (ownerCustomer.isEmpty()) {
                    exceptionMessages.add("Customer with identifier " + editRequest.getPersonalNumber() + " not found");
                }
                ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
            }

        });

    }

    //sets belonging capital and additional info
    private void belongingCapitalEdit(List<String> exceptionMessages, CustomerOwner owner, String additionalInformation, Long belongingOwnerCapitalId) {
        owner.setAdditionalInfo(additionalInformation);
        Optional<BelongingCapitalOwner> belongingCapitalOwner = capitalOwnerRepository.findByIdAndStatuses(belongingOwnerCapitalId,List.of(NomenclatureItemStatus.ACTIVE));
        if (belongingCapitalOwner.isEmpty()) {
            exceptionMessages.add("Can not attach customer owner to inactive belonging capital owner");
        }

        belongingCapitalOwner.ifPresent(owner::setBelongingCapitalOwner);
    }

    //create new  customer owner if given id is null. used from edit request
    private void addCustomerOwners(List<CustomerOwnerEditRequest> requests, Customer currentCustomer, List<CustomerOwner> owners, List<String> exceptionMessages) {

        requests.stream().filter(x -> x.getId() == null).forEach(request -> {

            CustomerOwner customerOwner = createCustomerOwner(currentCustomer, exceptionMessages, request);
            Optional<Customer> ownerCustomer = customerRepository
                    .findFirstByIdentifierAndStatusIn(request.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
            if (ownerCustomer.isEmpty()) {
                exceptionMessages.add("Customer with identifier " + request.getPersonalNumber() + " not found");
            }
            ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
            owners.add(customerOwner);


        });

    }

    //creates customer owner from given parameters
    private CustomerOwner createCustomerOwner(Customer currentCustomer, List<String> exceptionMessages, CustomerOwnerRequest request) {
        CustomerOwner customerOwner = new CustomerOwner();
        customerOwner.setCustomer(currentCustomer);

        customerOwner.setStatus(Status.ACTIVE);
        belongingCapitalEdit(exceptionMessages, customerOwner, request.getAdditionalInformation(), request.getBelongingOwnerCapitalId());

        customerOwner.setSystemUserId("admin");
        customerOwner.setCreateDate(LocalDateTime.now());
        return customerOwner;
    }
}
