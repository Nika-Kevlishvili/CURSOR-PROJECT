package bg.energo.phoenix.bulgariapost.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matches Bulgaria Post authentication response contract exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostAuthResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Token")
    private String Token;

    public static BulgariaPostAuthResponse success(String token) {
        return new BulgariaPostAuthResponse(0, "", token);
    }

    public static BulgariaPostAuthResponse error(int code, String message) {
        return new BulgariaPostAuthResponse(code, message, "");
    }
}

