package bg.energo.phoenix.model.request.systemMessage;

import bg.energo.phoenix.model.customAnotations.nomenclature.systemMessages.CreateSystemMessageRequestValidator;
import bg.energo.phoenix.model.enums.SystemMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CreateSystemMessageRequestValidator
public class CreateSystemMessageRequest {

    @NotBlank(message = "title-Title shouldn't be blank;")
    @Size(min = 1,max = 256,message = "title-Title length should be {min} to {max};")
    private String title;

    @NotBlank(message = "titleTransliterated-TitleTransliterated shouldn't be blank;")
    @Size(min = 1,max = 256,message = "titleTransliterated-TitleTransliterated length should be {min} to {max};")
    private String titleTransliterated;

    @NotBlank(message = "messageText-Message text shouldn't be blank;")
    @Size(min = 1,max = 2048,message = "messageText-Message text length should be {min} to {max};")
    private String messageText;

    @NotBlank(message = "messageTextTransliterated-MessageTransliterated text shouldn't be blank;")
    @Size(min = 1,max = 2048,message = "messageTextTransliterated-MessageTransliterated text length should be {min} to {max};")
    private String messageTextTransliterated;

    @NotBlank(message = "name-Name shouldn't be blank;")
    @Size(min = 1,max = 2048,message = "name-Name length should be {min} to {max};")
    private String name;

    private String okButtonText;

    private String okButtonTextTransliterated;

    private String noButtonText;

    private String noButtonTextTransliterated;

    private String cancelButtonText;

    private String cancelButtonTextTransliterated;

    @NotNull(message = "systemMessageType-System message type shouldn't be null;")
    private SystemMessageType systemMessageType;

    @NotBlank(message = "keyName-Key name shouldn't be blank;")
    @Size(min = 1,max = 2048,message = "keyName-Key name length should be {min} to {max};")
    private String keyName;

}
