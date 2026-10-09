package bg.energo.phoenix.virtualpos.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VirtualPosStatusCode {

    OK("00", "OK"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_SUBSCRIBER_NUMBER("14", "Invalid subscriber number (IDN)"),
    NO_OBLIGATION("62", "No Obligation"),
    TEMPORARILY_UNABLE_TO_EXECUTE("80", "Temporarily unable to execute"),
    INVALID_CHECKSUM("93", "Invalid checksum (CHECKSUM)"),
    REPEAT_NOTIFICATION("94", "Repeat of already received notification"),
    GENERAL_ERROR("96", "General error");

    private final String code;
    private final String description;
}


