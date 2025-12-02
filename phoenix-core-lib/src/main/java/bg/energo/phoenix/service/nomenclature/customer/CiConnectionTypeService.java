package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.CiConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.CiConnectionTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.CiConnectionTypeResponse;
import bg.energo.phoenix.repository.nomenclature.customer.CiConnectionTypeRepository;
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

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CI_CONNECTION_TYPE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CiConnectionTypeService implements NomenclatureBaseService {
    private final CiConnectionTypeRepository ciConnectionTypeRepository;

    /**
     * Filters {@link CiConnectionType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link CiConnectionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<CiConnectionTypeResponse> Page&lt;CiConnectionTypeResponse&gt;} containing detailed information
     */
    public Page<CiConnectionTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering CI_CONNECTION_TYPE list with request: {}", request.toString());
        Page<CiConnectionType> page = ciConnectionTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(CiConnectionTypeResponse::new);
    }

    /**
     * Filters {@link CiConnectionType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link CiConnectionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CI_CONNECTION_TYPE, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ciConnectionType list with statuses: {}", request);
        return ciConnectionTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link CiConnectionType} at the end with the highest ordering ID.
     * If the request asks to save {@link CiConnectionType} as a default and a default {@link CiConnectionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link CiConnectionType}
     * @return {@link CiConnectionTypeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public CiConnectionTypeResponse add(CiConnectionTypeRequest request) {
        log.debug("Adding CiConnectionType: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (ciConnectionTypeRepository.countCiConnectionTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-CiConnectionType with the same name already exists;");
            throw new OperationNotAllowedException("name-CiConnectionType with the same name already exists;");
        }

        Long lastSortOrder = ciConnectionTypeRepository.findLastOrderingId();
        CiConnectionType ciConnectionType = new CiConnectionType(request);
        ciConnectionType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<CiConnectionType> currentDefaultCiConnectionOptional = ciConnectionTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultCiConnectionOptional.isPresent()) {
                CiConnectionType currentDefaultCiConnectionType = currentDefaultCiConnectionOptional.get();
                currentDefaultCiConnectionType.setDefaultSelection(false);
                ciConnectionTypeRepository.save(currentDefaultCiConnectionType);
            }
        }
        CiConnectionType ciConnectionTypeEntity = ciConnectionTypeRepository.save(ciConnectionType);
        return new CiConnectionTypeResponse(ciConnectionTypeEntity);
    }

    /**
     * Retrieves detailed information about {@link CiConnectionType} by ID
     *
     * @param id ID of {@link CiConnectionType}
     * @return {@link CiConnectionTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link CiConnectionType} was found with the provided ID.
     */
    public CiConnectionTypeResponse view(Long id) {
        log.debug("Fetching CiConnectionType with ID: {}", id);
        CiConnectionType ciConnectionType = ciConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));
        return new CiConnectionTypeResponse(ciConnectionType);
    }

    /**
     * Edit the requested {@link CiConnectionType}.
     * If the request asks to save {@link CiConnectionType} as a default and a default {@link CiConnectionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link CiConnectionType}
     * @param request {@link CiConnectionTypeRequest}
     * @return {@link CiConnectionTypeResponse}
     * @throws DomainEntityNotFoundException if {@link CiConnectionType} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link CiConnectionType} is deleted.
     */
    @Transactional
    public CiConnectionTypeResponse edit(Long id, CiConnectionTypeRequest request) {
        log.debug("Editing CiConnectionType: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot set DELETED status",ILLEGAL_ARGUMENTS_PROVIDED);
        }

        CiConnectionType ciConnectionType = ciConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (ciConnectionTypeRepository.countCiConnectionTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !ciConnectionType.getName().equals(request.getName().trim())) {
            log.error("name-CiConnectionType with the same name already exists;");
            throw new OperationNotAllowedException("name-CiConnectionType with the same name already exists;");
        }

        if (ciConnectionType.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !ciConnectionType.isDefaultSelection()) {
            Optional<CiConnectionType> currentDefaultCiConnectionTypeOptional = ciConnectionTypeRepository.findByDefaultSelectionTrue();
            if (currentDefaultCiConnectionTypeOptional.isPresent()) {
                CiConnectionType currentDefaultCiConnectionType = currentDefaultCiConnectionTypeOptional.get();
                currentDefaultCiConnectionType.setDefaultSelection(false);
                ciConnectionTypeRepository.save(currentDefaultCiConnectionType);
            }
        }
        ciConnectionType.setDefaultSelection(request.getDefaultSelection());

        ciConnectionType.setName(request.getName().trim());
        ciConnectionType.setStatus(request.getStatus());
        return new CiConnectionTypeResponse(ciConnectionTypeRepository.save(ciConnectionType));
    }

    /**
     * Deletes {@link CiConnectionType} if the validations are passed.
     *
     * @param id ID of the {@link CiConnectionType}
     * @throws DomainEntityNotFoundException if {@link CiConnectionType} is not found.
     * @throws OperationNotAllowedException  if the {@link CiConnectionType} is already deleted.
     * @throws OperationNotAllowedException  if the {@link CiConnectionType} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CI_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing ciConnectionType with ID: {}", id);
        CiConnectionType ciConnectionType = ciConnectionTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        if (ciConnectionType.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (ciConnectionTypeRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("id-Item is connected to active object, cannot be deleted");
        }

        ciConnectionType.setStatus(DELETED);
        ciConnectionTypeRepository.save(ciConnectionType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return ciConnectionTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return ciConnectionTypeRepository.findByIdIn(ids);
    }

    /**
     * Sorts all {@link CiConnectionType} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CI_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the CiConnectionType alphabetically");
        List<CiConnectionType> ciConnectionTypes = ciConnectionTypeRepository.orderByName();
        long orderingId = 1;

        for (CiConnectionType c : ciConnectionTypes) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        ciConnectionTypeRepository.saveAll(ciConnectionTypes);
    }

    /**
     * Changes the ordering of a {@link CiConnectionType} item in the CiConnectionType list to a specified position.
     * The method retrieves the {@link CiConnectionType} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link CiConnectionType} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link CiConnectionType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CI_CONNECTION_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in ciConnectionTypes to top", request.getId());

        CiConnectionType ciConnectionType = ciConnectionTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found",DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<CiConnectionType> ciConnectionTypeList;

        if (ciConnectionType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = ciConnectionType.getOrderingId();
            ciConnectionTypeList = ciConnectionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    ciConnectionType.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (CiConnectionType c : ciConnectionTypeList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = ciConnectionType.getOrderingId();
            end = request.getOrderingId();
            ciConnectionTypeList = ciConnectionTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    ciConnectionType.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (CiConnectionType c : ciConnectionTypeList) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        ciConnectionType.setOrderingId(request.getOrderingId());
        ciConnectionTypeRepository.save(ciConnectionType);
        ciConnectionTypeRepository.saveAll(ciConnectionTypeList);
    }

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CI_CONNECTION_TYPE;
    }

}
