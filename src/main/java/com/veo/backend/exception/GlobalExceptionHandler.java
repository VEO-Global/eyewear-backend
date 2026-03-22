package com.veo.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

        HttpStatus status = switch (ex.getErrorCode()) {
            case LOCATION_API_ERROR -> HttpStatus.BAD_GATEWAY;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND, PRODUCT_NOT_FOUND, PRODUCT_VARIANT_NOT_FOUND, LENS_PRODUCT_NOT_FOUND,
                 CATEGORY_NOT_FOUND, ORDER_NOT_FOUND, PAYMENT_NOT_FOUND, RETURN_REQUEST_NOT_FOUND,
                 APPOINTMENT_NOT_FOUND, NOTIFICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, PRODUCT_ALREADY_EXIST, PRODUCT_VARIANT_ALREADY_EXIST, CATEGORY_ALREADY_EXIST,
                 EMAIL_ALREADY_EXIST, PAYMENT_ALREADY_CONFIRMED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity
                .status(status)
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .message("Request body contains invalid or unsupported values")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .message("Request contains invalid query or path parameter values")
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
