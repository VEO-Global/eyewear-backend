package com.veo.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex){
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode().name())
                .message(ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex){
        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex){
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .message("Something went wrong")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
