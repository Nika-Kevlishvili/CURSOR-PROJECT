package bg.energo.phoenix.model.response.crm.emailCommunication;

import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailCommunicationResendResponse {

    private Boolean communicationAsAnInstitution;

    private EmailCommunicationChannelType communicationChannelType;

    private EmailCommunicationType communicationType;

    private ShortResponse emailBox;

    private String customerEmailAddress;

    private String emailSubject;

    private String emailBody;

    private List<ShortResponse> attachmentsShortResponse;

    private ShortResponse customerShortResponse;

}
