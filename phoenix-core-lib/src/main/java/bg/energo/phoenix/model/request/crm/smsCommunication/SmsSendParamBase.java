package bg.energo.phoenix.model.request.crm.smsCommunication;

import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SmsSendParamBase {
    private String requestId;
    private String to;
    private SmsCommunicationCustomers smsCommunicationCustomers;
}
