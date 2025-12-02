package bg.energo.phoenix.model.request.contract.pod;

import bg.energo.phoenix.model.customAnotations.contract.pod.PodManualValidator;
import lombok.Data;

import java.time.LocalDate;

@Data
@PodManualValidator
public class PodManualActivationRequest {

    private Long podDetailId;
    private Long contractDetailId;
    private LocalDate activationDate;
    private LocalDate deactivationDate;
    private Long deactivationPurposeId;
}
