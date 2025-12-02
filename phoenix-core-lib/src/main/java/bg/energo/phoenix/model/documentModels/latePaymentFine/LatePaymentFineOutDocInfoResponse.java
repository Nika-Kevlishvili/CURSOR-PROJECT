package bg.energo.phoenix.model.documentModels.latePaymentFine;

import java.time.LocalDate;

public interface LatePaymentFineOutDocInfoResponse {

    LocalDate getFullPaymentDate();

    String getOverdueDocumentType();

    String getOverdueDocumentNumber();

    String getOverdueDocumentPrefix();

    LocalDate getOverdueDocumentDate();

    String getLiabilityInitialAmount();

}
