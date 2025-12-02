package bg.energo.phoenix.model.documentModels.latePaymentFine;

import java.time.LocalDate;

public interface LatePaymentFineInterestsMiddleResponse {

    String getInterestAmount();

    String getInterestRate();

    String getNumberDays();

    String getOverdueAmount();

    String getOverdueDocumentNumber();

    String getOverdueDocumentPrefix();

    LocalDate getOverdueEndDate();

    LocalDate getOverdueStartDate();

}
