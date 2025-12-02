package bg.energo.phoenix.model.request.systemMessage;

import bg.energo.phoenix.model.customAnotations.nomenclature.systemMessages.SystemMessageEditRequestValidator;
import bg.energo.phoenix.model.enums.SystemMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Param {@link #id} id of system message
 * @Param {@link #title} title of system message
 * @Param {@link #messageText} text for the system message
 * @Param {@link #okButtonText} Ok button text for the system message
 * @Param {@link #noButtonText} No button text for the system message
 * @Param {@link #cancelButtonText} Cancel button text for the system message
 * @Param {@link #systemMessageType} Type of the system message
 */
@Data
@SystemMessageEditRequestValidator
public class SystemMessageEditRequest {

    @NotNull(message = "id-ID must not be null;")
    private Long id;

    @NotBlank(message = "title-Title must not be empty;")
    @Size(min = 1,max = 256,message = "title-Title length should be between {min} and {max} characters;")
    private String title;

    @NotBlank(message = "titleTransliterated-TitleTransliterated shouldn't be blank;")
    @Size(min = 1,max = 256,message = "titleTransliterated-TitleTransliterated length should be {min} to {max};")
    private String titleTransliterated;

    @NotBlank(message = "messageText-Message text must not be empty")
    @Size(min = 1,max = 2048,message = "messageText-Message text length should be between {min} and {max} characters;")
    private String messageText;

    @NotBlank(message = "messageTextTransliterated-MessageTransliterated text shouldn't be blank;")
    @Size(min = 1,max = 2048,message = "messageTextTransliterated-MessageTransliterated text length should be {min} to {max};")
    private String messageTextTransliterated;

    private String okButtonText;

    private String okButtonTextTransliterated;

    private String noButtonText;

    private String noButtonTextTransliterated;

    private String cancelButtonText;

    private String cancelButtonTextTransliterated;

    @NotNull(message = "systemMessageType-System message type shouldn't be null;")
    private SystemMessageType systemMessageType;

}
