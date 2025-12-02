package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.AdditionalCondition;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.AdditionalConditionRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.AdditionalConditionResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CustomerAssessmentTypeShortResponse;
import bg.energo.phoenix.repository.nomenclature.receivable.AdditionalConditionRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.CustomerAssessmentTypeRepository;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.ADDITIONAL_CONDITIONS;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.ADDITIONAL_CONDITION;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdditionalConditionService implements NomenclatureBaseService {

    private final AdditionalConditionRepository additionalConditionRepository;
    private final CustomerAssessmentTypeRepository customerAssessmentTypeRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return ADDITIONAL_CONDITIONS;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = ADDITIONAL_CONDITION,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering additional conditions with statuses: {}", request);
        Page<AdditionalCondition> additionalConditions = additionalConditionRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return additionalConditions.map(this::nomenclatureResponseFromEntity);
    }

    public Page<AdditionalConditionResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering additional conditions list with request: {}", request);
        Page<AdditionalCondition> additionalConditions = additionalConditionRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return additionalConditions.map(this::responseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = ADDITIONAL_CONDITION,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in additional condition", request.getId());

        AdditionalCondition additionalCondition = additionalConditionRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-additional condition not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<AdditionalCondition> additionalConditions;

        if (additionalCondition.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = additionalCondition.getOrderingId();
            additionalConditions = additionalConditionRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            additionalCondition.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (AdditionalCondition cp : additionalConditions) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = additionalCondition.getOrderingId();
            end = request.getOrderingId();
            additionalConditions = additionalConditionRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            additionalCondition.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (AdditionalCondition cp : additionalConditions) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        additionalCondition.setOrderingId(request.getOrderingId());
        additionalConditions.add(additionalCondition);
        additionalConditionRepository.saveAll(additionalConditions);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = ADDITIONAL_CONDITION,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the additional condition alphabetically");

        List<AdditionalCondition> additionalConditions = additionalConditionRepository.orderByName();
        long orderingId = 1;

        for (AdditionalCondition additionalCondition : additionalConditions) {
            additionalCondition.setOrderingId(orderingId);
            orderingId++;
        }

        additionalConditionRepository.saveAll(additionalConditions);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = ADDITIONAL_CONDITION,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing additional condition with ID: {}", id);

        AdditionalCondition additionalCondition = additionalConditionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-additional condition not found, ID: " + id));

        if (additionalCondition.getStatus().equals(DELETED)) {
            log.error("additional condition {} is already deleted", id);
            throw new OperationNotAllowedException("id-additional condition " + id + " is already deleted");
        }
        if(additionalConditionRepository.isConnectedToCustomerAssessment(id)) {
            log.error("Can't delete the nomenclature because it is connected to Customer Assessment");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to Customer Assessment");
        }
        additionalCondition.setStatus(DELETED);
        additionalConditionRepository.save(additionalCondition);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return additionalConditionRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return additionalConditionRepository.findByIdIn(ids);
    }

    @Transactional
    public AdditionalConditionResponse add(AdditionalConditionRequest request) {
        log.debug("Adding additional condition: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (additionalConditionRepository.countAdditionalConditionByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("additional condition with such name already exists");
            throw new ClientException("additional condition with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = additionalConditionRepository.findLastOrderingId();
        AdditionalCondition additionalCondition = entityFromRequest(request);
        additionalCondition.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, additionalCondition);

        additionalConditionRepository.save(additionalCondition);
        return responseFromEntity(additionalCondition);
    }

    public AdditionalConditionResponse view(Long id) {
        log.debug("Fetching additional condition with ID: {}", id);
        AdditionalCondition additionalCondition = additionalConditionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-additional condition not found, ID: " + id));

        return responseFromEntity(additionalCondition);
    }

    @Transactional
    public AdditionalConditionResponse edit(Long id, AdditionalConditionRequest request) {
        log.debug("Editing additional condition: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        AdditionalCondition additionalCondition = additionalConditionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-additional condition not found, ID: " + id));

        if (additionalCondition.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (additionalConditionRepository.countAdditionalConditionByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
                && !additionalCondition.getName().equals(request.getName().trim())) {
            log.error("additional condition with such name already exists");
            throw new ClientException("additional condition with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, additionalCondition);

        additionalCondition.setName(request.getName().trim());
        additionalCondition.setStatus(request.getStatus());

        return responseFromEntity(additionalCondition);
    }

    private void assignDefaultSelectionWhenAdding(AdditionalConditionRequest request, AdditionalCondition additionalCondition) {
        if (request.getStatus().equals(INACTIVE)) {
            additionalCondition.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<AdditionalCondition> currentDefaultAdditionalConditionOptional = additionalConditionRepository.findByDefaultSelectionTrue();
                if (currentDefaultAdditionalConditionOptional.isPresent()) {
                    AdditionalCondition currentDefaulAdditionalCondition = currentDefaultAdditionalConditionOptional.get();
                    currentDefaulAdditionalCondition.setDefaultSelection(false);
                    additionalConditionRepository.save(currentDefaulAdditionalCondition);
                }
            }
            additionalCondition.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(AdditionalConditionRequest request, AdditionalCondition additionalCondition) {
        if (request.getStatus().equals(INACTIVE)) {
            additionalCondition.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!additionalCondition.isDefaultSelection()) {
                    Optional<AdditionalCondition> optionalAdditionalCondition = additionalConditionRepository.findByDefaultSelectionTrue();
                    if (optionalAdditionalCondition.isPresent()) {
                        AdditionalCondition currentAdditionalCondition = optionalAdditionalCondition.get();
                        currentAdditionalCondition.setDefaultSelection(false);
                        additionalConditionRepository.save(currentAdditionalCondition);
                    }
                }
            }
            additionalCondition.setDefaultSelection(request.getDefaultSelection());
        }
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(AdditionalCondition additionalCondition) {
        return new NomenclatureResponse(
                additionalCondition.getId(),
                additionalCondition.getName(),
                additionalCondition.getOrderingId(),
                additionalCondition.isDefaultSelection(),
                additionalCondition.getStatus()
        );
    }

    public AdditionalConditionResponse responseFromEntity(AdditionalCondition additionalCondition) {
        return new AdditionalConditionResponse(
                additionalCondition.getId(),
                additionalCondition.getName(),
                additionalCondition.getOrderingId(),
                additionalCondition.isDefaultSelection(),
                additionalCondition.getStatus(),
                getCustomerAssessmentTypeShortResponse(additionalCondition.getCustomerAssessmentTypeId())
        );
    }

    private CustomerAssessmentTypeShortResponse getCustomerAssessmentTypeShortResponse(Long customerAssessmentType) {
        return customerAssessmentTypeRepository
                .findByIdAndStatusIn(customerAssessmentType, List.of(ACTIVE))
                .map(CustomerAssessmentTypeShortResponse::new)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer assessment type with id %s not found".formatted(customerAssessmentType)));
    }

    private Long checkAndGetCustomerAssessmentTypeId(Long customerAssessmentType) {
        return customerAssessmentTypeRepository
                .findByIdAndStatusIn(customerAssessmentType, List.of(ACTIVE))
                .map(CustomerAssessmentType::getId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer assessment type with id %s not found".formatted(customerAssessmentType)));
    }

    public AdditionalCondition entityFromRequest(AdditionalConditionRequest request) {
        return AdditionalCondition
                .builder()
                .name(request.getName().trim())
                .customerAssessmentTypeId(checkAndGetCustomerAssessmentTypeId(request.getCustomerAssessmentTypeId()))
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }

}
