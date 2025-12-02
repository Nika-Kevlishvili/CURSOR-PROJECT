package bg.energo.phoenix.model.request.contract.pod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractModelMassImport {
    private Long contractId;
    private LocalDate signingDate;
    private LocalDate versionStartDate;
    private LocalDate podActivationDate;
}
