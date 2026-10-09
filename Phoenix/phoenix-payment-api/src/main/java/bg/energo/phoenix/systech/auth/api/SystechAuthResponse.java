package bg.energo.phoenix.systech.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matches Systech authentication response contract exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechAuthResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Token")
    private String Token;

    public static SystechAuthResponse success(String token) {
        return new SystechAuthResponse(0, "", token);
    }

    public static SystechAuthResponse error(int code, String message) {
        return new SystechAuthResponse(code, message, "");
    }
}
