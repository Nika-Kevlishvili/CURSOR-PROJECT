package bg.energo.phoenix.model.response.pod.billingByScales;

import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BillingByScalesResponse {

    private String identifier;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer billingPowerInKw;
    //private Integer numberOfDays;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private String invoiceCorrection;
    private Boolean correction;
    private Boolean override;
    private String basisForIssuingTheInvoice;
    private BillingByScaleStatus status;
    private List<BillingByScalesTableResponse> billingByScalesTableCreateRequests;
    private Boolean isLocked;

}
