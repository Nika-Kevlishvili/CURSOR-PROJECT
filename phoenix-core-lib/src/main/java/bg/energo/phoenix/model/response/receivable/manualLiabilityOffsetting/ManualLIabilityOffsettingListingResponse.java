package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.Reversed;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ManualLIabilityOffsettingListingResponse {

    private Long id;

    private LocalDate manualLiabilityDate;

    private String customer;

    private Reversed reversed;

    public ManualLIabilityOffsettingListingResponse(ManualLIabilityOffsettingListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.manualLiabilityDate = middleResponse.getManualLiabilityDate();
        this.customer = middleResponse.getCustomer();
        this.reversed = middleResponse.getReversed() ? Reversed.YES : Reversed.NO;
    }

}
