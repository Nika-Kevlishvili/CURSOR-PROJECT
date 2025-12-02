package bg.energo.phoenix.model.response.receivable.rescheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReschedulingLpfs {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("interest_default_currency")
    private BigDecimal interestDefaultCurrency;

}
