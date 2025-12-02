package bg.energo.phoenix.model.request.pod.billingByScales;

import bg.energo.phoenix.model.customAnotations.pod.billingByScales.ValidBillingByScalesPeriod;
import bg.energo.phoenix.model.customAnotations.pod.billingByScales.ValidInvoiceCorrection;
import bg.energo.phoenix.model.customAnotations.pod.billingByScales.ValidInvoiceDate;
import bg.energo.phoenix.model.customAnotations.pod.billingByScales.ValidPeriods;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidBillingByScalesPeriod
@ValidInvoiceDate
@ValidInvoiceCorrection
@ValidPeriods
public class BillingByScalesCreateRequest {

    @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "identifier-Allowed symbols in number are: A/Z a/z 0/9;")
    @Size(min = 1, max = 33, message = "identifier-Identifier should be between {min} and {max} characters;")
    @NotBlank(message = "identifier-[Identifier] must not be blank;")
    private String identifier;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "dateFrom-Date From must not be null;")
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "dateTo-Date to must not be null;")
    private LocalDate dateTo;

    @Min(value = 1, message = "billingPowerInKw-Amount Of Billing Power In Kw must be greater or equal then: [{value}];")
    @Max(value = 99999999, message = "billingPowerInKw-Amount Of Billing Power In Kw must be greater or equal then: [{value}];")
    private Integer billingPowerInKw;

    private Integer numberOfDays;

    @Pattern(regexp = "^[0-9A-Za-z./]+$", message = "invoiceNumber-Allowed symbols in number are: A/Z a/z 0/9;")
    @Size(min = 1, max = 32, message = "invoiceNumber-Invoice number should be between {min} and {max} characters;")
    @NotBlank(message = "invoiceNumber-invoiceNumber must not be blank;")
    private String invoiceNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "invoiceDate-invoice date must not be null;")
    private LocalDateTime invoiceDate;

    //@NotEmpty(message = "invoiceCorrection-shouldn't be empty;")
    private String invoiceCorrection;

    @NotNull(message = "correction-Correction must not be null;")
    private Boolean correction;

    @NotNull(message = "correction-Override must not be null;")
    private Boolean override;

    @Size(min = 1, max = 512, message = "basisForIssuingTheInvoice-BasisForIssuingTheInvoice should be between {min} and {max} characters;")
    private String basisForIssuingTheInvoice;

    @Size(max = 100, message = "billingByScalesTableCreateRequests-Identifier contain be {max} items;")
    private List<@Valid BillingByScalesTableCreateRequest> billingByScalesTableCreateRequests;

    @NotNull(message = "saveRecordForIntermediatePeriod- saveRecordForIntermediatePeriod shouldn't be null;")
    private Boolean saveRecordForIntermediatePeriod;

    @NotNull(message = "saveRecordForMeterReadings- saveRecordForMeterReadings shouldn't be null;")
    private Boolean saveRecordForMeterReadings;

}
