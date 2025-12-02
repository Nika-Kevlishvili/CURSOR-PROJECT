package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SingleEmailSendModel {

    private Long emailCommunicationId;//todo remove after testing

    private String subject;

    private String body;

    private EmailSendContactModel emailSendContactModel;

    private List<EmailCommunicationAttachment> attachments;
}
