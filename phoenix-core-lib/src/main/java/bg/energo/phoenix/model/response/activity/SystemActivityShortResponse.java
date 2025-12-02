package bg.energo.phoenix.model.response.activity;

import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemActivityShortResponse {

    private Long id;
    private LocalDateTime createDate;
    private String name;

    public SystemActivityShortResponse(SystemActivity systemActivity,
                                       Activity activity,
                                       SubActivity subActivity,
                                       LocalDateTime createDate) {

        this.id = systemActivity.getId();
        this.createDate = createDate;
        this.name = "%s - %s".formatted(
                activity.getName(),
                subActivity.getName()
        );
    }
}
