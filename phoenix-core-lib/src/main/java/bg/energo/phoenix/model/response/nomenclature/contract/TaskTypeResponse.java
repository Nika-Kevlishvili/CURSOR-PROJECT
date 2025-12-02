package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TaskTypeResponse {
    private Long id;
    private String name;
    private CalendarShortResponse calendar;
    private List<TaskTypeStageResponse> taskTypeStages;
    private Long orderingId;
    private Boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;
}
