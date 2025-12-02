package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CollectionPartner;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.CollectionPartnerRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CollectionPartnerResponse;
import bg.energo.phoenix.repository.nomenclature.receivable.CollectionPartnerRepository;
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
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.COLLECTION_PARTNERS;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.COLLECTION_PARTNER;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionPartnerService implements NomenclatureBaseService {

    private final CollectionPartnerRepository collectionPartnerRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return COLLECTION_PARTNERS;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = COLLECTION_PARTNER,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering collection partners with statuses: {}", request);
        Page<CollectionPartner> collectionPartners = collectionPartnerRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return collectionPartners.map(this::nomenclatureResponseFromEntity);
    }

    public Page<CollectionPartnerResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering collection partners list with request: {}", request);
        Page<CollectionPartner> collectionPartners = collectionPartnerRepository.filter(
                request.getPrompt(),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return collectionPartners.map(this::responseFromEntity);
    }


    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = COLLECTION_PARTNER,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in collection partner", request.getId());

        CollectionPartner collectionPartner = collectionPartnerRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection partner not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<CollectionPartner> collectionPartners;

        if (collectionPartner.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = collectionPartner.getOrderingId();
            collectionPartners = collectionPartnerRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            collectionPartner.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (CollectionPartner cp : collectionPartners) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = collectionPartner.getOrderingId();
            end = request.getOrderingId();
            collectionPartners = collectionPartnerRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            collectionPartner.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (CollectionPartner cp : collectionPartners) {
                cp.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        collectionPartner.setOrderingId(request.getOrderingId());
        collectionPartners.add(collectionPartner);
        collectionPartnerRepository.saveAll(collectionPartners);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = COLLECTION_PARTNER,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the collection partner alphabetically");

        List<CollectionPartner> collectionPartners = collectionPartnerRepository.orderByName();
        long orderingId = 1;

        for (CollectionPartner collectionPartner : collectionPartners) {
            collectionPartner.setOrderingId(orderingId);
            orderingId++;
        }

        collectionPartnerRepository.saveAll(collectionPartners);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = COLLECTION_PARTNER,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing collection partner with ID: {}", id);

        CollectionPartner collectionPartner = collectionPartnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection partner not found, ID: " + id));

        if (collectionPartner.getStatus().equals(DELETED)) {
            log.error("Collection partner {} is already deleted", id);
            throw new OperationNotAllowedException("id-Collection partner " + id + " is already deleted");
        }
        if (collectionPartnerRepository.isConnectedToChannel(id)) {
            log.error("Can't delete the nomenclature because it is connected to Collection Channel");
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to Collection Channel");
        }
        collectionPartner.setStatus(DELETED);
        collectionPartnerRepository.save(collectionPartner);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return collectionPartnerRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return collectionPartnerRepository.findByIdIn(ids);
    }

    @Transactional
    public CollectionPartnerResponse add(CollectionPartnerRequest request) {
        log.debug("Adding collection partner: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (collectionPartnerRepository.countCollectionPartnerByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Collection partner with such name already exists");
            throw new ClientException("collection-partner with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = collectionPartnerRepository.findLastOrderingId();
        CollectionPartner collectionPartner = entityFromRequest(request);
        collectionPartner.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);

        assignDefaultSelectionWhenAdding(request, collectionPartner);

        collectionPartnerRepository.save(collectionPartner);
        return responseFromEntity(collectionPartner);
    }

    public CollectionPartnerResponse view(Long id) {
        log.debug("Fetching collection partner with ID: {}", id);
        CollectionPartner collectionPartner = collectionPartnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection partner not found, ID: " + id));

        return responseFromEntity(collectionPartner);
    }

    @Transactional
    public CollectionPartnerResponse edit(Long id, CollectionPartnerRequest request) {
        log.debug("Editing collection partner: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        CollectionPartner collectionPartner = collectionPartnerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Collection partner not found, ID: " + id));

        if (collectionPartner.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (collectionPartnerRepository.countCollectionPartnerByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0
            && !collectionPartner.getName().equals(request.getName().trim())) {
            log.error("Collection partner with such name already exists");
            throw new ClientException("collection-partner with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, collectionPartner);

        collectionPartner.setName(request.getName().trim());
        collectionPartner.setStatus(request.getStatus());

        return responseFromEntity(collectionPartner);
    }


    private void assignDefaultSelectionWhenAdding(CollectionPartnerRequest request, CollectionPartner collectionPartner) {
        if (request.getStatus().equals(INACTIVE)) {
            collectionPartner.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<CollectionPartner> currentDefaultCollectionPartnerOptional = collectionPartnerRepository.findByDefaultSelectionTrue();
                if (currentDefaultCollectionPartnerOptional.isPresent()) {
                    CollectionPartner currentDefaultCollectionPartner = currentDefaultCollectionPartnerOptional.get();
                    currentDefaultCollectionPartner.setDefaultSelection(false);
                    collectionPartnerRepository.save(currentDefaultCollectionPartner);
                }
            }
            collectionPartner.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(CollectionPartnerRequest request, CollectionPartner collectionPartner) {
        if (request.getStatus().equals(INACTIVE)) {
            collectionPartner.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!collectionPartner.isDefaultSelection()) {
                    Optional<CollectionPartner> optionalCollectionPartner = collectionPartnerRepository.findByDefaultSelectionTrue();
                    if (optionalCollectionPartner.isPresent()) {
                        CollectionPartner currentCollectionPartner = optionalCollectionPartner.get();
                        currentCollectionPartner.setDefaultSelection(false);
                        collectionPartnerRepository.save(currentCollectionPartner);
                    }
                }
            }
            collectionPartner.setDefaultSelection(request.getDefaultSelection());
        }
    }


    public CollectionPartnerResponse responseFromEntity(CollectionPartner collectionPartner) {
        return new CollectionPartnerResponse(
                collectionPartner.getId(),
                collectionPartner.getName(),
                collectionPartner.getOrderingId(),
                collectionPartner.isDefaultSelection(),
                collectionPartner.getStatus()
        );
    }

    public NomenclatureResponse nomenclatureResponseFromEntity(CollectionPartner collectionPartner) {
        return new NomenclatureResponse(
                collectionPartner.getId(),
                collectionPartner.getName(),
                collectionPartner.getOrderingId(),
                collectionPartner.isDefaultSelection(),
                collectionPartner.getStatus()
        );
    }

    public CollectionPartner entityFromRequest(CollectionPartnerRequest request) {
        return CollectionPartner
                .builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }
}
