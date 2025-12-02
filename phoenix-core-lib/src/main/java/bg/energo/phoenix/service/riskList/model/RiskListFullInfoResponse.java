package bg.energo.phoenix.service.riskList.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RiskListFullInfoResponse {

    @JsonProperty("small")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RiskListSmallInfo smallInfo;

    @JsonProperty("large")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RiskListLargeInfo largeInfo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RiskListBasicInfoResponse basicInfo;

}
