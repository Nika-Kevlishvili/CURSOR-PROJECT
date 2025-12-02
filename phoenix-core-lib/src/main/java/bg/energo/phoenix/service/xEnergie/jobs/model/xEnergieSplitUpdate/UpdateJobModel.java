package bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitUpdate;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
public class UpdateJobModel {
    private Long contractPodId;
    private LocalDateTime customModifyDate;
    private Long splitId;
    private LocalDate activationDate;
    private LocalDate deactivationDate;
    private Long podDetailId;
    private String dealNumber;
    private Long podGridOperator;
    private String podIdentifier;
}
