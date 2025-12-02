package bg.energo.phoenix.model.request.pod.billingByProfile.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingByProfileDataJson {
    private LocalDateTime f;
    private LocalDateTime t;
    private BigDecimal v;
    private Boolean s;
}
