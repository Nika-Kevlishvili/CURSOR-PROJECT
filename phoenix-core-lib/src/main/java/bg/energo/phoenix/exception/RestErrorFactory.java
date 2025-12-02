package bg.energo.phoenix.exception;

import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class RestErrorFactory {

    public RestError createError(Throwable throwable, ErrorCode errorCode) {
        return createError(throwable, errorCode, null);
    }

    public RestError createError(Throwable throwable, ErrorCode errorCode, String message) {
        return this.prepareErrorWithDetails(throwable, errorCode, message);
    }

    private RestError prepareErrorWithDetails(Throwable throwable, ErrorCode errorCode, String message) {
        String errorMessage = isBlank(message) ? throwable.getMessage() : message;
        return new RestError(errorCode, this.getUniqueId(), errorMessage);
    }

    private String getUniqueId() {
        return UUID.randomUUID().toString();
    }

}
