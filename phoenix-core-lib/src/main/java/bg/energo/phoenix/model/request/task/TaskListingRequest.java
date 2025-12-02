package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.enums.task.TaskListingColumns;
import bg.energo.phoenix.model.enums.task.TaskListingSortColumns;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TaskListingRequest {

    private TaskListingColumns columnName;

    @NotNull(message = "columnValue-columnValue can not be null")
    private String prompt = "";

    @NotNull(message = "connectionType-connectionType can not be null")
    private List<TaskConnectionType> connectionType = new ArrayList<>();

    @NotNull(message = "performerDirection-performerDirection can not be null")
    private Sort.Direction performerDirection = Sort.Direction.ASC;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateFrom;

    @NotNull(message = "performer-performer can not be null")
    private List<Long> performer = new ArrayList<>();

    @NotNull(message = "currentPerformer-currentPerformer can not be null")
    private List<Long> currentPerformer = new ArrayList<>();

    @NotNull(message = "currentPerformer-currentPerformer can not be null")
    private List<Long> taskTypes = new ArrayList<>();

    private List<String> taskStatuses = new ArrayList<>();

    @NotNull(message = "size-size can not be null")
    private Integer size = 25;

    @NotNull(message = "page-page can not be null")
    private Integer page = 0;

    @NotNull(message = "sortDirection-sortDirection can not be null")
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    @NotNull(message = "sortBy-sortBy can not be null")
    private TaskListingSortColumns sortBy = TaskListingSortColumns.ID;

}
