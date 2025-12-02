package bg.energo.phoenix.service.nomenclature.product.priceComponent;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentPriceType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.PriceComponentPriceTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentPriceTypeResponse;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.PriceComponentPriceTypeRepository;
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
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PC_PRICE_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceComponentPriceTypeService implements NomenclatureBaseService {

    private final PriceComponentPriceTypeRepository priceComponentPriceTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.PRICE_COMPONENT_PRICE_TYPE;
    }

    /**
     * Filters {@link PriceComponentPriceTypeResponse} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link PriceComponentPriceTypeResponse}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<PriceComponentPriceTypeResponse> Page&lt;PriceComponentPriceTypeResponse&gt;} containing detailed information
     */
    public Page<PriceComponentPriceTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering PriceComponentPriceType list with request: {}", request.toString());
        Page<PriceComponentPriceType> page = priceComponentPriceTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(PriceComponentPriceTypeResponse::new);
    }

    /**
     * Filters {@link PriceComponentPriceType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link PriceComponentPriceType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_PRICE_TYPES, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return priceComponentPriceTypeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    @Transactional
    public PriceComponentPriceTypeResponse add(PriceComponentPriceTypeRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED ", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        String name = request.getName();
        request.setName(name.trim());
        Integer count = getExistingRecordsCountByName(request.getName());
        if (count > 0) {
            log.error("PriceComponentPriceType Name is not unique");
            throw new ClientException("name-PriceComponentPriceType Name is not unique ", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Long lastSortOrder = priceComponentPriceTypeRepository.findLastOrderingId();
        PriceComponentPriceType priceComponentPriceType = new PriceComponentPriceType(request);
        priceComponentPriceType.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), priceComponentPriceType);
        PriceComponentPriceType priceType = priceComponentPriceTypeRepository.save(priceComponentPriceType);
        return new PriceComponentPriceTypeResponse(priceType);
    }

    /**
     * <h1>Check Name For Uniqueness</h1>
     * function returns count of name in database
     *
     * @param name
     * @return Integer count of name
     */
    private Integer getExistingRecordsCountByName(String name) {
        return priceComponentPriceTypeRepository.getExistingRecordsCountByName(name.toLowerCase(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
    }

    /**
     * AssignDefaultSelection
     *
     * @param status
     * @param isDefaultSelection
     * @param priceComponentPriceType
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean isDefaultSelection, PriceComponentPriceType priceComponentPriceType) {
        if (status.equals(INACTIVE)) {
            priceComponentPriceType.setIsDefault(false);
        } else {
            if (isDefaultSelection) {
                Optional<PriceComponentPriceType> currentDefaultPriceComponentPriceTypeOptional = priceComponentPriceTypeRepository.findByIsDefaultTrue();
                if (currentDefaultPriceComponentPriceTypeOptional.isPresent()) {
                    PriceComponentPriceType currentDefaultPriceComponentPriceType = currentDefaultPriceComponentPriceTypeOptional.get();
                    currentDefaultPriceComponentPriceType.setIsDefault(false);
                    priceComponentPriceTypeRepository.save(currentDefaultPriceComponentPriceType);
                }
                priceComponentPriceType.setIsDefault(true);
            } else {
                priceComponentPriceType.setIsDefault(false);
            }
        }
    }

    /**
     * Retrieves detailed information about {@link PriceComponentPriceTypeResponse} by ID
     *
     * @param id ID of {@link PriceComponentPriceTypeResponse}
     * @return {@link PriceComponentPriceTypeResponse}
     * @throws DomainEntityNotFoundException if no {@link PriceComponentPriceTypeResponse} was found with the provided ID.
     */
    public PriceComponentPriceTypeResponse view(Long id) {
        log.debug("Fetching PriceComponentPriceType with ID: {}", id);
        PriceComponentPriceType priceComponentPriceType = priceComponentPriceTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));
        return new PriceComponentPriceTypeResponse(priceComponentPriceType);
    }

    /**
     * Edit the requested {@link PriceComponentPriceType}.
     * If the request asks to save {@link PriceComponentPriceType} as a default and a default {@link PriceComponentPriceType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link PriceComponentPriceType}
     * @param request {@link PriceComponentPriceTypeRequest}
     * @return {@link PriceComponentPriceTypeResponse}
     * @throws DomainEntityNotFoundException if {@link PriceComponentPriceType} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link PriceComponentPriceType} is deleted.
     */
    @Transactional
    public PriceComponentPriceTypeResponse edit(Long id, PriceComponentPriceTypeRequest request) {
        log.debug("Editing PriceComponentPriceType: {}, with ID: {}", request.toString(), id);
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        String name = request.getName();
        request.setName(name.trim());

        PriceComponentPriceType priceComponentPriceType = priceComponentPriceTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (priceComponentPriceType.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item");
        }

        if (!priceComponentPriceType.getName().equals(request.getName())) {
            if (getExistingRecordsCountByName(request.getName()) > 0) {
                log.error("PriceComponentPriceType Name is not unique");
                throw new ClientException("name-PriceComponentPriceType Name is not unique", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), priceComponentPriceType);
        priceComponentPriceType.setName(request.getName());
        priceComponentPriceType.setStatus(request.getStatus());
        return new PriceComponentPriceTypeResponse(priceComponentPriceTypeRepository.save(priceComponentPriceType));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_PRICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in PriceComponentPriceType to top", request.getId());

        PriceComponentPriceType priceComponentPriceType = priceComponentPriceTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<PriceComponentPriceType> priceComponentPriceTypeList;

        if (priceComponentPriceType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = priceComponentPriceType.getOrderingId();
            priceComponentPriceTypeList = priceComponentPriceTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    priceComponentPriceType.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (PriceComponentPriceType p : priceComponentPriceTypeList) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = priceComponentPriceType.getOrderingId();
            end = request.getOrderingId();
            priceComponentPriceTypeList = priceComponentPriceTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    priceComponentPriceType.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (PriceComponentPriceType cp : priceComponentPriceTypeList) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        priceComponentPriceType.setOrderingId(request.getOrderingId());
        priceComponentPriceTypeRepository.save(priceComponentPriceType);
        priceComponentPriceTypeRepository.saveAll(priceComponentPriceTypeList);
    }

    /**
     * Sorts all {@link PriceComponentPriceType} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_PRICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the PriceComponentPriceTypes alphabetically");
        List<PriceComponentPriceType> priceComponentPriceTypeList = priceComponentPriceTypeRepository.orderByName();
        long orderingId = 1;

        for (PriceComponentPriceType p : priceComponentPriceTypeList) {
            p.setOrderingId(orderingId);
            orderingId++;
        }

        priceComponentPriceTypeRepository.saveAll(priceComponentPriceTypeList);
    }

    /**
     * Deletes {@link PriceComponentPriceType} if the validations are passed.
     *
     * @param id ID of the {@link PriceComponentPriceType}
     * @throws DomainEntityNotFoundException if {@link PriceComponentPriceType} is not found.
     * @throws OperationNotAllowedException  if the {@link PriceComponentPriceType} is already deleted.
     * @throws OperationNotAllowedException  if the {@link PriceComponentPriceType} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PC_PRICE_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing PriceComponentPriceType with ID: {}", id);
        PriceComponentPriceType priceComponentPriceType = priceComponentPriceTypeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        if (priceComponentPriceType.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (Boolean.TRUE.equals(priceComponentPriceType.getIsHardcoded())) {
            log.error("Attempted to delete hardcoded item");
            throw new OperationNotAllowedException("id-Attempted to delete hardcoded item");
        }

        Long activeConnections = priceComponentPriceTypeRepository.activeConnectionCount(
                id,
                List.of(PriceComponentStatus.ACTIVE)
        );

        if (activeConnections > 0) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        // TODO:Check if there is no connected object to this nomenclature item in system
        priceComponentPriceType.setStatus(DELETED);
        priceComponentPriceTypeRepository.save(priceComponentPriceType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return priceComponentPriceTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return priceComponentPriceTypeRepository.findByIdIn(ids);
    }
}
