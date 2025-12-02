package bg.energo.phoenix.service.nomenclature.pod;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.pod.PodAdditionalParameters;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.pod.PodAdditionalParametersRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.pod.PodAdditionalParametersResponse;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.POD;
import static bg.energo.phoenix.permissions.PermissionContextEnum.POD_PARAMS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodAdditionalParametersService implements NomenclatureBaseService {

    private final PodAdditionalParametersRepository podAdditionalParametersRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.POD_ADDITIONAL_PARAMETERS;
    }

    /**
     * Filters PodAdditionalParameters against the provided request.
     *
     * @param request Filter criteria
     * @return Page of PodAdditionalParametersResponse
     */
    public Page<PodAdditionalParametersResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering PodAdditionalParameters list with request: {}", request.toString());
        Page<PodAdditionalParameters> page = podAdditionalParametersRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(PodAdditionalParametersResponse::new);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POD_PARAMS, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = POD, permissions = {
                            POD_EDIT_ADDITIONAL_PARAMS
                    })
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return podAdditionalParametersRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds a new PodAdditionalParameters at the end with the highest ordering ID.
     *
     * @param request PodAdditionalParametersRequest
     * @return PodAdditionalParametersResponse
     */
    @Transactional
    public PodAdditionalParametersResponse add(PodAdditionalParametersRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        String name = request.getName().trim();
        request.setName(name);

        Integer count = getExistingRecordsCountByName(name);
        if (count > 0) {
            log.error("PodAdditionalParameters Name is not unique");
            throw new ClientException("name-PodAdditionalParameters Name is not unique", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = podAdditionalParametersRepository.findLastOrderingId();
        PodAdditionalParameters parameters = new PodAdditionalParameters(request);
        parameters.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), parameters);

        return new PodAdditionalParametersResponse(podAdditionalParametersRepository.save(parameters));
    }

    /**
     * Retrieves detailed information about PodAdditionalParameters by ID
     */
    public PodAdditionalParametersResponse view(Long id) {
        log.debug("Fetching PodAdditionalParameters with ID: {}", id);
        PodAdditionalParameters parameters = podAdditionalParametersRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));
        return new PodAdditionalParametersResponse(parameters);
    }

    /**
     * Edits an existing PodAdditionalParameters
     */
    @Transactional
    public PodAdditionalParametersResponse edit(Long id, PodAdditionalParametersRequest request) {
        log.debug("Editing PodAdditionalParameters: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        String name = request.getName().trim();
        request.setName(name);

        PodAdditionalParameters parameters = podAdditionalParametersRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (parameters.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (!parameters.getName().equals(name)) {
            if (getExistingRecordsCountByName(name) > 0) {
                log.error("PodAdditionalParameters Name is not unique");
                throw new ClientException("name-PodAdditionalParameters Name is not unique", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), parameters);
        parameters.setName(name);
        parameters.setStatus(request.getStatus());

        return new PodAdditionalParametersResponse(podAdditionalParametersRepository.save(parameters));
    }

    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean isDefaultSelection, PodAdditionalParameters parameters) {
        if (status.equals(INACTIVE)) {
            parameters.setIsDefault(false);
        } else {
            if (isDefaultSelection) {
                Optional<PodAdditionalParameters> currentDefault = podAdditionalParametersRepository.findByIsDefaultTrue();
                if (currentDefault.isPresent()) {
                    PodAdditionalParameters defaultParam = currentDefault.get();
                    defaultParam.setIsDefault(false);
                    podAdditionalParametersRepository.save(defaultParam);
                }
                parameters.setIsDefault(true);
            } else {
                parameters.setIsDefault(false);
            }
        }
    }

    private Integer getExistingRecordsCountByName(String name) {
        return podAdditionalParametersRepository.getExistingRecordsCountByName(
                name.toLowerCase(),
                List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
        );
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POD_PARAMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} to new position", request.getId());

        PodAdditionalParameters parameters = podAdditionalParametersRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<PodAdditionalParameters> parametersList;

        if (parameters.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = parameters.getOrderingId();
            parametersList = podAdditionalParametersRepository.findInOrderingIdRange(
                    start,
                    end,
                    parameters.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (PodAdditionalParameters param : parametersList) {
                param.setOrderingId(tempOrderingId++);
            }
        } else {
            start = parameters.getOrderingId();
            end = request.getOrderingId();
            parametersList = podAdditionalParametersRepository.findInOrderingIdRange(
                    start,
                    end,
                    parameters.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (PodAdditionalParameters param : parametersList) {
                param.setOrderingId(tempOrderingId--);
            }
        }

        parameters.setOrderingId(request.getOrderingId());
        podAdditionalParametersRepository.save(parameters);
        podAdditionalParametersRepository.saveAll(parametersList);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POD_PARAMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the PodAdditionalParameters alphabetically");
        List<PodAdditionalParameters> parametersList = podAdditionalParametersRepository.orderByName();
        long orderingId = 1;

        for (PodAdditionalParameters param : parametersList) {
            param.setOrderingId(orderingId++);
        }

        podAdditionalParametersRepository.saveAll(parametersList);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = POD_PARAMS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing PodAdditionalParameters with ID: {}", id);
        PodAdditionalParameters parameters = podAdditionalParametersRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        if (parameters.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        parameters.setStatus(DELETED);
        podAdditionalParametersRepository.save(parameters);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return podAdditionalParametersRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return podAdditionalParametersRepository.findByIdIn(ids);
    }
}