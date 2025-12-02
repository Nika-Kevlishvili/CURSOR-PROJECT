package bg.energo.phoenix.model.response.nomenclature.systemMessage;

import bg.energo.phoenix.model.entity.SystemMessage;
import bg.energo.phoenix.model.enums.SystemMessageType;
import lombok.Data;

/**
 * @Param {@link #id} db id of System massage
 * @Param {@link #title} string title of System massage
 * @Param {@link #name} string name of System massage
 * @Param {@link #messageText} string text of the System massage
 * @Param {@link #okButtonText} string ok button text of System massage
 * @Param {@link #noButtonText} string no button text of System massage
 * @Param {@link #cancelButtonText} string cancel button text of System massage
 * @Param {@link #key} unique string key of System massage
 * @Param {@link #systemMessageType} type of the System message
 * @Param {@link #okEnabled} enable status of ok button is active or not
 * @Param {@link #noEnabled} enable status of  no button is active or not
 * @Param {@link #cancelEnabled} enable status of  cancel button is active or not
 */
@Data
public class SystemMessageResponse {
    private Long id;
    private String title;
    private String titleTransliterated;
    private String name;
    private String messageText;
    private String messageTextTransliterated;
    private String okButtonText;
    private String okButtonTextTransliterated;
    private String noButtonText;
    private String noButtonTextTransliterated;
    private String cancelButtonText;
    private String cancelButtonTextTransliterated;
    private String key;
    private SystemMessageType systemMessageType;
    private Boolean okEnabled;
    private Boolean noEnabled;
    private Boolean cancelEnabled;

    public SystemMessageResponse(SystemMessage systemMessage) {
        this.id = systemMessage.getId();
        this.title = systemMessage.getTitle();
        this.titleTransliterated = systemMessage.getTitleTransliterated();
        this.name = systemMessage.getName();
        this.messageText = systemMessage.getMessageText();
        this.messageTextTransliterated = systemMessage.getMessageTextTransliterated();
        this.okButtonText = systemMessage.getOkButtonText();
        this.okButtonTextTransliterated = systemMessage.getOkButtonTextTransliterated();
        this.noButtonText = systemMessage.getNoButtonText();
        this.noButtonTextTransliterated = systemMessage.getNoButtonTextTransliterated();
        this.cancelButtonText = systemMessage.getCancelButtonText();
        this.cancelButtonTextTransliterated = systemMessage.getCancelButtonTextTransliterated();
        this.key = systemMessage.getKey();
        this.systemMessageType = systemMessage.getSystemMessageType();
        this.okEnabled = systemMessage.getOkEnabled();
        this.noEnabled = systemMessage.getNoEnabled();
        this.cancelEnabled = systemMessage.getCancelEnabled();
    }

}
