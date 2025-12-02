package bg.energo.phoenix.exception;

import static bg.energo.phoenix.exception.ErrorCode.OPERATION_NOT_ALLOWED;

public class OperationNotAllowedException extends ClientException {

    public OperationNotAllowedException(String message) {
        super(message, OPERATION_NOT_ALLOWED);
    }

}
