package bg.energo.phoenix.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class XEnergieCommunicationException extends Exception {
    private final int statusCode;
    private final String code;
    private final String description;

    public XEnergieCommunicationException(int statusCode, String code, String description) {
        super("Exception handled while communicating with xEnergie, http status code: [%s], error code: [%s], description: [%s]".formatted(statusCode, code, description));
        this.statusCode = statusCode;
        this.code = code;
        this.description = description;
    }
}
