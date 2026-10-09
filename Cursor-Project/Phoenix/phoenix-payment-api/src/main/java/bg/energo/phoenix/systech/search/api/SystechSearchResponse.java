package bg.energo.phoenix.systech.search.api;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechSearchResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Results")
    private List<SystechSearchResultItem> Results;

    public static SystechSearchResponse success(List<SystechSearchResultItem> results) {
        List<SystechSearchResultItem> safeResults = results == null ? Collections.emptyList() : results;
        return new SystechSearchResponse(0, "", safeResults);
    }

    public static SystechSearchResponse error(int code, String message) {
        return new SystechSearchResponse(code, message, Collections.emptyList());
    }
}
