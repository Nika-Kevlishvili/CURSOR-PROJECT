package bg.energo.phoenix.process.model.request;

import bg.energo.phoenix.process.model.enums.ProcessSearchField;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessTableColumn;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProcessListRequest {

    @NotNull(message = "page-Page must not be null")
    private int page;

    @NotNull(message = "size-Size must not be null")
    private int size;

    private String prompt;

    private ProcessSearchField searchBy;

    private ProcessTableColumn sortBy;

    private Sort.Direction sortDirection;

    private ProcessStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime completeDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime completeDateTo;

}
