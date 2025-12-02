package phoenix.core.exception;

public class DomainEntityNotFoundException extends ClientException {

    public DomainEntityNotFoundException(String message) {
        super(message, ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
    }
}
