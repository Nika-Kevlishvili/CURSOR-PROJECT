package bg.energo.phoenix.model.response.pod.billingByScales;

import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScaleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingByScaleListResponse {
    private Long id;
    private String identifier;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String invoiced;
    private BillingByScaleStatus status;
    private LocalDateTime createDate;
}
