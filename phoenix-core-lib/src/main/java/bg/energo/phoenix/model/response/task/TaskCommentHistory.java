package bg.energo.phoenix.model.response.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommentHistory {
    private String commenter;
    private LocalDateTime createDate;
    private String comment;
}
