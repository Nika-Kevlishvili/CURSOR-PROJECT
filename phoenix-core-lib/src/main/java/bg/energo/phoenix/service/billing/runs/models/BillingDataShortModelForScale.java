package bg.energo.phoenix.service.billing.runs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class BillingDataShortModelForScale  extends BillingDataShortModel{
    private Long billingByScalesId;

    public BillingDataShortModelForScale(String podIdentifier, Long podId, LocalDate billingFrom, LocalDate billingTo, LocalDateTime createDate, Long billingByScalesId) {
        super(podId, billingFrom, billingTo, createDate);
        this.billingByScalesId = billingByScalesId;
    }
}
