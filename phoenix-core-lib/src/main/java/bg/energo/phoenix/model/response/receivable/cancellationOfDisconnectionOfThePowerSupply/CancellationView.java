package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
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
public class CancellationView {
    private Long id;
    private Long requestForDisconnectionId;
    private String requestForDisconnectionNumber;
    private String cancellationNumber;
    private LocalDateTime creationDateAndTime;
    private CancellationOfDisconnectionOfThePowerSupplyStatus cancellationStatus;
    private EntityStatus entityStatus;
    private List<CancellationFileResponse> files;
    private List<TaskShortResponse> tasks;
    private List<ContractTemplateShortResponse> templateResponses;
}
