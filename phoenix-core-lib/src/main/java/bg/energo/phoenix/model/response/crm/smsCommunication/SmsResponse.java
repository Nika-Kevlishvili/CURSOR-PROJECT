package bg.energo.phoenix.model.response.crm.smsCommunication;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmsResponse {
    @JsonProperty("return_code")
    private int returnCode;

    @JsonProperty("return_message")
    private String returnMessage;
}
