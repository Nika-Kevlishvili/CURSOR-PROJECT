package bg.energo.phoenix.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RestError {

    private ErrorCode errorCode;
    private String exceptionId;
    private String message;
}
