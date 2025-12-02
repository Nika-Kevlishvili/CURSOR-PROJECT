package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.mass_comm.models.Attachment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailSendModel {
    private List<EmailSendContactModel> emailSendContactModels;
    private List<Attachment> attachments;
    private String subject;
    private String body;
}
