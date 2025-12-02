package bg.energo.phoenix.service.crm.smsCommunication;

import bg.energo.phoenix.model.request.crm.smsCommunication.SmsCommunicationSenderRequest;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCommunicationSenderService {

    private WebClient webClient;
    @Value("${app.sms.service.api_url}")
    private String endpointUrl;

    @Value("${app.sms.service.send_url}")
    private String sendUrl;
    @Value("${app.sms.service.callback_url}")
    private String callBackUrl;
    @Value("${app.sms.service.sid}")
    private int sid;
    @Value("${app.sms.service.token}")
    private String token;
    @Value("${app.sms.service.priority}")
    private int priority;
    @Value("${app.sms.service.send_order}")
    private String sendOrder;
    @Value("${app.sms.service.encoding}")
    private String encode;
    @Value("${app.sms.service.concatenate}")
    private int concatenate;
    @Value("${app.sms.service.mccmnc}")
    private String mccmnc;
    @Value("${app.sms.service.ttl}")
    private int ttl;
    @Value("${app.sms.service.units}")
    private String units;
    @Value("${app.sms.service.status_check_url}")
    private String statusCheckUrl;

    public static String validateAndFormatPhoneNumber(String phoneNumber, ArrayList<String> errorMessages) {
        String PHONE_NUMBER_PATTERN = "^359\\d{9}$";

        if (phoneNumber.startsWith("359")) {
            if (!phoneNumber.matches(PHONE_NUMBER_PATTERN)) {
                errorMessages.add("Invalid phone number format. Must be 359XXXXXXXXX.");
            }
            return phoneNumber;
        }

        if (phoneNumber.startsWith("0")) {
            return "359" + phoneNumber.substring(1);
        }

        if (phoneNumber.length() == 9) {
            return "359" + phoneNumber;
        }

        errorMessages.add("Phone number must be in the format 359XXXXXXXXX or 0XXXXXXXXX.");
        return phoneNumber;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.create(endpointUrl);
    }

    /**
     * Sends a test SMS request to the SMS sender API.
     *
     * @param requestId the unique identifier for the request
     * @param to        the phone number to send the SMS to
     * @param text      the text content of the SMS
     * @param from      the phone number or name to send the SMS from
     * @param defer     the date to defer the SMS delivery to
     * @return a Mono that emits the response from the SMS sender API
     */
    public String sendTestSmsRequest(String requestId, String to, String text, String from, LocalDate defer) {
        log.info("Send request to sms sender API: requestId={}, to={}, text={}, from={}, defer={}", requestId, to, text, from, defer);
        ArrayList<String> errorMessages = new ArrayList<>();
//        validateRequestId(requestId, errorMessages);
        to = validateAndFormatPhoneNumber(to, errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        log.debug("Sending message , callback url is : {}", callBackUrl);
        SmsCommunicationSenderRequest request = createTestSmsRequest(requestId, to, text, from, defer);

        return webClient.post()
                .uri(sendUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Creates a test SMS request to send sms.
     *
     * @param requestId the unique identifier for the request
     * @param to        the phone number to send the SMS to
     * @param text      the text content of the SMS
     * @param from      the phone number or name to send the SMS from
     * @param defer     the date to defer the SMS send until
     * @return the created SMS communication sender request
     */
    private SmsCommunicationSenderRequest createTestSmsRequest(String requestId, String to, String text, String from, LocalDate defer) {
        return SmsCommunicationSenderRequest.builder()
                .sid(sid)
                .requestId(requestId)
                .to(to)
                .token(token)
                .priority(1)
                .defer(defer == null ? null : defer.toString())
                .sendOrder(Collections.singletonList(sendOrder))
                .callbackUrl(callBackUrl)
                .sms(SmsCommunicationSenderRequest.SmsDetails.builder()
                        .text(text)
                        .encoding(encode)
                        .concatenate(concatenate)
                        .from(from)
                        .validity(SmsCommunicationSenderRequest.Validity.builder()
                                .ttl(ttl)
                                .units(units)
                                .build())
                        .mccmnc(mccmnc)
                        .build())
                .build();
    }

    /**
     * Receives the result of an SMS send callback operation. use it for future development
     *
     * @param channel       The communication channel used for the SMS.
     * @param sid           The unique identifier of the SMS message.
     * @param messageStatus The status of the SMS message.
     * @param to            The recipient phone number of the SMS.
     * @param from          The sender phone number of the SMS.
     * @param timestamp     The timestamp when the result was received.
     * @param requestId     The unique identifier of the SMS request.
     */
    public void receiveResult(String channel,
                              int sid,
                              int messageStatus,
                              int to,
                              String from,
                              String timestamp,
                              String requestId) {

    }

    private void validateRequestId(String requestId, ArrayList<String> errorMessages) {
        String REQUEST_ID_PATTERN = "^EPROES\\d+\\d{2}$";

        Pattern pattern = Pattern.compile(REQUEST_ID_PATTERN);
        Matcher matcher = pattern.matcher(requestId);
        if (!matcher.matches()) {
            errorMessages.add("invalid request id: %s;".formatted(requestId));
        }
    }

}
