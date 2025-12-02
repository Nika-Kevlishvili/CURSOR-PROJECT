package phoenix.core.exception;


public class OperationNotAllowedException extends ClientException {

    public OperationNotAllowedException(String message) {
        super(message, ErrorCode.OPERATION_NOT_ALLOWED);
    }

}
