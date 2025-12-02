package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskTypeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTypeStagesRepository extends JpaRepository<TaskTypeStage, Long> {
    @Query("""
            select ttd from TaskTypeStage ttd
            where ttd.taskTypeId = :id
            and ttd.status in (:statuses)
            """)
    List<TaskTypeStage> findAllByTaskTypeIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query("""
            select ttd from TaskTypeStage ttd
            where ttd.taskTypeId = :taskTypeId
            and ttd.id = :id
            and ttd.status = 'ACTIVE'
            """)
    Optional<TaskTypeStage> findByIdAndTaskTypeId(Long id, Long taskTypeId);
}
