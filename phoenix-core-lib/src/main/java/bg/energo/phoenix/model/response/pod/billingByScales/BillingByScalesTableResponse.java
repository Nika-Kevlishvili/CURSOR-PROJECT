package bg.energo.phoenix.model.response.pod.billingByScales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class BillingByScalesTableResponse {
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String meterNumber;
    private Long scaleId;
    private String scaleNumber;
    private String scaleCode;
    private String scaleType;
    //private String timeZone;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private BigDecimal difference;
    private BigDecimal multiplier;
    private BigDecimal correction;
    private BigDecimal deducted;
    private BigDecimal totalVolumes;
    private String tariffScale;
    private BigDecimal volumes;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private Integer index;

}
