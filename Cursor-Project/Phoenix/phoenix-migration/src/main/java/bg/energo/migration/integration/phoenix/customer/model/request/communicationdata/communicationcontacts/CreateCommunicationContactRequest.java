package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.communicationcontacts;

import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerCommContactTypes;
import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunicationContactRequest {
    private Boolean sendSms;
    private Long platformId;
    private Status status;
    private CustomerCommContactTypes contactType;
    private String contactValue;
}
