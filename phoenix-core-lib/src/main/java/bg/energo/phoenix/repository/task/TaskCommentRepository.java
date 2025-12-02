package bg.energo.phoenix.repository.task;

import bg.energo.phoenix.model.entity.task.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    @Query("""
            select tc from TaskComment tc
            where tc.taskId = :id
            order by tc.createDate
            """)
    List<TaskComment> findAllByTaskId(@Param("id") Long id);
}