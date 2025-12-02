package bg.energo.phoenix.model.request.pod.billingByScales;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingByScalesTableCreateRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodTo;

    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "billingByScalesTableCreateRequests.meterNumber-Allowed symbols in number are: A/Z a/z 0/9;")
    @Size(min = 1, max = 32, message = "billingByScalesTableCreateRequests.meterNumber-Meter number should be between {min} and {max} characters;")
    private String meterNumber;

    @Pattern(regexp = "^[0-9]+$", message = "billingByScalesTableCreateRequests.scaleNumber-Allowed symbols in number are: A/Z a/z 0/9;")
    @Size(min = 0, max = 9, message = "billingByScalesTableCreateRequests.scaleNumber-Scale Number should be between {min} and {max} characters;")
    private String scaleNumber;

    //@Digits(integer = 2, fraction = 0, message = "billingByScalesTableCreateRequests.scaleCode - Scale code should be a numeric value with up to 2 digits")
    //@Min(value = 1, message = "billingByScalesTableCreateRequests.scaleCode - Scale code should be greater than or equal to 1")
    //@Max(value = 32, message = "billingByScalesTableCreateRequests.scaleCode - Scale code should be less than or equal to 32")
    @Size(min = 1, max = 32, message = "billingByScalesTableCreateRequests.scaleCode - Scale code should be between {min} and {max} characters;")
    private String scaleCode;

    @Size(min = 1, max = 32, message = "billingByScalesTableCreateRequests.scaleType-Scale type should be between {min} and {max} characters;")
    private String scaleType;

    @Size(min = 1, max = 256, message = "billingByScalesTableCreateRequests.timeZone-Time Zone should be between {min} and {max} characters;")
    private String timeZone;

    @DecimalMin(value = "0", message = "billingByScalesTableCreateRequests.newMeterReading-newMeterReading Value should be greater than or equal to 0;")
    @DecimalMax(value = "9999999.99999", message = "billingByScalesTableCreateRequests.newMeterReading-Value should be less than or equal to 9999999.99999;")
    @Digits(integer = 8, fraction = 5, message = "billingByScalesTableCreateRequests.newMeterReading-valueInPercent Invalid format. valueInPercent should be in range 0.00001_9 999 999.99999;")
    private BigDecimal newMeterReading;

    @DecimalMin(value = "0", message = "billingByScalesTableCreateRequests.oldMeterReading-Value should be greater than or equal to 0;")
    @DecimalMax(value = "9999999.99999", message = "billingByScalesTableCreateRequests.oldMeterReading-Value should be less than or equal to 9999999.99999;")
    @Digits(integer = 8, fraction = 5, message = "billingByScalesTableCreateRequests.oldMeterReading-valueInPercent Invalid format. valueInPercent should be in range 0.00001_9 999 999.99999;")
    private BigDecimal oldMeterReading;

    private BigDecimal difference;

    private BigDecimal multiplier;

    private BigDecimal correction;

    private BigDecimal deducted;

    private BigDecimal totalVolumes;

    @Size(min = 1, max = 1024, message = "billingByScalesTableCreateRequests.tariffScale-Tariff scale should be between {min} and {max} characters;")
    private String tariffScale;

    private BigDecimal volumes;

    @DecimalMin(value = "0", message = "billingByScalesTableCreateRequests.unitPrice-Value should be greater than or equal to 0;")
    @DecimalMax(value = "9999999.99999", message = "billingByScalesTableCreateRequests.unitPrice-Value should be less than or equal to 9999999.99999;")
    @Digits(integer = 8, fraction = 5, message = "billingByScalesTableCreateRequests.unitPrice-valueInPercent Invalid format. valueInPercent should be in range 0.00001_9 999 999.99999;")
    private BigDecimal unitPrice;

    private BigDecimal totalValue;

    @NotNull(message = "billingByScalesTableCreateRequests.index-[index] can't be null;")
    private Integer index;

}
