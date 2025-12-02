package bg.energo.phoenix.model.response.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemActivityListResponse {

    private Long id;

    private String activity;

    private String subActivity;

    private String connectionType;

    private LocalDateTime creationDate;

    private EntityStatus status;

    public SystemActivityListResponse(Long id,
                                      String activity,
                                      String subActivity,
                                      String connectionType,
                                      LocalDateTime creationDate,
                                      EntityStatus status) {
        this.id = id;
        this.activity = activity;
        this.subActivity = subActivity;
        this.connectionType = connectionType;
        this.creationDate = creationDate;
        this.status = status;
    }
}
