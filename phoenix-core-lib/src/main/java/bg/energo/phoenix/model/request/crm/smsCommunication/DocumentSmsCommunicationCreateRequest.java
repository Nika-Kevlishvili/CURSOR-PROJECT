package bg.energo.phoenix.model.request.crm.smsCommunication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSmsCommunicationCreateRequest {
    private Long customerDetailId;

    private Long customerCommunicationId;

    private String customerPhoneNumber;

    private String smsBody;

    private Long smsTemplateId;

    private Long communicationTopicId;

    private Long smsNumberId;
}
