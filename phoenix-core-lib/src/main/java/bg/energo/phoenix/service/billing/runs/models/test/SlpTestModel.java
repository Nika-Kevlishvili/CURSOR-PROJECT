package bg.energo.phoenix.service.billing.runs.models.test;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SlpTestModel {

    private Long podId;
    private Long podDetailId;
    private Integer invoiceNumber;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Long billingScaleId;

}
