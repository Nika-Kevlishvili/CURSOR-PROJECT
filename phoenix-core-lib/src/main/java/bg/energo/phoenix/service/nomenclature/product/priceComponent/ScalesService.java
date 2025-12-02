package bg.energo.phoenix.service.nomenclature.product.priceComponent;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.product.ScalesFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.ScalesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.ScalesResponse;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingDataByScaleRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.*;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SCALES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScalesService implements NomenclatureBaseService {

    private final ScalesRepository scalesRepository;

    private final GridOperatorRepository gridOperatorRepository;

    private final BillingDataByScaleRepository billingDataByScaleRepository;


    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.SCALES;
    }

    /**
     * Changes the ordering of a {@link Scales} item in the Scales list to a specified position.
     * The method retrieves the {@link Scales} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Scales} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Scales} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SCALES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        Scales scale = scalesRepository
                .findByIdAndStatuses(request.getId(), List.of(ACTIVE, INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Scale with presented id not found"));

        Long start;
        Long end;
        List<Scales> scales;

        if (scale.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = scale.getOrderingId();
            scales = scalesRepository.findInOrderingIdRange(start, end, scale.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Scales s : scales) {
                s.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = scale.getOrderingId();
            end = request.getOrderingId();
            scales = scalesRepository.findInOrderingIdRange(start, end, scale.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Scales s : scales) {
                s.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        scale.setOrderingId(request.getOrderingId());
        scales.add(scale);
        scalesRepository.saveAll(scales);
    }

    /**
     * Sorts all {@link Scales} alphabetically.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SCALES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Scales alphabetically");
        List<Scales> scales = scalesRepository.orderByName();
        long orderingId = 1;

        for (Scales s : scales) {
            s.setOrderingId(orderingId);
            orderingId++;
        }
        scalesRepository.saveAll(scales);
    }

    /**
     * Deletes {@link Scales}
     *
     * @param id ID of the {@link Scales}
     * @throws DomainEntityNotFoundException if {@link Scales} is not found.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SCALES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Scales with ID: {}", id);
        Scales scales = scalesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Scale not found"));
        if (scales.getStatus().equals(DELETED)) {
            throw new OperationNotAllowedException("id-Scale with presented id is already deleted");
        } else {
            Long connectionCount = scalesRepository.activeConnectionCount(id, List.of(PodSubObjectStatus.ACTIVE));
            if (connectionCount > 0) {
                throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to another object in the system");
            }

            scales.setStatus(DELETED);
            scales.setDefaultSelection(false);
            scalesRepository.save(scales);
        }
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return scalesRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return scalesRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link Scales} at the end with the highest ordering ID.
     * If the request asks to save {@link Scales} as a default and a default {@link Scales} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ScalesRequest}
     * @return {@link ScalesResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ScalesResponse add(ScalesRequest request) {
        request.setName(request.getName().trim());
        request.setScaleType(request.getScaleType().trim());
        request.setScaleCode(request.getScaleCode() == null ? null : request.getScaleCode().trim());
        request.setTariffOrScale(request.getTariffOrScale() == null ? null : request.getTariffOrScale().trim());

        log.debug("Adding Scales: {}", request);
        if (request.getScaleCode() == null && request.getTariffOrScale() == null) {
            throw new ClientException("scaleCode-ScaleCode or TariffScale shouldn't be empty;tariffOrScale-ScaleCode or TariffScale shouldn't be empty;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        String tariffOrScale = request.getTariffOrScale();
        String scaleCode = request.getScaleCode();
        Long gridOperatorId = request.getGridOperatorId();

        if (StringUtils.isNotEmpty(tariffOrScale) && StringUtils.isNotEmpty(scaleCode)) {
            throw new ClientException("scaleCode-ScaleCode and TariffScale shouldn’t be filled both under one nomenclature record;" +
                    "tariffOrScale-ScaleCode and TariffScale shouldn’t be filled both under one nomenclature record;", CONFLICT);
        }
        if (StringUtils.isNotEmpty(scaleCode)) {
            if (checkScaleCodeForUniqueness(scaleCode, gridOperatorId)) {
                throw new ClientException("scaleCode-ScaleCode there are nomenclature with the same scaleCode;", CONFLICT);
            }
        }
        if (StringUtils.isNotEmpty(tariffOrScale)) {
            if (checkTariffScaleForUniqueness(tariffOrScale, gridOperatorId)) {
                throw new ClientException("tariffOrScale-TariffScale there are nomenclature with the same TariffScale;", CONFLICT);
            }
        }

        if (request.getScaleCode() != null) {
            Optional<Scales> dbScale =
                    scalesRepository.findByGridOperatorIdAndScaleCodeAndStatus(request.getGridOperatorId(), request.getScaleCode(), ACTIVE);
            if (dbScale.isPresent()) {
                throw new ClientException("scaleCode-[ScaleCode] Nomenclature with this ScaleCode and GridOperatorId already exists ;" +
                        "gridOperatorId-[GridOperatorId] Nomenclature with this GridOperatorId and ScaleCode already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        if (request.getTariffOrScale() != null) {
            Optional<Scales> dbScale =
                    scalesRepository.findByGridOperatorIdAndTariffScaleAndStatus(request.getGridOperatorId(), request.getTariffOrScale(), ACTIVE);
            if (dbScale.isPresent()) {
                throw new ClientException("tariffOrScale-[TariffOrScale] Nomenclature with this TariffOrScale and GridOperatorId already exists ;" +
                        "gridOperatorId-[GridOperatorId] Nomenclature with this GridOperatorId and TariffOrScale already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<Scales> scalesWithName = scalesRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (scalesWithName.size() > 0) {
            log.error("Cannot add item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot add item with name [%s], scales with same name already exists;", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GridOperator gridOperator =
                gridOperatorRepository
                        .findByIdAndStatus(request.getGridOperatorId(), List.of(ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Active Grid Operator with presented id not found;"));

        Long lastSortOrder = scalesRepository.findLastOrderingId();
        Scales scales = new Scales(request, gridOperator);
        scales.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, scales);
        Scales scalesEntity = scalesRepository.save(scales);
        return new ScalesResponse(scalesEntity);
    }

    private boolean checkScaleCodeForUniqueness(String scaleCode, Long gridOperatorId) {
        return scalesRepository.existsByScaleCodeAndGridOperatorId(scaleCode, gridOperatorId);
    }

    private boolean checkTariffScaleForUniqueness(String tariffScale, Long gridOperatorId) {
        return scalesRepository.existsByTariffScaleAndGridOperatorId(tariffScale, gridOperatorId);
    }

    private void checkCurrentDefaultSelection(ScalesRequest request, Scales scales) {
        if (request.getStatus().equals(INACTIVE)) {
            scales.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<Scales> currentDefaultScaleOptional = scalesRepository.findByDefaultSelectionTrue();
                if (currentDefaultScaleOptional.isPresent()) {
                    Scales defaultScale = currentDefaultScaleOptional.get();
                    defaultScale.setDefaultSelection(false);
                    scalesRepository.save(defaultScale);
                }
            }
            scales.setDefaultSelection(request.getDefaultSelection());
        }
    }

    /**
     * Edit the requested {@link Scales}.
     * If the request asks to save {@link Scales} as a default and a default {@link Scales} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Scales}
     * @param request {@link ScalesRequest}
     * @return {@link ScalesResponse}
     * @throws DomainEntityNotFoundException if {@link Scales} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Scales} is deleted.
     */
    @Transactional
    public ScalesResponse edit(Long id, ScalesRequest request) {
        request.setName(request.getName().trim());
        log.debug("Editing Scales: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Scales scales = scalesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Scales with presented id not found"));

        if (scales.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (billingDataByScaleRepository.existsByScaleId(id)) {
            String scaleCode = scales.getScaleCode();
            String scaleTariff = scales.getTariffScale();
            if (StringUtils.isNotEmpty(scaleCode)) {
                if (StringUtils.isNotEmpty(request.getTariffOrScale())) {
                    throw new ClientException("tariffOrScale- you can't edit ScaleCode value with TariffScale when scale is bound to the billingByScales;", CONFLICT);
                }
            }
            if (StringUtils.isNotEmpty(scaleTariff)) {
                if (StringUtils.isNotEmpty(request.getScaleCode())) {
                    throw new ClientException("scaleCode- you can't edit ScaleCode value with TariffScale when scale is bound to the billingByScales;", CONFLICT);
                }
            }
        }

        String scaleCode = request.getScaleCode();
        String tariffOrScale = request.getTariffOrScale();
        Long gridOperatorId = request.getGridOperatorId();
        if (StringUtils.isNotEmpty(scaleCode)) {
            if (scalesRepository.existsByScaleCodeAndGridOperatorIdAndIdNotIn(scaleCode, gridOperatorId, List.of(scales.getId()))) {
                throw new ClientException("scaleCode-ScaleCode there are nomenclature with the same scaleCode;", CONFLICT);
            }
        }
        if (StringUtils.isNotEmpty(tariffOrScale)) {
            if (scalesRepository.existsByTariffScaleAndGridOperatorIdAndIdNotIn(tariffOrScale, gridOperatorId, List.of(scales.getId()))) {
                throw new ClientException("tariffOrScale-TariffScale there are nomenclature with the same TariffScale;", CONFLICT);
            }
        }

        if (request.getScaleCode() == null && request.getTariffOrScale() == null) {
            throw new ClientException("scaleCode-ScaleCode or TariffScale shouldn't be empty;tariffOrScale-ScaleCode or TariffScale shouldn't be empty;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (request.getScaleCode() != null) {
            Optional<Scales> dbScale =
                    scalesRepository.findByGridOperatorIdAndScaleCodeAndStatusAndIdIsNot(request.getGridOperatorId(), request.getScaleCode(), ACTIVE, scales.getId());
            if (dbScale.isPresent()) {
                throw new ClientException("scaleCode-[ScaleCode] Nomenclature with this ScaleCode and GridOperatorId already exists ;" +
                        "gridOperatorId-[GridOperatorId] Nomenclature with this GridOperatorId and ScaleCode already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        if (request.getTariffOrScale() != null) {
            Optional<Scales> dbScale =
                    scalesRepository.findByGridOperatorIdAndTariffScaleAndStatusAndIdIsNot(request.getGridOperatorId(), request.getTariffOrScale(), ACTIVE, scales.getId());
            if (dbScale.isPresent()) {
                throw new ClientException("tariffOrScale-[TariffOrScale] Nomenclature with this TariffOrScale and GridOperatorId already exists ;" +
                        "gridOperatorId-[GridOperatorId] Nomenclature with this GridOperatorId and TariffOrScale already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        List<Scales> scalesWithName = scalesRepository.findByNameAndStatuses(request.getName(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (scalesWithName.size() > 0 && scales.getId() != id) {
            log.error("Cannot edit item with name {}", request.getName());
            throw new ClientException(String.format("name-Cannot edit item with name [%s], scales with same name already exists", request.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        GridOperator gridOperator =
                gridOperatorRepository
                        .findByIdAndStatus(request.getGridOperatorId(), List.of(ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Active Grid Operator with presented id not found;"));

        checkCurrentDefaultSelection(request, scales);

        scales.setName(request.getName());
        scales.setGridOperator(gridOperator);
        scales.setScaleType(request.getScaleType());
        scales.setScaleCode(request.getScaleCode());
        scales.setTariffScale(request.getTariffOrScale());
        scales.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            scales.setDefaultSelection(false);
        }
        scales.setCalculationForNumberOfDays(Objects.requireNonNullElse(request.getCalculationForNumberOfDays(), false));
        scales.setScaleForActiveElectricity(Objects.requireNonNullElse(request.getScaleForActiveElectricity(), false));
        return new ScalesResponse(scalesRepository.save(scales));
    }

    /**
     * Retrieves detailed information about {@link Scales} by ID
     *
     * @param id ID of {@link Scales}
     * @return {@link ScalesResponse}
     * @throws DomainEntityNotFoundException if no {@link Scales} was found with the provided ID.
     */
    public ScalesResponse view(Long id) {
        log.debug("Fetching Scales with ID: {}", id);
        Scales scales = scalesRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-Scales with presented id not found", DOMAIN_ENTITY_NOT_FOUND));
        return new ScalesResponse(scales);
    }

    /**
     * Filters {@link Scales} against the provided {@link ScalesFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Scales}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<ScalesResponse> Page&lt;ScalesResponse&gt;} containing detailed information
     */
    public Page<ScalesResponse> filter(ScalesFilterRequest request) {
        log.debug("Filtering Scales list with request: {}", request.toString());
        Page<Scales> page = scalesRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getGridOperatorId(),
                        request.getExcludedItemId(),
                        request.getIncludedItemIds(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(ScalesResponse::new);
    }

    /**
     * Filters {@link Scales} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Scales}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = SCALES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Scales list with statuses: {}", request);
        return scalesRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }
}
