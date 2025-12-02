package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerOwner;
import bg.energo.phoenix.model.entity.nomenclature.customer.BelongingCapitalOwner;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.CustomerOwnerEditRequest;
import bg.energo.phoenix.model.request.customer.CustomerOwnerRequest;
import bg.energo.phoenix.model.response.customer.owner.CustomerOwnerDetailResponse;
import bg.energo.phoenix.model.response.customer.owner.CustomerOwnerResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerOwnerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BelongingCapitalOwnerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.CUSTOMER_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.CUSTOMER_VIEW_BASIC_AM;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class CustomerOwnerService {

    private final CustomerOwnerRepository ownerRepository;
    private final BelongingCapitalOwnerRepository capitalOwnerRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository detailsRepository;
    private final PermissionService permissionService;
    private final LegalFormRepository legalFormRepository;


    public List<CustomerOwnerResponse> getOwnersForCustomer(Long customerId, List<String> exceptionMessages) {
        List<CustomerOwner> owners = ownerRepository.findByCustomerIdAndStatuses(customerId, List.of(Status.ACTIVE));


        List<CustomerOwnerResponse> customerOwnerResponses = new ArrayList<>();
        for (int i = 0; i < owners.size(); i++) {
            CustomerOwner o = owners.get(i);
            Optional<CustomerDetails> ownersOptional = detailsRepository.findFirstByCustomerId(
                    o.getOwnerCustomer().getId(),
                    Sort.by(Sort.Direction.DESC, "createDate")
            );

            if (ownersOptional.isEmpty()) {
                exceptionMessages.add(String.format("owner[%s].personalNumber-Customer details not found for %s;", i, o.getOwnerCustomer().getId()));
                continue;
            }

            CustomerDetails customerDetails = ownersOptional.get();
            CustomerOwnerResponse customerOwnerResponse = new CustomerOwnerResponse(o, customerDetails);

            if (notHaveGDRP()) {
                customerOwnerResponse.setName("GDPR");
                customerOwnerResponse.setPersonalNumber("GDPR");
            }
            customerOwnerResponses.add(customerOwnerResponse);
        }
        return customerOwnerResponses;
    }

    public CustomerOwnerDetailResponse get(Long id) {
        CustomerOwner owner = ownerRepository.findByIdAndStatuses(id, List.of(Status.ACTIVE))
                .orElseThrow(() -> new ClientException("id-Customer owner not found;", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        Customer ownerCustomer = owner.getOwnerCustomer();
        CustomerDetails customerDetails = detailsRepository.findFirstByCustomerId(
                ownerCustomer.getId(),
                Sort.by(Sort.Direction.DESC, "createDate")
        ).orElseThrow(() -> new DomainEntityNotFoundException("Customer owner not found, ID %s;".formatted(ownerCustomer.getId())));

        LegalForm legalForm = legalFormRepository.findByIdAndStatus(customerDetails.getLegalFormId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id- Can't find Active Legal form"));
        CustomerOwnerDetailResponse response = new CustomerOwnerDetailResponse(owner, customerDetails, legalForm);
        if (notHaveGDRP()) {
            response.setName("GDPR");
            response.setPersonalNumber("GDPR");
        }
        return response;
    }

    public List<CustomerOwnerDetailResponse> getOwnersByCustomerId(Long customerId) {
        log.debug("Fetching owners by customer ID: {}", customerId);
        return ownerRepository.getOwnersByCustomerId(customerId, List.of(Status.ACTIVE));
    }

    @Transactional
    public void saveCustomerOwner(List<CustomerOwnerRequest> requests, Customer currentCustomer, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }

        if (currentCustomer == null) {
            exceptionMessages.add("%s-Can not create customer owner when given customer does not exist;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
            return;
        }

        log.debug("Request to save customer owner for customer: {}", currentCustomer.getId());
        Set<String> validValues = new HashSet<>();
        int y = 0;
        for (CustomerOwnerRequest request : requests) {
            if (!validValues.add(request.getPersonalNumber())) {
                exceptionMessages.add(String.format("owner[%s].personalNumber-You can not save same customer more than 1 time as owner. personalNumber:%s", y, request.getPersonalNumber()));
            }
            y++;
        }
        for (int i = 0; i < requests.size(); i++) {
            CustomerOwnerRequest request = requests.get(i);
            CustomerOwner customerOwner = createCustomerOwner(currentCustomer, exceptionMessages, request, i);
            Optional<Customer> ownerCustomer = customerRepository
                    .findFirstByIdentifierAndStatusIn(request.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
            if (ownerCustomer.isEmpty()) {
                log.error("owner[%s].personalNumber-Customer with identifier [%s] not found;".formatted(i, request.getPersonalNumber()));
                exceptionMessages.add("owner[%s].personalNumber-Customer with identifier [%s] not found;".formatted(i, request.getPersonalNumber()));
            }
            ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
            if (exceptionMessages.isEmpty()) {
                ownerRepository.save(customerOwner);
            }
        }
    }


    @Transactional
    public void editCustomerOwners(List<CustomerOwnerEditRequest> requests, Customer currentCustomer, List<String> exceptionMessages) {
        if (requests == null) {
            requests = new ArrayList<>();
        }
        if (currentCustomer != null) {
            log.debug("Request to edit customer owner for customer: {}", currentCustomer.getId());
        } else {
            exceptionMessages.add("%s-Can not edit customer owner when given customer does not exist;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
            return;
        }
        Set<String> validValues = new HashSet<>();
        int y = 0;
        for (CustomerOwnerRequest request : requests) {
            if (!validValues.add(request.getPersonalNumber())) {
                exceptionMessages
                        .add(String.format("owner[%s].personalNumber-You can not save same customer more than 1 time as owner. personalNumber:%s", y, request.getPersonalNumber()));
            }
            y++;
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
        for (int i = 0; i < owners.size(); i++) {
            CustomerOwner customerOwner = owners.get(i);
            CustomerOwnerEditRequest editRequest = requests.get(customerOwner.getId());
            if (editRequest == null && customerOwner.getId() != null) {
                customerOwner.setStatus(Status.DELETED);
            } else if (editRequest != null) {
                belongingCapitalEdit(exceptionMessages, customerOwner, editRequest.getAdditionalInformation(), editRequest.getBelongingOwnerCapitalId(), i);
                if (!StringUtils.equals(EPBFinalFields.GDPR, editRequest.getPersonalNumber())) {
                    Optional<Customer> ownerCustomer = customerRepository
                            .findFirstByIdentifierAndStatusIn(editRequest.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
                    if (ownerCustomer.isEmpty()) {
                        exceptionMessages.add("owner[%s].personalNumber-Customer with identifier [%s] not found;".formatted(i, editRequest.getPersonalNumber()));
                    }
                    ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
                }
            }
        }
    }

    //sets belonging capital and additional info
    private void belongingCapitalEdit(List<String> exceptionMessages,
                                      CustomerOwner owner,
                                      String additionalInformation,
                                      Long belongingOwnerCapitalId,
                                      int index) {
        owner.setAdditionalInfo(additionalInformation);
        Optional<BelongingCapitalOwner> belongingCapitalOwner = capitalOwnerRepository.findByIdAndStatuses(belongingOwnerCapitalId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
        if (belongingCapitalOwner.isEmpty()) {
            exceptionMessages.add("owner[%s].belongingOwnerCapitalId-Not found belonging capital owner by id %s;".formatted(index, belongingOwnerCapitalId));
        } else if (belongingCapitalOwner.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (owner.getBelongingCapitalOwner() == null || !owner.getBelongingCapitalOwner().getId().equals(belongingOwnerCapitalId))) {
            exceptionMessages.add("owner[%s].belongingOwnerCapitalId-Can not attach customer owner to INACTIVE belonging capital owner;".formatted(index));
        } else {
            owner.setBelongingCapitalOwner(belongingCapitalOwner.get());
        }

    }

    //create new  customer owner if given id is null. used from edit request
    private void addCustomerOwners(List<CustomerOwnerEditRequest> requests, Customer currentCustomer, List<CustomerOwner> owners, List<String> exceptionMessages) {

        for (int i = 0; i < requests.size(); i++) {
            CustomerOwnerEditRequest x = requests.get(i);
            if (x.getId() == null) {
                CustomerOwner customerOwner = createCustomerOwner(currentCustomer, exceptionMessages, x, i);
                Optional<Customer> ownerCustomer = customerRepository
                        .findFirstByIdentifierAndStatusIn(x.getPersonalNumber(), List.of(CustomerStatus.ACTIVE));
                if (ownerCustomer.isEmpty()) {
                    exceptionMessages.add("owner[%s].personalNumber-Customer with identifier [%s] not found;".formatted(i, x.getPersonalNumber()));
                }
                ownerCustomer.ifPresent(customerOwner::setOwnerCustomer);
                owners.add(customerOwner);
            }
        }
    }

    //creates customer owner from given parameters
    private CustomerOwner createCustomerOwner(Customer currentCustomer, List<String> exceptionMessages, CustomerOwnerRequest request, int index) {
        CustomerOwner customerOwner = new CustomerOwner();
        customerOwner.setCustomer(currentCustomer);

        customerOwner.setStatus(Status.ACTIVE);
        belongingCapitalEdit(exceptionMessages, customerOwner, request.getAdditionalInformation(), request.getBelongingOwnerCapitalId(), index);

        return customerOwner;
    }

    private boolean notHaveGDRP() {
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.CUSTOMER);

        return context.stream().noneMatch(x -> List.of(CUSTOMER_VIEW_BASIC.getId(), CUSTOMER_VIEW_BASIC_AM.getId()).contains(x));
    }
}
