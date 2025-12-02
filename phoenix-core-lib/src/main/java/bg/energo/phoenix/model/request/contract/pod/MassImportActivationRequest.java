package bg.energo.phoenix.model.request.contract.pod;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MassImportActivationRequest {
    private String podIdentifier;
    private LocalDate activationDate;

    public MassImportActivationRequest(String podIdentifier, LocalDate activationDate) {
        this.podIdentifier = podIdentifier;
        this.activationDate = activationDate;
    }
}
