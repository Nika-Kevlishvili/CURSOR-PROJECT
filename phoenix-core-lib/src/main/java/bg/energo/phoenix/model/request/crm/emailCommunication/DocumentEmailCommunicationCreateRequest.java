package bg.energo.phoenix.model.request.crm.emailCommunication;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmailCommunicationCreateRequest {

    @NotNull(message = "communicationTopicId-[communicationTopicId] should not be null;")
    private Long communicationTopicId;

    @NotNull(message = "emailBoxId-[emailBoxId] should not be null;")
    private Long emailBoxId;

    @Size(max = 1024, message = "customerEmailAddress-[customerEmailAddress] size should be 1024 symbol;")
    @NotNull(message = "customerEmailAddress-[customerEmailAddress] should not be null;")
    private String customerEmailAddress;

    @Size(max = 255, message = "emailSubject-[emailSubject] size should be 255 symbol;")
    @NotNull(message = "emailSubject-[emailSubject] should not be null;")
    private String emailSubject;

    @NotNull(message = "emailBody-[emailBody] should not be null;")
    private String emailBody;

    @NotNull(message = "customerDetailId-[customerDetailId] should not be null;")
    private Long customerDetailId;

    @NotNull(message = "customerCommunicationId-[customerCommunicationId] should not be null;")
    private Long customerCommunicationId;

    private Set<Long> attachmentFileIds;

    private Long emailTemplateId;

    private Set<Long> templateIds;

}
