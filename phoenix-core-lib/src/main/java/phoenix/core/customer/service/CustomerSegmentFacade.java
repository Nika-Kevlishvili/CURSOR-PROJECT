package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.CustomerSegment;
import phoenix.core.customer.model.entity.nomenclature.customer.Segment;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.CreateCustomerRequest;
import phoenix.core.customer.model.request.EditCustomerRequest;
import phoenix.core.customer.repository.customer.CustomerSegmentRepository;
import phoenix.core.customer.repository.nomenclature.customer.SegmentRepository;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.DomainEntityNotFoundException;
import phoenix.core.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.*;

@Service("coreCustomerSegmentService")
@RequiredArgsConstructor
@Validated
public class CustomerSegmentFacade {

    private final SegmentRepository segmentRepository;
    private final CustomerSegmentRepository customerSegmentRepository;

    public void createCustomerSegment(CreateCustomerRequest request,
                                      CustomerDetails customerDetails,
                                      List<NomenclatureItemStatus> statuses,
                                      List<String> exceptionMessages) {
        if (request.getSegmentIds() != null && !request.getSegmentIds().isEmpty()) {
            List<CustomerSegment> customerSegments = new ArrayList<>();
            for (Long segmentId : request.getSegmentIds()) {
                CustomerSegment customerSegment = new CustomerSegment();
                customerSegment.setCreateDate(LocalDateTime.now());
                customerSegment.setSystemUserId("bla");
                customerSegment.setStatus(Status.ACTIVE);//TODO: May be refactored in the future
                customerSegment.setSegment(getSegment(request, segmentId, statuses, exceptionMessages));
                customerSegment.setCustomerDetail(customerDetails);
                customerSegments.add(customerSegment);
            }
            if (exceptionMessages.isEmpty()) customerSegmentRepository.saveAll(customerSegments);
        }
    }

    private Segment getSegment(CreateCustomerRequest request,
                               Long segmentId,
                               List<NomenclatureItemStatus> statuses,
                               List<String> exceptionMessages) {
        Optional<Segment> optionalSegment = segmentRepository
                .findByIdAndStatus(segmentId, statuses);
        if (optionalSegment.isEmpty()) {
            if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                    || segmentId != null) {
                exceptionMessages.add("Segment not found with id: " + segmentId + "; ");
            }
            return null;
        } else {
            return optionalSegment.get();
        }
    }

    @Transactional
    public void editCustomerSegment(EditCustomerRequest request, CustomerDetails customerDetails, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<CustomerSegment> customerSegmentsList = new ArrayList<>();
        List<Long> changedSegmentsList = getSegments(request.getSegmentIds(), customerDetails.getCustomerSegments());
        if (customerDetails.getCustomerSegments().size() == 0) {
            createCustomerSegment(new CreateCustomerRequest(request), customerDetails, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
        } else {
            for (int i = 0; i < changedSegmentsList.size(); i++) {
                Optional<CustomerSegment> dbSegmentOptional = customerSegmentRepository
                        .findByIdAndCustomerDetailId(
                                getSegmentDetailsIdBySegmentId(changedSegmentsList.get(i)
                                        , customerDetails.getCustomerSegments()), customerDetails.getId());
                if (dbSegmentOptional.isPresent()) {
                    CustomerSegment customerSegment = dbSegmentOptional.get();
                    //if new and old ids equals - it's ok , if new is active - it's ok
                    Segment segment = segmentRepository.findById(customerSegment.getSegment().getId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Segment not found"));
                    if (!segment.getStatus().equals(NomenclatureItemStatus.ACTIVE) ||
                            !customerSegment.getSegment().getId().equals(segment.getId())) {
                        segment = segmentRepository.findByIdAndStatus(customerSegment.getSegment().getId(), statuses)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Segment not found"));
                    }
                    customerSegment.setSegment(segment);
                    customerSegment.setModifyDate(LocalDateTime.now());
                    customerSegment.setModifySystemUserId("user"); //TODO add SysUser
                    customerSegment.setCustomerDetail(customerDetails);
                    customerSegmentsList.add(customerSegment);
                } else {
                    throw new ClientException("Customer Segment with this id not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
                }
            }
            customerSegmentRepository.saveAll(customerSegmentsList);
            List<Long> saveSegments = getNewSegments(changedSegmentsList, request.getSegmentIds());
            if (saveSegments.size() != 0) {
                request.setSegmentIds(saveSegments);
                createCustomerSegment(new CreateCustomerRequest(request), customerDetails, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
            }
            if (changedSegmentsList.size() != 0) {
                customerSegmentRepository.deleteAllByIdNotInAndCustomerDetailId(changedSegmentsList, customerDetails.getId());
            }
        }
    }

    private List<Long> getNewSegments(List<Long> changedSegmentsList, List<Long> customerSegments) {
        List<Long> saveList = new ArrayList<>();
        for (int i = 0; i < customerSegments.size(); i++) {
            if (!changedSegmentsList.contains(customerSegments.get(i))) {
                saveList.add(customerSegments.get(i));
            }
        }
        return saveList;
    }

    private List<Long> getSegments(List<Long> segmentsList, List<CustomerSegment> customerSegments) {
        List<Long> returnList = new ArrayList<>();
        Map<Long, Long> retunrMap = new HashMap<>();
        if (segmentsList == null) {
            return new ArrayList<>();
        }
        if (segmentsList.size() != 0) {
            for (int i = 0; i < segmentsList.size(); i++) {
                if (customerSegments.size() != 0) {
                    for (int j = 0; j < customerSegments.size(); j++) {
                        if (segmentsList.get(i).equals(customerSegments.get(j).getSegment().getId())) {
                            if (!returnList.contains(segmentsList.get(i))) {
                                returnList.add(segmentsList.get(i));
                            }
                        }
                    }
                } else return segmentsList;

            }
        }
        return returnList;
    }

    private Long getSegmentDetailsIdBySegmentId(Long segmentId, List<CustomerSegment> customerSegments) {
        Long id = null;
        for (int i = 0; i < customerSegments.size(); i++) {
            Segment segment = customerSegments.get(i).getSegment();
            if (segment.getId().equals(segmentId))
                id = customerSegments.get(i).getId();
        }
        if (id == null) {
            throw new ClientException("Segment Id is null", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return id;
    }
}
