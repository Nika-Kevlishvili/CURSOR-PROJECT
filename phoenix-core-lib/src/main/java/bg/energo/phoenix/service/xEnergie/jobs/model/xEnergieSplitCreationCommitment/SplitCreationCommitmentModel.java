package bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitCreationCommitment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SplitCreationCommitmentModel {
    private Long id;
    private LocalDate activationDate;
    private LocalDate deactivationDate;
    private String identifier;
    private String dealNumber;
}
