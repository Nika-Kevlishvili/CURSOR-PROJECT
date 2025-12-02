package bg.energo.phoenix.service.nomenclature.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.customer.Title;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.customer.TitleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.TitleResponse;
import bg.energo.phoenix.repository.nomenclature.customer.TitleRepository;
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

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TitleService implements NomenclatureBaseService {
    private final TitleRepository titleRepository;


    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.TITLES;
    }


    /**
     * Filters {@link Title} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Title}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link NomenclatureResponse}
     */
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TITLES, permissions = {NOMENCLATURE_VIEW}),
                    @PermissionMapping(context = CUSTOMER, permissions = {
                            CUSTOMER_VIEW_BASIC,
                            CUSTOMER_VIEW_DELETED,
                            CUSTOMER_VIEW_GDPR,
                            CUSTOMER_VIEW_GDPR_AM,
                            CUSTOMER_VIEW_BASIC_AM}),
                    @PermissionMapping(context = EXPRESS_CONTRACT, permissions = {
                            EXPRESS_CONTRACT_CREATE})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering titles list with statuses: {}", request);
        return titleRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Changes the ordering of a {@link Title} item in the Title list to a specified position.
     * The method retrieves the {@link Title} item by ID, and then moves it to the requested position.
     * The method adjusts the ordering IDs of the other items to maintain a consistent order.
     *
     * @param request the request object containing the ID of the {@link Title} item and the new ordering ID
     * @throws DomainEntityNotFoundException if no {@link Title} item with the given ID is found
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TITLES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in titles to top", request.getId());

        Title title = titleRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Title not found"));

        Long start;
        Long end;
        List<Title> titles;

        if (title.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = title.getOrderingId();
            titles = titleRepository.findInOrderingIdRange(start, end, title.getId(), Sort.by(Sort.Direction.ASC, "orderingId"));

            long tempOrderingId = request.getOrderingId() + 1;
            for (Title t : titles) {
                t.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = title.getOrderingId();
            end = request.getOrderingId();
            titles = titleRepository.findInOrderingIdRange(start, end, title.getId(), Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (Title t : titles) {
                t.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        title.setOrderingId(request.getOrderingId());
        titles.add(title);
        titleRepository.saveAll(titles);
    }

    /**
     * Sorts all {@link Title} alphabetically not taking its status into consideration.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TITLES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the titles alphabetically");
        List<Title> titles = titleRepository.orderByName();
        long orderingId = 1;

        for (Title t : titles) {
            t.setOrderingId(orderingId);
            orderingId++;
        }

        titleRepository.saveAll(titles);
    }

    /**
     * Deletes {@link Title} if the validations are passed.
     *
     * @param id ID of the {@link Title}
     * @throws DomainEntityNotFoundException if {@link Title} is not found.
     * @throws OperationNotAllowedException  if the {@link Title} is already deleted.
     * @throws OperationNotAllowedException  if the {@link Title} is connected to active object.
     */
    @Transactional
    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TITLES, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing title with ID: {}", id);
        Title title = titleRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Title not found"));

        if (title.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new OperationNotAllowedException("Item is already deleted.");
        }

        if (titleRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        title.setStatus(DELETED);
        titleRepository.save(title);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return titleRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return titleRepository.findByIdIn(ids);
    }

    /**
     * Adds {@link Title} at the end with the highest ordering ID.
     * If the request asks to save {@link Title} as a default and a default {@link Title} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param request {@link Title}
     * @return {@link TitleResponse}
     * @throws ClientException if {@link NomenclatureItemStatus} in the request is DELETED.
     */
    @Transactional
    public TitleResponse add(TitleRequest request) {
        log.debug("Adding title: {}", request.toString());

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        if (titleRepository.countTitleByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("name-Title with the same name already exists;");
            throw new OperationNotAllowedException("name-Title with the same name already exists;");
        }

        Long lastSortOrder = titleRepository.findLastOrderingId();
        Title title = new Title(request);
        title.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        if (request.getDefaultSelection()) {
            Optional<Title> currentDefaultTitleOptional = titleRepository.findByDefaultSelectionTrue();
            if (currentDefaultTitleOptional.isPresent()) {
                Title currentDefaultTitle = currentDefaultTitleOptional.get();
                currentDefaultTitle.setDefaultSelection(false);
                titleRepository.save(currentDefaultTitle);
            }
        }
        Title titleEntity = titleRepository.save(title);
        return new TitleResponse(titleEntity);
    }

    /**
     * Retrieves detailed information about {@link Title} by ID
     *
     * @param id ID of {@link Title}
     * @return {@link TitleResponse}
     * @throws DomainEntityNotFoundException if no {@link Title} was found with the provided ID.
     */
    public TitleResponse view(Long id) {
        log.debug("Fetching title with ID: {}", id);
        Title title = titleRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Title not found"));
        return new TitleResponse(title);
    }

    /**
     * Edit the requested {@link Title}.
     * If the request asks to save {@link Title} as a default and a default {@link Title} already exists,
     * default selection will be removed from the latter and assigned to the new one.
     *
     * @param id      ID of {@link Title}
     * @param request {@link TitleRequest}
     * @return {@link TitleResponse}
     * @throws DomainEntityNotFoundException if {@link Title} is not found.
     * @throws ClientException               if {@link NomenclatureItemStatus} in the request is DELETED.
     * @throws OperationNotAllowedException  if the {@link Title} is deleted.
     */
    @Transactional
    public TitleResponse edit(Long id, TitleRequest request) {
        log.debug("Editing title: {}, with ID: {}", request.toString(), id);

        if (request.getStatus().equals(DELETED)) {
            log.error("status-Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item",ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Title title = titleRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Title not found"));

        if (titleRepository.countTitleByStatusAndName(request.getName(), List.of(ACTIVE, INACTIVE)) > 0
                && !title.getName().equals(request.getName().trim())) {
            log.error("name-Title with the same name already exists;");
            throw new OperationNotAllowedException("name-Title with the same name already exists;");
        }

        if (title.getStatus().equals(DELETED)) {
            log.error("status-Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        if (request.getDefaultSelection() && !title.isDefaultSelection()) {
            Optional<Title> currentDefaultTitleOptional = titleRepository.findByDefaultSelectionTrue();
            if (currentDefaultTitleOptional.isPresent()) {
                Title currentDefaultTitle = currentDefaultTitleOptional.get();
                currentDefaultTitle.setDefaultSelection(false);
                titleRepository.save(currentDefaultTitle);
            }
        }
        title.setDefaultSelection(request.getDefaultSelection());

        title.setName(request.getName().trim());
        title.setStatus(request.getStatus());
        return new TitleResponse(titleRepository.save(title));
    }

    /**
     * Filters {@link Title} against the provided {@link NomenclatureItemsBaseFilterRequest}.
     * If excludedItemId is provided in {@link NomenclatureItemsBaseFilterRequest}, the matching item won't be returned in response.
     * If prompt is provided in {@link NomenclatureItemsBaseFilterRequest}, the searchable field is {@link Title}'s name.
     *
     * @param request {@link NomenclatureItemsBaseFilterRequest}
     * @return {@link Page<TitleResponse> Page&lt;TitleResponse&gt;} containing detailed information
     */
    public Page<TitleResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering titles list with statuses: {}", request.toString());
        Page<Title> page = titleRepository
                .filter(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
        return page.map(TitleResponse::new);
    }
}
