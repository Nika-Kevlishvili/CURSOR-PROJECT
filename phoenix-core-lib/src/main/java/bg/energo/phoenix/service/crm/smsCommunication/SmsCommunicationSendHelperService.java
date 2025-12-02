package bg.energo.phoenix.service.crm.smsCommunication;

import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.request.crm.smsCommunication.SmsSendParamBase;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomersRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCommunicationSendHelperService {
    private final SmsCommunicationSenderService smsCommunicationSenderService;
    private final SmsCommunicationCustomersRepository smsCommunicationCustomersRepository;
    private final SmsCommunicationRepository smsCommunicationRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void sendBatch(List<SmsSendParamBase> to, String text, String from, LocalDate defer) {
        //todo removed for time being
//        for(SmsSendParamBase smsSendParamMass : to) {
//            try {
//                String response = smsCommunicationSenderService.sendTestSmsRequest(smsSendParamMass.getRequestId(), smsSendParamMass.getTo(), text, from, defer);
//                SmsResponse smsResponse = objectMapper.readValue(response, SmsResponse.class);
//                if(smsResponse.getReturnCode()!=0) {
//                    log.debug("Return code not 0 , sending failed!");
//                    log.debug("Reason : {}" , smsResponse.getReturnMessage());
//                    SmsCommunicationCustomers smsCommunicationCustomers = smsSendParamMass.getSmsCommunicationCustomers();
//                    smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.SEND_FAILED);
//                    smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//                } else {
//                    log.debug("Request sent for sms : to {}", smsSendParamMass.getTo());
//                }
//                log.debug(response);
//
//            }catch (Exception e) {
//                log.debug("Error while sending SMS to {}", smsSendParamMass.getTo(), e);
//                log.debug(e.getLocalizedMessage());
//                log.debug(e.getMessage());
//                SmsCommunicationCustomers smsCommunicationCustomers = smsSendParamMass.getSmsCommunicationCustomers();
//                smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.SEND_FAILED);
//                smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//            }
//        }
    }

    @Async
    public void send(SmsSendParamBase to, String text, String from, LocalDate defer) {
//        try {
//            String response = smsCommunicationSenderService.sendTestSmsRequest(to.getRequestId(), to.getTo(), text, from, defer);
//            SmsResponse smsResponse = objectMapper.readValue(response, SmsResponse.class);
//            if (smsResponse.getReturnCode() == 1) {
//                log.debug("Return code is 1 , sent successfully!");
//                SmsCommunicationCustomers smsCommunicationCustomers = to.getSmsCommunicationCustomers();
//                smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.SENT_SUCCESSFULLY);
//                smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//            } else if (smsResponse.getReturnCode() == 8) {
//                log.debug("Return code is 8 , sending in progress!");
//                SmsCommunicationCustomers smsCommunicationCustomers = to.getSmsCommunicationCustomers();
//                smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.IN_PROGRESS);
//                smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//            } else if (smsResponse.getReturnCode() == 2 || smsResponse.getReturnCode() == 16) {
//                log.debug("Return code not 0 , Delivery failed!");
//                log.debug("Reason : {}", smsResponse.getReturnMessage());
//                SmsCommunicationCustomers smsCommunicationCustomers = to.getSmsCommunicationCustomers();
//                smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.DELIVERY_FAILED);
//                smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//            }
//            log.debug(response);
//        } catch (Exception e) {
//            log.debug("Error while sending SMS to {}", to.getTo(), e);
//            log.debug(e.getLocalizedMessage());
//            log.debug(e.getMessage());
//            SmsCommunicationCustomers smsCommunicationCustomers = to.getSmsCommunicationCustomers();
//            smsCommunicationCustomers.setSmsCommStatus(SmsCommStatus.TODO);
//            smsCommunicationCustomersRepository.save(smsCommunicationCustomers);
//        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSmsComm(SmsCommunication smsCommunication) {
        smsCommunicationRepository.save(smsCommunication);
    }
}
