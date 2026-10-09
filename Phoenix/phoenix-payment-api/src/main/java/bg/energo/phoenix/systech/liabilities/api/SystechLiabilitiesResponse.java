package bg.energo.phoenix.systech.liabilities.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechLiabilitiesResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Results")
    private List<SystechLiabilityResultItem> Results;

    public static SystechLiabilitiesResponse success(List<SystechLiabilityResultItem> results) {
        List<SystechLiabilityResultItem> safeResults = results == null ? Collections.emptyList() : results;
        return new SystechLiabilitiesResponse(0, "", safeResults);
    }

    public static SystechLiabilitiesResponse error(int code, String message) {
        return new SystechLiabilitiesResponse(code, message, Collections.emptyList());
    }
}

