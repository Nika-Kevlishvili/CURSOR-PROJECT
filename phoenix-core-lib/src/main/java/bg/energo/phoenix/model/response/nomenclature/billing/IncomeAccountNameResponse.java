package bg.energo.phoenix.model.response.nomenclature.billing;

import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeAccountNameResponse {
    private Long id;
    private String name;
    private String number;
    private Long orderingId;
    private boolean defaultSelection;
    private NomenclatureItemStatus status;
    private String systemUserId;
    private List<DefaultAssignmentType> defaultAssignmentType;

    public IncomeAccountNameResponse(IncomeAccountName incomeAccountName){
        this.id = incomeAccountName.getId();
        this.name = incomeAccountName.getName();
        this.number = incomeAccountName.getNumber();
        this.orderingId = incomeAccountName.getOrderingId();
        this.defaultSelection = incomeAccountName.isDefaultSelection();
        this.status = incomeAccountName.getStatus();
        this.systemUserId = incomeAccountName.getSystemUserId();
        this.defaultAssignmentType = incomeAccountName.getDefaultAssignmentType();
    }
}
