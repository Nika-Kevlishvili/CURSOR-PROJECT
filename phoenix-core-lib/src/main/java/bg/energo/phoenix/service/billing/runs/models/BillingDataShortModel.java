package bg.energo.phoenix.service.billing.runs.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingDataShortModel {

    private Long podId;

    private LocalDate billingFrom;
    private LocalDate billingTo;
    private LocalDateTime createDate;
}
