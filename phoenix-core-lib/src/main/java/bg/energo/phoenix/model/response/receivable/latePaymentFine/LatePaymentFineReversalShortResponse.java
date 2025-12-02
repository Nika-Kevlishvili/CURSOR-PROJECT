package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LatePaymentFineReversalShortResponse {

    private Long id;
    private String latePaymentNumber;
    private LocalDate date;

}
