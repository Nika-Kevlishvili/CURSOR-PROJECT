package phoenix.core.exception;

/**
 * ClientExceptions should be used when client makes wrong requests,
 * like violating validation rules or has insufficient rights.
 * Exception handling logic should treat these exceptions like 4XX errors
 */
public class ClientException extends RuntimeException {

    private final ErrorCode errorCode;

    public ClientException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ClientException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
