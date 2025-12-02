package bg.energo.phoenix.model.request.crm.smsCommunication;

import bg.energo.phoenix.model.customAnotations.crm.smsCommunication.SmsCommunicationBaseRequestValidator;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationSave;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@SmsCommunicationBaseRequestValidator
@Builder
@AllArgsConstructor
public class SmsCommunicationBaseRequest {
    private boolean communicationAsInstitution;

    @NotNull(message = "topicOfCommunicationId-[topicOfCommunicationId] topic of communication is mandatory!;")
    private Long topicOfCommunicationId;

    @NotNull(message = "communicationType-[communicationType] communication type is mandatory!;")
    private CommunicationType communicationType;

    private LocalDateTime dateAndTime;

    @NotNull(message = "exchangeCodeId-[exchangeCodeId] exchangeCodeId is mandatory!;")
    private Long exchangeCodeId;

    @NotBlank(message = "customerPhoneNumber-[customerPhoneNumber] customer phone number is mandatory!;")
    @Pattern(regexp = "^[0-9\\-+*]+$", message = "Field must only contain digits, '-', '+', or '*'")
    @Size(min = 1, max = 32, message = "Field must be between 1 and 32 characters")
    private String customerPhoneNumber;

    private String smsBody;

    @NotNull(message = "customerId-[customerId] customer detail id is mandatory!;")
    private Long customerDetailId;

    @NotNull(message = "customerCommunicationId-[customerCommunicationId] customer communication is mandatory!;")
    private Long customerCommunicationId;

    private Set<Long> communicationFileIds;

    private Set<Long> relatedCustomerIds;

    @NotNull(message = "saveAs-[saveAs] saveAs is mandatory!;")
    private CommunicationSave saveAs;

    private Long templateId;

}
