package bg.energo.phoenix.service.nomenclature.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.contract.ContractVersionTypesRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
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

import static bg.energo.phoenix.permissions.PermissionContextEnum.CONTRACT_VERSION_TYPES;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractVersionTypesService implements NomenclatureBaseService {
    private final ContractVersionTypesRepository contractVersionTypesRepository;

    /**
     * @return {@link Nomenclature} type
     */
    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.CONTRACT_VERSION_TYPES;
    }

    /**
     * Retrieves detailed information about {@link ContractVersionType} by ID
     *
     * @param id ID of {@link ContractVersionType}
     * @return {@link ContractVersionTypesResponse}
     * @throws DomainEntityNotFoundException if no {@link ContractVersionType} was found with the provided ID.
     */
    public ContractVersionTypesResponse view(Long id) {
        return new ContractVersionTypesResponse(
                contractVersionTypesRepository
                        .findById(id)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Contract Version Type with presented id: [%s] not found".formatted(id)))
        );
    }

    /**
     * Filters the list of contract version types based on the given filter request parameters.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ContractVersionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return a Page of ContractVersionTypesResponse objects containing the filtered list of contract version types.
     */
    public Page<ContractVersionTypesResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        return contractVersionTypesRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    /**
     * Filters {@link ContractVersionType} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link ContractVersionType}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTRACT_VERSION_TYPES, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return contractVersionTypesRepository.filterNomenclature(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    /**
     * Adds {@link ContractVersionType} at the end with the highest ordering ID.
     * If the request asks to save {@link ContractVersionType} as a default and a default {@link ContractVersionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link ContractVersionTypesRequest}
     * @return {@link ContractVersionTypesResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public ContractVersionTypesResponse add(ContractVersionTypesRequest request) {
        if (request.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot add contract version type with status DELETED");
            throw new OperationNotAllowedException("status-You cannot add contract version type with status DELETED");
        }

        if (contractVersionTypesRepository.countContractVersionTypesByNameAndStatus(
                request.getName(),
                List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)
        ) > 0) {
            log.error("Contract Version Type with presented name: [%s] already exists".formatted(request.getName()));
            throw new IllegalArgumentsProvidedException("name-Contract Version Type with presented name: [%s] already exists".formatted(request.getName()));
        }

        Long lastOrderingId = contractVersionTypesRepository.findLastOrderingId();
        ContractVersionType contractVersionType = new ContractVersionType(request);
        contractVersionType.setOrderingId(lastOrderingId == null ? 1L : ++lastOrderingId);
        if (request.getDefaultSelection()) {
            Optional<ContractVersionType> currentDefaultOptional = contractVersionTypesRepository.findByIsDefaultTrue();
            if (currentDefaultOptional.isPresent()) {
                ContractVersionType defaultContractVersionType = currentDefaultOptional.get();
                defaultContractVersionType.setIsDefault(false);

                contractVersionTypesRepository.save(defaultContractVersionType);
            }
        }
        contractVersionType.setIsHardCoded(false);
        return new ContractVersionTypesResponse(contractVersionTypesRepository.save(contractVersionType));
    }

    /**
     * Edits the {@link ContractVersionType}.
     * If the request asks to save {@link ContractVersionType} as a default and a default {@link ContractVersionType} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link ContractVersionType}
     * @param request {@link ContractVersionTypesRequest}
     * @return {@link ContractVersionTypesResponse}
     * @throws DomainEntityNotFoundException if {@link ContractVersionType} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link ContractVersionType} is deleted.
     */
    @Transactional
    public ContractVersionTypesResponse edit(Long id, ContractVersionTypesRequest request) {
        if (request.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot set contract version type status to DELETED");
            throw new OperationNotAllowedException("status-You cannot set contract version type status to DELETED");
        }

        ContractVersionType contractVersionType = contractVersionTypesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Contract Version Type with presented id: [%s] not found".formatted(id)));

        if (contractVersionType.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Cannot edit deleted contract version type");
            throw new OperationNotAllowedException("You cannot edit deleted contract version type");
        }

        if (contractVersionType.getIsHardCoded()) {
            throw new OperationNotAllowedException("name- Hardcoded nomenclature can't be changed.;");
        }

        if (contractVersionTypesRepository
                .countContractVersionTypesByNameAndStatus(
                        request.getName(),
                        List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)
                ) > 0 && !request.getName().equals(contractVersionType.getName())) {
            log.error("Contract Version Type with presented name: [%s] already exists".formatted(request.getName()));
            throw new IllegalArgumentsProvidedException("name-Contract Version Type with presented name: [%s] already exists".formatted(request.getName()));
        }

        if (request.getDefaultSelection() && !contractVersionType.getIsDefault()) {
            Optional<ContractVersionType> currentDefaultOptional = contractVersionTypesRepository.findByIsDefaultTrue();
            if (currentDefaultOptional.isPresent()) {
                ContractVersionType defaultContractVersionType = currentDefaultOptional.get();
                defaultContractVersionType.setIsDefault(false);

                contractVersionTypesRepository.save(defaultContractVersionType);
            }
        }

        contractVersionType.setIsDefault(request.getDefaultSelection());
        contractVersionType.setName(request.getName());
        contractVersionType.setStatus(request.getStatus());

        return new ContractVersionTypesResponse(contractVersionType);
    }

    /**
     * Deletes {@link ContractVersionType} if the validations are passed.
     *
     * @param id ID of the {@link ContractVersionType}
     * @throws DomainEntityNotFoundException if {@link ContractVersionType} is not found.
     * @throws OperationNotAllowedException  if the {@link ContractVersionType} is already deleted.
     * @throws OperationNotAllowedException  if the {@link ContractVersionType} is connected to active object.
     */
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTRACT_VERSION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void delete(Long id) {
        ContractVersionType contractVersionType = contractVersionTypesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Contract Version Type with presented id: [%s] not found".formatted(id)));

        if (contractVersionType.getIsHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }

        if (contractVersionType.getStatus().equals(NomenclatureItemStatus.DELETED)) {
            log.error("Contract Version Type is already deleted");
            throw new IllegalArgumentsProvidedException("Contract Version Type is already deleted");
        }

        if (contractVersionTypesRepository.hasActiveConnections(id)) {
            log.error("Contract Version Type is connected to entity in system, cannot delete");
            throw new OperationNotAllowedException("Contract Version Type is connected to entity in system, cannot delete");
        }

        contractVersionType.setStatus(NomenclatureItemStatus.DELETED);

        contractVersionTypesRepository.save(contractVersionType);
    }

    /**
     * Changes the ordering of a {@link ContractVersionType} item in the contract version types list to a specified position.
     * The method retrieves the contract version type item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the contract version type item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link ContractVersionType} item with the given ID is found
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTRACT_VERSION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of contractVersionType item with ID: {} in contractVersionTypes to place: {}", request.getId(), request.getOrderingId());

        ContractVersionType contractVersionType = contractVersionTypesRepository.findById(request.getId()).orElseThrow(() -> new DomainEntityNotFoundException("id-Contract Version Type not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<ContractVersionType> contractVersionTypes;

        if (contractVersionType.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = contractVersionType.getOrderingId();

            contractVersionTypes = contractVersionTypesRepository.findInOrderingIdRange(start, end, contractVersionType.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (ContractVersionType c : contractVersionTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = contractVersionType.getOrderingId();
            end = request.getOrderingId();

            contractVersionTypes = contractVersionTypesRepository.findInOrderingIdRange(start, end, contractVersionType.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (ContractVersionType c : contractVersionTypes) {
                c.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        contractVersionType.setOrderingId(request.getOrderingId());
        contractVersionTypes.add(contractVersionType);
        contractVersionTypesRepository.saveAll(contractVersionTypes);
    }

    /**
     * Sorts all {@link ContractVersionType} alphabetically not taking its status into consideration.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = CONTRACT_VERSION_TYPES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the contract version types alphabetically");
        List<ContractVersionType> contractVersionTypes = contractVersionTypesRepository.orderByName();
        long orderingId = 1;

        for (ContractVersionType c : contractVersionTypes) {
            c.setOrderingId(orderingId);
            orderingId++;
        }
        contractVersionTypesRepository.saveAll(contractVersionTypes);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return contractVersionTypesRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return contractVersionTypesRepository.findByIdIn(ids);
    }
}
