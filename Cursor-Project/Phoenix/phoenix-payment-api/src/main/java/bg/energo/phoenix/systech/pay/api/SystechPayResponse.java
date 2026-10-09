package bg.energo.phoenix.systech.pay.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystechPayResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    public static SystechPayResponse success() {
        return new SystechPayResponse(0, "");
    }

    public static SystechPayResponse error(int code, String message) {
        return new SystechPayResponse(code, message);
    }
}
