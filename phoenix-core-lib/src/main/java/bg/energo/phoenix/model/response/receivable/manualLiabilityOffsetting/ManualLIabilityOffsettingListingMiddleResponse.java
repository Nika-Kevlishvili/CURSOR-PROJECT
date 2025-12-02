package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import java.time.LocalDate;

public interface ManualLIabilityOffsettingListingMiddleResponse {

    Long getId();

    LocalDate getManualLiabilityDate();

    String getCustomer();

    boolean getReversed();

}
