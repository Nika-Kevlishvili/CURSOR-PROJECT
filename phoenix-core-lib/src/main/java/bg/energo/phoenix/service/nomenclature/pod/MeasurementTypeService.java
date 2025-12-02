package bg.energo.phoenix.service.nomenclature.pod;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.MeasurementTypeRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.MeasurementTypeResponse;
import bg.energo.phoenix.repository.nomenclature.pod.MeasurementTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.EXPRESS_CONTRACT;
import static bg.energo.phoenix.permissions.PermissionContextEnum.MEASUREMENT_TYPE;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementTypeService implements NomenclatureBaseService {

    private final MeasurementTypeRepository measurementTypeRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.MEASUREMENT_TYPE;
    }

    public Page<MeasurementTypeResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Measurement types list with request: {};", request.toString());
        Page<MeasurementType> page = measurementTypeRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        //return page.map(MeasurementTypeResponse::new);
        return page.map(mt -> new MeasurementTypeResponse(mt, getGridOperatorName(mt.getGridOperatorId())));

    }
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MEASUREMENT_TYPE, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering measurement type nomenclature with request: {};", request.toString());
        return measurementTypeRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    @Transactional
    public MeasurementTypeResponse add(MeasurementTypeRequest request) {
        log.debug("Adding measurement type: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED;");
            throw new ClientException("status-Cannot add item with status DELETED;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (measurementTypeRepository.countMeasurementTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Measurement type with the same name already exists;");
            throw new OperationNotAllowedException("name-Measurement type with the same name already exists;");
        }

        GridOperator gridOperator = gridOperatorRepository
                .findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Active grid operator with ID %s not found;".formatted(request.getGridOperatorId())));

        Long lastSortOrder = measurementTypeRepository.findLastOrderingId();
        MeasurementType measurementTypes = new MeasurementType(request);
        measurementTypes.setGridOperatorId(gridOperator.getId());
        measurementTypes.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), measurementTypes);
        MeasurementType measurementType = measurementTypeRepository.save(measurementTypes);
        return new MeasurementTypeResponse(measurementType,getGridOperatorName(measurementType.getGridOperatorId()));
    }

    public MeasurementTypeResponse view(Long id) {
        log.debug("Fetching Measurement type with ID: {};", id);
        MeasurementType measurementType = measurementTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Measurement type not found, ID: " + id));
        return new MeasurementTypeResponse(measurementType,getGridOperatorName(measurementType.getGridOperatorId()));
    }

    public String getGridOperatorName(Long gridOperatorId) {
        if(gridOperatorId != null){
            Optional<GridOperator> gridOperatorOptional = gridOperatorRepository.findByIdAndStatus(gridOperatorId, List.of(NomenclatureItemStatus.ACTIVE));
            return gridOperatorOptional.map(GridOperator::getName).orElse(null);
        } return null;
    }
    @Transactional
    public MeasurementTypeResponse edit(Long id, MeasurementTypeRequest request) {
        log.debug("Editing measurement type: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item;");
            throw new ClientException("status-Cannot set DELETED status to item;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        MeasurementType measurementType = measurementTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Measurement type with given id %s not found!;", id)));
        if (measurementType.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item;");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item;");
        }
        if (!measurementType.getName().equals(request.getName())) {
            if (measurementTypeRepository.countMeasurementTypeByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
                log.error("Measurement type with ID {} is not unique and cannot be modified", measurementType.getId());
                throw new ClientException("name-Measurement type with the same name already exists;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
        GridOperator gridOperator = gridOperatorRepository
                .findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Active grid operator with ID %s not found;".formatted(request.getGridOperatorId())));

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), measurementType);
        measurementType.setName(request.getName());
        measurementType.setStatus(request.getStatus());
        measurementType.setGridOperatorId(gridOperator.getId());
        return new MeasurementTypeResponse(measurementTypeRepository.save(measurementType),getGridOperatorName(measurementType.getGridOperatorId()));
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MEASUREMENT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of measurement type item with ID: {} in measurement types to place: {}", request.getId(), request.getOrderingId());

        MeasurementType measurementType = measurementTypeRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-measurement type not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<MeasurementType> measurementTypes;

        if (measurementType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = measurementType.getOrderingId();

            measurementTypes = measurementTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    measurementType.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (MeasurementType c : measurementTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = measurementType.getOrderingId();
            end = request.getOrderingId();

            measurementTypes = measurementTypeRepository.findInOrderingIdRange(
                    start,
                    end,
                    measurementType.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (MeasurementType c : measurementTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        measurementType.setOrderingId(request.getOrderingId());
        measurementTypes.add(measurementType);
        measurementTypeRepository.saveAll(measurementTypes);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MEASUREMENT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the measuring types alphabetically;");
        List<MeasurementType> measurementTypes = measurementTypeRepository.orderByName();
        long orderingId = 1;

        for (MeasurementType c : measurementTypes) {
            c.setOrderingId(orderingId);
            orderingId++;
        }

        measurementTypeRepository.saveAll(measurementTypes);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MEASUREMENT_TYPE, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing measurement type with ID: {}", id);
        MeasurementType measurementType = measurementTypeRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Measurement type not found, ID: " + id));

        if (measurementType.getStatus().equals(DELETED)) {
            log.error("Measurement type with ID {} is already deleted;", measurementType.getId());
            throw new OperationNotAllowedException("status-Item is already deleted;");
        }

        List<PointOfDeliveryDetails> podDetails = pointOfDeliveryDetailsRepository.findByPodMeasurementTypeId(measurementType.getId());
        ArrayList<PointOfDelivery> deletedPointOfDeliveries = new ArrayList<>();
        for (PointOfDeliveryDetails podDetail : podDetails) {
            PointOfDelivery byLastPodDetailIdAndStatus = pointOfDeliveryRepository.findByLastPodDetailIdAndStatus(podDetail.getId(), PodStatus.DELETED);
            if (byLastPodDetailIdAndStatus != null) {
                deletedPointOfDeliveries.add(byLastPodDetailIdAndStatus);
            }
        }
        if (!podDetails.isEmpty() && (podDetails.size() != deletedPointOfDeliveries.size())) {
            log.error("Measurement type With id {} is connected with another object and can't be deleted;", measurementType.getId());
            throw new DomainEntityNotFoundException("MeasurementType - Measurement type with id %s is connected with another object and can't be deleted;".formatted(measurementType.getId()));
        }

        measurementType.setStatus(DELETED);
        measurementTypeRepository.save(measurementType);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return measurementTypeRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return measurementTypeRepository.findByIdIn(ids);
    }

    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean defaultSelection, MeasurementType measurementType) {
        if (status.equals(INACTIVE)) {
            measurementType.setDefault(false);
        } else {
            if (defaultSelection) {
                Optional<MeasurementType> currentDefaultMeasurementTypeOptional = measurementTypeRepository.findByIsDefaultTrue();
                if (currentDefaultMeasurementTypeOptional.isPresent()) {
                    MeasurementType currentDefaultMeasurementType = currentDefaultMeasurementTypeOptional.get();
                    currentDefaultMeasurementType.setDefault(false);
                    measurementTypeRepository.save(currentDefaultMeasurementType);
                }
                measurementType.setDefault(true);
            } else {
                measurementType.setDefault(false);
            }
        }
    }
}
