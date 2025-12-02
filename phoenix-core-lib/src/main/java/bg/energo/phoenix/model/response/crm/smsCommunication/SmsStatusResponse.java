package bg.energo.phoenix.model.response.crm.smsCommunication;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@ToString
public class SmsStatusResponse {
    @JsonProperty("channel")
    private String channel;
    @JsonProperty("sid")
    private String sid;
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("message_status")
    private int messageStatus;
    @JsonProperty("to")
    private String to;
    @JsonProperty("from")
    private String from;
}
