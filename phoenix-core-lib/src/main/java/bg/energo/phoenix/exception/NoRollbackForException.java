package bg.energo.phoenix.exception;

public class NoRollbackForException extends RuntimeException {

    public NoRollbackForException(String message) {
        super(message);
    }

}