package bg.energo.phoenix.model.response.task;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskListingResponse {
    private Long id;
    private String taskType;
    private String currentPerformer;
    private String performer;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate completionDate;
    private TaskConnectionType connectionType;
    private EntityStatus status;
    private LocalDateTime createDateTime;
    private TaskStatus taskStatus;

    public TaskListingResponse(TaskListingMiddleResponse listing) {
        this.id = listing.getId();
        this.taskType = listing.getType();
        this.currentPerformer = listing.getCurrentPerformer();
        this.performer = listing.getPerformer();
        if (listing.getStartDate() != null) {
            this.startDate = LocalDate.parse(listing.getStartDate());
        }
        if (listing.getEndDate() != null) {
            this.endDate = LocalDate.parse(listing.getEndDate());
        }
        if (listing.getCompletionDate() != null) {
            this.completionDate = LocalDate.parse(listing.getCompletionDate());
        }
        this.connectionType = TaskConnectionType.valueOf(listing.getConnectionType());
        this.status = EntityStatus.valueOf(listing.getStatus());
        this.createDateTime = listing.getCreateDate();
        if (listing.getCurrentStatus() != null) {
            this.taskStatus = TaskStatus.valueOf(listing.getCurrentStatus());
        }
    }
}
