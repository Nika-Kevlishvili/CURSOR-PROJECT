package bg.energo.phoenix.service.crm.smsCommunication;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomers;
import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.request.crm.smsCommunication.SmsSendParamBase;
import bg.energo.phoenix.model.response.crm.smsCommunication.SmsStatusResponse;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomerContactsRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomersRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.crm.SmsSendingNumberRepository;
import bg.energo.phoenix.util.epb.EPBBatchUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCommunicationStatusCheckJobService {

    private static final int BATCH_SIZE = 50;
    private static final int THREAD_COUNT = 10;
    private static final String REQUEST_ID_PREFIX = "EPROES";
    private final SmsCommunicationCustomersRepository smsCommunicationCustomersRepository;
    private final ObjectMapper objectMapper;
    private final SmsCommunicationSendHelperService smsCommunicationSendHelperService;
    private final SmsCommunicationService smsCommunicationService;
    private final SmsCommunicationCustomerContactsRepository smsCommunicationCustomerContactsRepository;
    private final SmsCommunicationRepository smsCommunicationRepository;
    private final SmsSendingNumberRepository smsSendingNumberRepository;
    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${app.sms.service.api_url}")
    private String endpointUrl;
    @Value("${app.sms.service.status_check_url}")
    private String statusCheckUrl;
    @Value("${app.sms.service.sid}")
    private int sid;
    @Value("${app.sms.service.token}")
    private String token;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkStatus(List<SmsCommunicationCustomers> smsToSend) {
        for (SmsCommunicationCustomers sms : smsToSend) {
            String requestId = REQUEST_ID_PREFIX + sms.getId();
            log.debug("RequestId is : {}", requestId);
            String response = null;
            try {
                response = checkStatus(requestId);
            } catch (IOException | InterruptedException e) {
                log.debug(e.getMessage());
                throw new RuntimeException(e);
            }
            log.debug("Response: {}", response);
            if (response == null) {
                log.debug("Response is null for sms : {}", sms.getId());
                sms.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
                continue;
            }
            SmsStatusResponse smsStatusResponse = null;
            try {
                smsStatusResponse = objectMapper.readValue(response, SmsStatusResponse.class);
            } catch (JsonProcessingException e) {
                log.debug(e.getMessage());
                log.debug("Exception occurred during status check of sms {} ", sms.getId());
                sms.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
                continue;
            }
            log.debug("Response from sms provider {} ", smsStatusResponse);
            if (smsStatusResponse.getMessageStatus() == 2 || smsStatusResponse.getMessageStatus() == 16) {
                sms.setSmsCommStatus(SmsCommStatus.DELIVERY_FAILED);
            } else if (smsStatusResponse.getMessageStatus() == 1) {
                sms.setSmsCommStatus(SmsCommStatus.SENT_SUCCESSFULLY);
                log.debug("Sent successfully sms id {}", sms.getId());
            } else if (smsStatusResponse.getMessageStatus() == 8) {
                sms.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
                log.debug("Sending in progress sms id {}", sms.getId());
            } else {
                log.debug("Unknown status");
            }
        }
    }

    //    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    @Transactional
    public void executeSmsStatusCheck() {
        log.info("SMS status check started");

        List<SmsCommunicationCustomers> smsToSend = smsCommunicationCustomersRepository.findSmsWithInProgressStatus(LocalDateTime.now().minusMinutes(5));
        new Thread(() -> {
            EPBBatchUtils.submitItemsInBatches(
                    smsToSend,
                    THREAD_COUNT,
                    BATCH_SIZE,
                    this::checkStatus
            );
        }).start();
        log.info("SMS status check ended");

    }

    @Transactional
    public void executeSmsResend() {
        log.info("SMS resend started");
        List<SmsCommunicationCustomers> smsToResend = smsCommunicationCustomersRepository.findSmsWithToDoStatus(LocalDateTime.now().minusMinutes(5));

        new Thread(() -> {
            EPBBatchUtils.submitItemsInBatches(
                    smsToResend,
                    THREAD_COUNT,
                    BATCH_SIZE,
                    this::resendSMS
            );
        }).start();
        log.info("SMS resend ended");

    }

    @Transactional
    public void updateTimedOutSmsCommunications() {
        log.info("Starting the update of timed out SMS communications.");
        smsCommunicationCustomersRepository.updateSmsCommStatusForTimedOutCommunications();
        log.info("Completed the update of timed out SMS communications.");
    }

    public String checkStatus(String id) throws IOException, InterruptedException {
        String url = buildUrl(id);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json, text/plain")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("HttpResponse from api {}", response);
        log.info("Response from sms sender API: {}", response.body());
        log.info("status code from api {}", response.statusCode());
        log.info("headers from api : {}", response.headers());
        return response.body();
    }

    private String buildUrl(String id) {
        return String.format("%s%s?sid=%s&id=%s",
                endpointUrl,
                statusCheckUrl,
                sid,
                id);
    }

    private void resendSMS(List<SmsCommunicationCustomers> smsToSend) {
        for (SmsCommunicationCustomers sms : smsToSend) {
            String phoneNumber = smsCommunicationCustomerContactsRepository.getPhoneNumberBySmsCommunicationCustomerId(sms.getId());
            SmsSendParamBase smsSendParamSingle = smsCommunicationService.createSmsSendParam(sms.getId(), phoneNumber, sms);

            SmsCommunication smsCommunication = smsCommunicationRepository.findBySmsCommunicationCustomerId(sms.getId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Sms communication not found;"));

            SmsSendingNumber smsSendingNumber = smsSendingNumberRepository.findById(smsCommunication.getSmsSendingNumberId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Sms sending number not found;"));

            smsCommunicationSendHelperService.send(smsSendParamSingle, smsCommunication.getSmsBody(), smsSendingNumber.getSmsNumber(), null);
        }
    }

}
