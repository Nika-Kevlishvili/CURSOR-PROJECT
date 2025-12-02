package bg.energo.phoenix.service.nomenclature.billing;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.billing.RiskAssessment;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.billing.RiskAssessmentRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.billing.RiskAssessmentResponse;
import bg.energo.phoenix.repository.nomenclature.billing.RiskAssessmentRepository;
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
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.RISK_ASSESSMENTS;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.RISK_ASSESSMENT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService implements NomenclatureBaseService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return RISK_ASSESSMENTS;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RISK_ASSESSMENT, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering risk assessment with statuses: {}", request);
        Page<RiskAssessment> riskAssessments = riskAssessmentRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return riskAssessments.map(this::nomenclatureResponseFromEntity);
    }

    public Page<RiskAssessmentResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering risk assessments list with request: {}", request);
        Page<RiskAssessment> riskAssessments = riskAssessmentRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return riskAssessments.map(this::responseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RISK_ASSESSMENT, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in risk assessment", request.getId());

        RiskAssessment riskAssessment = riskAssessmentRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Risk assessment not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<RiskAssessment> riskAssessments;

        if (riskAssessment.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = riskAssessment.getOrderingId();
            riskAssessments = riskAssessmentRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            riskAssessment.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (RiskAssessment ra : riskAssessments) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = riskAssessment.getOrderingId();
            end = request.getOrderingId();
            riskAssessments = riskAssessmentRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            riskAssessment.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (RiskAssessment ra : riskAssessments) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        riskAssessment.setOrderingId(request.getOrderingId());
        riskAssessments.add(riskAssessment);
        riskAssessmentRepository.saveAll(riskAssessments);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RISK_ASSESSMENT, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the risk assessments alphabetically");

        List<RiskAssessment> riskAssessments = riskAssessmentRepository.orderByName();
        long orderingId = 1;

        for (RiskAssessment riskAssessment : riskAssessments) {
            riskAssessment.setOrderingId(orderingId);
            orderingId++;
        }

        riskAssessmentRepository.saveAll(riskAssessments);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = RISK_ASSESSMENT, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing risk assessment with ID: {}", id);

        RiskAssessment riskAssessment = riskAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Risk assessment not found, ID: " + id));

        if (riskAssessment.getStatus().equals(DELETED)) {
            log.error("Risk assessment {} is already deleted", id);
            throw new OperationNotAllowedException("id-Risk assessment " + id + " is already deleted");
        }

        riskAssessment.setStatus(DELETED);
        riskAssessmentRepository.save(riskAssessment);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return riskAssessmentRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return riskAssessmentRepository.findByIdIn(ids);
    }

    @Transactional
    public RiskAssessmentResponse add(RiskAssessmentRequest request) {
        log.debug("Adding risk assessment: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (riskAssessmentRepository.countRiskAssessmentByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Risk assessment with such name already exists");
            throw new ClientException("name-Assessment with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = riskAssessmentRepository.findLastOrderingId();
        RiskAssessment riskAssessment = entityFromRequest(request);
        riskAssessment.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, riskAssessment);

        riskAssessmentRepository.save(riskAssessment);
        return responseFromEntity(riskAssessment);
    }

    public RiskAssessmentResponse view(Long id) {
        log.debug("Fetching risk assessment with ID: {}", id);
        RiskAssessment riskAssessment = riskAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Risk assessment not found, ID: " + id));

        return responseFromEntity(riskAssessment);
    }

    @Transactional
    public RiskAssessmentResponse edit(Long id, RiskAssessmentRequest request) {
        log.debug("Editing risk assessment: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        RiskAssessment riskAssessment = riskAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Risk assessment not found, ID: " + id));

        if (riskAssessment.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (riskAssessmentRepository.countRiskAssessmentByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
            && !riskAssessment.getName().equals(request.getName().trim())) {
            log.error("Risk assessment with such name already exists");
            throw new ClientException("risk-Assessment with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, riskAssessment);

        riskAssessment.setName(request.getName().trim());
        riskAssessment.setStatus(request.getStatus());

        return responseFromEntity(riskAssessment);
    }

    private void assignDefaultSelectionWhenAdding(RiskAssessmentRequest request, RiskAssessment riskAssessment) {
        if (request.getStatus().equals(INACTIVE)) {
            riskAssessment.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<RiskAssessment> currentDefaultRiskAssessmentOptional = riskAssessmentRepository.findByDefaultSelectionTrue();
                if (currentDefaultRiskAssessmentOptional.isPresent()) {
                    RiskAssessment currentDefaultRiskAssessment = currentDefaultRiskAssessmentOptional.get();
                    currentDefaultRiskAssessment.setDefaultSelection(false);
                    riskAssessmentRepository.save(currentDefaultRiskAssessment);
                }
            }
            riskAssessment.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(RiskAssessmentRequest request, RiskAssessment riskAssessment) {
        if (request.getStatus().equals(INACTIVE)) {
            riskAssessment.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!riskAssessment.isDefaultSelection()) {
                    Optional<RiskAssessment> optionalRiskAssessment = riskAssessmentRepository.findByDefaultSelectionTrue();
                    if (optionalRiskAssessment.isPresent()) {
                        RiskAssessment currentRiskAssessment = optionalRiskAssessment.get();
                        currentRiskAssessment.setDefaultSelection(false);
                        riskAssessmentRepository.save(currentRiskAssessment);
                    }
                }
            }
            riskAssessment.setDefaultSelection(request.getDefaultSelection());
        }
    }

    public RiskAssessmentResponse responseFromEntity(RiskAssessment riskAssessment) {
        return new RiskAssessmentResponse(
                riskAssessment.getId(),
                riskAssessment.getName(),
                riskAssessment.getOrderingId(),
                riskAssessment.isDefaultSelection(),
                riskAssessment.getStatus()
        );
    }

    public RiskAssessment entityFromRequest(RiskAssessmentRequest request) {
        return RiskAssessment.builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(RiskAssessment riskAssessment) {
        String name = riskAssessment.getName();

        return new NomenclatureResponse(
                riskAssessment.getId(),
                name,
                riskAssessment.getOrderingId(),
                riskAssessment.isDefaultSelection(),
                riskAssessment.getStatus()
        );
    }

}
