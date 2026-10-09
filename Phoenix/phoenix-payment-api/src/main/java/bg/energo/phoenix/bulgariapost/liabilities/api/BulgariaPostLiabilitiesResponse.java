package bg.energo.phoenix.bulgariapost.liabilities.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostLiabilitiesResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Results")
    private List<BulgariaPostLiabilityResultItem> Results;

    public static BulgariaPostLiabilitiesResponse success(List<BulgariaPostLiabilityResultItem> results) {
        List<BulgariaPostLiabilityResultItem> safeResults = results == null ? Collections.emptyList() : results;
        return new BulgariaPostLiabilitiesResponse(0, "", safeResults);
    }

    public static BulgariaPostLiabilitiesResponse error(int code, String message) {
        return new BulgariaPostLiabilitiesResponse(code, message, Collections.emptyList());
    }
}

