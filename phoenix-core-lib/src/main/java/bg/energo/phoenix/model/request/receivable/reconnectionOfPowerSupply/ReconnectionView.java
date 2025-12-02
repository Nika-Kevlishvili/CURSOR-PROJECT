package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.ReconnectionOfPowerSupplyFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReconnectionView {

    private ShortResponse gridOperatorResponse;
    private LocalDateTime creationDateAndTime;
    private ReconnectionStatus reconnectionStatus;
    private EntityStatus generalStatus;
    private List<ReconnectionOfPowerSupplyFileResponse> files;
    private List<TaskShortResponse> tasks;
    private List<ContractTemplateShortResponse> templateResponses;

}
