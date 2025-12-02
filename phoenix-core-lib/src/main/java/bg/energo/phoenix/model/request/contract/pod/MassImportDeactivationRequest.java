package bg.energo.phoenix.model.request.contract.pod;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MassImportDeactivationRequest {
    private String podIdentifier;
    private LocalDate deactivationDate;

    public MassImportDeactivationRequest(String podIdentifier, LocalDate deactivationDate) {
        this.podIdentifier = podIdentifier;
        this.deactivationDate = deactivationDate;
    }
}
