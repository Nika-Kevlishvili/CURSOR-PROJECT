package bg.energo.phoenix.service.lock;

import bg.energo.phoenix.exception.LockException;
import bg.energo.phoenix.model.entity.lock.Lock;
import bg.energo.phoenix.model.response.PermissionResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.lock.LockRepository;
import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    public static final String LOCKED_BY_MESSAGE = "This object is in editing by %s %s.";
    public static final String CANNOT_SAVE_MESSAGE = "Another system user is already editing this object. Saving is not possible.";
    public static final String SYSTEM_LOCK_MESSAGE = "This object is locked by the billing run process(es)%s";
    public static final String EXPIRED_EDITTING_TIME_MESSAGE = "Your editing time expired.";
    public static final String LOCK_IS_NOT_OWNED_MESSAGE = "This object is not locked.";

    @Value("${lock.minutes-before-expiration:60}")
    private int minutesBeforeLockExpiration;

    private final LockRepository lockRepository;
    private final PermissionService permissionService;

    /**
     * Acquires a lock for the specified entity type and entity id with the provided version id.
     * If a lock already exists for the given lock key, handles the lock state accordingly based on the lock's
     * current state, the current user, and expiration rules.
     *
     * @param entityType the type of the entity for which the lock is being acquired
     * @param entityId the unique identifier of the entity for which the lock is being acquired
     * @param versionId the version identifier of the entity to include in the lock
     * @return the acquired or updated lock for the specified entity
     * @throws LockException if the lock cannot be acquired due to locking rules or constraints
     */
    @Transactional
    public Lock acquireLock(String entityType, Long entityId, Long versionId) {
        String lockKey = Lock.generateLockKey(entityType, entityId, null);
        List<Lock> lockList = lockRepository.findAllByLockKey(lockKey);
        checkForSystemLock(lockList);
        LocalDateTime expiration = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(minutesBeforeLockExpiration);

        if (!lockList.isEmpty()) {
            Lock currentLock = lockList.get(0);
            String loggedInUserFullName = getLoggedInUserFullName();
            LockState lockState = determineLockState(currentLock, loggedInUserFullName);
            return switch (lockState) {
                case OWNED_BY_USER -> updateAndSaveLock(currentLock, loggedInUserFullName, expiration, currentLock.hasSuperOwner());
                case OVERRIDABLE -> updateAndSaveLock(currentLock, loggedInUserFullName, expiration, true);
                case EXPIRED -> updateAndSaveLock(currentLock, loggedInUserFullName, expiration, false);
                case SUPER_OWNER -> throw new LockException(CANNOT_SAVE_MESSAGE);
                case LOCKED -> throw new LockException(String.format(LOCKED_BY_MESSAGE, currentLock.getLockOwner(), currentLock.getFormattedCreatedAt()));
            };
        }
        return createLock(entityType, entityId, versionId, lockKey, expiration);
    }

    /**
     * Releases the lock associated with the specified entity type, entity ID,
     * and version ID. If a lock exists, it verifies the ownership and deletes it.
     *
     * @param entityType the type of the entity for which the lock is to be released
     * @param entityId the identifier of the entity for which the lock is to be released
     * @param versionId the version identifier of the entity for which the lock is to be released
     */
    public void releaseLock(String entityType, Long entityId, Long versionId) {
        try {
            List<Lock> foundLocks = lockRepository.findAllByLockKey(Lock.generateLockKey(entityType, entityId, versionId));
            checkForSystemLock(foundLocks);
            ensureLockExists(foundLocks, LOCK_IS_NOT_OWNED_MESSAGE);
            validateOwnership(foundLocks);
            lockRepository.deleteAll(foundLocks);
        } catch (Exception e){
            log.debug(e.getMessage(), e);
        }
    }

    /**
     * Checks the ownership of a specified entity and performs cleanup if the associated lock is expired.
     *
     * @param entityType the type of the entity to check
     * @param entityId the unique identifier of the entity
     * @param versionId the version identifier of the entity
     * @return true if a valid, non-expired lock exists for the entity; false otherwise
     */
    public boolean checkOwnershipAndCleanupIfExpired(String entityType, Long entityId, Long versionId) {
        List<Lock> lockList = findValidLock(entityType, entityId, versionId);
        return !lockList.isEmpty() && !lockList.get(0).isExpired();
    }

    /**
     * Checks the validity of a lock for a given entity by its type, ID, and version ID.
     *
     * @param entityType the type of the entity for which the lock validity is being checked
     * @param entityId the ID of the entity for which the lock validity is being checked
     * @param versionId the version ID of the entity for which the lock validity is being checked
     */
    public void checkLockValidity(String entityType, Long entityId, Long versionId) {
        List<Lock> lockList = findValidLock(entityType, entityId, versionId);
        ensureLockExists(lockList, CANNOT_SAVE_MESSAGE);
    }

    /**
     * Ensures that an entity is not locked by verifying the associated lock status.
     * If the entity is locked and the lock has not expired, a LockException is thrown.
     *
     * @param entityType the type of the entity to check for a lock (e.g., name of the entity class).
     * @param entityId the unique identifier of the entity whose lock status is to be verified.
     * @param versionId the version identifier of the entity to differentiate specific versions for locking.
     */
    @Transactional(readOnly = true)
    public void ensureEntityNotLocked(String entityType, Long entityId, Long versionId) {
        List<Lock> lockList = lockRepository.findAllByLockKey(Lock.generateLockKey(entityType, entityId, versionId));
        checkForSystemLock(lockList);
        if (!lockList.isEmpty()) {
            Lock lock = lockList.get(0);
            if (isOwnedByCurrentUser(lock) && !lock.isExpired()) {
                return;
            }
            if (!lock.isExpired()) {
                throw new LockException(String.format(LOCKED_BY_MESSAGE, lock.getLockOwner(), lock.getFormattedCreatedAt()));
            }
        }
    }

    /**
     * Retrieves the full name of the currently logged-in user by combining their first and last name.
     *
     * @return A String representing the full name of the logged-in user in the format "FirstName LastName".
     */
    public String getLoggedInUserFullName() {
        return permissionService.getUserPermissionResponse().getUserFirstName() + " " + permissionService.getUserPermissionResponse().getUserLastName();
    }

    /**
     * Checks the list of locks for any system locks. If system locks are found, it throws a
     * {@link LockException} with a message containing the details of the locks, including their billing IDs if available.
     *
     * @param lockList the list of {@link Lock} objects to be checked for system locks
     * @throws LockException if one or more system locks are found
     */
    private static void checkForSystemLock(List<Lock> lockList) {
        List<Lock> systemLocks = lockList.stream()
                .filter(Lock::isSystemLock)
                .toList();
        if (!systemLocks.isEmpty()){
            String message = ".";
            List<String> billingIds = systemLocks.stream()
                    .map(Lock::getBillingId)
                    .filter(Objects::nonNull)
                    .map(BigInteger::toString)
                    .toList();
            if (!billingIds.isEmpty()) {
                message = String.format(" with id(s): %s.", String.join(", ", billingIds));
            }
            throw new LockException(String.format(SYSTEM_LOCK_MESSAGE, message));
        }
    }

    private List<Lock> findValidLock(String entityType, Long entityId, Long versionId) {
        List<Lock> lockList = lockRepository.findAllByLockKey(Lock.generateLockKey(entityType, entityId, versionId));
        checkForSystemLock(lockList);
        if (lockList.isEmpty()) {
            throw new LockException(LOCK_IS_NOT_OWNED_MESSAGE);
        }
        return lockList.stream()
                .map(this::validateLock)
                .toList();
    }

    private Lock validateLock(Lock lock) {
        if (isOwnedByCurrentUser(lock) && lock.isExpired()) {
            releaseLock(lock.getEntityType(), lock.getEntityId(), lock.getVersionId());
            throw new LockException(EXPIRED_EDITTING_TIME_MESSAGE);
        } else if (!isOwnedByCurrentUser(lock) && !lock.isExpired()) {
            throw new LockException(CANNOT_SAVE_MESSAGE);
        } else {
            return lock;
        }
    }

    private void validateOwnership(List<Lock> lockList) {
        if (!lockList.isEmpty() && !isOwnedByCurrentUser(lockList.get(0))) {
            throw new LockException(String.format(LOCKED_BY_MESSAGE, lockList.get(0).getLockOwner(), lockList.get(0).getFormattedCreatedAt()));
        }
    }

    private static void ensureLockExists(List<Lock> lockList, String message) {
        if (lockList.isEmpty()) {
            throw new LockException(message);
        }
    }

    private boolean isOwnedByCurrentUser(Lock lock) {
        String loggedInUser = getLoggedInUserFullName();
        return loggedInUser != null && loggedInUser.equals(lock.getLockOwner());
    }

    private boolean hasOverrideParallelEditLockPermission() {
        return permissionService.getUserPermissionResponse().getPermissionContexts().stream()
                .map(PermissionResponse.PermissionContext::getId)
                .anyMatch(PermissionEnum.OVERRIDE_PARALLEL_EDIT_LOCK.getId()::equals);
    }

    private Lock createLock(String entityType, Long entityId, Long versionId, String lockKey, LocalDateTime expiration) {
        Lock newLock = new Lock();
        newLock.setEntityType(entityType);
        newLock.setEntityId(entityId);
        newLock.setVersionId(versionId);
        newLock.setLockKey(lockKey);
        newLock.setLockOwner(getLoggedInUserFullName());
        newLock.setExpiresAt(expiration);
        if (hasOverrideParallelEditLockPermission()) {
            newLock.setHasSuperOwner(true);
        }
        return lockRepository.save(newLock);
    }

    private Lock updateAndSaveLock(Lock lock, String lockOwner, LocalDateTime expiration, boolean hasSuperOwner) {
        lock.setLockOwner(lockOwner);
        lock.setExpiresAt(expiration);
        lock.setHasSuperOwner(hasSuperOwner);
        return lockRepository.save(lock);
    }

    private LockState determineLockState(Lock currentLock, String loggedInUserFullName) {
        if (isOwnedByCurrentUser(currentLock) && currentLock.getLockOwner().equals(loggedInUserFullName)) {
            return LockState.OWNED_BY_USER;
        }
        if (hasOverrideParallelEditLockPermission()) {
            return LockState.OVERRIDABLE;
        }
        if (currentLock.isExpired()) {
            return LockState.EXPIRED;
        }
        if (currentLock.hasSuperOwner()) {
            return LockState.SUPER_OWNER;
        }
        return LockState.LOCKED;
    }

    private enum LockState {
        OWNED_BY_USER,
        OVERRIDABLE,
        EXPIRED,
        SUPER_OWNER,
        LOCKED
    }
}
