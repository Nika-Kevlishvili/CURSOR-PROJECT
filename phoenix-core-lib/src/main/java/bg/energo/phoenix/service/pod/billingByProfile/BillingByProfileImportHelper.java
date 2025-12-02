package bg.energo.phoenix.service.pod.billingByProfile;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingByProfileImportHelper {
    private BigDecimal value;
    private int rowNum;
    private String time;
}
