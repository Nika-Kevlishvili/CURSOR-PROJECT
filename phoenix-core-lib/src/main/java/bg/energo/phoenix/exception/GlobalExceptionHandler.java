package bg.energo.phoenix.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.util.Locale;

import static bg.energo.phoenix.exception.ErrorCode.*;


@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String EXCEPTION_MSG_FORMAT = "%s, %s";

    private final RestErrorFactory restErrorFactory;

    private final MessageSource messageSource;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        StringTrimmerEditor stringTrimmer = new StringTrimmerEditor(false);
        binder.registerCustomEditor(String.class, stringTrimmer);
    }

    @ExceptionHandler
    public ResponseEntity<RestError> handleIllegalArgumentException(IllegalArgumentException exception) {
        return handleApplicationException(exception, HttpStatus.BAD_REQUEST, ILLEGAL_ARGUMENTS_PROVIDED);
    }

    @NonNull
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers, @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        var message = ex.getMessage();
        if (ex.getCause() instanceof InvalidFormatException) {
            message = ((InvalidFormatException) ex.getCause()).getPathReference();
        }

        RestError error = this.restErrorFactory.createError(ex,
                ILLEGAL_ARGUMENTS_PROVIDED,
                message);
        logError(ex, error);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @NonNull
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  @NonNull HttpHeaders headers, @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {

        StringBuilder stringBuilder = new StringBuilder();

        for (ObjectError error : exception.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError) {
                if (((FieldError) error).getField().contains("[")) {
                    stringBuilder.append(((FieldError) error).getField()).append("-").append(error.getDefaultMessage().substring(error.getDefaultMessage().indexOf('-') + 1));
                } else stringBuilder.append(error.getDefaultMessage());
            } else stringBuilder.append(error.getDefaultMessage());
        }

        RestError error = this.restErrorFactory.createError(exception, ILLEGAL_ARGUMENTS_PROVIDED, stringBuilder.toString());
        logError(exception, error);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler
    public ResponseEntity<RestError> handleJavaAccessDeniedException(AccessDeniedException exception) {
        return handleApplicationException(exception, HttpStatus.FORBIDDEN, ACCESS_DENIED);
    }

    @ExceptionHandler
    public ResponseEntity<RestError> handleLockReleaseException(LockException exception) {
        return handleApplicationException(exception, HttpStatus.CONFLICT, CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<RestError> handleAnyException(Throwable exception) {
        return handleApplicationException(exception, HttpStatus.INTERNAL_SERVER_ERROR, APPLICATION_ERROR);
    }

    @ExceptionHandler
    public ResponseEntity<RestError> handleClientException(ClientException exception) {
        String errorCode = String.valueOf(exception.getErrorCode());
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        if (errorCode.equals("ACCESS_DENIED"))
            httpStatus = HttpStatus.FORBIDDEN;

        return handleApplicationException(exception, httpStatus, APPLICATION_ERROR);
    }

//    @NonNull
//    @Override
//    protected ResponseEntity<Object> handleBindException(@NonNull BindException exception, @NonNull HttpHeaders headers,
//                                                         @NonNull HttpStatusCode status, @NonNull WebRequest request) {
//        StringBuilder exceptionBuilder = new StringBuilder();
//
//        for (ObjectError errorObject : exception.getAllErrors()) {
//            try {
//                if (errorObject instanceof FieldError) {
//                    if (Objects.equals(errorObject.getCode(), "typeMismatch")) {
//                        FieldError fieldError = (FieldError) errorObject;
//                        Class<?> fieldType = exception.getFieldType(fieldError.getField());
//                        if (fieldType != null) {
//                            if (fieldType.isEnum()) {
//                                Object[] enumConstants = fieldType.getEnumConstants();
//                                exceptionBuilder.append("%s.%s-Illegal Type Provided: [%s], valid arguments: %s;".formatted(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(), Arrays.toString(enumConstants)));
//                            } else {
//                                exceptionBuilder.append(errorObject.getDefaultMessage());
//                            }
//                        } else {
//                            exceptionBuilder.append(errorObject.getDefaultMessage());
//                        }
//                    } else {
//                        exceptionBuilder.append(errorObject.getDefaultMessage());
//                    }
//                } else {
//                    exceptionBuilder.append(errorObject.getDefaultMessage());
//                }
//            } catch (Exception e) {
//                exceptionBuilder.append(errorObject.getDefaultMessage());
//            }
//        }
//
//        RestError error = this
//                .restErrorFactory
//                .createError(
//                        exception,
//                        APPLICATION_ERROR,
//                        exceptionBuilder.toString()
//                );
//
//        logError(exception, error);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(error);
//    }

    private ResponseEntity<RestError> handleApplicationException(Throwable exception, HttpStatus httpStatus, ErrorCode errorCode) {
        String exceptionMessage = exception.getMessage();
        RestError error;

        if (exceptionMessage != null) {
            String[] parts = exceptionMessage.split("[<>]");
            String messageKey = parts[0];
            Object[] parameters = null;

            if (parts.length > 1) {
                String[] paramStrings = parts[1].split(",");
                parameters = new Object[paramStrings.length];
                for (int i = 0; i < paramStrings.length; i++) {
                    parameters[i] = paramStrings[i];
                }
            }

            Locale locale = LocaleContextHolder.getLocale();
            String errorMessage = messageSource.getMessage(messageKey, parameters, exceptionMessage, locale);
            error = new RestError(errorCode, exception.getClass().getSimpleName(), errorMessage);
        } else {
            error = this.restErrorFactory.createError(exception, APPLICATION_ERROR);
        }

        logError(exception, error);
        return ResponseEntity.status(httpStatus).body(error);
    }

    private void logError(Throwable exception, RestError error) {
        var message = EXCEPTION_MSG_FORMAT.formatted(error.getExceptionId(), exception.getMessage());
        this.logger.error(message, exception);
    }
}
