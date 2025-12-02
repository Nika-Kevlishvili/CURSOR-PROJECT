package bg.energo.phoenix.repository.task;

import bg.energo.phoenix.model.entity.task.TaskStage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskStageRepository extends JpaRepository<TaskStage, Long> {
    @Query("""
            select ts from TaskStage ts
            where ts.taskId = :id
            order by ts.stage
            """)
    List<TaskStage> findAllByTaskId(@Param("id") Long id);

    @Query("""
            select ts from TaskStage ts
            where ts.completionDate is null
            and ts.taskId = :id
            order by ts.stage
            """)
    TaskStage findTaskCurrentStage(@Param("id") Long id, PageRequest request);
}