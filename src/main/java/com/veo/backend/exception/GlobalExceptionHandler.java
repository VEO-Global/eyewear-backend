package com.veo.backend.exception;

import com.veo.backend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request){
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
                .body(buildErrorResponse(status, ex.getErrorCode().name(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request){
        String message = ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), message, request)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Request body contains invalid or unsupported values",
                        request
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Request contains invalid query or path parameter values",
                        request
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request){
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        resolveUnexpectedMessage(ex),
                        request
                ));
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String resolveUnexpectedMessage(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }

        if (message == null || message.isBlank()) {
            return "Unexpected server error";
        }

        return message;
    }
}
