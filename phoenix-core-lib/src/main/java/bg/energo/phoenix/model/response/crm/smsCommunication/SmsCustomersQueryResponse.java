package bg.energo.phoenix.model.response.crm.smsCommunication;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SmsCustomersQueryResponse {
    private Long customerDetailId;
    private Long customerCommunicationId;
}
