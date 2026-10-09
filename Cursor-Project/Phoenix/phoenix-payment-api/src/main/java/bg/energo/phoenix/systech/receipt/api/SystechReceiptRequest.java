package bg.energo.phoenix.systech.receipt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechReceiptRequest {

    @JsonProperty("CustomerNumber")
    private String CustomerNumber;

    @JsonProperty("DocumentNumber")
    private String DocumentNumber;

    @JsonProperty("Currency")
    private String Currency;

    @JsonProperty("Exchange")
    private BigDecimal Exchange;

    @JsonProperty("PaidSum")
    private BigDecimal PaidSum;

    @JsonProperty("TID")
    private String TID;

    @JsonProperty("PostCode")
    private String PostCode;
}

