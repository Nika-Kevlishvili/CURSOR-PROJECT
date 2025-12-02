package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.MissingCustomer;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.MissingCustomerRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.MissingCustomerResponse;
import bg.energo.phoenix.repository.nomenclature.customer.MissingCustomerRepository;
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
import static bg.energo.phoenix.permissions.PermissionContextEnum.MISSING_CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;


@Slf4j
@Service
@RequiredArgsConstructor
public class MissingCustomerService implements NomenclatureBaseService {

    private final MissingCustomerRepository missingCustomerRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.MISSING_CUSTOMER;
    }


    /**
     * Filters the list of missing customers based on the provided request parameters.
     *
     * @param request The request object containing the filter parameters.
     * @return A page of `MissingCustomerResponse` objects representing the filtered missing customers.
     */
    public Page<MissingCustomerResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering MissingCustomer list with request: {}", request.toString());
        Page<MissingCustomer> page = missingCustomerRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(MissingCustomerResponse::new);
    }

    /**
     * Filters the list of missing customers based on the provided request parameters.
     *
     * @param request The request object containing the filter parameters.
     * @return A page of `NomenclatureResponse` objects representing the filtered missing customers.
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MISSING_CUSTOMER, permissions = {NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        return missingCustomerRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    /**
     * Adds a new missing customer to the system.
     *
     * @param request The request object containing the details of the new missing customer.
     * @return A `MissingCustomerResponse` object representing the newly created missing customer.
     * @throws ClientException if the request has an invalid status or the missing customer name is not unique.
     */
    @Transactional
    public MissingCustomerResponse add(MissingCustomerRequest request) {
        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastSortOrder = missingCustomerRepository.findLastOrderingId();
        MissingCustomer missingCustomer = new MissingCustomer(request);
        missingCustomer.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), missingCustomer);

        return new MissingCustomerResponse(missingCustomerRepository.save(missingCustomer));
    }

    /**
     * Fetches a missing customer by its ID.
     *
     * @param id The ID of the missing customer to fetch.
     * @return A `MissingCustomerResponse` object representing the fetched missing customer.
     * @throws ClientException if the missing customer with the given ID is not found.
     */
    public MissingCustomerResponse view(Long id) {
        log.debug("Fetching MissingCustomer with ID: {}", id);
        MissingCustomer missingCustomer = missingCustomerRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));
        return new MissingCustomerResponse(missingCustomer);
    }

    /**
     * Edits an existing missing customer in the system.
     *
     * @param id      The ID of the missing customer to be edited.
     * @param request The request object containing the updated details of the missing customer.
     * @return A `MissingCustomerResponse` object representing the updated missing customer.
     * @throws ClientException if the request has an invalid status, the missing customer name is not unique, or the missing customer is in a deleted state.
     */
    @Transactional
    public MissingCustomerResponse edit(Long id, MissingCustomerRequest request) {
        log.debug("Editing MissingCustomer: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        MissingCustomer missingCustomer = missingCustomerRepository
                .findById(id)
                .orElseThrow(() -> new ClientException(DOMAIN_ENTITY_NOT_FOUND));

        if (missingCustomer.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        assignDefaultSelection(request.getStatus(), request.getDefaultSelection(), missingCustomer);
        missingCustomer.setUic(request.getUic());
        missingCustomer.setName(request.getName());
        missingCustomer.setNameTransliterated(request.getNameTransliterated());
        missingCustomer.setLegalForm(request.getLegalForm());
        missingCustomer.setLegalFormTransliterated(request.getLegalFormTransliterated());
        missingCustomer.setStatus(request.getStatus());

        return new MissingCustomerResponse(missingCustomerRepository.save(missingCustomer));
    }

    /**
     * Assigns the default selection status to a missing customer.
     * <p>
     * If the status is INACTIVE, the missing customer's `isDefault` flag is set to false.
     * Otherwise, if `isDefaultSelection` is true, the current default missing customer (if any) has its `isDefault` flag set to false,
     * and the current missing customer has its `isDefault` flag set to true.
     * If `isDefaultSelection` is false, the missing customer's `isDefault` flag is set to false.
     *
     * @param status             The status of the missing customer.
     * @param isDefaultSelection Whether the missing customer should be the default selection.
     * @param missingCustomer    The missing customer to assign the default selection status to.
     */
    private void assignDefaultSelection(NomenclatureItemStatus status, Boolean isDefaultSelection, MissingCustomer missingCustomer) {
        if (status.equals(INACTIVE)) {
            missingCustomer.setIsDefault(false);
        } else {
            if (isDefaultSelection) {
                Optional<MissingCustomer> currentDefault = missingCustomerRepository.findByIsDefaultTrue();
                if (currentDefault.isPresent()) {
                    MissingCustomer defaultParam = currentDefault.get();
                    defaultParam.setIsDefault(false);
                    missingCustomerRepository.save(defaultParam);
                }
                missingCustomer.setIsDefault(true);
            } else {
                missingCustomer.setIsDefault(false);
            }
        }
    }

    /**
     * Retrieves the count of existing records by the given name, considering only active and inactive statuses.
     *
     * @param name The name to search for.
     * @return The count of existing records with the given name and active or inactive status.
     */
    private Integer getExistingRecordsCountByName(String name) {
        return missingCustomerRepository.getExistingRecordsCountByName(
                name.toLowerCase(),
                List.of(NomenclatureItemStatus.ACTIVE, INACTIVE)
        );
    }

    /**
     * Changes the ordering of a missing customer in the system.
     * <p>
     * This method is responsible for updating the ordering IDs of the missing customers affected by the change.
     * It finds the missing customer with the given ID, and then updates the ordering IDs of the missing customers
     * between the current ordering ID and the new ordering ID, either ascending or descending depending on the
     * direction of the change.
     *
     * @param request The request containing the ID of the missing customer and the new ordering ID.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MISSING_CUSTOMER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} to new position", request.getId());

        MissingCustomer missingCustomer = missingCustomerRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<MissingCustomer> missingCustomerList;

        if (missingCustomer.getOrderingId() > request.getOrderingId()) {
            start = request.getOrderingId();
            end = missingCustomer.getOrderingId();
            missingCustomerList = missingCustomerRepository.findInOrderingIdRange(
                    start,
                    end,
                    missingCustomer.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() + 1;
            for (MissingCustomer param : missingCustomerList) {
                param.setOrderingId(tempOrderingId++);
            }
        } else {
            start = missingCustomer.getOrderingId();
            end = request.getOrderingId();
            missingCustomerList = missingCustomerRepository.findInOrderingIdRange(
                    start,
                    end,
                    missingCustomer.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId")
            );

            long tempOrderingId = request.getOrderingId() - 1;
            for (MissingCustomer param : missingCustomerList) {
                param.setOrderingId(tempOrderingId--);
            }
        }

        missingCustomer.setOrderingId(request.getOrderingId());
        missingCustomerRepository.save(missingCustomer);
        missingCustomerRepository.saveAll(missingCustomerList);
    }

    /**
     * Sorts the list of MissingCustomer entities alphabetically by name and updates their ordering IDs accordingly.
     * This method is secured by the NOMENCLATURE_EDIT permission.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MISSING_CUSTOMER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the MissingCustomer alphabetically");
        List<MissingCustomer> missingCustomerList = missingCustomerRepository.orderByName();
        long orderingId = 1;

        for (MissingCustomer missingCustomer : missingCustomerList) {
            missingCustomer.setOrderingId(orderingId++);
        }

        missingCustomerRepository.saveAll(missingCustomerList);
    }

    /**
     * Deletes a MissingCustomer entity with the specified ID.
     * This method is secured by the NOMENCLATURE_EDIT permission.
     *
     * @param id The ID of the MissingCustomer to delete.
     * @throws ClientException              if the MissingCustomer with the specified ID is not found.
     * @throws OperationNotAllowedException if the MissingCustomer is already in the DELETED status.
     */
    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = MISSING_CUSTOMER, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing MissingCustomer with ID: {}", id);
        MissingCustomer missingCustomer = missingCustomerRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("id-not found", DOMAIN_ENTITY_NOT_FOUND));

        if (missingCustomer.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("id-Item is already deleted.");
        }

        missingCustomer.setStatus(DELETED);
        missingCustomerRepository.save(missingCustomer);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return missingCustomerRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return missingCustomerRepository.findByIdIn(ids);
    }
}
