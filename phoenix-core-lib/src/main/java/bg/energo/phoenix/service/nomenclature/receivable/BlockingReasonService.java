package bg.energo.phoenix.service.nomenclature.receivable;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.BlockingReasonFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.receivable.BlockingReasonRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.BlockingReasonResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
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
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockingReasonService implements NomenclatureBaseService {
    private final BlockingReasonRepository blockingReasonRepository;


    @Override
    public Nomenclature getNomenclatureType() {
        return Nomenclature.BLOCKING_REASON;
    }

    @Transactional
    public BlockingReasonResponse add(BlockingReasonRequest request) {
        request.setName(request.getName().trim());
        log.debug("Adding Blocking Reason: {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-[status]Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        List<BlockingReason> blockingReasonsByName = blockingReasonRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
        if (blockingReasonsByName.size() > 0) {
            log.error("blocking reason with presented name already exists");
            throw new ClientException("name-blocking reason with presented name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (request.getReasonTypes().size() > 1) {
            for (ReceivableBlockingReasonType type : request.getReasonTypes()) {
                if (type.equals(ReceivableBlockingReasonType.ALL)) {
                    throw new ClientException("reasonType-select correct reason types", ILLEGAL_ARGUMENTS_PROVIDED);

                }
            }
        }

        Long lastSortOrder = blockingReasonRepository.findLastOrderingId();
        BlockingReason blockingReason = new BlockingReason(request);
        blockingReason.setOrderingId(lastSortOrder == null ? 1 : lastSortOrder + 1);
        checkCurrentDefaultSelection(request, blockingReason);
        blockingReason.setHardCoded(false);
        BlockingReason savedBlockingReason = blockingReasonRepository.save(blockingReason);
        return new BlockingReasonResponse(savedBlockingReason);
    }

    @Transactional
    public BlockingReasonResponse edit(Long id, BlockingReasonRequest request) {
        request.setName(request.getName().trim());
        log.debug("Editing Blocking Reason: {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-[status]Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        BlockingReason blockingReason = blockingReasonRepository
                .findById(id)
                .orElseThrow(
                        () -> new ClientException("id-Blocking reason with presented id not found", DOMAIN_ENTITY_NOT_FOUND)
                );

        if (blockingReason.isHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }

        if (!blockingReason.getName().equalsIgnoreCase(request.getName())) {
            List<BlockingReason> blockingReasonByName = blockingReasonRepository.findByNameAndStatuses(request.getName(), List.of(ACTIVE, INACTIVE));
            if (blockingReasonByName.size() > 0) {
                log.error("Blocking Reason with presented name already exists");
                throw new ClientException("name-Blocking Reason with presented name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (blockingReason.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item");
            throw new OperationNotAllowedException("status-Cannot edit DELETED item.");
        }

        checkCurrentDefaultSelection(request, blockingReason);

        if (request.getReasonTypes().size() > 1) {
            for (ReceivableBlockingReasonType type : request.getReasonTypes()) {
                if (type.equals(ReceivableBlockingReasonType.ALL)) {
                    throw new ClientException("reasonType-select correct reason types", ILLEGAL_ARGUMENTS_PROVIDED);

                }
            }
        }

        blockingReason.setName(request.getName());
        blockingReason.setReasonTypes(request.getReasonTypes());
        blockingReason.setStatus(request.getStatus());
        if (request.getStatus().equals(INACTIVE)) {
            blockingReason.setDefaultSelection(false);
        }
        return new BlockingReasonResponse(blockingReasonRepository.save(blockingReason));
    }

    public BlockingReasonResponse view(Long id) {
        log.debug("Fetching Blocking Reason with ID: {}", id);
        BlockingReason blockingReason = blockingReasonRepository.findById(id).orElseThrow(
                () -> new DomainEntityNotFoundException("id-Blocking reason with presented id not found")
        );
        return new BlockingReasonResponse(blockingReason);
    }

    private void checkCurrentDefaultSelection(BlockingReasonRequest request, BlockingReason blockingReason) {
        if (request.getStatus().equals(INACTIVE)) {
            blockingReason.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<BlockingReason> currentDefaultProductGroupsOptional = blockingReasonRepository.findByDefaultSelectionTrue();
                if (currentDefaultProductGroupsOptional.isPresent()) {
                    BlockingReason defaultProductGroups = currentDefaultProductGroupsOptional.get();
                    defaultProductGroups.setDefaultSelection(false);
                    blockingReasonRepository.save(defaultProductGroups);
                }
            }
            blockingReason.setDefaultSelection(request.getDefaultSelection());
        }
    }

    public Page<BlockingReasonResponse> filter(BlockingReasonFilterRequest request) {
        log.debug("Filtering Blocking Reason list with request: {}", request.toString());

        Page<BlockingReason> page = blockingReasonRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        Objects.isNull(request.getType()) ? null : request.getType().name(),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return page.map(BlockingReasonResponse::new);
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.BLOCKING_REASON, permissions = {PermissionEnum.NOMENCLATURE_VIEW})
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering Blocking Reasons list with statuses: {}", request);
        return blockingReasonRepository
                .filterNomenclature(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.BLOCKING_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Moving item with ID: {} in Blocking Reason to top", request.getId());

        BlockingReason blockingReason = blockingReasonRepository
                .findById(request.getId())
                .orElseThrow(() -> new ClientException("id-Blocking Reason with presented id not found", DOMAIN_ENTITY_NOT_FOUND));

        Long start;
        Long end;
        List<BlockingReason> blockingReasonsList;

        if (blockingReason.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = blockingReason.getOrderingId();
            blockingReasonsList = blockingReasonRepository.findInOrderingIdRange(
                    start,
                    end,
                    blockingReason.getId(),
                    Sort.by(Sort.Direction.ASC, "orderingId")
            );
            long tempOrderingId = request.getOrderingId() + 1;
            for (BlockingReason br : blockingReasonsList) {
                br.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = blockingReason.getOrderingId();
            end = request.getOrderingId();
            blockingReasonsList = blockingReasonRepository.findInOrderingIdRange(
                    start,
                    end,
                    blockingReason.getId(),
                    Sort.by(Sort.Direction.DESC, "orderingId"));

            long tempOrderingId = request.getOrderingId() - 1;
            for (BlockingReason br : blockingReasonsList) {
                br.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        blockingReason.setOrderingId(request.getOrderingId());
        blockingReasonRepository.save(blockingReason);
        blockingReasonRepository.saveAll(blockingReasonsList);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.BLOCKING_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void sortAlphabetically() {
        log.debug("Sorting the Blocking Reason alphabetically");
        List<BlockingReason> blockingReasons = blockingReasonRepository.orderByName();
        long orderingId = 1;

        for (BlockingReason reasons : blockingReasons) {
            reasons.setOrderingId(orderingId);
            orderingId++;
        }

        blockingReasonRepository.saveAll(blockingReasons);
    }

    @Override
    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = PermissionContextEnum.BLOCKING_REASON, permissions = {NOMENCLATURE_EDIT})
            }
    )
    public void delete(Long id) {
        log.debug("Removing Blocking Reason with ID: {}", id);

        BlockingReason blockingReason = blockingReasonRepository
                .findById(id)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("id-Blocking reason not found")
                );

        if (blockingReason.isHardCoded()) {
            log.error("Can't delete the hardcoded nomenclature");
            throw new OperationNotAllowedException("id-Can't delete the hardcoded nomenclature;");
        }

        if (blockingReason.getStatus().equals(DELETED)) {
            log.error("Item is already deleted");
            throw new ClientException("id-Item is already deleted.", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Optional<String> activeConnection = blockingReasonRepository.activeConnections(id);

        if (activeConnection.isPresent()) {
            log.error("Can't delete the nomenclature because it is connected to {}", activeConnection.get());
            throw new OperationNotAllowedException("id-You can't delete the nomenclature because it is connected to %s".formatted(activeConnection.get()));
        }

        blockingReason.setDefaultSelection(false);
        blockingReason.setStatus(DELETED);
        blockingReasonRepository.save(blockingReason);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return blockingReasonRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return blockingReasonRepository.findByIdIn(ids);
    }
}
