package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.DeactivationPurpose;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.DeactivationPurposeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.DeactivationPurposeResponse;
import bg.energo.phoenix.repository.nomenclature.contract.DeactivationPurposeRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.DEACTIVATION_PURPOSE;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeactivationPurposeService implements NomenclatureBaseService {

    private final DeactivationPurposeRepository deactivationPurposeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.DEACTIVATION_PURPOSE;
    }

    /**
     * Filters {@link DeactivationPurpose} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link DeactivationPurpose}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<DeactivationPurposeResponse> Page&lt;DeactivationPurposeResponse&gt;} containing detailed information
     */
    public Page<DeactivationPurposeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering DeactivationPurpose list with request: {}", request.toString());
        Page<DeactivationPurpose> page = deactivationPurposeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(DeactivationPurposeResponse::new);
    }

    /**
     * Filters {@link DeactivationPurpose} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link DeactivationPurpose}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator( //TODO permissions should be added
            permissions = {
                    @PermissionMapping(context = DEACTIVATION_PURPOSE, permissions = {NOMENCLATURE_VIEW}),
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return deactivationPurposeRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds {@link DeactivationPurpose} at the end with the highest ordering ID.
     * If the request asks to save {@link DeactivationPurpose} as a default and a default {@link DeactivationPurpose} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     * function also checks if request name is unique and if not returns exception
     *
     * @param request {@link DeactivationPurpose}
     * @return {@link DeactivationPurposeResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public DeactivationPurposeResponse add(DeactivationPurposeRequest request) {

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Integer count = getExistingRecordsCountByName(request.getName());
        if (count > 0) {
            log.error("DeactivationPurpose Name is not unique");
            throw new ClientException("name-DeactivationPurpose Name is not unique", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Long lastSortOrder = deactivationPurposeRepository.findLastOrderingId();
        DeactivationPurpose deactivationPurpose = new DeactivationPurpose(request);
        deactivationPurpose.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), deactivationPurpose);
        deactivationPurpose.setIsHardCoded(false);
        DeactivationPurpose purpose = deactivationPurposeRepository.save(deactivationPurpose);
        return new DeactivationPurposeResponse(purpose);
    }

    /**
     * Retrieves detailed information about {@link DeactivationPurpose} by ID
     *
     * @param id ID of {@link DeactivationPurpose}
     * @return {@link DeactivationPurposeResponse}
     * @throws DomainEntityNotFoundException if no {@link DeactivationPurpose} was found with the provided ID.
     */
    public DeactivationPurposeResponse view(Long id) {
        log.debug("Fetching deactivation purpose with ID: {}", id);
        DeactivationPurpose deactivationPurpose = deactivationPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));
        return new DeactivationPurposeResponse(deactivationPurpose);
    }

    /**
     * Edit the requested {@link DeactivationPurpose}.
     * If the request asks to save {@link DeactivationPurpose} as a default and a default {@link DeactivationPurpose} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link DeactivationPurpose}
     * @param request {@link DeactivationPurposeRequest}
     * @return {@link DeactivationPurposeResponse}
     * @throws DomainEntityNotFoundException if {@link DeactivationPurpose} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link DeactivationPurpose} is deleted.
     */
    @Transactional
    public DeactivationPurposeResponse edit(Long id, DeactivationPurposeRequest request) {
        log.debug("Editing DeactivationPurpose: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        DeactivationPurpose deactivationPurpose = deactivationPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));
        if (deactivationPurpose.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }
        if (deactivationPurpose.getIsHardCoded()) {
            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be changed.;");
        }
        if (!deactivationPurpose.getName().equals(request.getName())) {
            if (getExistingRecordsCountByName(request.getName()) > 0) {
                log.error("DeactivationPurpose Name is not unique");
                throw new ClientException("name-DeactivationPurpose Name is not unique", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), deactivationPurpose);
        deactivationPurpose.setName(request.getName());
        deactivationPurpose.setStatus(request.getStatus());
        return new DeactivationPurposeResponse(deactivationPurposeRepository.save(deactivationPurpose));
    }

    /**
     * AssignDefaultSelection
     *
     * @param status
     * @param defaultSelection
     * @param deactivationPurpose
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean defaultSelection, DeactivationPurpose deactivationPurpose) {
        if (status.equals(INACTIVE)) {
            deactivationPurpose.setIsDefault(false);
        } else {
            if (defaultSelection) {
                Optional<DeactivationPurpose> currentDefaultDeactivationPurposeOptional = deactivationPurposeRepository.findByIsDefaultTrue();
                if (currentDefaultDeactivationPurposeOptional.isPresent()) {
                    DeactivationPurpose currentDefaultDeactivationModel = currentDefaultDeactivationPurposeOptional.get();
                    currentDefaultDeactivationModel.setIsDefault(false);
                    deactivationPurposeRepository.save(currentDefaultDeactivationModel);
                }
                deactivationPurpose.setIsDefault(true);
            } else {
                deactivationPurpose.setIsDefault(false);
            }
        }
    }

    /**
     * <h1>Check Name For Uniqueness</h1>
     * function returns count of name in database, if name is > 0 it means that it's not unique
     *
     * @param name
     * @return Integer count of name
     */
    private Integer getExistingRecordsCountByName(String name) {
        return deactivationPurposeRepository.getExistingRecordsCountByName(name.toLowerCase(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
    }

    /**
     * Changes the ordering of a {@link DeactivationPurpose} item in the DeactivationPurpose list to a specified position.
     * The method retrieves the {@link DeactivationPurpose} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link DeactivationPurpose} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link DeactivationPurpose} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator( //TODO add permissions
            permissions = {
                    @PermissionMapping(context = DEACTIVATION_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in DeactivationPurpose to top", request.getId());

        DeactivationPurpose deactivationPurpose = deactivationPurposeRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<DeactivationPurpose> deactivationPurposeList;

        if (deactivationPurpose.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = deactivationPurpose.getOrderingId();
            deactivationPurposeList = deactivationPurposeRepository.findInOrderingIdRange(
                    start,
                    end,
                    deactivationPurpose.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (DeactivationPurpose p : deactivationPurposeList) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = deactivationPurpose.getOrderingId();
            end = request.getOrderingId();
            deactivationPurposeList = deactivationPurposeRepository.findInOrderingIdRange(
                    start,
                    end,
                    deactivationPurpose.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (DeactivationPurpose p : deactivationPurposeList) {
                p.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        deactivationPurpose.setOrderingId(request.getOrderingId());
        deactivationPurposeRepository.save(deactivationPurpose);
        deactivationPurposeRepository.saveAll(deactivationPurposeList);
    }

    @Override
    @Transactional
    @PermissionValidator( //TODO add permissions
            permissions = {
                    @PermissionMapping(context = DEACTIVATION_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the DeactivationPurpose alphabetically");
        List<DeactivationPurpose> deactivationPurposeList = deactivationPurposeRepository.orderByName();
        long orderingId = 1;

        for (DeactivationPurpose p : deactivationPurposeList) {
            p.setOrderingId(orderingId);
            orderingId++;
        }

        deactivationPurposeRepository.saveAll(deactivationPurposeList);
    }

    /**
     * Deletes {@link DeactivationPurpose} if the validations are passed.
     *
     * @param id ID of the {@link DeactivationPurpose}
     * @throws DomainEntityNotFoundException if {@link DeactivationPurpose} is not found.
     * @throws OperationNotAllowedException  if the {@link DeactivationPurpose} is already deleted.
     * @throws OperationNotAllowedException  if the {@link DeactivationPurpose} is connected to active object.
     */
    @Override
    @Transactional
    @PermissionValidator(  //TODO add permissions
            permissions = {
                    @PermissionMapping(context = DEACTIVATION_PURPOSE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing DeactivationPurpose with ID: {}", id);
        DeactivationPurpose deactivationPurpose = deactivationPurposeRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        if (deactivationPurpose.getIsHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }

        if (deactivationPurpose.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        if (deactivationPurposeRepository.hasActiveConnectionsToPointOfDeliveries(id)) {
            log.error("Can't delete the nomenclature because it is connected to another object in the system");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
        }

        deactivationPurpose.setStatus(DELETED);
        deactivationPurposeRepository.save(deactivationPurpose);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return deactivationPurposeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return deactivationPurposeRepository.findByIdIn(ids);
    }
}
