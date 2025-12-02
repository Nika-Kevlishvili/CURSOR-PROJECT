package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerSegment;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.CustomerSegmentRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerSegmentService {

    private final SegmentRepository segmentRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final PermissionService permissionService;

    /**
     * <h1>Create Customer Segment</h1>
     * Function validates if segment ids is not null or empty , then creates CustomerSegment object and saves it in db
     *
     * @param request           {@link CreateCustomerRequest}
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages to be populated if any exception occurs
     */
    public void createCustomerSegment(CreateCustomerRequest request,
                                      CustomerDetails customerDetails,
                                      List<NomenclatureItemStatus> statuses,
                                      Set<String> permissions,
                                      List<String> exceptionMessages,
                                      CustomerDetailStatus customerDetailStatus) {
        if ((request.getSegmentIds() == null || request.getSegmentIds().isEmpty()) && customerDetailStatus == CustomerDetailStatus.POTENTIAL) {
            return;
        }

        boolean hasSegmentPermission = (permissions != null && permissions.contains(PermissionEnum.MI_CUSTOMER_EDIT_SEGMENT.getId()))
                || hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT);

        if (!hasSegmentPermission) {
            Optional<Segment> defaultSegment = segmentRepository.findByDefaultSelectionTrue();
            if (defaultSegment.isEmpty()) {
                exceptionMessages.add("segments-No default segment found and you don't have permission to edit segments;");
                return;
            }

            if (!(request.getSegmentIds().size() == 1 && request.getSegmentIds().contains(defaultSegment.get().getId()))) {
                exceptionMessages.add("segments-You don't have permission to edit segments. Only default segment is allowed;");
                return;
            }

            if (customerDetails != null) {
                CustomerSegment customerSegment = new CustomerSegment();
                customerSegment.setStatus(Status.ACTIVE);
                customerSegment.setSegment(defaultSegment.get());
                customerSegment.setCustomerDetail(customerDetails);
                customerSegmentRepository.save(customerSegment);
            }

            return;
        }

        if (request.getSegmentIds() != null && !request.getSegmentIds().isEmpty()) {
            List<CustomerSegment> customerSegments = new ArrayList<>();
            for (Long segmentId : request.getSegmentIds()) {
                Segment segment = getSegment(request, segmentId, statuses, exceptionMessages);

                if (segment != null && customerDetails != null) {
                    CustomerSegment customerSegment = new CustomerSegment();
                    customerSegment.setStatus(Status.ACTIVE);
                    customerSegment.setSegment(segment);
                    customerSegment.setCustomerDetail(customerDetails);
                    customerSegments.add(customerSegment);
                }
            }

            if (exceptionMessages.isEmpty()) {
                customerSegmentRepository.saveAll(customerSegments);
            }
        }
    }

    /**
     * <h1>createCustomerSegment</h1>
     * if segmentIds from request is not null function will create new CustomerSegment object
     * and save it in database
     *
     * @param request           {@link EditCustomerRequest}
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages to be populated if any exception occurs
     * @return list of save customerSegment objects
     */
    public List<CustomerSegment> createAndGetCustomerSegment(EditCustomerRequest request,
                                                             CustomerDetails customerDetails,
                                                             CustomerDetails oldDetails,
                                                             Set<String> permissions,
                                                             List<String> exceptionMessages) {

        boolean hasSegmentPermission = (permissions != null && permissions.contains(PermissionEnum.MI_CUSTOMER_EDIT_SEGMENT.getId()))
                || hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT);

        if (!hasSegmentPermission) {
            Set<Long> oldSegmentIds = oldDetails.getCustomerSegments().stream()
                    .map(cs -> cs.getSegment().getId())
                    .collect(Collectors.toSet());

            if (request.getSegmentIds() != null &&
                    !new HashSet<>(request.getSegmentIds()).equals(oldSegmentIds)) {
                throw new IllegalArgumentsProvidedException("segments-You don't have permission to edit segments. Changes to segments are not allowed;");
            }

            return copySegmentsFromOldVersion(oldDetails, customerDetails);
        }

        List<CustomerSegment> returnList = null;

        if (!CollectionUtils.isEmpty(request.getSegmentIds())) {
            List<Long> oldSegments = oldDetails.getCustomerSegments().stream().map(x -> x.getSegment().getId()).toList();
            List<CustomerSegment> customerSegments = new ArrayList<>();
            for (Long segmentId : request.getSegmentIds()) {
                CustomerSegment customerSegment = new CustomerSegment();
                customerSegment.setStatus(Status.ACTIVE);//TODO: May be refactored in the future
                customerSegment.setSegment(getSegment(request, segmentId, getSegmentStatuses(oldSegments, segmentId), exceptionMessages));
                customerSegment.setCustomerDetail(customerDetails);
                customerSegments.add(customerSegment);
            }
            if (exceptionMessages.isEmpty()) {
                returnList = customerSegmentRepository.saveAll(customerSegments);
            }
        }
        return returnList;
    }

    private List<CustomerSegment> copySegmentsFromOldVersion(CustomerDetails oldDetails, CustomerDetails newDetails) {
        List<CustomerSegment> newSegments = new ArrayList<>();

        for (CustomerSegment oldSegment : oldDetails.getCustomerSegments()) {
            CustomerSegment newSegment = new CustomerSegment();
            newSegment.setStatus(oldSegment.getStatus());
            newSegment.setSegment(oldSegment.getSegment());
            newSegment.setCustomerDetail(newDetails);
            newSegments.add(newSegment);
        }

        return customerSegmentRepository.saveAll(newSegments);
    }

    private List<NomenclatureItemStatus> getSegmentStatuses(List<Long> oldSegments, Long id) {
        if (oldSegments.contains(id)) {
            return List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h1>Get Segment</h1>
     * function gets segment from table based on segment db id and status and returns it
     *
     * @param request           {@link CreateCustomerRequest}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages to be populated if any exception occurs
     * @return Segment object
     */
    private Segment getSegment(CreateCustomerRequest request,
                               Long segmentId,
                               List<NomenclatureItemStatus> statuses,
                               List<String> exceptionMessages) {
        Optional<Segment> optionalSegment = segmentRepository
                .findByIdAndStatus(segmentId, statuses);
        if (optionalSegment.isEmpty()) {
            if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL || segmentId != null) {
                log.error("segmentIds-Segment not found with id: " + segmentId + ";");
                exceptionMessages.add("segmentIds-Segment not found with id: " + segmentId + ";");
            }
            return null;
        } else {
            return optionalSegment.get();
        }
    }

    /**
     * <h1>Get Segment</h1>
     * function will get segments from segments nomenclature based on the status
     *
     * @param request           {@link EditCustomerRequest}
     * @param segmentId         segment db id
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages to be populated if any exception occurs
     * @return Segment object
     */
    private Segment getSegment(EditCustomerRequest request,
                               Long segmentId,
                               List<NomenclatureItemStatus> statuses,
                               List<String> exceptionMessages) {
        Optional<Segment> optionalSegment = segmentRepository.findByIdAndStatus(segmentId, statuses);
        if (optionalSegment.isEmpty()) {
            if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL || segmentId != null) {
                log.error("segmentIds-Segment not found with id: " + segmentId + ";");
                exceptionMessages.add("segmentIds-Segment not found with id: " + segmentId + ";");
            }
            return null;
        } else {
            return optionalSegment.get();
        }
    }

    /**
     * <h1>Edit Customer Segment</h1>
     * function will create new one or update customer segments list attached to the customer
     *
     * @param request           {@link EditCustomerRequest}
     * @param customerDetails   {@link CustomerDetails}
     * @param statuses          {@link NomenclatureItemStatus}
     * @param exceptionMessages list of exception messages to be populated if any exception occurs
     * @return list of customer segment object
     */
    @Transactional
    public List<CustomerSegment> editCustomerSegment(EditCustomerRequest request, CustomerDetails customerDetails, Set<String> permissions, List<String> exceptionMessages) {
        boolean hasSegmentPermission = (permissions != null && permissions.contains(PermissionEnum.MI_CUSTOMER_EDIT_SEGMENT.getId()))
                || hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT);

        if (!hasSegmentPermission) {
            Set<Long> existingSegments = customerDetails.getCustomerSegments().stream()
                    .map(cs -> cs.getSegment().getId())
                    .collect(Collectors.toSet());

            if (request.getSegmentIds() != null &&
                    !new HashSet<>(request.getSegmentIds()).equals(existingSegments)) {
                throw new IllegalArgumentsProvidedException("segments-You don't have permission to edit segments. Changes to segments are not allowed;");
            }

            return customerSegmentRepository.findAllByCustomerDetailId(customerDetails.getId());
        }

        if (request.getSegmentIds() == null) {
            customerSegmentRepository.deleteAll(customerDetails.getCustomerSegments());
        } else {
            List<Long> editedCustomerSegmentIds = new ArrayList<>();
            List<CustomerSegment> newCustomerSegments = new ArrayList<>();
            List<Long> segmentIds = request.getSegmentIds();
            for (long item : segmentIds) {
                //if new and old ids equals - it's ok , if new is active - it's ok
                Optional<Segment> segmentOptional = segmentRepository.findById(item);
                if (segmentOptional.isEmpty()) {
                    log.error("segmentIds-Segment not found with id: " + item);
                    exceptionMessages.add("segmentIds-Segment not found with id: " + item + ";");
                    return null;
                }
                Segment segment = segmentOptional.get();

                Optional<CustomerSegment> dbSegmentOptional = customerSegmentRepository.findBySegmentIdAndCustomerDetailId(item, customerDetails.getId());
                if (dbSegmentOptional.isPresent()) {
                    editedCustomerSegmentIds.add(dbSegmentOptional.get().getId());
                } else {
                    if (!segment.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                        exceptionMessages.add("segmentIds-Segment %s is not active;".formatted(item));
                        return null;
                    }
                    CustomerSegment customerSegment = new CustomerSegment();
                    customerSegment.setStatus(Status.ACTIVE);
                    customerSegment.setSegment(segment);
                    customerSegment.setCustomerDetail(customerDetails);
                    newCustomerSegments.add(customerSegment);
                }
            }
            for (CustomerSegment customerSegment : customerDetails.getCustomerSegments()) {
                if (!editedCustomerSegmentIds.contains(customerSegment.getId())) {
                    customerSegmentRepository.delete(customerSegment);
                }
            }
            customerSegmentRepository.saveAll(newCustomerSegments);
        }

        return customerSegmentRepository.findAllByCustomerDetailId(customerDetails.getId());
    }


    public List<SegmentResponse> findSegmentsForCustomer(Long customerDetailId) {
        List<CustomerSegment> customerSegments = customerSegmentRepository.findAllByCustomerDetailIdAndStatus(customerDetailId, Status.ACTIVE);
        List<SegmentResponse> segmentResponses = new ArrayList<>();
        for (CustomerSegment customerSegment : customerSegments) {
            segmentResponses.add(new SegmentResponse(customerSegment.getSegment()));
        }
        return segmentResponses;
    }

    public List<CustomerSegment> findCustomerSegmentsForCustomer(Long customerDetailId) {
        return customerSegmentRepository.findAllByCustomerDetailIdAndStatus(customerDetailId, Status.ACTIVE);
    }

    public Optional<Segment> getDefaultSegment() {
        return segmentRepository.findByDefaultSelectionTrue();
    }

    public boolean hasPermission(PermissionEnum permission) {
        return permissionService.getPermissionsFromContext(CUSTOMER).contains(permission.getId());
    }

    public boolean hasPermission(PermissionEnum permission, Set<String> permissions) {
        // First check explicit permissions set, for mass import
        if (permissions != null && !permissions.isEmpty()) {
            return permissions.contains(permission.getId());
        }

        // Fall back to regular permission check
        return permissionService.getPermissionsFromContext(CUSTOMER)
                .contains(permission.getId());
    }
}
