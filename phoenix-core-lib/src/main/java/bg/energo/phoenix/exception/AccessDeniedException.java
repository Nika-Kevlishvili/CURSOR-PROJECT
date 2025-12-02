package bg.energo.phoenix.exception;

import static bg.energo.phoenix.exception.ErrorCode.ACCESS_DENIED;

public class AccessDeniedException extends ClientException {

    public AccessDeniedException(String message) {
        super(message, ACCESS_DENIED);
    }

}