package bg.energo.phoenix.model.request.crm.smsCommunication;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SmsCommunicationSenderRequest {
    @JsonProperty("sid")
    private int sid;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("to")
    private String to;

    @JsonProperty("token")
    private String token;

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("defer")
    private String defer;

    @JsonProperty("send_order")
    private List<String> sendOrder;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("sms")
    private SmsDetails sms;

    @Data
    @Builder
    public static class SmsDetails {

        @JsonProperty("text")
        private String text;

        @JsonProperty("encoding")
        private String encoding;

        @JsonProperty("concatenate")
        private int concatenate;

        @JsonProperty("from")
        private String from;

        @JsonProperty("validity")
        private Validity validity;

        @JsonProperty("mccmnc")
        private String mccmnc;
    }

    @Data
    @Builder
    public static class Validity {

        @JsonProperty("ttl")
        private int ttl;

        @JsonProperty("units")
        private String units;
    }



}
