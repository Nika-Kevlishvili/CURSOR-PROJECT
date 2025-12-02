package bg.energo.phoenix.model.response.task;

import bg.energo.phoenix.model.enums.task.ConnectedEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskConnectedEntityResponse {
    private Long id;

    private String name;

    private ConnectedEntityType entityType;

    private LocalDateTime createDate;

    public TaskConnectedEntityResponse(Long id, String name, LocalDateTime createDate) {
        this.id = id;
        this.name = StringUtils.normalizeSpace(Objects.requireNonNullElse(name, " "));
        this.createDate = createDate;
    }

    public TaskConnectedEntityResponse(Long id, String name, LocalDateTime createDate, String customerIdentifier) {
        this.id = id;
        this.name = formatCustomerIdentifier(name, customerIdentifier);
        this.createDate = createDate;
    }

    private String formatCustomerIdentifier(String name, String customerIdentifier) {
        return String.format("%s (%s)", Objects.requireNonNullElse(customerIdentifier, " "), StringUtils.normalizeSpace(Objects.requireNonNullElse(name, " ")));
    }

    public TaskConnectedEntityResponse(Long id,LocalDateTime createDate) {
        this.id = id;
        this.createDate = createDate;
    }

}
