package bg.energo.phoenix.exception;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

public class IllegalArgumentsProvidedException extends ClientException {

    public IllegalArgumentsProvidedException(String message) {
        super(message, ILLEGAL_ARGUMENTS_PROVIDED);
    }

}
