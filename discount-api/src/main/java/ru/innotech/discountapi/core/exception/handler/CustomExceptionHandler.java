package ru.innotech.discountapi.core.exception.handler;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.innotech.discountapi.core.exception.ExceptionResponse;
import ru.innotech.discountapi.core.exception.NotFoundException;
import ru.innotech.discountapi.core.exception.TooManyRequestsException;

@RestControllerAdvice
@RequiredArgsConstructor
public class CustomExceptionHandler  {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Object> handleAllException(Exception exception, WebRequest webRequest) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse(message, LocalDateTime.now(), webRequest.getDescription(false));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleException(NotFoundException exception, WebRequest webRequest) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse(message, LocalDateTime.now(), webRequest.getDescription(false));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public final ResponseEntity<ExceptionResponse> handleException(TooManyRequestsException exception, WebRequest webRequest) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse(message, LocalDateTime.now(), webRequest.getDescription(false));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(exceptionResponse);
    }

}
