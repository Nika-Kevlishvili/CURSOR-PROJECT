package bg.energo.phoenix.exception;

import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;

public class DomainEntityNotFoundException extends ClientException {

    public DomainEntityNotFoundException(String message) {
        super(message, DOMAIN_ENTITY_NOT_FOUND);
    }
}
