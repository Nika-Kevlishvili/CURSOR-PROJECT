package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class LiabilitiesOffsettingChoice {

    private Long id;
    private String currentAmount;
    private Long customerId;
    private BigDecimal amount;
    private Long currencyId;

    public LiabilitiesOffsettingChoice(LiabilitiesMiddleResponse response) {
        this.id = response.getId();
        this.currentAmount = response.getCurrentAmount();
        this.customerId = response.getCustomerId();
        this.currencyId = response.getCurrencyId();
        this.amount = response.getAmount();
    }

}
