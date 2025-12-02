package bg.energo.phoenix.model.documentModels.latePaymentFine;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LatePaymentFineInterestsResponse {

    @JsonProperty("InterestAmount")
    public String InterestAmount;

    @JsonProperty("InterestRate")
    public String InterestRate;

    @JsonProperty("NumberDays")
    public String NumberDays;

    @JsonProperty("OverdueAmount")
    public String OverdueAmount;

    @JsonProperty("OverdueDocumentNumber")
    public String OverdueDocumentNumber;

    @JsonProperty("OverdueDocumentPrefix")
    public String OverdueDocumentPrefix;

    @JsonProperty("OverdueEndDate")
    public LocalDate OverdueEndDate;

    @JsonProperty("OverdueStartDate")
    public LocalDate OverdueStartDate;

}
