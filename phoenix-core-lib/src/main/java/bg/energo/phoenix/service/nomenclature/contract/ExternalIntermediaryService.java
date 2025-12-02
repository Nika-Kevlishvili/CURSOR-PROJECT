package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.ExternalIntermediary;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.ExternalIntermediaryRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ExternalIntermediaryResponse;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIntermediaryService implements NomenclatureBaseService {
    private final ExternalIntermediaryRepository externalIntermediaryRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.EXTERNAL_INTERMEDIARIES;
    }

    /**
     * Filters {@link ExternalIntermediary} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ExternalIntermediary}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = EXTERNAL_INTERMEDIARIES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(
                            context = PRODUCT_CONTRACTS,
                            permissions = {PRODUCT_CONTRACT_CREATE, PRODUCT_CONTRACT_EDIT}
                    ),
                    @PermissionMapping(
                            context = SERVICE_CONTRACTS,
                            permissions = {
                                    SERVICE_CONTRACT_CREATE,
                                    SERVICE_CONTRACT_EDIT,
                                    SERVICE_CONTRACT_EDIT_LOCKED,
                                    SERVICE_CONTRACT_EDIT_READY,
                                    SERVICE_CONTRACT_EDIT_DRAFT,
                                    SERVICE_CONTRACT_EDIT_STATUSES
                            }
                    ),
                    @PermissionMapping(
                            context = GOODS_ORDERS,
                            permissions = {
                                    GOODS_ORDER_CREATE,
                                    GOODS_ORDER_EDIT_CONFIRMED,
                                    GOODS_ORDER_EDIT_LOCKED,
                                    GOODS_ORDER_EDIT_REQUESTED,
                                    GOODS_ORDER_EDIT_STATUS
                            }
                    ),
                    @PermissionMapping(
                            context = SERVICE_ORDERS,
                            permissions = {
                                    SERVICE_ORDER_CREATE,
                                    SERVICE_ORDER_EDIT_CONFIRMED,
                                    SERVICE_ORDER_EDIT_LOCKED,
                                    SERVICE_ORDER_EDIT_REQUESTED,
                                    SERVICE_ORDER_EDIT_STATUSES
                            }
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering external intermediary nomenclature with request: {}", request.toString());
        return externalIntermediaryRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Changes the ordering of a {@link ExternalIntermediary} item in the ExternalIntermediary list to a specified position.
     * The method retrieves the ExternalIntermediary item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the ExternalIntermediary item and the new ordering ID
     * @throws DomainEntityNotFoundException  if no {@link ExternalIntermediary} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = EXTERNAL_INTERMEDIARIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of ExternalIntermediary item with ID: {} in ExternalIntermediaries to place: {}", request.getId(), request.getOrderingId());

        ExternalIntermediary externalIntermediary = externalIntermediaryRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ExternalIntermediary not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<ExternalIntermediary> externalIntermediaries;

        if (externalIntermediary.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = externalIntermediary.getOrderingId();

            externalIntermediaries = externalIntermediaryRepository.findInOrderingIdRange(
                    start,
                    end,
                    externalIntermediary.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (ExternalIntermediary c : externalIntermediaries) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = externalIntermediary.getOrderingId();
            end = request.getOrderingId();

            externalIntermediaries = externalIntermediaryRepository.findInOrderingIdRange(
                    start,
                    end,
                    externalIntermediary.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (ExternalIntermediary c : externalIntermediaries) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        externalIntermediary.setOrderingId(request.getOrderingId());
        externalIntermediaries.add(externalIntermediary);
        externalIntermediaryRepository.saveAll(externalIntermediaries);
    }

    /**
     * Sorts all {@link ExternalIntermediary} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = EXTERNAL_INTERMEDIARIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the ExternalIntermediaries alphabetically");
        List<ExternalIntermediary> externalIntermediaries = externalIntermediaryRepository.orderByName();
        long orderingId = 1;

        for (ExternalIntermediary c : externalIntermediaries) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        externalIntermediaryRepository.saveAll(externalIntermediaries);
    }

    /**
     * Deletes {@link ExternalIntermediary} if the validations are passed.
     *
     * @param id ID of the {@link ExternalIntermediary}
     * @throws DomainEntityNotFoundException if {@link ExternalIntermediary} is not found.
     * @throws OperationNotAllowedException if the {@link ExternalIntermediary} is already deleted.
     * @throws OperationNotAllowedException if the {@link ExternalIntermediary} has active or inactive children.
     * @throws OperationNotAllowedException if the {@link ExternalIntermediary} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = EXTERNAL_INTERMEDIARIES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing ExternalIntermediary with ID: {}", id);
        ExternalIntermediary externalIntermediary = externalIntermediaryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ExternalIntermediary not found, ID: " + id));

        if (externalIntermediary.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("status-Item is already deleted.");
        }

        if (externalIntermediaryRepository.hasActiveConnectionToProductContract(id)) {
            log.error("You cannot delete external intermediary because it is connected to a product contract;");
            throw new OperationNotAllowedException("You cannot delete external intermediary because it is connected to a product contract;");
        }

        if (externalIntermediaryRepository.hasActiveConnectionToServiceContract(id)) {
            log.error("You cannot delete external intermediary because it is connected to a service contract;");
            throw new OperationNotAllowedException("You cannot delete external intermediary because it is connected to a service contract;");
        }

        if (externalIntermediaryRepository.hasActiveConnectionToServiceOrder(id)) {
            log.error("You cannot delete external intermediary because it is connected to a service order;");
            throw new OperationNotAllowedException("You cannot delete external intermediary because it is connected to a service order;");
        }

        if (externalIntermediaryRepository.hasActiveConnectionToGoodsOrder(id)) {
            log.error("You cannot delete external intermediary because it is connected to a goods order;");
            throw new OperationNotAllowedException("You cannot delete external intermediary because it is connected to a goods order;");
        }

        externalIntermediary.setStatus(DELETED);
        externalIntermediaryRepository.save(externalIntermediary);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return externalIntermediaryRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return externalIntermediaryRepository.findByIdIn(ids);
    }

    /**
     * Retrieves detailed information about {@link ExternalIntermediary} by ID
     *
     * @param id ID of {@link ExternalIntermediary}
     * @return {@link ExternalIntermediaryResponse}
     * @throws DomainEntityNotFoundException if no {@link ExternalIntermediary} was found with the provided ID.
     */
    public ExternalIntermediaryResponse view(Long id) {
        log.debug("Fetching ExternalIntermediary with ID: {}", id);
        ExternalIntermediary externalIntermediary = externalIntermediaryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-ExternalIntermediary not found, ID: " + id));
        return new ExternalIntermediaryResponse(externalIntermediary);
    }

    /**
     * Adds {@link ExternalIntermediary} at the end with the highest ordering ID.
     * If the request asks to save {@link ExternalIntermediary} as a default and a default {@link ExternalIntermediary} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ExternalIntermediaryRequest}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @return {@link ExternalIntermediaryResponse}
     */
    @Transactional
    public ExternalIntermediaryResponse add(ExternalIntermediaryRequest request) {
        log.debug("Adding ExternalIntermediary: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (externalIntermediaryRepository.countExternalIntermediaryByStatusAndIdentifier(request.getIdentifier(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("identifier-ExternalIntermediary with the same identifier already exists;");
            throw new OperationNotAllowedException("identifier-ExternalIntermediary with the same identifier already exists;");
        }

        Long lastSortOrder = externalIntermediaryRepository.findLastOrderingId();
        ExternalIntermediary externalIntermediary = new ExternalIntermediary(request);
        externalIntermediary.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<ExternalIntermediary> currentDefaultExternalIntermediaryOptional = externalIntermediaryRepository.findByDefaultSelectionTrue();
            if (currentDefaultExternalIntermediaryOptional.isPresent()) {
                ExternalIntermediary currentDefaultExternalIntermediary = currentDefaultExternalIntermediaryOptional.get();
                currentDefaultExternalIntermediary.setDefaultSelection(false);
                externalIntermediaryRepository.save(currentDefaultExternalIntermediary);
            }
        }
        ExternalIntermediary externalIntermediaryEntity = externalIntermediaryRepository.save(externalIntermediary);
        return new ExternalIntermediaryResponse(externalIntermediaryEntity);
    }


    /**
     * Edits the {@link ExternalIntermediary}.
     * If the request asks to save {@link ExternalIntermediary} as a default and a default {@link ExternalIntermediary} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id ID of {@link ExternalIntermediary}
     * @param request {@link ExternalIntermediaryRequest}
     * @throws DomainEntityNotFoundException if {@link ExternalIntermediary} is not found.
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException if the {@link ExternalIntermediary} is deleted.
     * @return {@link ExternalIntermediaryResponse}
     */
    @Transactional
    public ExternalIntermediaryResponse edit(Long id, ExternalIntermediaryRequest request) {
        log.debug("Editing ExternalIntermediary: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        ExternalIntermediary externalIntermediary = externalIntermediaryRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (externalIntermediaryRepository.countExternalIntermediaryByStatusAndIdentifier(request.getIdentifier(), List.of(ACTIVE, INACTIVE)) > 0
                && !externalIntermediary.getIdentifier().equals(request.getIdentifier().trim())) {
            log.error("identifier-ExternalIntermediary with the same identifier already exists;");
            throw new OperationNotAllowedException("identifier-ExternalIntermediary with the same identifier already exists;");
        }

        if (externalIntermediary.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !externalIntermediary.isDefaultSelection()) {
            Optional<ExternalIntermediary> currentDefaultExternalIntermediaryOptional = externalIntermediaryRepository.findByDefaultSelectionTrue();
            if (currentDefaultExternalIntermediaryOptional.isPresent()) {
                ExternalIntermediary currentDefaultExternalIntermediary = currentDefaultExternalIntermediaryOptional.get();
                currentDefaultExternalIntermediary.setDefaultSelection(false);
                externalIntermediaryRepository.save(currentDefaultExternalIntermediary);
            }
        }
        externalIntermediary.setDefaultSelection(request.getDefaultSelection());

        externalIntermediary.setName(request.getName().trim());
        externalIntermediary.setIdentifier(request.getIdentifier().trim());
        externalIntermediary.setStatus(request.getStatus());
        return new ExternalIntermediaryResponse(externalIntermediaryRepository.save(externalIntermediary));
    }

    /**
     * Filters the list of ExternalIntermediaries based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ExternalIntermediary}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of ExternalIntermediaryResponse objects containing the filtered list of ExternalIntermediaries.
     */
    public Page<ExternalIntermediaryResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering ExternalIntermediary list with request: {}", request.toString());
        Page<ExternalIntermediary> page = externalIntermediaryRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ExternalIntermediaryResponse::new);
    }
}
