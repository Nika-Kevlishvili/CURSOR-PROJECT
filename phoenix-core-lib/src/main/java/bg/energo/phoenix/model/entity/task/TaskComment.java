package bg.energo.phoenix.model.entity.task;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "task_comments", schema = "task")
public class TaskComment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_comment_seq")
    @SequenceGenerator(name = "task_comment_seq", schema = "task", sequenceName = "task_comments_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "comment")
    private String comment;

    @Column(name = "task_id")
    private Long taskId;
}