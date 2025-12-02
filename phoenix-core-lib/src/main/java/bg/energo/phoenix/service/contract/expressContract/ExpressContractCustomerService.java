package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.model.response.contract.express.CustomerExpressContractDto;
import bg.energo.phoenix.model.response.contract.express.ExpressContractCustomerShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.service.customer.CustomerSegmentService;
import bg.energo.phoenix.service.customer.CustomerService;
import bg.energo.phoenix.service.customer.UnwantedCustomerService;
import bg.energo.phoenix.service.massImport.DatabaseMapper;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpressContractCustomerService {

    private final CustomerService customerService;
    private final DatabaseMapper databaseMapper;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final CommunicationContactPurposeProperties productContractProperties;
    private final UnwantedCustomerService unwantedCustomerService;
    private final CustomerSegmentService customerSegmentService;

    @Transactional(propagation = Propagation.MANDATORY)
    public CustomerExpressContractDto create(ExpressContractCustomerRequest request) {

        UnwantedCustomer unwantedCustomer = unwantedCustomerService.checkUnwantedCustomer(request.getIdentifier());
        if (unwantedCustomer != null && (Boolean.TRUE.equals(unwantedCustomer.getCreateContractRestriction()) && unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.ACTIVE))) {
            throw new IllegalArgumentsProvidedException("customer.identifier-Customer is unwanted!;");

        }
        Optional<Customer> customerOptional = customerRepository.findByIdentifierAndStatus(request.getIdentifier(), CustomerStatus.ACTIVE);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();

            CustomerDetails customerDetails = customerDetailsRepository.findFirstByCustomerId(customer.getId(), Sort.by(Sort.Direction.DESC, "versionId"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("customer.identifier-Customer details not found!;"));
            ExpressContractCustomerShortResponse customerShortResponse = customerService.getCustomerShortResponse(customerDetails, customer);
            if(request.getBusinessCustomerDetails() != null && request.getBusinessCustomerDetails().getProcurementLaw() == null){
                request.getBusinessCustomerDetails().setProcurementLaw(false);
            }
            if (customerShortResponse.equalsRequest(request)) {
                return new CustomerExpressContractDto(customer.getId(),customerDetails.getId(),customerDetails.getVersionId(),customer.getIdentifier());
            } else {
                return updateCustomer(request, customerDetails, customer);
            }
        } else {
            return createCustomer(request);
        }
    }

    public ExpressContractCustomerShortResponse getCustomerShortResponse(String customerIdentifier) {
        Customer customer = customerRepository.findByIdentifierAndStatus(customerIdentifier, CustomerStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Customer not found;"));
        CustomerDetails customerDetails = customerDetailsRepository.findFirstByCustomerId(customer.getId(), Sort.by(Sort.Direction.DESC, "versionId"))
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Customer not found;"));
        return customerService.getCustomerShortResponse(customerDetails, customer);
    }


    private CustomerExpressContractDto createCustomer(ExpressContractCustomerRequest request) {
        CreateCustomerRequest createCustomerRequest = databaseMapper.fillCustomerCreateRequestForExpress(request);
        CustomerResponse customerResponse = customerService.create(createCustomerRequest, new HashSet<>());
        return new CustomerExpressContractDto(customerResponse.getId(),customerResponse.getLastCustomerDetailId(),customerResponse.getVersion(),customerResponse.getIdentifier());
    }

    private CustomerExpressContractDto updateCustomer(ExpressContractCustomerRequest request, CustomerDetails details, Customer customer) {
        EditCustomerRequest editCustomerRequest = databaseMapper.fillEditCustomerRequestWithDBInfo(customer, details);
        editCustomerRequest.setCustomerType(request.getCustomerType());
        if (request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER) {
            editCustomerRequest.setPrivateCustomerDetails(request.getPrivateCustomerDetails().toPrivateCustomerDetails());
            editCustomerRequest.setBusinessActivity(null);
            editCustomerRequest.setBusinessCustomerDetails(null);
            editCustomerRequest.setOwnershipFormId(null);
            editCustomerRequest.setEconomicBranchId(null);
            editCustomerRequest.setMainSubjectOfActivity(null);
            editCustomerRequest.setManagers(null);

        }
        if(request.getCustomerType().equals(CustomerType.LEGAL_ENTITY)||(request.getBusinessActivity()!=null && request.getBusinessActivity())){

            editCustomerRequest.setBusinessActivity(true);
            editCustomerRequest.setBusinessCustomerDetails(request.getBusinessCustomerDetails().toBusinessCustomer());
            editCustomerRequest.setOwnershipFormId(request.getOwnershipFormId());
            editCustomerRequest.setEconomicBranchId(request.getEconomicBranchCiId());
            editCustomerRequest.setMainSubjectOfActivity(request.getMainActivitySubject());
            if(request.getManagerRequests()!=null && !request.getManagerRequests().isEmpty()){
            editCustomerRequest.setManagers(request.getManagerRequests().stream().map(ExpressContractMapper::editManagerRequest).toList());}
        }
        if(request.getCustomerType().equals(CustomerType.LEGAL_ENTITY)){
            editCustomerRequest.setPrivateCustomerDetails(null);
        }

        if (!customerSegmentService.hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT)) {
            Set<Long> existingSegments = details.getCustomerSegments().stream()
                    .map(cs -> cs.getSegment().getId())
                    .collect(Collectors.toSet());

            if (!request.getCustomerSegments().equals(existingSegments)) {
                throw new IllegalArgumentsProvidedException("segments-You don't have permission to edit segments;");
            }

            editCustomerRequest.setSegmentIds(new ArrayList<>(existingSegments));
        } else {
            editCustomerRequest.setSegmentIds(new ArrayList<>(request.getCustomerSegments()));
        }

        editCustomerRequest.setCommunicationData(request.getCommunications()
                .stream()
                .map(x -> ExpressContractMapper
                        .editCommunications(x, productContractProperties.getBillingCommunicationId(), productContractProperties.getContractCommunicationId()))
                .toList());
        editCustomerRequest.setAddress(request.getAddress());
        editCustomerRequest.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
        editCustomerRequest.setMarketingConsent(request.isConsentToMarketingCommunication());
        CustomerResponse update = customerService.update(customer.getId(), editCustomerRequest, new HashSet<>());
        return new CustomerExpressContractDto(customer.getId(),update.getLastCustomerDetailId(),update.getVersion(),customer.getIdentifier());
    }


}
