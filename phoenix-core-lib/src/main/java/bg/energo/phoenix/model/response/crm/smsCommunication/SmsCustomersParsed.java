package bg.energo.phoenix.model.response.crm.smsCommunication;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class SmsCustomersParsed {
    private String customerIdentifier;
    private Long versionId;
}
