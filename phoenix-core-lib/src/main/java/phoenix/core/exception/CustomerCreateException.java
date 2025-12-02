package phoenix.core.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomerCreateException extends ClientException {

    private final List<String> errors;

    public CustomerCreateException(List<String> errors) {
        super("Error while creating customer", ErrorCode.CUSOMER_CREATE_ERROR);
        this.errors = errors;
    }


}
