package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.SegmentRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SEGMENTS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService implements NomenclatureBaseService {
    private final SegmentRepository segmentRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SEGMENTS;
    }

    /**
     * Filters {@link Segment} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Segment}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SEGMENTS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering segments list with statuses: {}", request);
        return segmentRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link Segment} item in the Segment list to a specified position.
     * The method retrieves the {@link Segment} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Segment} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Segment} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SEGMENTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in segments to top", request.getId());

        Segment segment = segmentRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Segment not found"));

        Long start;
        Long end;
        List<Segment> segments;

        if (segment.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = segment.getOrderingId();
            segments = segmentRepository.findInOrderingIdRange(start, end, segment.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Segment s : segments) {
                s.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = segment.getOrderingId();
            end = request.getOrderingId();
            segments = segmentRepository.findInOrderingIdRange(start, end, segment.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Segment s : segments) {
                s.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        segment.setOrderingId(request.getOrderingId());
        segments.add(segment);
        segmentRepository.saveAll(segments);
    }

    /**
     * Sorts all {@link Segment} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SEGMENTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the segments alphabetically");
        List<Segment> segments = segmentRepository.orderByName();
        long orderingId = 1;

        for (Segment s : segments) {
            s.setOrderingId(orderingId);
            orderingId++;
        }

        segmentRepository.saveAll(segments);
    }

    /**
     * Deletes {@link Segment} if the validations are passed.
     *
     * @param id ID of the {@link Segment}
     * @throws DomainEntityNotFoundException if {@link Segment} is not found.
     * @throws OperationNotAllowedException  if the {@link Segment} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Segment} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SEGMENTS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing segment with ID: {}", id);
        Segment segment = segmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Segment not found"));

        if (segment.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        Long activeConnections = segmentRepository.activeConnectionCount(
                id,
                List.of(ProductDetailStatus.ACTIVE,ProductDetailStatus.INACTIVE),
                List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                List.of(GoodsDetailStatus.ACTIVE,GoodsDetailStatus.INACTIVE)
        );

        if (activeConnections > 0){
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        if (segmentRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        segment.setStatus(DELETED);
        segmentRepository.save(segment);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return segmentRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return segmentRepository.findByIdIn(ids);
    }


    /**
     * Adds {@link Segment} at the end with the highest ordering ID.
     * If the request asks to save {@link Segment} as a default and a default {@link Segment} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link Segment}
     * @return {@link SegmentResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public SegmentResponse add(SegmentRequest request) {

        log.debug("Adding Segment: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (segmentRepository.countSegmentByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Segment with the same name already exists;");
            throw new OperationNotAllowedException("name-Segment with the same name already exists;");
        }

        Long lastSortOrder = segmentRepository.findLastOrderingId();
        Segment segment = new Segment(request);
        segment.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Segment> currentDefaultSegmentOptional = segmentRepository.findByDefaultSelectionTrue();
            if (currentDefaultSegmentOptional.isPresent()) {
                Segment currentDefaultSegment = currentDefaultSegmentOptional.get();
                currentDefaultSegment.setDefaultSelection(false);
                segmentRepository.save(currentDefaultSegment);
            }
        }
        Segment segmentEntity = segmentRepository.save(segment);
        return new SegmentResponse(segmentEntity);
    }

    /**
     * Retrieves detailed information about {@link Segment} by ID
     *
     * @param id ID of {@link Segment}
     * @return {@link SegmentResponse}
     * @throws DomainEntityNotFoundException if no {@link Segment} was found with the provided ID.
     */
    public SegmentResponse view(Long id) {
        log.debug("Fetching Segment with ID: {}", id);
        Segment segment = segmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Segment not found"));
        return new SegmentResponse(segment);
    }

    /**
     * Edit the requested {@link Segment}.
     * If the request asks to save {@link Segment} as a default and a default {@link Segment} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Segment}
     * @param request {@link SegmentRequest}
     * @return {@link SegmentResponse}
     * @throws DomainEntityNotFoundException if {@link Segment} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Segment} is deleted.
     */
    @Transactional
    public SegmentResponse edit(Long id, SegmentRequest request) {
        log.debug("Editing segment: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Segment segment = segmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Segment not found"));

        if (segmentRepository.countSegmentByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !segment.getName().equals(request.getName().trim())) {
            log.error("name-Segment with the same name already exists;");
            throw new OperationNotAllowedException("name-Segment with the same name already exists;");
        }

        if (segment.getStatus().equals(DELETED)) {
            log.error("status-Can not edit DELETED item");
            throw new OperationNotAllowedException("status-Can not edit DELETED item");
        }

        if (request.getDefaultSelection() && !segment.isDefaultSelection()) {
            Optional<Segment> currentDefaultSegmentOptional = segmentRepository.findByDefaultSelectionTrue();
            if (currentDefaultSegmentOptional.isPresent()) {
                Segment currentDefaultSegment = currentDefaultSegmentOptional.get();
                currentDefaultSegment.setDefaultSelection(false);
                segmentRepository.save(currentDefaultSegment);
            }
        }
        segment.setDefaultSelection(request.getDefaultSelection());

        segment.setName(request.getName().trim());
        segment.setStatus(request.getStatus());
        return new SegmentResponse(segmentRepository.save(segment));
    }

    /**
     * Filters {@link Segment} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Segment}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<SegmentResponse> Page&lt;SegmentResponse&gt;} containing detailed information
     */
    public Page<SegmentResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering segments list with statuses: {}", request.toString());
        Page<Segment> page = segmentRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(SegmentResponse::new);
    }
}
