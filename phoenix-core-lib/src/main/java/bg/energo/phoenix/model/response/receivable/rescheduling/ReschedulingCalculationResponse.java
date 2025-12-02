package bg.energo.phoenix.model.response.receivable.rescheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReschedulingCalculationResponse {

    @JsonProperty("lpfs")
    private List<ReschedulingLpfs> lpfs;

    @JsonProperty("installments")
    private List<ReschedulingInstallment> instalments;

    private BigDecimal sumOfAmount;
    private BigDecimal sumOfPrincipalAmount;
    private BigDecimal sumOfInterestAmount;
    private BigDecimal sumOfFeeAmount;
}
