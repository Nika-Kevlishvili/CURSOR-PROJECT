package bg.energo.phoenix.bulgariapost.pay.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostPayResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    public static BulgariaPostPayResponse success() {
        return new BulgariaPostPayResponse(0, "");
    }

    public static BulgariaPostPayResponse error(int code, String message) {
        return new BulgariaPostPayResponse(code, message);
    }
}
