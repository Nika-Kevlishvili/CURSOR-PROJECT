package bg.energo.phoenix.bulgariapost.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matches Bulgaria Post authentication request contract exactly.
 *
 * Note: all symbols are allowed; only length is validated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostAuthRequest {

    @JsonProperty("UserName")
    private String UserName;

    @JsonProperty("UserPassword")
    private String UserPassword;

    @JsonProperty("PostCode")
    private String PostCode;
}

