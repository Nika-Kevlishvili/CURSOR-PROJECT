package bg.energo.phoenix.repository.lock;

import bg.energo.phoenix.model.entity.lock.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LockRepository extends JpaRepository<Lock, String> {

    /**
     * Finds all locks by their unique key.
     * Note: Multiple locks with the same lock key will only exist when the billing run process locks 
     * the same object simultaneously; otherwise, there will only be a single instance.
     *
     * @param lockKey the unique identifier for the lock
     * @return a list of locks associated with the specified lockKey
     */
    List<Lock> findAllByLockKey(String lockKey);
}
