package bg.energo.phoenix.model.response.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivity;
import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivityJsonField;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

import java.util.List;

@Data
public class SubActivityResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;
    private Long activityId;
    private String activityName;
    private List<SubActivityJsonField> fields;

    public SubActivityResponse(SubActivity subActivity) {
        this.id = subActivity.getId();
        this.name = subActivity.getName();
        this.orderingId = subActivity.getOrderingId();
        this.defaultSelection = subActivity.isDefaultSelection();
        this.status = subActivity.getStatus();
        this.systemUserId = subActivity.getSystemUserId();
        this.fields = subActivity.getFields();
        this.activityId = subActivity.getActivity().getId();
        this.activityName = subActivity.getActivity().getName();
    }
}
