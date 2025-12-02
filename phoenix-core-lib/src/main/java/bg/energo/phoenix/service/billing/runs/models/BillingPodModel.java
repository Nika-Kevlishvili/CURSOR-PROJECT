package bg.energo.phoenix.service.billing.runs.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BillingPodModel {

    private Long detailId;
    private Long podId;

    private LocalDate activationDate;
    private LocalDate deactivationDate;
}
