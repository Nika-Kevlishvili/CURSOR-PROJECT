package bg.energo.phoenix.model.response.customer.communicationData.detailed;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDetailedResponse {
    private Long id;
    private Boolean sendSms;
    private Long platformId;
    private String platformName;
    private Status status;
    private CustomerCommContactTypes contactType;
    private String contactValue;
    private Long customerCommunicationsId;
}
