package bg.energo.phoenix.service.receivable.massOperationForBlocking;

import bg.energo.phoenix.exception.AccessDeniedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.permissions.PermissionContextEnum.RECEIVABLE_BLOCKING;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableBlockingPermissionService {

    private final PermissionService permissionService;

    public ReceivableBlockingStatus checkAndGetBlockingStatusByPermissionOnCreate(ReceivableBlockingStatus requestedStatus) {
        ReceivableBlockingStatus receivableBlockingStatus = null;
        switch (requestedStatus) {
            case DRAFT -> {
                if (hasCreateDraftPermission()) {
                    receivableBlockingStatus = ReceivableBlockingStatus.DRAFT;
                } else {
                    log.error("You do not have permission to create draft receivable blocking object.");
                    throw new AccessDeniedException("You do not have permission to create draft receivable blocking object.");
                }
            }
            case EXECUTED -> {
                if (hasCreateExecutedPermission()) {
                    receivableBlockingStatus = ReceivableBlockingStatus.EXECUTED;
                } else {
                    log.error("You do not have permission to create and execute receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to create and execute receivable blocking.");
                }
            }
        }
        return receivableBlockingStatus;
    }

    public ReceivableBlockingStatus checkAndGetBlockingStatusByPermissionOnEdit(ReceivableBlockingStatus requestedStatus) {
        ReceivableBlockingStatus receivableBlockingStatus = null;
        switch (requestedStatus) {
            case DRAFT -> {
                if (hasEditDraftPermission()) {
                    receivableBlockingStatus = ReceivableBlockingStatus.DRAFT;
                } else {
                    log.error("You do not have permission to edit draft receivable blocking object.");
                    throw new AccessDeniedException("You do not have permission to edit draft receivable blocking object.");
                }
            }
            case EXECUTED -> {
                if (hasEditExecutedPermission()) {
                    receivableBlockingStatus = ReceivableBlockingStatus.EXECUTED;
                } else {
                    log.error("You do not have permission to edit executed receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to edit executed receivable blocking.");
                }
            }
        }
        return receivableBlockingStatus;
    }

    public List<String> filterBlockingStatusByPermissionOnListing() {
        List<String> statuses = new ArrayList<>();
        if (hasViewActiveDraftPermission()) {
            statuses.add(ReceivableBlockingStatus.DRAFT.name());
        }

        if (hasViewActiveExecutedPermission()) {
            statuses.add(ReceivableBlockingStatus.EXECUTED.name());
        }
        return statuses;
    }

    public List<String> filterBlockingDeletedStatusByPermissionOnListing() {
    List<String> statuses = new ArrayList<>();
    if (hasViewDeletedDraftPermission()) {
        statuses.add(ReceivableBlockingStatus.DRAFT.name());
    }

    if (hasViewDeletedExecutedPermission()) {
        statuses.add(ReceivableBlockingStatus.EXECUTED.name());
    }

    if (!statuses.isEmpty()) {
        statuses.add(EntityStatus.DELETED.name());
    }
    return statuses;
}

    public void checkOnViewPermissionsByStatuses(EntityStatus entityStatus, ReceivableBlockingStatus blockingStatus) {
        switch (entityStatus) {
            case ACTIVE -> checkOnBlockingStatusWhenActive(blockingStatus);
            case DELETED -> checkOnBlockingStatusWhenDeleted(blockingStatus);
        }
    }

    public void checkOnDeletePermissionByBlockingStatus(ReceivableBlockingStatus blockingStatus) {
        switch (blockingStatus) {
            case DRAFT -> {
                if (!hasDeleteDraftPermission()) {
                    log.error("You do not have permission to delete draft receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to delete draft receivable blocking.");
                }
            }
            case EXECUTED -> {
                if (!hasDeleteExecutedPermission()) {
                    log.error("You do not have permission to delete executed receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to delete executed receivable blocking.");
                }
            }
        }
    }

    private void checkOnBlockingStatusWhenActive(ReceivableBlockingStatus blockingStatus) {
        switch (blockingStatus) {
            case DRAFT -> {
                if (!hasViewActiveDraftPermission()) {
                    log.error("You do not have permission to view draft receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to view draft receivable blocking.");
                }
            }
            case EXECUTED -> {
                if (!hasViewActiveExecutedPermission()) {
                    log.error("You do not have permission to view executed receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to view executed receivable blocking.");
                }
            }
        }
    }

    private void checkOnBlockingStatusWhenDeleted(ReceivableBlockingStatus blockingStatus) {
        switch (blockingStatus) {
            case DRAFT -> {
                if (!hasViewDeletedDraftPermission()) {
                    log.error("You do not have permission to view deleted draft receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to view deleted draft receivable blocking.");
                }
            }
            case EXECUTED -> {
                if (!hasViewDeletedExecutedPermission()) {
                    log.error("You do not have permission to view deleted executed receivable blocking.");
                    throw new AccessDeniedException("You do not have permission to view deleted executed receivable blocking.");
                }
            }
        }
    }

    private boolean hasCreateDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_CREATE_AS_DRAFT));
    }

    private boolean hasCreateExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_CREATE_AS_EXECUTE));
    }

    private boolean hasDeleteExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_DELETE_EXECUTED));
    }

    private boolean hasDeleteDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_DELETE_DRAFT));
    }

    private boolean hasViewActiveDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_VIEW_DRAFT));
    }

    private boolean hasViewDeletedDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_VIEW_DELETED_DRAFT));
    }

    private boolean hasViewActiveExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_VIEW_EXECUTED));
    }

    private boolean hasViewDeletedExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_VIEW_DELETED_EXECUTED));
    }

    private boolean hasEditDraftPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_EDIT_DRAFT));
    }

    private boolean hasEditExecutedPermission() {
        return checkOnPermission(List.of(PermissionEnum.RECEIVABLE_BLOCKING_EDIT_EXECUTED));
    }

    private boolean checkOnPermission(List<PermissionEnum> requiredPermissions) {
        return permissionService.permissionContextContainsPermissions(RECEIVABLE_BLOCKING, requiredPermissions);
    }

}
