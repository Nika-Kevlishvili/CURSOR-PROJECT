package bg.energo.phoenix.model.response.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import lombok.Data;

import java.util.List;

@Data
public class SystemActivityResponse {

    private Long id;

    private Long activityId;

    private String activityName;

    private Long subActivityId;

    private String subActivityName;

    private SystemActivityConnectionType connectionType;

    private Long connectedObjectId; // This is the ID of the domain object (e.g. customer ID, contract ID, etc.)

    private String connectedObjectName; // This is the name/number of the domain object (e.g. customer name, task number, etc.)

    private List<SystemActivityJsonFieldResponse> fields;

    private EntityStatus status;

}
