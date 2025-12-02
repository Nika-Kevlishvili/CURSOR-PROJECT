package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionToCbgFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ObjectionToChangeOfCbgResponse {

    private String number;

    private EntityStatus status;

    private GridOperatorResponse gridOperatorResponse;

    private LocalDate creationDate;

    private ChangeOfCbgStatus changeOfCbgStatus;

    private LocalDate dateOfChange;

    private List<TaskShortResponse> taskShortResponse;

    private List<ShortResponse> withdrawalShortResponse;
    private List<ContractTemplateShortResponse> templateResponses;
    private ContractTemplateShortResponse emailTemplateResponse;
    private List<ObjectionToCbgFileResponse> subFiles;

    public ObjectionToChangeOfCbgResponse(ObjectionToChangeOfCbg objection) {
        this.number = objection.getChangeOfCbgNumber();
        this.changeOfCbgStatus = objection.getChangeOfCbgStatus();
        this.dateOfChange = objection.getChangeDate();
        this.status = objection.getStatus();
    }
}
