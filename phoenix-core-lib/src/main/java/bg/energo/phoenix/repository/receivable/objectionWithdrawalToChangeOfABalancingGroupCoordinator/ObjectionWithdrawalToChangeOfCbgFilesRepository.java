package bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface ObjectionWithdrawalToChangeOfCbgFilesRepository extends JpaRepository<ObjectionWithdrawalToChangeOfCbgFiles, Long> {
    @Query("""
                    select f from ObjectionWithdrawalToChangeOfCbgFiles f
                    where f.id in :ids
                    and f.status in :statuses
            """)
    Set<ObjectionWithdrawalToChangeOfCbgFiles> findByIdsAndStatuses(Collection<Long> ids, List<EntityStatus> statuses);

    Set<ObjectionWithdrawalToChangeOfCbgFiles> findByObjWithdrawalToChangeOfCbgIdAndStatus(Long id, EntityStatus status);
}
