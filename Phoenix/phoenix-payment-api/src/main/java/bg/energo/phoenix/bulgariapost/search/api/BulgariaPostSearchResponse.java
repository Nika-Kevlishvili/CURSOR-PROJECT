package bg.energo.phoenix.bulgariapost.search.api;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostSearchResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Results")
    private List<BulgariaPostSearchResultItem> Results;

    public static BulgariaPostSearchResponse success(List<BulgariaPostSearchResultItem> results) {
        List<BulgariaPostSearchResultItem> safeResults = results == null ? Collections.emptyList() : results;
        return new BulgariaPostSearchResponse(0, "", safeResults);
    }

    public static BulgariaPostSearchResponse error(int code, String message) {
        return new BulgariaPostSearchResponse(code, message, Collections.emptyList());
    }
}
