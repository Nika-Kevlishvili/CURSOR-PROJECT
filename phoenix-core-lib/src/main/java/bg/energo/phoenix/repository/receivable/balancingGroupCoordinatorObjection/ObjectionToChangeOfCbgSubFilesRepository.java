package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgSubFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ObjectionToChangeOfCbgSubFilesRepository extends JpaRepository<ObjectionToChangeOfCbgSubFiles, Long> {
    @Query("""
                    select f from ObjectionToChangeOfCbgSubFiles f
                    where f.id in :ids
                    and f.status in :statuses
            """)
    Set<ObjectionToChangeOfCbgSubFiles> findByIdsAndStatuses(List<Long> ids, List<EntityStatus> statuses);

    Set<ObjectionToChangeOfCbgSubFiles> findByObjToChangeOfCbgIdAndStatus(Long id, EntityStatus status);

    @Query("""
        select cbgf.id
        from ObjectionToChangeOfCbgSubFiles cbgf
        where cbgf.objToChangeOfCbgId = :cbgId
    """)
    List<Long> findFileIdsByCbgId(@Param("cbgId") Long cbgId);
}
