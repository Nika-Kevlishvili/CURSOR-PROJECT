package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CommunicationShortResponse {

    private Long id;
    private LocalDate date;

}
